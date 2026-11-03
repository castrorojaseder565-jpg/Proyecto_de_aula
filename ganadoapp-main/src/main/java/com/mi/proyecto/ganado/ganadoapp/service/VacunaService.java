package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import com.mi.proyecto.ganado.ganadoapp.mode.Vacuna;
import com.mi.proyecto.ganado.ganadoapp.repository.VacunaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class VacunaService {

    private final VacunaRepository vacunaRepository;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMITE_DIAS_ATRAS = 30;

    public VacunaService(VacunaRepository vacunaRepository) {
        this.vacunaRepository = vacunaRepository;
    }


    public Vacuna guardarVacuna(Vacuna vacuna, String nombreOtraVacuna) {
        if (vacuna == null) throw new IllegalArgumentException("La vacuna no puede ser nula.");

        corregirNombreVacunaSiEsOtro(vacuna, nombreOtraVacuna);
        if (vacuna.getFechaAplicacion() == null)
            vacuna.setFechaAplicacion(LocalDate.now());
        validarVacuna(vacuna);

        if (vacunaRepository.existsByNombreAndGanadoIdAndFechaAplicacion(
                vacuna.getNombre(), vacuna.getGanado().getId(), vacuna.getFechaAplicacion())) {
            throw new IllegalArgumentException("Ya existe una vacuna con ese nombre para este ganado en la misma fecha.");
        }

        calcularProximaDosis(vacuna);
        vacuna.setEstado(calcularEstado(vacuna));

        return vacunaRepository.saveAndFlush(vacuna);
    }

    public Vacuna actualizarVacuna(Long id, Vacuna vacunaActualizada, String nombreOtraVacuna) {
        Vacuna vacuna = vacunaRepository.findByIdWithGanado(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacuna no encontrada."));

        corregirNombreVacunaSiEsOtro(vacunaActualizada, nombreOtraVacuna);

        if (vacunaActualizada.getNombre() != null && !vacunaActualizada.getNombre().isBlank())
            vacuna.setNombre(vacunaActualizada.getNombre());

        if (vacunaActualizada.getFechaAplicacion() != null)
            vacuna.setFechaAplicacion(vacunaActualizada.getFechaAplicacion());

        if (vacunaActualizada.getIntervaloCantidad() > 0)
            vacuna.setIntervaloCantidad(vacunaActualizada.getIntervaloCantidad());

        if (vacunaActualizada.getUnidadTiempo() != null && !vacunaActualizada.getUnidadTiempo().isBlank())
            vacuna.setUnidadTiempo(vacunaActualizada.getUnidadTiempo());

        calcularProximaDosis(vacuna);
        vacuna.setEstado(calcularEstado(vacuna));

        return vacunaRepository.saveAndFlush(vacuna);
    }


    public String generarRecomendacionPorRaza(String raza) {
        if (raza == null || raza.isEmpty()) {
            return "No hay recomendaciones específicas por raza.";
        }

        raza = raza.toLowerCase();

        return switch (raza) {
            case "holstein" -> "- Mastitis (S. aureus): aplicar refuerzo cada 6 meses.\n" +
                    "- Mantener control estricto de IBR y BVD.\n" +
                    "- Reforzar minerales y vitaminas para soporte inmunológico.";
            case "brahman" -> "- Aplicar refuerzo anual de carbunco y leptospirosis.\n" +
                    "- Programar desparasitación preventiva cada 2 o 3 meses.\n" +
                    "- Vigilar fiebre aftosa por alta exposición a pastoreo.";
            case "normando" -> "- Refuerzo anual para IBR y BVD.\n" +
                    "- Aplicar anticlostridial doble en zonas húmedas.\n" +
                    "- Reforzar prevención respiratoria en climas fríos.";
            case "jersey" -> "- Mantener control estricto de mastitis y fiebre aftosa.\n" +
                    "- Aplicar IBR y BVD cada 6 meses.\n" +
                    "- Programar refuerzo anual de carbunco.";
            case "angus" -> "- Aplicar carbunco una vez al año.\n" +
                    "- Refuerzo de leptospirosis cada 8 meses.\n" +
                    "- Mantener control antiparasitario reforzado.";
            default -> "- Mantener calendario sanitario base y controles veterinarios periódicos.";
        };
    }

    public String generarRecomendacionVacunas(Ganado ganado) {
        if (ganado == null) {
            return "<em>No se pudo generar recomendación: datos del ganado no disponibles.</em>";
        }

        String raza = Optional.ofNullable(ganado.getRaza()).orElse("Desconocida");
        double edad = ganado.getEdad();
        String sexo = Optional.ofNullable(ganado.getSexo()).orElse("No definido");

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='recomendacion'>");
        sb.append("<strong>Plan sanitario profesional para ").append(raza).append(":</strong><br><br>");

        sb.append("<u>Vacunas generales:</u><br>");
        sb.append("• Fiebre aftosa → cada 6 meses.<br>");
        sb.append("• Carbunco → anual.<br>");
        sb.append("• Leptospirosis → cada 6-12 meses.<br>");
        sb.append("• Clostridiosis → iniciar 4-6 meses, refuerzo anual.<br>");
        sb.append("• Rabia bovina → anual en zonas endémicas.<br>");
        sb.append("• Pasteurelosis → anual, especialmente en zonas húmedas.<br>");
        sb.append("• Control de parásitos → cada 3-4 meses.<br><br>");

        sb.append("<u>Vacunas según sexo:</u><br>");
        if (sexo.equalsIgnoreCase("Hembra")) {
            sb.append("• Brucelosis → dosis única 3-8 meses.<br>");
            sb.append("• IBR/BVD/PI3 → refuerzos semestrales para hembras reproductoras.<br>");
            sb.append("• Vacuna reproductiva (campylobacteriosis/tricomoniasis) → antes del servicio.<br>");
            sb.append("• Mastitis → semestral si es lechera.<br><br>");
        } else if (sexo.equalsIgnoreCase("Macho")) {
            sb.append("• IBR/BVD/PI3 → anual o semestral si reproductor.<br>");
            sb.append("• Leptospirosis → refuerzo semestral si monta natural.<br>");
            sb.append("• Carbunco → anual.<br><br>");
        }

        sb.append("<u>Vacunas según edad:</u><br>");
        if (edad < 1) {
            sb.append("• Terneros: anticlostridial 4-6 meses, refuerzo a los 30 días.<br>");
            sb.append("• Pasteurelosis: dosis inicial + refuerzo a 30 días.<br>");
            sb.append("• Diarrea neonatal → según riesgo.<br><br>");
        } else if (edad >= 1 && edad < 3) {
            sb.append("• Refuerzos de aftosa, leptospirosis y rabia cada 6 meses.<br>");
            sb.append("• Clostridiosis anual.<br><br>");
        } else if (edad >= 3 && edad < 6) {
            sb.append("• Vacuna reproductiva anual.<br>");
            sb.append("• Clostridiosis y leptospirosis anuales.<br><br>");
        } else {
            sb.append("• Refuerzos generales anuales.<br>");
            sb.append("• Control antiparasitario cada 3-4 meses.<br>");
            sb.append("• Refuerzo contra carbunco y rabia.<br><br>");
        }

        sb.append("<u>Recomendaciones según raza:</u><br>");
        sb.append(generarRecomendacionPorRaza(raza).replace("\n", "<br>"));

        sb.append("<br><em>Nota: El calendario puede variar según la zona, clima y condiciones del hato. Consulte siempre al veterinario de confianza para ajustes individuales.</em>");
        sb.append("</div>");

        return sb.toString();
    }


    private void corregirNombreVacunaSiEsOtro(Vacuna vacuna, String nombreOtraVacuna) {
        if ("Otros".equalsIgnoreCase(vacuna.getNombre())) {
            if (nombreOtraVacuna == null || nombreOtraVacuna.trim().isEmpty()) {
                throw new IllegalArgumentException("Debes escribir el nombre de la vacuna cuando eliges 'Otros'.");
            }
            vacuna.setNombre(nombreOtraVacuna.trim());
        }
    }


    private void calcularProximaDosis(Vacuna vacuna) {
        if (vacuna.getFechaAplicacion() == null || vacuna.getIntervaloCantidad() <= 0 ||
                vacuna.getUnidadTiempo() == null) return;

        LocalDate base = vacuna.getFechaAplicacion();
        String unidad = vacuna.getUnidadTiempo().toLowerCase().trim();

        switch (unidad) {
            case "día", "días", "dia", "dias" -> vacuna.setProximaDosis(base.plusDays(vacuna.getIntervaloCantidad()));
            case "semana", "semanas" -> vacuna.setProximaDosis(base.plusWeeks(vacuna.getIntervaloCantidad()));
            case "mes", "meses" -> vacuna.setProximaDosis(base.plusMonths(vacuna.getIntervaloCantidad()));
            case "año", "años", "ano", "anos" -> vacuna.setProximaDosis(base.plusYears(vacuna.getIntervaloCantidad()));
        }
    }

    private String calcularEstado(Vacuna vacuna) {
        LocalDate hoy = LocalDate.now();
        LocalDate proxima = vacuna.getProximaDosis();

        if (proxima == null) return "Sin fecha";
        long dias = ChronoUnit.DAYS.between(hoy, proxima);

        if (hoy.isEqual(proxima)) return "Aplicar hoy";
        if (dias > 7) return "Pendiente";
        if (dias > 0) return "Próxima";
        return "Vencida";
    }

    private void validarVacuna(Vacuna vacuna) {
        if (vacuna.getNombre() == null || vacuna.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre de la vacuna es obligatorio.");
        if (vacuna.getGanado() == null || vacuna.getGanado().getId() == null)
            throw new IllegalArgumentException("Debe seleccionar un ganado.");
        if (vacuna.getIntervaloCantidad() <= 0)
            throw new IllegalArgumentException("El intervalo debe ser mayor que 0.");
        if (vacuna.getUnidadTiempo() == null || vacuna.getUnidadTiempo().isBlank())
            throw new IllegalArgumentException("La unidad de tiempo es obligatoria.");
        if (vacuna.getFechaAplicacion() == null) {
            throw new IllegalArgumentException("La fecha de aplicación es obligatoria.");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate minimoPermitido = hoy.minusDays(LIMITE_DIAS_ATRAS);
        if (vacuna.getFechaAplicacion().isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de aplicación no puede ser futura.");
        }
        if (vacuna.getFechaAplicacion().isBefore(minimoPermitido)) {
            throw new IllegalArgumentException("La fecha de aplicación no puede ser mayor a " + LIMITE_DIAS_ATRAS + " días hacia atrás.");
        }
    }


    public List<Vacuna> obtenerTodas() {
        List<Vacuna> vacunas = vacunaRepository.findAllWithGanado();
        vacunas.forEach(v -> v.setEstado(calcularEstado(v)));
        return vacunas;
    }

    public long contarAnimalesConVacunaAplicada() {
        return vacunaRepository.countGanadosConVacunaAplicada();
    }

    public List<Vacuna> obtenerQueRequierenAtencionHasta(LocalDate fechaLimite) {
        List<Vacuna> vacunas = vacunaRepository.findQueRequierenAtencionHasta(fechaLimite);
        vacunas.forEach(v -> v.setEstado(calcularEstado(v)));
        return vacunas;
    }

    public Optional<Vacuna> obtenerPorId(Long id) {
        Optional<Vacuna> vacuna = vacunaRepository.findByIdWithGanado(id);
        vacuna.ifPresent(v -> v.setEstado(calcularEstado(v)));
        return vacuna;
    }

    public List<Vacuna> obtenerVacunasPorGanado(Long ganadoId) {
        List<Vacuna> vacunas = vacunaRepository.findByGanadoIdWithGanado(ganadoId);
        vacunas.forEach(v -> v.setEstado(calcularEstado(v)));
        return vacunas;
    }

    @Transactional
    public Vacuna eliminar(Long id) {
        Vacuna vacuna = vacunaRepository.findByIdWithGanado(id)
                .orElseThrow(() -> new RuntimeException("Vacuna no encontrada"));

        vacunaRepository.delete(vacuna);

        return vacuna; // Ã°Å¸â€˜Ë† CLAVE
    }
}
