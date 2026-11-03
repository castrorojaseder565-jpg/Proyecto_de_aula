package com.mi.proyecto.ganado.ganadoapp.config;

import com.mi.proyecto.ganado.ganadoapp.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdministradorInicialBootstrap {
    private final UsuarioService usuarioService;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;
    @Value("${app.bootstrap-admin.name:}")
    private String nombre;
    @Value("${app.bootstrap-admin.email:}")
    private String correo;
    @Value("${app.bootstrap-admin.password:}")
    private String password;

    public AdministradorInicialBootstrap(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostConstruct
    void crearSoloSiFueConfigurado() {
        if (!enabled || usuarioService.existeAdmin()) {
            return;
        }
        if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
            throw new IllegalStateException("La creación inicial de administrador requiere nombre, correo y contraseña configurados.");
        }
        usuarioService.crearAdministradorInicial(nombre, correo, password);
    }
}
