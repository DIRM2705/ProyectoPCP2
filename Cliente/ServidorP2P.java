package Proyecto2.Cliente;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;

public class ServidorP2P implements Runnable {
    private final int puertoP2P = 1236;
    
    // Mantenemos tu HashMap por si otras partes del programa lo necesitan leer,
    // pero ya no será el medio de almacenamiento principal.
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
                
                // 1. En lugar de buscar en el HashMap, LEEMOS DIRECTAMENTE DEL DISCO
                Fragmento fragSolicitado = leerDeDisco(nombreDoc, numSecuencia);
                
                if (fragSolicitado != null) {
                    salidaObj.writeObject(fragSolicitado);
                    salidaObj.flush();
                    System.out.println("\n[P2P] Fragmento enviado al vecino desde el disco: " + clave);
                } else {
                    System.err.println("\n[P2P] Error: El vecino pidió " + clave + " pero no está en el disco.");
                }
                
            } else if (accion.equals("STORE")) {
                // Lógica de SUBIDA (Un vecino nos está regalando un pedazo)
                Fragmento fragNuevo = (Fragmento) entradaObj.readObject();
                String clave = fragNuevo.getIdDocumento() + ":" + fragNuevo.getNumeroSecuencia();
                
                // 1. Guardamos el fragmento FÍSICAMENTE en el disco duro
                guardarEnDisco(fragNuevo);
                
                // 2. Lo guardamos en memoria local (opcional, para no romper código externo)
                misFragmentos.put(clave, fragNuevo);
                System.out.println("\n[P2P] Fragmento " + clave + " recibido y guardado en DISCO.");
                
                // 3. ¡EL REPORTE AUTOMÁTICO! Le avisamos al servidor central
                servidorTracker.reportarFragmento(fragNuevo.getIdDocumento(), fragNuevo.getNumeroSecuencia());
            }
            
        } catch (Exception e) { 
            System.err.println("Error en comunicación P2P: " + e.getMessage());
        }
    }
    // =========================================================================
    // MÉTODO NUEVO: INVENTARIO AL ARRANCAR
    // =========================================================================
    public void reportarInventarioLocal() {
        File directorio = new File(CARPETA_OCULTA);
        
        // Si no existe la carpeta o está vacía, no hay nada que reportar
        if (!directorio.exists() || !directorio.isDirectory()) {
            System.out.println("[Inventario] No hay fragmentos guardados localmente aún.");
            return;
        }

        File[] archivos = directorio.listFiles();
        if (archivos == null || archivos.length == 0) {
            System.out.println("[Inventario] La carpeta está vacía.");
            return;
        }

        System.out.println("[Inventario] Encontrados " + archivos.length + " archivos locales. Reportando al servidor...");
        
        for (File archivo : archivos) {
            String nombre = archivo.getName();
            
            // Recordamos que los guardamos como: idDocumento_parte_numeroSecuencia.frag
            if (nombre.endsWith(".frag") && nombre.contains("_parte_")) {
                try {
                    // Quitamos la extensión ".frag"
                    String sinExtension = nombre.replace(".frag", "");
                    
                    // Cortamos el texto por la palabra "_parte_"
                    String[] partes = sinExtension.split("_parte_");
                    
                    String idDocumento = partes[0];
                    int numSecuencia = Integer.parseInt(partes[1]);

                    // ¡Reutilizamos tu método para avisarle al servidor!
                    servidorTracker.reportarFragmento(idDocumento, numSecuencia);
                    System.out.println(" -> Reportado: " + idDocumento + " (Parte " + numSecuencia + ")");
                    
                } catch (Exception e) {
                    System.err.println("Archivo con nombre inválido ignorado: " + nombre);
                }
            }
        }
        System.out.println("[Inventario] Reporte de inicio finalizado.");
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