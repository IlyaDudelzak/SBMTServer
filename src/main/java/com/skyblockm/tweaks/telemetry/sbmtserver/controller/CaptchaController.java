package com.skyblockm.tweaks.telemetry.sbmtserver.controller;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaBatchUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Captcha", description = "Эндпоинты для загрузки и сохранения капч")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "Загрузить одну капчу")
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadCaptcha(@Valid @RequestBody CaptchaUploadDto dto) {
        boolean saved = captchaService.processAndSaveCaptcha(dto);

        if (saved) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "SAVED"));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("status", "DUPLICATE_IGNORED"));
        }
    }

    @Operation(summary = "Загрузить пачку капч (до 9 штук)")
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> uploadCaptchaBatch(@Valid @RequestBody CaptchaBatchUploadDto dto) {
        int saved = captchaService.processAndSaveCaptchas(dto.captchas());
        int total = dto.captchas().size();

        return ResponseEntity.status(saved > 0 ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of(
                        "status", "PROCESSED",
                        "total", total,
                        "saved", saved,
                        "ignored", total - saved
                ));
    }
}