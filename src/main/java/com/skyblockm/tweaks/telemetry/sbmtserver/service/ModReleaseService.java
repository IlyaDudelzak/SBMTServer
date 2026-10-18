package com.skyblockm.tweaks.telemetry.sbmtserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ModReleaseService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("v?\\d+\\.\\d+(\\.\\d+)*");

    private final String githubRepo;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Cache TTL: 15 minutes
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private Instant lastFetchTime = Instant.EPOCH;

    private final AtomicReference<ReleaseInfo> cachedStable;
    private final AtomicReference<ReleaseInfo> cachedNightly;

    public record ReleaseInfo(
            String version,
            String name,
            String releaseUrl,
            String publishedAt
    ) {}

    public record ModVersionsInfo(
            String version,
            String releaseUrl,
            ReleaseInfo stable,
            ReleaseInfo nightly
    ) {
        public ModVersionsInfo(ReleaseInfo stable, ReleaseInfo nightly) {
            this(
                    stable != null ? stable.version() : "—",
                    stable != null ? stable.releaseUrl() : "",
                    stable,
                    nightly
            );
        }
    }

    public ModReleaseService(
            @Value("${app.github.repo:IlyaDudelzak/SkyBlockM-Tweaks-plus}") String githubRepo
    ) {
        this.githubRepo = githubRepo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cachedStable = new AtomicReference<>(
                new ReleaseInfo("—", "Stable Release", "https://github.com/" + githubRepo + "/releases", "")
        );
        this.cachedNightly = new AtomicReference<>(
                new ReleaseInfo("—", "Nightly Build", "https://github.com/" + githubRepo + "/releases/tag/nightly", "")
        );
    }

    public ModVersionsInfo getVersions() {
        if (Instant.now().isAfter(lastFetchTime.plus(CACHE_TTL))) {
            refreshReleaseInfo();
        }
        return new ModVersionsInfo(cachedStable.get(), cachedNightly.get());
    }

    public ReleaseInfo getLatestRelease() {
        return getVersions().stable();
    }

    public ReleaseInfo getNightlyRelease() {
        return getVersions().nightly();
    }

    public synchronized void refreshReleaseInfo() {
        if (Instant.now().isBefore(lastFetchTime.plus(CACHE_TTL))
                && !cachedStable.get().version().equals("—")
                && !cachedNightly.get().version().equals("—")) {
            return;
        }

        try {
            String url = "https://api.github.com/repos/" + githubRepo + "/releases?per_page=10";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SBMTServer")
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode releases = objectMapper.readTree(response.body());
                if (releases != null && releases.isArray()) {
                    ReleaseInfo foundStable = null;
                    ReleaseInfo foundNightly = null;

                    for (JsonNode rel : releases) {
                        String tagName = rel.has("tag_name") ? rel.get("tag_name").asText() : "";
                        String name = rel.has("name") ? rel.get("name").asText() : tagName;
                        String htmlUrl = rel.has("html_url") ? rel.get("html_url").asText() : "";
                        String publishedAt = rel.has("published_at") ? rel.get("published_at").asText() : "";
                        boolean isPrerelease = rel.has("prerelease") && rel.get("prerelease").asBoolean();

                        if ("nightly".equalsIgnoreCase(tagName) || tagName.toLowerCase().contains("nightly")) {
                            if (foundNightly == null) {
                                String cleanVer = extractVersion(name, tagName);
                                foundNightly = new ReleaseInfo(cleanVer, name, htmlUrl, publishedAt);
                            }
                        } else if (!isPrerelease && foundStable == null) {
                            foundStable = new ReleaseInfo(tagName, name, htmlUrl, publishedAt);
                        }
                    }

                    if (foundStable != null) {
                        cachedStable.set(foundStable);
                        log.info("Updated stable release: {} ({})", foundStable.version(), foundStable.releaseUrl());
                    }
                    if (foundNightly != null) {
                        cachedNightly.set(foundNightly);
                        log.info("Updated nightly release: {} ({})", foundNightly.version(), foundNightly.releaseUrl());
                    }
                    lastFetchTime = Instant.now();
                }
            } else {
                log.warn("Failed to fetch releases from GitHub (status: {}): {}", response.statusCode(), response.body());
                lastFetchTime = Instant.now().minus(CACHE_TTL).plus(Duration.ofMinutes(2));
            }
        } catch (Exception e) {
            log.warn("Error fetching releases from GitHub: {}", e.getMessage());
            lastFetchTime = Instant.now().minus(CACHE_TTL).plus(Duration.ofMinutes(2));
        }
    }

    private String extractVersion(String name, String fallback) {
        if (name != null) {
            Matcher matcher = VERSION_PATTERN.matcher(name);
            if (matcher.find()) {
                String ver = matcher.group();
                if (!ver.startsWith("v")) {
                    ver = "v" + ver;
                }
                return ver + " (dev)";
            }
        }
        return fallback;
    }
}
