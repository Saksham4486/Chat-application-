import java.io.*;
import java.util.*;
import java.net.*;
public class ClientHandler implements Runnable {
  public static ArrayList<ClientHandler> ClientHandlers=new ArrayList<>();
private Socket socket;//used for establishing the connection between the server and client
private BufferedReader bufferedReader;//used to read data that sent from the client
private BufferedWriter bufferedWriter;//used to send data to clients 
private String clientUsername;
  public ClientHandler(Socket socket) {
    try{
      this.socket=socket;
      this.bufferedWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      this.bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.clientUsername=bufferedReader.readLine();
      //add user to array list 
      ClientHandlers.add(this);
      broadcastmessage("Server:"+clientUsername+"has enetered the chat");
    }catch(IOException e)
    {
      closeEverything(socket,bufferedReader,bufferedWriter);
    }
  }
  public void run()
  {
    String messagefromclient;
    while(socket.isConnected())
    {
      try{
          messagefromclient=bufferedReader.readLine();
          broadcastmessage(messagefromclient);
      }catch(IOException e)
      {
        closeEverything(socket,bufferedReader,bufferedWriter);
        break;
      }
    }
  }
  public void broadcastmessage(String messagetosend)
  {
for(ClientHandler clientHandler:ClientHandlers)
  {
    try{
      if(!clientHandler.clientUsername.equals(clientUsername))
      {
        clientHandler.bufferedWriter.write(messagetosend);
        clientHandler.bufferedWriter.newLine();
        clientHandler.bufferedWriter.flush();
      }
    }catch(IOException e)
    {
      closeEverything(socket,bufferedReader,bufferedWriter);
    }
  }
  }
  public void removeClientHandler()
  {
    ClientHandlers.remove(this);
    broadcastmessage("Server"+clientUsername+"has left the chat!");
  }
  public void closeEverything(Socket socket,BufferedReader bufferedReader,BufferedWriter bufferedWriter)
  {
    try{
        if(bufferedReader!=null)
          bufferedReader.close();
        if(bufferedWriter!=null)
          bufferedWriter.close();
        if(socket!=null)
          socket.close();
    }catch(IOException e)
    {
      e.printStackTrace();
    }
  }

}
