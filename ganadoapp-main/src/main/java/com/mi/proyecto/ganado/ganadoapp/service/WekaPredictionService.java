package com.mi.proyecto.ganado.ganadoapp.service;

import org.springframework.stereotype.Service;
import weka.classifiers.functions.LinearRegression;
import weka.core.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class WekaPredictionService {

    public record ResultadoRegresionGrafico(
            List<Double> valoresAjustados,
            double prediccionSiguiente
    ) {}

    public double entrenarYPrediccion(
            List<LecheTrazabilidadService.ProduccionDiaria> datos
    ) throws Exception {

        if (datos == null || datos.isEmpty()) {
            return 0.0;
        }

        ArrayList<Attribute> atributos = new ArrayList<>();
        atributos.add(new Attribute("dia"));
        atributos.add(new Attribute("litros"));

        Instances dataset = new Instances("ProduccionLeche", atributos, datos.size());
        dataset.setClassIndex(1);

        int dia = 1;
        double sumaLitros = 0.0;

        for (var item : datos) {
            DenseInstance instancia = new DenseInstance(2);
            instancia.setValue(atributos.get(0), dia);
            instancia.setValue(atributos.get(1), item.litros());
            dataset.add(instancia);

            sumaLitros += item.litros();
            dia++;
        }

        // Promedio simple en caso de que la regresión falle o de un resultado incoherente
        double promedioHistorico = sumaLitros / datos.size();

        LinearRegression modelo = new LinearRegression();
        // Usamos opciones para eliminar atributos irrelevantes y evitar ruido matemático
        modelo.setOptions(new String[]{"-S", "1"});
        modelo.buildClassifier(dataset);

        DenseInstance futura = new DenseInstance(2);
        futura.setDataset(dataset);
        futura.setValue(0, datos.size() + 1);

        return sanitizarPrediccion(modelo.classifyInstance(futura), promedioHistorico);
    }

    public ResultadoRegresionGrafico calcularRegresionParaGrafico(
            List<LecheTrazabilidadService.ProduccionDiaria> datos
    ) throws Exception {
        if (datos == null || datos.size() < 3) {
            throw new IllegalArgumentException("Se requieren al menos 3 días con producción para la regresión.");
        }

        ArrayList<Attribute> atributos = new ArrayList<>();
        atributos.add(new Attribute("dia"));
        atributos.add(new Attribute("litros"));

        Instances dataset = new Instances("ProduccionLecheGrafico", atributos, datos.size());
        dataset.setClassIndex(1);

        int dia = 1;
        double sumaLitros = 0.0;

        for (var item : datos) {
            DenseInstance instancia = new DenseInstance(2);
            instancia.setValue(atributos.get(0), dia);
            instancia.setValue(atributos.get(1), item.litros());
            dataset.add(instancia);
            sumaLitros += item.litros();
            dia++;
        }

        double promedioHistorico = sumaLitros / datos.size();

        LinearRegression modelo = new LinearRegression();
        modelo.setOptions(new String[]{"-S", "1"});
        modelo.buildClassifier(dataset);

        List<Double> ajustados = new ArrayList<>();
        for (int indice = 1; indice <= datos.size(); indice++) {
            DenseInstance punto = new DenseInstance(2);
            punto.setDataset(dataset);
            punto.setValue(0, indice);
            ajustados.add(sanitizarPrediccion(modelo.classifyInstance(punto), promedioHistorico));
        }

        DenseInstance futura = new DenseInstance(2);
        futura.setDataset(dataset);
        futura.setValue(0, datos.size() + 1);
        double prediccion = sanitizarPrediccion(modelo.classifyInstance(futura), promedioHistorico);

        return new ResultadoRegresionGrafico(ajustados, prediccion);
    }

    private double sanitizarPrediccion(double prediccion, double promedioHistorico) {
        if (prediccion <= 0 || prediccion > (promedioHistorico * 2)) {
            return promedioHistorico;
        }
        return prediccion;
    }
}