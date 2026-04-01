package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

import static Proyecto2.Server.Main.broadcastMessaging;
import static Proyecto2.Server.Main.tablaDocs;

public class Handler implements Runnable {
    private final Socket client;
    private final BufferedReader reader;
    private final PrintWriter writter;
    private final int origen; //El número de cliente que envió el mensaje, se asigna a partir de la dirección IP del cliente

    public Handler(Socket client) throws IOException {
        this.client = client;
        reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        writter = new PrintWriter(client.getOutputStream(), true);
        origen = 0;
    }

    public void announce() {
        try {
            broadcastMessaging.send(Codes.NEW_CLIENT, client.getInetAddress().getHostAddress().getBytes());
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Error al anunciar nuevo cliente: " + e.getMessage());
            try {
                client.close();
            } catch (IOException ex) {
                System.out.println("Error al cerrar la conexión con el cliente: " + ex.getMessage());
            }
        }
    }

    public void run() {
        while (true) {
            String message = receive();
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
            System.out.println("Error al recibir mensaje del cliente: " + e.getMessage());
            return "";
        }
    }

    private void processMessage(int code, String doc) throws IllegalArgumentException, IOException {
        switch (code) {
            case Codes.NEW_DOC:
                System.out.println("Mensaje recibido para crear un nuevo documento: " + doc);
                tablaDocs.insertarDoc(doc, origen, new java.util.ArrayList<>());
                break;
            case Codes.OPEN_DOC:
                System.out.println("Mensaje recibido para abrir un documento: " + doc);
                tablaDocs.get(doc);
                break;
            case Codes.CLOSE_DOC:
                System.out.println("Mensaje recibido para cerrar un documento: " + doc);
                break;
            case Codes.DELETE_DOC:
                System.out.println("Mensaje recibido para eliminar un documento: " + doc);
                tablaDocs.eliminarDoc(doc);
                break;
            default:
                System.out.println("Código de mensaje desconocido: " + code);
                break;
        }
    }
}
