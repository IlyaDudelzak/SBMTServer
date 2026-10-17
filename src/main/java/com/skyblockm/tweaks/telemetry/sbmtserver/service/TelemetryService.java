package com.skyblockm.tweaks.telemetry.sbmtserver.service;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.TelemetryRequestDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.TelemetryEvent;
import com.skyblockm.tweaks.telemetry.sbmtserver.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryRepository repository;

    public void recordEvent(TelemetryRequestDto dto) {
        TelemetryEvent event = TelemetryEvent.builder()
                .modVersion(dto.modVersion())
                .mcVersion(dto.mcVersion())
                .eventType(dto.eventType())
                .rawPayload(dto.payload() != null ? dto.payload().toString() : null)
                .build();

        repository.save(event);
    }
}