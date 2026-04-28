package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

import static Proyecto2.Server.Main.*;

public class Handler implements Runnable {
    private final Socket client;
    private final BufferedReader reader;
    private final PrintWriter writter;
    private final String origen; //El número de cliente que envió el mensaje, se asigna a partir de la dirección IP del cliente

    public Handler(Socket client) throws IOException {
        this.client = client;
        reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        writter = new PrintWriter(client.getOutputStream(), true);
        origen = client.getInetAddress().getHostAddress();
    }

public void announce() {
        try {
            // 1. Lo guardamos en la memoria central del Servidor
            Main.clientesConectados.add(origen);
            
            // 2. Recorremos la lista y anunciamos a TODOS.
            // Así, el nuevo se entera de los viejos, y los viejos se enteran del nuevo.
            for (String ipRegistrada : Main.clientesConectados) {
                broadcastMessaging.send(Codes.NEW_CLIENT, ipRegistrada.getBytes());
            }
            
            System.out.println("Se ha actualizado la red. Clientes totales: " + Main.clientesConectados.size());
            
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Error al anunciar clientes: " + e.getMessage());
            try {
                client.close();
            } catch (IOException ex) {
                System.out.println("Error al cerrar la conexión: " + ex.getMessage());
            }
        }
    }

    public void run() {
        while (true) {
            String message = receive();
            if(message.equals("FinHilo"))
            {
                System.out.println("El cliente " + origen + " se ha desconectado.");
                clientesConectados.remove(origen);
                try {
                    client.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar la conexión: " + e.getMessage());
                }
                break; // Salimos del bucle para terminar el hilo
            }
            if (message.length() < 2) {
                System.out.println("Mensaje recibido con formato incorrecto");
                return;
            }
            int code = message.getBytes()[0]; //El primer byte del mensaje es el código de la operación
            String doc = message.substring(1); //El resto del mensaje es el nombre del documento
            try {
                processMessage(code, doc);
            } catch (Exception e) {
                System.out.println("Ha ocurrido un error al procesar el mensaje: " + e.getMessage());
                writter.println(((byte)Codes.ERROR) + "Ha ocurrido un error: " + e.getMessage());
            }
        }
    }

    private String receive() {
        try {
            String message = reader.readLine();
            return Objects.requireNonNullElse(message, "");
        } catch (IOException e) {
            if(Objects.equals(e.getMessage(), "Connection reset"))
                return "FinHilo"; // Indicamos que el cliente se ha desconectado abruptamente
            else
                System.out.println("Error al recibir mensaje del cliente: " + e.getMessage());
            return "";
        }
    }

    private void processMessage(int code, String doc) throws IllegalArgumentException, IOException {
        switch (code) {
            case Codes.NEW_DOC:
                System.out.println("Mensaje recibido para crear un nuevo documento: " + doc);
                if(tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("Ya existe un documento con ese nombre");
                tablaDocs.insertarDoc(doc);
                break;
            case Codes.DELETE_DOC:
                System.out.println("Mensaje recibido para eliminar un documento: " + doc);
                if(!tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("No existe un documento con ese nombre");
                tablaDocs.eliminarDoc(doc);
                break;
            case Codes.REPORT_CHUNK:
                // El formato que recibiremos del cliente será: "nombreDoc:numFragmento"
                // Ejemplo: "mi_archivo.txt:3"
                String[] partesReporte = doc.split(":");
                if(partesReporte.length == 2) {
                    String nombreDocumento = partesReporte[0];
                    int numFragmento = Integer.parseInt(partesReporte[1]);
                    tablaDocs.registrarFragmento(nombreDocumento, numFragmento, origen); // "origen" es la IP del cliente (ya la tienes en la clase)
                }
                break;
            case Codes.REQUEST_LOCATIONS:
                // El cliente nos pide dónde están los fragmentos de un documento
                System.out.println("Cliente " + origen + " solicitó ubicaciones del doc: " + doc);
                if(!tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("No existe el documento");
                
                String mapa = tablaDocs.obtenerMapaUbicacionesComoString(doc);
                
                // Le respondemos SOLO a este cliente (usamos su writter TCP, no un broadcast UDP)
                writter.println(((char)Codes.LOCATIONS_RESPONSE) + mapa);
                break;
                case Codes.INVENTORY: // LIST_DOCS
                    System.out.println("Cliente " + origen + " solicitó la lista de documentos.");
                    String listaArchivos = tablaDocs.obtenerListaDocumentos();
                    writter.println(listaArchivos); // Le enviamos la lista en texto plano
                 break;
            default:
                System.out.println("Código de mensaje desconocido: " + code);
                break;
        }
    }
}
