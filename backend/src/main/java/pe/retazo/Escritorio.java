package pe.retazo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import org.springframework.boot.web.server.PortInUseException;

/** Ventanas del lanzador local. Las pantallas del taller se sirven desde Angular. */
class Escritorio {
    String pedirClave() {
        var respuesta = new FutureTask<String>(() -> {
            var entrada = new JPasswordField(22);
            Object[] mensaje = {"Crea la contraseña de " + DatosDemo.EMAIL,
                "Usa al menos 8 caracteres y hasta 72 bytes.", entrada};
            while (JOptionPane.showConfirmDialog(null, mensaje, "Retazo · Primer inicio",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                char[] caracteres = entrada.getPassword();
                String clave = new String(caracteres);
                Arrays.fill(caracteres, '\0');
                entrada.setText("");
                if (DatosDemo.claveValida(clave)) return clave;
                JOptionPane.showMessageDialog(null, "Escribe al menos 8 caracteres y hasta 72 bytes.",
                    "Revisa la contraseña", JOptionPane.WARNING_MESSAGE);
            }
            entrada.setText("");
            throw new CancellationException("Primer inicio cancelado.");
        });
        SwingUtilities.invokeLater(respuesta);
        try {
            return respuesta.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió el inicio.", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("No se completó el primer inicio.", error.getCause());
        }
    }

    void abrir(int puerto, Runnable detener) {
        URI sitio = URI.create("http://127.0.0.1:" + puerto + "/");
        SwingUtilities.invokeLater(() -> {
            var ventana = new JFrame("Retazo · Taller");
            var contenido = new JPanel(new BorderLayout(8, 10));
            contenido.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
            contenido.add(new JLabel("El taller está abierto en este equipo."), BorderLayout.NORTH);
            var direccion = new JTextField(sitio.toString(), 24);
            direccion.setEditable(false);
            direccion.getAccessibleContext().setAccessibleName("Dirección del taller");
            contenido.add(direccion, BorderLayout.CENTER);
            var acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            var abrir = new JButton("Abrir");
            abrir.addActionListener(evento -> navegar(sitio, ventana));
            var salir = new JButton("Salir");
            salir.addActionListener(evento -> ventana.dispatchEvent(new WindowEvent(ventana, WindowEvent.WINDOW_CLOSING)));
            acciones.add(abrir);
            acciones.add(salir);
            contenido.add(acciones, BorderLayout.SOUTH);
            ventana.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            ventana.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent evento) {
                    ventana.dispose();
                    new Thread(detener, "retazo-cierre").start();
                }
            });
            ventana.setContentPane(contenido);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
            navegar(sitio, ventana);
        });
    }

    private void navegar(URI sitio, JFrame ventana) {
        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(sitio);
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ventana,
                    "Abre tu navegador y pega esta dirección:\n" + sitio, "Abrir Retazo", JOptionPane.INFORMATION_MESSAGE));
            }
        }, "retazo-navegador").start();
    }

    void errorDeInicio(Throwable error) {
        String detalle = "Comprueba que extrajiste el JAR en una carpeta con permisos de escritura y "
            + "que no hay otro Retazo abierto usando sus datos.";
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof PortInUseException puerto) {
                detalle = "El puerto " + puerto.getPort() + " está ocupado. Cierra la otra instancia "
                    + "o inicia Retazo con --server.port=0.";
                break;
            }
        }
        String mensaje = "No se pudo abrir Retazo.\n" + detalle;
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, mensaje, "Retazo", JOptionPane.ERROR_MESSAGE));
    }
}
