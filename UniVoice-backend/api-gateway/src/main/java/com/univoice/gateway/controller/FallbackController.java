package com.univoice.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

	@GetMapping("/users")
	public Mono<ResponseEntity<Map<String, String>>> userFallback() {
	    Map<String, String> response = Map.of(
	            "status", "FAILED",
	            "message", "User Service is currently unavailable. Please try again later."
	    );
	    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
	}

	@GetMapping("/chats")
	public Mono<ResponseEntity<Map<String, String>>> chatFallback() {
	    Map<String, String> response = Map.of(
	            "status", "FAILED",
	            "message", "Chat Service is currently unavailable. Please try again later."
	    );
	    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
	}
}