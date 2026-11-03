package com.mi.proyecto.ganado.ganadoapp.mode;

import java.util.ArrayList;
import java.util.List;

public class DatosGraficoLeche {

    private List<String> etiquetas = new ArrayList<>();
    private List<Double> produccionReal = new ArrayList<>();
    private List<Double> lineaRegresion = new ArrayList<>();
    private double valorPrediccion;
    private boolean regresionDisponible;

    public DatosGraficoLeche() {
    }

    public DatosGraficoLeche(
            List<String> etiquetas,
            List<Double> produccionReal,
            List<Double> lineaRegresion,
            double valorPrediccion,
            boolean regresionDisponible
    ) {
        this.etiquetas = etiquetas != null ? etiquetas : new ArrayList<>();
        this.produccionReal = produccionReal != null ? produccionReal : new ArrayList<>();
        this.lineaRegresion = lineaRegresion != null ? lineaRegresion : new ArrayList<>();
        this.valorPrediccion = valorPrediccion;
        this.regresionDisponible = regresionDisponible;
    }

    public static DatosGraficoLeche vacio() {
        return new DatosGraficoLeche(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, false);
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public List<Double> getProduccionReal() {
        return produccionReal;
    }

    public void setProduccionReal(List<Double> produccionReal) {
        this.produccionReal = produccionReal;
    }

    public List<Double> getLineaRegresion() {
        return lineaRegresion;
    }

    public void setLineaRegresion(List<Double> lineaRegresion) {
        this.lineaRegresion = lineaRegresion;
    }

    public double getValorPrediccion() {
        return valorPrediccion;
    }

    public void setValorPrediccion(double valorPrediccion) {
        this.valorPrediccion = valorPrediccion;
    }

    public boolean isRegresionDisponible() {
        return regresionDisponible;
    }

    public void setRegresionDisponible(boolean regresionDisponible) {
        this.regresionDisponible = regresionDisponible;
    }
}
