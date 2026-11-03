package com.mi.proyecto.ganado.ganadoapp;

import com.mi.proyecto.ganado.ganadoapp.repository.GanadoRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PrediccionLechePageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GanadoRepository ganadoRepository;

    @Test
    void prediccionPageDebeResponderOk() throws Exception {
        Long ganadoId = ganadoRepository.findAll().stream()
                .findFirst()
                .map(g -> g.getId())
                .orElse(null);
        Assumptions.assumeTrue(ganadoId != null, "No hay ganado en la base de datos de prueba");

        mockMvc.perform(get("/ganado/prediccion/" + ganadoId))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
