package com.ecopizza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EcoPizzaApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcoPizzaApplication.class, args);
	}

}
