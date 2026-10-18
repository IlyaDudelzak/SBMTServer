package com.skyblockm.tweaks.telemetry.sbmtserver.controller;

import com.skyblockm.tweaks.telemetry.sbmtserver.service.ModReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Version", description = "Информация о версиях мода")
@RestController
@RequestMapping("/api/v1/version")
@RequiredArgsConstructor
public class VersionController {

    private final ModReleaseService modReleaseService;

    @Operation(summary = "Получить информацию о релизах мода (стабильный и nightly)")
    @GetMapping
    public ModReleaseService.ModVersionsInfo getVersions() {
        return modReleaseService.getVersions();
    }

    @Operation(summary = "Получить информацию только о стабильном релизе мода")
    @GetMapping("/latest")
    public ModReleaseService.ReleaseInfo getLatestVersion() {
        return modReleaseService.getLatestRelease();
    }

    @Operation(summary = "Получить информацию о nightly (dev) релизе мода")
    @GetMapping("/nightly")
    public ModReleaseService.ReleaseInfo getNightlyVersion() {
        return modReleaseService.getNightlyRelease();
    }
}
