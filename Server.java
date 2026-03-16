import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server {

    public static void main(String[] args) {

        try {

            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Server started...");

            Socket socket = serverSocket.accept();
            System.out.println("Client connected");

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()));

            BufferedWriter bufferedWriter =
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            Scanner sc = new Scanner(System.in);

            // Thread to receive messages
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = bufferedReader.readLine()) != null) {
                        System.out.println("Client: " + msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Send messages to client
            while (true) {

                String msgToSend = sc.nextLine();

                bufferedWriter.write(msgToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                if (msgToSend.equalsIgnoreCase("BYE"))
                    break;
            }

            socket.close();
            serverSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}