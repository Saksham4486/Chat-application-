
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientGroup {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public ClientGroup(Socket socket, String username) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.username = username;
        } catch (IOException e) {
            closeEverything();
        }
    }

    // Send messages
    public void sendMessage() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Scanner sc = new Scanner(System.in);

            while (socket.isConnected()) {
                String messageToSend = sc.nextLine();
                if (messageToSend.startsWith("@")) {
        bufferedWriter.write(messageToSend);  // send raw private message
        } else {
         bufferedWriter.write(username + ": " + messageToSend); // normal message
        }
                bufferedWriter.newLine();
                bufferedWriter.flush();
                if (messageToSend.equalsIgnoreCase("BYE")) {
        closeEverything();
        break;
            }
   
}

        } catch (IOException e) {
            closeEverything();
        }
    }

    // Receive messages (thread)
    public void listenForMessage() {
        new Thread(() -> {
            String msgFromGroupChat;

            while (socket.isConnected()) {
                try {
                    msgFromGroupChat = bufferedReader.readLine();
                    if (msgFromGroupChat == null) break;
                    System.out.println(msgFromGroupChat);
                } catch (IOException e) {
                    closeEverything();
                    break;
                }
            }
        }).start();
    }

    public void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = sc.nextLine();

        try {
            Socket socket = new Socket("localhost", 1234);

            ClientGroup client = new ClientGroup(socket, username);
            client.listenForMessage();
            client.sendMessage();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}