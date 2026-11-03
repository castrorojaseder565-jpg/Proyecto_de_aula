package com.mi.proyecto.ganado.ganadoapp.mode;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "vacuna") // ✅ TABLA CORRECTA (singular)
public class Vacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Relación con Ganado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganado_id", nullable = false)
    private Ganado ganado;

    @Column(nullable = false, length = 100)
    private String nombre;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "proxima_dosis")
    private LocalDate proximaDosis;

    @Column(name = "intervalo_cantidad")
    private int intervaloCantidad;

    // Compatibilidad con esquema legado donde esta columna es obligatoria
    @Column(name = "intervalo_meses")
    private Integer intervaloMeses;

    // "dias", "semanas", "meses"
    @Column(name = "unidad_tiempo", length = 20)
    private String unidadTiempo;

    // "Pendiente", "Vencida", "Aplicar", etc.
    @Column(length = 50)
    private String estado;

    // ======= CONSTRUCTORES =======

    public Vacuna() {
    }

    public Vacuna(String nombre, Ganado ganado) {
        this.nombre = nombre;
        this.ganado = ganado;
        this.fechaAplicacion = LocalDate.now();
    }

    // ======= GETTERS & SETTERS =======

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ganado getGanado() {
        return ganado;
    }

    public void setGanado(Ganado ganado) {
        this.ganado = ganado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public LocalDate getProximaDosis() {
        return proximaDosis;
    }

    public void setProximaDosis(LocalDate proximaDosis) {
        this.proximaDosis = proximaDosis;
    }

    public int getIntervaloCantidad() {
        return intervaloCantidad;
    }

    public void setIntervaloCantidad(int intervaloCantidad) {
        this.intervaloCantidad = intervaloCantidad;
        this.intervaloMeses = intervaloCantidad;
    }

    public Integer getIntervaloMeses() {
        return intervaloMeses;
    }

    public void setIntervaloMeses(Integer intervaloMeses) {
        this.intervaloMeses = intervaloMeses;
    }

    public String getUnidadTiempo() {
        return unidadTiempo;
    }

    public void setUnidadTiempo(String unidadTiempo) {
        this.unidadTiempo = unidadTiempo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @PrePersist
    @PreUpdate
    private void sincronizarIntervaloMeses() {
        if (this.intervaloCantidad > 0) {
            this.intervaloMeses = this.intervaloCantidad;
        } else if (this.intervaloMeses != null && this.intervaloMeses > 0) {
            this.intervaloCantidad = this.intervaloMeses;
        }
    }

    // ======= MÉTODO UTILITARIO =======

    @Transient
    public String getClaseEstadoCss() {
        if (estado == null || estado.isBlank()) {
            return "";
        }
        return estado.trim().replace(' ', '_');
    }

    @Transient
    public String getEstadoCalculado() {
        if (proximaDosis == null) return "Sin fecha";

        LocalDate hoy = LocalDate.now();

        if (hoy.isBefore(proximaDosis)) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, proximaDosis);
            return dias <= 7 ? "Aplicar" : "Pendiente";
        } else if (hoy.isEqual(proximaDosis)) {
            return "Aplicar";
        } else {
            return "Vencida";
        }
    }

    @Override
    public String toString() {
        return "Vacuna{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", fechaAplicacion=" + fechaAplicacion +
                ", proximaDosis=" + proximaDosis +
                ", intervaloCantidad=" + intervaloCantidad +
                ", unidadTiempo='" + unidadTiempo + '\'' +
                ", estado='" + estado + '\'' +
                ", ganadoId=" + (ganado != null ? ganado.getId() : "null") +
                '}';
    }
}
