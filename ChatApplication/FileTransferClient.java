package ChatApplication;

import java.io.*;
import java.net.*;
import java.util.*;

public class FileTransferClient {
    private Socket socket;
    private FileTransferHandler fileTransferHandler;
    private BackupManager backupManager;
    private RestoreManager restoreManager;
    private Scanner scanner;

    public FileTransferClient(String host, int port) throws IOException
    {
        this.socket = new Socket(host, port);
        this.fileTransferHandler = new FileTransferHandler(socket);
        this.backupManager = new BackupManager("groupchat");
        this.restoreManager = null;
        this.scanner = new Scanner(System.in);
        System.out.println("Connected to server: " + host + ":" + port);
    }

    public void start() throws IOException
    {
        listenForMessages();

        System.out.println("================================");
        System.out.println("Commands:");
        System.out.println("SENDFILE  → send file");
        System.out.println("BACKUP    → save chat");
        System.out.println("RESTORE   → load backup");
        System.out.println("HISTORY   → view messages");
        System.out.println("EXIT      → close");
        System.out.println("================================");

        while(true)
        {
            String input = scanner.nextLine();

            if(input.equalsIgnoreCase("SENDFILE"))
            {
                System.out.println("Enter file path: ");
                String filePath = scanner.nextLine();
                fileTransferHandler.sendFile(filePath);
            }
            else if(input.equalsIgnoreCase("BACKUP"))
            {
                System.out.println("Load from database? (yes/no): ");
                String loadDB = scanner.nextLine();
                if(loadDB.equalsIgnoreCase("yes"))
                {
                    backupManager.loadFromDatabase();
                }
                System.out.println("Choose format (txt/json): ");
                String format = scanner.nextLine();
                if(format.equalsIgnoreCase("txt"))
                    backupManager.exportToTxt();
                else if(format.equalsIgnoreCase("json"))
                    backupManager.exportToJson();
                else
                    System.out.println("Invalid format — type txt or json");
            }
            else if(input.equalsIgnoreCase("RESTORE"))
            {
                System.out.println("Enter backup file path: ");
                String filePath = scanner.nextLine();
                restoreManager = new RestoreManager(filePath);
                System.out.println("Choose format (txt/json): ");
                String format = scanner.nextLine();
                if(format.equalsIgnoreCase("txt"))
                    restoreManager.restoreFromTxt();
                else if(format.equalsIgnoreCase("json"))
                    restoreManager.restoreFromJson();
                else
                    System.out.println("Invalid format — type txt or json");
            }
            else if(input.equalsIgnoreCase("HISTORY"))
            {
                if(restoreManager == null)
                    System.out.println("Please RESTORE first!");
                else
                    restoreManager.printMessages();
            }
            else if(input.equalsIgnoreCase("EXIT"))
            {
                System.out.println("Disconnecting...");
                fileTransferHandler.closeEverything();
                socket.close();
                break;
            }
            else if(input.trim().isEmpty())
            {
                // blank line ignore
            }
            else
            {
                System.out.println("Unknown command — type SENDFILE, BACKUP, RESTORE, HISTORY or EXIT");
            }
        }
    }

    private void listenForMessages()
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                    String message;
                    while((message = bufferedReader.readLine()) != null)
                    {
                        if(message.equals("FILE:"))
                        {
                            System.out.println("Incoming file transfer...");
                            fileTransferHandler.receiveFile();
                        }
                        else
                        {
                            System.out.println(message);
                            if(message.contains(":"))
                            {
                                String[] parts = message.split(":", 2);
                                backupManager.addMessage(parts[0].trim(), parts[1].trim());
                            }
                        }
                    }
                }
                catch(IOException e)
                {
                    System.out.println("Connection closed: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void main(String[] args)
    {
        try
        {
            FileTransferClient client = new FileTransferClient("localhost", 1234);
            client.start();
        }
        catch(IOException e)
        {
            System.out.println("Server not connected: " + e.getMessage());
        }
    }
}