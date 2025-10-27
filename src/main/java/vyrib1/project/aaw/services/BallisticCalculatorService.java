package vyrib1.project.aaw.services;

import org.opencv.core.Point;

/**
 * Сервіс для балістичних розрахунків та конвертації мілірадіан у пікселі.
 * 
 * Виконує математичні перетворення для визначення точки упередження
 * на основі балістичних поправок (come-up та lateral lead).
 */
public interface BallisticCalculatorService {

    /**
     * Конвертує балістичні поправки з мілірадіан у зсув у пікселях.
     * 
     * Використовує модель pinhole камери та FOV для розрахунку фокальної відстані:
     * - fx = (width/2) / tan(FOV_x/2)
     * - fy = (height/2) / tan(FOV_y/2)
     * 
     * Потім конвертує мілірадіани в пікселі:
     * - dx = (leadMrad / 1000.0) × fx
     * - dy = (-comeUpMrad / 1000.0) × fy
     * 
     * @param comeUpMrad вертикальна балістична поправка в мілірадіанах (позитивна = вгору)
     * @param leadMrad горизонтальна балістична поправка в мілірадіанах (негативна = вліво, позитивна = вправо)
     * @param frameWidth ширина кадру в пікселях
     * @param frameHeight висота кадру в пікселях
     * @param fovXdeg горизонтальне поле зору камери в градусах
     * @param fovYdeg вертикальне поле зору камери в градусах
     * @return Point з координатами зсуву (x, y) в пікселях
     */
    Point calculatePixelOffsets(double comeUpMrad, double leadMrad, 
                                int frameWidth, int frameHeight, 
                                double fovXdeg, double fovYdeg);

    /**
     * Розраховує абсолютну позицію точки упередження відносно червоного хреста.
     * Використовує константи з BallisticConstants для всіх параметрів.
     * 
     * Додає балістичні зсуви до поточної позиції прицілу (червоного хреста)
     * для отримання фінальної точки, куди потрібно прицілюватися.
     * 
     * @param crosshairX X-координата червоного хреста (прицілу)
     * @param crosshairY Y-координата червоного хреста (прицілу)
     * @param frameWidth ширина кадру в пікселях
     * @param frameHeight висота кадру в пікселях
     * @return Point з абсолютними координатами точки упередження на екрані
     */
    Point calculateAimPoint(int crosshairX, int crosshairY, 
                           int frameWidth, int frameHeight);

    /**
     * Розраховує абсолютну позицію точки упередження з використанням реальної відстані.
     * Дозволяє використовувати дані з LRF датчика замість константи.
     * 
     * @param crosshairX X-координата червоного хреста (прицілу)
     * @param crosshairY Y-координата червоного хреста (прицілу)
     * @param frameWidth ширина кадру в пікселях
     * @param frameHeight висота кадру в пікселях
     * @param range відстань до цілі в метрах (з LRF датчика)
     * @return Point з абсолютними координатами точки упередження на екрані
     */
    Point calculateAimPoint(int crosshairX, int crosshairY, 
                           int frameWidth, int frameHeight, 
                           double range);

    /**
     * Обчислює фокальну відстань камери по горизонталі в пікселях.
     * 
     * Формула: fx = (width/2) / tan(FOV_x/2)
     * 
     * @param width ширина зображення в пікселях
     * @param fovXdeg горизонтальне поле зору в градусах
     * @return фокальна відстань по X в пікселях
     */
    double calculateFocalLengthX(int width, double fovXdeg);

    /**
     * Обчислює фокальну відстань камери по вертикалі в пікселях.
     * 
     * Формула: fy = (height/2) / tan(FOV_y/2)
     * 
     * @param height висота зображення в пікселях
     * @param fovYdeg вертикальне поле зору в градусах
     * @return фокальна відстань по Y в пікселях
     */
    double calculateFocalLengthY(int height, double fovYdeg);

    /**
     * Розраховує час польоту кулі до цілі (Time of Flight, TOF).
     * 
     * Спрощена формула (без опору повітря):
     * TOF = RANGE / muzzleVelocity
     * 
     * Для більшої точності враховує опір повітря та балістичний коефіцієнт.
     * 
     * @param range відстань до цілі в метрах
     * @param muzzleVelocity початкова швидкість кулі в м/с
     * @return час польоту в секундах
     */
    double calculateFlightTime(double range, double muzzleVelocity);

    /**
     * Розраховує падіння кулі (DROP) на певній відстані.
     * 
     * Формула (спрощена):
     * DROP = 0.5 × g × t²
     * 
     * де:
     * - g = 9.81 м/с² (прискорення вільного падіння)
     * - t = час польоту (FLIGHT_TIME)
     * 
     * Для більшої точності враховує опір повітря.
     * 
     * @param flightTime час польоту кулі в секундах
     * @return падіння кулі в метрах (завжди позитивне значення)
     */
    double calculateDrop(double flightTime);

    /**
     * Розраховує вертикальну балістичну поправку (come-up) в мілірадіанах.
     * 
     * Формула:
     * COME_UP_MRAD = (DROP / RANGE) × 1000
     * 
     * @param drop падіння кулі в метрах
     * @param range відстань до цілі в метрах
     * @return вертикальна поправка в мілірадіанах
     */
    double calculateComeUpMrad(double drop, double range);

    /**
     * Розраховує горизонтальну балістичну поправку (lateral lead) в мілірадіанах.
     * 
     * Формула:
     * LEAD_MRAD = (TARGET_SPEED × FLIGHT_TIME / RANGE) × 1000
     * 
     * @param targetSpeed швидкість цілі в м/с
     * @param flightTime час польоту кулі в секундах
     * @param range відстань до цілі в метрах
     * @return горизонтальна поправка в мілірадіанах (негативна для руху ліворуч)
     */
    double calculateLeadMrad(double targetSpeed, double flightTime, double range);

    /**
     * Розраховує всі балістичні параметри динамічно на основі відстані до цілі.
     * 
     * @param range відстань до цілі в метрах (з LRF датчика)
     * @param muzzleVelocity початкова швидкість кулі в м/с
     * @param targetSpeed швидкість цілі в м/с
     * @return Point з координатами (comeUpMrad, leadMrad)
     */
    org.opencv.core.Point calculateBallisticCorrections(double range, double muzzleVelocity, double targetSpeed);
}

