package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.service.ChatbotException;
import com.mi.proyecto.ganado.ganadoapp.service.ChatbotAccessDeniedException;
import com.mi.proyecto.ganado.ganadoapp.service.ChatbotService;
import com.mi.proyecto.ganado.ganadoapp.web.ChatErrorResponse;
import com.mi.proyecto.ganado.ganadoapp.web.ChatRequest;
import com.mi.proyecto.ganado.ganadoapp.web.ChatResponse;
import com.mi.proyecto.ganado.ganadoapp.web.ChatTurn;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final String HISTORIAL_SESION = "BOVIBOT_HISTORY";
    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ChatResponse preguntar(@Valid @RequestBody ChatRequest request, Authentication authentication, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<ChatTurn> historial = (List<ChatTurn>) session.getAttribute(HISTORIAL_SESION);
        if (historial == null) historial = new ArrayList<>();
        String respuesta = chatbotService.responder(request.mensaje(), authentication, List.copyOf(historial));
        historial.add(new ChatTurn("user", request.mensaje().trim()));
        historial.add(new ChatTurn("assistant", respuesta));
        if (historial.size() > 12) historial = new ArrayList<>(historial.subList(historial.size() - 12, historial.size()));
        session.setAttribute(HISTORIAL_SESION, historial);
        return new ChatResponse(respuesta);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> limpiarHistorial(HttpSession session) {
        session.removeAttribute(HISTORIAL_SESION);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ChatbotAccessDeniedException.class)
    public ResponseEntity<ChatErrorResponse> manejarAccesoDenegado(ChatbotAccessDeniedException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ChatErrorResponse(error.getMessage()));
    }

    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<ChatErrorResponse> manejarErrorChatbot(ChatbotException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ChatErrorResponse(error.getMessage()));
    }
}
