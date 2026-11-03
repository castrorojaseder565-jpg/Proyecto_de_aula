package com.mi.proyecto.ganado.ganadoapp.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.security.Principal;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler roleAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String rolReal = obtenerRolPrincipal(authentication);
            response.sendRedirect(request.getContextPath() + rutaInicialPorRol(rolReal));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler roleAuthenticationSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro/invitacion", "/registro/invitacion/**", "/css/**", "/js/**", "/images/**", "/webjars/**")
                        .permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/chatbot", "/api/chat/**").hasAnyRole("ADMIN", "VETERINARIO")
                        .requestMatchers("/ganado/nuevo", "/ganado/guardar", "/ganado/existe-arete").hasRole("ADMIN")
                        .requestMatchers("/ganado/editar/**", "/ganado/actualizar", "/ganado/eliminar/**").hasRole("ADMIN")
                        .requestMatchers("/ganado/leche/registrar", "/ganado/leche/eliminar").hasRole("ADMIN")
                        .requestMatchers("/ganado/estadisticas").hasRole("ADMIN")
                        .requestMatchers("/vacuna/registrar", "/vacuna/guardar", "/vacuna/editar/**").hasRole("VETERINARIO")
                        .requestMatchers("/vacuna/actualizar", "/vacuna/eliminar/**").hasRole("VETERINARIO")
                        .requestMatchers("/ganado/lista", "/ganado/detalle/**", "/ganado/prediccion/**", "/ganado/gestion-sanitaria", "/vacuna/todas")
                        .hasAnyRole("ADMIN", "VETERINARIO")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleAuthenticationSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                redirigirAccesoDenegado(response, request.getContextPath(), request.getUserPrincipal()))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    private String obtenerRolPrincipal(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .filter(role -> "ADMIN".equals(role) || "VETERINARIO".equals(role))
                .findFirst()
                .orElse("");
    }

    private String rutaInicialPorRol(String rol) {
        return "/ganado/lista";
    }

    private void redirigirAccesoDenegado(HttpServletResponse response,
                                         String contextPath,
                                         Principal principal) throws IOException {
        if (principal instanceof Authentication authentication) {
            response.sendRedirect(contextPath + rutaInicialPorRol(obtenerRolPrincipal(authentication)) + "?denegado");
            return;
        }
        response.sendRedirect(contextPath + "/login?error");
    }
}
