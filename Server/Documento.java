package Proyecto2.Server;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.io.Serializable;
import static Proyecto2.Server.Main.connections;

public class Documento implements Serializable {
    private final String id;
    private int aperturas;
    public Semaphore eliminar;
    
    // NUEVO: Mapa de ubicaciones [Número de Fragmento] -> [IP del Cliente]
    private final HashMap<Integer, String> ubicacionesFragmentos;
    // NUEVO: Total de fragmentos esperados para este archivo
    private int totalFragmentos;

    public Documento(String id) {
        this.id = id;
        this.eliminar = new Semaphore(connections);
        this.ubicacionesFragmentos = new HashMap<>();
        this.totalFragmentos = 0;
    }

    public byte[] getID() { return id.getBytes(); }
    // --- NUEVOS MÉTODOS ---
    public synchronized void setTotalFragmentos(int total) {
        this.totalFragmentos = total;
    }

    public synchronized int getTotalFragmentos() {
        return totalFragmentos;
    }

    public synchronized void registrarUbicacion(int numeroFragmento, String ipCliente) {
        ubicacionesFragmentos.put(numeroFragmento, ipCliente);
    }

    public synchronized HashMap<Integer, String> getUbicaciones() {
        return ubicacionesFragmentos;
    }
}