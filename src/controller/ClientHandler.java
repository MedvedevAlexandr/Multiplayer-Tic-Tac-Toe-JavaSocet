package controller;

import model.GameRoom;
import model.GameBoard;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String nick;
    private GameRoom currentRoom;
    private char playerSymbol;
    private boolean inGame = false;
    
    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Аутентификация с проверкой уникальности ника
            out.println("WELCOME Введите ваш ник:");
            String input = in.readLine();
            
            while (input != null) {
                input = input.trim();
                if (input.isEmpty()) {
                    out.println("NICK_EMPTY Введите непустой ник:");
                    input = in.readLine();
                    continue;
                }
                
                if (server.isNickTaken(input)) {
                    out.println("NICK_TAKEN Ник '" + input + "' уже занят. Введите другой ник:");
                    input = in.readLine();
                } else {
                    nick = input;
                    server.addClient(nick, this);
                    out.println("AUTH_SUCCESS " + nick);
                    out.println("ROOM_LIST " + server.getRoomListString());
                    break;
                }
            }
            
            if (input == null) {
                disconnect();
                return;
            }
            
            // Обработка команд
            while (true) {
                input = in.readLine();
                if (input == null || input.equals("QUIT")) {
                    break;
                }
                
                processCommand(input);
            }
            
        } catch (IOException e) {
            System.out.println("Ошибка клиента " + nick + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void processCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];
        String param = parts.length > 1 ? parts[1] : "";
        
        switch (cmd) {
            case "CREATE_ROOM":
                if (currentRoom == null) {
                    GameRoom room = server.createGameRoom(param, nick);
                    joinRoom(room, GameBoard.PLAYER_X);
                    out.println("ROOM_CREATED " + room.getRoomId() + " " + GameBoard.PLAYER_X);
                    // Создатель сразу видит игровое поле
                    out.println("WAITING_FOR_OPPONENT");
                }
                break;
                
            case "JOIN_ROOM":
                if (currentRoom == null) {
                    try {
                        int roomId = Integer.parseInt(param);
                        GameRoom room = server.getRoomById(roomId);
                        if (room != null && room.getPlayerCount() < 2 && room.getStatus().equals("WAITING")) {
                            char symbol = (room.getPlayerX() == null) ? GameBoard.PLAYER_X : GameBoard.PLAYER_O;
                            joinRoom(room, symbol);
                            out.println("ROOM_JOINED " + roomId + " " + symbol);
                        } else {
                            out.println("ERROR Комната заполнена, не существует или уже играет");
                        }
                    } catch (NumberFormatException e) {
                        out.println("ERROR Неверный ID комнаты");
                    }
                }
                break;
                
            case "LEAVE_ROOM":
                leaveRoom();
                break;
                
            case "GET_ROOMS":
                out.println("ROOM_LIST " + server.getRoomListString());
                break;
                
            case "MOVE":
                if (currentRoom != null && currentRoom.getStatus().equals("PLAYING")) {
                    String[] moveParts = param.split(" ");
                    if (moveParts.length == 2) {
                        try {
                            int row = Integer.parseInt(moveParts[0]);
                            int col = Integer.parseInt(moveParts[1]);
                            handleMove(row, col);
                        } catch (NumberFormatException e) {
                            out.println("ERROR Неверные координаты");
                        }
                    }
                }
                break;
        }
    }
    
    private void joinRoom(GameRoom room, char symbol) {
        if (room.joinRoom(nick, symbol)) {
            currentRoom = room;
            playerSymbol = symbol;
            inGame = true;
            
            // Уведомляем другого игрока
            String opponentNick = (symbol == GameBoard.PLAYER_X) ? 
                room.getPlayerO() : room.getPlayerX();
            
            ClientHandler opponent = (opponentNick != null) ? 
                server.getClientHandler(opponentNick) : null;
            
            if (opponent != null) {
                // Игра начинается для обоих
                opponent.sendMessage("OPPONENT_JOINED " + nick);
                opponent.sendMessage("GAME_START " + opponent.playerSymbol);
                this.sendMessage("GAME_START " + playerSymbol);
                
                String boardState = room.getBoard().boardToString();
                this.sendMessage("BOARD " + boardState);
                opponent.sendMessage("BOARD " + boardState);
                
                // Определяем, чей ход (X всегда ходит первым)
                if (room.getCurrentPlayer() == GameBoard.PLAYER_X) {
                    ClientHandler playerX = server.getClientHandler(room.getPlayerX());
                    if (playerX != null) {
                        playerX.sendMessage("YOUR_TURN");
                    }
                    ClientHandler playerO = server.getClientHandler(room.getPlayerO());
                    if (playerO != null) {
                        playerO.sendMessage("OPPONENT_TURN");
                    }
                } else {
                    ClientHandler playerO = server.getClientHandler(room.getPlayerO());
                    if (playerO != null) {
                        playerO.sendMessage("YOUR_TURN");
                    }
                    ClientHandler playerX = server.getClientHandler(room.getPlayerX());
                    if (playerX != null) {
                        playerX.sendMessage("OPPONENT_TURN");
                    }
                }
            }
            
            server.broadcastRoomList();
        }
    }
    
    public void leaveRoom() {
        if (currentRoom != null) {
            currentRoom.leaveRoom(nick);
            
            // Уведомляем оппонента
            String opponentNick = (playerSymbol == GameBoard.PLAYER_X) ? 
                currentRoom.getPlayerO() : currentRoom.getPlayerX();
            
            if (opponentNick != null) {
                ClientHandler opponent = server.getClientHandler(opponentNick);
                if (opponent != null) {
                    opponent.sendMessage("OPPONENT_LEFT");
                    opponent.leaveRoom();
                }
            }
            
            server.removeGameRoom(currentRoom.getRoomId());
            currentRoom = null;
            playerSymbol = '\0';
            inGame = false;
            sendMessage("RETURN_TO_LOBBY");
        }
    }
    
    private void handleMove(int row, int col) {
        if (currentRoom.getCurrentPlayer() != playerSymbol) {
            sendMessage("ERROR Не ваш ход");
            return;
        }
        
        if (currentRoom.makeMove(row, col, playerSymbol)) {
            String boardState = currentRoom.getBoard().boardToString();
            
            // Отправляем обновление доски обоим игрокам
            ClientHandler playerX = server.getClientHandler(currentRoom.getPlayerX());
            ClientHandler playerO = server.getClientHandler(currentRoom.getPlayerO());
            
            if (playerX != null) playerX.sendMessage("BOARD " + boardState);
            if (playerO != null) playerO.sendMessage("BOARD " + boardState);
            
            // Проверяем победу
            if (currentRoom.getBoard().checkWinner(row, col, playerSymbol)) {
                if (playerX != null) playerX.sendMessage("WINNER " + playerSymbol);
                if (playerO != null) playerO.sendMessage("WINNER " + playerSymbol);
                currentRoom.setStatus("FINISHED");
                server.removeGameRoom(currentRoom.getRoomId());
            } 
            // Проверяем ничью
            else if (currentRoom.getBoard().isBoardFull()) {
                if (playerX != null) playerX.sendMessage("DRAW");
                if (playerO != null) playerO.sendMessage("DRAW");
                currentRoom.setStatus("FINISHED");
                server.removeGameRoom(currentRoom.getRoomId());
            } 
            // Продолжаем игру
            else {
                char nextPlayer = currentRoom.getCurrentPlayer();
                
                if (nextPlayer == GameBoard.PLAYER_X && playerX != null) {
                    playerX.sendMessage("YOUR_TURN");
                    if (playerO != null) playerO.sendMessage("OPPONENT_TURN");
                } else if (nextPlayer == GameBoard.PLAYER_O && playerO != null) {
                    playerO.sendMessage("YOUR_TURN");
                    if (playerX != null) playerX.sendMessage("OPPONENT_TURN");
                }
            }
        } else {
            sendMessage("ERROR Неверный ход");
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public void sendRoomList(String roomList) {
        sendMessage("ROOM_LIST " + roomList);
    }
    
    private void disconnect() {
        try {
            if (currentRoom != null) {
                leaveRoom();
            }
            server.removeClient(nick);
            
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            
            System.out.println("Клиент " + nick + " отключился");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isInGame() {
        return inGame;
    }
    
    public String getNick() {
        return nick;
    }
    
    public char getPlayerSymbol() {
        return playerSymbol;
    }
}