package com.ramirez.mediturnosback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediturnosbackApplication {
	private static final Logger log = LoggerFactory.getLogger(MediturnosbackApplication.class);

	public static void main(String[] args) {
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			System.setProperty("java.net.preferIPv4Addresses", "true");
			SpringApplication.run(MediturnosbackApplication.class, args);
		} catch (Exception e) {
			log.error("No se pudo iniciar Mediturnos backend", e);
			System.exit(1);
		}
	}

}
