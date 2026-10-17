package com.skyblockm.tweaks.telemetry.sbmtserver.entity;

public enum CaptchaClass {
    CHESTS("Сундуки"),
    PIGS("Свиньи"),
    ZOMBIES("Зомби"),
    UNKNOWN("Неизвестно");

    private final String displayName;

    CaptchaClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    public char getCode() {
        return switch (this) {
            case PIGS -> 'P';
            case ZOMBIES -> 'Z';
            case CHESTS -> 'C';
            default -> '?';
        };
    }
}