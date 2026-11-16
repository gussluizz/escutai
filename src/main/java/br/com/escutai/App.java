
package br.com.escutai;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            System.out.println("ESCUTAÍ iniciado!");
        });

    }
}
