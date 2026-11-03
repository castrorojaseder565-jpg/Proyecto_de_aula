package com.mi.proyecto.ganado.ganadoapp.web;

import java.io.Serializable;

public record ChatTurn(String role, String content) implements Serializable {
}
