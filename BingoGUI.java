import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class BingoGUI implements ActionListener {
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Initial screen components
    private JPanel initialPanel;
    private JTextField nameField;
    private JButton readyButton;
    private JLabel statusLabel; // For general messages on initial screen

    // Game screen components
    private JPanel gamePanel;
    private JButton[][] cardButtons; // 5x5 grid for the bingo card
    private JLabel calledNumberLabel; // To display numbers called by server
    private JLabel bingoStatusLabel;  // For "BINGO!" or winner messages

    private BingoCard bingoCard;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;

    private final int CARD_SIZE = 5;

    public BingoGUI() {
        frame = new JFrame("Bingo Game Client");
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createInitialScreen();
        createGameScreen(); // Prepare game screen but don't show it yet

        mainPanel.add(initialPanel, "INITIAL");
        mainPanel.add(gamePanel, "GAME");

        frame.add(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);
    }

    private void createInitialScreen() {
        initialPanel = new JPanel(new BorderLayout(10, 10));
        initialPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("Welcome to Bingo!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel nameLabel = new JLabel("Enter your name:");
        nameField = new JTextField(15);
        readyButton = new JButton("Ready");
        readyButton.addActionListener(this);
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(readyButton);

        statusLabel = new JLabel("Enter your name and click Ready to join.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        initialPanel.add(titleLabel, BorderLayout.NORTH);
        initialPanel.add(inputPanel, BorderLayout.CENTER);
        initialPanel.add(statusLabel, BorderLayout.SOUTH);
    }

    private void createGameScreen() {
        gamePanel = new JPanel(new BorderLayout(10, 10));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cardDisplayPanel = new JPanel(new GridLayout(CARD_SIZE, CARD_SIZE, 5, 5));
        cardDisplayPanel.setBorder(BorderFactory.createTitledBorder("Your Bingo Card"));
        cardButtons = new JButton[CARD_SIZE][CARD_SIZE];
        Font cardFont = new Font("Arial", Font.BOLD, 20);
        for (int i = 0; i < CARD_SIZE; i++) {
            for (int j = 0; j < CARD_SIZE; j++) {
                cardButtons[i][j] = new JButton(""); // Placeholder text
                cardButtons[i][j].setFont(cardFont);
                cardButtons[i][j].setPreferredSize(new Dimension(60, 60));
                cardButtons[i][j].setEnabled(false); // Card numbers are displayed, not clicked by user
                cardDisplayPanel.add(cardButtons[i][j]);
            }
        }

        calledNumberLabel = new JLabel("Waiting for game to start...", SwingConstants.CENTER);
        calledNumberLabel.setFont(new Font("Arial", Font.BOLD, 22));
        
        bingoStatusLabel = new JLabel(" ", SwingConstants.CENTER); // For BINGO/Winner messages
        bingoStatusLabel.setFont(new Font("Arial", Font.BOLD, 28));
        bingoStatusLabel.setForeground(new Color(0, 128, 0)); // Green for positive messages

        JPanel topPanel = new JPanel(new BorderLayout(5,5));
        topPanel.add(calledNumberLabel, BorderLayout.NORTH);
        topPanel.add(bingoStatusLabel, BorderLayout.SOUTH);

        gamePanel.add(topPanel, BorderLayout.NORTH);
        gamePanel.add(cardDisplayPanel, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == readyButton) {
            playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                statusLabel.setText("Name cannot be empty. Please enter a name.");
                return;
            }
            nameField.setEnabled(false);
            readyButton.setEnabled(false);
            statusLabel.setText("Attempting to connect to server...");
            connectAndPrepareGame();
        }
    }

    private void connectAndPrepareGame() {
        try {
            socket = new Socket("localhost", 12345); // Ensure BingoServer is running
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String serverResponse = in.readLine(); // Expect "WELCOME" or "GAME_IN_PROGRESS"
            if ("WELCOME".equals(serverResponse)) {
                statusLabel.setText("Connected! Sending name: " + playerName);
                out.println(playerName); // Send player name

                serverResponse = in.readLine(); // Expect "Type 'ready' to start..."
                statusLabel.setText("Server: " + serverResponse + " Sending 'ready'...");
                out.println("ready"); // Confirm readiness

                this.bingoCard = new BingoCard(); // Player generates their card now

                cardLayout.show(mainPanel, "GAME");
                frame.setTitle("Bingo Game - " + playerName); // Set window title
                frame.pack();
                frame.setLocationRelativeTo(null);
                
                calledNumberLabel.setText("Waiting for game to start..."); // Initial message on game screen

                new Thread(this::listenToServer).start(); // Start listening for game messages

            } else if ("GAME_IN_PROGRESS".equals(serverResponse)) {
                statusLabel.setText("Game currently in progress. Try again later.");
                nameField.setEnabled(true);
                readyButton.setEnabled(true);
                socket.close();
            } else {
                statusLabel.setText("Unexpected server response: " + serverResponse);
                nameField.setEnabled(true);
                readyButton.setEnabled(true);
                if(socket != null) socket.close();
            }
        } catch (IOException ex) {
            statusLabel.setText("Connection Error: " + ex.getMessage() + ". Ensure server is running.");
            nameField.setEnabled(true);
            readyButton.setEnabled(true);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException cex) { /* ignore */ }
        }
    }
    
    private void initializeCardGUI() {
        if (bingoCard == null) {
             System.err.println("Error: BingoCard is null during GUI initialization.");
             calledNumberLabel.setText("Error: Card not generated.");
             return;
        }
        int[][] numbers = bingoCard.getCardNumbers();
        for (int i = 0; i < CARD_SIZE; i++) {
            for (int j = 0; j < CARD_SIZE; j++) {
                if (i == CARD_SIZE / 2 && j == CARD_SIZE / 2) {
                    cardButtons[i][j].setText("FREE");
                } else {
                    cardButtons[i][j].setText(String.valueOf(numbers[i][j]));
                }
            }
        }
        updateCardGUI(); // Apply initial styling (e.g., free space marked)
    }

    private void updateCardGUI() {
        if (bingoCard == null) return;

        boolean[][] markedStatus = bingoCard.getMarkedStatus();
        for (int i = 0; i < CARD_SIZE; i++) {
            for (int j = 0; j < CARD_SIZE; j++) {
                if (markedStatus[i][j]) {
                    if (i == CARD_SIZE / 2 && j == CARD_SIZE / 2) { // Free space
                        cardButtons[i][j].setBackground(Color.CYAN);
                        cardButtons[i][j].setForeground(Color.BLACK);
                    } else { // Marked number
                        cardButtons[i][j].setBackground(Color.GREEN);
                        cardButtons[i][j].setForeground(Color.BLACK);
                    }
                } else { // Unmarked number
                    cardButtons[i][j].setBackground(UIManager.getColor("Button.background"));
                    cardButtons[i][j].setForeground(UIManager.getColor("Button.foreground"));
                }
            }
        }
    }

    private void listenToServer() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null && !socket.isClosed()) {
                final String finalMessage = serverMessage;
                SwingUtilities.invokeLater(() -> { // All GUI updates must be on the Event Dispatch Thread
                    if (finalMessage.startsWith("GAME_START")) {
                        initializeCardGUI(); // Populate the card display now that game is starting
                        calledNumberLabel.setText("Game Started! Good Luck, " + playerName + "!");
                        bingoStatusLabel.setText(" "); // Clear previous status
                    } else if (finalMessage.startsWith("NUMBER:")) {
                        if (bingoCard == null) { // Should have been created and GUI initialized
                             System.err.println("Received NUMBER: but card not ready.");
                             return;
                        }
                        try {
                            int number = Integer.parseInt(finalMessage.split(":")[1]);
                            calledNumberLabel.setText("Number Called: " + number);
                            bingoCard.markNumber(number);
                            updateCardGUI(); // Visually mark the number

                            if (bingoCard.checkBingo()) {
                                bingoStatusLabel.setText("BINGO!!!");
                                bingoStatusLabel.setForeground(Color.ORANGE);
                                out.println("BINGO"); // Announce BINGO to server
                                // Client will wait for server's WINNER message to confirm.
                            }
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                            System.err.println("Error parsing number from server: " + finalMessage);
                        }
                    } else if (finalMessage.startsWith("WINNER:")) {
                        String winnerName = finalMessage.split(":")[1];
                        calledNumberLabel.setText("Game Over!");
                        if (winnerName.equals(playerName) && bingoCard.checkBingo()) {
                            bingoStatusLabel.setText("You are the WINNER! Congratulations!");
                            bingoStatusLabel.setForeground(Color.MAGENTA);
                        } else {
                            bingoStatusLabel.setText(winnerName + " has won the game!");
                            bingoStatusLabel.setForeground(Color.RED);
                        }
                        // Server will likely close connections. Client can also initiate cleanup.
                        endGameCleanup("Winner declared: " + winnerName);
                        JOptionPane.showMessageDialog(frame, winnerName + " has won the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        // Disable further actions or prepare for a new game if server supports it (current server doesn't without restart)
                        // For now, the game ends here for this client instance.
                        return; // Exit listener thread processing
                    } else {
                        System.out.println("Server message: " + finalMessage); // Log other messages
                    }
                });
            }
        } catch (SocketException se) {
            // This often happens when the server closes the connection (e.g., game ends, client removed)
            // or if the client closes the socket itself in endGameCleanup.
            SwingUtilities.invokeLater(() -> {
                if (!bingoStatusLabel.getText().contains("won the game") && !bingoStatusLabel.getText().contains("WINNER")) {
                     bingoStatusLabel.setText("Disconnected from server.");
                     calledNumberLabel.setText("Game Over.");
                }
                 System.out.println("SocketException in listener: " + se.getMessage());
            });
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                bingoStatusLabel.setText("Communication error with server.");
                calledNumberLabel.setText("Connection Lost.");
                 System.err.println("IOException in server listener: " + e.getMessage());
            });
        } finally {
            endGameCleanup("Listener thread ended.");
        }
    }

    private void endGameCleanup(String reason) {
        System.out.println("Cleaning up game resources: " + reason);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            System.err.println("Error during cleanup: " + ex.getMessage());
        }
        // Optionally, re-enable the initial screen for a new connection attempt
        // SwingUtilities.invokeLater(() -> {
        //     cardLayout.show(mainPanel, "INITIAL");
        //     nameField.setEnabled(true);
        //     readyButton.setEnabled(true);
        //     statusLabel.setText("Disconnected. Enter name and ready to play again.");
        //     frame.setTitle("Bingo Game Client");
        // });
    }

    public static void main(String[] args) {
        // Set a more modern look and feel if available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel.");
        }
        SwingUtilities.invokeLater(BingoGUI::new);
    }
}
