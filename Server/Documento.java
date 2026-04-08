package Proyecto2.Server;

import java.util.concurrent.Semaphore;

import static Proyecto2.Server.Main.connections;

public class Documento
{
    private String id; //Número único del documento, se asigna a partir del contador de documentos en la clase Main
    private int aperturas; //Número de clientes que tienen el documento abierto
    public Semaphore eliminar; //Semáforo para controlar el acceso a la eliminación del documento

    public Documento(String id)
    {
        this.id = id;
        aperturas = 0;
        eliminar = new Semaphore(connections);
    }

    public byte[] getID(){
        return id.getBytes();
    }

    public synchronized void abrir()
    {
        aperturas++;
    }

    public synchronized void cerrar()
    {
        if (aperturas > 0)
            aperturas--;
    }
}
