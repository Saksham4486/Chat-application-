package ChatApplication;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMulti {

    private ServerSocket serverSocket;

    public ServerMulti(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        try {
            System.out.println("ChatApp Server started on port " + serverSocket.getLocalPort());
            System.out.println("--> LAN IP Address for other laptops: " + InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {}
        
        System.out.println("Waiting for clients...\n");
        
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            closeServer();
        }
    }

    public void closeServer() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            ServerMulti server = new ServerMulti(serverSocket);
            server.startServer();
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}