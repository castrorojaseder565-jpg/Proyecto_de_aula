package com.mi.proyecto.ganado.ganadoapp.controller;

import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import com.mi.proyecto.ganado.ganadoapp.service.EstadisticasService;
import com.mi.proyecto.ganado.ganadoapp.service.GanadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class EstadisticaController {



    private final EstadisticasService estadisticasService;


    public EstadisticaController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }


    @GetMapping("ganado/estadisticas")
    public String verEstadisticas(Model model) {

        Map<String, Object> estadisticas = estadisticasService.obtenerEstadisticas();

        model.addAllAttributes(estadisticas);

        return "estadisticas-ganado";
    }

}