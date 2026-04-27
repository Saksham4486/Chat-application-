package ChatApplication;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public class FileShareManager {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100 MB hard cap
    private static final long INLINE_PREVIEW_LIMIT = 25L * 1024 * 1024; // 25 MB for inline
    private static final String RECV_DIR = "ChatApp_Received";

    /**
     * @param parent      parent JFrame for the dialog
     * @param username    sending user's name
     * @param onProgress  optional callback for progress 
     */
    public static String chooseAndEncodeFile(Component parent, String username,
                                              Consumer<Integer> onProgress) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Send File — Image, Video, Document, Audio");
        fc.setMultiSelectionEnabled(false);

        // Filter groups
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Images (png, jpg, gif, bmp, webp)",
                "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Videos (mp4, avi, mov, mkv, webm)",
                "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Audio (mp3, wav, ogg, aac, flac)",
                "mp3", "wav", "ogg", "aac", "flac", "m4a", "wma"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Documents (pdf, doc, docx, xls, xlsx, ppt, txt, zip)",
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "csv", "json", "xml", "zip", "rar", "7z"));
        fc.setAcceptAllFileFilterUsed(true);

        int result = fc.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null;

        File file = fc.getSelectedFile();
        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(parent, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        long size = file.length();
        if (size == 0) {
            JOptionPane.showMessageDialog(parent, "File is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (size > MAX_FILE_SIZE) {
            JOptionPane.showMessageDialog(parent,
                    "File too large! Maximum is 100 MB.\nSelected: " + formatSize(size),
                    "File Too Large", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String name = file.getName();
        String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "bin";

        try {
            byte[] bytes = readWithProgress(file, onProgress);
            String b64   = Base64.getEncoder().encodeToString(bytes);
            return FileData.toFileMessage(username, name, ext, size, b64);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Could not read file: " + e.getMessage(),
                    "Read Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // ── Receive & Render 
    /**
     * Decodes a FILE| line and builds the chat bubble panel to display it.
     *
     * @param fileParts   result of FileData.parseFileMessage()
     * @param isOwn       true if the current user sent this file
     */
    public static JPanel buildFileBubble(String[] fileParts, boolean isOwn) {
        // fileParts: [FILE, sender, fileName, fileType, fileSize, base64]
        String sender   = fileParts[1];
        String fileName = fileParts[2];
        String fileType = fileParts[3];
        long   fileSize;
        try { fileSize = Long.parseLong(fileParts[4]); } catch (NumberFormatException e) { fileSize = 0; }
        String base64   = fileParts[5];

        FileData meta = new FileData(fileName, fileSize, fileType);
        FileData.FileCategory cat = meta.getCategory();

        Color bubbleBg = isOwn ? new Color(70, 100, 165) : new Color(30, 40, 70);

        JPanel outer = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2));
        outer.setOpaque(false);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        // Sender label (for others' messages)
        if (!isOwn) {
            JLabel nameLbl = new JLabel(sender);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            nameLbl.setForeground(new Color(160, 180, 230));
            box.add(nameLbl);
            box.add(Box.createVerticalStrut(3));
        }

        // Main bubble
        JPanel bubble = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bubbleBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        if (cat == FileData.FileCategory.IMAGE) {
            buildImagePreview(bubble, base64, fileName, fileSize, isOwn);
        } else if (cat == FileData.FileCategory.VIDEO) {
            buildVideoAttachment(bubble, base64, fileName, fileSize, fileType);
        } else if (cat == FileData.FileCategory.AUDIO) {
            buildAudioAttachment(bubble, base64, fileName, fileSize, fileType);
        } else {
            buildDocumentAttachment(bubble, base64, fileName, fileSize, meta.getCategoryIcon());
        }

        box.add(bubble);
        outer.add(box);
        return outer;
    }

    // ── Image Preview ─────────────────────────────────────────────────────────

    private static void buildImagePreview(JPanel bubble, String base64,
                                           String fileName, long fileSize, boolean isOwn) {
        try {
            byte[] imgBytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));

            if (img != null) {
                // Scale to max 260×200 preserving aspect ratio
                int maxW = 260, maxH = 200;
                int w = img.getWidth(), h = img.getHeight();
                double scale = Math.min((double) maxW / w, (double) maxH / h);
                if (scale > 1) scale = 1;
                int sw = (int) (w * scale), sh = (int) (h * scale);
                Image scaled = img.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);

                JLabel imgLbl = new JLabel(icon);
                imgLbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                imgLbl.setAlignmentX(isOwn ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
                imgLbl.setToolTipText("Click to open full size");

                // Click → open full-size in dialog
                final BufferedImage finalImg = img;
                imgLbl.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        showFullImage(finalImg, fileName);
                    }
                });

                bubble.add(imgLbl);
                bubble.add(Box.createVerticalStrut(6));
            }
        } catch (Exception ex) {
            bubble.add(errorLabel("Could not render image"));
        }

        // File name + size + save button
        addFileFooter(bubble, base64, fileName, fileSize, "🖼", isOwn);
    }

    // ── Video Attachment ──────────────────────────────────────────────────────

    private static void buildVideoAttachment(JPanel bubble, String base64,
                                              String fileName, long fileSize, String fileType) {
        // Video thumbnail placeholder
        JPanel thumb = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 36));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("▶", (getWidth() - fm.stringWidth("▶")) / 2,
                        (getHeight() + fm.getAscent()) / 2 - 4);
            }
        };
        thumb.setPreferredSize(new Dimension(240, 130));
        thumb.setMaximumSize(new Dimension(240, 130));
        thumb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        thumb.setToolTipText("Click to save and open video");

        thumb.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                saveAndOpen(base64, fileName, fileType, thumb);
            }
        });

        bubble.add(thumb);
        bubble.add(Box.createVerticalStrut(8));
        addFileFooter(bubble, base64, fileName, fileSize, "🎬", false);
    }

    // ── Audio Attachment ──────────────────────────────────────────────────────

    private static void buildAudioAttachment(JPanel bubble, String base64,
                                              String fileName, long fileSize, String fileType) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        JLabel icon = new JLabel("🎵");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(truncate(fileName, 28));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel sizeLbl = new JLabel(formatSize(fileSize) + " • Audio");
        sizeLbl.setForeground(new Color(180, 200, 230));
        sizeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        info.add(nameLbl);
        info.add(sizeLbl);

        JButton playBtn = attachButton("▶ Play / Save");
        playBtn.addActionListener(e -> saveAndOpen(base64, fileName, fileType, playBtn));

        row.add(icon);
        row.add(info);
        row.add(playBtn);
        bubble.add(row);
    }

    // ── Document Attachment ───────────────────────────────────────────────────

    private static void buildDocumentAttachment(JPanel bubble, String base64,
                                                  String fileName, long fileSize, String icon) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row.setOpaque(false);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(truncate(fileName, 28));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel sizeLbl = new JLabel(formatSize(fileSize));
        sizeLbl.setForeground(new Color(180, 200, 230));
        sizeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        info.add(nameLbl);
        info.add(sizeLbl);
        row.add(iconLbl);
        row.add(info);
        bubble.add(row);
        bubble.add(Box.createVerticalStrut(8));
        addFileFooter(bubble, base64, fileName, fileSize, icon, false);
    }

    // ── Shared Footer (download + save button) ────────────────────────────────

    private static void addFileFooter(JPanel bubble, String base64,
                                       String fileName, long fileSize,
                                       String icon, boolean isOwn) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        footer.setOpaque(false);

        JLabel meta = new JLabel(icon + " " + truncate(fileName, 22) + "  •  " + formatSize(fileSize));
        meta.setForeground(new Color(190, 205, 235));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JButton saveBtn = attachButton("⬇ Save");
        saveBtn.addActionListener(e -> saveFile(base64, fileName, saveBtn));

        footer.add(meta);
        footer.add(saveBtn);
        bubble.add(footer);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void showFullImage(BufferedImage img, String title) {
        JDialog dlg = new JDialog();
        dlg.setTitle(title);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setModal(false);

        int maxW = 900, maxH = 700;
        int w = img.getWidth(), h = img.getHeight();
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale > 1) scale = 1;
        int sw = (int)(w * scale), sh = (int)(h * scale);
        Image scaled = img.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);

        JLabel lbl = new JLabel(new ImageIcon(scaled));
        lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        dlg.add(lbl);
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
    }

    private static void saveFile(String base64, String fileName, Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        fc.setDialogTitle("Save File As");
        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                Files.write(dest.toPath(), bytes);
                JOptionPane.showMessageDialog(parent,
                        "✅ Saved to: " + dest.getAbsolutePath(),
                        "File Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent,
                        "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveAndOpen(String base64, String fileName, String ext, Component parent) {
        try {
            File dir = new File(RECV_DIR);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, fileName);
            byte[] bytes = Base64.getDecoder().decode(base64);
            Files.write(dest.toPath(), bytes);
            Desktop.getDesktop().open(dest);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static byte[] readWithProgress(File file, Consumer<Integer> onProgress) throws IOException {
        long total = file.length();
        byte[] bytes = new byte[(int) total];
        try (FileInputStream fis = new FileInputStream(file)) {
            long read = 0;
            int chunk;
            while (read < total) {
                chunk = fis.read(bytes, (int) read, (int) Math.min(65536, total - read));
                if (chunk == -1) break;
                read += chunk;
                if (onProgress != null) onProgress.accept((int)(read * 100 / total));
            }
        }
        return bytes;
    }

    private static JButton attachButton(String label) {
        JButton btn = new JButton(label) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(80, 110, 170));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 26));
        return btn;
    }

    private static JLabel errorLabel(String msg) {
        JLabel lbl = new JLabel(msg);
        lbl.setForeground(new Color(255, 100, 100));
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        return lbl;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)               return bytes + " B";
        if (bytes < 1024 * 1024)        return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
