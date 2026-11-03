package com.mi.proyecto.ganado.ganadoapp.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroUsuarioRequest {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;
    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
    private String correo;
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres.")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*", message = "Usa mayúscula, minúscula, número y símbolo.")
    private String password;
    @NotBlank(message = "Confirma la contraseña.")
    private String confirmPassword;
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
