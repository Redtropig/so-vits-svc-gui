import gui.GUI;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            GUI gui = new GUI();
        });
    }
}