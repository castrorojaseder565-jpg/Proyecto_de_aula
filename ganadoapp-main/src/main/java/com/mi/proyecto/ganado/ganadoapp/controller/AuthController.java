package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.mode.Usuario;
import com.mi.proyecto.ganado.ganadoapp.service.UsuarioService;
import com.mi.proyecto.ganado.ganadoapp.service.CuentaEstadoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class AuthController {
    private final UsuarioService usuarioService;
    private final CuentaEstadoService cuentaEstadoService;

    public AuthController(UsuarioService usuarioService, CuentaEstadoService cuentaEstadoService) {
        this.usuarioService = usuarioService;
        this.cuentaEstadoService = cuentaEstadoService;
    }

    @GetMapping("/")
    public String bienvenido() { return "bienvenido"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        model.addAttribute("solicitudesPendientes", cuentaEstadoService.listarPendientes());
        return "usuarios-lista";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable Long id, Model model,
                                                 RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.obtenerPorId(id);
            if ("ADMINISTRADOR".equalsIgnoreCase(usuario.getRol())) usuario.setRol("ADMIN");
            usuario.setPassword("");
            model.addAttribute("usuario", usuario);
            return "editar-admin";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/usuarios/editar")
    public String actualizarUsuario(@ModelAttribute("usuario") Usuario usuarioFormulario,
                                    RedirectAttributes redirectAttributes) {
        try {
            usuarioService.editarUsuario(usuarioFormulario);
            redirectAttributes.addFlashAttribute("exito", "Usuario actualizado correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("exito", "Usuario eliminado correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/aprobar")
    public String aprobarVeterinario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.aprobarVeterinario(id);
            redirectAttributes.addFlashAttribute("exito", "Acceso aprobado. El veterinario ya puede iniciar sesión.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
