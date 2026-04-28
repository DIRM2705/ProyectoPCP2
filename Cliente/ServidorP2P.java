package Proyecto2.Cliente;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;

public class ServidorP2P implements Runnable {
    private final int puertoP2P = 1236;
    
    public static final HashMap<String, Fragmento> misFragmentos = new HashMap<>();
    
    // Nombre de la carpeta donde se guardarán los fragmentos físicamente
    private static final String CARPETA_OCULTA = ".fragments";
    
    // Necesitamos la conexión al tracker para reportar cuando recibimos algo
    private ConexionServidor servidorTracker;

    public ServidorP2P(ConexionServidor servidorTracker) {
        this.servidorTracker = servidorTracker;
    }

    @Override
    public void run() {
        try (ServerSocket serverP2P = new ServerSocket(puertoP2P)) {
            System.out.println("Servidor P2P local escuchando en el puerto " + puertoP2P + "...");
            while (true) {
                Socket clientePedidor = serverP2P.accept();
                new Thread(() -> manejarPeticion(clientePedidor)).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void manejarPeticion(Socket socket) {
        // Usamos ObjectStreams para poder enviar/recibir el objeto Fragmento completo
        try (ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
            
            // Leemos la acción que el otro nodo quiere hacer
            String accion = entradaObj.readUTF(); 
            
            if (accion.startsWith("GET:")) {
                // Lógica de DESCARGA (Ejemplo: "GET:mi_archivo.zip:2")
                String[] partes = accion.split(":");
                String nombreDoc = partes[1];
                int numSecuencia = Integer.parseInt(partes[2]);
                String clave = nombreDoc + ":" + numSecuencia;
                
                // 1. Leer desde disco
                Fragmento fragSolicitado = leerDeDisco(nombreDoc, numSecuencia);
                
                if (fragSolicitado != null) {
                    salidaObj.writeObject(fragSolicitado);
                    salidaObj.flush();
                    System.out.println("\n[P2P] Fragmento enviado al vecino desde el disco: " + clave);
                } else {
                    System.err.println("\n[P2P] Error: El vecino pidió " + clave + " pero no está en el disco.");
                }
                
            } else if (accion.equals("STORE")) {
                // Lógica de SUBIDA 
                Fragmento fragNuevo = (Fragmento) entradaObj.readObject();
                String clave = fragNuevo.getIdDocumento() + ":" + fragNuevo.getNumeroSecuencia();
                
                // 1. Guardamos el fragmento FÍSICAMENTE en el disco duro
                guardarEnDisco(fragNuevo);
                
                // 2. Lo guardamos en memoria local
                misFragmentos.put(clave, fragNuevo);
                System.out.println("\n[P2P] Fragmento " + clave + " recibido y guardado en DISCO.");
                
                servidorTracker.reportarFragmento(fragNuevo.getIdDocumento(), fragNuevo.getNumeroSecuencia());
            }
            
        } catch (Exception e) { 
            System.err.println("Error en comunicación P2P: " + e.getMessage());
        }
    }

    // =========================================================================
    // MÉTODOS PARA GESTIONAR ARCHIVOS FÍSICOS EN DISCO
    // =========================================================================

    private void guardarEnDisco(Fragmento fragmento) throws IOException {
        File directorio = new File(CARPETA_OCULTA);

        // Crear la carpeta oculta si no existe
        if (!directorio.exists()) {
            directorio.mkdirs();
            try {
                // Ocultar en Windows
                Files.setAttribute(directorio.toPath(), "dos:hidden", true);
            } catch (Exception e) {
                // Si es Linux/Mac fallará silenciosamente, pero el "." inicial ya la ocultó
            }
        }

        // Nomenclatura del archivo
        String nombreArchivo = fragmento.getIdDocumento() + "_parte_" + fragmento.getNumeroSecuencia() + ".frag";
        File archivoDestino = new File(directorio, nombreArchivo);

        // Escritura física
        try (FileOutputStream fos = new FileOutputStream(archivoDestino);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(fragmento);
        }
    }

    private Fragmento leerDeDisco(String idDocumento, int numeroSecuencia) {
        String nombreArchivo = idDocumento + "_parte_" + numeroSecuencia + ".frag";
        File archivo = new File(CARPETA_OCULTA, nombreArchivo);

        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                return (Fragmento) ois.readObject();
            } catch (Exception e) {
                System.err.println("Error al leer el fragmento físico: " + e.getMessage());
            }
        }
        return null;
    }
}
