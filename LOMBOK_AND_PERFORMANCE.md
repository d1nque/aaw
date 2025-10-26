# Lombok та Оптимізація Продуктивності

## 🚀 Виправлення Lombok в IntelliJ IDEA

### Покрокова інструкція:

1. **Встановіть Lombok Plugin**
   - `File → Settings → Plugins → Marketplace`
   - Шукайте "Lombok" → Install → Restart IDE

2. **Увімкніть Annotation Processing**
   - `File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
   - ✅ Поставте галочку: **Enable annotation processing**

3. **Reimport Maven проекту**
   - Правий клік на `pom.xml` → `Maven → Reload Project`
   - Або натисніть 🔄 в Maven панелі (зазвичай справа)

4. **Перебудуйте проект**
   - `Build → Rebuild Project`

5. **Якщо не допоможе - Invalidate Caches**
   - `File → Invalidate Caches → Invalidate and Restart`

---

## ⚡ Оптимізація Швидкості Запуску

### Що було виправлено?

**Проблема:** Використання `javacv-platform` включало **ВСІ платформи** (Windows, Linux, macOS, Android) для всіх архітектур, що:
- Створювало 20+ попереджень про Class-Path
- Сповільнювало запуск на 50+ секунд
- Збільшувало розмір проекту на сотні MB

**Рішення:** Замінено на платформо-специфічні залежності.

### Очікуваний результат:

✅ **До змін:**
```
Started AawApplication in 53.445 seconds
20+ попереджень про Class-Path manifest
```

✅ **Після змін:**
```
Started AawApplication in 5-10 seconds
Без попереджень про Class-Path
```

---

## 🎯 Використання Maven Profiles

### Для Windows x64 (за замовчуванням):
```bash
mvn clean install
# або просто запустіть з IntelliJ IDEA
```

Профіль `windows` активний за замовчуванням і включає тільки нативні бібліотеки для Windows x64.

### Для Raspberry Pi 5 (Linux ARM64):
```bash
# Білд проекту для Raspberry Pi
mvn clean package -P raspberry-pi

# Або з пропуском тестів
mvn clean package -P raspberry-pi -DskipTests
```

**ВАЖЛИВО:** Завжди використовуйте профіль `-P raspberry-pi` при білді для Raspberry Pi, щоб включити правильні ARM64 нативні бібліотеки!

### Або в IntelliJ IDEA:
1. Відкрийте Maven панель (праворуч)
2. Розгорніть `Profiles`
3. Зніміть галочку з `windows` (якщо активна)
4. Поставте галочку на `raspberry-pi`
5. Виконайте `clean` та `package`

---

## 📝 Що означає `<optional>true</optional>`?

`<optional>true</optional>` означає, що залежність **не буде транзитивно передаватися** іншим проектам.

**Приклад:**
- Ваш проект `A` використовує Lombok з `<optional>true</optional>`
- Хтось створює проект `B`, який залежить від вашого проекту `A`
- Проект `B` **НЕ отримає** Lombok автоматично

**Чому це корисно?**
- Lombok потрібен тільки під час компіляції
- DevTools та інші інструменти розробки не повинні передаватися в залежні проекти
- Зменшує розмір фінального артефакту

---

## ⚠️ Troubleshooting

### Lombok не працює після всіх кроків?

1. Перевірте версію IntelliJ IDEA (2023.3+)
2. Перевірте версію Java (21+)
3. Видаліть папку `.idea` та reimport проект
4. Очистіть Maven кеш:
   ```bash
   mvn clean
   rm -rf ~/.m2/repository/org/projectlombok
   mvn install
   ```

### Все ще повільно запускається?

1. Перевірте чи Maven завантажив нові залежності:
   ```bash
   mvn dependency:tree
   ```
2. Переконайтесь що `javacv-platform` більше не використовується
3. Видаліть папку `target`:
   ```bash
   mvn clean
   ```

### Помилка `UnsatisfiedLinkError` при роботі з OpenCV?

**Проблема:** `java.lang.UnsatisfiedLinkError: 'long org.opencv.core.Mat.n_Mat()'`

**Рішення:** Нативні бібліотеки OpenCV завантажуються через `nu.pattern.OpenCV.loadLocally()` в `CameraServiceImpl`. Переконайтесь що:
1. Залежність `org.openpnp:opencv` присутня в `pom.xml`
2. Ви НЕ запускаєте на платформі, для якої немає бінарників (наприклад, macOS ARM якщо немає відповідної залежності)

### Багато повідомлень "Did not match" при запуску тестів?

**Це НОРМАЛЬНО!** 🎉

Повідомлення типу:
```
ReactiveOAuth2ResourceServerAutoConfiguration:
   Did not match:
      - @ConditionalOnClass did not find required class
```

Це **НЕ помилки**, а інформація від Spring Boot про те, які автоконфігурації НЕ активовані. Spring Boot перевіряє сотні можливих конфігурацій (Web, Security, Database, etc.) і показує які з них не підходять для вашого проєкту.

**Ігноруйте ці повідомлення** - вони з'являються тільки при запуску з `--debug` або під час тестів.

### Тести падають з помилкою створення bean'ів?

Тести використовують профіль `test` з файлу `src/test/resources/application-test.yml`, де всі апаратні функції вимкнені. Це дозволяє тестам працювати без доступу до камери, GPIO, LRF тощо.

---

## 🥧 Білд та Деплой на Raspberry Pi 5

### Структура Maven Profiles:

**Основні залежності** (в `<dependencies>`):
- `javacv` - Core бібліотека (без нативних бінарників)
- `opencv` - Core бібліотека (без нативних бінарників)
- `ffmpeg` - Core бібліотека (без нативних бінарників)
- `openpnp:opencv` - Для автоматичного завантаження нативних бібліотек

**Profile `windows` (активний за замовчуванням):**
- `opencv:windows-x86_64` - Нативні бібліотеки OpenCV для Windows
- `ffmpeg:windows-x86_64` - Нативні бібліотеки FFmpeg для Windows

**Profile `raspberry-pi`:**
- `opencv:linux-arm64` - Нативні бібліотеки OpenCV для Linux ARM64
- `ffmpeg:linux-arm64` - Нативні бібліотеки FFmpeg для Linux ARM64
- `javacpp:linux-arm64` - JavaCPP для Linux ARM64

### Правильний білд для Raspberry Pi:

```bash
# На вашій Windows машині
mvn clean package -P raspberry-pi -DskipTests

# Або з тестами (якщо потрібно)
mvn clean package -P raspberry-pi
```

**КРИТИЧНО ВАЖЛИВО:** 
- **НЕ** використовуйте білд без профіля `-P raspberry-pi` для Raspberry Pi!
- Без профіля завантажаться Windows бібліотеки, які не працюватимуть на ARM64

### Перевірка що профіль активний:

```bash
mvn help:active-profiles -P raspberry-pi
```

Має вивести:
```
Active Profiles for Project 'vyrib1.project:aaw:jar:0.0.1-SNAPSHOT':
The following profiles are active:
 - raspberry-pi (source: vyrib1.project:aaw:0.0.1-SNAPSHOT)
```

### Перевірка залежностей:

```bash
# Перевірити які native бібліотеки включені
mvn dependency:tree -P raspberry-pi | grep -i "arm64\|linux"
```

Має показати `linux-arm64` бібліотеки, а НЕ `windows-x86_64`.

