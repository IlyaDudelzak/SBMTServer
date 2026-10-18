package com.skyblockm.tweaks.telemetry.sbmtserver.controller;

import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaResponseDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.StatsResponseDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.TelemetryEvent;
import com.skyblockm.tweaks.telemetry.sbmtserver.service.DashboardService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Hidden
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping(value = {"", "/"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderDashboard() {
        return DASHBOARD_HTML;
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<StatsResponseDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/api/telemetry")
    @ResponseBody
    public ResponseEntity<Page<TelemetryEvent>> getTelemetry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(dashboardService.getTelemetry(page, size));
    }

    @GetMapping("/api/captchas")
    @ResponseBody
    public ResponseEntity<Page<CaptchaResponseDto>> getCaptchas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean hardCasesOnly
    ) {
        return ResponseEntity.ok(dashboardService.getCaptchas(page, size, hardCasesOnly));
    }

    @GetMapping(value = "/api/captchas/{id}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getCaptchaImage(@PathVariable Long id) {
        byte[] imageBytes = dashboardService.renderCaptchaPng(id);
        if (imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(imageBytes);
    }

    private static final String DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SBMT Server — Dashboard</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-body: #0b0f19;
            --bg-card: #111827;
            --bg-card-hover: #1f2937;
            --border: #1f2937;
            --text-main: #f3f4f6;
            --text-muted: #9ca3af;
            --accent: #3b82f6;
            --accent-hover: #2563eb;
            --danger: #ef4444;
            --danger-bg: rgba(239, 68, 68, 0.15);
            --success: #10b981;
            --success-bg: rgba(16, 185, 129, 0.15);
            --warning: #f59e0b;
            --warning-bg: rgba(245, 158, 11, 0.15);
            --purple: #8b5cf6;
            --purple-bg: rgba(139, 92, 246, 0.15);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', system-ui, sans-serif; background: var(--bg-body); color: var(--text-main); min-height: 100vh; padding: 24px; line-height: 1.5; }
        .container { max-width: 1400px; margin: 0 auto; }
        
        /* Header */
        header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
        .logo-area { display: flex; align-items: center; gap: 12px; }
        .logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg, #3b82f6, #8b5cf6); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #fff; font-size: 18px; }
        h1 { font-size: 20px; font-weight: 700; letter-spacing: -0.02em; }
        .badge-live { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; background: rgba(16, 185, 129, 0.15); color: var(--success); border-radius: 999px; font-size: 12px; font-weight: 500; }
        .dot { width: 8px; height: 8px; background: var(--success); border-radius: 50%; box-shadow: 0 0 8px var(--success); }
        .actions-top { display: flex; gap: 10px; align-items: center; }
        button { cursor: pointer; border: none; font-family: inherit; font-size: 13px; font-weight: 500; border-radius: 6px; padding: 8px 14px; transition: all 0.2s; }
        .btn-primary { background: var(--accent); color: white; }
        .btn-primary:hover { background: var(--accent-hover); }
        .btn-outline { background: transparent; border: 1px solid var(--border); color: var(--text-muted); }
        .btn-outline:hover { background: var(--bg-card-hover); color: var(--text-main); }

        /* Stats Grid */
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .stat-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 8px; }
        .stat-label { font-size: 13px; font-weight: 500; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
        .stat-value { font-size: 28px; font-weight: 700; color: var(--text-main); }
        .stat-sub { font-size: 12px; color: var(--text-muted); }

        /* Tabs */
        .tabs { display: flex; gap: 8px; border-bottom: 1px solid var(--border); margin-bottom: 20px; }
        .tab-btn { background: transparent; border: none; padding: 10px 18px; color: var(--text-muted); font-size: 14px; font-weight: 600; cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.2s; }
        .tab-btn.active { color: var(--accent); border-bottom-color: var(--accent); }
        .tab-btn:hover:not(.active) { color: var(--text-main); }

        /* Tab Content */
        .tab-pane { display: none; }
        .tab-pane.active { display: block; }

        /* Table Card */
        .panel-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
        .panel-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); flex-wrap: wrap; gap: 12px; }
        .panel-title { font-size: 16px; font-weight: 600; }
        table { width: 100%; border-collapse: collapse; text-align: left; font-size: 13px; }
        th { background: rgba(255,255,255,0.02); color: var(--text-muted); font-weight: 600; padding: 12px 20px; border-bottom: 1px solid var(--border); }
        td { padding: 14px 20px; border-bottom: 1px solid var(--border); color: var(--text-main); vertical-align: middle; }
        tr:hover td { background: rgba(255,255,255,0.015); }
        
        /* Badges */
        .badge { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; }
        .badge-chest { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
        .badge-pig { background: rgba(244, 63, 94, 0.15); color: #fb7185; }
        .badge-zombie { background: rgba(16, 185, 129, 0.15); color: #34d399; }
        .badge-unknown { background: rgba(245, 158, 11, 0.15); color: #fbbf24; }
        .badge-success { background: var(--success-bg); color: var(--success); }
        .badge-fail { background: var(--danger-bg); color: var(--danger); }

        /* Captcha Thumbnails */
        .captcha-img { width: 48px; height: 48px; border-radius: 6px; border: 1px solid #334155; image-rendering: pixelated; cursor: pointer; transition: transform 0.15s; background: #000; }
        .captcha-img:hover { transform: scale(1.1); border-color: var(--accent); }

        /* Pagination */
        .pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: var(--bg-card); border-top: 1px solid var(--border); font-size: 13px; color: var(--text-muted); }

        /* Modal */
        .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.75); backdrop-filter: blur(4px); display: none; align-items: center; justify-content: center; z-index: 999; }
        .modal { background: #1e293b; border: 1px solid #334155; border-radius: 12px; width: 90%; max-width: 600px; padding: 24px; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
        .modal-close { background: none; border: none; color: var(--text-muted); font-size: 20px; cursor: pointer; }
        .modal pre { background: #0b0f19; padding: 16px; border-radius: 8px; overflow-x: auto; color: #a5f3fc; font-size: 12px; max-height: 400px; border: 1px solid #1e293b; }
        
        /* Grid for Analytics */
        .analytics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
        .card-inner { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
        .bar-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
        .progress-bar { width: 100%; height: 8px; background: #1e293b; border-radius: 4px; overflow: hidden; margin-bottom: 14px; }
        .progress-fill { height: 100%; background: var(--accent); border-radius: 4px; }
    </style>
</head>
<body>
<div class="container">
    <header>
        <div class="logo-area">
            <div class="logo-icon">S</div>
            <div>
                <h1>SBMT Server</h1>
                <div style="font-size: 12px; color: var(--text-muted);">Панель мониторинга и телеметрии</div>
            </div>
            <span class="badge-live"><span class="dot"></span> Online</span>
        </div>
        <div class="actions-top">
            <button class="btn-outline" onclick="loadAllData()">Обновить данные</button>
        </div>
    </header>

    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-label">Событий телеметрии</div>
            <div class="stat-value" id="statTelemetry">...</div>
            <div class="stat-sub">Всего записей в базе данных</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Капч в базе</div>
            <div class="stat-value" id="statCaptchas">...</div>
            <div class="stat-sub">Уникальных картинок 128x128</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Сложные случаи (Hard)</div>
            <div class="stat-value" style="color: #f59e0b;" id="statHardCases">...</div>
            <div class="stat-sub">Confidence < 0.85 или UNKNOWN</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Релиз (Stable)</div>
            <div class="stat-value" style="color: #8b5cf6;" id="statLatestMod">...</div>
            <div class="stat-sub"><a id="statReleaseLink" href="https://github.com/IlyaDudelzak/SkyBlockM-Tweaks-plus/releases/latest" target="_blank" style="color: var(--accent); text-decoration: none;">GitHub Release ↗</a></div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Nightly (Dev)</div>
            <div class="stat-value" style="color: #ec4899;" id="statNightlyMod">...</div>
            <div class="stat-sub"><a id="statNightlyLink" href="https://github.com/IlyaDudelzak/SkyBlockM-Tweaks-plus/releases/tag/nightly" target="_blank" style="color: #ec4899; text-decoration: none;">Nightly Release ↗</a></div>
        </div>
    </div>

    <div class="tabs">
        <button class="tab-btn active" onclick="switchTab('tab-captchas', this)">Капчи</button>
        <button class="tab-btn" onclick="switchTab('tab-telemetry', this)">Телеметрия</button>
        <button class="tab-btn" onclick="switchTab('tab-analytics', this)">Аналитика</button>
    </div>

    <!-- TAB 1: CAPTCHAS -->
    <div id="tab-captchas" class="tab-pane active">
        <div class="panel-card">
            <div class="panel-header">
                <div class="panel-title">Сохраненные капчи</div>
                <div style="display: flex; gap: 8px; align-items: center;">
                    <label style="font-size: 13px; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px;">
                        <input type="checkbox" id="hardOnlyCheckbox" onchange="loadCaptchas(0)"> Только сложные случаи
                    </label>
                </div>
            </div>
            <table>
                <thead>
                    <tr>
                        <th style="width: 60px;">Превью</th>
                        <th>ID</th>
                        <th>Класс</th>
                        <th>Уверенность</th>
                        <th>Успешно?</th>
                        <th>Дата</th>
                        <th>pHash</th>
                    </tr>
                </thead>
                <tbody id="captchasTableBody">
                    <tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Загрузка...</td></tr>
                </tbody>
            </table>
            <div class="pagination">
                <span id="captchasPageInfo">Страница 1</span>
                <div>
                    <button class="btn-outline" id="captchasPrevBtn" onclick="prevCaptchaPage()">Назад</button>
                    <button class="btn-outline" id="captchasNextBtn" onclick="nextCaptchaPage()">Вперед</button>
                </div>
            </div>
        </div>
    </div>

    <!-- TAB 2: TELEMETRY -->
    <div id="tab-telemetry" class="tab-pane">
        <div class="panel-card">
            <div class="panel-header">
                <div class="panel-title">События телеметрии</div>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Игрок</th>
                        <th>Версия мода</th>
                        <th>MC Версия</th>
                        <th>Тип события</th>
                        <th>Дата</th>
                        <th>Payload</th>
                    </tr>
                </thead>
                <tbody id="telemetryTableBody">
                    <tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Загрузка...</td></tr>
                </tbody>
            </table>
            <div class="pagination">
                <span id="telemetryPageInfo">Страница 1</span>
                <div>
                    <button class="btn-outline" id="telemetryPrevBtn" onclick="prevTelemetryPage()">Назад</button>
                    <button class="btn-outline" id="telemetryNextBtn" onclick="nextTelemetryPage()">Вперед</button>
                </div>
            </div>
        </div>
    </div>

    <!-- TAB 3: ANALYTICS -->
    <div id="tab-analytics" class="tab-pane">
        <div class="analytics-grid">
            <div class="card-inner">
                <h3 style="font-size: 15px; margin-bottom: 16px;">Распределение по версиям мода</h3>
                <div id="analyticsModVersions"></div>
            </div>
            <div class="card-inner">
                <h3 style="font-size: 15px; margin-bottom: 16px;">Распределение по типам событий</h3>
                <div id="analyticsEventTypes"></div>
            </div>
            <div class="card-inner">
                <h3 style="font-size: 15px; margin-bottom: 16px;">Распределение по классам капч</h3>
                <div id="analyticsCaptchaClasses"></div>
            </div>
        </div>
    </div>
</div>

<!-- Modal Dialog -->
<div class="modal-overlay" id="modalOverlay" onclick="closeModal(event)">
    <div class="modal" onclick="event.stopPropagation()">
        <div class="modal-header">
            <h3 id="modalTitle">Информация</h3>
            <button class="modal-close" onclick="closeModal()">&times;</button>
        </div>
        <div id="modalBody"></div>
    </div>
</div>

<script>
    let captchaPage = 0;
    let captchaTotalPages = 1;
    let telemetryPage = 0;
    let telemetryTotalPages = 1;

    function switchTab(tabId, btn) {
        document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
        document.getElementById(tabId).classList.add('active');
        btn.classList.add('active');
    }

    async function loadStats() {
        try {
            const res = await fetch('/dashboard/api/stats');
            if (!res.ok) throw new Error('Failed to load stats');
            const data = await res.json();
            document.getElementById('statTelemetry').innerText = data.totalTelemetryEvents.toLocaleString();
            document.getElementById('statCaptchas').innerText = data.totalCaptchas.toLocaleString();
            document.getElementById('statHardCases').innerText = data.hardCasesCount.toLocaleString();

            const latestMod = data.latestModVersion || Object.keys(data.telemetryByModVersion || {})[0] || '—';
            document.getElementById('statLatestMod').innerText = latestMod;
            if (data.latestReleaseUrl) {
                const link = document.getElementById('statReleaseLink');
                if (link) link.href = data.latestReleaseUrl;
            }

            const nightlyMod = data.nightlyModVersion || '—';
            document.getElementById('statNightlyMod').innerText = nightlyMod;
            if (data.nightlyReleaseUrl) {
                const nLink = document.getElementById('statNightlyLink');
                if (nLink) nLink.href = data.nightlyReleaseUrl;
            }

            renderBars('analyticsModVersions', data.telemetryByModVersion, data.totalTelemetryEvents);
            renderBars('analyticsEventTypes', data.telemetryByEventType, data.totalTelemetryEvents);
            renderBars('analyticsCaptchaClasses', data.captchasByClass, data.totalCaptchas);
        } catch (e) {
            console.error(e);
        }
    }

    function renderBars(containerId, map, total) {
        const c = document.getElementById(containerId);
        c.innerHTML = '';
        if (!map || Object.keys(map).length === 0) {
            c.innerHTML = '<div style="color:var(--text-muted);font-size:13px;">Нет данных</div>';
            return;
        }
        for (const [key, count] of Object.entries(map)) {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            c.innerHTML += `
                <div class="bar-row"><span>${key}</span><span style="color:var(--text-muted);">${count} (${pct}%)</span></div>
                <div class="progress-bar"><div class="progress-fill" style="width: ${pct}%"></div></div>
            `;
        }
    }

    async function loadCaptchas(page = 0) {
        captchaPage = page;
        const hardOnly = document.getElementById('hardOnlyCheckbox').checked;
        const tbody = document.getElementById('captchasTableBody');
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Загрузка...</td></tr>';

        try {
            const res = await fetch(`/dashboard/api/captchas?page=${page}&size=20&hardCasesOnly=${hardOnly}`);
            const data = await res.json();
            captchaTotalPages = data.totalPages || 1;
            document.getElementById('captchasPageInfo').innerText = `Страница ${data.number + 1} из ${Math.max(1, data.totalPages)}`;
            document.getElementById('captchasPrevBtn').disabled = data.first;
            document.getElementById('captchasNextBtn').disabled = data.last;

            if (!data.content || data.content.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Капчи не найдены</td></tr>';
                return;
            }

            tbody.innerHTML = data.content.map(c => {
                let badgeClass = 'badge-unknown';
                if (c.predictedClass === 'CHESTS') badgeClass = 'badge-chest';
                else if (c.predictedClass === 'PIGS') badgeClass = 'badge-pig';
                else if (c.predictedClass === 'ZOMBIES') badgeClass = 'badge-zombie';

                const solvedBadge = c.solvedSuccessfully === true ? '<span class="badge badge-success">Да</span>' :
                                    c.solvedSuccessfully === false ? '<span class="badge badge-fail">Нет</span>' : '<span style="color:var(--text-muted)">—</span>';
                
                const confPct = Math.round(c.confidence * 100);
                const confColor = c.confidence >= 0.85 ? 'var(--success)' : '#f59e0b';

                return `
                    <tr>
                        <td>
                            <img class="captcha-img" src="/dashboard/api/captchas/${c.id}/image?t=${Date.now()}" alt="Captcha #${c.id}" onclick="showCaptchaZoom(${c.id}, '${c.predictedClass}', ${c.confidence})">
                        </td>
                        <td>#${c.id}</td>
                        <td><span class="badge ${badgeClass}">${c.predictedClass}</span></td>
                        <td><span style="font-weight:600; color:${confColor}">${confPct}%</span></td>
                        <td>${solvedBadge}</td>
                        <td style="color:var(--text-muted);">${new Date(c.createdAt).toLocaleString()}</td>
                        <td style="font-family:monospace; font-size:11px; color:var(--text-muted);">${c.perceptualHash}</td>
                    </tr>
                `;
            }).join('');
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--danger);">Ошибка загрузки капч</td></tr>';
        }
    }

    async function loadTelemetry(page = 0) {
        telemetryPage = page;
        const tbody = document.getElementById('telemetryTableBody');
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Загрузка...</td></tr>';

        try {
            const res = await fetch(`/dashboard/api/telemetry?page=${page}&size=20`);
            const data = await res.json();
            telemetryTotalPages = data.totalPages || 1;
            document.getElementById('telemetryPageInfo').innerText = `Страница ${data.number + 1} из ${Math.max(1, data.totalPages)}`;
            document.getElementById('telemetryPrevBtn').disabled = data.first;
            document.getElementById('telemetryNextBtn').disabled = data.last;

            if (!data.content || data.content.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">Событий нет</td></tr>';
                return;
            }

            tbody.innerHTML = data.content.map(t => {
                const escapedPayload = t.rawPayload ? t.rawPayload.replace(/"/g, '&quot;') : '';
                const userDisplay = t.username ? `<span style="font-weight:600;color:var(--text-main);">${t.username}</span>` : '<span style="color:var(--text-muted)">—</span>';
                return `
                    <tr>
                        <td>#${t.id}</td>
                        <td>${userDisplay}</td>
                        <td><span class="badge badge-chest">${t.modVersion}</span></td>
                        <td>${t.mcVersion}</td>
                        <td><span style="font-weight:600;">${t.eventType}</span></td>
                        <td style="color:var(--text-muted);">${new Date(t.createdAt).toLocaleString()}</td>
                        <td>
                            ${t.rawPayload ? `<button class="btn-outline" style="padding:4px 8px;font-size:11px;" onclick="showPayloadModal('#${t.id}', '${escapedPayload}')">Посмотреть payload</button>` : '<span style="color:var(--text-muted)">null</span>'}
                        </td>
                    </tr>
                `;
            }).join('');
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--danger);">Ошибка загрузки телеметрии</td></tr>';
        }
    }

    function showPayloadModal(id, raw) {
        document.getElementById('modalTitle').innerText = 'Payload события ' + id;
        try {
            const parsed = JSON.parse(raw);
            document.getElementById('modalBody').innerHTML = `<pre>${JSON.stringify(parsed, null, 2)}</pre>`;
        } catch (e) {
            document.getElementById('modalBody').innerHTML = `<pre>${raw}</pre>`;
        }
        document.getElementById('modalOverlay').style.display = 'flex';
    }

    function showCaptchaZoom(id, cls, conf) {
        document.getElementById('modalTitle').innerText = `Капча #${id} (${cls})`;
        document.getElementById('modalBody').innerHTML = `
            <div style="text-align:center; padding: 10px;">
                <img src="/dashboard/api/captchas/${id}/image?t=${Date.now()}" style="width: 256px; height: 256px; image-rendering: pixelated; border-radius: 8px; border: 2px solid #334155; box-shadow: 0 4px 20px rgba(0,0,0,0.5);">
                <div style="margin-top: 14px; color: var(--text-muted); font-size: 13px;">
                    Класс: <b>${cls}</b> | Уверенность: <b>${Math.round(conf * 100)}%</b>
                </div>
            </div>
        `;
        document.getElementById('modalOverlay').style.display = 'flex';
    }

    function closeModal() {
        document.getElementById('modalOverlay').style.display = 'none';
    }

    function prevCaptchaPage() { if (captchaPage > 0) loadCaptchas(captchaPage - 1); }
    function nextCaptchaPage() { if (captchaPage < captchaTotalPages - 1) loadCaptchas(captchaPage + 1); }
    function prevTelemetryPage() { if (telemetryPage > 0) loadTelemetry(telemetryPage - 1); }
    function nextTelemetryPage() { if (telemetryPage < telemetryTotalPages - 1) loadTelemetry(telemetryPage + 1); }

    function loadAllData() {
        loadStats();
        loadCaptchas(0);
        loadTelemetry(0);
    }

    document.addEventListener('DOMContentLoaded', loadAllData);
</script>
</body>
</html>
    """;
}
