package com.JoaoGabriel.vacation_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VacationSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				VacationSchedulerApplication.class,
				args
		);
	}
}