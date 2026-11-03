package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.mode.Usuario;
import com.mi.proyecto.ganado.ganadoapp.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CuentaEstadoService cuentaEstadoService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          CuentaEstadoService cuentaEstadoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cuentaEstadoService = cuentaEstadoService;
    }

    private void registrarUsuario(Usuario usuario) {
        usuario.setCorreo(normalizarCorreo(usuario.getCorreo()));
        if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo ya esta registrado.");
        }

        String rolNormalizado = normalizarRol(usuario.getRol());
        usuario.setRol(rolNormalizado);

        if ("ADMIN".equals(rolNormalizado) && existeAdmin()) {
            throw new RuntimeException("Ya existe un administrador registrado.");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.findByCorreo(normalizarCorreo(correo)).isPresent();
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    public void registrarVeterinario(String nombre, String correo, String password) {
        validarPasswordSegura(password);
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre == null ? "" : nombre.trim());
        usuario.setCorreo(normalizarCorreo(correo));
        usuario.setPassword(password);
        usuario.setRol("VETERINARIO");
        registrarUsuario(usuario);
    }

    public void aprobarVeterinario(Long id) {
        Usuario usuario = obtenerPorId(id);
        if (!"VETERINARIO".equals(normalizarRol(usuario.getRol()))) {
            throw new IllegalArgumentException("Solo se pueden aprobar cuentas de veterinario.");
        }
        cuentaEstadoService.aprobar(id);
    }

    public void crearAdministradorInicial(String nombre, String correo, String password) {
        if (existeAdmin()) {
            return;
        }
        validarPasswordSegura(password);
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre == null ? "" : nombre.trim());
        usuario.setCorreo(normalizarCorreo(correo));
        usuario.setPassword(password);
        usuario.setRol("ADMIN");
        registrarUsuario(usuario);
    }

    public void editarUsuario(Usuario usuarioActualizado) {
        Usuario usuarioDB = usuarioRepository.findById(usuarioActualizado.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String correoNormalizado = normalizarCorreo(usuarioActualizado.getCorreo());
        usuarioRepository.findByCorreo(correoNormalizado)
                .filter(usuarioConCorreo -> !usuarioConCorreo.getId().equals(usuarioDB.getId()))
                .ifPresent(usuarioConCorreo -> {
                    throw new RuntimeException("El correo ya está registrado.");
                });
        String nuevoRol = normalizarRol(usuarioActualizado.getRol());
        if ("ADMIN".equals(normalizarRol(usuarioDB.getRol()))
                && !"ADMIN".equals(nuevoRol) && contarAdmins() <= 1) {
            throw new RuntimeException("No se puede cambiar el rol del último administrador.");
        }

        usuarioDB.setNombre(usuarioActualizado.getNombre());
        usuarioDB.setCorreo(correoNormalizado);
        usuarioDB.setRol(nuevoRol);

        if (usuarioActualizado.getPassword() != null && !usuarioActualizado.getPassword().isBlank()) {
            usuarioDB.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
        }

        usuarioRepository.save(usuarioDB);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if ("ADMIN".equals(normalizarRol(usuario.getRol())) && contarAdmins() <= 1) {
            throw new RuntimeException("No se puede eliminar el último administrador.");
        }
        usuarioRepository.deleteById(id);
    }

    public boolean existeAdmin() {
        return contarAdmins() > 0;
    }

    private long contarAdmins() {
        return usuarioRepository.findAll().stream()
                .map(Usuario::getRol)
                .filter(rol -> "ADMIN".equals(normalizarRol(rol)))
                .count();
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        String rolSpring = normalizarRol(usuario.getRol());

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles(rolSpring)
                .disabled(!cuentaEstadoService.estaActivo(usuario.getId()))
                .build();
    }

    private String normalizarRol(String rol) {
        if (rol == null || rol.isBlank()) {
            return "VETERINARIO";
        }
        String valor = rol.trim().toUpperCase(Locale.ROOT);
        if ("ADMINISTRADOR".equals(valor) || "ADMIN".equals(valor)) {
            return "ADMIN";
        }
        return "VETERINARIO";
    }

    private String normalizarCorreo(String correo) {
        if (correo == null) {
            return "";
        }
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void validarPasswordSegura(String password) {
        if (password == null || password.length() < 12 || password.length() > 72
                || !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("La contraseña no cumple la política de seguridad.");
        }
    }
}
