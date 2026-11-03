package com.mi.proyecto.ganado.ganadoapp.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuentaEstadoService {
    public static final String PENDIENTE_APROBACION = "PENDIENTE_APROBACION";
    public static final String ACTIVO = "ACTIVO";
    private final JdbcTemplate jdbcTemplate;

    public CuentaEstadoService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @PostConstruct
    void crearTablasSiNoExisten() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cuenta_estado_usuario (
                    usuario_id BIGINT NOT NULL,
                    estado VARCHAR(30) NOT NULL,
                    creada_en DATETIME NOT NULL,
                    aprobada_en DATETIME NULL,
                    PRIMARY KEY (usuario_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public boolean estaActivo(Long usuarioId) {
        List<String> estados = jdbcTemplate.query("SELECT estado FROM cuenta_estado_usuario WHERE usuario_id = ?",
                (rs, rowNum) -> rs.getString(1), usuarioId);
        return estados.isEmpty() || ACTIVO.equals(estados.getFirst());
    }

    public void aprobar(Long usuarioId) {
        int actualizadas = jdbcTemplate.update("""
                UPDATE cuenta_estado_usuario SET estado = ?, aprobada_en = ?
                WHERE usuario_id = ? AND estado = ?
                """, ACTIVO, LocalDateTime.now(), usuarioId, PENDIENTE_APROBACION);
        if (actualizadas != 1) throw new IllegalArgumentException("La solicitud no está pendiente de aprobación.");
    }

    public List<SolicitudPendiente> listarPendientes() {
        return jdbcTemplate.query("""
                SELECT u.id, u.nombre, u.correo, e.creada_en
                FROM cuenta_estado_usuario e JOIN usuario u ON u.id = e.usuario_id
                WHERE e.estado = ? ORDER BY e.creada_en ASC
                """, (rs, rowNum) -> new SolicitudPendiente(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("correo"), rs.getTimestamp("creada_en").toLocalDateTime()), PENDIENTE_APROBACION);
    }

    public record SolicitudPendiente(Long usuarioId, String nombre, String correo, LocalDateTime creadaEn) { }
}
