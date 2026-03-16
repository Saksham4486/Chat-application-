import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try {

            Socket socket = new Socket("localhost", 1234);

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
                        System.out.println("Server: " + msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Send messages to server
            while (true) {

                String msgToSend = sc.nextLine();

                bufferedWriter.write(msgToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                if (msgToSend.equalsIgnoreCase("BYE"))
                    break;
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}