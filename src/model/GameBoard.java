package model;

import java.util.Arrays;

public class GameBoard {
    public static final char EMPTY = '.';
    public static final char PLAYER_X = 'X';
    public static final char PLAYER_O = 'O';
    public static final int BOARD_SIZE = 10;
    public static final int WIN_COUNT = 5;
    
    private char[][] board;
    
    public GameBoard() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        reset();
    }
    
    public void reset() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            Arrays.fill(board[i], EMPTY);
        }
    }
    
    public boolean makeMove(int row, int col, char player) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }
        if (board[row][col] != EMPTY) {
            return false;
        }
        
        board[row][col] = player;
        return true;
    }
    
    public boolean checkWinner(int row, int col, char player) {
        // Проверяем во всех направлениях
        int[] dx = {0, 1, 1, 1};
        int[] dy = {1, 0, 1, -1};
        
        for (int dir = 0; dir < 4; dir++) {
            int count = 1;
            
            // Проверка в положительном направлении
            for (int i = 1; i < WIN_COUNT; i++) {
                int newRow = row + dx[dir] * i;
                int newCol = col + dy[dir] * i;
                if (newRow < 0 || newRow >= BOARD_SIZE || newCol < 0 || newCol >= BOARD_SIZE) break;
                if (board[newRow][newCol] == player) count++;
                else break;
            }
            
            // Проверка в отрицательном направлении
            for (int i = 1; i < WIN_COUNT; i++) {
                int newRow = row - dx[dir] * i;
                int newCol = col - dy[dir] * i;
                if (newRow < 0 || newRow >= BOARD_SIZE || newCol < 0 || newCol >= BOARD_SIZE) break;
                if (board[newRow][newCol] == player) count++;
                else break;
            }
            
            if (count >= WIN_COUNT) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) return false;
            }
        }
        return true;
    }
    
    public String boardToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                sb.append(board[i][j]);
            }
        }
        return sb.toString();
    }
    
    public void updateFromString(String boardString) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = boardString.charAt(i * BOARD_SIZE + j);
            }
        }
    }
    
    public char getCell(int row, int col) {
        return board[row][col];
    }
}