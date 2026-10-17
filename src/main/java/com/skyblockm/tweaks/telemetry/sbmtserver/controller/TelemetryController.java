package com.skyblockm.tweaks.telemetry.sbmtserver.controller;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.TelemetryRequestDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED) // 202 Accepted
    public void receiveTelemetry(@Valid @RequestBody TelemetryRequestDto request) {
        service.recordEvent(request);
    }
}