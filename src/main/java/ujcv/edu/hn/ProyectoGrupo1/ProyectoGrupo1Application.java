package ujcv.edu.hn.ProyectoGrupo1;

import org.openxava.util.DBServer;
import org.openxava.spring.OpenXavaApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal para arrancar la aplicaci�n.
 *
 * Ejec�tala directamente desde tu IDE o usa
 * <code>mvn spring-boot:run</code> desde la l�nea de �rdenes.
 */
@SpringBootApplication
public class ProyectoGrupo1Application extends OpenXavaApplication {

	public static void main(String[] args) throws Exception {
		DBServer.start("ProyectoGrupo1-db"); // Para usar tu propia base de datos comenta esta l�nea y configura src/main/resources/application.properties
		SpringApplication.run(ProyectoGrupo1Application.class, args);
	}

}
