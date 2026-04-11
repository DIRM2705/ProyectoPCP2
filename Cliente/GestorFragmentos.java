package Proyecto2.Cliente;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GestorFragmentos {

    // Tamaño de cada bloque (ej. 64 KB). Puedes ajustarlo según las necesidades de la red.
    private static final int TAMANO_MAX_FRAGMENTO = 64 * 1024; 

    /**
     * Toma un archivo físico y lo convierte en una lista de fragmentos listos para enviarse por red.
     */
    public static List<Fragmento> dividirArchivo(File archivoFisico, String idDocumento) throws IOException {
        List<Fragmento> listaFragmentos = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(archivoFisico);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            byte[] buffer = new byte[TAMANO_MAX_FRAGMENTO];
            int bytesLeidos;
            int contadorSecuencia = 0;

            // bis.read() intentará llenar el buffer. Retorna la cantidad de bytes que realmente leyó.
            while ((bytesLeidos = bis.read(buffer)) > 0) {
                // Como el último bloque casi nunca llenará los 64KB exactos, 
                // creamos un arreglo del tamaño exacto de los bytes leídos.
                byte[] datosReales = new byte[bytesLeidos];
                System.arraycopy(buffer, 0, datosReales, 0, bytesLeidos);

                // Verificamos si ya no hay más bytes disponibles para marcar el último fragmento
                boolean esUltimo = (bis.available() == 0);

                Fragmento fragmento = new Fragmento(idDocumento, contadorSecuencia, esUltimo, datosReales);
                listaFragmentos.add(fragmento);
                
                contadorSecuencia++;
            }
        }
        return listaFragmentos;
    }

    /**
     * Toma una lista de fragmentos (posiblemente desordenados si llegaron por red UDP/TCP),
     * los ordena y reconstruye el archivo binario original en el disco.
     */
    public static void reconstruirArchivo(List<Fragmento> fragmentosRecibidos, File archivoDestino) throws IOException {
        if (fragmentosRecibidos == null || fragmentosRecibidos.isEmpty()) {
            throw new IllegalArgumentException("No hay fragmentos para reconstruir.");
        }

        // 1. Ordenar los fragmentos basándonos en el número de secuencia
        fragmentosRecibidos.sort(Comparator.comparingInt(Fragmento::getNumeroSecuencia));

        // 2. Escribir los bytes en orden al nuevo archivo
        try (FileOutputStream fos = new FileOutputStream(archivoDestino);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            for (Fragmento frag : fragmentosRecibidos) {
                bos.write(frag.getDatos());
            }
            bos.flush(); // Asegurar que todo se escriba en disco
        }
    }
}