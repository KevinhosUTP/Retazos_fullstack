package pe.retazo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class SesionController {
    record Credenciales(@NotBlank @Email @Size(max=254) String email,
                        @NotBlank @Size(max=72) String password) {}
    record Usuario(String email) {}

    private final JdbcClient db;
    private final BCryptPasswordEncoder encoder;

    SesionController(JdbcClient db, BCryptPasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
    }

    @PostMapping(path="/login", consumes=MediaType.APPLICATION_JSON_VALUE)
    Usuario login(@Valid @RequestBody Credenciales datos, HttpServletRequest request) {
        String email = datos.email().toLowerCase(Locale.ROOT);
        var hash = db.sql("SELECT clave_hash FROM usuario WHERE email=?")
            .param(email).query(String.class).optional();
        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72
                || hash.isEmpty() || !encoder.matches(datos.password(), hash.get())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }
        var anterior = request.getSession(false);
        if (anterior != null) anterior.invalidate();
        request.getSession(true).setAttribute("email", email);
        return new Usuario(email);
    }

    @GetMapping("/me")
    Usuario actual(@SessionAttribute String email) {
        return new Usuario(email);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void salir(HttpServletRequest request) {
        request.getSession(false).invalidate();
    }
}
