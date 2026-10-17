package com.skyblockm.tweaks.telemetry.sbmtserver.dto;

import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaPicture;

import java.time.Instant;
import java.util.Base64;

public record CaptchaResponseDto(
        Long id,
        String exactHash,
        Long perceptualHash,
        CaptchaClass predictedClass,
        Float confidence,
        Boolean solvedSuccessfully,
        Instant createdAt,
        String colorsBase64
) {
    public static CaptchaResponseDto fromEntity(CaptchaPicture entity, boolean includeColors) {
        String base64 = null;
        if (includeColors && entity.getColors() != null) {
            base64 = Base64.getEncoder().encodeToString(entity.getColors());
        }
        return new CaptchaResponseDto(
                entity.getId(),
                entity.getExactHash(),
                entity.getPerceptualHash(),
                entity.getPredictedClass(),
                entity.getConfidence(),
                entity.getSolvedSuccessfully(),
                entity.getCreatedAt(),
                base64
        );
    }
}
