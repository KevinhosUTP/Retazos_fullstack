package pe.retazo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/retazos")
class RetazosController {
    record Datos(@NotBlank @Size(max=80) String material,
                 @NotNull @Pattern(regexp="#[a-fA-F0-9]{6}") String color,
                 @NotNull @Min(1) @Max(10000) Integer ancho,
                 @NotNull @Min(1) @Max(10000) Integer largo,
                 @NotBlank @Size(max=80) String ubicacion) {
        Datos {
            if (material != null) material = material.strip();
            if (color != null) color = color.toUpperCase(Locale.ROOT);
            if (ubicacion != null) ubicacion = ubicacion.strip();
        }
        Retazo conId(long id) {
            return new Retazo(id, material, color, ancho, largo, ubicacion, false);
        }
    }

    record Retazo(long id, String material, String color, int ancho, int largo,
                  String ubicacion, boolean girado) {}

    private final JdbcClient db;

    RetazosController(JdbcClient db) {
        this.db = db;
    }

    @GetMapping
    List<Retazo> listar(@SessionAttribute String email,
                        @RequestParam(required=false) @Min(1) @Max(10000) Integer anchoMin,
                        @RequestParam(required=false) @Min(1) @Max(10000) Integer largoMin) {
        if ((anchoMin == null) != (largoMin == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica ancho y largo juntos.");
        }
        // Con mínimos cero todos los retazos caben directamente.
        return db.sql("""
            SELECT id, material, color, ancho, largo, ubicacion,
                   NOT (ancho >= :ancho AND largo >= :largo) AS girado
              FROM retazo
             WHERE propietario = :email AND ((ancho >= :ancho AND largo >= :largo)
                                         OR (ancho >= :largo AND largo >= :ancho))
             ORDER BY id DESC
            """)
            .param("email", email).param("ancho", anchoMin == null ? 0 : anchoMin)
            .param("largo", largoMin == null ? 0 : largoMin).query(Retazo.class).list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Retazo crear(@SessionAttribute String email, @Valid @RequestBody Datos datos) {
        var clave = new GeneratedKeyHolder();
        db.sql("INSERT INTO retazo(propietario, material, color, ancho, largo, ubicacion) VALUES (?,?,?,?,?,?)")
            .params(email, datos.material(), datos.color(), datos.ancho(), datos.largo(), datos.ubicacion())
            .update(clave, "id");
        return datos.conId(clave.getKey().longValue());
    }

    @PutMapping("/{id}")
    Retazo editar(@SessionAttribute String email, @PathVariable long id, @Valid @RequestBody Datos datos) {
        int filas = db.sql("UPDATE retazo SET material=?, color=?, ancho=?, largo=?, ubicacion=? WHERE id=? AND propietario=?")
            .params(datos.material(), datos.color(), datos.ancho(), datos.largo(), datos.ubicacion(), id, email).update();
        comprobarFila(filas);
        return datos.conId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@SessionAttribute String email, @PathVariable long id) {
        comprobarFila(db.sql("DELETE FROM retazo WHERE id=? AND propietario=?").params(id, email).update());
    }

    private void comprobarFila(int filas) {
        if (filas == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retazo no encontrado.");
    }
}
