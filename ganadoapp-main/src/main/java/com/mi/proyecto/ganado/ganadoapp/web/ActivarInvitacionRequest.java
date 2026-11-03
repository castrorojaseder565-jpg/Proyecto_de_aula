package com.mi.proyecto.ganado.ganadoapp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActivarInvitacionRequest {
    @NotBlank
    private String token;

    @NotBlank(message = "Escribe una contraseña.")
    @Size(min = 12, max = 72, message = "Usa entre 12 y 72 caracteres.")
    private String password;

    @NotBlank(message = "Confirma tu contraseña.")
    private String confirmPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
