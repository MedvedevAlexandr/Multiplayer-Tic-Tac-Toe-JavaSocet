import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class GameClient extends JFrame {
    private String serverAddress = "localhost";
    private int serverPort = 5555;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private String nick;
    private char playerSymbol;
    private boolean inLobby = true;
    private boolean myTurn = false;
    private boolean gameActive = false;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    // Лобби компоненты
    private JPanel lobbyPanel;
    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private JTextField roomNameField;
    private JLabel statusLabel;
    
    // Игровые компоненты
    private JPanel gamePanel;
    private JButton[][] buttons;
    private JLabel gameStatusLabel;
    private JLabel roomInfoLabel;
    private JButton leaveGameButton;
    private static final int BOARD_SIZE = 10;
    private static final int CELL_SIZE = 50;
    private char[][] board;
    
    public GameClient() {
        setTitle("Крестики-нолики 10x10 - Мультиплеер");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 800);
        setLocationRelativeTo(null);
        
        // Настройка CardLayout для переключения между лобби и игрой
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createLobbyPanel();
        createGamePanel();
        
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        
        add(mainPanel);
        showConnectionDialog();
    }
    
    private void createLobbyPanel() {
        lobbyPanel = new JPanel(new BorderLayout(10, 10));
        lobbyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Верхняя панель с приветствием
        JPanel topPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Добро пожаловать в игру!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(new Color(30, 100, 200));
        topPanel.add(statusLabel, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Обновить список");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 12));
        refreshButton.addActionListener(e -> refreshRoomList());
        topPanel.add(refreshButton, BorderLayout.EAST);
        
        lobbyPanel.add(topPanel, BorderLayout.NORTH);
        
        // Панель с информацией
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Информация"));
        
        JLabel info1 = new JLabel("• Создайте свою комнату или присоединитесь к существующей");
        JLabel info2 = new JLabel("• Для победы нужно собрать 5 символов в ряд");
        JLabel info3 = new JLabel("• Игрок X ходит первым");
        
        infoPanel.add(info1);
        infoPanel.add(info2);
        infoPanel.add(info3);
        
        lobbyPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Центральная панель - список комнат
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Доступные комнаты (выберите для присоединения)"));
        
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Добавляем рендерер для красивого отображения
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().contains("[Ожидание]")) {
                    c.setBackground(new Color(200, 255, 200));
                } else if (value.toString().contains("[В игре]")) {
                    c.setBackground(new Color(255, 200, 200));
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setPreferredSize(new Dimension(650, 300));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        lobbyPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Панель создания комнаты
        JPanel createRoomPanel = new JPanel(new GridBagLayout());
        createRoomPanel.setBorder(BorderFactory.createTitledBorder("Создание новой комнаты"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel createLabel = new JLabel("Название комнаты:");
        createLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        createRoomPanel.add(createLabel, gbc);
        
        roomNameField = new JTextField(20);
        roomNameField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        createRoomPanel.add(roomNameField, gbc);
        
        JButton createButton = new JButton("Создать комнату и начать ждать противника");
        createButton.setFont(new Font("Arial", Font.BOLD, 14));
        createButton.setBackground(new Color(70, 130, 180));
        createButton.setForeground(Color.WHITE);
        createButton.setFocusPainted(false);
        createButton.addActionListener(e -> createRoom());
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        createRoomPanel.add(createButton, gbc);
        
        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        JButton joinButton = new JButton("Присоединиться к выбранной комнате");
        joinButton.setFont(new Font("Arial", Font.BOLD, 12));
        joinButton.addActionListener(e -> joinRoom());
        
        JButton disconnectButton = new JButton("Выйти из игры");
        disconnectButton.setFont(new Font("Arial", Font.BOLD, 12));
        disconnectButton.setBackground(new Color(220, 20, 60));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.addActionListener(e -> disconnect());
        
        controlPanel.add(joinButton);
        controlPanel.add(disconnectButton);
        
        // Объединяем все в South панель
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(createRoomPanel);
        southPanel.add(controlPanel);
        
        lobbyPanel.add(southPanel, BorderLayout.SOUTH);
    }
    
    private void createGamePanel() {
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Верхняя панель с информацией о комнате
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel roomInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roomInfoLabel = new JLabel();
        roomInfoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        roomInfoLabel.setForeground(new Color(30, 100, 200));
        roomInfoPanel.add(roomInfoLabel);
        
        gameStatusLabel = new JLabel("Ожидание противника...", SwingConstants.CENTER);
        gameStatusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gameStatusLabel.setForeground(Color.RED);
        
        leaveGameButton = new JButton("Вернуться в лобби");
        leaveGameButton.setFont(new Font("Arial", Font.BOLD, 12));
        leaveGameButton.setBackground(new Color(220, 20, 60));
        leaveGameButton.setForeground(Color.WHITE);
        leaveGameButton.addActionListener(e -> leaveGame());
        
        topPanel.add(roomInfoPanel, BorderLayout.WEST);
        topPanel.add(gameStatusLabel, BorderLayout.CENTER);
        topPanel.add(leaveGameButton, BorderLayout.EAST);
        
        gamePanel.add(topPanel, BorderLayout.NORTH);
        
        // Игровое поле
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE, 2, 2));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        boardPanel.setBackground(Color.BLACK);
        
        buttons = new JButton[BOARD_SIZE][BOARD_SIZE];
        board = new char[BOARD_SIZE][BOARD_SIZE];
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.';
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 20));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].setEnabled(false);
                
                // Чередуем цвет фона для шахматного эффекта
                if ((i + j) % 2 == 0) {
                    buttons[i][j].setBackground(new Color(240, 240, 240));
                }
                
                final int row = i;
                final int col = j;
                buttons[i][j].addActionListener(e -> makeMove(row, col));
                
                boardPanel.add(buttons[i][j]);
            }
        }
        
        gamePanel.add(boardPanel, BorderLayout.CENTER);
        
        // Панель управления игрой
        JPanel gameControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        JButton quitButton = new JButton("Сдаться");
        quitButton.setFont(new Font("Arial", Font.BOLD, 12));
        quitButton.setBackground(new Color(255, 140, 0));
        quitButton.setForeground(Color.WHITE);
        quitButton.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(this,
                "Вы действительно хотите сдаться?",
                "Сдаться",
                JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                leaveGame();
            }
        });
        
        gameControlPanel.add(quitButton);
        
        gamePanel.add(gameControlPanel, BorderLayout.SOUTH);
    }
    
    private void showConnectionDialog() {
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel titleLabel = new JLabel("Подключение к серверу игры");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 100, 200));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        connectionPanel.add(titleLabel, gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        
        JLabel addressLabel = new JLabel("Адрес сервера:");
        addressLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(addressLabel, gbc);
        
        JTextField addressField = new JTextField("localhost", 20);
        addressField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 1;
        connectionPanel.add(addressField, gbc);
        
        JLabel portLabel = new JLabel("Порт:");
        portLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        connectionPanel.add(portLabel, gbc);
        
        JTextField portField = new JTextField("5555", 20);
        portField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 2;
        connectionPanel.add(portField, gbc);
        
        JLabel nickLabel = new JLabel("Ваш ник:");
        nickLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 3;
        connectionPanel.add(nickLabel, gbc);
        
        JTextField nickField = new JTextField(20);
        nickField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 3;
        connectionPanel.add(nickField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JButton connectButton = new JButton("Подключиться");
        connectButton.setFont(new Font("Arial", Font.BOLD, 16));
        connectButton.setBackground(new Color(70, 130, 180));
        connectButton.setForeground(Color.WHITE);
        connectButton.setPreferredSize(new Dimension(150, 40));
        
        JButton exitButton = new JButton("Выйти");
        exitButton.setFont(new Font("Arial", Font.BOLD, 16));
        exitButton.setBackground(new Color(220, 20, 60));
        exitButton.setForeground(Color.WHITE);
        exitButton.setPreferredSize(new Dimension(150, 40));
        
        connectButton.addActionListener(e -> {
            serverAddress = addressField.getText().trim();
            try {
                serverPort = Integer.parseInt(portField.getText().trim());
                nick = nickField.getText().trim();
                
                if (nick.isEmpty()) {
                    nick = "Игрок" + (int)(Math.random() * 1000);
                }
                
                connectToServer();
                ((Window)SwingUtilities.getRoot(connectionPanel)).dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Неверный порт! Используйте число.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        exitButton.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(connectButton);
        buttonPanel.add(exitButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        connectionPanel.add(buttonPanel, gbc);
        
        JDialog dialog = new JDialog(this, "Подключение к серверу", true);
        dialog.setContentPane(connectionPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Отправляем ник
            out.println(nick);
            
            // Запускаем поток для прослушивания сообщений от сервера
            new Thread(this::listenToServer).start();
            
            setTitle("Крестики-нолики 10x10 - " + nick);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Не удалось подключиться к серверу: " + e.getMessage(),
                "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
            showConnectionDialog();
        }
    }
    
    private void listenToServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Соединение с сервером разорвано");
                JOptionPane.showMessageDialog(this, "Соединение с сервером потеряно", 
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String param = parts.length > 1 ? parts[1] : "";
            
            switch (command) {
                case "WELCOME":
                    // Первое сообщение от сервера
                    break;
                    
                case "AUTH_SUCCESS":
                    statusLabel.setText("Добро пожаловать, " + param + "!");
                    break;
                    
                case "ROOM_LIST":
                    updateRoomList(param);
                    break;
                    
                case "ROOM_CREATED":
                    JOptionPane.showMessageDialog(this, 
                        "Комната создана успешно! Ожидайте присоединения противника.", 
                        "Комната создана", JOptionPane.INFORMATION_MESSAGE);
                    break;
                    
                case "ROOM_JOINED":
                    String[] joinParams = param.split(" ");
                    playerSymbol = joinParams[1].charAt(0);
                    roomInfoLabel.setText("Комната #" + joinParams[0] + " | Вы играете за: " + playerSymbol);
                    break;
                    
                case "WAITING_FOR_OPPONENT":
                    enterGameMode();
                    gameStatusLabel.setText("Ожидание противника...");
                    gameStatusLabel.setForeground(Color.ORANGE);
                    break;
                    
                case "OPPONENT_JOINED":
                    gameActive = true;
                    gameStatusLabel.setText("Игра началась! Противник: " + param);
                    gameStatusLabel.setForeground(Color.GREEN);
                    break;
                    
                case "GAME_START":
                    playerSymbol = param.charAt(0);
                    gameActive = true;
                    enterGameMode();
                    gameStatusLabel.setText("Игра началась! Вы играете за " + playerSymbol);
                    gameStatusLabel.setForeground(Color.GREEN);
                    break;
                    
                case "YOUR_TURN":
                    myTurn = true;
                    gameStatusLabel.setText("Ваш ход (" + playerSymbol + ")");
                    gameStatusLabel.setForeground(Color.BLUE);
                    enableValidButtons();
                    break;
                    
                case "OPPONENT_TURN":
                    myTurn = false;
                    gameStatusLabel.setText("Ход противника...");
                    gameStatusLabel.setForeground(Color.RED);
                    disableAllButtons();
                    break;
                    
                case "BOARD":
                    updateBoard(param);
                    break;
                    
                case "WINNER":
                    char winner = param.charAt(0);
                    gameActive = false;
                    disableAllButtons();
                    
                    if (winner == playerSymbol) {
                        gameStatusLabel.setText("ВЫ ПОБЕДИЛИ! 🎉");
                        gameStatusLabel.setForeground(Color.GREEN);
                        JOptionPane.showMessageDialog(this, 
                            "Поздравляем! Вы одержали победу!", 
                            "Победа!", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        gameStatusLabel.setText("Вы проиграли 😔");
                        gameStatusLabel.setForeground(Color.RED);
                        JOptionPane.showMessageDialog(this, 
                            "Вы проиграли. Попробуйте еще раз!", 
                            "Поражение", JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                    showRestartDialog();
                    break;
                    
                case "DRAW":
                    gameActive = false;
                    gameStatusLabel.setText("НИЧЬЯ! 🤝");
                    gameStatusLabel.setForeground(Color.ORANGE);
                    disableAllButtons();
                    JOptionPane.showMessageDialog(this, 
                        "Игра закончилась вничью! Отличная игра!", 
                        "Ничья", JOptionPane.INFORMATION_MESSAGE);
                    showRestartDialog();
                    break;
                    
                case "GAME_RESTARTED":
                    playerSymbol = param.charAt(0);
                    resetBoard();
                    gameActive = true;
                    gameStatusLabel.setText("Игра перезапущена. Вы играете за " + playerSymbol);
                    break;
                    
                case "OPPONENT_LEFT":
                    gameActive = false;
                    gameStatusLabel.setText("Противник покинул игру 🚪");
                    gameStatusLabel.setForeground(Color.RED);
                    disableAllButtons();
                    JOptionPane.showMessageDialog(this, 
                        "Противник покинул игру. Возвращаюсь в лобби...", 
                        "Игрок вышел", JOptionPane.WARNING_MESSAGE);
                    break;
                    
                case "RETURN_TO_LOBBY":
                    returnToLobby();
                    break;
                    
                case "ERROR":
                    JOptionPane.showMessageDialog(this, param, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });
    }
    
    private void updateRoomList(String roomListStr) {
        roomListModel.clear();
        
        if (!roomListStr.equals("EMPTY")) {
            String[] rooms = roomListStr.split("\\|");
            for (String room : rooms) {
                String[] roomInfo = room.split(",");
                if (roomInfo.length >= 5) {
                    String status = roomInfo[3].equals("WAITING") ? "Ожидание" : "В игре";
                    String players = roomInfo[4];
                    
                    roomListModel.addElement(String.format(
                        "ID: %-3s | Название: %-20s | Создатель: %-15s | Статус: [%s] | Игроки: %s",
                        roomInfo[0], roomInfo[1], roomInfo[2], status, players
                    ));
                }
            }
        } else {
            roomListModel.addElement("⚠️ Нет доступных комнат. Создайте свою первую игру!");
        }
    }
    
    private void refreshRoomList() {
        out.println("GET_ROOMS");
        statusLabel.setText("Список комнат обновлен");
    }
    
    private void createRoom() {
        String roomName = roomNameField.getText().trim();
        if (!roomName.isEmpty()) {
            out.println("CREATE_ROOM " + roomName);
            roomNameField.setText("");
            statusLabel.setText("Создаю комнату: " + roomName + "...");
        } else {
            JOptionPane.showMessageDialog(this, 
                "Пожалуйста, введите название для комнаты!", 
                "Пустое название", JOptionPane.WARNING_MESSAGE);
            roomNameField.requestFocus();
        }
    }
    
    private void joinRoom() {
        int selectedIndex = roomList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selected = roomListModel.get(selectedIndex);
            // Извлекаем ID комнаты из строки
            String[] parts = selected.split("\\|");
            if (parts.length > 0) {
                String roomId = parts[0].replace("ID:", "").trim();
                out.println("JOIN_ROOM " + roomId);
                statusLabel.setText("Присоединяюсь к комнате " + roomId + "...");
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "Пожалуйста, выберите комнату из списка!", 
                "Комната не выбрана", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void enterGameMode() {
        inLobby = false;
        cardLayout.show(mainPanel, "GAME");
        resetBoard();
    }
    
    private void returnToLobby() {
        inLobby = true;
        cardLayout.show(mainPanel, "LOBBY");
        gameActive = false;
        myTurn = false;
        statusLabel.setText("Добро пожаловать обратно в лобби, " + nick + "!");
        refreshRoomList();
    }
    
    private void leaveGame() {
        out.println("LEAVE_ROOM");
    }
    
    private void disconnect() {
        int response = JOptionPane.showConfirmDialog(this,
            "Вы действительно хотите отключиться от сервера?",
            "Отключение",
            JOptionPane.YES_NO_OPTION);
        
        if (response == JOptionPane.YES_OPTION) {
            out.println("QUIT");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }
    
    private void makeMove(int row, int col) {
        if (myTurn && gameActive && board[row][col] == '.') {
            out.println("MOVE " + row + " " + col);
            myTurn = false;
            disableAllButtons();
        }
    }
    
    private void updateBoard(String boardString) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                char symbol = boardString.charAt(i * BOARD_SIZE + j);
                board[i][j] = symbol;
                
                if (symbol == 'X') {
                    buttons[i][j].setText("X");
                    buttons[i][j].setForeground(Color.RED);
                } else if (symbol == 'O') {
                    buttons[i][j].setText("O");
                    buttons[i][j].setForeground(Color.BLUE);
                } else {
                    buttons[i][j].setText("");
                }
            }
        }
    }
    
    private void resetBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.';
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(false);
                buttons[i][j].setBackground((i + j) % 2 == 0 ? 
                    new Color(240, 240, 240) : Color.WHITE);
            }
        }
    }
    
    private void enableValidButtons() {
        if (!gameActive) return;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j].setEnabled(board[i][j] == '.');
            }
        }
    }
    
    private void disableAllButtons() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }
    
    private void showRestartDialog() {
        Object[] options = {"Вернуться в лобби", "Выйти из игры"};
        int response = JOptionPane.showOptionDialog(this,
            "Игра завершена! Комната будет удалена.",
            "Игра завершена",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (response == 0) {
            leaveGame();
        } else if (response == 1) {
            disconnect();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Устанавливаем Look and Feel для более современного вида
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            GameClient client = new GameClient();
            client.setVisible(true);
        });
    }
}