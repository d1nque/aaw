# AAW - Advanced Aiming Workspace

Spring Boot додаток для відображення відео з камери з накладанням перехрестя, дистанції та кута для Raspberry Pi 5.

## Основні можливості

- 📹 Відображення відео в реальному часі з камери
- 🎯 Накладання перехрестя з можливістю налаштування позиції
- 📏 Відображення дистанції та кута з лазерного далекоміра (LRF)
- 🎮 Підтримка GPIO кнопок для управління
- 🖥️ **Повноекранний режим** (нова можливість!)
- 🔧 Розширюваний GUI на базі Java Swing

## Технології

- **Java 17+**
- **Spring Boot** - фреймворк додатку
- **OpenCV** - обробка відео та зображень
- **Java Swing** - графічний інтерфейс (замість обмеженого HighGui)
- **GPIO (Pi4J)** - підтримка апаратних кнопок

## Запуск на ноутбуці (розробка)

```bash
# Компіляція
mvn clean package

# Запуск
mvn spring-boot:run
```

## Запуск на Raspberry Pi 5

### Важливо!

На Raspberry Pi 5 можуть виникати проблеми з відображенням GUI (білий екран). Детальні інструкції по вирішенню цієї проблеми див. у [RASPBERRY_PI_SETUP.md](RASPBERRY_PI_SETUP.md).

### Швидкий старт

1. Перейдіть з Wayland на X11:
   ```bash
   sudo raspi-config
   # 6 Advanced Options -> A6 Wayland -> Виберіть X11
   sudo reboot
   ```

2. Скомпілюйте проєкт:
   ```bash
   mvn clean package
   ```

3. Запустіть через скрипт:
   ```bash
   chmod +x start-raspberry-pi.sh
   ./start-raspberry-pi.sh
   ```

Альтернативно, запуск з JVM параметрами вручну:
```bash
java -Dsun.java2d.opengl=false \
     -Dsun.java2d.xrender=false \
     -Dawt.useSystemAAFontSettings=on \
     -jar target/aaw-*.jar
```

## Конфігурація

Налаштування додатку знаходяться в `src/main/resources/application.yml`:

```yaml
features:
  lrf:
    enabled: true  # Увімкнути лазерний далекомір
  gpio-buttons:
    enabled: true  # Увімкнути GPIO кнопки
  camera:
    enabled: true  # Увімкнути камеру
```

## Управління

### GPIO кнопки (якщо увімкнено)

- **↑** - Зсув перехрестя вгору
- **↓** - Зсув перехрестя вниз
- **←** - Зсув перехрестя вліво
- **→** - Зсув перехрестя вправо
- **Центральна кнопка** - Спеціальна дія

Позиція перехрестя зберігається в файлах `x.txt` та `y.txt`.

## Структура проєкту

```
src/main/java/vyrib1/project/aaw/
├── AawApplication.java              # Головний клас
├── config/
│   └── FeatureConfig.java           # Конфігурація можливостей
├── data/
│   ├── Constants.java               # Константи
│   └── domain/
│       └── GpioButtons.java         # GPIO кнопки
└── services/
    ├── CameraService.java           # Інтерфейс камери
    ├── GuiService.java              # Інтерфейс GUI
    ├── LrfService.java              # Інтерфейс LRF
    └── impl/
        ├── CameraServiceImpl.java   # Реалізація камери
        ├── GuiServiceImpl.java      # Головна логіка GUI
        ├── SwingGuiServiceImpl.java # Swing GUI компонент
        └── LrfServiceImpl.java      # Реалізація LRF
```

## Переваги нового Swing GUI

Замість обмеженого OpenCV HighGui, який не підтримує повноекранний режим та має мало функцій на Java, тепер використовується повноцінний Swing:

| Можливість | HighGui | Swing GUI |
|------------|---------|-----------|
| Повноекранний режим | ❌ | ✅ |
| Додавання кнопок | ❌ | ✅ |
| Додавання слайдерів | ❌ | ✅ |
| Стабільна робота на RPi 5 | ⚠️ | ✅ |
| Розширюваність | ❌ | ✅ |

## Розробка

### Додавання нових UI елементів

Swing GUI легко розширювати. Приклад додавання кнопки в `SwingGuiServiceImpl.java`:

```java
JButton myButton = new JButton("Моя кнопка");
myButton.addActionListener(e -> {
    System.out.println("Кнопка натиснута!");
});
frame.add(myButton, BorderLayout.SOUTH);
```

### Налагодження

Для детального логування додайте в `application.yml`:

```yaml
logging:
  level:
    vyrib1.project.aaw: DEBUG
```

## Усунення несправностей

- **Білий екран на Raspberry Pi 5** - див. [RASPBERRY_PI_SETUP.md](RASPBERRY_PI_SETUP.md)
- **Камера не працює** - перевірте підключення та доступ до пристрою
- **GPIO кнопки не працюють** - переконайтеся, що Pi4J правильно встановлено

## Ліцензія

Проєкт для внутрішнього використання.

## Автор

vyrib1

