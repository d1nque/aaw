package vyrib1.project.aaw;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import vyrib1.project.aaw.services.GuiService;

@SpringBootApplication
public class AawApplication {

    private final GuiService guiService;

    public AawApplication(GuiService guiService) {
        this.guiService = guiService;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(AawApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            guiService.startGui();
        };
    }

}
