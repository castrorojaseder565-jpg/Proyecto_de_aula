package com.mi.proyecto.ganado.ganadoapp.repository;

import com.mi.proyecto.ganado.ganadoapp.mode.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VacunaRepository extends JpaRepository<Vacuna, Long> {

    @Query("SELECT COUNT(DISTINCT v.ganado.id) FROM Vacuna v WHERE v.fechaAplicacion IS NOT NULL")
    long countGanadosConVacunaAplicada();

    @Query("SELECT DISTINCT v FROM Vacuna v JOIN FETCH v.ganado")
    List<Vacuna> findAllWithGanado();

    @Query("SELECT v FROM Vacuna v JOIN FETCH v.ganado g WHERE g.id = :ganadoId")
    List<Vacuna> findByGanadoIdWithGanado(@Param("ganadoId") Long ganadoId);

    @Query("SELECT v FROM Vacuna v JOIN FETCH v.ganado WHERE v.id = :id")
    Optional<Vacuna> findByIdWithGanado(@Param("id") Long id);

    // Obtener vacunas asociadas a un ganado específico
    List<Vacuna> findByGanadoId(Long ganadoId);

    // Obtener vacunas pendientes o próximas a vencer
    @Query("SELECT v FROM Vacuna v WHERE v.estado = 'Pendiente' OR v.proximaDosis <= CURRENT_DATE")
    List<Vacuna> findVacunasPendientes();

    //Obtener historial de vacunas por nombre y raza
    @Query("SELECT v FROM Vacuna v WHERE LOWER(v.nombre) = LOWER(:nombre) AND LOWER(v.ganado.raza) = LOWER(:raza)")
    List<Vacuna> findByNombreAndGanadoRaza(@Param("nombre") String nombre, @Param("raza") String raza);

    // Obtener todas las vacunas por raza (para recomendaciones generales)
    @Query("SELECT v FROM Vacuna v WHERE LOWER(v.ganado.raza) = LOWER(:raza)")
    List<Vacuna> findByGanadoRaza(@Param("raza") String raza);

    @Query("""
            SELECT v FROM Vacuna v JOIN FETCH v.ganado g
            WHERE v.proximaDosis IS NOT NULL AND v.proximaDosis <= :fechaLimite
            ORDER BY v.proximaDosis ASC, g.codigoIdentificacion ASC
            """)
    List<Vacuna> findQueRequierenAtencionHasta(@Param("fechaLimite") LocalDate fechaLimite);

    //Verificar si ya existe una vacuna para un ganado en una fecha específica
    boolean existsByNombreAndGanadoIdAndFechaAplicacion(String nombre, Long id, LocalDate fechaAplicacion);

    //Calcular el promedio de días entre aplicación y próxima dosis por tipo y raza
    @Query(value = "SELECT AVG(DATEDIFF(v.proxima_dosis, v.fecha_aplicacion)) " +
            "FROM vacuna v JOIN ganado g ON v.ganado_id = g.id " +
            "WHERE LOWER(v.nombre) = LOWER(:nombre) AND LOWER(g.raza) = LOWER(:raza)",
            nativeQuery = true)
    Optional<Double> promedioDiasPorTipoYRaza(@Param("nombre") String nombre, @Param("raza") String raza);

    //Obtener las vacunas que vencen en los próximos X días (para alertas)
    @Query("SELECT v FROM Vacuna v WHERE v.proximaDosis BETWEEN CURRENT_DATE AND :fechaLimite")
    List<Vacuna> findVacunasPorVencer(@Param("fechaLimite") LocalDate fechaLimite);

    //(Opcional) Vacunas recientes — evitar recomendar duplicadas
    @Query("SELECT v FROM Vacuna v WHERE v.ganado.id = :ganadoId AND v.nombre = :nombre AND v.fechaAplicacion >= :fechaLimite")
    List<Vacuna> findVacunasRecientes(@Param("ganadoId") Long ganadoId,
                                      @Param("nombre") String nombre,
                                      @Param("fechaLimite") LocalDate fechaLimite);
}
