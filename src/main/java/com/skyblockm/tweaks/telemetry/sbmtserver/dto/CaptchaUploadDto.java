package com.skyblockm.tweaks.telemetry.sbmtserver.dto;

import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CaptchaUploadDto(
        @NotNull(message = "Colors are required")
        @Size(min = 16384, max = 16384, message = "Colors must be exactly 16384 bytes")
        @Schema(
                description = "Массив MapState.colors (16384 байта), закодированный в Base64",
                type = "string",
                format = "byte",
                example = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4ODpMTk5OU1TVV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6fX5/gIGCg4SFhoeIi4qLjI2Oj5CRkpOUlZaXmJmalZadm5ydnqGioqOjpKWmp6ipqqutrq+wsLKztLW2t7i5urvMzs/Q0dLT1NXV19jZ2tvc3d7f4OHi4+Tl5ufo6err7O3u7/Dx8vP09fb3+Pn6+/4="
        )
        byte[] colors,

        @NotNull
        CaptchaClass predictedClass,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        Float confidence,

        Boolean solvedSuccessfully
) {}