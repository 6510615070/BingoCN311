import java.io.*;
import java.net.*;
import java.util.*;

public class BingoServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<ClientHandler> readyClients = Collections.synchronizedSet(new HashSet<>());
    private static Set<Integer> drawnNumbers = new HashSet<>();
    private static Random random = new Random();
    private static volatile boolean gameStarted = false;
    private static volatile boolean stopGame = false;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Bingo Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            if (gameStarted) {
                PrintWriter tempOut = new PrintWriter(clientSocket.getOutputStream(), true);
                tempOut.println("GAME_IN_PROGRESS");
                clientSocket.close();
                continue;
            }

            ClientHandler handler = new ClientHandler(clientSocket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    private static void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        stopGame = false;
        drawnNumbers.clear();

        new Thread(() -> {
            System.out.println("All players ready. Game started!");
            broadcast("GAME_START");

            try {
                while (!stopGame && drawnNumbers.size() < 25) {
                    Thread.sleep(3000);

                    int number;
                    do {
                        number = random.nextInt(25) + 1;
                    } while (drawnNumbers.contains(number));
                    drawnNumbers.add(number);

                    broadcast("NUMBER:" + number);
                    System.out.println("Sent number: " + number);
                }

                if (stopGame) {
                    System.out.println("Game stopped due to a BINGO winner.");
                }

                // Clean up for next round
                cleanupGame();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.send(msg);
            }
        }
    }

    private static void cleanupGame() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            readyClients.clear();
        }

        gameStarted = false;
        stopGame = false;
        drawnNumbers.clear();

        System.out.println("Ready for new game round.");
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;
        private boolean active = true;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void send(String msg) {
            if (active) out.println(msg);
        }

        public void close() {
            try {
                active = false;
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void run() {
            try {
                out.println("WELCOME");
                name = in.readLine();
                System.out.println(name + " has connected.");

                out.println("Type 'ready' to start the game...");
                while (true) {
                    String msg = in.readLine();
                    if (msg == null) break;

                    if (msg.equalsIgnoreCase("ready")) {
                        System.out.println(name + " is ready.");
                        readyClients.add(this);
                        if (readyClients.size() == clients.size()) {
                            startGame();
                        }
                        break;
                    }
                }

                // Game loop
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("BINGO")) {
                        System.out.println(name + " claims BINGO!");
                        broadcast("WINNER:" + name);
                        stopGame = true;
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println(name + " disconnected.");
            } finally {
                readyClients.remove(this);
            }
        }
    }
}
