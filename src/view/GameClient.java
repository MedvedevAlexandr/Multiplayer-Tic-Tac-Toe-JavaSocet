package view;

import javax.swing.*;
import java.awt.*;
import controller.ClientController;

public class GameClient extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private ClientController controller;
    
    public GameClient() {
        setTitle("Крестики-нолики 10x10");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(null);
        
        controller = new ClientController(this);
        
        // Настройка CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Создаем панели
        lobbyPanel = new LobbyPanel(controller);
        gamePanel = new GamePanel(controller);
        
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        
        add(mainPanel);
        
        // Показываем диалог подключения
        showConnectionDialog();
    }
    
    private void showConnectionDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        
        JTextField addressField = new JTextField("localhost");
        JTextField portField = new JTextField("5555");
        JTextField nickField = new JTextField();
        
        panel.add(new JLabel("Адрес сервера:"));
        panel.add(addressField);
        panel.add(new JLabel("Порт:"));
        panel.add(portField);
        panel.add(new JLabel("Ваш ник:"));
        panel.add(nickField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Подключение к серверу", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String address = addressField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String nick = nickField.getText().trim();
            
            if (nick.isEmpty()) {
                nick = "Игрок" + (int)(Math.random() * 1000);
            }
            
            controller.connectToServer(address, port, nick);
        } else {
            System.exit(0);
        }
    }
    
    public void showLobby() {
        cardLayout.show(mainPanel, "LOBBY");
    }
    
    public void showGame() {
        cardLayout.show(mainPanel, "GAME");
    }
    
    public LobbyPanel getLobbyPanel() {
        return lobbyPanel;
    }
    
    public GamePanel getGamePanel() {
        return gamePanel;
    }
    
    public void showMessage(String title, String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
    
    public int showOptionDialog(String title, String message, String[] options) {
        return JOptionPane.showOptionDialog(this, 
            message, title, 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, options, options[0]);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameClient().setVisible(true);
        });
    }
}