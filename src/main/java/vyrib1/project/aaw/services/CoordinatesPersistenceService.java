package vyrib1.project.aaw.services;

import vyrib1.project.aaw.data.domain.Coordinates;

/**
 * Service for persisting and loading crosshair coordinates to/from files.
 */
public interface CoordinatesPersistenceService {
    
    /**
     * Loads coordinates from files.
     * 
     * @return Coordinates loaded from files, or default if files don't exist or are invalid
     */
    Coordinates loadCoordinates();
    
    /**
     * Saves coordinates to files.
     * 
     * @param coordinates coordinates to save
     */
    void saveCoordinates(Coordinates coordinates);
}

