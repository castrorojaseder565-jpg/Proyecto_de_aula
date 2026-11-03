package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import com.mi.proyecto.ganado.ganadoapp.repository.GanadoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class GanadoService {

    private static final Set<String> RAZAS_LECHERAS = Set.of(
            "holstein", "jersey", "normando", "pardo suizo", "ayrshire", "girolando"
    );

    public record PlanAlimentacion(
            Long ganadoId,
            String nombre,
            String raza,
            double edadAnios,
            double pesoKg,
            String etapa,
            double materiaSecaKg,
            double forrajeKg,
            double concentradoKg,
            double aguaLitros,
            String frecuencia,
            String observacion,
            String estadoSalud,
            String tipoForraje,
            String tipoConcentrado,
            double ratioForraje,
            double ratioConcentrado
    ) {}

    public record RegistroLeche(
            LocalDate fecha,
            double litros
    ) {}

    public record ResumenLecheGanado(
            Long ganadoId,
            String nombre,
            String raza,
            double litrosDiaEstimados,
            double litros7Dias,
            double litros30Dias,
            double prediccion7Dias
    ) {}

    private final GanadoRepository ganadoRepository;
    private final JdbcTemplate jdbcTemplate;

    public GanadoService(GanadoRepository ganadoRepository, JdbcTemplate jdbcTemplate) {
        this.ganadoRepository = ganadoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Ganado> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return ganadoRepository.findAllByOrderByCodigoIdentificacionAsc();
        }
        return ganadoRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    public List<Ganado> listarGanados() {
        return ganadoRepository.findAllByOrderByCodigoIdentificacionAsc();
    }

    public long contarGanadosRegistrados() {
        return ganadoRepository.count();
    }

    public boolean existeNumeroArete(String numeroArete) {
        if (numeroArete == null || numeroArete.isBlank()) {
            return false;
        }
        return ganadoRepository.existsByNumeroAreteIgnoreCase(normalizarTexto(numeroArete));
    }

    public Optional<Ganado> obtenerPorId(Long id) {
        return ganadoRepository.findById(id);
    }

    public Optional<Ganado> obtenerPorIdentificadorChatbot(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String valor = identificador.trim();
        Optional<Ganado> porArete = ganadoRepository.findByNumeroAreteIgnoreCase(valor);
        if (porArete.isPresent()) return porArete;

        Optional<Ganado> porCodigo = ganadoRepository.findByCodigoIdentificacionIgnoreCase(valor);
        if (porCodigo.isPresent()) return porCodigo;

        try {
            return ganadoRepository.findById(Long.parseLong(valor));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Transactional
    public Ganado guardarGanado(Ganado ganado) {
        if (ganado == null) {
            throw new IllegalArgumentException("El registro de ganado no puede ser nulo.");
        }

        prepararGanado(ganado);
        validarDatosBasicos(ganado);
        validarDuplicadosNuevo(ganado);

        if (ganado.getCodigoIdentificacion() == null || ganado.getCodigoIdentificacion().isBlank()) {
            ganado.setCodigoIdentificacion(generarCodigoUnico());
        } else if (ganadoRepository.existsByCodigoIdentificacionIgnoreCase(ganado.getCodigoIdentificacion())) {
            throw new IllegalArgumentException("El código interno ya existe en el sistema.");
        }

        return ganadoRepository.save(ganado);
    }

    @Transactional
    public Ganado actualizarGanado(Long id, Ganado nuevoGanado) {
        Ganado ganadoExistente = ganadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ganado con ID " + id + " no encontrado"));

        ganadoExistente.setNumeroArete(nuevoGanado.getNumeroArete());
        ganadoExistente.setMarcaHierro(nuevoGanado.getMarcaHierro());
        ganadoExistente.setPropietario(nuevoGanado.getPropietario());
        ganadoExistente.setNombre(nuevoGanado.getNombre());
        ganadoExistente.setRaza(nuevoGanado.getRaza());
        ganadoExistente.setEdad(nuevoGanado.getEdad());
        ganadoExistente.setPeso(nuevoGanado.getPeso());
        ganadoExistente.setSexo(nuevoGanado.getSexo());
        ganadoExistente.setEstadoSalud(nuevoGanado.getEstadoSalud());

        prepararGanado(ganadoExistente);
        validarDatosBasicos(ganadoExistente);
        validarDuplicadosActualizacion(ganadoExistente);

        return ganadoRepository.save(ganadoExistente);
    }

    @Transactional
    public void eliminarGanado(Long id) {
        Ganado ganado = ganadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ganado con ID " + id + " no encontrado"));
        jdbcTemplate.update("DELETE FROM leche_registro WHERE ganado_id = ?", id);
        ganadoRepository.delete(ganado);
    }

    public String generarReporteDeLimites(Ganado ganado) {
        StringBuilder reporte = new StringBuilder();
        String raza = ganado.getRaza();
        String sexo = ganado.getSexo();

        if (raza == null || sexo == null) {
            return "Evaluación incompleta: se requieren los datos de raza y sexo.";
        }

        double pesoMax = obtenerPesoMaximoPorRaza(raza, sexo);
        double edadMax = obtenerEdadMaximaPorRaza(raza);

        boolean hayAdvertencias = false;

        if (ganado.getEdad() > edadMax) {
            reporte.append("Edad actual: ").append(ganado.getEdad())
                    .append(" años, supera el máximo recomendado (").append(edadMax).append("). ");
            hayAdvertencias = true;
        } else if (ganado.getEdad() > edadMax * 0.75) {
            reporte.append("Edad avanzada: ").append(ganado.getEdad())
                    .append(" años (máximo estimado: ").append(edadMax).append("). ");
            hayAdvertencias = true;
        }

        if (ganado.getPeso() > pesoMax) {
            reporte.append("Peso actual: ").append(ganado.getPeso())
                    .append(" kg, excede el máximo recomendado (").append(pesoMax).append(" kg). ");
            hayAdvertencias = true;
        } else if (ganado.getPeso() > pesoMax * 0.9) {
            reporte.append("Peso elevado: ").append(ganado.getPeso())
                    .append(" kg (máximo sugerido: ").append(pesoMax).append(" kg). ");
            hayAdvertencias = true;
        }

        if (!hayAdvertencias) {
            return "Evaluación satisfactoria: el ejemplar se encuentra dentro de parámetros normales.";
        }

        return reporte.toString().trim();
    }

    public String generarRecomendacionNutricional(Ganado ganado) {
        double pesoMax = obtenerPesoMaximoPorRaza(ganado.getRaza(), ganado.getSexo());
        double pesoOptimo;
        String etapa;

        if (ganado.getEdad() <= 1.5) {
            etapa = "Joven en desarrollo";
            pesoOptimo = pesoMax * 0.45;
        } else if (ganado.getEdad() <= 3) {
            etapa = "Crecimiento o producción inicial";
            pesoOptimo = pesoMax * 0.75;
        } else {
            etapa = "Adulto en mantenimiento o reproducción";
            pesoOptimo = pesoMax * 0.9;
        }

        StringBuilder rec = new StringBuilder("Etapa de vida: ").append(etapa).append(". ");
        double porcentaje = (ganado.getPeso() / pesoOptimo) * 100;
        double ingestaMS = ganado.getPeso() * 0.025;

        if (porcentaje < 90) {
            rec.append("Bajo peso: aumentar concentrado y forraje proteínico. ");
        } else if (porcentaje > 110) {
            rec.append("Sobrepeso: reducir granos y aumentar forraje verde. ");
        } else {
            rec.append("Peso dentro del rango ideal. ");
        }

        rec.append("Consumo diario estimado de materia seca: ")
                .append(String.format("%.1f", ingestaMS)).append(" kg. ");

        if (ganado.getEdad() > 10) {
            rec.append("Suplementar calcio y fósforo por desgaste óseo. ");
        }

        return rec.toString().trim();
    }

    public PlanAlimentacion generarPlanAlimentacionRigido(Ganado ganado) {
        if (ganado == null) {
            throw new IllegalArgumentException("No se puede generar plan de alimentación sin datos del bovino.");
        }

        double edad = Math.max(0, ganado.getEdad());
        double peso = Math.max(1, ganado.getPeso());
        double lecheDia = estimarLecheBaseDiaria(ganado);

        double porcentajeMS = edad < 1 ? 0.028 : edad < 2 ? 0.027 : edad < 5 ? 0.026 : 0.024;
        double materiaSeca = peso * porcentajeMS;
        double forraje = materiaSeca * 0.68;
        double concentrado = lecheDia > 0 ? Math.max(2.0, lecheDia * 0.42) : Math.max(1.2, peso * 0.006);
        double agua = (peso * 0.08) + (lecheDia * 3.2);

        String etapa = edad < 1 ? "Ternero" : edad < 2 ? "Novillo joven" : edad < 5 ? "Adulto productivo" : "Adulto maduro";
        String frecuencia = edad < 1 ? "4 raciones al día" : edad < 2 ? "3 raciones al día" : "2 raciones al día";

        String observacion = lecheDia > 0
                ? "Controlar condición corporal semanal, sal mineralizada y balance energético en lactancia."
                : "Asegurar fibra efectiva, sal mineralizada y ajuste según ganancia de peso esperada.";

        if (ganado.getEstadoSalud() != null && !"Saludable".equalsIgnoreCase(ganado.getEstadoSalud())) {
            observacion = "Estado en monitoreo: dividir concentrado en porciones pequeñas y vigilar consumo diario.";
        }

        // Nuevos cálculos para ración nutricional
        double totalAlimento = forraje + concentrado;
        double ratioForraje = totalAlimento > 0 ? redondear((forraje / totalAlimento) * 100) : 70.0;
        double ratioConcentrado = totalAlimento > 0 ? redondear((concentrado / totalAlimento) * 100) : 30.0;

        String tipoForraje = "Pasto verde fresco";
        String tipoConcentrado = "Suplemento Estándar";

        if (edad < 1) {
            tipoForraje = "Heno de alfalfa tierna";
            tipoConcentrado = "Iniciador Terneros (21% Prot.)";
        } else if (edad < 2) {
            tipoForraje = "Pasto de corte / Silaje";
            tipoConcentrado = "Crecimiento Novillas (16% Prot.)";
        } else {
            String razaNormalizada = ganado.getRaza() == null ? "" : ganado.getRaza().trim().toLowerCase();
            if ("holstein".equalsIgnoreCase(razaNormalizada) || "jersey".equalsIgnoreCase(razaNormalizada)) {
                tipoForraje = "Silo de maíz / Alfalfa";
                tipoConcentrado = "Balanceado Lactancia Alta Producción";
            } else {
                tipoForraje = "Kikuyo / Pasto estrella";
                tipoConcentrado = "Balanceado Mantenimiento";
            }
        }

        String estadoSalud = ganado.getEstadoSalud() == null || ganado.getEstadoSalud().isBlank() ? "Saludable" : ganado.getEstadoSalud();

        return new PlanAlimentacion(
                ganado.getId(),
                ganado.getNombre(),
                ganado.getRaza(),
                redondear(edad),
                redondear(peso),
                etapa,
                redondear(materiaSeca),
                redondear(forraje),
                redondear(concentrado),
                redondear(agua),
                frecuencia,
                observacion,
                estadoSalud,
                tipoForraje,
                tipoConcentrado,
                ratioForraje,
                ratioConcentrado
        );
    }

    public List<PlanAlimentacion> generarPlanesAlimentacionRigidos(List<Ganado> ganados) {
        List<PlanAlimentacion> planes = new ArrayList<>();
        for (Ganado ganado : ganados) {
            planes.add(generarPlanAlimentacionRigido(ganado));
        }
        return planes;
    }

    public List<RegistroLeche> generarHistorialLeche(Ganado ganado, int dias) {
        int totalDias = Math.max(1, dias);
        List<RegistroLeche> historial = new ArrayList<>();
        double base = estimarLecheBaseDiaria(ganado);
        long semilla = ganado.getId() == null ? 1L : ganado.getId();

        for (int i = totalDias - 1; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            double variacion = (Math.sin((semilla * 0.37) + (i * 0.61)) + Math.cos(i * 0.27)) * 0.04;
            double litros = Math.max(0, base * (1 + variacion));
            historial.add(new RegistroLeche(fecha, redondear(litros)));
        }

        return historial;
    }

    public double calcularLechePeriodo(Ganado ganado, int dias) {
        return redondear(generarHistorialLeche(ganado, dias).stream()
                .mapToDouble(RegistroLeche::litros)
                .sum());
    }

    public double predecirLecheGanado(Ganado ganado, int diasProyeccion) {
        int dias = Math.max(1, diasProyeccion);
        List<RegistroLeche> historial = generarHistorialLeche(ganado, 14);
        double promedioReciente = historial.subList(7, 14).stream().mapToDouble(RegistroLeche::litros).average().orElse(0);
        double promedioPrevio = historial.subList(0, 7).stream().mapToDouble(RegistroLeche::litros).average().orElse(0);
        double tendencia = promedioReciente - promedioPrevio;
        double promedioProyectado = Math.max(0, promedioReciente + (tendencia * 0.35));
        return redondear(promedioProyectado * dias);
    }

    public List<ResumenLecheGanado> generarResumenLecheGanado(List<Ganado> ganados) {
        List<ResumenLecheGanado> resumenes = new ArrayList<>();
        for (Ganado ganado : ganados) {
            double diario = redondear(estimarLecheBaseDiaria(ganado));
            double sieteDias = calcularLechePeriodo(ganado, 7);
            double treintaDias = calcularLechePeriodo(ganado, 30);
            double prediccion = predecirLecheGanado(ganado, 7);

            resumenes.add(new ResumenLecheGanado(
                    ganado.getId(),
                    ganado.getNombre(),
                    ganado.getRaza(),
                    diario,
                    sieteDias,
                    treintaDias,
                    prediccion
            ));
        }
        return resumenes;
    }

    public double calcularTotalLecheDiaria(List<Ganado> ganados) {
        return redondear(ganados.stream().mapToDouble(this::estimarLecheBaseDiaria).sum());
    }

    public double calcularTotalLechePeriodo(List<Ganado> ganados, int dias) {
        return redondear(ganados.stream().mapToDouble(g -> calcularLechePeriodo(g, dias)).sum());
    }

    public double predecirTotalLecheHato(List<Ganado> ganados, int diasProyeccion) {
        return redondear(ganados.stream().mapToDouble(g -> predecirLecheGanado(g, diasProyeccion)).sum());
    }

    public String generarRecomendacionBienestar(Ganado ganado) {
        StringBuilder rec = new StringBuilder();

        if ("brahman".equalsIgnoreCase(ganado.getRaza())) {
            rec.append("Proporcionar sombra y baños antiparasitarios. ");
        }
        if ("holstein".equalsIgnoreCase(ganado.getRaza())) {
            rec.append("Mantener buena ventilación para evitar estrés térmico. ");
        }
        if (ganado.getEdad() < 1) {
            rec.append("Garantizar acceso a leche o sustituto lácteo. ");
        }
        if (ganado.getPeso() < 150) {
            rec.append("Proteger del frío y mantener cama seca. ");
        }

        rec.append("Revisar pezuñas cada tres meses y mantener registro sanitario.");
        return rec.toString().trim();
    }

    private void prepararGanado(Ganado ganado) {
        ganado.setNumeroArete(normalizarTexto(ganado.getNumeroArete()).toUpperCase());
        ganado.setMarcaHierro(valorPorDefecto(ganado.getMarcaHierro(), "CR").toUpperCase());
        ganado.setPropietario(valorPorDefecto(ganado.getPropietario(), "Eder Castro"));
        ganado.setNombre(normalizarTexto(ganado.getNombre()));
        ganado.setRaza(normalizarTexto(ganado.getRaza()));
        ganado.setSexo(normalizarTexto(ganado.getSexo()));
        ganado.setEstadoSalud(valorPorDefecto(ganado.getEstadoSalud(), "Saludable"));
        ganado.setUnidadTiempoEdad(calcularUnidadTiempo(ganado.getEdad()));

        if (ganado.getCodigoIdentificacion() != null && !ganado.getCodigoIdentificacion().isBlank()) {
            ganado.setCodigoIdentificacion(normalizarTexto(ganado.getCodigoIdentificacion()).toUpperCase());
        }
    }

    private void validarDatosBasicos(Ganado ganado) {
        if (ganado.getNumeroArete() == null || ganado.getNumeroArete().isBlank()) {
            throw new IllegalArgumentException("El número de arete es obligatorio.");
        }
        if (ganado.getNombre() == null || ganado.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del ganado es obligatorio.");
        }
        if (ganado.getRaza() == null || ganado.getRaza().isBlank()) {
            throw new IllegalArgumentException("La raza del ganado es obligatoria.");
        }
        if (ganado.getSexo() == null || ganado.getSexo().isBlank()) {
            throw new IllegalArgumentException("El sexo del ganado es obligatorio.");
        }
        if (ganado.getPeso() <= 0 || ganado.getPeso() > 2000) {
            throw new IllegalArgumentException("El peso debe estar entre 0.1 y 2000 kg.");
        }
        if (ganado.getEdad() < 0 || ganado.getEdad() > 40) {
            throw new IllegalArgumentException("La edad debe estar entre 0 y 40 años.");
        }
    }

    private void validarDuplicadosNuevo(Ganado ganado) {
        if (ganadoRepository.existsByNumeroAreteIgnoreCase(ganado.getNumeroArete())) {
            throw new IllegalArgumentException("Ya existe un ganado con ese número de arete.");
        }
        if (ganadoRepository.existsFichaExacta(
                ganado.getNombre(), ganado.getRaza(), ganado.getSexo(), ganado.getPropietario(), ganado.getEdad(), ganado.getPeso())) {
            throw new IllegalArgumentException("Ya existe un bovino registrado con la misma ficha técnica.");
        }
    }

    private void validarDuplicadosActualizacion(Ganado ganado) {
        if (ganadoRepository.existsByNumeroAreteIgnoreCaseAndIdNot(ganado.getNumeroArete(), ganado.getId())) {
            throw new IllegalArgumentException("Ya existe otro ganado con ese número de arete.");
        }
        if (ganado.getCodigoIdentificacion() != null
                && ganadoRepository.existsByCodigoIdentificacionIgnoreCaseAndIdNot(ganado.getCodigoIdentificacion(), ganado.getId())) {
            throw new IllegalArgumentException("Ya existe otro ganado con ese código interno.");
        }
        if (ganadoRepository.existsFichaExactaExcluyendoId(
                ganado.getNombre(), ganado.getRaza(), ganado.getSexo(), ganado.getPropietario(), ganado.getEdad(), ganado.getPeso(), ganado.getId())) {
            throw new IllegalArgumentException("Ya existe otro bovino registrado con la misma ficha técnica.");
        }
    }

    private String generarCodigoUnico() {
        String anio = String.valueOf(Year.now().getValue());
        String codigo;
        do {
            codigo = "BOV-" + anio + "-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
        } while (ganadoRepository.existsByCodigoIdentificacionIgnoreCase(codigo));
        return codigo;
    }

    private String valorPorDefecto(String valor, String valorDefecto) {
        String normalizado = normalizarTexto(valor);
        return normalizado.isBlank() ? valorDefecto : normalizado;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor.trim().replaceAll("\\s+", " "), Normalizer.Form.NFKC);
    }

    private double obtenerPesoMaximoPorRaza(String raza, String sexo) {
        if (raza == null || sexo == null) {
            return 800;
        }
        return switch (raza.toLowerCase()) {
            case "holstein" -> sexo.equalsIgnoreCase("macho") ? 1200 : 800;
            case "brahman" -> sexo.equalsIgnoreCase("macho") ? 1000 : 700;
            case "angus" -> sexo.equalsIgnoreCase("macho") ? 950 : 650;
            case "simmental" -> sexo.equalsIgnoreCase("macho") ? 1100 : 750;
            case "jersey" -> sexo.equalsIgnoreCase("macho") ? 800 : 550;
            case "hereford" -> sexo.equalsIgnoreCase("macho") ? 1000 : 700;
            case "limousin" -> sexo.equalsIgnoreCase("macho") ? 1100 : 750;
            case "charolais" -> sexo.equalsIgnoreCase("macho") ? 1200 : 850;
            default -> sexo.equalsIgnoreCase("macho") ? 900 : 650;
        };
    }

    private double obtenerEdadMaximaPorRaza(String raza) {
        if (raza == null) {
            return 20;
        }
        return switch (raza.toLowerCase()) {
            case "holstein", "jersey" -> 18;
            case "brahman" -> 22;
            case "angus", "hereford", "limousin" -> 20;
            case "simmental", "charolais" -> 21;
            default -> 20;
        };
    }

    private double estimarLecheBaseDiaria(Ganado ganado) {
        if (ganado == null || ganado.getSexo() == null || !"hembra".equalsIgnoreCase(ganado.getSexo())) {
            return 0;
        }

        if (ganado.getEdad() < 2) {
            return 0;
        }

        String raza = ganado.getRaza() == null ? "" : ganado.getRaza().trim().toLowerCase();
        double baseRaza = switch (raza) {
            case "holstein" -> 24;
            case "jersey" -> 18;
            case "normando" -> 16;
            case "pardo suizo" -> 20;
            case "ayrshire" -> 19;
            case "girolando" -> 21;
            default -> RAZAS_LECHERAS.contains(raza) ? 16 : 8;
        };

        double factorEdad = ganado.getEdad() <= 4 ? 1.0 : ganado.getEdad() <= 8 ? 0.9 : 0.75;
        double factorPeso = Math.max(0.75, Math.min(1.15, ganado.getPeso() / 500.0));
        double factorSalud = (ganado.getEstadoSalud() != null && !"Saludable".equalsIgnoreCase(ganado.getEstadoSalud())) ? 0.72 : 1.0;

        return baseRaza * factorEdad * factorPeso * factorSalud;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public String calcularUnidadTiempo(double edad) {
        if (edad < 1.0) {
            double meses = edad * 12;
            return meses < 1 ? "dias" : "meses";
        }
        return "anios";
    }

    public String formatoEdadParaVista(Double edadEnAnios) {
        if (edadEnAnios == null || edadEnAnios < 0) {
            return "No registrada";
        }
        final double edad = edadEnAnios;

        if (edad < 1.0) {
            double edadMeses = edad * 12;
            if (edadMeses < 1) {
                long dias = Math.round(edad * 365);
                return dias < 1 ? "Menos de un día" : dias + " día" + (dias != 1 ? "s" : "");
            }
            long meses = Math.round(edadMeses);
            return meses + " mes" + (meses != 1 ? "es" : "");
        }

        return Math.floor(edad) == edad
                ? String.format("%d año%s", Math.round(edad), edad != 1 ? "s" : "")
                : String.format("%.1f año%s", edad, edad != 1 ? "s" : "");
    }
}
