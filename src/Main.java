import controller.GameServer;
import view.GameClient;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("server")) {
            // Запуск сервера
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 5555;
            new GameServer(port).start();
        } else {
            // Запуск клиента
            javax.swing.SwingUtilities.invokeLater(() -> {
                new GameClient().setVisible(true);
            });
        }
    }
}