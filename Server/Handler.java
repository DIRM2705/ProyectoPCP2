package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

import static Proyecto2.Server.Main.broadcastMessaging;

public class Handler implements Runnable {
    private final TablaDocs tablaDocs;
    private final Socket client;
    private final BufferedReader reader;
    private final int origen; //El número de cliente que envió el mensaje, se asigna a partir de la dirección IP del cliente

    public Handler(Socket client) throws IOException {
        this.client = client;
        reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        tablaDocs = new TablaDocs();
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
            processMessage(code, doc);
        }
    }

    private String receive() {
        try {
            String message = reader.readLine();
            if (message == null) {
                return "";
            }
            else return message;
        } catch (IOException e) {
            System.out.println("Error al recibir mensaje del cliente: " + e.getMessage());
            return "";
        }
    }

    private void processMessage(int code, String doc) {
        switch (code) {
            case Codes.NEW_DOC:
                System.out.println("Mensaje recibido para agregar un nuevo documento: " + doc);
                break;
            case Codes.OPEN_DOC:
                System.out.println("Mensaje recibido para abrir un documento: " + doc);
                break;
            case Codes.CLOSE_DOC:
                System.out.println("Mensaje recibido para cerrar un documento: " + doc);
                break;
            case Codes.DELETE_DOC:
                System.out.println("Mensaje recibido para eliminar un documento: " + doc);
                break;
            default:
                System.out.println("Código de mensaje desconocido: " + code);
                break;
        }
    }
}
