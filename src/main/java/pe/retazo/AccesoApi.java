package pe.retazo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.server.ResponseStatusException;

@Configuration
class AccesoApi implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                boolean lectura = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
                // Esta cabecera requiere preflight en peticiones de otro origen. La API no habilita CORS.
                if (!lectura && !"1".equals(request.getHeader("X-Retazo"))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Petición de escritura no válida.");
                }
                boolean login = request.getRequestURI().equals(request.getContextPath() + "/api/auth/login")
                        && request.getMethod().equals("POST");
                var sesion = request.getSession(false);
                if (!login && (sesion == null || sesion.getAttribute("email") == null)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión.");
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
