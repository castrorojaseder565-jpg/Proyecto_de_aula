package com.mi.proyecto.ganado.ganadoapp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Escribe una pregunta.")
        @Size(max = 1000, message = "La pregunta no puede superar 1000 caracteres.")
        String mensaje
) { }
