package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.mode.DatosGraficoLeche;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LecheTrazabilidadService {

    private static final double SIN_DATO_GRAFICO = -1.0;

    private static final Set<String> TURNOS_VALIDOS =
            Set.of("MANANA", "TARDE", "NOCHE");

    private final JdbcTemplate jdbcTemplate;
    private final WekaPredictionService wekaService;

    public LecheTrazabilidadService(
            JdbcTemplate jdbcTemplate,
            WekaPredictionService wekaService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.wekaService = wekaService;

        inicializarTabla();
    }

    public record RegistroLeche(
            Long id,
            Long ganadoId,
            LocalDate fechaRegistro,
            String turno,
            double litros,
            String observacion
    ) {}

    public record ProduccionDiaria(
            LocalDate fecha,
            double litros
    ) {}

    public record ResumenLecheGanado(
            Long ganadoId,
            String nombre,
            String raza,
            double litrosDiaPromedio,
            double litros7Dias,
            double litros30Dias,
            double prediccion7Dias
    ) {}

    public record ResumenProduccionLeche(
            double litrosHistoricos,
            double litros7Dias,
            double litros30Dias,
            long totalRegistros,
            LocalDate ultimoRegistro
    ) {}

    public record ProduccionLecheHato(
            Long ganadoId,
            String nombre,
            String raza,
            double litros7Dias,
            double litros30Dias
    ) {}

    public List<ProduccionLecheHato> obtenerProduccionRealPorGanado() {
        LocalDate hoy = LocalDate.now();
        return jdbcTemplate.query(
                """
                SELECT g.id AS ganado_id, g.nombre, g.raza,
                       COALESCE(SUM(CASE WHEN l.fecha_registro >= ? THEN l.litros ELSE 0 END), 0) AS litros_7_dias,
                       COALESCE(SUM(CASE WHEN l.fecha_registro >= ? THEN l.litros ELSE 0 END), 0) AS litros_30_dias
                FROM ganado g
                LEFT JOIN leche_registro l ON l.ganado_id = g.id
                GROUP BY g.id, g.nombre, g.raza
                ORDER BY litros_7_dias DESC, litros_30_dias DESC, g.nombre ASC
                """,
                (rs, rowNum) -> new ProduccionLecheHato(
                        rs.getLong("ganado_id"),
                        rs.getString("nombre"),
                        rs.getString("raza"),
                        redondear(rs.getDouble("litros_7_dias")),
                        redondear(rs.getDouble("litros_30_dias"))
                ),
                hoy.minusDays(6),
                hoy.minusDays(29)
        );
    }

    public ResumenProduccionLeche obtenerResumenProduccionGanado(Long ganadoId) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde7Dias = hoy.minusDays(6);
        LocalDate desde30Dias = hoy.minusDays(29);
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(litros), 0) AS litros_historicos,
                       COALESCE(SUM(CASE WHEN fecha_registro >= ? THEN litros ELSE 0 END), 0) AS litros_7_dias,
                       COALESCE(SUM(CASE WHEN fecha_registro >= ? THEN litros ELSE 0 END), 0) AS litros_30_dias,
                       COUNT(*) AS total_registros,
                       MAX(fecha_registro) AS ultimo_registro
                FROM leche_registro
                WHERE ganado_id = ?
                """,
                (rs, rowNum) -> {
                    java.sql.Date ultimoRegistro = rs.getDate("ultimo_registro");
                    return new ResumenProduccionLeche(
                            redondear(rs.getDouble("litros_historicos")),
                            redondear(rs.getDouble("litros_7_dias")),
                            redondear(rs.getDouble("litros_30_dias")),
                            rs.getLong("total_registros"),
                            ultimoRegistro == null ? null : ultimoRegistro.toLocalDate()
                    );
                },
                desde7Dias,
                desde30Dias,
                ganadoId
        );
    }

    public void registrarProduccion(
            Long ganadoId,
            LocalDate fechaRegistro,
            String turno,
            Double litros,
            String observacion
    ) {
        if (ganadoId == null) {
            throw new IllegalArgumentException("Debes indicar el bovino para registrar producción.");
        }
        if (fechaRegistro == null) {
            throw new IllegalArgumentException("Debes indicar la fecha del ordeño.");
        }
        if (!fechaRegistro.equals(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se permite registrar producción para la fecha de hoy.");
        }
        if (litros == null || litros <= 0 || litros > 80) {
            throw new IllegalArgumentException("Los litros deben estar entre 0.1 y 80.");
        }

        String turnoNormalizado = normalizarTurno(turno);

        if (!TURNOS_VALIDOS.contains(turnoNormalizado)) {
            throw new IllegalArgumentException("Turno no válido. Usa mañana, tarde o noche.");
        }

        Integer existe = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM leche_registro
                WHERE ganado_id = ?
                AND fecha_registro = ?
                AND turno = ?
                """,
                Integer.class,
                ganadoId,
                fechaRegistro,
                turnoNormalizado
        );

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe un registro para ese bovino, fecha y turno.");
        }

        jdbcTemplate.update(
                """
                INSERT INTO leche_registro
                (ganado_id, fecha_registro, turno, litros, observacion)
                VALUES (?, ?, ?, ?, ?)
                """,
                ganadoId,
                fechaRegistro,
                turnoNormalizado,
                litros,
                limpiarObservacion(observacion)
        );
    }

    public List<RegistroLeche> obtenerHistorialGanado(Long ganadoId, int limite) {
        int max = Math.max(1, limite);
        return jdbcTemplate.query(
                """
                SELECT id, ganado_id, fecha_registro, turno, litros, observacion
                FROM leche_registro
                WHERE ganado_id = ?
                ORDER BY fecha_registro DESC, turno ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new RegistroLeche(
                        rs.getLong("id"),
                        rs.getLong("ganado_id"),
                        rs.getDate("fecha_registro").toLocalDate(),
                        rs.getString("turno"),
                        rs.getDouble("litros"),
                        rs.getString("observacion")
                ),
                ganadoId,
                max
        );
    }

    private RegistroLeche obtenerRegistroPorIdInterno(Long registroId) {
        List<RegistroLeche> lista = jdbcTemplate.query(
                """
                SELECT id, ganado_id, fecha_registro, turno, litros, observacion
                FROM leche_registro
                WHERE id = ?
                """,
                (rs, rowNum) -> new RegistroLeche(
                        rs.getLong("id"),
                        rs.getLong("ganado_id"),
                        rs.getDate("fecha_registro").toLocalDate(),
                        rs.getString("turno"),
                        rs.getDouble("litros"),
                        rs.getString("observacion")
                ),
                registroId
        );

        if (lista.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el registro.");
        }
        return lista.get(0);
    }

    public void actualizarRegistro(Long registroId, String turno, Double litros, String observacion) {
        RegistroLeche actual = obtenerRegistroPorIdInterno(registroId);

        if (!actual.fechaRegistro().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden editar registros de hoy.");
        }
        if (litros == null || litros <= 0 || litros > 80) {
            throw new IllegalArgumentException("Los litros deben estar entre 0.1 y 80.");
        }

        String turnoNormalizado = normalizarTurno(turno);

        Integer existe = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM leche_registro
                WHERE ganado_id = ?
                AND fecha_registro = ?
                AND turno = ?
                AND id <> ?
                """,
                Integer.class,
                actual.ganadoId(),
                actual.fechaRegistro(),
                turnoNormalizado,
                registroId
        );

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe un registro para ese turno.");
        }

        jdbcTemplate.update(
                """
                UPDATE leche_registro
                SET turno = ?, litros = ?, observacion = ?
                WHERE id = ?
                """,
                turnoNormalizado,
                litros,
                limpiarObservacion(observacion),
                registroId
        );
    }

    public void eliminarRegistro(Long registroId) {
        RegistroLeche actual = obtenerRegistroPorIdInterno(registroId);

        if (!actual.fechaRegistro().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden eliminar registros de hoy.");
        }

        jdbcTemplate.update("DELETE FROM leche_registro WHERE id = ?", registroId);
    }

    public List<ProduccionDiaria> obtenerProduccionDiariaGanado(Long ganadoId, int dias) {
        LocalDate desde = LocalDate.now().minusDays(Math.max(1, dias) - 1L);

        List<ProduccionDiaria> datos = jdbcTemplate.query(
                """
                SELECT fecha_registro, SUM(litros) AS litros
                FROM leche_registro
                WHERE ganado_id = ?
                AND fecha_registro >= ?
                GROUP BY fecha_registro
                ORDER BY fecha_registro ASC
                """,
                (rs, rowNum) -> new ProduccionDiaria(
                        rs.getDate("fecha_registro").toLocalDate(),
                        redondear(rs.getDouble("litros"))
                ),
                ganadoId,
                desde
        );

        return rellenarDiasFaltantes(datos, Math.max(1, dias));
    }

    public double calcularLechePeriodoGanado(Long ganadoId, int dias) {
        LocalDate desde = LocalDate.now().minusDays(Math.max(1, dias) - 1L);

        Double total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(litros), 0)
                FROM leche_registro
                WHERE ganado_id = ?
                AND fecha_registro >= ?
                """,
                Double.class,
                ganadoId,
                desde
        );

        return redondear(total == null ? 0 : total);
    }

    /*
     * ==========================================
     * PREDICCIONES CON WEKA
     * ==========================================
     */

    public double predecirLitrosManana(Long ganadoId) {
        try {
            List<ProduccionDiaria> serie = obtenerProduccionDiariaGanado(ganadoId, 30);

            // Quitamos días vacíos o que no tengan data suficiente para no sesgar a Weka
            List<ProduccionDiaria> serieFiltrada = serie.stream()
                    .filter(d -> d.litros() > 0.0)
                    .collect(Collectors.toList());

            if (serieFiltrada.size() < 3) {
                return redondear(calcularLechePeriodoGanado(ganadoId, 7) / 7.0);
            }

            double prediccion = wekaService.entrenarYPrediccion(serieFiltrada);
            return redondear(Math.max(0, prediccion));

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public double predecirLecheGanado(Long ganadoId, int diasProyeccion) {
        int dias = Math.max(1, diasProyeccion);
        double litrosDia = predecirLitrosManana(ganadoId);
        return redondear(litrosDia * dias);
    }

    public double predecirProximaSemana(Long ganadoId) {
        return predecirLecheGanado(ganadoId, 7);
    }

    public double predecirProximoMes(Long ganadoId) {
        return predecirLecheGanado(ganadoId, 30);
    }

    public DatosGraficoLeche obtenerDatosGraficoPrediccion(Long ganadoId) {
        try {
            return construirDatosGraficoPrediccion(ganadoId);
        } catch (Exception e) {
            return DatosGraficoLeche.vacio();
        }
    }

    private DatosGraficoLeche construirDatosGraficoPrediccion(Long ganadoId) {
        int diasVentana = 30;
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(diasVentana - 1L);
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM");

        Map<LocalDate, Double> mapaReal = obtenerProduccionDiariaGanadoReal(ganadoId, diasVentana).stream()
                .collect(Collectors.toMap(
                        ProduccionDiaria::fecha,
                        ProduccionDiaria::litros,
                        Double::sum,
                        LinkedHashMap::new
                ));

        List<ProduccionDiaria> serieFiltrada = obtenerProduccionDiariaGanado(ganadoId, diasVentana).stream()
                .filter(d -> d.litros() > 0)
                .sorted(Comparator.comparing(ProduccionDiaria::fecha))
                .toList();

        Map<LocalDate, Double> mapaRegresion = new LinkedHashMap<>();
        double valorPrediccion = predecirLitrosManana(ganadoId);
        boolean regresionDisponible = serieFiltrada.size() >= 3;

        if (regresionDisponible) {
            try {
                WekaPredictionService.ResultadoRegresionGrafico resultado =
                        wekaService.calcularRegresionParaGrafico(serieFiltrada);
                for (int i = 0; i < serieFiltrada.size(); i++) {
                    mapaRegresion.put(
                            serieFiltrada.get(i).fecha(),
                            redondear(resultado.valoresAjustados().get(i))
                    );
                }
                valorPrediccion = redondear(resultado.prediccionSiguiente());
            } catch (Exception e) {
                regresionDisponible = false;
            }
        }

        List<String> etiquetas = new ArrayList<>();
        List<Double> produccionReal = new ArrayList<>();
        List<Double> lineaRegresion = new ArrayList<>();

        for (int i = 0; i < diasVentana; i++) {
            LocalDate fecha = inicio.plusDays(i);
            etiquetas.add(fecha.format(formatoFecha));
            produccionReal.add(mapaReal.containsKey(fecha) ? mapaReal.get(fecha) : SIN_DATO_GRAFICO);
            lineaRegresion.add(mapaRegresion.containsKey(fecha) ? mapaRegresion.get(fecha) : SIN_DATO_GRAFICO);
        }

        etiquetas.add(hoy.plusDays(1).format(formatoFecha) + " (pred.)");
        produccionReal.add(SIN_DATO_GRAFICO);
        lineaRegresion.add(regresionDisponible ? valorPrediccion : SIN_DATO_GRAFICO);

        return new DatosGraficoLeche(
                etiquetas,
                produccionReal,
                lineaRegresion,
                valorPrediccion,
                regresionDisponible
        );
    }

    public List<ResumenLecheGanado> generarResumenLecheGanado() {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                """
                SELECT g.id AS ganado_id, g.nombre, g.raza
                FROM ganado g
                ORDER BY g.nombre ASC
                """
        );

        List<ResumenLecheGanado> resumen = new ArrayList<>();

        for (Map<String, Object> fila : filas) {
            Long ganadoId = ((Number) fila.get("ganado_id")).longValue();
            String nombre = String.valueOf(fila.get("nombre"));
            String raza = String.valueOf(fila.get("raza"));

            double litros7 = calcularLechePeriodoGanado(ganadoId, 7);
            double litros30 = calcularLechePeriodoGanado(ganadoId, 30);
            double promedioDia = redondear(litros7 / 7.0);
            double prediccion = predecirProximaSemana(ganadoId);

            resumen.add(new ResumenLecheGanado(
                    ganadoId, nombre, raza, promedioDia, litros7, litros30, prediccion
            ));
        }

        return resumen;
    }

    public double totalLecheDiariaHato() {
        return redondear(
                generarResumenLecheGanado().stream()
                        .mapToDouble(ResumenLecheGanado::litrosDiaPromedio)
                        .sum()
        );
    }

    public double totalLecheHatoPeriodo(int dias) {
        LocalDate desde = LocalDate.now().minusDays(Math.max(1, dias) - 1L);

        Double total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(litros), 0)
                FROM leche_registro
                WHERE fecha_registro >= ?
                """,
                Double.class,
                desde
        );

        return redondear(total == null ? 0 : total);
    }

    public double prediccionLecheHato(int diasProyeccion) {
        return redondear(
                generarResumenLecheGanado().stream()
                        .mapToDouble(item -> item.prediccion7Dias() * (Math.max(1, diasProyeccion) / 7.0))
                        .sum()
        );
    }

    private void inicializarTabla() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS leche_registro (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    ganado_id BIGINT NOT NULL,
                    fecha_registro DATE NOT NULL,
                    turno VARCHAR(20) NOT NULL,
                    litros DECIMAL(10,2) NOT NULL,
                    observacion VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                    CONSTRAINT fk_leche_registro_ganado
                    FOREIGN KEY (ganado_id)
                    REFERENCES ganado(id),

                    CONSTRAINT uk_leche_registro
                    UNIQUE (ganado_id, fecha_registro, turno)
                )
                """
        );
    }

    private String normalizarTurno(String turno) {
        if (turno == null || turno.isBlank()) {
            return "MANANA";
        }
        String limpio = turno.trim().toUpperCase();
        if ("MAÑANA".equals(limpio) || "MANANA".equals(limpio)) {
            return "MANANA";
        }
        if ("TARDE".equals(limpio)) {
            return "TARDE";
        }
        if ("NOCHE".equals(limpio)) {
            return "NOCHE";
        }
        return limpio;
    }

    private String limpiarObservacion(String observacion) {
        if (observacion == null) {
            return null;
        }
        String texto = observacion.trim();
        return texto.isEmpty() ? null : texto;
    }

    private List<ProduccionDiaria> rellenarDiasFaltantes(List<ProduccionDiaria> datos, int dias) {
        LocalDate inicio = LocalDate.now().minusDays(dias - 1L);

        // Calculamos un promedio real de los días que SÍ tienen datos
        double sumaConData = datos.stream().mapToDouble(ProduccionDiaria::litros).sum();
        double promedioRelleno = datos.isEmpty() ? 0.0 : redondear(sumaConData / datos.size());

        Map<LocalDate, Double> mapa = datos.stream()
                .collect(Collectors.toMap(
                        ProduccionDiaria::fecha,
                        ProduccionDiaria::litros,
                        Double::sum
                ));

        List<ProduccionDiaria> serie = new ArrayList<>();

        for (int i = 0; i < dias; i++) {
            LocalDate fecha = inicio.plusDays(i);
            // Reemplazamos el antiguo 0.0 por el promedioRelleno para no romper la Regresión Lineal
            serie.add(new ProduccionDiaria(
                    fecha,
                    redondear(mapa.getOrDefault(fecha, promedioRelleno))
            ));
        }

        serie.sort(Comparator.comparing(ProduccionDiaria::fecha));
        return serie;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public List<ProduccionDiaria> obtenerProduccionDiariaGanadoReal(Long ganadoId, int dias) {
        LocalDate desde = LocalDate.now().minusDays(Math.max(1, dias) - 1L);

        return jdbcTemplate.query(
                "SELECT fecha_registro, SUM(litros) AS litros " +
                        "FROM leche_registro " +
                        "WHERE ganado_id = ? AND fecha_registro >= ? " +
                        "GROUP BY fecha_registro " +
                        "ORDER BY fecha_registro DESC",
                (rs, rowNum) -> new ProduccionDiaria(
                        rs.getDate("fecha_registro").toLocalDate(),
                        redondear(rs.getDouble("litros"))
                ),
                ganadoId,
                desde
        );
    }
}
