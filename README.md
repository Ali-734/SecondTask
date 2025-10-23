# FileShare - Сервис обмена файлами

Простой веб-сервис для загрузки и скачивания файлов, написанный на Java с токен-авторизацией.

## Возможности

- **Загрузка файлов** через веб-интерфейс с поддержкой всех типов файлов
- **Скачивание файлов** по уникальным ссылкам с правильными именами
- **Токен-авторизация** - доступ только для авторизованных пользователей
- **Управление файлами**: просмотр списка, удаление через веб-интерфейс (только для авторизованных)
- **Статистика использования**: базовая и детальная статистика по файлам (только для авторизованных)
- **Сортировка файлов** по различным критериям (имя, размер, дата, скачивания)
- **Автоматическая очистка** старых файлов по истечении срока жизни
- **REST API** для интеграции с другими приложениями
- **Адаптивный дизайн** для работы на мобильных устройствах
- **Безопасность**: файлы и статистика скрыты от неавторизованных пользователей

## Требования

- **Java 17 или выше**
- **Maven 3.6 или выше**
- Windows/Linux/macOS

## Быстрый запуск

### Одна команда для запуска

```bash
mvn clean compile exec:java
```

**Или если Maven не в PATH:**
```bash
"C:\Program Files\maven\apache-maven-3.9.11\bin\mvn.cmd" clean compile exec:java
```

Эта команда:
1. Очищает предыдущую сборку
2. Компилирует Java код
3. Копирует веб-ресурсы
4. Запускает приложение



Приложение запустится на порту 8080. Вы увидите сообщение:
```
FileShare server started on port 8080
Data directory: C:\Users\farii\IdeaProjects\SecondTask\data
Upload auth: enabled (token-based)
Token expiration: 24 hours
```

### 3. Использование

Откройте браузер и перейдите по адресу: http://localhost:8080

**Для неавторизованных пользователей:**
- **Главная страница**: Форма авторизации
- **Доступ**: Только авторизация

**Для авторизованных пользователей:**
- **Главная страница**: Загрузка файлов через веб-форму
- **API статистики**: http://localhost:8080/api/stats (базовая) - требует авторизации
- **Детальная статистика**: http://localhost:8080/api/file-stats - требует авторизации
- **Список файлов**: http://localhost:8080/api/files - требует авторизации
- **Скачивание файлов**: http://localhost:8080/d/{token} - доступно всем (по прямой ссылке)

## Настройка через переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `PORT` | 8080 | Порт для веб-сервера |
| `DATA_DIR` | data | Директория для хранения файлов |
| `AUTH_ENABLED` | true | Включить токен-авторизацию |
| `TOKEN_EXPIRATION_HOURS` | 24 | Время жизни токена в часах |
| `DAYS_TO_LIVE` | 30 | Количество дней хранения файлов |

### Пример запуска с настройками

```bash
# Windows
set PORT=9000
set DATA_DIR=myfiles
set AUTH_ENABLED=true
set TOKEN_EXPIRATION_HOURS=48
set DAYS_TO_LIVE=7
mvn clean compile exec:java

# Linux/macOS
PORT=9000 DATA_DIR=myfiles AUTH_ENABLED=true TOKEN_EXPIRATION_HOURS=48 DAYS_TO_LIVE=7 mvn clean compile exec:java
```

## API

### Загрузка файла
```
POST /api/upload
Content-Type: multipart/form-data

Параметры:
- file: файл для загрузки
- Authorization: Bearer {token} 

Ответ:
{
  "token": "уникальный_токен_файла",
  "url": "http://localhost:8080/d/уникальный_токен_файла"
}
```

### Авторизация
```
POST /api/auth
Content-Type: application/json

Тело запроса:
{
  "username": "имя_пользователя"
}

Ответ:
{
  "token": "токен_доступа",
  "username": "имя_пользователя"
}
```

### Базовая статистика (требует авторизации)
```
GET /api/stats
Authorization: Bearer {token}

Ответ:
{
  "totalFiles": 5,
  "totalBytes": 1048576,
  "totalDownloads": 12
}
```

### Детальная статистика (требует авторизации)
```
GET /api/file-stats
Authorization: Bearer {token}

Ответ:
{
  "totalFiles": 5,
  "totalSize": 1048576,
  "totalDownloads": 12,
  "sizeStats": {
    "max": 524288,
    "min": 1024,
    "median": 262144,
    "average": 209715
  },
  "downloadStats": {
    "max": 5,
    "min": 0,
    "median": 2,
    "average": 2.4
  },
  "timeStats": {
    "oldest": 1699123456789,
    "newest": 1699209856789,
    "medianAge": 43200
  },
  "formatStats": [
    {"format": "pdf", "count": 2, "size": 524288},
    {"format": "docx", "count": 3, "size": 524288}
  ]
}
```

### Список файлов (требует авторизации)
```
GET /api/files
Authorization: Bearer {token}

Ответ:
{
  "files": [
    {
      "token": "abc123",
      "name": "document.pdf",
      "size": 262144,
      "downloads": 3,
      "created": 1699123456789,
      "lastDownloaded": 1699209856789
    }
  ]
}
```

### Удаление файла
```
DELETE /api/delete/{token}

Ответ:
{
  "success": true
}
```

### Скачивание файла
```
GET /d/{token}

Ответ: файл с оригинальным именем
```

## Структура проекта

```
src/
├── main/
│   ├── java/com/fileshare/
│   │   ├── FileShareApplication.java    # Главный класс приложения
│   │   ├── core/                        # Основные компоненты
│   │   │   ├── Storage.java             # Управление файлами
│   │   │   └── Auth.java                # Аутентификация
│   │   ├── handlers/                     # HTTP обработчики
│   │   │   ├── AuthHandler.java         # Авторизация
│   │   │   ├── FileUploadHandler.java   # Загрузка файлов
│   │   │   ├── FileDownloadHandler.java # Скачивание файлов
│   │   │   ├── StaticFileHandler.java   # Статические файлы
│   │   │   ├── StatisticsHandler.java   # Базовая статистика
│   │   │   ├── DetailedStatisticsHandler.java # Детальная статистика
│   │   │   ├── FileListHandler.java     # Список файлов
│   │   │   └── FileDeleteHandler.java  # Удаление файлов
│   │   ├── services/                     # Сервисы
│   │   │   └── CleanupService.java      # Очистка старых файлов
│   │   └── utils/                       # Утилиты
│   │       ├── Environment.java         # Переменные окружения
│   │       ├── MimeTypeDetector.java    # Определение MIME типов
│   │       ├── HttpUtils.java           # HTTP утилиты
│   │       ├── JsonUtils.java           # JSON утилиты
│   │       └── PathUtils.java           # Утилиты для путей
│   └── resources/public/
│       ├── index.html                   # Веб-интерфейс
│       ├── script.js                    # JavaScript
│       └── styles.css                  # Стили
```

## Остановка приложения

Нажмите `Ctrl+C` в терминале или выполните:
```bash
# Windows
taskkill /f /im java.exe

# Linux/macOS
pkill -f com.fileshare.FileShareApplication
```

## Проверка работоспособности

1. Запустите приложение: `mvn clean compile exec:java`
2. Откройте http://localhost:8080 в браузере
3. Загрузите тестовый файл через веб-форму
4. Скопируйте ссылку для скачивания
5. Откройте ссылку в новом окне браузера
6. Проверьте статистику:
   - Базовая: http://localhost:8080/api/stats
   - Детальная: http://localhost:8080/api/file-stats
   - Список файлов: http://localhost:8080/api/files
7. Попробуйте удалить файл через веб-интерфейс

## Устранение неполадок

- **Порт занят**: Измените порт через переменную `PORT`
- **Ошибка компиляции**: Убедитесь, что используется Java 17+ и Maven 3.6+
- **Файлы не сохраняются**: Проверьте права доступа к директории `DATA_DIR`
- **Maven не найден**: Установите Maven и добавьте в PATH