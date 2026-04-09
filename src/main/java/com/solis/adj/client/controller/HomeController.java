package com.solis.adj.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping("/adjudication")
	public String adjudication() {
		return "adjudication";
	}

	@GetMapping("/ehealth-bc")
	public String ehealthBc() {
		return "ehealth-bc";
	}

}
