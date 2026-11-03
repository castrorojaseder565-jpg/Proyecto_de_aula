package com.mi.proyecto.ganado.ganadoapp.service;

import com.mi.proyecto.ganado.ganadoapp.repository.GanadoRepository;
import com.mi.proyecto.ganado.ganadoapp.repository.VacunaRepository;
import com.mi.proyecto.ganado.ganadoapp.mode.Ganado;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class EstadisticasService {

    private final GanadoRepository ganadoRepository;
    private final VacunaRepository vacunaRepository;
    private final LecheTrazabilidadService lecheService;

    public EstadisticasService(GanadoRepository ganadoRepository, VacunaRepository vacunaRepository,
                               LecheTrazabilidadService lecheService) {
        this.ganadoRepository = ganadoRepository;
        this.vacunaRepository = vacunaRepository;
        this.lecheService = lecheService;
    }

    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> datos = new HashMap<>();

        //Métricas Simples y Conteo
        Long totalGanado = ganadoRepository.count();
        datos.put("totalGanado", totalGanado);

        List<Ganado> ganados = ganadoRepository.findAll();
        datos.put("pesoMinimo", ganados.stream().mapToDouble(Ganado::getPeso).min().orElse(0));
        datos.put("pesoMaximo", ganados.stream().mapToDouble(Ganado::getPeso).max().orElse(0));
        Double edadPromedio = ganadoRepository.promedioEdad();
        datos.put("promedioEdad", edadPromedio != null ? edadPromedio : 0.0);
        Map<String, Long> porEdad = new LinkedHashMap<>();
        porEdad.put("Menos de 1 año", ganados.stream().filter(g -> g.getEdad() < 1).count());
        porEdad.put("1 a 3 años", ganados.stream().filter(g -> g.getEdad() >= 1 && g.getEdad() < 3).count());
        porEdad.put("3 a 8 años", ganados.stream().filter(g -> g.getEdad() >= 3 && g.getEdad() < 8).count());
        porEdad.put("8 años o más", ganados.stream().filter(g -> g.getEdad() >= 8).count());
        datos.put("porEdad", porEdad);
        Map<String, Long> porEstadoSaludCompleto = ganados.stream()
                .map(g -> g.getEstadoSalud() == null || g.getEstadoSalud().isBlank() ? "Sin dato" : g.getEstadoSalud())
                .collect(Collectors.groupingBy(estado -> estado, LinkedHashMap::new, Collectors.counting()));
        datos.put("porEstadoSaludCompleto", porEstadoSaludCompleto);

        List<LecheTrazabilidadService.ResumenLecheGanado> produccion = lecheService.generarResumenLecheGanado();
        double prediccion7 = produccion.stream().mapToDouble(LecheTrazabilidadService.ResumenLecheGanado::prediccion7Dias).sum();
        datos.put("lecheDiaria", produccion.stream().mapToDouble(LecheTrazabilidadService.ResumenLecheGanado::litrosDiaPromedio).sum());
        datos.put("leche7Dias", lecheService.totalLecheHatoPeriodo(7));
        datos.put("leche30Dias", lecheService.totalLecheHatoPeriodo(30));
        datos.put("prediccionLecheManana", Math.round(prediccion7 / 7.0 * 10.0) / 10.0);
        datos.put("prediccionLeche7Dias", Math.round(prediccion7 * 10.0) / 10.0);
        datos.put("prediccionLeche30Dias", Math.round(prediccion7 / 7.0 * 30.0 * 10.0) / 10.0);

        //Peso promedio
        Double pesoPromedio = ganadoRepository.promedioPeso();
        datos.put("promedioPeso", pesoPromedio != null ? pesoPromedio : 0.0);

        //Conteo por Sexo
        datos.put("machos", ganadoRepository.countBySexo("Macho"));
        datos.put("hembras", ganadoRepository.countBySexo("Hembra"));

        //Conteo por Estado de Salud (solo los existentes)
        datos.put("saludables", ganadoRepository.countByEstadoSalud("Saludable"));
        datos.put("enfermos", ganadoRepository.countByEstadoSalud("Enfermo"));

        //Distribución por Raza
        List<Object[]> razaCounts = ganadoRepository.countByRaza();
        Map<String, Long> porRaza = razaCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1],
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));
        datos.put("porRaza", porRaza);

        //Distribución por Estado de Salud
        Map<String, Long> porEstadoSalud = new LinkedHashMap<>();
        porEstadoSalud.put("Saludable", (Long) datos.get("saludables"));
        porEstadoSalud.put("Enfermo", (Long) datos.get("enfermos"));

        datos.put("porEstadoSalud", porEstadoSalud);

        datos.put("proximasVacunas", vacunaRepository.findQueRequierenAtencionHasta(LocalDate.now().plusDays(30))
                .stream().limit(5).toList());
        datos.put("totalVacunasProximas", vacunaRepository.findVacunasPorVencer(LocalDate.now().plusDays(30)).size());

        return datos;
    }
}
