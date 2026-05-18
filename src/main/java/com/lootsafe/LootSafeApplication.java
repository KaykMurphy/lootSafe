package com.lootsafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LootSafeApplication {

	public static void main(String[] args) {
		SpringApplication.run(LootSafeApplication.class, args);
	}

}
