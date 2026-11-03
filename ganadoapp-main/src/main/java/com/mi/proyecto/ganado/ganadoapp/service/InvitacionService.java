package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.mode.Invitacion;
import com.mi.proyecto.ganado.ganadoapp.repository.InvitacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class InvitacionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final InvitacionRepository invitacionRepository;
    private final UsuarioService usuarioService;
    private final JavaMailSender mailSender;

    @Value("${app.invitation.base-url:}")
    private String baseUrl;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.invitation.allow-local-url:false}")
    private boolean allowLocalUrl;

    public InvitacionService(InvitacionRepository invitacionRepository, UsuarioService usuarioService,
                             JavaMailSender mailSender) {
        this.invitacionRepository = invitacionRepository;
        this.usuarioService = usuarioService;
        this.mailSender = mailSender;
    }

    @Transactional
    public void invitarVeterinario(String nombre, String correo) {
        validarConfiguracion();
        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        String correoNormalizado = normalizarCorreo(correo);
        if (nombreNormalizado.isBlank() || nombreNormalizado.length() > 120) {
            throw new IllegalArgumentException("Escribe el nombre del veterinario (máximo 120 caracteres).");
        }
        if (!CORREO_VALIDO.matcher(correoNormalizado).matches() || correoNormalizado.length() > 254) {
            throw new IllegalArgumentException("Escribe un correo electrónico válido.");
        }
        if (usuarioService.existeCorreo(correoNormalizado)) {
            throw new IllegalArgumentException("Ya existe una cuenta asociada a ese correo.");
        }

        invitacionRepository.deleteByCorreoIgnoreCaseAndUsadaEnIsNull(correoNormalizado);
        String token = generarToken();
        LocalDateTime ahora = LocalDateTime.now();
        Invitacion invitacion = new Invitacion();
        invitacion.setNombre(nombreNormalizado);
        invitacion.setCorreo(correoNormalizado);
        invitacion.setTokenHash(hash(token));
        invitacion.setCreadaEn(ahora);
        invitacion.setExpiraEn(ahora.plusHours(48));
        invitacionRepository.save(invitacion);

        enviarCorreo(nombreNormalizado, correoNormalizado, token);
    }

    @Transactional(readOnly = true)
    public Optional<InvitacionPublica> buscarInvitacionValida(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return invitacionRepository.findByTokenHashAndUsadaEnIsNullAndExpiraEnAfter(hash(token), LocalDateTime.now())
                .map(invitacion -> new InvitacionPublica(invitacion.getNombre(), invitacion.getCorreo()));
    }

    @Transactional
    public void activarCuenta(String token, String password) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El enlace no es válido, ya fue usado o venció.");
        }
        Invitacion invitacion = invitacionRepository.buscarVigenteParaActivacion(hash(token), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido, ya fue usado o venció."));
        usuarioService.registrarVeterinario(invitacion.getNombre(), invitacion.getCorreo(), password);
        invitacion.setUsadaEn(LocalDateTime.now());
        invitacionRepository.save(invitacion);
    }

    private void validarConfiguracion() {
        if (mailHost == null || mailHost.isBlank() || mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank() || mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("Configura MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD y MAIL_FROM en .env para enviar invitaciones por correo.");
        }
        try {
            URI uri = URI.create(baseUrl == null ? "" : baseUrl.trim());
            String host = uri.getHost();
            boolean localHttpTest = allowLocalUrl
                    && "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                    && uri.getPort() == 8081;
            boolean publicHttps = "https".equalsIgnoreCase(uri.getScheme())
                    && host != null && !host.isBlank()
                    && !host.equalsIgnoreCase("localhost") && !host.endsWith(".local")
                    && !host.equals("127.0.0.1") && !host.equals("0.0.0.0");
            if (!localHttpTest && !publicHttps) {
                throw new IllegalStateException("Para pruebas locales usa APP_BASE_URL=http://localhost:8081 y habilita APP_ALLOW_LOCAL_URL=true. Para uso real, configura una URL pública HTTPS.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("APP_BASE_URL no es válido. Usa http://localhost:8081 para pruebas locales o una dirección pública HTTPS.");
        }
    }

    private void enviarCorreo(String nombre, String correo, String token) {
        String enlace = baseUrl.trim().replaceAll("/+$", "") + "/registro/invitacion/" + token;
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(mailFrom.trim());
        mensaje.setTo(correo);
        mensaje.setSubject("Invitación a BoviControl como veterinario");
        mensaje.setText("Hola, " + nombre + ".\n\n"
                + "Has sido invitado a formar parte de BoviControl como veterinario.\n\n"
                + "Para activar tu cuenta y crear tu contraseña, abre este enlace:\n"
                + enlace + "\n\n"
                + "El enlace es personal, de un solo uso y vence en 48 horas.\n"
                + "Si no esperabas esta invitación, puedes ignorar este correo.");
        try {
            mailSender.send(mensaje);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo enviar el correo. Verifica las credenciales SMTP y la dirección remitente configuradas.", e);
        }
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger el token de invitación.", e);
        }
    }

    public record InvitacionPublica(String nombre, String correo) { }
}
