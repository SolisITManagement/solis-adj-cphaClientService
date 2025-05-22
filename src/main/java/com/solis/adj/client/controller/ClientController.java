package com.solis.adj.client.controller;

import com.solis.adj.client.service.CphaClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ClientController {

    @Autowired
    private CphaClientService clientService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/submit")
    public String submitJson(@RequestParam("jsonData") String jsonData, Model model) {
        String response = clientService.sendJsonToPublisher(jsonData);
        model.addAttribute("response", response);
        return "index";
    }
}
