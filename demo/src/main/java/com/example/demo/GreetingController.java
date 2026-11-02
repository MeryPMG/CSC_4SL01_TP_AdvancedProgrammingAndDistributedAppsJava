package com.example.demo;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();
	
//	The @GetMapping annotation ensures that HTTP GET requests to 
//	/greeting are mapped to the greeting() method.
	@GetMapping("/greeting")
	public Greeting greeting(@RequestParam(defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), template.formatted(name));
	}
}
