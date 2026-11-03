package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.mode.DatosGraficoLeche;
import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import com.mi.proyecto.ganado.ganadoapp.mode.Vacuna;
import com.mi.proyecto.ganado.ganadoapp.service.GanadoService;
import com.mi.proyecto.ganado.ganadoapp.service.LecheTrazabilidadService;
import com.mi.proyecto.ganado.ganadoapp.service.VacunaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/ganado")
public class GanadoController {

    @Autowired
    private GanadoService ganadoService;

    @Autowired
    private VacunaService vacunaService;

    @Autowired
    private LecheTrazabilidadService lecheTrazabilidadService;

    @GetMapping("/nuevo")
    public String mostrarFormularioGanado(Model model) {
        if (!model.containsAttribute("ganado")) {
            Ganado ganado = new Ganado();
            ganado.setMarcaHierro("CR");
            ganado.setPropietario("Eder Castro");
            model.addAttribute("ganado", ganado);
        }

        Ganado ganadoFormulario = (Ganado) model.asMap().get("ganado");
        prepararContextoFormularioRegistro(model, ganadoFormulario);
        return "registro-ganado";
    }

    @GetMapping("/existe-arete")
    @ResponseBody
    public boolean existeNumeroArete(@RequestParam String numeroArete) {
        return ganadoService.existeNumeroArete(numeroArete);
    }

    @GetMapping("/lista")
    public String listarGanado(@RequestParam(name = "buscar", required = false) String buscar,
                               Model model) {
        List<Ganado> listaGanado = ganadoService.buscarPorNombre(buscar);
        model.addAttribute("listaGanado", listaGanado);
        model.addAttribute("filtro", buscar);
        return "lista-ganado";
    }

    @GetMapping("/detalle/{id}")
    public String detalleGanado(@PathVariable Long id,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Optional<Ganado> optGanado = ganadoService.obtenerPorId(id);

        if (optGanado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró el ganado con ID " + id);
            return "redirect:/ganado/lista";
        }

        Ganado ganado = optGanado.get();
        List<Vacuna> vacunas = vacunaService.obtenerVacunasPorGanado(id);
        if (vacunas == null) {
            vacunas = List.of();
        }

        model.addAttribute("ganado", ganado);
        model.addAttribute("vacunas", vacunas);
        model.addAttribute("edadFormateada", ganadoService.formatoEdadParaVista(ganado.getEdad()));
        model.addAttribute("reporteLimites", ganadoService.generarReporteDeLimites(ganado));
        model.addAttribute("recomendacionNutricional", ganadoService.generarRecomendacionNutricional(ganado));
        model.addAttribute("recomendacionBienestar", ganadoService.generarRecomendacionBienestar(ganado));
        model.addAttribute("recomendacionVacuna", vacunaService.generarRecomendacionPorRaza(ganado.getRaza()));
        model.addAttribute("sugerenciasManejo", generarSugerenciasManejo(ganado, vacunas));
        model.addAttribute("planAlimentacion", ganadoService.generarPlanAlimentacionRigido(ganado));
        model.addAttribute("historialLeche", lecheTrazabilidadService.obtenerProduccionDiariaGanadoReal(id, 30));
        model.addAttribute("registrosLeche", lecheTrazabilidadService.obtenerHistorialGanado(id, 20));
        model.addAttribute("leche7Dias", lecheTrazabilidadService.calcularLechePeriodoGanado(id, 7));
        model.addAttribute("leche30Dias", lecheTrazabilidadService.calcularLechePeriodoGanado(id, 30));
        model.addAttribute("prediccionLecheManana", lecheTrazabilidadService.predecirLitrosManana(id));
        model.addAttribute("prediccionLeche7Dias", lecheTrazabilidadService.predecirProximaSemana(id));
        model.addAttribute("prediccionLeche30Dias", lecheTrazabilidadService.predecirProximoMes(id));
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("periodo7Dias", construirRango(LocalDate.now().minusDays(6), LocalDate.now()));
        model.addAttribute("periodo30Dias", construirRango(LocalDate.now().minusDays(29), LocalDate.now()));

        LocalDate hoy = LocalDate.now();
        Vacuna proximaVacuna = vacunas.stream()
                .filter(v -> v.getProximaDosis() != null && !v.getProximaDosis().isBefore(hoy))
                .min(Comparator.comparing(Vacuna::getProximaDosis))
                .orElse(null);

        model.addAttribute("proximaVacuna", proximaVacuna);
        model.addAttribute("diasProximaVacuna",
                proximaVacuna == null ? null : ChronoUnit.DAYS.between(hoy, proximaVacuna.getProximaDosis()));

        return "detalle-ganado";
    }

    @GetMapping("/prediccion/{id}")
    public String verPrediccionLeche(@PathVariable Long id,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Optional<Ganado> optGanado = ganadoService.obtenerPorId(id);
        if (optGanado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró el ganado con ID " + id);
            return "redirect:/ganado/lista";
        }

        Ganado ganado = optGanado.get();
        DatosGraficoLeche graficoVacio = DatosGraficoLeche.vacio();
        model.addAttribute("ganado", ganado);
        model.addAttribute("graficoLeche", graficoVacio);
        model.addAttribute("chartEtiquetas", graficoVacio.getEtiquetas());
        model.addAttribute("chartProduccionReal", graficoVacio.getProduccionReal());
        model.addAttribute("chartLineaRegresion", graficoVacio.getLineaRegresion());
        model.addAttribute("chartRegresionDisponible", false);
        model.addAttribute("prediccionLecheManana", lecheTrazabilidadService.predecirLitrosManana(id));
        model.addAttribute("prediccionLeche7Dias", lecheTrazabilidadService.predecirProximaSemana(id));
        model.addAttribute("prediccionLeche30Dias", lecheTrazabilidadService.predecirProximoMes(id));
        model.addAttribute("leche7Dias", lecheTrazabilidadService.calcularLechePeriodoGanado(id, 7));
        model.addAttribute("leche30Dias", lecheTrazabilidadService.calcularLechePeriodoGanado(id, 30));
        model.addAttribute("historialLeche", lecheTrazabilidadService.obtenerProduccionDiariaGanadoReal(id, 30));
        model.addAttribute("registrosLeche", lecheTrazabilidadService.obtenerHistorialGanado(id, 20));
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("periodo7Dias", construirRango(LocalDate.now().minusDays(6), LocalDate.now()));
        model.addAttribute("periodo30Dias", construirRango(LocalDate.now().minusDays(29), LocalDate.now()));
        agregarGraficoLecheAlModelo(model, id);
        return "prediccion-leche";
    }

    private void agregarGraficoLecheAlModelo(Model model, Long ganadoId) {
        DatosGraficoLeche grafico = lecheTrazabilidadService.obtenerDatosGraficoPrediccion(ganadoId);
        if (grafico == null) {
            grafico = DatosGraficoLeche.vacio();
        }
        model.addAttribute("graficoLeche", grafico);
        model.addAttribute("chartEtiquetas", grafico.getEtiquetas());
        model.addAttribute("chartProduccionReal", grafico.getProduccionReal());
        model.addAttribute("chartLineaRegresion", grafico.getLineaRegresion());
        model.addAttribute("chartRegresionDisponible", grafico.isRegresionDisponible());
    }

    @GetMapping("/gestion-sanitaria")
    public String mostrarGestionSanitaria(Model model) {
        List<Ganado> ganados = ganadoService.listarGanados();
        List<Vacuna> vacunas = vacunaService.obtenerTodas();
        LocalDate hoy = LocalDate.now();
        LocalDate limiteProximas = hoy.plusDays(15);

        List<Ganado> ganadoEnfermo = ganados.stream()
                .filter(g -> g.getEstadoSalud() != null && !"Saludable".equalsIgnoreCase(g.getEstadoSalud()))
                .toList();

        List<Vacuna> vacunasVencidas = vacunas.stream()
                .filter(v -> v.getProximaDosis() != null && v.getProximaDosis().isBefore(hoy))
                .toList();

        List<Vacuna> vacunasProximas = vacunas.stream()
                .filter(v -> v.getProximaDosis() != null
                        && !v.getProximaDosis().isBefore(hoy)
                        && !v.getProximaDosis().isAfter(limiteProximas))
                .toList();

        model.addAttribute("ganados", ganados);
        model.addAttribute("ganadoEnfermo", ganadoEnfermo);
        model.addAttribute("vacunasVencidas", vacunasVencidas);
        model.addAttribute("vacunasProximas", vacunasProximas);
        model.addAttribute("totalGanado", ganados.size());
        model.addAttribute("totalEnMonitoreo", ganadoEnfermo.size());
        model.addAttribute("totalVacunasVencidas", vacunasVencidas.size());
        model.addAttribute("totalVacunasProximas", vacunasProximas.size());
        model.addAttribute("sugerenciasSanitarias",
                generarSugerenciasSanitarias(ganadoEnfermo.size(), vacunasVencidas.size(), vacunasProximas.size()));
        model.addAttribute("planesAlimentacion", ganadoService.generarPlanesAlimentacionRigidos(ganados));
        model.addAttribute("totalLecheDiaria", lecheTrazabilidadService.totalLecheDiariaHato());
        model.addAttribute("totalLeche7Dias", lecheTrazabilidadService.totalLecheHatoPeriodo(7));
        model.addAttribute("totalLeche30Dias", lecheTrazabilidadService.totalLecheHatoPeriodo(30));
        model.addAttribute("prediccionLecheHato7Dias", lecheTrazabilidadService.prediccionLecheHato(7));
        return "gestion-sanitaria";
    }

    @PostMapping("/leche/registrar")
    public String registrarLeche(@RequestParam("ganadoId") Long ganadoId,
                                 @RequestParam("fechaRegistro") String fechaRegistro,
                                 @RequestParam("turno") String turno,
                                 @RequestParam("litros") Double litros,
                                 @RequestParam(value = "observacion", required = false) String observacion,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        boolean puedeRegistrar = authentication != null
                && authentication.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()));
        if (!puedeRegistrar) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para registrar leche. Solo es de consulta.");
            return "redirect:/ganado/prediccion/" + ganadoId;
        }

        try {
            LocalDate fecha = LocalDate.parse(fechaRegistro);
            lecheTrazabilidadService.registrarProduccion(ganadoId, fecha, turno, litros, observacion);
            redirectAttributes.addFlashAttribute("mensaje", "Registro de leche guardado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el registro de leche.");
        }
        return "redirect:/ganado/prediccion/" + ganadoId;
    }

    @PostMapping("/leche/eliminar")
    public String eliminarRegistroLeche(@RequestParam("registroId") Long registroId,
                                        @RequestParam("ganadoId") Long ganadoId,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        boolean esAdmin = authentication != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!esAdmin) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar registros de leche.");
            return "redirect:/ganado/prediccion/" + ganadoId;
        }
        try {
            lecheTrazabilidadService.eliminarRegistro(registroId);
            redirectAttributes.addFlashAttribute("mensaje", "Registro de leche eliminado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el registro de leche.");
        }
        return "redirect:/ganado/prediccion/" + ganadoId;
    }

    @PostMapping("/guardar")
    public String guardarGanado(@ModelAttribute Ganado ganado,
                                @RequestParam(value = "otraRaza", required = false) String otraRaza,
                                RedirectAttributes redirectAttributes) {

        try {
            aplicarRazaDesdeFormulario(ganado, otraRaza);
            ganado.setPropietario("Eder Castro");
            ganado.setMarcaHierro("CR");

            Ganado guardado = ganadoService.guardarGanado(ganado);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Ganado registrado exitosamente: " + guardado.getNombre() + " (" + guardado.getNumeroArete() + ")");
            return "redirect:/ganado/detalle/" + guardado.getId();
        } catch (IllegalArgumentException e) {
            prepararRetornoFormularioRegistro(redirectAttributes, ganado, otraRaza, null, null);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ganado/nuevo";
        } catch (Exception e) {
            prepararRetornoFormularioRegistro(redirectAttributes, ganado, otraRaza, null, null);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el registro: " + e.getMessage());
            return "redirect:/ganado/nuevo";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable Long id,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        Optional<Ganado> optGanado = ganadoService.obtenerPorId(id);

        if (optGanado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró el ganado con ID " + id);
            return "redirect:/ganado/lista";
        }

        model.addAttribute("ganado", optGanado.get());
        return "editar-ganado";
    }

    @PostMapping("/actualizar")
    public String actualizarGanado(@ModelAttribute Ganado ganado,
                                   @RequestParam(value = "otraRaza", required = false) String otraRaza,
                                   RedirectAttributes redirectAttributes) {

        try {
            aplicarRazaDesdeFormulario(ganado, otraRaza);
            ganado.setPropietario("Eder Castro");
            ganado.setMarcaHierro("CR");

            ganadoService.actualizarGanado(ganado.getId(), ganado);
            redirectAttributes.addFlashAttribute("mensaje", "Ganado actualizado correctamente.");
            return "redirect:/ganado/detalle/" + ganado.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("ganado", ganado);
            return "redirect:/ganado/editar/" + ganado.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("ganado", ganado);
            return "redirect:/ganado/editar/" + ganado.getId();
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarGanado(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            ganadoService.eliminarGanado(id);
            redirectAttributes.addFlashAttribute("mensaje", "Ganado eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/ganado/lista";
    }

    private void configurarEdadDesdeFormulario(Ganado ganado, Double edadValor, String edadUnidad) {
        if (edadValor == null || edadValor < 0) {
            return;
        }

        if (edadUnidad == null || edadUnidad.isBlank()) {
            ganado.setEdad(edadValor);
            return;
        }

        String unidadNormalizada = normalizarUnidadFormulario(edadUnidad).toLowerCase();

        double edadEnAnios = switch (unidadNormalizada) {
            case "dias" -> edadValor / 365.25;
            case "meses" -> edadValor / 12;
            default -> edadValor;
        };

        ganado.setEdad(edadEnAnios);
    }

    private void aplicarRazaDesdeFormulario(Ganado ganado, String otraRaza) {
        if (ganado.getRaza() == null || ganado.getRaza().isBlank()) {
            throw new IllegalArgumentException("Debes seleccionar una raza.");
        }

        if ("Otros".equalsIgnoreCase(ganado.getRaza())) {
            if (otraRaza == null || otraRaza.isBlank()) {
                throw new IllegalArgumentException("Si eliges 'Otro o mestizo', debes indicar la raza o cruce.");
            }
            ganado.setRaza(otraRaza.trim());
        }
    }

    private void prepararRetornoFormularioRegistro(RedirectAttributes redirectAttributes,
                                                   Ganado ganado,
                                                   String otraRaza,
                                                   Double edadValor,
                                                   String edadUnidad) {
        if (ganado == null) {
            return;
        }

        String razaRegistrada = ganado.getRaza();
        if ("Otros".equalsIgnoreCase(razaRegistrada) && otraRaza != null && !otraRaza.isBlank()) {
            redirectAttributes.addFlashAttribute("otraRaza", otraRaza.trim());
        } else if (esRazaPersonalizada(razaRegistrada)) {
            redirectAttributes.addFlashAttribute("otraRaza", razaRegistrada);
            ganado.setRaza("Otros");
        }

        if (edadValor != null && edadValor >= 0) {
            redirectAttributes.addFlashAttribute("edadValor", edadValor);
        }
        if (edadUnidad != null && !edadUnidad.isBlank()) {
            redirectAttributes.addFlashAttribute("edadUnidad", normalizarUnidadFormulario(edadUnidad));
        }

        redirectAttributes.addFlashAttribute("ganado", ganado);
    }

    private void prepararContextoFormularioRegistro(Model model, Ganado ganado) {
        if (ganado == null) {
            return;
        }

        if (!model.containsAttribute("otraRaza")) {
            model.addAttribute("otraRaza", esRazaPersonalizada(ganado.getRaza()) ? ganado.getRaza() : "");
        }

        if (esRazaPersonalizada(ganado.getRaza())) {
            ganado.setRaza("Otros");
        }

        if (!model.containsAttribute("edadUnidad")) {
            String unidad = normalizarUnidadFormulario(ganado.getUnidadTiempoEdad());
            if ("Anios".equalsIgnoreCase(unidad) && ganado.getEdad() > 0 && ganado.getEdad() < 1) {
                unidad = "Meses";
            }
            model.addAttribute("edadUnidad", unidad);
        }

        if (!model.containsAttribute("edadValor")) {
            String unidad = String.valueOf(model.asMap().get("edadUnidad"));
            double edadValor = convertirEdadParaFormulario(ganado.getEdad(), unidad);
            model.addAttribute("edadValor", edadValor);
        }
    }

    private boolean esRazaPersonalizada(String raza) {
        if (raza == null || raza.isBlank()) {
            return false;
        }
        Set<String> razasCatalogo = Set.of("Holstein", "Brahman", "Angus", "Jersey", "Hereford", "Charolais", "Otros");
        return razasCatalogo.stream().noneMatch(item -> item.equalsIgnoreCase(raza.trim()));
    }

    private String normalizarUnidadFormulario(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return "Anios";
        }
        String valor = unidad.trim().toLowerCase();
        if (valor.startsWith("dia")) {
            return "Dias";
        }
        if (valor.startsWith("mes")) {
            return "Meses";
        }
        return "Anios";
    }

    private double convertirEdadParaFormulario(double edadEnAnios, String unidad) {
        String valorUnidad = normalizarUnidadFormulario(unidad);
        return switch (valorUnidad) {
            case "Dias" -> Math.round(edadEnAnios * 365.25 * 100.0) / 100.0;
            case "Meses" -> Math.round(edadEnAnios * 12 * 100.0) / 100.0;
            default -> Math.round(edadEnAnios * 100.0) / 100.0;
        };
    }

    private List<String> generarSugerenciasManejo(Ganado ganado, List<Vacuna> vacunas) {
        List<String> sugerencias = new ArrayList<>();

        if (ganado == null) {
            sugerencias.add("Completa la ficha del animal para generar recomendaciones.");
            return sugerencias;
        }

        if (ganado.getRaza() != null && !ganado.getRaza().isBlank()) {
            sugerencias.add("Raza " + ganado.getRaza() + ": " + vacunaService.generarRecomendacionPorRaza(ganado.getRaza()));
        }

        if (ganado.getEdad() < 1) {
            sugerencias.add("Etapa ternero: reforzar vigilancia respiratoria y plan de inmunización temprana.");
        } else if (ganado.getEdad() > 8) {
            sugerencias.add("Edad avanzada: aumentar control sanitario preventivo y chequeos periódicos.");
        }

        if (ganado.getPeso() < 180) {
            sugerencias.add("Peso bajo para etapa productiva: ajustar dieta con apoyo de veterinario y control mensual.");
        } else if (ganado.getPeso() > 900) {
            sugerencias.add("Peso alto: revisar manejo nutricional y condicion corporal para evitar sobrecarga.");
        }

        if (ganado.getEstadoSalud() != null && !"Saludable".equalsIgnoreCase(ganado.getEstadoSalud())) {
            sugerencias.add("Estado sanitario en monitoreo: priorizar evaluación clínica y seguimiento diario.");
        } else {
            sugerencias.add("Estado saludable: mantener rutina preventiva y control de signos tempranos.");
        }

        LocalDate hoy = LocalDate.now();
        long vencidas = vacunas.stream()
                .filter(v -> v.getProximaDosis() != null && v.getProximaDosis().isBefore(hoy))
                .count();
        long proximas = vacunas.stream()
                .filter(v -> v.getProximaDosis() != null
                        && !v.getProximaDosis().isBefore(hoy)
                        && !v.getProximaDosis().isAfter(hoy.plusDays(15)))
                .count();

        if (vencidas > 0) {
            sugerencias.add("Hay " + vencidas + " vacuna(s) vencida(s): prioriza aplicación inmediata.");
        }
        if (proximas > 0) {
            sugerencias.add("Hay " + proximas + " vacuna(s) programada(s) en los próximos 15 días.");
        }
        if (vencidas == 0 && proximas == 0) {
            sugerencias.add("Calendario de vacunas al día para las próximas dos semanas.");
        }

        return sugerencias;
    }

    private List<String> generarSugerenciasSanitarias(int enMonitoreo, int vencidas, int proximas) {
        List<String> sugerencias = new ArrayList<>();

        if (enMonitoreo > 0) {
            sugerencias.add("Animales en monitoreo: realiza control clínico diario y registra evolución.");
        } else {
            sugerencias.add("No hay animales en monitoreo activo: mantener rondas preventivas.");
        }

        if (vencidas > 0) {
            sugerencias.add("Vacunas vencidas detectadas: agendar jornada de regularización prioritaria.");
        } else {
            sugerencias.add("Sin vacunas vencidas: conservar plan de aplicación actual.");
        }

        if (proximas > 0) {
            sugerencias.add("Vacunas próximas: prepara insumos y agenda al equipo para los siguientes 7 días.");
        } else {
            sugerencias.add("No hay vacunas próximas inmediatas: revisar inventario y mantener stock.");
        }

        return sugerencias;
    }

    private String construirRango(LocalDate inicio, LocalDate fin) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return inicio.format(formato) + " - " + fin.format(formato);
    }
}
