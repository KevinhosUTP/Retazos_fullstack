package pe.retazo;

import java.awt.GraphicsEnvironment;
import java.util.Map;
import java.util.concurrent.CancellationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class RetazoApplication {
    public static void main(String[] args) {
        var app = new SpringApplication(RetazoApplication.class);
        var origen = new ApplicationHome(RetazoApplication.class);
        boolean desdeJar = origen.getSource() != null && origen.getSource().isFile();
        String preferencia = new SimpleCommandLinePropertySource(args).getProperty("retazo.escritorio");
        boolean conVentana = desdeJar && !GraphicsEnvironment.isHeadless()
            && (preferencia == null || Boolean.parseBoolean(preferencia));
        if (desdeJar) {
            String archivo = origen.getDir().toPath().resolve("data/retazo").toString()
                .replace('\\', '/').replace(";", "\\;");
            app.setDefaultProperties(Map.of("retazo.archivo-datos", archivo,
                    "server.port", conVentana ? "0" : "8080"));
        }

        if (!conVentana) {
            app.run(args);
            return;
        }
        var escritorio = new Escritorio();
        app.setHeadless(false);
        app.addInitializers(contexto -> contexto.getBeanFactory().registerSingleton("escritorio", escritorio));
        try {
            var contexto = (ServletWebServerApplicationContext) app.run(args);
            escritorio.abrir(contexto.getWebServer().getPort(), contexto::close);
        } catch (RuntimeException error) {
            if (!(NestedExceptionUtils.getMostSpecificCause(error) instanceof CancellationException)) {
                escritorio.errorDeInicio(error);
            }
        }
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
