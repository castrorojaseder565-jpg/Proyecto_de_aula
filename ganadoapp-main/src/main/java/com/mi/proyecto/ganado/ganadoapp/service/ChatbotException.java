package com.mi.proyecto.ganado.ganadoapp.service;

public class ChatbotException extends RuntimeException {
    public ChatbotException(String message) {
        super(message);
    }

    public ChatbotException(String message, Throwable cause) {
        super(message, cause);
    }
}
