package com.mi.proyecto.ganado.ganadoapp.repository;

import com.mi.proyecto.ganado.ganadoapp.mode.Invitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {
    List<Invitacion> findByCorreoIgnoreCaseAndUsadaEnIsNull(String correo);

    void deleteByCorreoIgnoreCaseAndUsadaEnIsNull(String correo);

    Optional<Invitacion> findByTokenHashAndUsadaEnIsNullAndExpiraEnAfter(String tokenHash, LocalDateTime ahora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitacion i where i.tokenHash = :tokenHash and i.usadaEn is null and i.expiraEn > :ahora")
    Optional<Invitacion> buscarVigenteParaActivacion(@Param("tokenHash") String tokenHash,
                                                     @Param("ahora") LocalDateTime ahora);
}
