package com.skyblockm.tweaks.telemetry.sbmtserver.service;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaPicture;
import com.skyblockm.tweaks.telemetry.sbmtserver.repository.CaptchaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaRepository captchaRepository;

    // Порог уверенности: всё, что ниже 0.85 — считаем "сложным случаем" и сохраняем приоритетно
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.85f;

    // Максимальное отличие в битах (из 64) для признания картинок визуальными дубликатами
    private static final int MAX_HAMMING_DISTANCE = 3;

    /**
     * Принимает капчу от мода, проверяет на дубликаты и решает, нужно ли сохранять её в БД.
     *
     * @return true, если картинка была сохранена, false — если отброшена как дубликат.
     */
    @Transactional
    public boolean processAndSaveCaptcha(CaptchaUploadDto dto) {
        byte[] colors = dto.colors();

        // 1. Считаем точный SHA-256 хеш
        String exactHash = calculateSha256(colors);
        if (captchaRepository.existsByExactHash(exactHash)) {
            log.debug("Точный дубликат капчи (SHA-256: {}), пропускаем.", exactHash);
            return false;
        }

        // 2. Считаем 64-битный перцептивный хеш (устойчивый к сдвигу цвета/яркости)
        long pHash = calculatePerceptualHash(colors);

        // 3. Проверяем, является ли этот случай интересным для дообучения нейросети
        boolean isHardCase = dto.predictedClass() == CaptchaClass.UNKNOWN
                || dto.confidence() < LOW_CONFIDENCE_THRESHOLD
                || Boolean.FALSE.equals(dto.solvedSuccessfully());

        // 4. Проверяем визуальные дубликаты (с учетом небольшого цветового сдвига)
        boolean isVisualDuplicate = captchaRepository.existsVisualDuplicate(pHash, MAX_HAMMING_DISTANCE);

        // Если это визуальный дубликат И нейронка в нем уверена — не засоряем базу
        if (isVisualDuplicate && !isHardCase) {
            log.debug("Визуальный дубликат (pHash: {}, conf: {}), пропускаем.", pHash, dto.confidence());
            return false;
        }

        // 5. Сохраняем новую уникальную или сложную картинку
        CaptchaPicture picture = CaptchaPicture.builder()
                .colors(colors)
                .exactHash(exactHash)
                .perceptualHash(pHash)
                .predictedClass(dto.predictedClass())
                .confidence(dto.confidence())
                .solvedSuccessfully(dto.solvedSuccessfully())
                .build();

        captchaRepository.save(picture);
        log.info("Сохранена новая капча: id={}, class={}, confidence={}, hardCase={}",
                picture.getId(), picture.getPredictedClass(), picture.getConfidence(), isHardCase);

        return true;
    }

    /**
     * Получить картинку по ID (например, для просмотра или скачивания).
     */
    @Transactional(readOnly = true)
    public Optional<CaptchaPicture> getById(Long id) {
        return captchaRepository.findById(id);
    }

    /**
     * Получить список всех "проблемных" капч, где нейронка сомневалась или выдала UNKNOWN.
     */
    @Transactional(readOnly = true)
    public List<CaptchaPicture> getHardCasesForTraining() {
        return captchaRepository.findByConfidenceLessThanOrPredictedClassOrderByCreatedAtDesc(
                LOW_CONFIDENCE_THRESHOLD,
                CaptchaClass.UNKNOWN
        );
    }

    /**
     * Вычисляет 64-битный перцептивный хеш (aHash) для карты 128x128.
     * Отбрасывает младшие 2 бита яркости каждого пикселя, чтобы игнорировать цветовой шум сервера.
     */
    public long calculatePerceptualHash(byte[] colors) {
        double[] blockAverages = new double[64];
        double totalSum = 0;

        // Разбиваем карту 128x128 на 64 блока (сетка 8x8, каждый блок по 16x16 пикселей)
        for (int blockY = 0; blockY < 8; blockY++) {
            for (int blockX = 0; blockX < 8; blockX++) {
                int sum = 0;
                for (int y = 0; y < 16; y++) {
                    int rowOffset = (blockY * 16 + y) * 128 + (blockX * 16);
                    for (int x = 0; x < 16; x++) {
                        int unsignedByte = colors[rowOffset + x] & 0xFF;
                        // В MapState младшие 2 бита — это оттенок (0..3), старшие 6 бит — ID базового цвета
                        int baseColorId = unsignedByte >>> 2;
                        sum += baseColorId;
                    }
                }
                double blockAvg = sum / 256.0;
                blockAverages[blockY * 8 + blockX] = blockAvg;
                totalSum += blockAvg;
            }
        }

        double globalAvg = totalSum / 64.0;
        long hash = 0L;

        for (int i = 0; i < 64; i++) {
            if (blockAverages[i] >= globalAvg) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    /**
     * Вычисляет SHA-256 в виде hex-строки (64 символа).
     */
    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}