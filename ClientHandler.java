
import java.io.*;
import java.net.*;
import java.util.*;

import ChatApplication.MessageDAO;
public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private MessageDAO messageDAO;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.messageDAO = new MessageDAO();

            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);

            broadcastMessage("Server: " + clientUsername + " has entered the chat");

        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) break;
                
                if (messageFromClient.startsWith("@")) {
                    String[] parts = messageFromClient.split(" ", 2);
                    if (parts.length == 2) {
                        String receiver = parts[0].substring(1);
                        String msg = parts[1];
                        sendPrivateMessage(msg, receiver);
                        messageDAO.saveMessage(clientUsername, "(Private to " + receiver + ") " + msg);
                    }
                } else {
                    broadcastMessage(messageFromClient);
                    messageDAO.saveMessage(clientUsername, messageFromClient);
                }

            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything();
            }
        }
    }

    public void sendPrivateMessage(String message, String receiverUsername) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientUsername.equals(receiverUsername)) {
                try {
                    clientHandler.bufferedWriter.write("(Private) " + this.clientUsername + ": " + message);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("Server: " + clientUsername + " has left the chat");
    }

    public void closeEverything() {
        removeClientHandler();
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}