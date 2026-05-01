package view;

import javax.swing.*;
import java.awt.*;
import controller.ClientController;

public class GamePanel extends JPanel {
    private static final int BOARD_SIZE = model.GameBoard.BOARD_SIZE;
    private static final int CELL_SIZE = 50;
    
    private JButton[][] buttons;
    private JLabel gameStatusLabel;
    private JLabel roomInfoLabel;
    private JButton leaveButton;
    private ClientController controller;
    private char[][] boardState;
    
    public GamePanel(ClientController controller) {
        this.controller = controller;
        this.boardState = new char[BOARD_SIZE][BOARD_SIZE];
        initComponents();
        resetBoard();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Верхняя панель с информацией
        JPanel infoPanel = new JPanel(new BorderLayout());
        
        roomInfoLabel = new JLabel("", SwingConstants.LEFT);
        roomInfoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        gameStatusLabel = new JLabel("Ожидание противника...", SwingConstants.CENTER);
        gameStatusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        leaveButton = new JButton("Покинуть игру");
        leaveButton.addActionListener(e -> controller.leaveRoom());
        
        infoPanel.add(roomInfoLabel, BorderLayout.WEST);
        infoPanel.add(gameStatusLabel, BorderLayout.CENTER);
        infoPanel.add(leaveButton, BorderLayout.EAST);
        
        add(infoPanel, BorderLayout.NORTH);
        
        // Игровое поле
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        buttons = new JButton[BOARD_SIZE][BOARD_SIZE];
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 16));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setEnabled(false);
                
                final int row = i;
                final int col = j;
                buttons[i][j].addActionListener(e -> controller.makeMove(row, col));
                
                boardPanel.add(buttons[i][j]);
            }
        }
        
        add(boardPanel, BorderLayout.CENTER);
    }
    
    public void resetBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                boardState[i][j] = model.GameBoard.EMPTY;
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(false);
                buttons[i][j].setBackground(null);
            }
        }
    }
    
    public void updateBoard(String boardString) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                char symbol = boardString.charAt(i * BOARD_SIZE + j);
                boardState[i][j] = symbol;
                buttons[i][j].setText(symbol == model.GameBoard.EMPTY ? "" : String.valueOf(symbol));
                
                if (symbol == model.GameBoard.PLAYER_X) {
                    buttons[i][j].setForeground(Color.RED);
                } else if (symbol == model.GameBoard.PLAYER_O) {
                    buttons[i][j].setForeground(Color.BLUE);
                }
            }
        }
    }
    
    public void setRoomInfo(String info) {
        roomInfoLabel.setText(info);
    }
    
    public void setGameStatus(String status) {
        gameStatusLabel.setText(status);
    }
    
    public void enableValidMoves() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j].setEnabled(boardState[i][j] == model.GameBoard.EMPTY);
            }
        }
    }
    
    public void disableAllButtons() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }
}