package vyrib1.project.aaw.services.impl;

import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.data.BallisticConstants;
import vyrib1.project.aaw.services.BallisticCalculatorService;

/**
 * Реалізація сервісу балістичних розрахунків.
 * 
 * Використовує математичну модель pinhole камери для конвертації
 * кутових поправок (мілірадіани) у лінійні зміщення (пікселі) на екрані.
 */
@Service
public class BallisticCalculatorServiceImpl implements BallisticCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(BallisticCalculatorServiceImpl.class);

    @Override
    public Point calculatePixelOffsets(double comeUpMrad, double leadMrad, 
                                      int frameWidth, int frameHeight, 
                                      double fovXdeg, double fovYdeg) {
        // Validate input parameters
        if (frameWidth <= 0 || frameHeight <= 0) {
            logger.warn("Invalid frame dimensions: {}x{}, using defaults", frameWidth, frameHeight);
            frameWidth = Math.max(1, frameWidth);
            frameHeight = Math.max(1, frameHeight);
        }
        if (fovXdeg <= 0 || fovXdeg >= 180) {
            logger.warn("Invalid horizontal FOV: {}, using default", fovXdeg);
            fovXdeg = BallisticConstants.FOV_HORIZONTAL_DEG;
        }
        if (fovYdeg <= 0 || fovYdeg >= 180) {
            logger.warn("Invalid vertical FOV: {}, using default", fovYdeg);
            fovYdeg = BallisticConstants.FOV_VERTICAL_DEG;
        }
        
        // Обчислюємо фокальні відстані для pinhole camera model
        double fx = calculateFocalLengthX(frameWidth, fovXdeg);
        double fy = calculateFocalLengthY(frameHeight, fovYdeg);

        // Конвертуємо мілірадіани в радіани (ділимо на 1000)
        // Потім множимо на фокальну відстань для отримання пікселів
        
        // Горизонтальний зсув (lateral lead)
        // Позитивний leadMrad = рух вправо, негативний = вліво
        double dx = (leadMrad / 1000.0) * fx;

        // Вертикальний зсув (come-up)
        // Мінус тому що в OpenCV Y-вісь спрямована вниз (0 = верх, height = низ)
        // Позитивний comeUpMrad означає підняття прицілу, тому dy негативний
        double dy = (-comeUpMrad / 1000.0) * fy;

        return new Point(dx, dy);
    }

    @Override
    public Point calculateAimPoint(int crosshairX, int crosshairY, 
                                  int frameWidth, int frameHeight) {
        // Викликаємо перевантажену версію з константою відстані
        return calculateAimPoint(crosshairX, crosshairY, frameWidth, frameHeight, 
                                BallisticConstants.TARGET_DISTANCE);
    }

    @Override
    public Point calculateAimPoint(int crosshairX, int crosshairY, 
                                  int frameWidth, int frameHeight, 
                                  double range) {
        // Перевіряємо валідність відстані
        if (range <= 0 || range > 3000) {
            // Якщо відстань некоректна, використовуємо значення за замовчуванням
            range = BallisticConstants.TARGET_DISTANCE;
        }
        
        // Динамічно розраховуємо балістичні поправки на основі реальної відстані
        Point corrections = calculateBallisticCorrections(
            range,
            BallisticConstants.MUZZLE_VELOCITY_MPS,
            BallisticConstants.TARGET_SPEED_MPS
        );
        
        double comeUpMrad = corrections.x;
        double leadMrad = corrections.y;
        
        // Конвертуємо мілірадіани в пікселі
        Point offsets = calculatePixelOffsets(
            comeUpMrad,
            leadMrad,
            frameWidth,
            frameHeight,
            BallisticConstants.FOV_HORIZONTAL_DEG,
            BallisticConstants.FOV_VERTICAL_DEG
        );

        // Додаємо зсуви до позиції червоного хреста
        double aimX = crosshairX + offsets.x;
        double aimY = crosshairY + offsets.y;

        // Обмежуємо координати межами кадру
        aimX = Math.max(0, Math.min(frameWidth - 1, aimX));
        aimY = Math.max(0, Math.min(frameHeight - 1, aimY));

        return new Point(aimX, aimY);
    }

    @Override
    public double calculateFocalLengthX(int width, double fovXdeg) {
        // Конвертуємо градуси в радіани
        double fovXrad = Math.toRadians(fovXdeg);
        
        // Формула pinhole camera model:
        // FOV/2 = arctan(width/2 / fx)
        // => fx = (width/2) / tan(FOV/2)
        return (width / 2.0) / Math.tan(fovXrad / 2.0);
    }

    @Override
    public double calculateFocalLengthY(int height, double fovYdeg) {
        // Конвертуємо градуси в радіани
        double fovYrad = Math.toRadians(fovYdeg);
        
        // Формула pinhole camera model:
        // FOV/2 = arctan(height/2 / fy)
        // => fy = (height/2) / tan(FOV/2)
        return (height / 2.0) / Math.tan(fovYrad / 2.0);
    }

    @Override
    public double calculateFlightTime(double range, double muzzleVelocity) {
        // Спрощена формула: TOF = відстань / швидкість
        // Для більшої точності потрібно враховувати опір повітря та зміну швидкості
        // Але для початкової реалізації використовуємо середню швидкість
        
        // Враховуємо втрату швидкості через опір повітря (спрощено)
        // Припускаємо, що куля втрачає ~5% швидкості на 100 метрів
        double velocityLossFactor = 1.0 - (range / 100.0 * 0.05);
        double averageVelocity = muzzleVelocity * Math.max(0.7, velocityLossFactor);
        
        return range / averageVelocity;
    }

    @Override
    public double calculateDrop(double flightTime) {
        // Формула вільного падіння: h = 0.5 × g × t²
        // де g = 9.81 м/с² (з BallisticConstants.GRAVITY)
        return 0.5 * BallisticConstants.GRAVITY * flightTime * flightTime;
    }

    @Override
    public double calculateComeUpMrad(double drop, double range) {
        // Формула: COME_UP_MRAD = (DROP / RANGE) × 1000
        if (range <= 0) {
            return 0.0;
        }
        return (drop / range) * 1000.0;
    }

    @Override
    public double calculateLeadMrad(double targetSpeed, double flightTime, double range) {
        // Формула: LEAD_MRAD = (TARGET_SPEED × FLIGHT_TIME / RANGE) × 1000
        if (range <= 0) {
            return 0.0;
        }
        
        // Зміщення цілі за час польоту кулі
        double targetDisplacement = targetSpeed * flightTime;
        
        // Конвертуємо в мілірадіани
        return (targetDisplacement / range) * 1000.0;
    }

    @Override
    public Point calculateBallisticCorrections(double range, double muzzleVelocity, double targetSpeed) {
        // Validate input parameters
        if (range <= 0) {
            logger.warn("Invalid range: {}, using default", range);
            range = BallisticConstants.TARGET_DISTANCE;
        }
        if (muzzleVelocity <= 0) {
            logger.warn("Invalid muzzle velocity: {}, using default", muzzleVelocity);
            muzzleVelocity = BallisticConstants.MUZZLE_VELOCITY_MPS;
        }
        
        // Крок 1: Розраховуємо час польоту
        double flightTime = calculateFlightTime(range, muzzleVelocity);
        
        // Крок 2: Розраховуємо падіння кулі
        double drop = calculateDrop(flightTime);
        
        // Крок 3: Розраховуємо вертикальну поправку
        double comeUpMrad = calculateComeUpMrad(drop, range);
        
        // Крок 4: Розраховуємо горизонтальну поправку
        // Знак мінус для руху ліворуч (за замовчуванням)
        double leadMrad = -calculateLeadMrad(Math.abs(targetSpeed), flightTime, range);
        
        // Повертаємо Point з обома поправками
        return new Point(comeUpMrad, leadMrad);
    }
}

