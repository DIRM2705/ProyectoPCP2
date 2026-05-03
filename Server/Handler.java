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
    private final String ipFisica; // Solo la IP (ej. 192.168.1.69)
    private String identidadUnica; // Será IP:Puerto (ej. 192.168.1.69:51422)

    public Handler(Socket client) throws IOException {
        this.client = client;
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writter = new PrintWriter(client.getOutputStream(), true);
        
        this.ipFisica = client.getInetAddress().getHostAddress();
        // Le damos un valor por defecto temporal por si se desconecta antes de enviar su puerto
        this.identidadUnica = this.ipFisica; 
    }

    // Nota: Eliminamos el método public void announce() porque ahora esa lógica 
    // debe ocurrir obligatoriamente DESPUÉS de recibir el puerto dinámico (en el switch).

    public void run() {
        while (true) {
            String message = receive();
            if(message.equals("FinHilo"))
            {
                // Usamos identidadUnica para borrarlo correctamente con todo y su puerto
                System.out.println("El cliente " + identidadUnica + " se ha desconectado.");
                clientesConectados.remove(identidadUnica);
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
            String doc = message.substring(1); //El resto del mensaje es el payload (documento o puerto)
            
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
                return "FinHilo"; 
            else
                System.out.println("Error al recibir mensaje del cliente: " + e.getMessage());
            return "";
        }
    }

    private void processMessage(int code, String doc) throws IllegalArgumentException, IOException {
        switch (code) {
            // =================================================================
            // NUEVO CASE: Aquí recibimos el puerto y completamos la identidad
            // =================================================================
            case Codes.NEW_CLIENT:
                // El "doc" en este caso trae el puerto (ej. "51422")
                this.identidadUnica = this.ipFisica + ":" + doc.trim();
                System.out.println("Cliente registrado exitosamente con puerto dinámico: " + identidadUnica);

                // 1. Lo guardamos en la memoria central del Servidor (Tracker)
                Main.clientesConectados.add(identidadUnica);
                
                // 2. Recorremos la lista y anunciamos a TODOS usando Broadcast UDP.
                for (String ipRegistrada : Main.clientesConectados) {
                    broadcastMessaging.send(Codes.NEW_CLIENT, ipRegistrada.getBytes());
                }
                System.out.println("Se ha actualizado la red. Clientes totales: " + Main.clientesConectados.size());
                break;

            case Codes.NEW_DOC:
                System.out.println("Mensaje recibido para crear un nuevo documento: " + doc);
                if(tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("Ya existe un documento con ese nombre");
                tablaDocs.insertarDoc(doc);
                break;
                
           case Codes.DELETE_DOC:
                System.out.println("Mensaje recibido para eliminar un documento: " + doc);
                if(!tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("No existe un documento con ese nombre");
                
                tablaDocs.eliminarDoc(doc); // Lo borra del Tracker
                Main.broadcastMessaging.send(Codes.PURGE_FILE, doc.getBytes());
                System.out.println("Orden de purga enviada a la red para el archivo: " + doc);
                break;
                
            case Codes.REPORT_CHUNK:
                String[] partesReporte = doc.split(":");
                if(partesReporte.length == 2) {
                    String nombreDocumento = partesReporte[0];
                    int numFragmento = Integer.parseInt(partesReporte[1]);
                    
                    // ¡VITAL! Aquí cambiamos "origen" por "identidadUnica"
                    tablaDocs.registrarFragmento(nombreDocumento, numFragmento, identidadUnica); 
                }
                break;
                
            case Codes.REQUEST_LOCATIONS:
                System.out.println("Cliente " + identidadUnica + " solicitó ubicaciones del doc: " + doc);
                if(!tablaDocs.existeDoc(doc)) throw new IllegalArgumentException("No existe el documento");
                
                String mapa = tablaDocs.obtenerMapaUbicacionesComoString(doc);
                writter.println(((char)Codes.LOCATIONS_RESPONSE) + mapa);
                break;
                
            case Codes.INVENTORY: // LIST_DOCS
                System.out.println("Cliente " + identidadUnica + " solicitó la lista de documentos.");
                String listaArchivos = tablaDocs.obtenerListaDocumentos();
                writter.println(listaArchivos); 
                break;
                
            default:
                System.out.println("Código de mensaje desconocido: " + code);
                break;
        }
    }
}