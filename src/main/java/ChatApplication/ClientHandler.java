package ChatApplication;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    public static final List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());
    public static final Map<String, List<ClientHandler>> groups = Collections.synchronizedMap(new HashMap<>());

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            if (clientUsername == null || clientUsername.isBlank()) clientUsername = "Unknown";
            clientHandlers.add(this);
            broadcastMessage("Server: " + clientUsername + " has entered the chat 👋");
            broadcastUserList();
            System.out.println("[+] " + clientUsername + " connected. Total: " + clientHandlers.size());
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String message;
        while (socket.isConnected()) {
            try {
                message = bufferedReader.readLine();
                if (message == null) break;

                System.out.println("[" + clientUsername + "]: " +
                        (message.startsWith("FILE|") ? "[FILE TRANSFER: " + message.split("\\|")[2] + "]" : message));

                if (message.startsWith("FILE|")) {
                    String[] parts = message.split("\\|", 6);
                    if (parts.length == 6) {
                        broadcastMessage(message);
                    }
                } else if (message.startsWith("/fileto ")) {
                    String[] split = message.split(" ", 3);
                    if (split.length == 3 && split[2].startsWith("FILE|")) {
                        sendPrivateFile(split[1], split[2]);
                    }
                } else if (message.startsWith("/gfile ")) {
                    String[] split = message.split(" ", 3);
                    if (split.length == 3 && split[2].startsWith("FILE|")) {
                        sendGroupFile(split[1], split[2]);
                    }
                } else if (message.startsWith("/msg ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        sendPrivateMessage(parts[1], parts[2]);
                    } else {
                        sendMessage("Server: Usage: /msg <username> <message>");
                    }
                } else if (message.equals("/users")) {
                    sendUserList();
                } else if (message.startsWith("/creategroup ")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        String groupName = parts[1].trim();
                        groups.putIfAbsent(groupName, Collections.synchronizedList(new ArrayList<>()));
                        List<ClientHandler> members = groups.get(groupName);
                        if (!members.contains(this)) members.add(this);
                        sendMessage("Server: ✅ Group '" + groupName + "' created and joined.");
                        broadcastGroupList();
                    }
                } else if (message.startsWith("/joingroup ")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        String groupName = parts[1].trim();
                        if (groups.containsKey(groupName)) {
                            List<ClientHandler> members = groups.get(groupName);
                            if (!members.contains(this)) members.add(this);
                            sendMessage("Server: ✅ Joined group '" + groupName + "'.");
                            broadcastToGroup(groupName, "Server: " + clientUsername + " joined the room.");
                        } else {
                            sendMessage("Server: ❌ Group '" + groupName + "' not found. Create it first.");
                        }
                    }
                } else if (message.startsWith("/g ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        sendGroupMessage(parts[1], parts[2]);
                    } else {
                        sendMessage("Server: Usage: /g <groupName> <message>");
                    }
                } else if (message.startsWith("/acceptfriend ")) {
                    String target = message.substring(14).trim();
                    synchronized (clientHandlers) {
                        for (ClientHandler c : clientHandlers) {
                            if (c.clientUsername.equalsIgnoreCase(target)) {
                                c.sendMessage("FRIEND_ACCEPT:" + clientUsername);
                            }
                        }
                    }
                } else if (message.startsWith("/friend ")) {
                    String target = message.substring(8).trim();
                    sendFriendRequest(target);
                } else if (message.startsWith("/invite ")) {
                    String[] invParts = message.split(" ", 3);
                    if (invParts.length == 3) {
                        String groupName = invParts[1], invitee = invParts[2];
                        sendMessage("Server: Invite sent to " + invitee);
                        synchronized (clientHandlers) {
                            for (ClientHandler c : clientHandlers) {
                                if (c.clientUsername.equalsIgnoreCase(invitee)) {
                                    c.sendMessage("Server: You have been invited to join group '" + groupName + "'.");
                                }
                            }
                        }
                    }
                } else {
                    broadcastMessage(clientUsername + ": " + message);
                }
            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }

    public void sendFriendRequest(String targetName) {
        boolean found = false;
        synchronized (clientHandlers) {
            for (ClientHandler c : clientHandlers) {
                if (c.clientUsername.equalsIgnoreCase(targetName)) {
                    c.sendMessage("FRIEND_REQUEST:" + clientUsername);
                    sendMessage("Server: ✅ Friend request sent to " + targetName);
                    found = true;
                }
            }
        }
        if (!found) sendMessage("Server: User '" + targetName + "' not found online.");
    }

    public void sendPrivateFile(String receiverName, String fileMsg) {
        boolean found = false;
        synchronized (clientHandlers) {
            for (ClientHandler c : clientHandlers) {
                if (c.clientUsername.equalsIgnoreCase(receiverName)) {
                    c.sendMessage(fileMsg);
                    found = true;
                }
            }
        }
        sendMessage(fileMsg);
        if (!found) sendMessage("Server: ❌ User '" + receiverName + "' not found.");
    }

    public void sendGroupFile(String groupName, String fileMsg) {
        List<ClientHandler> members = groups.get(groupName);
        if (members != null) {
            synchronized (members) {
                for (ClientHandler c : members) c.sendMessage(fileMsg);
            }
        } else {
            sendMessage("Server: ❌ Group '" + groupName + "' not found.");
        }
    }

    public void broadcastMessage(String message) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                try {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                } catch (IOException e) {
                    client.closeEverything();
                }
            }
        }
    }

    public void sendPrivateMessage(String receiverName, String msg) {
        boolean found = false;
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.clientUsername.equalsIgnoreCase(receiverName)) {
                    try {
                        client.bufferedWriter.write("(Private) " + clientUsername + ": " + msg);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                        found = true;
                    } catch (IOException e) {
                        client.closeEverything();
                    }
                }
            }
        }
        if (!found) {
            sendMessage("Server: ❌ User '" + receiverName + "' not found or offline.");
        }
    }

    public void sendGroupMessage(String groupName, String msg) {
        List<ClientHandler> members = groups.get(groupName);
        if (members != null) {
            broadcastToGroup(groupName, "[" + groupName + "] " + clientUsername + ": " + msg);
        } else {
            sendMessage("Server: ❌ Group '" + groupName + "' not found.");
        }
    }

    private void broadcastToGroup(String groupName, String message) {
        List<ClientHandler> members = groups.get(groupName);
        if (members == null) return;
        synchronized (members) {
            for (ClientHandler client : members) {
                try {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                } catch (IOException e) {
                    client.closeEverything();
                }
            }
        }
    }

    public void sendUserList() {
        StringBuilder sb = new StringBuilder("USERS:");
        synchronized (clientHandlers) {
            for (ClientHandler c : clientHandlers) {
                sb.append(c.clientUsername).append(",");
            }
        }
        sendMessage(sb.toString());
    }

    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder("USERS:");
        synchronized (clientHandlers) {
            for (ClientHandler c : clientHandlers) {
                sb.append(c.clientUsername).append(",");
            }
        }
        String userListMsg = sb.toString();
        synchronized (clientHandlers) {
            for (ClientHandler c : clientHandlers) {
                c.sendMessage(userListMsg);
            }
        }
    }

    private void broadcastGroupList() {}

    public void sendMessage(String msg) {
        try {
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void removeClient() {
        clientHandlers.remove(this);
        synchronized (groups) {
            for (List<ClientHandler> members : groups.values()) {
                members.remove(this);
            }
        }
        broadcastMessage("Server: " + clientUsername + " has left the chat 👋");
        broadcastUserList();
        System.out.println("[-] " + clientUsername + " disconnected. Total: " + clientHandlers.size());
    }

    public void closeEverything() {
        removeClient();
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}