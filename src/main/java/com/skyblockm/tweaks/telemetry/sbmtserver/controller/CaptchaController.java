package com.skyblockm.tweaks.telemetry.sbmtserver.controller;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

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
}