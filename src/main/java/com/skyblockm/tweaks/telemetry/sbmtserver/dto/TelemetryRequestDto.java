package com.skyblockm.tweaks.telemetry.sbmtserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record TelemetryRequestDto(
        @Size(max = 64)
        String username,

        @NotBlank(message = "Mod version is required")
        @Size(max = 32)
        String modVersion,

        @NotBlank(message = "Minecraft version is required")
        @Size(max = 32)
        String mcVersion,

        @NotBlank(message = "Event type is required")
        String eventType, // например: "STARTUP", "CRASH", "HEARTBEAT"

        Map<String, Object> payload // дополнительные данные (ОС, версия Java, стектрейс)
) {}