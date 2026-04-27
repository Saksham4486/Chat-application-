package ChatApplication;

import java.util.Set;

public class FileData {
    private String fileName;
    private long fileSize;
    private String fileType;

    private static final Set<String> IMAGE_EXTS = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "tiff");
    private static final Set<String> VIDEO_EXTS = Set.of(
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp");
    private static final Set<String> AUDIO_EXTS = Set.of(
            "mp3", "wav", "ogg", "aac", "flac", "m4a", "wma");
    private static final Set<String> DOC_EXTS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "json", "xml", "zip", "rar", "7z");

    public enum FileCategory { IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER }

    public FileData(String fileName, long fileSize, String fileType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType.toLowerCase();
    }

    public String getFileName()  { return fileName; }
    public long   getFileSize()  { return fileSize; }
    public String getFileType()  { return fileType; }

    public FileCategory getCategory() {
        String ext = fileType.toLowerCase();
        if (IMAGE_EXTS.contains(ext)) return FileCategory.IMAGE;
        if (VIDEO_EXTS.contains(ext)) return FileCategory.VIDEO;
        if (AUDIO_EXTS.contains(ext)) return FileCategory.AUDIO;
        if (DOC_EXTS.contains(ext))   return FileCategory.DOCUMENT;
        return FileCategory.OTHER;
    }

    public String getCategoryIcon() {
        return switch (getCategory()) {
            case IMAGE    -> "🖼";
            case VIDEO    -> "🎬";
            case AUDIO    -> "🎵";
            case DOCUMENT -> "📄";
            case OTHER    -> "📎";
        };
    }

    public String getFormattedSize() {
        if (fileSize < 1024)               return fileSize + " B";
        if (fileSize < 1024 * 1024)        return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024L * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    public String toTransferString() {
        return fileName + "," + fileSize + "," + fileType;
    }

    public static FileData fromTransferString(String data) {
        String[] parts = data.split(",", 3);
        return new FileData(parts[0], Long.parseLong(parts[1]), parts[2]);
    }

    /**
     * Protocol line sent through the chat text channel:
     *   FILE|<sender>|<fileName>|<fileType>|<fileSize>|<base64data>
     */
    public static String toFileMessage(String sender, String fileName,
                                       String fileType, long fileSize,
                                       String base64data) {
        return "FILE|" + sender + "|" + fileName + "|" + fileType + "|" + fileSize + "|" + base64data;
    }

    /**
     * Returns String[6]: [FILE, sender, fileName, fileType, fileSize, base64data]
     * or null if not a FILE line.
     */
    public static String[] parseFileMessage(String line) {
        if (line == null || !line.startsWith("FILE|")) return null;
        String[] parts = line.split("\\|", 6);
        if (parts.length != 6) return null;
        return parts;
    }
}
