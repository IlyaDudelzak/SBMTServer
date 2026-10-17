package com.skyblockm.tweaks.telemetry.sbmtserver.dto;

import java.util.Map;

public record StatsResponseDto(
        long totalTelemetryEvents,
        long totalCaptchas,
        long hardCasesCount,
        Map<String, Long> telemetryByModVersion,
        Map<String, Long> telemetryByEventType,
        Map<String, Long> captchasByClass
) {}
