
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientGroup {
private Socket socket;
private BufferedReader bufferedReader;
private BufferedWriter bufferedWriter;
private String username;
public ClientGroup(Socket socket,String username)
{ try{  
    this.socket=socket;
    this.bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.bufferedWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    this.username=username;
}catch(Exception e){
    closeEverything(socket, bufferedReader, bufferedWriter);
}
}
public void sendtomessage()
{
    try{
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            @SuppressWarnings("resource")
            Scanner sc=new Scanner(System.in);
            while(socket.isConnected())
            {
                String messagetosend=sc.nextLine();
                bufferedWriter.write(username+": "+messagetosend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
    }catch(IOException e)
    {closeEverything(socket,bufferedReader,bufferedWriter);

    }
}
public void listenforMessage()
{//thread to receive message
    new Thread(new Runnable() {
        public void run()
        { while(socket.isConnected())
        { try{
            String msgfromgroupchat=bufferedReader.readLine();
            System.out.println(msgfromgroupchat);
        }catch(IOException e)
            {
                closeEverything(socket,bufferedReader,bufferedWriter);

            }
    }
    }
    }).start();
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

    public static void main(String[] args) {
            Scanner sc=new Scanner(System.in);
            System.out.println("Enter your username for the groupchat: ");
            String username=sc.nextLine();
            Socket socket = null;
            try {
                socket = new Socket("localhost",1234);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ClientGroup client=new ClientGroup(socket,username);
            client.listenforMessage();
            client.sendtomessage();

       
        }
    }