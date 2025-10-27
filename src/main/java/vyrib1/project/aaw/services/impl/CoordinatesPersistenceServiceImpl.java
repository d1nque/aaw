package vyrib1.project.aaw.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.data.domain.Coordinates;
import vyrib1.project.aaw.services.CoordinatesPersistenceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of CoordinatesPersistenceService that stores coordinates
 * in separate text files (x.txt and y.txt) in the working directory.
 */
@Service
public class CoordinatesPersistenceServiceImpl implements CoordinatesPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoordinatesPersistenceServiceImpl.class);
    private static final String X_FILE = "x.txt";
    private static final String Y_FILE = "y.txt";
    
    @Override
    public Coordinates loadCoordinates() {
        Path xPath = Paths.get(X_FILE);
        Path yPath = Paths.get(Y_FILE);
        
        int x = loadCoordinateFromFile(xPath, "x", Coordinates.DEFAULT.x());
        int y = loadCoordinateFromFile(yPath, "y", Coordinates.DEFAULT.y());
        
        Coordinates coordinates = new Coordinates(x, y);
        logger.info("Loaded coordinates: {}", coordinates);
        return coordinates;
    }
    
    @Override
    public void saveCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            logger.warn("Attempted to save null coordinates, ignoring");
            return;
        }
        
        saveCoordinateToFile(Paths.get(X_FILE), coordinates.x(), "x");
        saveCoordinateToFile(Paths.get(Y_FILE), coordinates.y(), "y");
        
        logger.debug("Saved coordinates: {}", coordinates);
    }
    
    private int loadCoordinateFromFile(Path path, String coordinateName, int defaultValue) {
        try {
            if (Files.exists(path)) {
                String content = Files.readString(path).trim();
                int value = Integer.parseInt(content);
                logger.debug("Loaded {} = {} from {}", coordinateName, value, path.getFileName());
                return value;
            } else {
                logger.info("{} not found, using default value: {}", path.getFileName(), defaultValue);
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid number format in {}, using default value: {}", path.getFileName(), defaultValue);
            return defaultValue;
        } catch (IOException e) {
            logger.error("Error reading {}: {}", path.getFileName(), e.getMessage());
            return defaultValue;
        }
    }
    
    private void saveCoordinateToFile(Path path, int value, String coordinateName) {
        try {
            Files.writeString(path, String.valueOf(value));
            logger.trace("Saved {} = {} to {}", coordinateName, value, path.getFileName());
        } catch (IOException e) {
            logger.error("Error writing {} to file {}: {}", coordinateName, path.getFileName(), e.getMessage());
        }
    }
}

