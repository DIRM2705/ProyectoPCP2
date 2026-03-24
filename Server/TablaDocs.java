package Proyecto2.Server;

import java.util.ArrayList;
import java.util.HashMap;

public class TablaDocs
{
    private HashMap<String, Documento> tabla;

    public TablaDocs()
    {
        tabla = new HashMap<>();
    }

    public void insertarDoc(String name, int origen, ArrayList<Integer> particiones) throws IllegalArgumentException
    {
        String titulo = name + "_og" + origen; //Construye el título del documento con su origen
        if (tabla.containsKey(titulo))
            throw new IllegalArgumentException("El documento ya existe en la tabla");

        Documento doc = new Documento(titulo, origen, particiones);
        tabla.put(name, doc); //Agrega el documento a la tabla con su lista de particiones
    }

    public Documento get(String name, int origin) throws IllegalArgumentException
    {
        String doc = name + "_og" + origin; //Construye el título del documento con su origen
        if (!tabla.containsKey(doc))
            throw new IllegalArgumentException("El documento no existe en la tabla");
        return tabla.get(doc); //Devuelve la lista de particiones asociada al documento
    }

    public void eliminarDoc(String name, int origin) throws IllegalArgumentException, IllegalStateException
    {
        String doc = name + "_og" + origin; //Elimina el origen del título para buscar el documento en la tabla
        if (!tabla.containsKey(doc))
            throw new IllegalArgumentException("El documento no existe en la tabla");
        else if (tabla.get(doc).estaAbierto())
            throw new IllegalStateException("El documento no se puede eliminar porque está abierto");
        tabla.remove(doc); //Elimina el documento de la tabla
    }
}
