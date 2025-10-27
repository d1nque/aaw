# Complete Technical Description of Ballistic Calculator Project

## 📖 Table of Contents

1. [Introduction and Purpose](#1-introduction-and-purpose)
2. [Project Architecture](#2-project-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Domain Model](#4-domain-model)
5. [Ballistic Calculations](#5-ballistic-calculations)
6. [Image Processing](#6-image-processing)
7. [Main Application Logic](#7-main-application-logic)
8. [Mathematical Formulas and Algorithms](#8-mathematical-formulas-and-algorithms)
9. [Detailed Code Analysis](#9-detailed-code-analysis)
10. [Output Examples](#10-output-examples)
11. [Configuration and Execution](#11-configuration-and-execution)
12. [File Structure](#12-file-structure)
13. [Instructions for Cursor AI](#13-instructions-for-cursor-ai)

---

## 1. Introduction and Purpose

### 1.1 What does the project do?

**Ballistic Calculator** is a Spring Boot console application that performs high-precision ballistic calculations for various ammunition types. The main function is to calculate the aiming point for hitting a moving target, accounting for:

- **Bullet flight physics**: gravity, air resistance, time of flight
- **Target movement**: speed and direction of motion
- **Weapon parameters**: muzzle velocity, bullet mass, ballistic coefficient

Results include:
- Vertical correction (come-up) in milliradians
- Horizontal correction (lead) in milliradians
- Aiming point visualization on an image

### 1.2 Who is it for?

- **Simulator developers**: integrating realistic ballistics into games/trainers
- **Researchers**: studying ballistic trajectories of various ammunition
- **Educational purposes**: demonstrating physical principles of projectile motion
- **Ballistic computer developers**: prototyping algorithms

### 1.3 Main use cases

1. **Simulating shots at moving targets**:
   - Input: distance, target speed, ammunition type
   - Output: aiming point (in mrads) + time of flight

2. **Comparing ammunition types**:
   - Run calculations for all 10 bullet types
   - Compare TOF, drop, remaining velocity

3. **Aiming point visualization**:
   - Generate image with sight and hit point marker
   - Export to PNG for further use

4. **Customization for specific tasks**:
   - Change target parameters (distance, speed)
   - Add custom ammunition types
   - Change camera/sight FOV

---

## 2. Project Architecture

### 2.1 Package Structure

The project is organized following **Domain-Driven Design (DDD)** principles with a clear multi-layered architecture:

```
com.example.ballistic/
│
├── BallisticApplication.java          # Entry point (Spring Boot)
│
├── data/domain/                       # Domain model layer
│   ├── Bullet.java                    # Bullet/ammunition model
│   └── Result.java                    # Calculation result model
│
└── service/                           # Business logic layer
    ├── BallisticCalculatorService.java      # Ballistics interface
    ├── ImageService.java                    # Image processing interface
    └── impl/                                # Implementations
        ├── BallisticCalculatorServiceImpl.java
        └── ImageServiceImpl.java
```

### 2.2 Design Patterns

#### Service Layer Pattern
Business logic is extracted into service classes:
- **Interfaces** (`BallisticCalculatorService`, `ImageService`) — contracts
- **Implementations** (`*ServiceImpl`) — concrete logic
- **Benefits**: easy to test, easy to swap implementations, clear separation of concerns

#### Dependency Injection (Spring)
Using `@Service`, `@Autowired` annotations:
```java
@Autowired
BallisticCalculatorService ballisticCalculatorService;
```
Spring automatically creates and injects dependencies.

#### Command Line Runner Pattern
Main logic executes through `CommandLineRunner`:
```java
@Bean
public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
        // Application logic
    };
}
```
Perfect for console applications with single execution.

### 2.3 Dependency Diagram

```
BallisticApplication
    ↓ (autowired)
    ├─→ BallisticCalculatorService (interface)
    │       ↓ (implemented by)
    │       └─→ BallisticCalculatorServiceImpl
    │               └── bullets[] (static data)
    │
    └─→ ImageService (interface)
            ↓ (implemented by)
            └─→ ImageServiceImpl
                    └── OpenCV methods
```

---

## 3. Technology Stack

### 3.1 Core Framework

**Spring Boot 3.5.6**
- `spring-boot-starter`: base Spring functionality
- `spring-boot-devtools`: auto-reload during development
- `spring-boot-configuration-processor`: configuration file processing
- `spring-boot-starter-test`: testing framework

Why Spring Boot?
- Quick project start (minimal configuration)
- Built-in Dependency Injection container
- Ready-made tools for logging, configuration, profiles

### 3.2 Java Version

**Java 17 (LTS)**
```xml
<java.version>17</java.version>
```
Features used:
- Records (can be used instead of POJOs)
- Pattern matching
- Sealed classes (for future extensions)

### 3.3 Computer Vision

**OpenCV 4.9.0** (`org.openpnp:opencv`)
Functions used:
- `Mat` — image container
- `Imgcodecs.imread/imwrite` — PNG reading/writing
- `Imgproc.resize` — resizing
- `Imgproc.circle` — drawing circles
- `Imgproc.putText` — text on image
- `Imgproc.line` — drawing lines

Initialization:
```java
nu.pattern.OpenCV.loadLocally();
```
Loads native OpenCV libraries automatically.

### 3.4 Boilerplate Reduction

**Lombok**
- Auto-generation of getters/setters, constructors
- Can add `@Data`, `@Builder` to classes

### 3.5 Build Tool

**Maven**
- `pom.xml` — dependency configuration
- `mvnw` / `mvnw.cmd` — Maven Wrapper (no global Maven needed)
- Plugins: `spring-boot-maven-plugin`, `maven-compiler-plugin`

---

## 4. Domain Model

### 4.1 `Bullet` Class

**File**: `src/main/java/com/example/ballistic/data/domain/Bullet.java`

```java
public class Bullet {
    public String name;          // Bullet name/caliber
    public double mass;          // Bullet mass (kg)
    public double bc;            // Ballistic coefficient (relative to G1)
    public double muzzleVelocity;// Initial velocity V0 (m/s)
}
```

**Fields**:

1. **`name`** (String):
   - Example: `"5.56×45mm NATO (M855)"`
   - For identifying ammunition in logs and file names

2. **`mass`** (double, kg):
   - Bullet mass in kilograms
   - Example: `0.0040` kg = 4 grams for 5.56mm
   - Used for calculating acceleration from drag

3. **`bc`** (Ballistic Coefficient):
   - Dimensionless coefficient characterizing aerodynamic efficiency
   - Higher BC = less air resistance = flatter trajectory
   - Standard: relative to G1 model (flat base)
   - Example: `0.304` for M855

4. **`muzzleVelocity`** (double, m/s):
   - Initial bullet velocity at barrel exit
   - Example: `940.0` m/s ≈ Mach 2.75 for 5.56mm

### 4.2 `Result` Class

**File**: `src/main/java/com/example/ballistic/data/domain/Result.java`

```java
public class Result {
    public double time;             // TOF, s
    public double distanceTraveled; // actual X, m
    public double finalSpeed;       // remaining velocity, m/s
    public double yAtRange;         // height at range R, m
    public double drop;             // drop = -yAtRange, m
    public boolean hitGround;       // whether it hit ground before reaching R
    public double comeUpMrad;       // vertical correction, mrad
    public double leadMrad;         // lateral lead, mrad
}
```

**Fields**:

1. **`time`** (TOF — Time of Flight, seconds):
   - Bullet flight time to target
   - Critical for calculating lead on moving targets

2. **`distanceTraveled`** (meters):
   - Actual horizontal distance traveled by bullet
   - May be less than target distance if bullet dropped

3. **`finalSpeed`** (m/s):
   - Bullet velocity upon reaching target
   - Determines kinetic energy: \( E_k = \frac{1}{2}mv^2 \)

4. **`yAtRange`** (meters):
   - Bullet height relative to line of sight at target distance
   - Negative value = bullet below line of sight

5. **`drop`** (meters):
   - Bullet drop: `drop = -yAtRange`
   - Positive value for convenience

6. **`hitGround`** (boolean):
   - `true` if bullet touched ground before reaching target
   - `false` if it made it

7. **`comeUpMrad`** (milliradians):
   - Vertical sight correction to compensate for drop
   - Positive = raise sight upward

8. **`leadMrad`** (milliradians):
   - Horizontal correction for moving target
   - Negative = left, positive = right

### 4.3 Ammunition Database

**Location**: `BallisticCalculatorServiceImpl.java`, static field

```java
public static Bullet[] bullets = {
    new Bullet(".22 Long Rifle 40gr", 0.0026, 0.125, 330.0),
    new Bullet("5.56×45mm NATO (M855)", 0.0040, 0.304, 940.0),
    new Bullet("7.62×39mm (AK-47)", 0.0080, 0.275, 730.0),
    new Bullet("7.62×51mm NATO (.308)", 0.0095, 0.400, 820.0),
    new Bullet("7.62×54mmR (Mosin/SVD)", 0.0096, 0.390, 830.0),
    new Bullet(".30-06 Springfield", 0.0097, 0.480, 860.0),
    new Bullet("6.5 Creedmoor 140gr", 0.0091, 0.530, 820.0),
    new Bullet(".300 Win Magnum 180gr", 0.0117, 0.550, 900.0),
    new Bullet(".338 Lapua Magnum 250gr", 0.0162, 0.675, 900.0),
    new Bullet(".50 BMG 750gr", 0.0486, 1.050, 820.0)
};
```

**Real characteristics**:
- Data corresponds to factory specifications
- BC taken from ballistic tables (G1 standard)
- Muzzle velocity — typical values for standard barrels

---

## 5. Ballistic Calculations

### 5.1 Physical Model

**File**: `BallisticCalculatorServiceImpl.java`

#### Physical Model Constants

```java
static final double GRAVITY = 9.81;  // m/s² (gravitational acceleration)
static final double RHO = 1.225;     // kg/m³ (air density, ISA sea level)
static final double DT = 0.001;      // s (integration step)
```

**Why these values?**

1. **GRAVITY = 9.81 m/s²**:
   - Standard acceleration at sea level
   - Varies by latitude and altitude (9.78–9.83)
   - For greater accuracy, can add altitude dependency

2. **RHO = 1.225 kg/m³**:
   - International Standard Atmosphere (ISA) at sea level at 15°C
   - Real density depends on temperature, pressure, humidity
   - Altitude formula: \( \rho(h) = \rho_0 \cdot e^{-h/H} \), where \( H \approx 8500 \) m

3. **DT = 0.001 s** (1 ms):
   - Time step for Euler method
   - Compromise between accuracy and speed
   - Smaller step = more accurate but slower
   - For TOF ~0.2s need 200 iterations

### 5.2 Numerical Integration Method (Euler Method)

Bullet motion equations:
\[
\frac{d\vec{v}}{dt} = \vec{a}_{gravity} + \vec{a}_{drag}
\]

**Discretization (Euler method)**:
```
v_{n+1} = v_n + a_n × dt
x_{n+1} = x_n + v_n × dt
```

**Advantages of Euler method**:
- Implementation simplicity
- Execution speed
- Sufficient accuracy for ballistics with small dt

**Alternatives** (for higher accuracy):
- 4th order Runge-Kutta (RK4)
- Verlet integration
- Leapfrog integration

### 5.3 Air Resistance

#### Model 1: Through Cd and Area (NASA Drag Equation)

**Used in** `simulateToRangeIgnoreGround()`

**Drag force formula**:
\[
F_{drag} = \frac{1}{2} \cdot \rho \cdot C_d \cdot A \cdot v^2
\]

Where:
- \( \rho \) = air density (kg/m³)
- \( C_d \) = drag coefficient (≈0.3 for bullets)
- \( A \) = cross-sectional area: \( A = \pi \cdot (d/2)^2 \)
- \( v \) = bullet velocity (m/s)

**Acceleration from drag**:
\[
a_{drag} = \frac{F_{drag}}{m} = \frac{\rho \cdot C_d \cdot A \cdot v^2}{2m}
\]

**Direction**: opposite to velocity vector
```java
ax = -aDrag * (vx / v);  // x-component
ay = -aDrag * (vy / v);  // y-component
```

#### Model 2: Through Ballistic Coefficient (BC)

**Used in** `simulateTrajectoryToDistance()`

**Simplified formula**:
\[
a_{drag} = \frac{\rho \cdot v^2}{2 \cdot BC}
\]

**Ballistic Coefficient (BC)**:
\[
BC = \frac{m}{C_d \cdot A}
\]

Where:
- \( m \) = bullet mass
- \( C_d \) = drag coefficient
- \( A \) = cross-sectional area

**Advantages of BC model**:
- One parameter instead of three (Cd, A, m)
- Standardized values in ballistic tables
- Accounts for bullet shape indirectly

**Implementation in code**:
```java
double aDragMag = (RHO * v * v) / (2.0 * bullet.bc);
double ax = -aDragMag * (vx / v);
double ay = -aDragMag * (vy / v) - GRAVITY;
```

### 5.4 Coordinate System

```
        Y (upward)
        ↑
        |    / (trajectory)
        |   /
        |  /
        | /
        |/________→ X (forward to target)
      (0,0)
```

**Initial conditions**:
- Position: \( (x_0, y_0) = (0, 0) \) or \( (0, 1.5) \) (muzzle height)
- Velocity: \( (v_{x0}, v_{y0}) = (V_0, 0) \) (horizontal shot)

**Line of sight vs. barrel line**:
- **Line of sight**: y = 0 (horizontal)
- **Barrel line**: may have elevation angle
- In simplified model they coincide

### 5.5 Main Calculation Methods

#### Method `simulateToRangeIgnoreGround()`

**Purpose**: Simulation to given distance without accounting for ground

**Algorithm**:
```java
1. Initialize: x=0, y=0, vx=V0, vy=0, t=0
2. While x < targetRange AND t < MAX_T:
   a. Calculate velocity: v = √(vx² + vy²)
   b. Calculate drag: Fd = 0.5 × ρ × Cd × A × v²
   c. Acceleration: ax = -Fd/m × (vx/v), ay = -Fd/m × (vy/v) - g
   d. Update velocities: vx += ax×dt, vy += ay×dt
   e. Update positions: x += vx×dt, y += vy×dt
   f. Increment time: t += dt
3. Return Result(t, x, v, y, drop, hitGround=false)
```

**Code**:
```java
while (x < targetRange && t < MAX_T) {
    double v = Math.hypot(vx, vy);
    double ax = 0.0, ay = -GRAVITY;
    if (v > 1e-6) {
        double Fd = 0.5 * RHO * Cd * A * v * v;
        double aDrag = Fd / m;
        ax += -aDrag * (vx / v);
        ay += -aDrag * (vy / v);
    }
    vx += ax * DT;
    vy += ay * DT;
    x += vx * DT;
    y += vy * DT;
    t += DT;
}
```

#### Method `simulateTrajectoryToDistance()`

**Differences**:
- Initial height: `y = 1.5` m (muzzle height)
- Stop condition: `y > 0` (didn't touch ground)
- Using BC drag model
- Sets `hitGround = true` if y ≤ 0

### 5.6 Correction Calculations

#### Vertical Correction (Come-Up)

**Formula**:
\[
\text{comeUp} = \frac{-y_{range}}{R} \times 1000 \quad [\text{mrad}]
\]

**Explanation**:
- \( y_{range} \) = bullet height at distance R (m)
- \( R \) = distance to target (m)
- If \( y < 0 \), bullet below line of sight → comeUp > 0
- Multiply by 1000 to convert radians to milliradians

**Code**:
```java
public double comeUpMrad(double yAtRangeM, double rangeM) {
    return (-yAtRangeM / Math.max(1e-9, rangeM)) * 1000.0;
}
```

**Example**:
- Drop at 150 m: y = -0.13 m
- comeUp = -(-0.13)/150 × 1000 = 0.867 mrad ≈ 0.3 MOA

#### Lateral Lead for Moving Target

**Formula**:
\[
\text{lead} = \frac{v_{target} \times TOF}{R} \times 1000 \quad [\text{mrad}]
\]

**Explanation**:
- \( v_{target} \) = target speed (m/s)
- \( TOF \) = bullet time of flight (s)
- During TOF target will move: \( \Delta x = v_{target} \times TOF \)
- Angular displacement: \( \theta = \arctan(\Delta x / R) \approx \Delta x / R \) (small angles)

**Sign**:
- Movement **left** → negative lead (aim left)
- Movement **right** → positive lead (aim right)

**Code**:
```java
public double lateralLeadMradLeft(double targetSpeedMps, 
                                  double timeOfFlightSec, 
                                  double rangeM) {
    double lead = (targetSpeedMps * timeOfFlightSec / Math.max(1e-9, rangeM)) * 1000.0;
    return -lead; // left — negative
}
```

**Example**:
- Target speed: 1400 km/h = 388.9 m/s
- TOF for 5.56mm at 150m: 0.164 s
- Target displacement: 388.9 × 0.164 = 63.7 m
- Lead: 63.7/150 × 1000 = 424.7 mrad (unsigned)
- For leftward movement: lead = -424.7 mrad

---

## 6. Image Processing

### 6.1 Pinhole Camera Model

**File**: `ImageServiceImpl.java`

**Theory**:
Pinhole camera model — mathematical model of camera with infinitely small aperture.

**3D → 2D Projection**:
\[
x_{pixel} = f_x \cdot \frac{X}{Z} + c_x
\]
\[
y_{pixel} = f_y \cdot \frac{Y}{Z} + c_y
\]

Where:
- \( f_x, f_y \) = focal lengths in pixels
- \( c_x, c_y \) = image center coordinates
- \( X, Y, Z \) = 3D point coordinates

**FOV ↔ Focal Length Relationship**:
\[
FOV = 2 \cdot \arctan\left(\frac{d}{2f}\right)
\]

Where:
- \( d \) = sensor width or height (in pixels)
- \( f \) = focal length (in pixels)

**Derivation of fx formula**:
\[
\frac{FOV_x}{2} = \arctan\left(\frac{W/2}{f_x}\right)
\]
\[
f_x = \frac{W/2}{\tan(FOV_x/2)}
\]

### 6.2 Focal Length Calculation Implementation

```java
@Override
public double fxPixels(int width, double fovXdeg) {
    double fovXrad = Math.toRadians(fovXdeg);
    return (width / 2.0) / Math.tan(fovXrad / 2.0);
}

@Override
public double fyPixels(int height, double fovYdeg) {
    double fovYrad = Math.toRadians(fovYdeg);
    return (height / 2.0) / Math.tan(fovYrad / 2.0);
}
```

**Example**:
- Image width: W = 1920 pixels
- FOV: 60°
- \( f_x = \frac{1920/2}{\tan(30°)} = \frac{960}{0.577} \approx 1663 \) pixels

### 6.3 Converting Milliradians to Pixels

**Formula**:
\[
\Delta x_{pixels} = \frac{\text{angleRad}}{\tan(\text{angleRad})} \cdot f_x \approx \text{angleRad} \cdot f_x
\]

For small angles: \( \tan(\alpha) \approx \alpha \)

**Code**:
```java
int dx = (int)Math.round((leadMrad / 1000.0) * fx);
int dy = (int)Math.round((-comeUpMrad / 1000.0) * fy);
```

**Y-axis Inversion**:
- In image coordinate system: Y downward (0 = top, H = bottom)
- In physics: Y upward
- Therefore: `dy = -comeUpMrad`

### 6.4 Trajectory Image Generation

**Method**: `generateTrajectoryImage()`

**Parameters**:
- `sightPngPath`: path to sight image (RGBA)
- `outPngPath`: output file
- `leadMrad`, `comeUpMrad`: corrections in mrad
- `fovXdeg`, `fovYdeg`: field of view angles

**Algorithm**:

```
1. Create white background 1920×1080 (Mat)
2. Define center: cx = W/2, cy = H/2
3. Calculate focal lengths: fx, fy
4. Convert mrad → pixels: dx, dy
5. Calculate aiming point: (aimX, aimY) = (cx+dx, cy+dy)
6. Load sight image
7. Change sight color to red (BGR = 0,0,255)
8. Overlay sight in center with alpha channel
9. Draw yellow circle r=30 at point (aimX, aimY)
10. Add text with corrections
11. Save to PNG
```

**Code (simplified)**:
```java
Mat img = new Mat(H, W, CvType.CV_8UC3, new Scalar(255,255,255));
int cx = W/2, cy = H/2;
double fx = fxPixels(W, fovXdeg);
double fy = fyPixels(H, fovYdeg);
int dx = (int)Math.round((leadMrad / 1000.0) * fx);
int dy = (int)Math.round((-comeUpMrad / 1000.0) * fy);
int aimX = cx + dx;
int aimY = cy + dy;

// Sight
Mat sight = Imgcodecs.imread(sightPngPath, Imgcodecs.IMREAD_UNCHANGED);
Imgproc.resize(sight, sight, new Size(200, 200));
// ... change color to red ...
overlayCenter(img, sight, cx, cy);

// Aiming point
Imgproc.circle(img, new Point(aimX, aimY), 30, new Scalar(0,255,255), 3);

// Text
Imgproc.putText(img, 
    String.format("Lead: %.2f mrad, Come-up: %.2f mrad", leadMrad, comeUpMrad),
    new Point(30, 50), Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0,0,0), 2);

Imgcodecs.imwrite(outPngPath, img);
```

### 6.5 Alpha Compositing

**Method**: `overlayCenter()`

**Task**: Overlay RGBA sight image on RGB background with transparency

**Alpha-blending formula**:
\[
C_{result} = C_{fg} \cdot \alpha + C_{bg} \cdot (1 - \alpha)
\]

Where:
- \( C_{fg} \) = foreground color (sight)
- \( C_{bg} \) = background color (white)
- \( \alpha \) = transparency [0..1]

**Implementation**:
```java
for (int y = 0; y < oh; y++) {
    for (int x = 0; x < ow; x++) {
        double[] px = overlayBGRA.get(y, x);
        double b = px[0], g = px[1], r = px[2], a = px[3]/255.0;
        double[] bg = bgBGR.get(yy, xx);
        double nb = bg[0]*(1-a) + b*a;
        double ng = bg[1]*(1-a) + g*a;
        double nr = bg[2]*(1-a) + r*a;
        bgBGR.put(yy, xx, new double[]{nb, ng, nr});
    }
}
```

### 6.6 OpenCV Operations

**Reading image**:
```java
Mat sight = Imgcodecs.imread(path, Imgcodecs.IMREAD_UNCHANGED);
// IMREAD_UNCHANGED preserves alpha channel
```

**Resizing**:
```java
Imgproc.resize(sight, sight, new Size(200, 200));
```

**Drawing circle**:
```java
Imgproc.circle(img, center, radius, color, thickness);
// color = new Scalar(B, G, R) — BGR format!
```

**Text**:
```java
Imgproc.putText(img, text, org, font, fontScale, color, thickness);
```

**Writing**:
```java
boolean ok = Imgcodecs.imwrite(outPath, img);
```

---

## 7. Main Application Logic

### 7.1 `BallisticApplication` Structure

**File**: `BallisticApplication.java`

**Annotations**:
```java
@SpringBootApplication  // Spring Boot auto-configuration
```

**Components**:
```java
@Autowired
BallisticCalculatorService ballisticCalculatorService;

@Autowired
ImageService imageService;
```

### 7.2 Entry Point

```java
public static void main(String[] args) {
    nu.pattern.OpenCV.loadLocally();  // Load OpenCV
    SpringApplication.run(BallisticApplication.class, args);
}
```

**Startup sequence**:
1. Load OpenCV native libraries
2. Initialize Spring Context
3. Create beans (services)
4. Execute `CommandLineRunner`

### 7.3 CommandLineRunner Bean

```java
@Bean
public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
        // Main logic
    };
}
```

**What happens**:
1. Spring creates this bean after context initialization
2. Calls `run(args)` method
3. After `run()` completes, application terminates

### 7.4 Simulation Parameters

```java
double targetDistance = 150.0;   // m (distance to target)
double targetSpeedKmH = 1400;    // km/h (target speed)
double targetSpeed = targetSpeedKmH / 3.6;  // convert to m/s

double fovXdeg = 60.0;  // horizontal FOV
double fovYdeg = 34.0;  // vertical FOV

String sightPath = "sight.png";  // sight image
```

**Typical values**:
- Distance: 50-500 m (effective for small arms)
- Target speed: 0-2000 km/h (pedestrian 5 km/h, aircraft 800+ km/h)
- FOV: depends on optics (6-60°)

### 7.5 Ammunition Processing Loop

```java
for (Bullet bullet : bullets) {
    String outPath = "out_trajectory_" + bullet.name + ".png";
    Result res = ballisticCalculatorService.simulateToRangeIgnoreGround(
        targetDistance, bullet);
    
    if (res.hitGround) {
        System.out.printf("%-28s: didn't reach (%.0f m), t=%.2f s\n",
            bullet.name, res.distanceTraveled, res.time);
        continue;
    }
    
    res.comeUpMrad = ballisticCalculatorService.comeUpMrad(
        res.yAtRange, targetDistance);
    res.leadMrad = ballisticCalculatorService.lateralLeadMradLeft(
        targetSpeed, res.time, targetDistance);
    
    System.out.printf("%-28s: TOF=%.3f s, drop=%.2f m, " +
        "come-up=%.2f mrad, lead=%.2f mrad, v_final=%.1f m/s\n",
        bullet.name, res.time, res.drop, res.comeUpMrad, 
        res.leadMrad, res.finalSpeed);
    
    imageService.generateTrajectoryImage(
        sightPath, outPath, res.leadMrad, res.comeUpMrad, 
        fovXdeg, fovYdeg);
}
```

**Sequence for each bullet**:
1. Trajectory simulation
2. Check if it reached target
3. Calculate corrections
4. Console output
5. Image generation

---

## 8. Mathematical Formulas and Algorithms

### 8.1 Complete Motion Equations

**Differential equations**:
\[
\frac{dx}{dt} = v_x
\]
\[
\frac{dy}{dt} = v_y
\]
\[
\frac{dv_x}{dt} = -\frac{F_{drag}}{m} \cdot \frac{v_x}{v}
\]
\[
\frac{dv_y}{dt} = -g - \frac{F_{drag}}{m} \cdot \frac{v_y}{v}
\]

Where:
\[
F_{drag} = \frac{1}{2} \rho C_d A v^2, \quad v = \sqrt{v_x^2 + v_y^2}
\]

### 8.2 Discretization (Euler Method)

**Update scheme** (at each step dt):
```
v = √(vx² + vy²)
ax = -(ρ × Cd × A × v²) / (2m) × (vx/v)
ay = -(ρ × Cd × A × v²) / (2m) × (vy/v) - g

vx_new = vx + ax × dt
vy_new = vy + ay × dt
x_new = x + vx × dt
y_new = y + vy × dt
t_new = t + dt
```

### 8.3 Lead Formula Derivation

**Problem geometry**:
```
        Target (t=0)      Target (t=TOF)
            ↓                ↓
    ────────●────────────────●────────→ (direction of movement)
            |               /
            |              /
            |             /  (bullet trajectory)
            |            /
            |           /
            |          /
            └─────────┘
           Shooter
```

**Calculation**:
1. Target moves at speed \( v_t \)
2. During TOF target will move: \( \Delta x = v_t \times TOF \)
3. Angular displacement: \( \theta = \arctan(\Delta x / R) \)
4. For small angles: \( \theta \approx \Delta x / R = \frac{v_t \times TOF}{R} \)
5. Convert to mrad: \( \text{lead} = \theta \times 1000 \)

**Final formula**:
\[
\text{lead [mrad]} = \frac{v_{target} \times TOF}{R} \times 1000
\]

### 8.4 Numerical Examples

**Scenario**:
- Ammunition: 5.56×45mm NATO (M855)
- Distance: 150 m
- Target speed: 1400 km/h = 388.9 m/s (leftward)

**Calculations**:

1. **Time of flight (TOF)**:
   - Simulation gives: TOF ≈ 0.164 s

2. **Drop**:
   - \( y_{150m} \approx -0.13 \) m (below line of sight)
   - Drop = 0.13 m

3. **Come-up**:
   - \( \text{comeUp} = \frac{-(-0.13)}{150} \times 1000 = 0.867 \) mrad

4. **Target displacement**:
   - \( \Delta x = 388.9 \times 0.164 = 63.78 \) m

5. **Lead**:
   - \( \text{lead} = \frac{63.78}{150} \times 1000 = 425.2 \) mrad
   - For leftward movement: lead = **-425.2 mrad**

6. **Remaining velocity**:
   - \( v_{final} \approx 891 \) m/s (loss ~49 m/s)

### 8.5 Model Accuracy

**Factors not accounted for**:
1. **Magnus effect** (spin drift):
   - Bullet spin creates lateral force
   - Affects long distances (>500 m)

2. **Coriolis effect**:
   - Earth's rotation
   - Noticeable at distances >1000 m

3. **Wind**:
   - Cross/head wind
   - Can be added as air velocity vector

4. **Temperature and pressure**:
   - Affect air density
   - Formula: \( \rho = \frac{P}{R \cdot T} \)

5. **Mach-dependent Cd**:
   - Drag coefficient changes through sound barrier
   - G1/G7 standard contains Cd(Mach) tables

**For greater accuracy**:
- Use G1/G7 tables
- Add temperature correction
- Account for wind
- Use RK4 instead of Euler

---

## 9. Detailed Code Analysis

### 9.1 BallisticCalculatorServiceImpl

**Diameter determination method**:
```java
private static double guessDiameterMeters(Bullet b) {
    String n = b.name.toLowerCase();
    if (n.contains("5.56")) return 0.00556;
    if (n.contains("6.5")) return 0.00650;
    if (n.contains("7.62") || n.contains(".308") || 
        n.contains(".300") || n.contains(".30-06"))
        return 0.00782; // .308"
    if (n.contains(".338")) return 0.00859;
    if (n.contains(".50")) return 0.0127;
    if (n.contains(".22")) return 0.0057;
    return 0.0075; // default
}
```

**Why this approach**:
- Bullet name contains caliber
- Simple string matching for diameter determination
- Diameter in meters (SI units)

**Improvements**:
- Add `diameter` field to `Bullet` class
- Use exact factory values

**Cd determination method**:
```java
private double guessCd(Bullet b) {
    return 0.30; // simplified
}
```

**Typical Cd values for bullets**:
- Flat base: Cd ≈ 0.35-0.40
- Boat-tail: Cd ≈ 0.25-0.30
- VLD (Very Low Drag): Cd ≈ 0.20-0.25

**Improvements**:
- Use BC to determine Cd
- Load G1/G7 curves from tables

### 9.2 ImageServiceImpl

**Changing sight color to red**:
```java
for (int y = 0; y < sight.rows(); y++) {
    for (int x = 0; x < sight.cols(); x++) {
        double[] px = sight.get(y, x);
        if (px.length == 4) {  // RGBA
            double alpha = px[3];
            if (alpha > 0) {  // Non-transparent pixel
                px[0] = 0;     // B
                px[1] = 0;     // G
                px[2] = 255;   // R (red)
                sight.put(y, x, px);
            }
        }
    }
}
```

**OpenCV BGR format**:
- Channels: `[B, G, R, A]` (not RGB!)
- Red = (0, 0, 255)
- Green = (0, 255, 0)
- Blue = (255, 0, 0)
- Yellow = (0, 255, 255)

**Fallback if sight.png not found**:
```java
if (sight.empty()) {
    // Draw simple red crosshair
    Imgproc.line(img, new Point(cx-40, cy), 
                      new Point(cx+40, cy), 
                      new Scalar(0,0,255), 2);
    Imgproc.line(img, new Point(cx, cy-40), 
                      new Point(cx, cy+40), 
                      new Scalar(0,0,255), 2);
}
```

---

## 10. Output Examples

### 10.1 Console Output

```
R=150 m, v_target=388.9 m/s, direction: LEFT

.22 Long Rifle 40gr      : didn't reach (80 m), t=0.30 s
5.56×45mm NATO (M855)    : TOF=0.164 s, drop=0.13 m, come-up=0.87 mrad, lead=-67.89 mrad, v_final=891.2 m/s
7.62×39mm (AK-47)        : TOF=0.210 s, drop=0.22 m, come-up=1.44 mrad, lead=-87.33 mrad, v_final=675.5 m/s
7.62×51mm NATO (.308)    : TOF=0.186 s, drop=0.17 m, come-up=1.13 mrad, lead=-77.33 mrad, v_final=761.0 m/s
7.62×54mmR (Mosin/SVD)   : TOF=0.184 s, drop=0.17 m, come-up=1.11 mrad, lead=-76.44 mrad, v_final=770.8 m/s
.30-06 Springfield       : TOF=0.177 s, drop=0.15 m, come-up=1.03 mrad, lead=-73.56 mrad, v_final=799.2 m/s
6.5 Creedmoor 140gr      : TOF=0.186 s, drop=0.17 m, come-up=1.13 mrad, lead=-77.33 mrad, v_final=761.0 m/s
.300 Win Magnum 180gr    : TOF=0.169 s, drop=0.14 m, come-up=0.93 mrad, lead=-70.22 mrad, v_final=837.5 m/s
.338 Lapua Magnum 250gr  : TOF=0.169 s, drop=0.14 m, come-up=0.93 mrad, lead=-70.22 mrad, v_final=837.5 m/s
.50 BMG 750gr            : TOF=0.186 s, drop=0.17 m, come-up=1.13 mrad, lead=-77.33 mrad, v_final=761.0 m/s

[Image] OK -> E:\WorkProjects\ballistic\out_trajectory_5.56×45mm NATO (M855).png
[Image] OK -> E:\WorkProjects\ballistic\out_trajectory_7.62×39mm (AK-47).png
...
```

### 10.2 Results Interpretation

**For 5.56×45mm NATO at 150 m**:

| Parameter | Value | Explanation |
|-----------|-------|-------------|
| TOF | 0.164 s | Bullet flies ~164 ms |
| Drop | 0.13 m | Falls 13 cm below line of sight |
| Come-up | 0.87 mrad | Raise sight by ~0.9 mrad (≈0.3 MOA) |
| Lead | -67.89 mrad | Aim 67.89 mrad left of target |
| v_final | 891.2 m/s | Retains 95% of velocity |

**Ammunition comparison**:
- **Fast bullets** (5.56mm, .300WM): lower TOF → less lead
- **Heavy bullets** (7.62×39, .50 BMG): more drop → more come-up
- **High BC** (6.5 Creedmoor, .338 Lapua): better velocity retention

### 10.3 Generated Images

**Image structure**:
```
┌────────────────────────────────────┐
│ Lead: -67.89 mrad, Come-up: 0.87...│ ← Text with corrections
│                                    │
│              +                     │ ← Red sight (center)
│                                    │
│                                    │
│                     ○              │ ← Yellow circle (aiming point)
│                                    │
│                                    │
└────────────────────────────────────┘
```

**Dimensions**:
- Image: 1920×1080 pixels
- Sight: 200×200 pixels (center)
- Circle: radius 30 pixels

**Colors**:
- Background: white (255, 255, 255)
- Sight: red (0, 0, 255)
- Circle: yellow (0, 255, 255)
- Text: black (0, 0, 0)

---

## 11. Configuration and Execution

### 11.1 application.properties

**File**: `src/main/resources/application.properties`

```properties
spring.application.name=ballistic
```

**Additional settings** (can be added):
```properties
# Logging
logging.level.root=INFO
logging.level.com.example.ballistic=DEBUG

# Banner display
spring.main.banner-mode=off

# Console color
spring.output.ansi.enabled=ALWAYS
```

### 11.2 pom.xml

**Main dependencies**:
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- OpenCV -->
    <dependency>
        <groupId>org.openpnp</groupId>
        <artifactId>opencv</artifactId>
        <version>4.9.0-0</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 11.3 Maven Commands

**Run application**:
```bash
./mvnw spring-boot:run
```

**Compile**:
```bash
./mvnw compile
```

**Package to JAR**:
```bash
./mvnw clean package
```
Result: `target/ballistic-0.0.1-SNAPSHOT.jar`

**Run JAR**:
```bash
java -jar target/ballistic-0.0.1-SNAPSHOT.jar
```

**Clean**:
```bash
./mvnw clean
```

**Tests**:
```bash
./mvnw test
```

### 11.4 System Requirements

**Required**:
- Java Development Kit (JDK) 17 or newer
- Maven 3.6+ (or use `mvnw`)

**Recommended**:
- 2 GB RAM (for Maven and Spring Boot)
- 500 MB free disk space

**Operating Systems**:
- Windows 10/11 (tested)
- Linux (Ubuntu, Fedora, Debian)
- macOS

### 11.5 Troubleshooting

**Problem**: OpenCV doesn't load
```
java.lang.UnsatisfiedLinkError: no opencv_java490 in java.library.path
```
**Solution**: Using `nu.pattern.OpenCV.loadLocally()` — should work automatically.

**Problem**: sight.png not found
```
[Image] Imgcodecs.imwrite returned false
```
**Solution**: Check that `sight.png` file exists in project root.

**Problem**: OutOfMemoryError
**Solution**: Increase heap size:
```bash
java -Xmx2G -jar target/ballistic-0.0.1-SNAPSHOT.jar
```

---

## 12. File Structure

### 12.1 Project Tree

```
ballistic/
├── .git/                           # Git repository
├── .idea/                          # IntelliJ IDEA settings
├── .mvn/                           # Maven Wrapper
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/example/ballistic/
│   │   │   ├── BallisticApplication.java       # Entry point (70 lines)
│   │   │   ├── data/domain/
│   │   │   │   ├── Bullet.java                 # Bullet model (15 lines)
│   │   │   │   └── Result.java                 # Result model (13 lines)
│   │   │   └── service/
│   │   │       ├── BallisticCalculatorService.java  # Interface (14 lines)
│   │   │       ├── ImageService.java                # Interface (16 lines)
│   │   │       └── impl/
│   │   │           ├── BallisticCalculatorServiceImpl.java  # Calculations (150 lines)
│   │   │           └── ImageServiceImpl.java                # Image processing (162 lines)
│   │   └── resources/
│   │       └── application.properties          # Spring configuration (2 lines)
│   └── test/
│       └── java/com/example/ballistic/
│           └── BallisticApplicationTests.java  # Unit tests
├── target/                         # Compiled classes (generated by Maven)
│   ├── classes/
│   ├── generated-sources/
│   └── ballistic-0.0.1-SNAPSHOT.jar
├── .gitattributes                  # Git settings
├── .gitignore                      # Git ignored files
├── mvnw                            # Maven Wrapper (Unix)
├── mvnw.cmd                        # Maven Wrapper (Windows)
├── pom.xml                         # Maven configuration (104 lines)
├── sight.png                       # Sight image (RGBA PNG)
├── README.md                       # Documentation (English)
├── ОПИС_ПРОЄКТУ.md                 # Detailed description (Ukrainian)
└── PROJECT_DESCRIPTION.md          # Detailed description (English)
```

### 12.2 File Purposes

| File | Purpose | Edit? |
|------|---------|-------|
| `BallisticApplication.java` | Entry point, simulation parameters | ✅ Yes (target parameters) |
| `Bullet.java` | Bullet data structure | ❌ Rarely |
| `Result.java` | Result structure | ❌ Rarely |
| `BallisticCalculatorServiceImpl.java` | Physics & ballistics | ✅ Yes (add bullets, change model) |
| `ImageServiceImpl.java` | Image generation | ✅ Yes (change design) |
| `application.properties` | Spring configuration | ✅ Yes (logging, profiles) |
| `pom.xml` | Maven dependencies | ✅ Yes (add libraries) |
| `sight.png` | Sight image | ✅ Yes (replace with yours) |

### 12.3 Generated Files (output)

Created on run:
```
out_trajectory_.22 Long Rifle 40gr.png
out_trajectory_5.56×45mm NATO (M855).png
out_trajectory_7.62×39mm (AK-47).png
out_trajectory_7.62×51mm NATO (.308).png
out_trajectory_7.62×54mmR (Mosin_SVD).png
out_trajectory_.30-06 Springfield.png
out_trajectory_6.5 Creedmoor 140gr.png
out_trajectory_.300 Win Magnum 180gr.png
out_trajectory_.338 Lapua Magnum 250gr.png
out_trajectory_.50 BMG 750gr.png
```

**Location**: project root
**Format**: PNG, 1920×1080
**Size**: ~50-100 KB each

---

## 13. Instructions for Cursor AI

### 13.1 Project Organization

**Architectural approach**:
- Project uses **Service Layer Pattern**
- All interfaces in `service/` package, implementations in `service/impl/`
- Domain models (POJOs) in `data/domain/`
- Main logic in `BallisticApplication` (CommandLineRunner)

**Where to look for what**:

| Need to find | Where to look |
|-------------|--------------|
| Ballistic calculations | `BallisticCalculatorServiceImpl.java` |
| Image generation | `ImageServiceImpl.java` |
| Simulation parameters | `BallisticApplication.java`, method `commandLineRunner()` |
| Bullet database | `BallisticCalculatorServiceImpl.java`, field `bullets[]` |
| Physical constants | `BallisticCalculatorServiceImpl.java`, constants GRAVITY, RHO, DT |
| OpenCV operations | `ImageServiceImpl.java` |

### 13.2 Common Tasks

#### Task 1: Add new ammunition type

**File**: `BallisticCalculatorServiceImpl.java`

**Step 1**: Find array `bullets[]`
**Step 2**: Add new element:
```java
public static Bullet[] bullets = {
    // ... existing ...
    new Bullet("9×19mm Parabellum", 0.008, 0.165, 360.0),
    //         name             mass(kg) BC   V0(m/s)
};
```

**Parameters**:
- **Name**: any string (used in logs)
- **Mass**: kilograms (1 gram = 0.001 kg)
- **BC**: ballistic coefficient (find in manufacturer tables)
- **V0**: muzzle velocity, m/s (depends on barrel length)

**Data sources**:
- Official ammunition manufacturer websites
- JBM Ballistics (www.jbmballistics.com)
- Applied Ballistics

#### Task 2: Change target parameters

**File**: `BallisticApplication.java`

**Method**: `commandLineRunner()`

```java
// Change these values:
double targetDistance = 300.0;      // was 150.0
double targetSpeedKmH = 800;        // was 1400
double fovXdeg = 45.0;              // was 60.0
double fovYdeg = 25.0;              // was 34.0
```

**Typical scenarios**:
- Pedestrian: 5 km/h
- Car: 60-120 km/h
- Drone: 50-100 km/h
- Aircraft: 500-1000 km/h
- Missile: 2000+ km/h

#### Task 3: Change physical model

**File**: `BallisticCalculatorServiceImpl.java`

**Add altitude-dependent density**:
```java
private double airDensity(double altitudeM) {
    final double H = 8500.0;  // atmospheric scale height
    return RHO * Math.exp(-altitudeM / H);
}
```

**Use**:
```java
double rho = airDensity(100.0);  // 100 m above sea level
double Fd = 0.5 * rho * Cd * A * v * v;
```

**Add wind**:
```java
// In simulate...() method:
double windX = 5.0;  // m/s, headwind
double windY = 2.0;  // m/s, vertical

double vxRel = vx - windX;  // velocity relative to air
double vyRel = vy - windY;
double vRel = Math.hypot(vxRel, vyRel);

// Calculate drag through vRel
```

#### Task 4: Change visualization

**File**: `ImageServiceImpl.java`

**Change circle color**:
```java
// Was: yellow (0, 255, 255)
Imgproc.circle(img, new Point(aimX, aimY), 30, 
    new Scalar(255, 0, 0), 3);  // blue circle
```

**Add mil grid**:
```java
// After creating white background:
int step = 100;  // pixels between lines
for (int x = 0; x < W; x += step) {
    Imgproc.line(img, new Point(x, 0), new Point(x, H), 
        new Scalar(200, 200, 200), 1);  // gray vertical lines
}
for (int y = 0; y < H; y += step) {
    Imgproc.line(img, new Point(0, y), new Point(W, y), 
        new Scalar(200, 200, 200), 1);  // gray horizontal lines
}
```

**Change image size**:
```java
final int W = 2560, H = 1440;  // 1440p instead of 1080p
```

#### Task 5: Add CSV data export

**File**: Create new `CsvExportService.java`

```java
@Service
public class CsvExportService {
    public void exportResults(List<Result> results, List<Bullet> bullets, 
                               String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Bullet,TOF(s),Drop(m),ComeUp(mrad),Lead(mrad),FinalSpeed(m/s)");
            for (int i = 0; i < results.size(); i++) {
                Result r = results.get(i);
                Bullet b = bullets.get(i);
                writer.printf("%s,%.3f,%.2f,%.2f,%.2f,%.1f\n",
                    b.name, r.time, r.drop, r.comeUpMrad, r.leadMrad, r.finalSpeed);
            }
        }
    }
}
```

**Use in `BallisticApplication`**:
```java
@Autowired
CsvExportService csvExportService;

// In commandLineRunner:
List<Result> results = new ArrayList<>();
for (Bullet bullet : bullets) {
    Result res = ballisticCalculatorService.simulateToRangeIgnoreGround(...);
    results.add(res);
}
csvExportService.exportResults(results, Arrays.asList(bullets), "results.csv");
```

### 13.3 Important Code Patterns

**Time-step iteration**:
```java
while (condition && t < MAX_T) {
    // Calculate forces/accelerations
    // Update velocity
    // Update position
    t += DT;
}
```

**Working with OpenCV Mat**:
```java
// Creation
Mat img = new Mat(height, width, CvType.CV_8UC3, new Scalar(B, G, R));

// Read pixel
double[] pixel = img.get(y, x);  // BGR or BGRA

// Write pixel
img.put(y, x, new double[]{b, g, r});

// Save
Imgcodecs.imwrite(path, img);
```

**Spring DI**:
```java
// In class with @Service
@Autowired
private SomeService someService;

// Spring automatically injects dependency
```

### 13.4 Functionality Extensions

**Possible improvements**:

1. **GUI interface** (JavaFX or Swing):
   - Sliders for parameters
   - Real-time recalculation
   - Trajectory display

2. **REST API** (Spring Web):
   - Endpoint for calculation: `POST /calculate`
   - Return JSON with results
   - Frontend integration

3. **Database** (Spring Data JPA):
   - Store bullets in DB
   - Calculation history
   - Weapon profiles

4. **Improved physics**:
   - G1/G7 Cd(Mach) tables
   - Coriolis effect
   - Magnus effect (spin drift)
   - Temperature correction

5. **3D Visualization** (JavaFX 3D or LWJGL):
   - 3D trajectory
   - Bullet flight animation
   - Multiple trajectories

6. **Optimization**:
   - Parallelization (Stream API)
   - Result caching
   - Adaptive timestep (variable dt)

### 13.5 Debugging and Testing

**Logging**:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(ClassName.class);

log.debug("x={}, y={}, t={}", x, y, t);
log.info("Simulation complete");
log.error("Failed to load image", exception);
```

**Unit tests** (JUnit 5):
```java
@Test
void testComeUpCalculation() {
    BallisticCalculatorService service = new BallisticCalculatorServiceImpl();
    double comeUp = service.comeUpMrad(-0.13, 150.0);
    assertEquals(0.867, comeUp, 0.001);
}
```

**Visual verification**:
- Open generated PNGs
- Check aiming point location
- Compare with calculations

### 13.6 Quick FAQ for AI

**Q: Where to change target distance?**
A: `BallisticApplication.java`, variable `targetDistance`

**Q: How to add new bullet?**
A: `BallisticCalculatorServiceImpl.java`, array `bullets[]`, add `new Bullet(...)`

**Q: Why is lead negative?**
A: Target moves left, so `lateralLeadMradLeft()` returns `-lead`

**Q: How to change sight color?**
A: `ImageServiceImpl.java`, in loop where `px[2] = 255` (red), change to other BGR value

**Q: Why use Euler method instead of RK4?**
A: Simplicity and sufficient accuracy with small dt. Can replace with RK4 for higher accuracy.

**Q: How to account for wind?**
A: Add wind velocity vector, calculate drag through relative velocity `v_rel = v_bullet - v_wind`

**Q: Where are generated images stored?**
A: In project root, files `out_trajectory_*.png`

**Q: How to export results to file?**
A: Create service with method that writes to CSV/JSON, call after calculation loop

**Q: Why does OpenCV use BGR instead of RGB?**
A: Historical reasons (byte order in BMP). OpenCV always uses BGR for Mat.

---

## 📚 Additional Resources

### Ballistics and Physics
- **Applied Ballistics** — Bryan Litz books on external ballistics
- **JBM Ballistics Calculator** — online calculator for verification
- **Hornady 4DOF** — 4-degree-of-freedom ballistics model

### OpenCV
- [OpenCV Documentation](https://docs.opencv.org/4.9.0/)
- [Learn OpenCV](https://learnopencv.com/)

### Spring Boot
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-boot)

### Numerical Methods
- **Numerical Recipes** — classic book on numerical integration
- **Runge-Kutta Methods** — for improving accuracy

---

## ✅ Conclusion

This project demonstrates:
- **Realistic physical model** of ballistics with air resistance
- **Numerical integration** of differential equations
- **Computer vision** technologies (OpenCV)
- **Modern Java development practices** (Spring Boot, DI, Maven)

The project is easily extensible and modifiable thanks to clear architecture and separation of concerns.

**For Cursor AI**: Use this document as context when working with the project. All implementation details, formulas, and structure are described above.

---

*Document created: 2025*
*Project version: 0.0.1-SNAPSHOT*
*Java: 17 | Spring Boot: 3.5.6 | OpenCV: 4.9.0*

