package com.ramirez.mediturnosback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediturnosbackApplication {

	public static void main(String[] args) {
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			System.setProperty("java.net.preferIPv4Addresses", "true");
			SpringApplication.run(MediturnosbackApplication.class, args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
