package view;

import javax.swing.*;
import java.awt.*;
import controller.ClientController;

public class LobbyPanel extends JPanel {
    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private JTextField roomNameField;
    private JLabel statusLabel;
    private ClientController controller;
    
    public LobbyPanel(ClientController controller) {
        this.controller = controller;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Верхняя панель
        JPanel topPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Подключение к серверу...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(statusLabel, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> controller.refreshRoomList());
        topPanel.add(refreshButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Центральная панель - список комнат
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Доступные комнаты"));
        
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(roomList);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Нижняя панель - управление
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        
        // Панель создания комнаты
        JPanel createPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        createPanel.setBorder(BorderFactory.createTitledBorder("Создать комнату"));
        
        roomNameField = new JTextField();
        JButton createButton = new JButton("Создать");
        createButton.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            if (!roomName.isEmpty()) {
                controller.createRoom(roomName);
                roomNameField.setText("");
            }
        });
        
        createPanel.add(roomNameField);
        createPanel.add(createButton);
        
        bottomPanel.add(createPanel, BorderLayout.NORTH);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        JButton joinButton = new JButton("Присоединиться");
        joinButton.addActionListener(e -> {
            int selectedIndex = roomList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selected = roomListModel.get(selectedIndex);
                String roomId = selected.split("\\|")[0].replace("ID:", "").trim();
                controller.joinRoom(Integer.parseInt(roomId));
            }
        });
        
        JButton disconnectButton = new JButton("Отключиться");
        disconnectButton.addActionListener(e -> controller.disconnect());
        
        buttonPanel.add(joinButton);
        buttonPanel.add(disconnectButton);
        
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    public void updateRoomList(String roomListStr) {
        roomListModel.clear();
        
        if (!roomListStr.equals("EMPTY")) {
            String[] rooms = roomListStr.split("\\|");
            for (String room : rooms) {
                String[] roomInfo = room.split(",");
                if (roomInfo.length >= 5) {
                    String status = roomInfo[3].equals("WAITING") ? "Ожидание" : "В игре";
                    roomListModel.addElement(String.format(
                        "ID: %s | %s (Создатель: %s) [%s] [%s]",
                        roomInfo[0], roomInfo[1], roomInfo[2], status, roomInfo[4]
                    ));
                }
            }
        } else {
            roomListModel.addElement("Нет доступных комнат");
        }
    }
}