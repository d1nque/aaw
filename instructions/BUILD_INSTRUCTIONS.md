# Інструкції по білду проекту AAW

## 🖥️ Білд для Windows (розробка)

**Автоматично використовується профіль `windows`:**

```bash
# Білд з тестами
mvn clean package

# Білд без тестів
mvn clean package -DskipTests

# Запуск
java -jar target/aaw-0.0.1-SNAPSHOT.jar
```

---

## 🥧 Білд для Raspberry Pi 5

**ОБОВ'ЯЗКОВО використовуйте профіль `-P raspberry-pi`:**

```bash
# Білд з тестами
mvn clean package -P raspberry-pi

# Білд без тестів (рекомендовано для швидкого деплою)
mvn clean package -P raspberry-pi -DskipTests
```

### Деплой на Raspberry Pi:

```bash
# 1. Білд на Windows машині
mvn clean package -P raspberry-pi -DskipTests

# 2. Копіювання на Raspberry Pi (замініть IP адресу)
scp target/aaw-0.0.1-SNAPSHOT.jar pi@192.168.1.100:~/aaw/

# 3. Запуск на Raspberry Pi (через SSH)
ssh pi@192.168.1.100
cd ~/aaw
java -jar aaw-0.0.1-SNAPSHOT.jar
```

---

## ⚠️ ВАЖЛИВО

### ❌ НЕ робіть так:

```bash
# БЕЗ профіля - включить Windows бібліотеки!
mvn clean package

# Потім запуск на Raspberry Pi - НЕ ПРАЦЮВАТИМЕ!
java -jar aaw-0.0.1-SNAPSHOT.jar
```

### ✅ Правильно:

```bash
# З профілем raspberry-pi - включить ARM64 бібліотеки
mvn clean package -P raspberry-pi -DskipTests

# Запуск на Raspberry Pi - ПРАЦЮЄ!
java -jar aaw-0.0.1-SNAPSHOT.jar
```

---

## 🔍 Перевірка конфігурації

### Перевірити активні профілі:

```bash
# Для Windows
mvn help:active-profiles

# Для Raspberry Pi
mvn help:active-profiles -P raspberry-pi
```

### Перевірити які native бібліотеки включені:

```bash
# Windows (має показати windows-x86_64)
mvn dependency:tree | findstr "windows-x86_64"

# Raspberry Pi (має показати linux-arm64)
mvn dependency:tree -P raspberry-pi | findstr "linux-arm64"
```

---

## 📦 Структура Maven Profiles

**За замовчуванням (без `-P`):**
- Core бібліотеки (платформо-незалежні)
- **Windows x64 native** бібліотеки (профіль `windows` активний за замовчуванням)

**З `-P raspberry-pi`:**
- Core бібліотеки (платформо-незалежні)
- **Linux ARM64 native** бібліотеки (профіль `raspberry-pi`)

---

## 🚀 Швидкий старт

### Windows розробка:
```bash
mvn clean package && java -jar target/aaw-0.0.1-SNAPSHOT.jar
```

### Raspberry Pi деплой:
```bash
# Білд
mvn clean package -P raspberry-pi -DskipTests

# Копіювання та запуск
scp target/aaw-0.0.1-SNAPSHOT.jar pi@192.168.1.100:~/aaw/ && \
ssh pi@192.168.1.100 "cd ~/aaw && java -jar aaw-0.0.1-SNAPSHOT.jar"
```

