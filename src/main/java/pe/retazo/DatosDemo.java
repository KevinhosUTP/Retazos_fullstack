package pe.retazo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DatosDemo implements CommandLineRunner {
    static final String EMAIL = "taller@retazo.local";
    private final JdbcClient db;
    private final BCryptPasswordEncoder encoder;
    private final String claveConfigurada;
    private final Optional<Escritorio> escritorio;

    DatosDemo(JdbcClient db, BCryptPasswordEncoder encoder,
              @Value("${retazo.demo.password:}") String clave, Optional<Escritorio> escritorio) {
        this.db = db;
        this.encoder = encoder;
        this.claveConfigurada = clave;
        this.escritorio = escritorio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (db.sql("SELECT COUNT(*) FROM usuario WHERE email=?").param(EMAIL).query(Integer.class).single() > 0) {
            return;
        }
        String clave = claveConfigurada;
        if (clave.isEmpty() && escritorio.isPresent()) {
            clave = escritorio.get().pedirClave();
        } else if (clave.isEmpty() && System.console() != null) {
            char[] entrada = System.console().readPassword(
                "Crea la contraseña de %s (mínimo 8 caracteres): ", EMAIL);
            if (entrada != null) {
                clave = new String(entrada);
                Arrays.fill(entrada, '\0');
            }
        }
        if (!claveValida(clave)) {
            throw new IllegalStateException("Antes del primer inicio, define RETAZO_DEMO_PASSWORD con al menos 8 caracteres y hasta 72 bytes.");
        }
        db.sql("INSERT INTO usuario(email, clave_hash) VALUES (?,?)")
            .params(EMAIL, encoder.encode(clave)).update();
        muestra("Lino", "#BD7256", 75, 60, "Estante A");
        muestra("Denim", "#40596F", 68, 40, "Cajón 2");
        muestra("Algodón", "#7B8C70", 42, 65, "Estante B");
    }

    static boolean claveValida(String clave) {
        return !clave.isBlank() && clave.length() >= 8 && clave.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private void muestra(String material, String color, int ancho, int largo, String ubicacion) {
        db.sql("INSERT INTO retazo(propietario, material, color, ancho, largo, ubicacion) VALUES (?,?,?,?,?,?)")
            .params(EMAIL, material, color, ancho, largo, ubicacion).update();
    }
}
