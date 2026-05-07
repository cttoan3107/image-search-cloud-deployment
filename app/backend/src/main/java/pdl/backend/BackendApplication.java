/*
  Global role of this class:

  - Entry point of the Spring Boot application
  - Starts and initializes the backend server
*/
package pdl.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
