package com.skyblockm.tweaks.telemetry.sbmtserver.repository;

import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaPicture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaptchaRepository extends JpaRepository<CaptchaPicture, Long> {

    // 1. Проверка полного 100% совпадения байтов
    boolean existsByExactHash(String exactHash);

    // 2. Поиск визуальных дубликатов (расстояние Хэмминга <= maxDistance)
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM captcha_pictures 
            WHERE bit_count(CAST(perceptual_hash AS bit(64)) # CAST(:pHash AS bit(64))) <= :maxDistance
        )
        """, nativeQuery = true)
    boolean existsVisualDuplicate(@Param("pHash") long perceptualHash, @Param("maxDistance") int maxDistance);

    // 3. Выборка сложных примеров для дообучения нейронки
    List<CaptchaPicture> findByConfidenceLessThanOrPredictedClassOrderByCreatedAtDesc(
            Float maxConfidence, CaptchaClass targetClass
    );

    Page<CaptchaPicture> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CaptchaPicture> findByConfidenceLessThanOrPredictedClassOrderByCreatedAtDesc(
            Float maxConfidence, CaptchaClass targetClass, Pageable pageable
    );

    long countByConfidenceLessThanOrPredictedClass(Float maxConfidence, CaptchaClass targetClass);

    @Query("SELECT c.predictedClass, COUNT(c) FROM CaptchaPicture c GROUP BY c.predictedClass")
    List<Object[]> countByPredictedClass();
}