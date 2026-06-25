package com.hmbrandt.delay_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DelayTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DelayTrackerApplication.class, args);
	}

}
