package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import com.mi.proyecto.ganado.ganadoapp.mode.Vacuna;
import com.mi.proyecto.ganado.ganadoapp.service.GanadoService;
import com.mi.proyecto.ganado.ganadoapp.service.VacunaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vacuna")
public class VacunaController {

    private final VacunaService vacunaService;
    private final GanadoService ganadoService;

    public VacunaController(VacunaService vacunaService, GanadoService ganadoService) {
        this.vacunaService = vacunaService;
        this.ganadoService = ganadoService;
    }

    @GetMapping("/registrar")
    public String mostrarFormularioVacuna(Model model,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        if (!esVeterinario(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Veterinario puede registrar vacunas.");
            return "redirect:/ganado/lista";
        }
        if (!model.containsAttribute("vacuna")) {
            model.addAttribute("vacuna", new Vacuna());
        }
        model.addAttribute("ganados", ganadoService.listarGanados());
        return "vacuna-formulario";
    }

    @PostMapping("/guardar")
    public String guardarVacuna(@ModelAttribute Vacuna vacuna,
                                BindingResult result,
                                @RequestParam("ganadoId") String ganadoIdRaw,
                                @RequestParam(value = "nombreOtraVacuna", required = false) String nombreOtraVacuna,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (!esVeterinario(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Veterinario puede registrar vacunas.");
            return "redirect:/ganado/lista";
        }
        Long ganadoId = parseGanadoId(ganadoIdRaw);

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Datos inválidos en el formulario de vacuna.");
            redirectAttributes.addFlashAttribute("vacuna", vacuna);
            redirectAttributes.addFlashAttribute("ganadoId", ganadoId);
            return "redirect:/vacuna/registrar";
        }

        if (ganadoId == null) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar un ganado válido.");
            redirectAttributes.addFlashAttribute("vacuna", vacuna);
            return "redirect:/vacuna/registrar";
        }

        try {
            Ganado ganado = ganadoService.obtenerPorId(ganadoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ganado no encontrado con ID: " + ganadoId));

            vacuna.setGanado(ganado);
            Vacuna vacunaGuardada = vacunaService.guardarVacuna(vacuna, nombreOtraVacuna);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Vacuna '" + vacunaGuardada.getNombre() + "' registrada correctamente para el ganado '" +
                            ganado.getNombre() + "' (" + ganado.getNumeroArete() + ")."
            );
            return "redirect:/ganado/detalle/" + ganadoId;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar vacuna: " + e.getMessage());
            vacuna.setGanado(null);
            redirectAttributes.addFlashAttribute("vacuna", vacuna);
            redirectAttributes.addFlashAttribute("ganadoId", ganadoId);
            return "redirect:/vacuna/registrar";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error inesperado al guardar la vacuna: " + extraerMensajeRaiz(e));
            vacuna.setGanado(null);
            redirectAttributes.addFlashAttribute("vacuna", vacuna);
            redirectAttributes.addFlashAttribute("ganadoId", ganadoId);
            return "redirect:/vacuna/registrar";
        }
    }

    @GetMapping("/editar/{id}")
    public String editarVacuna(@PathVariable Long id,
                               Model model,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!esVeterinario(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Veterinario puede editar vacunas.");
            return "redirect:/ganado/lista";
        }
        Optional<Vacuna> optVacuna = vacunaService.obtenerPorId(id);
        if (optVacuna.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la vacuna con ID " + id + ".");
            return "redirect:/vacuna/todas";
        }

        model.addAttribute("vacuna", optVacuna.get());
        model.addAttribute("ganados", ganadoService.listarGanados());
        return "vacuna-formulario-editar";
    }

    @PostMapping("/actualizar")
    public String actualizarVacuna(@ModelAttribute Vacuna vacunaActualizada,
                                   BindingResult result,
                                   @RequestParam("ganadoId") String ganadoIdRaw,
                                   @RequestParam(value = "nombreOtraVacuna", required = false) String nombreOtraVacuna,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        if (!esVeterinario(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Veterinario puede actualizar vacunas.");
            return "redirect:/ganado/lista";
        }
        Long ganadoId = parseGanadoId(ganadoIdRaw);

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Datos inválidos en el formulario de vacuna.");
            return "redirect:/vacuna/editar/" + vacunaActualizada.getId();
        }

        if (ganadoId == null) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar un ganado válido.");
            return "redirect:/vacuna/editar/" + vacunaActualizada.getId();
        }

        try {
            Ganado ganado = ganadoService.obtenerPorId(ganadoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ganado no encontrado con ID: " + ganadoId));

            vacunaActualizada.setGanado(ganado);
            Vacuna actualizada = vacunaService.actualizarVacuna(
                    vacunaActualizada.getId(),
                    vacunaActualizada,
                    nombreOtraVacuna
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Vacuna '" + actualizada.getNombre() + "' actualizada correctamente para el ganado '" +
                            ganado.getNombre() + "'."
            );
            return "redirect:/ganado/detalle/" + ganadoId;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vacuna/editar/" + vacunaActualizada.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error inesperado al actualizar la vacuna: " + extraerMensajeRaiz(e));
            return "redirect:/ganado/detalle/" + ganadoId;
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarVacuna(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (!esVeterinario(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Veterinario puede eliminar vacunas.");
            return "redirect:/ganado/lista";
        }
        Optional<Vacuna> vacunaOpt = vacunaService.obtenerPorId(id);
        if (vacunaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la vacuna a eliminar.");
            return "redirect:/ganado/lista";
        }

        Vacuna vacuna = vacunaOpt.get();
        Long ganadoId = vacuna.getGanado().getId();

        try {
            vacunaService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Vacuna '" + vacuna.getNombre() + "' eliminada correctamente.");
            return "redirect:/ganado/detalle/" + ganadoId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar la vacuna: " + extraerMensajeRaiz(e));
            return "redirect:/ganado/detalle/" + ganadoId;
        }
    }

    @GetMapping("/todas")
    public String mostrarTodasLasVacunas(Model model) {
        List<Vacuna> todas = vacunaService.obtenerTodas();
        model.addAttribute("vacunas", todas);
        return "vacunas-todas";
    }

    private String extraerMensajeRaiz(Throwable throwable) {
        Throwable actual = throwable;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        if (actual.getMessage() == null || actual.getMessage().isBlank()) {
            return "Sin detalle tecnico disponible.";
        }
        return actual.getMessage();
    }

    private Long parseGanadoId(String ganadoIdRaw) {
        if (ganadoIdRaw == null || ganadoIdRaw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(ganadoIdRaw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean esVeterinario(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_VETERINARIO".equals(a.getAuthority()));
    }
}
