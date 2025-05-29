# BingoCN311

# 🎯 Multiplayer Bingo Game (Java Socket)

A simple multiplayer Bingo game built using Java's socket programming.

## 📦 Features

- Server handles multiple clients
- Game starts only when all clients type `ready` or press Enter
- Server broadcasts random Bingo numbers (1–75)
- First client to type `BINGO` wins the round
- Server announces the winner and resets for the next round

## 🛠 Requirements

- Java 8 or higher
- Terminal or Command Prompt

## 🚀 How to Run

### 1. Compile

```bash
javac BingoServer.java BingoClient.java BingoCard.java
```

### 2. Run Server

```bash
java BingoServer
```

### 3. Run Clients (in separate terminals)

```bash
java BingoClient
```
You can run multiple clients on the same computer (localhost) for testing.
### 4. Run Clients (with GUI)
If you would like to play the game with GUI, run this command instead:

```bash
java BingoGUI
```

## 🎮 Game Flow

1. Clients connect and enter their name.
2. Each client types ready to indicate they're ready.
3. When all clients are ready, the server starts the game.
4. Server sends random Bingo numbers every 3 seconds.
5. Clients type BINGO when they complete their card.
6. Server announces the winner and resets for the next round.

## 🧼 Server Behavior

- Server resets after a win and waits for new clients to connect
- All previous connections are closed after a game ends