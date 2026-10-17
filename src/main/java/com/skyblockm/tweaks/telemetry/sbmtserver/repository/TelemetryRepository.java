package com.skyblockm.tweaks.telemetry.sbmtserver.repository;

import com.skyblockm.tweaks.telemetry.sbmtserver.entity.TelemetryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TelemetryRepository extends JpaRepository<TelemetryEvent, Long> {

    Page<TelemetryEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t.modVersion, COUNT(t) FROM TelemetryEvent t GROUP BY t.modVersion ORDER BY COUNT(t) DESC")
    List<Object[]> countByModVersion();

    @Query("SELECT t.eventType, COUNT(t) FROM TelemetryEvent t GROUP BY t.eventType ORDER BY COUNT(t) DESC")
    List<Object[]> countByEventType();
}