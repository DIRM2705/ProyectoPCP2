package Proyecto2.Cliente;

import Proyecto2.Codes;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class ClienteMain {
 
    public static void main(String[] args) {
        System.out.println("=== Iniciando Sistema de Archivos Distribuidos ===");

        // 1. Iniciar el hilo para escuchar los Broadcasts (UDP)
        Thread hiloBroadcast = new Thread(new ReceptorBroadcast());
        hiloBroadcast.setDaemon(true); // Para que se cierre cuando acabe el programa principal
        hiloBroadcast.start();
        
        // 2. Iniciar la conexión de control con el servidor (TCP)
        try {
            // PRIMERO creas la conexión con el servidor central
            ConexionServidor servidor = new ConexionServidor("192.168.1.69", 1235);
            
            // LUEGO inicias el servidor P2P pasándole esa conexión al constructor
            Thread hiloP2P = new Thread(new ServidorP2P(servidor));
            hiloP2P.setDaemon(true);
            hiloP2P.start();
            
            Scanner scanner = new Scanner(System.in);
            
            System.out.println("Conexión establecida con el servidor principal.");
            System.out.println("Comandos disponibles: crear <nombre>, abrir <nombre>, borrar <nombre>, salir");
            
            // SE ELIMINÓ LA DUPLICACIÓN AQUÍ
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                
                if (input.equalsIgnoreCase("salir")) {
                    break;
                }
                
                String[] partes = input.split(" ", 2);
                if (partes.length < 2) {
                    System.out.println("Formato incorrecto. Ejemplo: crear mi_archivo.bin");
                    continue;
                }
                
                String comando = partes[0].toLowerCase();
                String nombreDoc = partes[1];
                
                // Mapear el comando de texto al código binario de Codes.java
                switch (comando) {
                   case "crear":
                        File archivoLocal = new File(nombreDoc);
                        if (!archivoLocal.exists()) {
                            System.out.println("Error: El archivo '" + nombreDoc + "' no existe en tu carpeta.");
                            break;
                        }
                        
                        // 1. Avisar al Servidor Central para que registre el nombre del documento
                        servidor.enviarComando(Codes.NEW_DOC, nombreDoc);
                        
                        try {
                            // 2. Dividir el archivo en fragmentos (usando el GestorFragmentos)
                            List<Fragmento> fragmentos = GestorFragmentos.dividirArchivo(archivoLocal, nombreDoc);
                            System.out.println("El archivo se dividió en " + fragmentos.size() + " fragmentos.");

                            // 3. Obtener la lista de vecinos disponibles (se llenó gracias a los Broadcasts)
                            List<String> vecinosDisponibles = new java.util.ArrayList<>(ReceptorBroadcast.vecinos);
                            
                            if (vecinosDisponibles.isEmpty()) {
                                System.out.println("No hay vecinos conectados. Guardando el archivo completo localmente...");
                                // Si estás solo, guardas los fragmentos en tu propia memoria y le reportas al servidor
                                for (Fragmento frag : fragmentos) {
                                    ServidorP2P.misFragmentos.put(nombreDoc + ":" + frag.getNumeroSecuencia(), frag);
                                    servidor.reportarFragmento(nombreDoc, frag.getNumeroSecuencia());
                                }
                            } else {
                                System.out.println("Distribuyendo fragmentos entre " + vecinosDisponibles.size() + " vecinos conectados...");
                                
                                // 4. Repartición
                                for (int i = 0; i < fragmentos.size(); i++) {
                                    Fragmento frag = fragmentos.get(i);
                                    
                                    // Calcula a qué vecino le toca
                                    String ipDestino = vecinosDisponibles.get(i % vecinosDisponibles.size());
                                    
                                    enviarFragmentoAVecino(ipDestino, frag);
                                }
                                System.out.println("Distribución completada con éxito");
                            }
                        } catch (IOException e) {
                            System.out.println("Error al procesar el archivo: " + e.getMessage());
                        }
                        break;
                    case "abrir":
                        servidor.enviarComando(Codes.OPEN_DOC, nombreDoc);
                        break;
                    case "borrar":
                        servidor.enviarComando(Codes.DELETE_DOC, nombreDoc);
                        break;
                    default:
                        System.out.println("Comando no reconocido.");
                }
            } // Cierre correcto del while(true)
            
            servidor.cerrar();
            System.out.println("Cliente desconectado.");
            System.exit(0);
            
        } catch (Exception e) { // Cierre correcto del try-catch
            System.err.println("Fallo al conectar con el servidor TCP: " + e.getMessage());
        }
    } // Cierre correcto del main
    
    public static void enviarFragmentoAVecino(String ipVecino, Fragmento fragmento) {
        try (Socket socket = new Socket(ipVecino, 1236);
            ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
            
            // 1. Decimos que queremos guardar
            salidaObj.writeUTF("STORE");
            // 2. Aventamos el objeto completo serializado
            salidaObj.writeObject(fragmento);
            salidaObj.flush();
            
        } catch (IOException e) {
            System.err.println("No se pudo enviar el fragmento a la IP: " + ipVecino);
        }
    }
}