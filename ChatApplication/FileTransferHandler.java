package ChatApplication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class FileTransferHandler {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    public FileTransferHandler(Socket socket) throws IOException
    {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public  void sendFile(String filePath) throws IOException
    {
        File file=new File(filePath);
        String fileName = file.getName();
        long fileSize = file.length();
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
        FileData metadata = new FileData(fileName, fileSize, fileType);
        bufferedWriter.write(metadata.toTransferString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesSent = 0;
        System.out.println("Sending file: " + fileName + " (" + fileSize + " bytes)");
        while((bytesRead = fileInputStream.read(buffer)) != -1)
        {
            outputStream.write(buffer, 0, bytesRead);
            totalBytesSent += bytesRead;
            int progress = (int)((totalBytesSent * 100) / fileSize);
            System.out.println("Sending: " + progress + "% complete");
        }
        System.out.println("File sent successfully: " + fileName);
        outputStream.flush();
        fileInputStream.close();
    }

    public  void receiveFile() throws IOException
    {
        String metadataString = bufferedReader.readLine();
        FileData metadata = FileData.fromTransferString(metadataString);
        System.out.println("Receiving file: " + metadata.getFileName() + " (" + metadata.getFileSize() + " bytes)");
        File existingFile = new File(metadata.getFileName());
        if(existingFile.exists())
        {
            System.out.println("Warning: File already exists, overwriting: " + metadata.getFileName());
        }
        FileOutputStream fileOutputStream = new FileOutputStream(metadata.getFileName());
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesReceived = 0;
        long fileSize = metadata.getFileSize();
        while(totalBytesReceived < fileSize)
        {
            bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesReceived));
            fileOutputStream.write(buffer, 0, bytesRead);
            totalBytesReceived += bytesRead;
        }

        fileOutputStream.flush();
        fileOutputStream.close();
        System.out.println("File received: " + metadata.getFileName());
    }

    public  void closeEverything()
    {
        try
        {
            if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();
            if(socket != null)
                socket.close();
        }
        catch(IOException e)
        {
            System.out.println("Something went wrong: " + e.getMessage());

        }
        
    }
}