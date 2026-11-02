package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import org.springframework.boot.ApplicationRunner;

@SpringBootApplication
public class Exercise3Tutorial3Application {
	private static final Logger log = LoggerFactory.getLogger(Exercise3Tutorial3Application.class);
	
	public static void main(String[] args) {
		SpringApplication.run(Exercise3Tutorial3Application.class, args);
	}

	@Bean
	@Profile("!test")
	public ApplicationRunner run(RestClient.Builder builder) {
		RestClient restClient = builder.baseUrl("http://localhost:90").build();
		return args -> {
			Quote quote = restClient.get().uri("/api/random").retrieve().body(Quote.class);
			log.info(quote.toString());
		};
	}
}
