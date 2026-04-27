package ChatApplication;

public class FileData
{
    private String fileName;
    private long fileSize;
    private String fileType;

    public FileData(String fileName, long fileSize, String fileType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

    public String getFileName() 
    { return fileName; }
    public long getFileSize() 
    { return fileSize; }
    public String getFileType()
    { return fileType; }

    //for sender
    public String toTransferString() {
        return fileName + "," + fileSize + "," + fileType;
    }

    //for receiver 
    public static FileData fromTransferString(String data) {
        String[] parts = data.split(",");
        return new FileData(parts[0], Long.parseLong(parts[1]), parts[2]);
    }

}