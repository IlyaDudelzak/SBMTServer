# SBMTServer (SkyBlock Tweaks Telemetry & Captcha Server)

Серверная часть для сбора телеметрии и капч из мода SkyBlock Tweaks. Включает в себя REST API, хранилище на PostgreSQL и веб-дашборд мониторинга.

---

## 🚀 Быстрый локальный старт

### Требования
* JDK 21+
* Docker и Docker Compose

### Запуск в Docker:
```bash
# 1. Скопируйте шаблон переменных окружения
cp .env.example .env

# 2. Соберите и запустите контейнеры
docker compose up -d --build

# 3. Просмотр логов
docker compose logs -f app
```

После старта будут доступны:
* **Дашборд мониторинга:** [http://localhost:8080/dashboard](http://localhost:8080/dashboard) (или [http://localhost:8080/](http://localhost:8080/))
* **Swagger UI / Документация API:** [http://localhost:8080/docs](http://localhost:8080/docs)
* **Actuator Healthcheck:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🌐 Инструкция по деплою на VPS (Continuous Deployment)

Проект настроен на полностью автоматический деплой: при каждом `git push` в ветку `master`:
1. **GitHub Actions** прогоняет юнит- и интеграционные тесты с реальным PostgreSQL.
2. Собирает Docker-образ и публикует его в GitHub Container Registry (`ghcr.io/ilyadudelzak/sbmtserver:latest`).
3. Подключается по SSH к вашему VPS и накатывает свежую версию без простоя базы данных.

Ниже приведена пошаговая инструкция для первичной настройки вашего сервера.

---

### Шаг 1. Подготовка папки на VPS

Подключитесь к вашему VPS по SSH и создайте рабочую директорию проекта:

```bash
sudo mkdir -p /opt/sbmtserver
cd /opt/sbmtserver
```

---

### Шаг 2. Размещение конфигурационных файлов на VPS

В папке `/opt/sbmtserver` создайте файл `docker-compose.yml`:

```yaml
services:
  app:
    image: ghcr.io/ilyadudelzak/sbmtserver:latest
    container_name: sbmt-server
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/sbmt
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_FLYWAY_ENABLED: "false"
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:17-alpine
    container_name: sbmt-postgres
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-sbmt}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-sbmt}"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

Рядом создайте файл переменных окружения `.env` и задайте надежный пароль для базы данных:

```bash
cat << 'EOF' > /opt/sbmtserver/.env
POSTGRES_DB=sbmt
POSTGRES_USER=postgres
POSTGRES_PASSWORD=придумайте_надежный_пароль_здесь
EOF
```

---

### Шаг 3. Доступ к GitHub Container Registry (GHCR)

Если пакет в GHCR является публичным, образы будут скачиваться автоматически без авторизации.

Если ваш репозиторий/пакет **приватный**, один раз авторизуйте Docker на VPS:
1. Создайте в GitHub **Personal Access Token (classic)** с правами `read:packages`:
   * GitHub ➔ **Settings** ➔ **Developer settings** ➔ **Personal access tokens** ➔ **Generate new token**.
2. Авторизуйтесь на VPS:
   ```bash
   echo "ВАШ_ТОКЕН" | docker login ghcr.io -u ВАШ_GITHUB_ЛОГИН --password-stdin
   ```

---

### Шаг 4. Первый запуск на VPS

Запустите контейнеры, чтобы убедиться, что всё работает:

```bash
cd /opt/sbmtserver
docker compose pull
docker compose up -d
```

Проверьте статус и логи:
```bash
docker compose ps
docker compose logs -f app
```
Приложение должно быть доступно по адресу `http://IP_ВАШЕГО_СЕРВЕРА:8080/dashboard`.

---

### Шаг 5. Настройка SSH-доступа для GitHub Actions

Для того чтобы GitHub Actions мог автоматически накатывать обновления по SSH:

1. На сервере сгенерируйте отдельный ключ для деплоя:
   ```bash
   ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy -N ""
   cat ~/.ssh/github_deploy.pub >> ~/.ssh/authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```

2. Выведите на экран приватный ключ:
   ```bash
   cat ~/.ssh/github_deploy
   ```
   *(Скопируйте весь текст целиком, включая `-----BEGIN OPENSSH PRIVATE KEY-----` и `-----END OPENSSH PRIVATE KEY-----`)*.

3. В репозитории GitHub перейдите в:
   **Settings ➔ Secrets and variables ➔ Actions ➔ New repository secret**

   Добавьте следующие секреты:
   * **`VPS_HOST`**: внешний IP-адрес вашего VPS (например, `123.45.67.89`).
   * **`VPS_USER`**: пользователь для подключения (например, `root` или имя вашего пользователя с доступом к docker).
   * **`VPS_SSH_KEY`**: скопированный приватный ключ `~/.ssh/github_deploy`.
   * **`VPS_PORT`**: *(опционально, если порт не 22)*, например `22`.

---

### Шаг 6. Проверка автодеплоя

Теперь любой `git push` в ветку `master` автоматически:
1. Запустит тесты и сборку.
2. Соберет новый Docker-образ и отправит его в GHCR.
3. По SSH выполнит на сервере:
   ```bash
   cd /opt/sbmtserver
   docker compose pull app
   docker compose up -d app
   docker image prune -f
   ```
4. Ваша версия на сервере обновится за несколько секунд без простоя базы данных!

---

## 🛠 Полезные команды на VPS

```bash
# Просмотр логов приложения в реальном времени
docker compose -f /opt/sbmtserver/docker-compose.yml logs -f app

# Ручной перезапуск приложения
docker compose -f /opt/sbmtserver/docker-compose.yml restart app

# Проверка статуса контейнеров
docker compose -f /opt/sbmtserver/docker-compose.yml ps

# Очистка неиспользуемых старых образов для освобождения диска
docker image prune -f
```
