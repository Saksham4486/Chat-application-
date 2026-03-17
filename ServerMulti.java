import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
public class ServerMulti {
  private ServerSocket serverSocket;
  public ServerMulti(ServerSocket serverSocket)
  {
    this.serverSocket=serverSocket;
  }
  public void startserver()
  {
      try{
        while(!serverSocket.isClosed())//we will wait for client to connect
        {
          Socket socket=serverSocket.accept();//it is a blocking method program will be halted until user connects
          System.out.println("New client has connected");
          ClientHandler clientHandler=new ClientHandler(socket);//each object of this class is responsible for communicating
          Thread thread=new Thread(clientHandler);
          thread.start();

        }
      }catch(IOException e)
      {

      }
  }
  public void closeserver()
  {
    try{
        if(serverSocket!=null)
        {
          serverSocket.close();
        }

    }catch(IOException e)
    {
      e.printStackTrace();
    }
  }
  public static void main(String args[])throws IOException
  {
    ServerSocket serverSocket=new ServerSocket(1234);
    ServerMulti server=new ServerMulti(serverSocket);
    server.startserver();

  }
  
}
