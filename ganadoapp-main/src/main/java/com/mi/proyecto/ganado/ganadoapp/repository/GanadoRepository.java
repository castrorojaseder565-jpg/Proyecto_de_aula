package com.mi.proyecto.ganado.ganadoapp.repository;

import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GanadoRepository extends JpaRepository<Ganado, Long> {

    Optional<Ganado> findByCodigoIdentificacion(String codigoIdentificacion);

    Optional<Ganado> findByCodigoIdentificacionIgnoreCase(String codigoIdentificacion);

    Optional<Ganado> findByNumeroArete(String numeroArete);

    Optional<Ganado> findByNumeroAreteIgnoreCase(String numeroArete);

    boolean existsByNumeroArete(String numeroArete);

    boolean existsByNumeroAreteIgnoreCase(String numeroArete);

    boolean existsByNumeroAreteIgnoreCaseAndIdNot(String numeroArete, Long id);

    boolean existsByCodigoIdentificacionIgnoreCase(String codigoIdentificacion);

    boolean existsByCodigoIdentificacionIgnoreCaseAndIdNot(String codigoIdentificacion, Long id);

    @Query("""
            SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END
            FROM Ganado g
            WHERE LOWER(g.nombre) = LOWER(:nombre)
              AND LOWER(g.raza) = LOWER(:raza)
              AND LOWER(g.sexo) = LOWER(:sexo)
              AND LOWER(g.propietario) = LOWER(:propietario)
              AND g.edad = :edad
              AND g.peso = :peso
            """)
    boolean existsFichaExacta(@Param("nombre") String nombre,
                              @Param("raza") String raza,
                              @Param("sexo") String sexo,
                              @Param("propietario") String propietario,
                              @Param("edad") double edad,
                              @Param("peso") double peso);

    @Query("""
            SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END
            FROM Ganado g
            WHERE LOWER(g.nombre) = LOWER(:nombre)
              AND LOWER(g.raza) = LOWER(:raza)
              AND LOWER(g.sexo) = LOWER(:sexo)
              AND LOWER(g.propietario) = LOWER(:propietario)
              AND g.edad = :edad
              AND g.peso = :peso
              AND g.id <> :id
            """)
    boolean existsFichaExactaExcluyendoId(@Param("nombre") String nombre,
                                          @Param("raza") String raza,
                                          @Param("sexo") String sexo,
                                          @Param("propietario") String propietario,
                                          @Param("edad") double edad,
                                          @Param("peso") double peso,
                                          @Param("id") Long id);

    @Query("""
            SELECT g
            FROM Ganado g
            WHERE LOWER(g.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(g.raza) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(g.numeroArete) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(g.codigoIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
            ORDER BY g.codigoIdentificacion ASC
            """)
    List<Ganado> buscarPorTermino(@Param("termino") String termino);

    List<Ganado> findByMarcaHierro(String marcaHierro);

    List<Ganado> findByNombreContainingIgnoreCase(String nombre);

    List<Ganado> findByRazaContainingIgnoreCase(String raza);

    long countBySexo(String sexo);

    long countByEstadoSalud(String estadoSalud);

    @Query("SELECT AVG(g.peso) FROM Ganado g")
    Double promedioPeso();

    @Query("SELECT AVG(g.edad) FROM Ganado g")
    Double promedioEdad();

    @Query("SELECT g.raza, COUNT(g) FROM Ganado g GROUP BY g.raza ORDER BY COUNT(g) DESC")
    List<Object[]> countByRaza();

    List<Ganado> findAllByOrderByCodigoIdentificacionAsc();
}
