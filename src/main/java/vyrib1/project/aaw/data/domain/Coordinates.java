package vyrib1.project.aaw.data.domain;

/**
 * Immutable record representing crosshair coordinates.
 * 
 * @param x horizontal position
 * @param y vertical position
 */
public record Coordinates(int x, int y) {
    
    /**
     * Default coordinates pointing to center of a 1280x960 frame.
     */
    public static final Coordinates DEFAULT = new Coordinates(640, 480);
    
    /**
     * Validates that coordinates are non-negative.
     */
    public Coordinates {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative: x=" + x + ", y=" + y);
        }
    }
}

