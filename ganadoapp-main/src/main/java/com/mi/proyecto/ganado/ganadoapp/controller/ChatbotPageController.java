package com.mi.proyecto.ganado.ganadoapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatbotPageController {
    @GetMapping("/chatbot")
    public String mostrarChatbot() {
        return "redirect:/ganado/lista?bovibot=open";
    }
}
