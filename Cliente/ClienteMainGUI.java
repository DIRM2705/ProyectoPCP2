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
    private int miPuertoDinamico;
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
            new Thread(() -> { // Hilo separado para no trabar la GUI
                try {
                    procesarComando("crear");
                } catch (IOException ex) {
                    imprimirLog("Error: " + ex.getMessage());
                }
            }).start();
        });

        btnAbrir.addActionListener(e -> {
            new Thread(() -> { // Hilo separado para la descarga P2P
                try {
                    procesarComando("abrir");
                } catch (IOException ex) {
                    imprimirLog("Error: " + ex.getMessage());
                }
            }).start();
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

            ServidorP2P miServidorP2P = new ServidorP2P(servidor);
            Thread hiloP2P = new Thread(miServidorP2P);
            miPuertoDinamico = miServidorP2P.getPuertoP2P();
            hiloP2P.setDaemon(true);
            hiloP2P.start();

            imprimirLog("Conexión establecida con el servidor en: " + ipServidor);
            imprimirLog("Puerto P2P dinámico asignado: " + miPuertoDinamico);
            // --- NUEVA LÍNEA AQUÍ ---
            // Le decimos al servidor local que lea el disco y reporte lo que tiene
            servidor.enviarComando(Codes.NEW_CLIENT, String.valueOf(miPuertoDinamico));
            miServidorP2P.reportarInventarioLocal();
            
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

                    List<String> todosLosVecinos = new java.util.ArrayList<>(ReceptorBroadcast.vecinos);
                    HashMap<String, List<String>> maquinasFisicas = new HashMap<>();

                    for (String nodo : todosLosVecinos) {
                        String ipReal = nodo.split(":")[0];
                        maquinasFisicas.putIfAbsent(ipReal, new ArrayList<>());
                        maquinasFisicas.get(ipReal).add(nodo);
                    }

                    List<String> ipsDisponibles = new ArrayList<>(maquinasFisicas.keySet());
                    // =====================================================================

                    if (ipsDisponibles.isEmpty()) {
                        imprimirLog("No hay vecinos externos. Guardando localmente en disco...");
                        for (Fragmento frag : fragmentos) {
                            enviarFragmentoAVecino("127.0.0.1:" + miPuertoDinamico, frag);
                        }
                    } else {
                        imprimirLog("Distribuyendo entre " + ipsDisponibles.size() + " máquinas físicas...");

                        for (int i = 0; i < fragmentos.size(); i++) {
                            Fragmento frag = fragmentos.get(i);
                            
                            // Elegimos a qué IP física le toca este fragmento
                            String ipElegida = ipsDisponibles.get(i % ipsDisponibles.size());
                            
                            // Obtenemos todos los clientes (hermanos) en esa IP
                            List<String> hermanosEnEsaIP = maquinasFisicas.get(ipElegida);
                            boolean fragmentoEntregado = false;

                            // Intentamos entregar el fragmento. Si uno es fantasma, intentamos con el siguiente.
                            // Intentamos entregar el fragmento.
                            // USAMOS UN ITERATOR para poder borrar de la lista sin que el ciclo explote
                            java.util.Iterator<String> iteradorHermanos = hermanosEnEsaIP.iterator();
                            
                            while (iteradorHermanos.hasNext()) {
                                String nodoDestino = iteradorHermanos.next();
                                
                                if (enviarFragmentoAVecino(nodoDestino, frag)) {
                                    fragmentoEntregado = true;
                                    break; // ¡Se entregó con éxito!
                                } else {
                                    // ¡EL TRUCO DE VELOCIDAD! Si falló, lo quitamos de la lista de hermanos
                                    // Así el siguiente fragmento NO intentará conectarse a este nodo muerto
                                    iteradorHermanos.remove();
                                }
                            }

                            // Si todos los hermanos fallaron (toda la PC se apagó de golpe)
                            if (!fragmentoEntregado) {
                                imprimirLog("Alerta: No se pudo entregar a la IP " + ipElegida + ". Guardando localmente como respaldo.");
                                // Obligatorio usar el puerto dinámico para guardarlo localmente
                                enviarFragmentoAVecino("127.0.0.1:" + miPuertoDinamico, frag);
                            }
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
    }).start(); 
    break;        }
        
        // Limpiamos la caja de texto y la memoria del archivo después de usar un comando
        txtNombreArchivo.setText(""); 
        archivoSeleccionadoLocal = null;
    }
private void iniciarDescarga(String nombreDoc, HashMap<Integer, String> mapaUbicaciones) throws IOException {
    // Aquí guardaremos los pedazos conforme vayan llegando
    List<Fragmento> fragmentosRecolectados = new ArrayList<>();
    
    imprimirLog("Iniciando descarga P2P...");

    // Recorremos el mapa que nos dio el servidor
 
    // Recorremos el mapa y pedimos los pedazos
    for (Map.Entry<Integer, String> entrada : mapaUbicaciones.entrySet()) {
        int numeroFragmento = entrada.getKey();
        String ipVecino = entrada.getValue();
        
        Fragmento frag = pedirFragmentoAVecino(ipVecino, nombreDoc, numeroFragmento);
        
        if (frag != null) {
            fragmentosRecolectados.add(frag);
        } else {
            imprimirLog("Fallo al obtener el fragmento " + numeroFragmento);
            return; 
        }
    } 
    imprimirLog("¡Todos los fragmentos descargados con éxito!");
    
    String rutaDestino = "descarga_" + nombreDoc; 
    
    try {
        // 1. Pegamos los pedazos físicamente en el disco duro
        GestorFragmentos.ensamblarArchivo(fragmentosRecolectados, rutaDestino);
        
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
        
        // 2. Verificamos si la computadora soporta esta función 
        if (!Desktop.isDesktopSupported()) {
            imprimirLog("Tu sistema operativo no soporta la apertura automática de archivos.");
            return;
        }
     
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
// Cambiamos 'ipVecino' por 'destino' para reflejar que trae IP:Puerto
    private Fragmento pedirFragmentoAVecino(String destino, String nombreDoc, int numSecuencia) {
        
        // 1. Separamos la IP del puerto
        String[] partesDestino = destino.split(":");
        String ipReal = partesDestino[0];
        int puertoReal = partesDestino.length > 1 ? Integer.parseInt(partesDestino[1]) : 1236;

        try (Socket socket = new Socket(ipReal, puertoReal);
             ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
            
            salidaObj.writeUTF("GET:" + nombreDoc + ":" + numSecuencia);
            salidaObj.flush();
            
            Fragmento fragmentoRecibido = (Fragmento) entradaObj.readObject();
            return fragmentoRecibido;
            
        } catch (Exception e) {
            System.err.println("Fallo al pedir fragmento a " + destino + ": " + e.getMessage());
            return null;
        }
    }
// Le cambiamos el nombre a la variable de "ipVecino" a "destino" para que sea más claro
   public static boolean enviarFragmentoAVecino(String destino, Fragmento fragmento) {
        String[] partesDestino = destino.split(":");
        String ipReal = partesDestino[0];
        int puertoReal = partesDestino.length > 1 ? Integer.parseInt(partesDestino[1]) : 1236;

        // 1. Creamos el socket vacío (sin conectarlo de golpe)
        try (Socket socket = new Socket()) {
            
            // 2. Intentamos conectar con un TIMEOUT ESTRICTO de 1500 milisegundos (1.5 segundos)
            socket.connect(new java.net.InetSocketAddress(ipReal, puertoReal), 1500);
            
            try (ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
                
                salidaObj.writeUTF("STORE");
                salidaObj.writeObject(fragmento);
                salidaObj.flush();
                return true; // Éxito
            }
            
        } catch (IOException e) {
            System.err.println("[P2P] Nodo muerto o muy lento. Limpiando caché: " + destino);
            ReceptorBroadcast.vecinos.remove(destino); 
            return false; // Falló
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