import java.util.*;

public class BingoCard {
    private final int SIZE = 5;
    private int[][] card = new int[SIZE][SIZE];
    private boolean[][] marked = new boolean[SIZE][SIZE];

    public BingoCard() {
        generateCard();
    }

    private void generateCard() {
        Random rand = new Random();
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int num;
                do {
                    num = rand.nextInt(75) + 1;
                } while (used.contains(num));
                used.add(num);
                card[i][j] = num;
                marked[i][j] = false;
            }
        }
        card[2][2] = 0; // Free space in center
        marked[2][2] = true;
    }

    public void markNumber(int number) {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                if (card[i][j] == number)
                    marked[i][j] = true;
    }

    public boolean checkBingo() {
        for (int i = 0; i < SIZE; i++) {
            // Check rows
            boolean row = true, col = true;
            for (int j = 0; j < SIZE; j++) {
                row &= marked[i][j];
                col &= marked[j][i];
            }
            if (row || col) return true;
        }

        // Check diagonals
        boolean diag1 = true, diag2 = true;
        for (int i = 0; i < SIZE; i++) {
            diag1 &= marked[i][i];
            diag2 &= marked[i][SIZE - i - 1];
        }
        return diag1 || diag2;
    }

    public void printCard() {
        System.out.println("Your Bingo Card:");
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (card[i][j] == 0) System.out.print("  * ");
                else System.out.printf("%3d ", marked[i][j] ? -card[i][j] : card[i][j]);
            }
            System.out.println();
        }
    }
        // --- Getters for GUI ---
    public int[][] getCardNumbers() {
        return card;
    }

    public boolean[][] getMarkedStatus() {
        return marked;
    }
}
