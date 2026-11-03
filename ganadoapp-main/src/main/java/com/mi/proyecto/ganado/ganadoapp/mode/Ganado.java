package com.mi.proyecto.ganado.ganadoapp.mode;

import jakarta.persistence.*;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ganado")
public class Ganado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_identificacion", unique = true, nullable = false, length = 50)
    private String codigoIdentificacion;

    @Column(name = "numero_arete", unique = true, nullable = false, length = 30)
    private String numeroArete;

    @Column(name = "marca_hierro", nullable = false, length = 20)
    private String marcaHierro;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String raza;

    @Column(nullable = false)
    private double peso;

    @Column(nullable = false)
    private double edad;

    @Column(name = "unidad_tiempo_edad", length = 20, nullable = false)
    private String unidadTiempoEdad;

    @Column(nullable = false, length = 20)
    private String sexo;

    @Column(name = "estado_salud", length = 50, nullable = false)
    private String estadoSalud;

    @Column(nullable = false, length = 100)
    private String propietario = "Eder Castro";

    @OneToMany(mappedBy = "ganado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Vacuna> vacunas = new ArrayList<>();

    public Ganado() {
    }

    public Ganado(String nombre, String raza, double peso, double edad,
                  String unidadTiempoEdad, String sexo, String estadoSalud,
                  String numeroArete, String marcaHierro) {
        this.nombre = nombre;
        this.raza = raza;
        this.peso = peso;
        this.edad = edad;
        this.unidadTiempoEdad = unidadTiempoEdad;
        this.sexo = sexo;
        this.estadoSalud = estadoSalud;
        this.numeroArete = numeroArete;
        this.marcaHierro = marcaHierro;
        this.propietario = "Eder Castro";
        generarCodigoIdentificacion();
    }

    @PrePersist
    public void generarCodigoSiNoExiste() {
        if (this.codigoIdentificacion == null || this.codigoIdentificacion.isBlank()) {
            generarCodigoIdentificacion();
        }

        if (this.propietario == null || this.propietario.isBlank()) {
            this.propietario = "Eder Castro";
        }
    }

    public void generarCodigoIdentificacion() {
        String anio = String.valueOf(Year.now().getValue());
        String aleatorio = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        this.codigoIdentificacion = "BOV-" + anio + "-" + aleatorio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoIdentificacion() { return codigoIdentificacion; }
    public void setCodigoIdentificacion(String codigoIdentificacion) { this.codigoIdentificacion = codigoIdentificacion; }

    public String getNumeroArete() { return numeroArete; }
    public void setNumeroArete(String numeroArete) { this.numeroArete = numeroArete; }

    public String getMarcaHierro() { return marcaHierro; }
    public void setMarcaHierro(String marcaHierro) { this.marcaHierro = marcaHierro; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getEdad() { return edad; }
    public void setEdad(double edad) { this.edad = edad; }

    public String getUnidadTiempoEdad() { return unidadTiempoEdad; }
    public void setUnidadTiempoEdad(String unidadTiempoEdad) { this.unidadTiempoEdad = unidadTiempoEdad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getEstadoSalud() { return estadoSalud; }
    public void setEstadoSalud(String estadoSalud) { this.estadoSalud = estadoSalud; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }

    public List<Vacuna> getVacunas() { return vacunas; }
    public void setVacunas(List<Vacuna> vacunas) { this.vacunas = vacunas; }

    public void addVacuna(Vacuna vacuna) {
        vacunas.add(vacuna);
        vacuna.setGanado(this);
    }

    public void removeVacuna(Vacuna vacuna) {
        vacunas.remove(vacuna);
        vacuna.setGanado(null);
    }

    @Override
    public String toString() {
        return "Ganado{" +
                "id=" + id +
                ", codigoIdentificacion='" + codigoIdentificacion + '\'' +
                ", numeroArete='" + numeroArete + '\'' +
                ", marcaHierro='" + marcaHierro + '\'' +
                ", nombre='" + nombre + '\'' +
                ", raza='" + raza + '\'' +
                ", peso=" + peso +
                ", edad=" + edad + " " + unidadTiempoEdad +
                ", sexo='" + sexo + '\'' +
                ", estadoSalud='" + estadoSalud + '\'' +
                ", propietario='" + propietario + '\'' +
                '}';
    }
}
