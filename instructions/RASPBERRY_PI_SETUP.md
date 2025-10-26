# Налаштування для Raspberry Pi 5

## Проблема з білим екраном на Raspberry Pi 5

Якщо ви стикаєтеся з білим екраном при запуску Java GUI додатків на Raspberry Pi 5, це може бути пов'язано з:
- Використанням графічного сервера Wayland (за замовчуванням в Raspberry Pi OS Bookworm)
- Проблемами з апаратним рендерингом графіки
- Несумісністю JavaFX/Swing з новим обладнанням

## Рішення 1: Перехід на X11 (Рекомендовано)

Raspberry Pi OS Bookworm за замовчуванням використовує Wayland. Перехід на X11 вирішує більшість проблем з GUI:

```bash
# Відкрийте утиліту конфігурації
sudo raspi-config

# Виберіть:
# 6 Advanced Options
# A6 Wayland
# Виберіть "X11"

# Перезавантажте систему
sudo reboot
```

## Рішення 2: JVM параметри для програмного рендерингу

Якщо проблема залишається після переходу на X11, додайте наступні JVM параметри при запуску додатку:

### Для запуску через Maven

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dsun.java2d.opengl=false -Dsun.java2d.xrender=false -Dawt.useSystemAAFontSettings=on"
```

### Для запуску JAR файлу

```bash
java -Dsun.java2d.opengl=false \
     -Dsun.java2d.xrender=false \
     -Dawt.useSystemAAFontSettings=on \
     -jar target/aaw-*.jar
```

### Створення скрипту запуску

Створіть файл `start-raspberry-pi.sh`:

```bash
#!/bin/bash

# JVM параметри для стабільної роботи на Raspberry Pi 5
JVM_OPTS="-Dsun.java2d.opengl=false"
JVM_OPTS="$JVM_OPTS -Dsun.java2d.xrender=false"
JVM_OPTS="$JVM_OPTS -Dawt.useSystemAAFontSettings=on"
JVM_OPTS="$JVM_OPTS -Dprism.order=sw"

# Запуск додатку
java $JVM_OPTS -jar target/aaw-*.jar
```

Зробіть скрипт виконуваним:
```bash
chmod +x start-raspberry-pi.sh
```

Запустіть:
```bash
./start-raspberry-pi.sh
```

## Пояснення JVM параметрів

- `-Dsun.java2d.opengl=false` - вимикає OpenGL для Java 2D, використовує програмний рендеринг
- `-Dsun.java2d.xrender=false` - вимикає XRender для Java 2D
- `-Dawt.useSystemAAFontSettings=on` - покращує відображення шрифтів
- `-Dprism.order=sw` - примушує JavaFX використовувати програмний рендеринг (якщо використовується JavaFX)

## Оновлення системи

Перед налаштуванням рекомендується оновити систему:

```bash
sudo apt update
sudo apt upgrade -y
```

## Перевірка встановленої Java

```bash
java -version
```

Рекомендована версія: OpenJDK 17 або новіша.

## Налаштування Maven POM (опційно)

Додайте наступну конфігурацію до `pom.xml` для автоматичного додавання JVM параметрів:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <jvmArguments>
                    -Dsun.java2d.opengl=false
                    -Dsun.java2d.xrender=false
                    -Dawt.useSystemAAFontSettings=on
                </jvmArguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Переваги нового Swing GUI

Новий GUI на базі Swing має наступні переваги:

- ✅ **Повноекранний режим** - справжній fullscreen замість обмеженого HighGui
- ✅ **Стабільна робота на Raspberry Pi 5** - з X11 та правильними JVM параметрами
- ✅ **Розширюваність** - легко додавати кнопки, слайдери, меню тощо
- ✅ **Кросплатформність** - працює на Windows, Linux, macOS
- ✅ **Програмний рендеринг** - уникає проблем з апаратним прискоренням

## Усунення несправностей

### Білий екран все ще з'являється

1. Перевірте, чи використовується X11:
   ```bash
   echo $XDG_SESSION_TYPE
   ```
   Має показувати "x11"

2. Перевірте змінну DISPLAY:
   ```bash
   echo $DISPLAY
   ```
   Має бути встановлена (наприклад, `:0`)

3. Перевірте, чи запущений X-сервер:
   ```bash
   ps aux | grep X
   ```

### Повільне відображення

- Зменшіть розмір вікна або частоту кадрів в коді
- Переконайтеся, що використовується програмний рендеринг (JVM параметри вище)

### Помилки з DISPLAY

Якщо ви запускаєте через SSH:
```bash
export DISPLAY=:0
```

Або запускайте безпосередньо з графічного середовища Raspberry Pi.

## Контакти

Якщо проблеми залишаються, перевірте логи додатку для детальнішої інформації про помилки.

