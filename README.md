# Retazo

Retazo es un inventario de sobrantes de tela para un taller de costura. Puedes registrar dónde guardaste una tela y consultar si alcanza para cortar una pieza rectangular, incluso si tienes que girarla 90°.

## Abrir la demo

1. Extrae `Retazo.zip` en una carpeta donde puedas guardar archivos. El ZIP contiene un único archivo, `Retazo.jar`.
2. Con **Java 17 o 22** instalado y asociado a los archivos JAR en Windows, haz doble clic en `Retazo.jar`. La primera vez aparecerá una ventana para crear la contraseña del taller. Usa al menos ocho caracteres y hasta 72 bytes en UTF-8.
3. Retazo abre el navegador. Inicia sesión con **taller@retazo.local** y la contraseña que elegiste.

Para detener la aplicación, cierra la ventana de Retazo. La base H2 se guarda en la carpeta `data/` al lado del JAR. Conserva esa carpeta si mueves el programa a otro lugar. También puedes arrancar con `java -jar Retazo.jar`. Para ejecución automatizada puedes indicar `-Djava.awt.headless=true` antes de `-jar` y suministrar la contraseña inicial mediante la variable de entorno `RETAZO_DEMO_PASSWORD`.

## Recorrido para exponer

1. Añade un retazo de **lino**, elige un color, indica **55 cm de ancho × 35 cm de largo** y guárdalo en **Cajón 2**.
2. Busca una pieza de **30 × 50 cm**. El retazo aparece porque al girarla ocupa **50 × 30 cm**, que sí caben en el lino.
3. Cambia el ancho del retazo a **45 cm** y repite la búsqueda. Ahora ninguna orientación permite cortar esa pieza.
4. Quita el filtro, localiza el lino por su ubicación y elimínalo. Puedes cancelar la confirmación antes de probar el borrado.

## Trabajar con el código

El directorio `frontend/` contiene la interfaz Angular y `backend/` contiene la API Spring Boot. Para desarrollo, usa JDK 17 y Node.js 22.12 o una versión posterior de la rama 22, con npm. El backend incluye Maven Wrapper, que descarga Maven la primera vez si hace falta. [Maven Wrapper](https://maven.apache.org/tools/wrapper/) · [Versiones de Node compatibles con Angular](https://angular.dev/reference/versions)

Desde la raíz de Retazo, abre dos terminales de PowerShell. En la primera ejecuta

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En la segunda ejecuta

```powershell
cd frontend
npm.cmd ci
npm.cmd start
```

Abre **http://127.0.0.1:4200**. El servidor de Angular reenvía `/api` al backend del puerto 8080. Para probar la API automáticamente, ejecuta `.\mvnw.cmd test` en `backend/`. Las pruebas utilizan una base en memoria. En macOS y Linux, usa `./mvnw` en lugar de `.\mvnw.cmd` y `npm` en lugar de `npm.cmd`.

Para reconstruir el archivo ejecutable, ejecuta `npm.cmd run build` en `frontend/` y después `.\mvnw.cmd package` en `backend/`. Maven incorpora la interfaz compilada al JAR que deja en `backend/target/`.

## Contrato de la API

| Método y ruta | Operación |
| --- | --- |
| `POST /api/auth/login` | Recibe `email` y `password`. Inicia la sesión y envía su identificador en una cookie HTTP-only. |
| `GET /api/auth/me` | Devuelve el correo de la sesión activa. |
| `POST /api/auth/logout` | Cierra la sesión. |
| `GET /api/retazos` | Lista los retazos del usuario. Acepta juntos `anchoMin` y `largoMin` para buscar en ambas orientaciones. |
| `POST /api/retazos` | Crea un retazo. |
| `PUT /api/retazos/{id}` | Actualiza el material, color, medidas y ubicación de un retazo propio. |
| `DELETE /api/retazos/{id}` | Elimina un retazo propio. |

Angular envía la cabecera `X-Retazo: 1` en las operaciones de escritura. Los cuerpos JSON de creación y edición tienen esta forma:

```json
{
  "material": "Lino",
  "color": "#B26750",
  "ancho": 55,
  "largo": 35,
  "ubicacion": "Cajón 2"
}
```

Las medidas se expresan en centímetros enteros entre 1 y 10 000. La búsqueda devuelve `girado: true` cuando la pieza solo cabe al intercambiar ancho y largo. Si ambas orientaciones caben, el resultado conserva la directa.

### Modern Web Guidance

- **autofill-sign-in-form**. Apliqué «`autocomplete="username"`» al campo de correo para que el navegador reconozca la cuenta y pueda completar el login.
- **forms**. Seguí «move focus to the first invalid field» para que al enviar el formulario se pueda corregir el campo directamente con el teclado.
- **dialog**. Usé «`dialog.showModal()`» para confirmar el borrado, con foco inicial en Cancelar y cierre con Escape.
- **accessibility**. Adopté «Announce dynamic errors via live regions» para que los lectores de pantalla comuniquen un fallo de acceso.
