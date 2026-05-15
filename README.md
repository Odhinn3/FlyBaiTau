# FlyBaiTau

Android-приложение для планирования аэрофотосъёмочных миссий дрона DJI Mavic 4 Pro.
Генерирует KMZ-файлы в формате DJI WPML для загрузки на пульт RC2.

## Возможности

- **Grid-миссия** — автоматическая генерация маршрута по заданному полигону
  - Настройка высоты полёта, скорости, перекрытия снимков (frontlap/sidelap) и азимута профилей
  - Первая точка всегда в юго-западном углу полигона
  - Корректные азимуты съёмки для каждой полосы
  - Наклон камеры строго вертикально вниз (-90°)
  - Фотосъёмка в каждой точке маршрута
- **Waypoint-миссия** — ручная расстановка точек маршрута
- Отображение маршрута на Google Maps
- Экспорт KMZ-файла через стандартный диалог Android (Telegram, Google Drive и др.)

## Параметры камеры

Приложение настроено под основную камеру DJI Mavic 4 Pro:

| Параметр | Значение |
|---|---|
| Камера | Hasselblad 4/3" CMOS |
| Разрешение | 12288 × 8192 (100 MP) |
| Соотношение сторон | 3:2 |
| FOV | 72° |
| Фокусное расстояние (экв.) | 28 мм |

## Формат файла

Генерируется KMZ-архив в формате [DJI WPML](https://developer.dji.com/doc/cloud-api-tutorial/en/api-reference/dji-wpml/overview.html):

```
grid_mission.kmz
└── wpmz/
    ├── waylines.wpml   — маршрут с точками и действиями
    └── template.kml    — метаданные миссии
```

В каждой точке маршрута выполняются три действия последовательно:
1. `rotateYaw` — поворот дрона по азимуту съёмки
2. `gimbalRotate` — наклон камеры вертикально вниз (-90°)
3. `takePhoto` — съёмка

## Загрузка миссии на RC2

1. Подключить RC2 к компьютеру
2. Открыть Device Explorer в Android Studio
3. Перейти в `/Android/data/dji.go.v5/files/waypoint/`
4. Заменить существующий KMZ-файл на сгенерированный

## Установка и сборка

### Требования

- Android Studio
- Android SDK API 26+
- Google Maps API ключ

### Настройка API ключа

Создай файл `local.properties` в корне проекта и добавь:

```
MAPS_API_KEY=твой_ключ
```

Получить ключ можно в [Google Cloud Console](https://console.cloud.google.com/).

### Сборка

```bash
./gradlew assembleDebug
```

## Структура проекта

```
app/src/main/java/com/example/flybaitau/
├── MainActivity.java               — основной экран, карта, управление
├── model/
│   ├── Waypoint.java               — точка маршрута
│   └── CameraProfile.java          — параметры камеры
└── generator/
    ├── GridMissionHelper.java      — алгоритм генерации grid-маршрута
    └── KmzGenerator.java           — генерация KMZ/WPML файлов
```

## Совместимость

| Компонент | Версия |
|---|---|
| Дрон | DJI Mavic 4 Pro |
| Пульт | DJI RC2 |
| Приложение пульта | DJI Fly |
| Android | API 26+ (Android 8.0) |
| WPML | 1.0.2 |

## Лицензия

MIT

---

# FlyBaiTau

Android app for planning aerial survey missions for DJI Mavic 4 Pro.
Generates KMZ files in DJI WPML format for upload to the RC2 controller.

## Features

- **Grid mission** — automatic route generation over a defined polygon
  - Configurable altitude, speed, frontlap/sidelap overlap, and strip azimuth
  - First waypoint always at the southwest corner of the polygon
  - Correct shooting azimuth for each strip
  - Camera tilted straight down (-90°) at every waypoint
  - Photo taken at every waypoint
- **Waypoint mission** — manual waypoint placement
- Route preview on Google Maps
- KMZ export via standard Android share dialog (Telegram, Google Drive, etc.)

## Camera Parameters

The app is configured for the DJI Mavic 4 Pro main camera:

| Parameter | Value |
|---|---|
| Camera | Hasselblad 4/3" CMOS |
| Resolution | 12288 × 8192 (100 MP) |
| Aspect ratio | 3:2 |
| FOV | 72° |
| Focal length (equiv.) | 28 mm |

## File Format

Generates a KMZ archive in [DJI WPML](https://developer.dji.com/doc/cloud-api-tutorial/en/api-reference/dji-wpml/overview.html) format:

```
grid_mission.kmz
└── wpmz/
    ├── waylines.wpml   — route with waypoints and actions
    └── template.kml    — mission metadata
```

Three actions are executed sequentially at each waypoint:
1. `rotateYaw` — rotate drone to shooting azimuth
2. `gimbalRotate` — tilt camera straight down (-90°)
3. `takePhoto` — take photo

## Loading Mission to RC2

1. Connect RC2 to your computer
2. Open Device Explorer in Android Studio
3. Navigate to `/Android/data/dji.go.v5/files/waypoint/`
4. Replace the existing KMZ file with the generated one

## Setup and Build

### Requirements

- Android Studio
- Android SDK API 26+
- Google Maps API key

### API Key Setup

Create a `local.properties` file in the project root and add:

```
MAPS_API_KEY=your_key_here
```

Get your key at [Google Cloud Console](https://console.cloud.google.com/).

### Build

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/example/flybaitau/
├── MainActivity.java               — main screen, map, controls
├── model/
│   ├── Waypoint.java               — waypoint model
│   └── CameraProfile.java          — camera parameters
└── generator/
    ├── GridMissionHelper.java      — grid route generation algorithm
    └── KmzGenerator.java           — KMZ/WPML file generation
```

## Compatibility

| Component | Version |
|---|---|
| Drone | DJI Mavic 4 Pro |
| Controller | DJI RC2 |
| Controller app | DJI Fly |
| Android | API 26+ (Android 8.0) |
| WPML | 1.0.2 |

## License

MIT
