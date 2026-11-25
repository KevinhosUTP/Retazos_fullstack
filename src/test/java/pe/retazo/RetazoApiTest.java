package pe.retazo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pruebas")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Transactional
class RetazoApiTest {
    private static final String CLAVE_DE_PRUEBA = UUID.randomUUID().toString();
    private static final String BASE = "/api/retazos";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcClient db;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("retazo.demo.password", () -> CLAVE_DE_PRUEBA);
    }

    private String credenciales(String clave) throws Exception {
        return json.writeValueAsString(Map.of("email", DatosDemo.EMAIL, "password", clave));
    }

    private MockHttpSession entrar() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").header("X-Retazo", "1")
                .contentType(APPLICATION_JSON).content(credenciales(CLAVE_DE_PRUEBA)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(DatosDemo.EMAIL))
            .andReturn().getRequest().getSession(false);
    }

    private String pieza(int ancho) throws Exception {
        return json.writeValueAsString(Map.of("material", "Lino de prueba", "color", "#8a541d",
            "ancho", ancho, "largo", 35, "ubicacion", "Cajón de prueba"));
    }

    private long crear(MockHttpSession sesion) throws Exception {
        var resultado = mvc.perform(post(BASE).session(sesion).header("X-Retazo", "1")
                .contentType(APPLICATION_JSON).content(pieza(55)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.color").value("#8A541D"))
            .andReturn().getResponse().getContentAsByteArray();
        return json.readTree(resultado).get("id").longValue();
    }

    @Test
    void validaCredencialesProtegeEscriturasYCierraSesion() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(credenciales(CLAVE_DE_PRUEBA)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").header("X-Retazo", "1").contentType(APPLICATION_JSON)
            .content("{\"email\":\"invalido\",\"password\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").header("X-Retazo", "1").contentType(APPLICATION_JSON)
            .content(credenciales(UUID.randomUUID().toString()))).andExpect(status().isUnauthorized());
        var sesion = entrar();
        mvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(DatosDemo.EMAIL));
        mvc.perform(post(BASE).session(sesion).contentType(APPLICATION_JSON).content(pieza(55)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").header("X-Retazo", "1").session(sesion))
            .andExpect(status().isNoContent());
        assertTrue(sesion.isInvalid());
    }

    @Test
    void creaBuscaGiraEditaYElimina() throws Exception {
        var sesion = entrar();
        long id = crear(sesion);
        mvc.perform(get(BASE).session(sesion).param("anchoMin", "30").param("largoMin", "50"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].girado").value(true));
        mvc.perform(get(BASE).session(sesion).param("anchoMin", "55").param("largoMin", "35"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].girado").value(false));
        mvc.perform(put(BASE + "/" + id).session(sesion).header("X-Retazo", "1")
            .contentType(APPLICATION_JSON).content(pieza(45))).andExpect(status().isOk());
        mvc.perform(get(BASE).session(sesion).param("anchoMin", "30").param("largoMin", "50"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + id + ")]", hasSize(0)));
        mvc.perform(delete(BASE + "/" + id).session(sesion).header("X-Retazo", "1"))
            .andExpect(status().isNoContent());
        mvc.perform(delete(BASE + "/" + id).session(sesion).header("X-Retazo", "1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void rechazaMedidasYColorInvalidos() throws Exception {
        var sesion = entrar();
        mvc.perform(get(BASE).session(sesion).param("anchoMin", "10")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).session(sesion).param("anchoMin", "0").param("largoMin", "3"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(BASE).session(sesion).header("X-Retazo", "1").contentType(APPLICATION_JSON)
            .content(pieza(10001))).andExpect(status().isBadRequest());
        mvc.perform(post(BASE).session(sesion).header("X-Retazo", "1").contentType(APPLICATION_JSON)
            .content(pieza(55).replace("#8a541d", "rojo"))).andExpect(status().isBadRequest());
        mvc.perform(post(BASE).session(sesion).header("X-Retazo", "1").contentType(APPLICATION_JSON)
            .content(pieza(55).replace("55", "55.5"))).andExpect(status().isBadRequest());
    }

    @Test
    void aislaElInventarioPorPropietario() throws Exception {
        var sesion = entrar();
        String otroEmail = "otro@retazo.local";
        db.sql("INSERT INTO usuario SELECT ?, clave_hash FROM usuario WHERE email=?")
            .params(otroEmail, DatosDemo.EMAIL).update();
        long id = crear(sesion);
        db.sql("UPDATE retazo SET propietario=? WHERE id=?").params(otroEmail, id).update();
        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + id + ")]", hasSize(0)));
        mvc.perform(put(BASE + "/" + id).session(sesion).header("X-Retazo", "1")
            .contentType(APPLICATION_JSON).content(pieza(45))).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/" + id).session(sesion).header("X-Retazo", "1"))
            .andExpect(status().isNotFound());
    }
}
