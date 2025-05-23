import java.io.*;
import java.net.*;

public class BingoClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        BingoCard card = new BingoCard();

        if (in.readLine().equals("WELCOME")) {
            System.out.print("Enter your name: ");
            String name = console.readLine();
            out.println(name);
        }

        // Wait for ready prompt
        System.out.println(in.readLine());
        System.out.print("Press Enter to send 'ready': ");
        console.readLine();
        out.println("ready");

        System.out.println("Waiting for game to start...");

        new Thread(() -> {
            try {
                String line;
                boolean gameStarted = false;

                while ((line = in.readLine()) != null) {
                    if (line.startsWith("GAME_START")) {
                        System.out.println("🎯 Game started!");
                        gameStarted = true;
                        card.printCard();
                    } else if (line.startsWith("NUMBER:") && gameStarted) {
                        int number = Integer.parseInt(line.split(":")[1]);
                        System.out.println("Number drawn: " + number);
                        card.markNumber(number);
                        card.printCard();

                        if (card.checkBingo()) {
                            System.out.println("🎉 You got BINGO! 🎉");
                            out.println("BINGO");
                        }

                    } else if (line.startsWith("WINNER:")) {
                        String winner = line.split(":")[1];
                        System.out.println(winner + " has won the game!");
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected.");
            }
        }).start();
    }
}
