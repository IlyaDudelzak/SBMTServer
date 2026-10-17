package com.skyblockm.tweaks.telemetry.sbmtserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(
        name = "captcha_pictures",
        indexes = {
                @Index(name = "idx_exact_hash", columnList = "exactHash", unique = true),
                @Index(name = "idx_perceptual_hash", columnList = "perceptualHash"),
                @Index(name = "idx_predicted_class_conf", columnList = "predictedClass, confidence")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptchaPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16384)
    private byte[] colors;

    // Точный SHA-256 (отсекает 100% идентичные байтовые дубликаты)
    @Column(nullable = false, unique = true, length = 64)
    private String exactHash;

    // 64-битный визуальный хеш (устойчив к сдвигу цвета/яркости)
    @Column(nullable = false)
    private Long perceptualHash;

    // Предсказанный нейронкой класс (CHESTS, PIGS, ZOMBIES, UNKNOWN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaptchaClass predictedClass;

    // Уверенность нейронки (от 0.0 до 1.0)
    @Column(nullable = false)
    private Float confidence;

    // Прошла ли капча успешно на сервере (если мод может это отследить)
    private Boolean solvedSuccessfully;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}