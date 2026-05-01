import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int DEFAULT_PORT = 5555;
    private static final int BOARD_SIZE = 10;
    public static final char EMPTY = '.';
    public static final char PLAYER_X = 'X';
    public static final char PLAYER_O = 'O';
    
    private ServerSocket serverSocket;
    private int port;
    private volatile boolean running;
    
    // Обычные HashMap с синхронизацией
    private HashMap<Integer, GameRoom> gameRooms;
    private HashMap<String, ClientHandler> connectedClients;
    private int roomCounter = 1;
    
    // Объекты для синхронизации
    private final Object roomsLock = new Object();
    private final Object clientsLock = new Object();
    
    public GameServer(int port) {
        this.port = port;
        this.gameRooms = new HashMap<>();
        this.connectedClients = new HashMap<>();
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Сервер запущен на порту " + port);
            System.out.println("Ожидание подключений...");
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сервера: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Синхронизированные методы для работы с комнатами
    public HashMap<Integer, GameRoom> getGameRooms() {
        synchronized (roomsLock) {
            return new HashMap<>(gameRooms); // Возвращаем копию для безопасности
        }
    }
    
    public GameRoom createGameRoom(String roomName, String creatorNick) {
        synchronized (roomsLock) {
            int roomId = roomCounter++;
            GameRoom room = new GameRoom(roomId, roomName, creatorNick, BOARD_SIZE);
            gameRooms.put(roomId, room);
            broadcastRoomList();
            return room;
        }
    }
    
    public void removeGameRoom(int roomId) {
        synchronized (roomsLock) {
            GameRoom room = gameRooms.remove(roomId);
            if (room != null) {
                room.setStatus("FINISHED");
            }
            broadcastRoomList();
        }
    }
    
    public GameRoom getRoomById(int roomId) {
        synchronized (roomsLock) {
            return gameRooms.get(roomId);
        }
    }
    
    // Синхронизированные методы для работы с клиентами
    public void addClient(String nick, ClientHandler handler) {
        synchronized (clientsLock) {
            connectedClients.put(nick, handler);
        }
    }
    
    public void removeClient(String nick) {
        synchronized (clientsLock) {
            connectedClients.remove(nick);
        }
    }
    
    public ClientHandler getClient(String nick) {
        synchronized (clientsLock) {
            return connectedClients.get(nick);
        }
    }
    
    public void broadcastRoomList() {
        String roomList = buildRoomListString();
        List<ClientHandler> clientsToNotify;
        
        // Копируем список клиентов для безопасной итерации
        synchronized (clientsLock) {
            clientsToNotify = new ArrayList<>(connectedClients.values());
        }
        
        for (ClientHandler client : clientsToNotify) {
            if (client.getCurrentRoom() == null) { // Только клиентам в лобби
                client.sendRoomList(roomList);
            }
        }
    }
    
    public String buildRoomListString() {
        StringBuilder sb = new StringBuilder();
        
        // Создаем копию комнат для безопасной итерации
        HashMap<Integer, GameRoom> roomsCopy;
        synchronized (roomsLock) {
            roomsCopy = new HashMap<>(gameRooms);
        }
        
        for (GameRoom room : roomsCopy.values()) {
            // Показываем только комнаты в статусе WAITING (ожидающие игроков)
            if (room.getStatus().equals("WAITING")) {
                sb.append(room.getRoomId()).append(",")
                  .append(room.getRoomName()).append(",")
                  .append(room.getCreatorNick()).append(",")
                  .append(room.getStatus()).append(",")
                  .append(room.getPlayerCount()).append("/2|");
            }
        }
        return sb.length() > 0 ? sb.toString() : "EMPTY";
    }
    
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        GameServer server = new GameServer(port);
        server.start();
    }
}

class GameRoom {
    private int roomId;
    private String roomName;
    private String creatorNick;
    private ClientHandler playerX;
    private ClientHandler playerO;
    private char[][] board;
    private char currentPlayer;
    private String status; // "WAITING", "PLAYING", "FINISHED"
    private final int boardSize;
    
    public GameRoom(int roomId, String roomName, String creatorNick, int boardSize) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.creatorNick = creatorNick;
        this.boardSize = boardSize;
        this.board = new char[boardSize][boardSize];
        this.status = "WAITING";
        this.currentPlayer = GameServer.PLAYER_X;
        initializeBoard();
    }
    
    private void initializeBoard() {
        for (int i = 0; i < boardSize; i++) {
            Arrays.fill(board[i], GameServer.EMPTY);
        }
    }
    
    public synchronized boolean joinRoom(ClientHandler player, char symbol) {
        if (status.equals("WAITING")) {
            if (symbol == GameServer.PLAYER_X) {
                playerX = player;
            } else {
                playerO = player;
            }
            
            if (playerX != null && playerO != null) {
                status = "PLAYING";
            }
            return true;
        }
        return false;
    }
    
    public synchronized void leaveRoom(ClientHandler player) {
        if (player == playerX) {
            playerX = null;
            if (playerO != null) {
                playerO.sendMessage("OPPONENT_LEFT");
                playerO.leaveRoom();
            }
        } else if (player == playerO) {
            playerO = null;
            if (playerX != null) {
                playerX.sendMessage("OPPONENT_LEFT");
                playerX.leaveRoom();
            }
        }
        
        status = "FINISHED";
    }
    
    public synchronized boolean makeMove(int row, int col, char player) {
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
            return false;
        }
        if (board[row][col] != GameServer.EMPTY) {
            return false;
        }
        if ((player == GameServer.PLAYER_X && currentPlayer != GameServer.PLAYER_X) || 
            (player == GameServer.PLAYER_O && currentPlayer != GameServer.PLAYER_O)) {
            return false;
        }
        
        board[row][col] = player;
        return true;
    }
    
    public synchronized boolean checkWinner(int row, int col, char player) {
        return checkDirection(row, col, player, 1, 0) ||
               checkDirection(row, col, player, 0, 1) ||
               checkDirection(row, col, player, 1, 1) ||
               checkDirection(row, col, player, 1, -1);
    }
    
    private boolean checkDirection(int row, int col, char player, int dRow, int dCol) {
        int count = 1;
        
        for (int i = 1; i < 5; i++) {
            int newRow = row + dRow * i;
            int newCol = col + dCol * i;
            if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize) break;
            if (board[newRow][newCol] == player) count++;
            else break;
        }
        
        for (int i = 1; i < 5; i++) {
            int newRow = row - dRow * i;
            int newCol = col - dCol * i;
            if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize) break;
            if (board[newRow][newCol] == player) count++;
            else break;
        }
        
        return count >= 5;
    }
    
    public synchronized boolean isBoardFull() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == GameServer.EMPTY) return false;
            }
        }
        return true;
    }
    
    public synchronized String getBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                sb.append(board[i][j]);
            }
        }
        return sb.toString();
    }
    
    public synchronized void endGame() {
        status = "FINISHED";
    }
    
    // Геттеры и сеттеры
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getCreatorNick() { return creatorNick; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPlayerCount() { 
        int count = 0;
        if (playerX != null) count++;
        if (playerO != null) count++;
        return count;
    }
    public ClientHandler getPlayerX() { return playerX; }
    public ClientHandler getPlayerO() { return playerO; }
    public char getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(char player) { this.currentPlayer = player; }
}

class ClientHandler extends Thread {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String nick;
    private GameRoom currentRoom;
    private char playerSymbol;
    
    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Аутентификация
            out.println("WELCOME Введите ваш ник:");
            String input = in.readLine();
            if (input == null || input.isEmpty()) {
                disconnect();
                return;
            }
            
            nick = input.trim();
            server.addClient(nick, this);
            out.println("AUTH_SUCCESS " + nick);
            out.println("ROOM_LIST " + server.buildRoomListString());
            
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
                    joinRoom(room, GameServer.PLAYER_X);
                    out.println("ROOM_CREATED " + room.getRoomId());
                }
                break;
                
            case "JOIN_ROOM":
                if (currentRoom == null) {
                    try {
                        int roomId = Integer.parseInt(param);
                        GameRoom room = server.getRoomById(roomId);
                        if (room != null && room.getPlayerCount() < 2 && room.getStatus().equals("WAITING")) {
                            char symbol = (room.getPlayerX() == null) ? GameServer.PLAYER_X : GameServer.PLAYER_O;
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
                out.println("ROOM_LIST " + server.buildRoomListString());
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
                
            case "RESTART":
                // Убрали возможность рестарта, т.к. комната удаляется после игры
                break;
        }
    }
    
    private void joinRoom(GameRoom room, char symbol) {
        if (room.joinRoom(this, symbol)) {
            currentRoom = room;
            playerSymbol = symbol;
            
            // Уведомляем другого игрока, если он есть
            ClientHandler opponent = (symbol == GameServer.PLAYER_X) ? room.getPlayerO() : room.getPlayerX();
            if (opponent != null) {
                opponent.sendMessage("OPPONENT_JOINED " + nick);
                opponent.sendMessage("GAME_START " + opponent.playerSymbol);
                this.sendMessage("GAME_START " + playerSymbol);
                this.sendMessage("BOARD " + room.getBoardString());
                opponent.sendMessage("BOARD " + room.getBoardString());
                
                if (room.getCurrentPlayer() == playerSymbol) {
                    this.sendMessage("YOUR_TURN");
                } else {
                    opponent.sendMessage("YOUR_TURN");
                }
            } else {
                this.sendMessage("WAITING_FOR_OPPONENT");
            }
            
            server.broadcastRoomList();
        }
    }
    
    public void leaveRoom() {
        if (currentRoom != null) {
            currentRoom.leaveRoom(this);
            int roomId = currentRoom.getRoomId();
            currentRoom = null;
            playerSymbol = '\0';
            server.removeGameRoom(roomId);
            sendMessage("RETURN_TO_LOBBY");
        }
    }
    
    private void handleMove(int row, int col) {
        if (currentRoom.getCurrentPlayer() != playerSymbol) {
            sendMessage("ERROR Не ваш ход");
            return;
        }
        
        if (currentRoom.makeMove(row, col, playerSymbol)) {
            String boardState = currentRoom.getBoardString();
            
            // Отправляем обновление доски обоим игрокам
            currentRoom.getPlayerX().sendMessage("BOARD " + boardState);
            if (currentRoom.getPlayerO() != null) {
                currentRoom.getPlayerO().sendMessage("BOARD " + boardState);
            }
            
            // Проверяем победу
            if (currentRoom.checkWinner(row, col, playerSymbol)) {
                currentRoom.getPlayerX().sendMessage("WINNER " + playerSymbol);
                if (currentRoom.getPlayerO() != null) {
                    currentRoom.getPlayerO().sendMessage("WINNER " + playerSymbol);
                }
                currentRoom.endGame();
                server.removeGameRoom(currentRoom.getRoomId());
            } 
            // Проверяем ничью
            else if (currentRoom.isBoardFull()) {
                currentRoom.getPlayerX().sendMessage("DRAW");
                if (currentRoom.getPlayerO() != null) {
                    currentRoom.getPlayerO().sendMessage("DRAW");
                }
                currentRoom.endGame();
                server.removeGameRoom(currentRoom.getRoomId());
            } 
            // Продолжаем игру
            else {
                char nextPlayer = (playerSymbol == GameServer.PLAYER_X) ? GameServer.PLAYER_O : GameServer.PLAYER_X;
                currentRoom.setCurrentPlayer(nextPlayer);
                
                if (nextPlayer == GameServer.PLAYER_X && currentRoom.getPlayerX() != null) {
                    currentRoom.getPlayerX().sendMessage("YOUR_TURN");
                } else if (nextPlayer == GameServer.PLAYER_O && currentRoom.getPlayerO() != null) {
                    currentRoom.getPlayerO().sendMessage("YOUR_TURN");
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
    
    // Геттеры
    public String getNick() { return nick; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public char getPlayerSymbol() { return playerSymbol; }
}