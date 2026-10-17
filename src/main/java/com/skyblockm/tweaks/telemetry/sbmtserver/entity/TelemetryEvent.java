package com.skyblockm.tweaks.telemetry.sbmtserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_events")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String modVersion;

    @Column(nullable = false, length = 32)
    private String mcVersion;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String rawPayload; // Или jsonb для PostgreSQL

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}