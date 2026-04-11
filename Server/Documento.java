package Proyecto2.Server;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import static Proyecto2.Server.Main.connections;

public class Documento {
    private String id;
    private int aperturas;
    public Semaphore eliminar;
    
    // NUEVO: Mapa de ubicaciones [Número de Fragmento] -> [IP del Cliente]
    private HashMap<Integer, String> ubicacionesFragmentos;
    // NUEVO: Total de fragmentos esperados para este archivo
    private int totalFragmentos;

    public Documento(String id) {
        this.id = id;
        this.aperturas = 0;
        this.eliminar = new Semaphore(connections);
        this.ubicacionesFragmentos = new HashMap<>();
        this.totalFragmentos = 0;
    }

    public byte[] getID() { return id.getBytes(); }
    public synchronized void abrir() { aperturas++; }
    public synchronized void cerrar() { if (aperturas > 0) aperturas--; }

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