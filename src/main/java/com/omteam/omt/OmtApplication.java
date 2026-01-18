package com.omteam.omt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class OmtApplication {

	public static void main(String[] args) {
		SpringApplication.run(OmtApplication.class, args);
	}

}
