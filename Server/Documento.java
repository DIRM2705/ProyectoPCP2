package Proyecto2.Server;

import java.util.ArrayList;

public class Documento
{
    private ArrayList<Integer> particiones;
    private int origen;
    private String title;
    private int aperturas; //Número de clientes que tienen el documento abierto

    public Documento(String title, int origen, ArrayList<Integer> particiones)
    {
        this.title = title;
        this.origen = origen;
        this.particiones = particiones;
        aperturas = 0;
    }

    public ArrayList<Integer> getParticiones()
    {
        return particiones;
    }

    public void abrir()
    {
        aperturas++;
    }

    public void cerrar()
    {
        if (aperturas > 0)
            aperturas--;
    }

    public boolean estaAbierto()
    {
        return aperturas > 0;
    }
}
