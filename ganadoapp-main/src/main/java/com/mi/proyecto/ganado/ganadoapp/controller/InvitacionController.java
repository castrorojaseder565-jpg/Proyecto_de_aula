package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.service.InvitacionService;
import com.mi.proyecto.ganado.ganadoapp.service.InvitacionService.InvitacionPublica;
import com.mi.proyecto.ganado.ganadoapp.web.ActivarInvitacionRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class InvitacionController {
    private final InvitacionService invitacionService;

    public InvitacionController(InvitacionService invitacionService) {
        this.invitacionService = invitacionService;
    }

    @GetMapping("/usuarios/invitaciones")
    public String formularioInvitacion() {
        return "invitaciones";
    }

    @PostMapping("/usuarios/invitaciones")
    public String invitarVeterinario(@RequestParam String nombre, @RequestParam String correo,
                                     RedirectAttributes redirectAttributes) {
        try {
            invitacionService.invitarVeterinario(nombre, correo);
            redirectAttributes.addFlashAttribute("exito", "Invitación enviada a " + correo.trim() + ". El enlace vence en 48 horas.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios/invitaciones";
    }

    @GetMapping({"/registro/invitacion", "/registro/invitacion/{token}"})
    public String mostrarActivacion(@PathVariable(required = false) String token,
                                    @RequestParam(required = false) String queryToken, Model model) {
        if (token == null || token.isBlank()) token = queryToken;
        Optional<InvitacionPublica> invitacion = invitacionService.buscarInvitacionValida(token);
        if (invitacion.isEmpty()) {
            model.addAttribute("error", "El enlace no es válido, ya fue utilizado o venció.");
            return "registro-invitacion";
        }
        model.addAttribute("invitacion", invitacion.get());
        ActivarInvitacionRequest solicitud = new ActivarInvitacionRequest();
        solicitud.setToken(token);
        model.addAttribute("solicitud", solicitud);
        return "registro-invitacion";
    }

    @PostMapping("/registro/invitacion")
    public String activarCuenta(@Valid @ModelAttribute("solicitud") ActivarInvitacionRequest solicitud,
                                BindingResult resultado, Model model, RedirectAttributes redirectAttributes) {
        Optional<InvitacionPublica> invitacion = invitacionService.buscarInvitacionValida(solicitud.getToken());
        if (invitacion.isEmpty()) {
            model.addAttribute("error", "El enlace no es válido, ya fue utilizado o venció.");
            return "registro-invitacion";
        }
        model.addAttribute("invitacion", invitacion.get());
        if (!java.util.Objects.equals(solicitud.getPassword(), solicitud.getConfirmPassword())) {
            resultado.rejectValue("confirmPassword", "password.mismatch", "Las contraseñas no coinciden.");
        }
        if (resultado.hasErrors()) return "registro-invitacion";

        try {
            invitacionService.activarCuenta(solicitud.getToken(), solicitud.getPassword());
            redirectAttributes.addFlashAttribute("mensaje", "Tu cuenta de veterinario quedó activada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "registro-invitacion";
        }
    }
}
