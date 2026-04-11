package Proyecto2.Cliente;

import java.io.*;
import java.net.Socket;
import Proyecto2.Codes;

public class ConexionServidor {
    private Socket socket;
    private DataOutputStream salida;
    private BufferedReader entrada;

    public ConexionServidor(String ip, int puerto) throws IOException {
        this.socket = new Socket(ip, puerto);
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public synchronized void enviarComando(int codigo, String nombreDoc) throws IOException {
        // Es vital agregar el salto de línea para que reader.readLine() en el servidor funcione
        String payload = nombreDoc + "\n";
        byte[] bytesString = payload.getBytes();
        
        // El arreglo final tiene 1 byte extra para el código de operación
        byte[] mensaje = new byte[1 + bytesString.length];
        mensaje[0] = (byte) codigo;
        
        // Copiamos el texto después del código
        System.arraycopy(bytesString, 0, mensaje, 1, bytesString.length);
        
        // Enviamos el mensaje en bruto
        salida.write(mensaje);
        salida.flush();
    }

    public String recibirRespuesta() throws IOException {
        // En caso de que el servidor envíe errores u otras respuestas directas
        return entrada.readLine();
    }

    // Avisa al servidor que este cliente tiene un pedazo específico
    public void reportarFragmento(String nombreDoc, int numFrag) throws IOException {
        // Formato que espera el servidor: "nombreDoc:num"
        enviarComando(7, nombreDoc + ":" + numFrag); 
    }

    // Pide al servidor la lista de quién tiene qué
    public void pedirMapaUbicaciones(String nombreDoc) throws IOException {
        enviarComando(8, nombreDoc);
    }

    public void cerrar() throws IOException {
        socket.close();
    }
}