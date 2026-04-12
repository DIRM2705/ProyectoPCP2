package Proyecto2.Cliente;

import Proyecto2.Codes;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

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
        setSize(650, 400);
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
            JButton btnListar = new JButton("Ver Red");
            panelControles.add(btnListar);

            // Le asignamos su acción:
            btnListar.addActionListener(e -> {
                        try {
                            procesarComando("listar");
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
        
        if (nombreDoc.isEmpty() && !"listar".equals(comando)) {
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
                imprimirLog("> Solicitando ubicaciones del archivo: " + nombreDoc);

                try {
                    // 1. Pedimos el mapa (enviamos comando 8)
                    servidor.pedirMapaUbicaciones(nombreDoc);

                    // 2. Esperamos a que nos devuelva el HashMap armado
                    HashMap<Integer, String> mapaUbicaciones = servidor.recibirMapaUbicaciones();

                    if (mapaUbicaciones == null || mapaUbicaciones.isEmpty()) {
                        imprimirLog("Error: El archivo '" + nombreDoc + "' no existe o no tiene fragmentos registrados.");
                        break;
                    }

                    imprimirLog("¡Mapa recibido! El archivo tiene " + mapaUbicaciones.size() + " fragmentos.");

                    // 3. ¡Iniciamos la Fase 2 de descarga!
                    iniciarDescarga(nombreDoc, mapaUbicaciones); 

                } catch (IOException e) {
                    imprimirLog("Error al pedir el archivo: " + e.getMessage());
                }
                break;
            case "borrar":
                imprimirLog("> Ejecutando comando BORRAR para: " + nombreDoc);
                servidor.enviarComando(Codes.DELETE_DOC, nombreDoc);
                break;
            case "listar":
    imprimirLog("> Consultando el directorio global...");
    
    // Envolvemos la petición en un Hilo nuevo para no congelar la GUI
    new Thread(() -> {
        try {
            List<String> archivos = servidor.pedirListaArchivos(); // Aquí va a esperar sin colgar la ventana
            
            if (archivos.isEmpty()) {
                imprimirLog("No hay ningún archivo compartido en la red todavía.");
            } else {
                imprimirLog("\n=== ARCHIVOS DISPONIBLES EN LA RED ===");
                for (String arc : archivos) {
                    imprimirLog(" - " + arc);
                }
                imprimirLog("======================================\n");
            }
        } catch (IOException ex) {
            imprimirLog("Error al pedir la lista al servidor: " + ex.getMessage());
        }
    }).start(); // ¡No olvides el .start()!
    break;        }
        
        // Limpiamos la caja de texto y la memoria del archivo después de usar un comando
        txtNombreArchivo.setText(""); 
        archivoSeleccionadoLocal = null;
    }
// NUEVO MÉTODO en tu ClienteMainGUI
private void iniciarDescarga(String nombreDoc, HashMap<Integer, String> mapaUbicaciones) throws IOException {
    // Aquí guardaremos los pedazos conforme vayan llegando
    List<Fragmento> fragmentosRecolectados = new ArrayList<>();
    
    imprimirLog("Iniciando descarga P2P...");

    // Recorremos el mapa que nos dio el servidor
   // ... inicio del método iniciarDescarga ...

    // Recorremos el mapa y pedimos los pedazos
    for (Map.Entry<Integer, String> entrada : mapaUbicaciones.entrySet()) {
        int numeroFragmento = entrada.getKey();
        String ipVecino = entrada.getValue();
        
        Fragmento frag = pedirFragmentoAVecino(ipVecino, nombreDoc, numeroFragmento);
        
        if (frag != null) {
            fragmentosRecolectados.add(frag);
        } else {
            imprimirLog("Fallo al obtener el fragmento " + numeroFragmento);
            return; // Abortamos si falta un pedazo
        }
    } 
    imprimirLog("¡Todos los fragmentos descargados con éxito!");
    
    String rutaDestino = "descarga_" + nombreDoc; 
    
    try {
        // 1. Pegamos los pedazos físicamente en el disco duro
        GestorFragmentos.ensamblarArchivo(fragmentosRecolectados, rutaDestino);
        
        // 2. Le pedimos a Windows que lo abra (solo 1 vez)
        abrirArchivoConSistemaOperativo(rutaDestino);
        
    } catch (IOException e) {
        imprimirLog("Error al intentar ensamblar el archivo: " + e.getMessage());
    }
}
private void abrirArchivoConSistemaOperativo(String rutaDelArchivo) {
    try {
        File archivoFisico = new File(rutaDelArchivo);
        
        // 1. Verificamos que el archivo realmente exista en el disco duro
        if (!archivoFisico.exists()) {
            imprimirLog("Error: No se puede abrir porque el archivo no se encontró en " + rutaDelArchivo);
            return;
        }
        
        // 2. Verificamos si la computadora soporta esta función (el 99% de Windows/Mac lo hacen)
        if (!Desktop.isDesktopSupported()) {
            imprimirLog("Tu sistema operativo no soporta la apertura automática de archivos.");
            return;
        }
        
        // 3. ¡La magia! Le pedimos a Windows/Mac que lo abra
        Desktop desktop = Desktop.getDesktop();
        desktop.open(archivoFisico);
        
        imprimirLog("Abriendo archivo: " + archivoFisico.getName() + "...");
        
    } catch (IOException ex) {
        imprimirLog("Error al intentar abrir el archivo con el sistema: " + ex.getMessage());
    } catch (IllegalArgumentException ex) {
        imprimirLog("El archivo no existe o la ruta es inválida.");
    }
}
// NUEVO MÉTODO para conectarse y pedir un solo pedazo
private Fragmento pedirFragmentoAVecino(String ipVecino, String nombreDoc, int numSecuencia) {
    try (Socket socket = new Socket(ipVecino, 1236);
         ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
         ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
        
        // 1. Le decimos al vecino "Dame este archivo y este pedazo exacto"
        salidaObj.writeUTF("GET:" + nombreDoc + ":" + numSecuencia);
        salidaObj.flush();
        
        // 2. Nos quedamos esperando a que el vecino nos aviente el objeto Fragmento
        Fragmento fragmentoRecibido = (Fragmento) entradaObj.readObject();
        return fragmentoRecibido;
        
    } catch (Exception e) {
        System.err.println("Fallo al pedir fragmento a " + ipVecino + ": " + e.getMessage());
        return null;
    }
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