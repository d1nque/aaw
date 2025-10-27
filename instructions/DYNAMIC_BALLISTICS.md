# Динамічні балістичні розрахунки

## 🎯 Огляд

Система балістичного упередження тепер підтримує **динамічні розрахунки** всіх балістичних параметрів в реальному часі на основі даних з датчиків.

## 📊 Що розраховується динамічно

### 1. **FLIGHT_TIME** (Час польоту кулі)
```java
FLIGHT_TIME = calculateFlightTime(range, muzzleVelocity)
```

**Формула**:
```
TOF = RANGE / averageVelocity
```

де `averageVelocity` враховує втрату швидкості через опір повітря (~5% на 100 м)

**Приклад**:
- RANGE = 150 м
- MUZZLE_VELOCITY = 940 м/с (5.56×45mm NATO)
- FLIGHT_TIME ≈ 0.164 с

---

### 2. **DROP** (Падіння кулі)
```java
DROP = calculateDrop(flightTime)
```

**Формула**:
```
DROP = 0.5 × g × t²
```

де:
- g = 9.81 м/с² (прискорення вільного падіння)
- t = час польоту (FLIGHT_TIME)

**Приклад**:
- FLIGHT_TIME = 0.164 с
- DROP = 0.5 × 9.81 × (0.164)² ≈ 0.132 м

---

### 3. **COME_UP_MRAD** (Вертикальна поправка)
```java
COME_UP_MRAD = calculateComeUpMrad(drop, range)
```

**Формула**:
```
COME_UP_MRAD = (DROP / RANGE) × 1000
```

**Приклад**:
- DROP = 0.132 м
- RANGE = 150 м
- COME_UP_MRAD = (0.132 / 150) × 1000 ≈ 0.88 mrad

---

### 4. **LEAD_MRAD** (Горизонтальна поправка)
```java
LEAD_MRAD = calculateLeadMrad(targetSpeed, flightTime, range)
```

**Формула**:
```
LEAD_MRAD = (TARGET_SPEED × FLIGHT_TIME / RANGE) × 1000
```

**Приклад**:
- TARGET_SPEED = 388.9 м/с (1400 км/год)
- FLIGHT_TIME = 0.164 с
- RANGE = 150 м
- Зміщення цілі = 388.9 × 0.164 = 63.78 м
- LEAD_MRAD = (63.78 / 150) × 1000 ≈ 425 mrad

---

## 🔄 Потік обчислень

```
1. GuiServiceImpl отримує відстань з LRF
   ↓
2. Передає в BallisticCalculatorService.calculateAimPoint(range)
   ↓
3. BallisticCalculatorService викликає:
   a. calculateFlightTime(range, muzzleVelocity)
   b. calculateDrop(flightTime)
   c. calculateComeUpMrad(drop, range)
   d. calculateLeadMrad(targetSpeed, flightTime, range)
   ↓
4. calculateBallisticCorrections() повертає (comeUpMrad, leadMrad)
   ↓
5. calculatePixelOffsets() конвертує мілірадіани → пікселі
   ↓
6. Повертає координати жовтого кола
```

---

## 📝 API методів

### calculateFlightTime()
```java
double calculateFlightTime(double range, double muzzleVelocity)
```
Розраховує час польоту кулі з урахуванням втрати швидкості.

### calculateDrop()
```java
double calculateDrop(double flightTime)
```
Розраховує падіння кулі під дією гравітації.

### calculateComeUpMrad()
```java
double calculateComeUpMrad(double drop, double range)
```
Конвертує падіння в вертикальну поправку (мілірадіани).

### calculateLeadMrad()
```java
double calculateLeadMrad(double targetSpeed, double flightTime, double range)
```
Розраховує горизонтальну поправку для упередження рухомої цілі.

### calculateBallisticCorrections()
```java
Point calculateBallisticCorrections(double range, double muzzleVelocity, double targetSpeed)
```
**Головний метод** - розраховує всі поправки одним викликом.

**Повертає**: `Point(comeUpMrad, leadMrad)`

### calculateAimPoint() - перевантажені версії

#### Версія 1: З константами
```java
Point calculateAimPoint(int crosshairX, int crosshairY, 
                       int frameWidth, int frameHeight)
```
Використовує `BallisticConstants.TARGET_DISTANCE`

#### Версія 2: З реальною відстанню
```java
Point calculateAimPoint(int crosshairX, int crosshairY, 
                       int frameWidth, int frameHeight, 
                       double range)
```
Використовує реальну відстань з LRF датчика.

**Валідація відстані**: 
- Якщо `range <= 0` або `range > 3000` → використовується константа
- Інакше → використовується реальне значення

---

## 🚀 Використання в коді

### GuiServiceImpl - автоматичний вибір джерела даних

```java
// Отримати відстань з LRF
double rangeFromLrf = lrfService.getDistanceMeters();

// Розрахувати точку упередження
Point aimPoint;
if (rangeFromLrf > 0 && rangeFromLrf < 3000) {
    // Використовувати реальні дані з LRF
    aimPoint = ballisticCalculatorService.calculateAimPoint(
        x, y, frameWidth, frameHeight, rangeFromLrf
    );
} else {
    // Використовувати константу
    aimPoint = ballisticCalculatorService.calculateAimPoint(
        x, y, frameWidth, frameHeight
    );
}
```

### Ручний розрахунок параметрів

```java
// Приклад 1: Розрахунок для конкретної відстані
double range = 200.0; // метрів
double muzzleVelocity = 940.0; // м/с (5.56mm NATO)
double targetSpeed = 100.0; // м/с

Point corrections = ballisticCalculatorService.calculateBallisticCorrections(
    range, muzzleVelocity, targetSpeed
);

double comeUpMrad = corrections.x;
double leadMrad = corrections.y;

System.out.println("Come-up: " + comeUpMrad + " mrad");
System.out.println("Lead: " + leadMrad + " mrad");
```

```java
// Приклад 2: Покроковий розрахунок
double flightTime = ballisticCalculatorService.calculateFlightTime(150, 940);
double drop = ballisticCalculatorService.calculateDrop(flightTime);
double comeUp = ballisticCalculatorService.calculateComeUpMrad(drop, 150);

System.out.println("Flight time: " + flightTime + " s");
System.out.println("Drop: " + drop + " m");
System.out.println("Come-up: " + comeUp + " mrad");
```

---

## ⚙️ Налаштування константв

Відкрийте `BallisticConstants.java`:

### Параметри боєприпасів

```java
// Початкова швидкість кулі
MUZZLE_VELOCITY_MPS = 940.0;  // 5.56×45mm NATO

// Інші варіанти:
// 730.0  - 7.62×39mm (AK-47)
// 820.0  - 7.62×51mm NATO (.308)
// 900.0  - .338 Lapua Magnum
```

### Параметри цілі

```java
// Відстань за замовчуванням (якщо LRF не працює)
TARGET_DISTANCE = 150.0;  // метрів

// Швидкість цілі (для розрахунку упередження)
TARGET_SPEED_KMH = 1400.0;  // км/год
TARGET_SPEED_MPS = TARGET_SPEED_KMH / 3.6;  // автоматична конвертація
```

### Фізичні константи

```java
GRAVITY = 9.81;  // м/с²
RHO = 1.225;     // кг/м³ (густина повітря)
DT = 0.001;      // с (крок інтегрування)
```

---

## 📈 Приклади розрахунків

### Сценарій 1: Ближня дистанція (100 м)

```
Вхідні дані:
- RANGE = 100 м
- MUZZLE_VELOCITY = 940 м/с
- TARGET_SPEED = 388.9 м/с

Розрахунок:
1. FLIGHT_TIME = 100 / 940 ≈ 0.106 с
2. DROP = 0.5 × 9.81 × (0.106)² ≈ 0.055 м
3. COME_UP = (0.055 / 100) × 1000 ≈ 0.55 mrad
4. LEAD = (388.9 × 0.106 / 100) × 1000 ≈ 412 mrad
```

### Сценарій 2: Середня дистанція (150 м)

```
Вхідні дані:
- RANGE = 150 м
- MUZZLE_VELOCITY = 940 м/с
- TARGET_SPEED = 388.9 м/с

Розрахунок:
1. FLIGHT_TIME = 150 / 940 ≈ 0.160 с
2. DROP = 0.5 × 9.81 × (0.160)² ≈ 0.125 м
3. COME_UP = (0.125 / 150) × 1000 ≈ 0.83 mrad
4. LEAD = (388.9 × 0.160 / 150) × 1000 ≈ 415 mrad
```

### Сценарій 3: Дальня дистанція (300 м)

```
Вхідні дані:
- RANGE = 300 м
- MUZZLE_VELOCITY = 940 м/с
- TARGET_SPEED = 388.9 м/с

Розрахунок:
1. FLIGHT_TIME = 300 / 940 ≈ 0.319 с (з втратою швидкості)
2. DROP = 0.5 × 9.81 × (0.319)² ≈ 0.500 м
3. COME_UP = (0.500 / 300) × 1000 ≈ 1.67 mrad
4. LEAD = (388.9 × 0.319 / 300) × 1000 ≈ 414 mrad
```

---

## 🔍 Режими роботи

### Режим 1: Константи (тестування)
- LRF вимкнено або повертає -1
- Використовується `BallisticConstants.TARGET_DISTANCE`
- Ідеально для тестування та налагодження

### Режим 2: Динамічний (бойовий)
- LRF активний та повертає валідну відстань
- Система автоматично перераховує всі параметри
- Точність залежить від якості даних з LRF

---

## 🎓 Математичне обґрунтування

### Чому DROP = 0.5 × g × t²?

Це класична формула вільного падіння з фізики:
```
h(t) = h₀ + v₀t - ½gt²
```

Якщо початкова висота h₀ = 0 і початкова вертикальна швидкість v₀ = 0 (горизонтальний постріл), то:
```
h(t) = -½gt²
```

Знак мінус означає падіння вниз, тому DROP = |h(t)| = ½gt²

### Чому враховується втрата швидкості?

Куля втрачає швидкість через опір повітря:
```
F_drag = ½ × ρ × Cd × A × v²
```

Спрощено моделюємо як ~5% втрати на 100 метрів:
```
v(x) ≈ v₀ × (1 - 0.05 × x/100)
```

Для більшої точності потрібно використовувати чисельне інтегрування (метод Ейлера) з урахуванням BC (Ballistic Coefficient).

---

## ✅ Переваги динамічних розрахунків

1. **Точність**: Розрахунки адаптуються до реальної відстані
2. **Гнучкість**: Легко змінювати параметри патрону
3. **Масштабованість**: Можна додати врахування вітру, температури, тиску
4. **Тестовність**: Кожен метод можна тестувати окремо
5. **Зрозумілість**: Чіткий поділ на етапи розрахунку

---

## 🚀 Майбутні покращення

### Фаза 1 (поточна): ✅
- Динамічний розрахунок DROP, FLIGHT_TIME, COME_UP, LEAD
- Інтеграція з LRF для отримання відстані

### Фаза 2 (планується):
- Врахування кута нахилу (з акселерометра)
- Врахування вітру (швидкість та напрямок)
- Залежність від температури та тиску повітря

### Фаза 3 (планується):
- Чисельне інтегрування траєкторії (метод Ейлера)
- База даних різних типів патронів
- Автоматичний вибір патрону на основі зброї

### Фаза 4 (планується):
- Трекінг цілі для визначення швидкості
- Машинне навчання для передбачення руху цілі
- Адаптивне налаштування на основі результатів пострілів

---

## 📚 Додаткові ресурси

- `BallisticConstants.java` - всі константи з детальним описом
- `BALLISTIC_INTEGRATION.md` - повна документація системи
- `BALLISTIC_LEAD_QUICK_START.md` - швидкий старт

---

**Версія**: 1.1  
**Дата оновлення**: Жовтень 2025  
**Статус**: ✅ Implemented & Tested

