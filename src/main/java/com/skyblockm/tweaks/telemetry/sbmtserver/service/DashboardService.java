package com.skyblockm.tweaks.telemetry.sbmtserver.service;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaResponseDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.StatsResponseDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaPicture;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.TelemetryEvent;
import com.skyblockm.tweaks.telemetry.sbmtserver.repository.CaptchaRepository;
import com.skyblockm.tweaks.telemetry.sbmtserver.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TelemetryRepository telemetryRepository;
    private final CaptchaRepository captchaRepository;

    private static final float LOW_CONFIDENCE_THRESHOLD = 0.85f;

    private static final int[][] BASE_COLORS = {
            {0, 0, 0}, {127, 178, 56}, {247, 233, 163}, {199, 199, 199}, {255, 0, 0},
            {160, 160, 255}, {167, 167, 167}, {0, 124, 0}, {255, 255, 255}, {164, 168, 184},
            {151, 109, 77}, {112, 112, 112}, {64, 64, 255}, {143, 119, 72}, {255, 252, 245},
            {216, 127, 51}, {178, 76, 216}, {102, 153, 216}, {229, 229, 51}, {127, 204, 25},
            {242, 127, 165}, {76, 76, 76}, {153, 153, 153}, {76, 127, 153}, {127, 63, 178},
            {51, 76, 178}, {102, 76, 51}, {102, 127, 51}, {153, 51, 51}, {25, 25, 25},
            {250, 238, 77}, {92, 219, 213}, {74, 128, 255}, {0, 217, 58}, {129, 86, 49},
            {112, 2, 0}, {209, 177, 161}, {159, 82, 36}, {149, 87, 108}, {112, 108, 138},
            {186, 133, 36}, {103, 117, 53}, {160, 77, 78}, {57, 41, 35}, {135, 107, 98},
            {87, 92, 92}, {122, 73, 88}, {76, 62, 92}, {76, 50, 35}, {76, 82, 42},
            {142, 60, 46}, {37, 22, 16}, {189, 48, 49}, {148, 63, 97}, {92, 25, 29},
            {22, 126, 134}, {58, 142, 140}, {86, 44, 62}, {20, 180, 133}, {100, 100, 100},
            {216, 175, 147}, {127, 167, 150}
    };

    private static final float[] MULTIPLIERS = {0.705f, 0.862f, 1.0f, 0.529f};
    private static final int[] COLOR_PALETTE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int baseColorId = i >>> 2;
            int shadeId = i & 3;

            if (baseColorId < BASE_COLORS.length) {
                int[] base = BASE_COLORS[baseColorId];
                float mult = MULTIPLIERS[shadeId];
                int r = Math.clamp(Math.round(base[0] * mult), 0, 255);
                int g = Math.clamp(Math.round(base[1] * mult), 0, 255);
                int b = Math.clamp(Math.round(base[2] * mult), 0, 255);
                COLOR_PALETTE[i] = (r << 16) | (g << 8) | b;
            } else {
                COLOR_PALETTE[i] = 0;
            }
        }
    }

    @Transactional(readOnly = true)
    public StatsResponseDto getStats() {
        long totalTelemetry = telemetryRepository.count();
        long totalCaptchas = captchaRepository.count();
        long hardCases = captchaRepository.countByConfidenceLessThanOrPredictedClass(
                LOW_CONFIDENCE_THRESHOLD, CaptchaClass.UNKNOWN
        );

        Map<String, Long> byModVersion = new LinkedHashMap<>();
        for (Object[] row : telemetryRepository.countByModVersion()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                byModVersion.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> byEventType = new LinkedHashMap<>();
        for (Object[] row : telemetryRepository.countByEventType()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                byEventType.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> byCaptchaClass = new LinkedHashMap<>();
        for (Object[] row : captchaRepository.countByPredictedClass()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                byCaptchaClass.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        return new StatsResponseDto(
                totalTelemetry,
                totalCaptchas,
                hardCases,
                byModVersion,
                byEventType,
                byCaptchaClass
        );
    }

    @Transactional(readOnly = true)
    public Page<TelemetryEvent> getTelemetry(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return telemetryRepository.findAllByOrderByCreatedAtDesc(pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<CaptchaResponseDto> getCaptchas(int page, int size, boolean hardCasesOnly) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CaptchaPicture> entities;
        if (hardCasesOnly) {
            entities = captchaRepository.findByConfidenceLessThanOrPredictedClassOrderByCreatedAtDesc(
                    LOW_CONFIDENCE_THRESHOLD, CaptchaClass.UNKNOWN, pageRequest
            );
        } else {
            entities = captchaRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }
        return entities.map(entity -> CaptchaResponseDto.fromEntity(entity, true));
    }

    @Transactional(readOnly = true)
    public byte[] renderCaptchaPng(Long id) {
        CaptchaPicture picture = captchaRepository.findById(id).orElse(null);
        if (picture == null || picture.getColors() == null || picture.getColors().length != 16384) {
            return new byte[0];
        }

        byte[] colors = picture.getColors();
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int colorIndex = colors[y * 128 + x] & 0xFF;
                image.setRGB(x, y, COLOR_PALETTE[colorIndex]);
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to render captcha PNG for id: {}", id, e);
            return new byte[0];
        }
    }
}
