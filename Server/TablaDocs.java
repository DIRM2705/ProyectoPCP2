package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.IOException;
import java.util.HashMap;

import static Proyecto2.Server.Main.*;

import java.io.Serializable;

public class TablaDocs implements Serializable {
    private HashMap<String, Documento> tabla;

    public TablaDocs(String filePath) {
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.FileInputStream(filePath))) {
            tabla = (HashMap<String, Documento>) ois.readObject();
            System.out.println("TablaDocs cargada correctamente desde " + filePath);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error al cargar tablaDocs desde " + filePath + ": " + e.getMessage());
            tabla = new HashMap<>();
        }
    }

    public TablaDocs() {
        tabla = new HashMap<>();
    }

    public boolean existeDoc(String name) {
        return tabla.containsKey(name);
    }

    public void incrementarSemaforos() {
        /*
        Incrementa el número de permisos disponibles en los semáforos de eliminación de cada documento, esto se hace
        cada vez que un nuevo cliente se conecta al servidor para permitir que el nuevo cliente pueda abrir
        los documentos existentes
         */

        for (Documento doc : tabla.values()) {
            doc.eliminar.release();
        }
    }

    public synchronized void insertarDoc(String titulo) throws IOException {
        Documento doc = new Documento("" + (docCounter++)); //Crea un nuevo documento con un ID único
        tabla.put(titulo, doc); //Agrega el documento a la tabla con su lista de particiones
        //Anuncia a los clientes que se ha creado un nuevo documento
        broadcastMessaging.send(Codes.NEW_DOC, doc.getID());
    }

    public void eliminarDoc(String name) throws IllegalStateException, IOException {
        Documento doc = tabla.get(name); //Devuelve la info del documento
        //Intenta adquirir el semáforo para eliminar el documento,
        //si no se puede adquirir es porque hay clientes con el documento abierto
        if (!doc.eliminar.tryAcquire(connections)) {
            throw new IllegalStateException("El documento no se puede eliminar porque hay clientes con el documento abierto");
        }
        byte[] id = tabla.get(name).getID(); //Obtiene el ID del documento antes de eliminarlo
        tabla.remove(name); //Elimina el documento de la tabla
        guardarTablaDocs();
    }

    // Método para que un cliente reporte que tiene un pedazo
    public synchronized void registrarFragmento(String docName, int numFragmento, String ipCliente) {
        if (existeDoc(docName)) {
            Documento doc = tabla.get(docName);
            doc.registrarUbicacion(numFragmento, ipCliente);
            System.out.println("Registrado: Fragmento " + numFragmento + " del doc '" + docName + "' está en IP " + ipCliente);
            try {
                guardarTablaDocs();
            }
            catch (IOException e) {
                System.out.println("Error al guardar tablaDocs después de registrar fragmento: " + e.getMessage());
            }
        }
    }

    public String obtenerListaDocumentos() {
        if (tabla.isEmpty()) {
            return "VACIO";
        }
        // Junta todos los nombres de los archivos separados por comas
        return String.join(",", tabla.keySet());
    }

    // Método para que el servidor empaquete el "mapa" y se lo envíe al cliente que lo pide
    public String obtenerMapaUbicacionesComoString(String docName) throws IllegalStateException {
        if (!existeDoc(docName)) return "";

        Documento doc = tabla.get(docName);
        if(!doc.eliminar.tryAcquire())
            throw new IllegalStateException("El archivo no puede abrirse porque está siendo eliminado");

        HashMap<Integer, String> ubicaciones = doc.getUbicaciones();

        // Vamos a crear un string fácil de leer para el cliente, formato: "0:192.168.1.5;1:192.168.1.6;2:192.168.1.5"
        StringBuilder mapaStr = new StringBuilder();
        mapaStr.append(doc.getTotalFragmentos()).append("|"); // Ponemos el total al inicio

        for (Integer frag : ubicaciones.keySet()) {
            mapaStr.append(frag).append(":").append(ubicaciones.get(frag)).append(";");
        }

        doc.eliminar.release(); //Libera un permiso

        return mapaStr.toString();
    }

    private void guardarTablaDocs() throws IOException {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream("tablaDocs.dat"))) {
            oos.writeObject(this.tabla);
            System.out.println("TablaDocs guardada correctamente");
        }
    }
}
