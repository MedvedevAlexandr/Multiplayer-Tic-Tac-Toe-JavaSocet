package model;

public class GameRoom {
    private int roomId;
    private String roomName;
    private String creatorNick;
    private String playerX;
    private String playerO;
    private GameBoard board;
    private String status; // "WAITING", "PLAYING", "FINISHED"
    private char currentPlayer;
    
    public GameRoom(int roomId, String roomName, String creatorNick) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.creatorNick = creatorNick;
        this.board = new GameBoard();
        this.status = "WAITING";
        this.currentPlayer = GameBoard.PLAYER_X;
    }
    
    public synchronized boolean joinRoom(String playerNick, char symbol) {
        if (!status.equals("WAITING")) {
            return false;
        }
        
        if (symbol == GameBoard.PLAYER_X) {
            if (playerX == null) {
                playerX = playerNick;
            } else {
                return false;
            }
        } else if (symbol == GameBoard.PLAYER_O) {
            if (playerO == null) {
                playerO = playerNick;
            } else {
                return false;
            }
        } else {
            return false;
        }
        
        if (playerX != null && playerO != null) {
            status = "PLAYING";
        }
        
        return true;
    }
    
    public synchronized void leaveRoom(String playerNick) {
        if (playerNick.equals(playerX)) {
            playerX = null;
        } else if (playerNick.equals(playerO)) {
            playerO = null;
        }
        
        if (playerX == null && playerO == null) {
            status = "FINISHED";
        } else {
            status = "WAITING";
        }
    }
    
    public synchronized boolean makeMove(int row, int col, char player) {
        if (status.equals("PLAYING") && board.makeMove(row, col, player)) {
            // Меняем текущего игрока
            currentPlayer = (player == GameBoard.PLAYER_X) ? GameBoard.PLAYER_O : GameBoard.PLAYER_X;
            return true;
        }
        return false;
    }
    
    public char getCurrentPlayer() {
        return currentPlayer;
    }
    
    public void setCurrentPlayer(char player) {
        this.currentPlayer = player;
    }
    
    public int getPlayerCount() {
        int count = 0;
        if (playerX != null) count++;
        if (playerO != null) count++;
        return count;
    }
    
    // Геттеры
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getCreatorNick() { return creatorNick; }
    public String getPlayerX() { return playerX; }
    public String getPlayerO() { return playerO; }
    public GameBoard getBoard() { return board; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getInfoString() {
        return String.format("%d,%s,%s,%s,%d/2",
            roomId, roomName, creatorNick, status, getPlayerCount());
    }
}