package Proyecto2.Cliente;

import Proyecto2.Codes;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class ClienteMainGUI extends JFrame {
    
    private JTextArea areaConsola;
    private JTextField txtNombreArchivo;
    private ConexionServidor servidor;
    
    // Variable para guardar el archivo real que el usuario seleccionó con la ventana
    private File archivoSeleccionadoLocal = null;

    public ClienteMainGUI() {
        super("Cliente P2P - Archivos Distribuidos");
        
        configurarInterfaz();
        
        String ipIngresada = JOptionPane.showInputDialog(this, 
                "Ingresa la IP del Servidor Central:", "192.168.1.69");
        
        if (ipIngresada == null || ipIngresada.trim().isEmpty()) {
            System.exit(0); 
        }

        iniciarRed(ipIngresada.trim());
    }

    private void configurarInterfaz() {
        setSize(650, 400); // Hice la ventana un poco más ancha para que quepan los botones
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Panel Central (La "Consola") ---
        areaConsola = new JTextArea();
        areaConsola.setEditable(false);
        areaConsola.setBackground(Color.BLACK);
        areaConsola.setForeground(Color.GREEN);
        areaConsola.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scroll = new JScrollPane(areaConsola);
        add(scroll, BorderLayout.CENTER);

        // --- Panel Inferior (Controles) ---
        JPanel panelControles = new JPanel();
        panelControles.setLayout(new FlowLayout());

        panelControles.add(new JLabel("Archivo:"));
        
        txtNombreArchivo = new JTextField(15);
        panelControles.add(txtNombreArchivo);

        // NUEVO BOTÓN: Para abrir el JFileChooser
        JButton btnBuscar = new JButton("Examinar...");
        panelControles.add(btnBuscar);

        JButton btnCrear = new JButton("Crear (Subir)");
        JButton btnAbrir = new JButton("Abrir (Descargar)");
        JButton btnBorrar = new JButton("Borrar");

        panelControles.add(btnCrear);
        panelControles.add(btnAbrir);
        panelControles.add(btnBorrar);

        add(panelControles, BorderLayout.SOUTH);

        // --- Asignar acciones a los botones ---
        btnBuscar.addActionListener(e -> abrirBuscadorArchivos());
        btnCrear.addActionListener(e -> {
            try {
                procesarComando("crear");
            } catch (IOException ex) {
                System.getLogger(ClienteMainGUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
        btnAbrir.addActionListener(e -> {
            try {
                procesarComando("abrir");
            } catch (IOException ex) {
                System.getLogger(ClienteMainGUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
        btnBorrar.addActionListener(e -> {
            try {
                procesarComando("borrar");
            } catch (IOException ex) {
                System.getLogger(ClienteMainGUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
    }

    // NUEVO MÉTODO: Lógica del JFileChooser
    private void abrirBuscadorArchivos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona un archivo para compartir");
        
        int seleccion = fileChooser.showOpenDialog(this);
        
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionadoLocal = fileChooser.getSelectedFile();
            // Ponemos solo el nombre del archivo en la caja de texto
            txtNombreArchivo.setText(archivoSeleccionadoLocal.getName());
            imprimirLog("Archivo preparado: " + archivoSeleccionadoLocal.getAbsolutePath());
        }
    }

    private void imprimirLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            areaConsola.append(mensaje + "\n");
            areaConsola.setCaretPosition(areaConsola.getDocument().getLength());
        });
    }

    private void iniciarRed(String ipServidor) {
        imprimirLog("=== Iniciando Sistema de Archivos Distribuidos ===");

        Thread hiloBroadcast = new Thread(new ReceptorBroadcast());
        hiloBroadcast.setDaemon(true); 
        hiloBroadcast.start();
        imprimirLog("Escuchando red local (Broadcast)...");

        try {
            servidor = new ConexionServidor(ipServidor, 1235);
            
            Thread hiloP2P = new Thread(new ServidorP2P(servidor));
            hiloP2P.setDaemon(true);
            hiloP2P.start();

            imprimirLog("Conexión establecida con el servidor en: " + ipServidor);
            imprimirLog("Listo para recibir instrucciones.");

        } catch (Exception e) {
            imprimirLog("Fallo al conectar con el servidor TCP: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesarComando(String comando) throws IOException {
        String nombreDoc = txtNombreArchivo.getText().trim();
        
        if (nombreDoc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, escribe un nombre o selecciona un archivo.");
            return;
        }

        if (servidor == null) {
            imprimirLog("Error: No estás conectado al servidor.");
            return;
        }

        switch (comando) {
            case "crear":
                imprimirLog("> Ejecutando comando CREAR para: " + nombreDoc);
                
                File archivoLocal;
                // Verificamos si el usuario usó el botón de "Examinar" y el nombre coincide
                if (archivoSeleccionadoLocal != null && archivoSeleccionadoLocal.getName().equals(nombreDoc)) {
                    archivoLocal = archivoSeleccionadoLocal;
                } else {
                    // Si el usuario lo escribió a mano, lo busca en la carpeta por defecto
                    archivoLocal = new File(nombreDoc);
                }

                if (!archivoLocal.exists()) {
                    imprimirLog("Error: El archivo no existe en la ruta: " + archivoLocal.getAbsolutePath());
                    break;
                }
                
                servidor.enviarComando(Codes.NEW_DOC, nombreDoc);
                
                try {
                    List<Fragmento> fragmentos = GestorFragmentos.dividirArchivo(archivoLocal, nombreDoc);
                    imprimirLog("El archivo se dividió en " + fragmentos.size() + " fragmentos.");

                    List<String> vecinosDisponibles = new java.util.ArrayList<>(ReceptorBroadcast.vecinos);
                    
                    if (vecinosDisponibles.isEmpty()) {
                        imprimirLog("No hay vecinos. Guardando localmente...");
                        for (Fragmento frag : fragmentos) {
                            ServidorP2P.misFragmentos.put(nombreDoc + ":" + frag.getNumeroSecuencia(), frag);
                            servidor.reportarFragmento(nombreDoc, frag.getNumeroSecuencia());
                        }
                    } else {
                        imprimirLog("Distribuyendo entre " + vecinosDisponibles.size() + " vecinos...");
                        for (int i = 0; i < fragmentos.size(); i++) {
                            Fragmento frag = fragmentos.get(i);
                            String ipDestino = vecinosDisponibles.get(i % vecinosDisponibles.size());
                            enviarFragmentoAVecino(ipDestino, frag);
                        }
                        imprimirLog("Distribución completada.");
                    }
                } catch (IOException ex) {
                    imprimirLog("Error al procesar: " + ex.getMessage());
                }
                break;

            case "abrir":
                imprimirLog("> Ejecutando comando ABRIR para: " + nombreDoc);
                servidor.enviarComando(Codes.OPEN_DOC, nombreDoc);
                break;

            case "borrar":
                imprimirLog("> Ejecutando comando BORRAR para: " + nombreDoc);
                servidor.enviarComando(Codes.DELETE_DOC, nombreDoc);
                break;
        }
        
        // Limpiamos la caja de texto y la memoria del archivo después de usar un comando
        txtNombreArchivo.setText(""); 
        archivoSeleccionadoLocal = null;
    }

    public static void enviarFragmentoAVecino(String ipVecino, Fragmento fragmento) {
        try (Socket socket = new Socket(ipVecino, 1236);
            ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
            
            salidaObj.writeUTF("STORE");
            salidaObj.writeObject(fragmento);
            salidaObj.flush();
            
        } catch (IOException e) {
            System.err.println("No se pudo enviar el fragmento a la IP: " + ipVecino);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteMainGUI ventana = new ClienteMainGUI();
            ventana.setLocationRelativeTo(null); 
            ventana.setVisible(true);
        });
    }
}