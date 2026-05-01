package controller;

import view.GameClient;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class ClientController {
    private GameClient view;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nick;
    private char playerSymbol;
    private boolean myTurn = false;
    private boolean gameActive = false;
    private int currentRoomId = -1;
    private boolean awaitingNick = true;
    
    public ClientController(GameClient view) {
        this.view = view;
    }
    
    public void connectToServer(String address, int port, String nick) {
        this.nick = nick;
        
        try {
            socket = new Socket(address, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Отправляем ник серверу
            out.println(nick);
            
            // Запускаем поток для чтения сообщений
            new Thread(this::listenToServer).start();
            
        } catch (IOException e) {
            view.showMessage("Ошибка подключения", 
                "Не удалось подключиться к серверу: " + e.getMessage(), 
                JOptionPane.ERROR_MESSAGE);
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
                view.getLobbyPanel().updateStatus("Соединение разорвано");
                view.showMessage("Ошибка", "Соединение с сервером потеряно", 
                    JOptionPane.ERROR_MESSAGE);
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
                    // Первое сообщение - игнорируем, так как ник уже отправлен
                    break;
                    
                case "NICK_TAKEN":
                    view.showMessage("Ник занят", 
                        "Ник '" + param.split("'")[1] + "' уже занят. Введите другой ник.", 
                        JOptionPane.WARNING_MESSAGE);
                    // Показываем диалог для ввода нового ника
                    String newNick = JOptionPane.showInputDialog(view, 
                        "Ник '" + param.split("'")[1] + "' уже занят. Введите другой ник:",
                        "Ник занят",
                        JOptionPane.WARNING_MESSAGE);
                    if (newNick != null && !newNick.trim().isEmpty()) {
                        out.println(newNick.trim());
                    }
                    break;
                    
                case "NICK_EMPTY":
                    String emptyNick = JOptionPane.showInputDialog(view, 
                        "Ник не может быть пустым. Введите ваш ник:",
                        "Пустой ник",
                        JOptionPane.WARNING_MESSAGE);
                    if (emptyNick != null && !emptyNick.trim().isEmpty()) {
                        out.println(emptyNick.trim());
                    }
                    break;
                    
                case "AUTH_SUCCESS":
                    nick = param;
                    view.getLobbyPanel().updateStatus("Добро пожаловать, " + nick + "!");
                    break;
                    
                case "ROOM_LIST":
                    view.getLobbyPanel().updateRoomList(param);
                    break;
                    
                case "ROOM_CREATED":
                    String[] createdParams = param.split(" ");
                    currentRoomId = Integer.parseInt(createdParams[0]);
                    playerSymbol = createdParams[1].charAt(0);
                    view.getGamePanel().setRoomInfo("Комната #" + currentRoomId + " | Вы: " + playerSymbol);
                    view.showGame();
                    break;
                    
                case "ROOM_JOINED":
                    String[] joinParams = param.split(" ");
                    currentRoomId = Integer.parseInt(joinParams[0]);
                    playerSymbol = joinParams[1].charAt(0);
                    view.getGamePanel().setRoomInfo("Комната #" + currentRoomId + " | Вы: " + playerSymbol);
                    view.showGame();
                    break;
                    
                case "WAITING_FOR_OPPONENT":
                    view.getGamePanel().setGameStatus("Ожидание противника...");
                    gameActive = false;
                    myTurn = false;
                    break;
                    
                case "OPPONENT_JOINED":
                    gameActive = true;
                    view.getGamePanel().setGameStatus("Игра началась! Противник: " + param);
                    // Если текущий игрок - X, то его ход
                    if (playerSymbol == 'X') {
                        myTurn = true;
                        view.getGamePanel().enableValidMoves();
                    }
                    break;
                    
                case "GAME_START":
                    playerSymbol = param.charAt(0);
                    gameActive = true;
                    view.getGamePanel().setGameStatus("Игра началась! Вы: " + playerSymbol);
                    // Если текущий игрок - X, то его ход
                    if (playerSymbol == 'X') {
                        myTurn = true;
                        view.getGamePanel().enableValidMoves();
                    }
                    break;
                    
                case "YOUR_TURN":
                    myTurn = true;
                    view.getGamePanel().setGameStatus("Ваш ход (" + playerSymbol + ")");
                    view.getGamePanel().enableValidMoves();
                    break;
                    
                case "OPPONENT_TURN":
                    myTurn = false;
                    view.getGamePanel().setGameStatus("Ход противника...");
                    view.getGamePanel().disableAllButtons();
                    break;
                    
                case "BOARD":
                    view.getGamePanel().updateBoard(param);
                    break;
                    
                case "WINNER":
                    char winner = param.charAt(0);
                    gameActive = false;
                    view.getGamePanel().disableAllButtons();
                    
                    if (winner == playerSymbol) {
                        view.getGamePanel().setGameStatus("Вы победили!");
                        view.showMessage("Победа!", "Поздравляем! Вы победили!", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        view.getGamePanel().setGameStatus("Вы проиграли!");
                        view.showMessage("Поражение", "Вы проиграли. Попробуйте еще раз!", 
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                    showGameOverDialog();
                    break;
                    
                case "DRAW":
                    gameActive = false;
                    view.getGamePanel().setGameStatus("Ничья!");
                    view.getGamePanel().disableAllButtons();
                    view.showMessage("Ничья", "Игра закончилась вничью!", 
                        JOptionPane.INFORMATION_MESSAGE);
                    showGameOverDialog();
                    break;
                    
                case "OPPONENT_LEFT":
                    gameActive = false;
                    view.getGamePanel().setGameStatus("Противник вышел");
                    view.getGamePanel().disableAllButtons();
                    view.showMessage("Игрок вышел", "Противник покинул игру", 
                        JOptionPane.WARNING_MESSAGE);
                    leaveRoom();
                    break;
                    
                case "RETURN_TO_LOBBY":
                    returnToLobby();
                    break;
                    
                case "ERROR":
                    view.showMessage("Ошибка", param, JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });
    }
    
    public void createRoom(String roomName) {
        out.println("CREATE_ROOM " + roomName);
    }
    
    public void joinRoom(int roomId) {
        out.println("JOIN_ROOM " + roomId);
    }
    
    public void leaveRoom() {
        out.println("LEAVE_ROOM");
    }
    
    public void refreshRoomList() {
        out.println("GET_ROOMS");
    }
    
    public void makeMove(int row, int col) {
        if (myTurn && gameActive) {
            out.println("MOVE " + row + " " + col);
            myTurn = false;
            view.getGamePanel().disableAllButtons();
        }
    }
    
    public void disconnect() {
        out.println("QUIT");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    private void returnToLobby() {
        view.showLobby();
        gameActive = false;
        myTurn = false;
        currentRoomId = -1;
        refreshRoomList();
        view.getLobbyPanel().updateStatus("Вернулись в лобби");
    }
    
    private void showGameOverDialog() {
        String[] options = {"Вернуться в лобби", "Выйти из игры"};
        int choice = view.showOptionDialog("Игра завершена", 
            "Что вы хотите сделать дальше?", options);
        
        if (choice == 0) {
            leaveRoom();
        } else {
            disconnect();
        }
    }
}