package com.solis.adj.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.solis.adj.client.util.BCPayloadBuilder;
import com.solis.adj.client.util.NLPayloadBuilder;

@Controller
public class HomeController {

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("bcCount", BCPayloadBuilder.EVENT_CONFIG.size());
		model.addAttribute("nlCount", NLPayloadBuilder.EVENT_CONFIG.size());
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
