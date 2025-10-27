# Ballistic Calculator

A high-precision ballistic trajectory calculator with visual aiming point generation for moving targets. Built with Spring Boot and OpenCV.

## 📋 Overview

This application simulates bullet trajectories using real-world physics models, calculating precise aiming corrections (come-up and lead) for hitting moving targets at various distances. It generates visual representations showing where to aim on a sight picture.

## ✨ Features

- **Physics-Based Simulation**: Accurate ballistic calculations using:
  - Gravity (9.81 m/s²)
  - Air resistance with drag coefficients
  - Ballistic coefficient (BC) modeling
  - Euler numerical integration (dt=0.001s)

- **10 Real-World Ammunition Types**:
  - .22 Long Rifle 40gr
  - 5.56×45mm NATO (M855)
  - 7.62×39mm (AK-47)
  - 7.62×51mm NATO (.308)
  - 7.62×54mmR (Mosin/SVD)
  - .30-06 Springfield
  - 6.5 Creedmoor 140gr
  - .300 Win Magnum 180gr
  - .338 Lapua Magnum 250gr
  - .50 BMG 750gr

- **Moving Target Calculations**:
  - Time of Flight (TOF)
  - Bullet drop compensation (come-up in milliradians)
  - Lateral lead for moving targets (in milliradians)
  - Remaining velocity at target

- **Visual Output**:
  - Generates aim point overlay images
  - Red crosshair sight reticle
  - Yellow circle marking precise aim point
  - Supports custom FOV (Field of View) settings

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.5.6**
- **OpenCV 4.9.0** - Computer vision and image processing
- **Lombok** - Boilerplate code reduction
- **Maven** - Dependency management and build tool

## 📦 Project Structure

```
ballistic/
├── src/main/java/com/example/ballistic/
│   ├── BallisticApplication.java          # Main application entry point
│   ├── data/domain/
│   │   ├── Bullet.java                     # Bullet data model
│   │   └── Result.java                     # Calculation result model
│   └── service/
│       ├── BallisticCalculatorService.java # Ballistic calculations interface
│       ├── ImageService.java               # Image generation interface
│       └── impl/
│           ├── BallisticCalculatorServiceImpl.java  # Physics simulation
│           └── ImageServiceImpl.java                 # Image processing
├── src/main/resources/
│   └── application.properties              # Spring configuration
├── sight.png                               # Sight reticle image
└── pom.xml                                 # Maven configuration
```

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation & Run

1. Clone the repository:
```bash
git clone <repository-url>
cd ballistic
```

2. Run the application:
```bash
./mvnw spring-boot:run
```

Or on Windows:
```cmd
mvnw.cmd spring-boot:run
```

### Build JAR

```bash
./mvnw clean package
java -jar target/ballistic-0.0.1-SNAPSHOT.jar
```

## 💡 Usage Example

The application runs as a command-line tool and processes all ammunition types automatically:

**Default Configuration:**
- Target distance: 150 meters
- Target speed: 1400 km/h moving left
- Camera FOV: 60° horizontal × 34° vertical

**Console Output:**
```
R=150 м, v_target=388.9 м/с, напрямок: ВЛІВО
.22 Long Rifle 40gr      : не долетіла (80 м), t=0.30 c
5.56×45mm NATO (M855)    : TOF=0.164 c, drop=0.13 м, come-up=0.87 mrad, lead=-67.89 mrad, v_зал=891.2 м/с
7.62×39mm (AK-47)        : TOF=0.210 c, drop=0.22 м, come-up=1.44 mrad, lead=-87.33 mrad, v_зал=675.5 м/с
...
```

**Generated Images:**
- `out_trajectory_<bullet_name>.png` - Visual aim point for each ammunition type

## 🔧 Customization

### Change Target Parameters

Edit `BallisticApplication.java`:

```java
double targetDistance = 150.0;   // meters
double targetSpeedKmH = 1400;    // km/h
double fovXdeg = 60.0;           // horizontal FOV
double fovYdeg = 34.0;           // vertical FOV
```

### Add Custom Ammunition

Edit `BallisticCalculatorServiceImpl.java`:

```java
public static Bullet[] bullets = {
    new Bullet("Custom Ammo", 0.010, 0.450, 850.0),
    // name, mass (kg), BC, muzzle velocity (m/s)
    ...
};
```

## 📐 Physics Model

### Ballistic Equations

**Drag Force:**
```
F_drag = 0.5 × ρ × C_d × A × v²
```

**Motion Integration (Euler Method):**
```
vx_{n+1} = vx_n + ax × dt
vy_{n+1} = vy_n + (ay - g) × dt
x_{n+1} = x_n + vx × dt
y_{n+1} = y_n + vy × dt
```

**Aiming Corrections:**
- Come-up (vertical): `(-y/R) × 1000` mrad
- Lead (lateral): `(v_target × TOF / R) × 1000` mrad

### Pinhole Camera Model

```
fx = (width/2) / tan(FOV_x/2)
fy = (height/2) / tan(FOV_y/2)
```

## 📚 Documentation

For detailed technical documentation:
- **Ukrainian**: See [ОПИС_ПРОЄКТУ.md](ОПИС_ПРОЄКТУ.md)
- **English**: See [PROJECT_DESCRIPTION.md](PROJECT_DESCRIPTION.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## 📄 License

This project is open source and available under the MIT License.

## 🎯 Use Cases

- Precision shooting simulation
- Ballistic computer development
- Educational purposes for ballistics and physics
- Game development (realistic ballistics)
- Sniper training aids
- Research and development

## 📞 Contact

For questions or support, please open an issue in the repository.

---

**Note:** This software is for educational and simulation purposes only. Always follow local laws and regulations regarding firearms and ammunition.

