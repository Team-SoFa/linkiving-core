package com.sofa.linkiving.global.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalController {

	@GetMapping("/health-check")
	public String health() {
		return "OK";
	}
}
