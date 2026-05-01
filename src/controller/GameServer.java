package controller;

import model.GameRoom;
import model.GameBoard;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int DEFAULT_PORT = 5555;
    private ServerSocket serverSocket;
    private int port;
    private boolean running;
    
    private Map<Integer, GameRoom> gameRooms;
    private Map<String, ClientHandler> connectedClients;
    private int roomCounter = 1;
    private final Object lock = new Object();
    
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
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сервера: " + e.getMessage());
        }
    }
    
    public GameRoom createGameRoom(String roomName, String creatorNick) {
        synchronized (lock) {
            int roomId = roomCounter++;
            GameRoom room = new GameRoom(roomId, roomName, creatorNick);
            gameRooms.put(roomId, room);
            broadcastRoomList();
            return room;
        }
    }
    
    public void removeGameRoom(int roomId) {
        synchronized (lock) {
            gameRooms.remove(roomId);
            broadcastRoomList();
        }
    }
    
    public GameRoom getRoomById(int roomId) {
        synchronized (lock) {
            return gameRooms.get(roomId);
        }
    }
    
    public void addClient(String nick, ClientHandler handler) {
        synchronized (lock) {
            connectedClients.put(nick, handler);
        }
    }
    
    public void removeClient(String nick) {
        synchronized (lock) {
            connectedClients.remove(nick);
        }
    }
    
    public boolean isNickTaken(String nick) {
        synchronized (lock) {
            return connectedClients.containsKey(nick);
        }
    }
    
    public ClientHandler getClientHandler(String nick) {
        synchronized (lock) {
            return connectedClients.get(nick);
        }
    }
    
    public void broadcastRoomList() {
        String roomList = buildRoomListString();
        List<ClientHandler> clientsToNotify = new ArrayList<>();
        
        synchronized (lock) {
            clientsToNotify.addAll(connectedClients.values());
        }
        
        for (ClientHandler client : clientsToNotify) {
            if (!client.isInGame()) {
                client.sendRoomList(roomList);
            }
        }
    }
    
    public String getRoomListString() {
        return buildRoomListString();
    }
    
    private String buildRoomListString() {
        StringBuilder sb = new StringBuilder();
        synchronized (lock) {
            for (GameRoom room : gameRooms.values()) {
                if (room.getStatus().equals("WAITING")) {
                    sb.append(room.getInfoString()).append("|");
                }
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