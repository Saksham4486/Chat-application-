package ChatApplication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

public class GoogleDriveManager {
    private static final String APPLICATION_NAME = "ChatApp Backup";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
            new InputStreamReader(GoogleDriveManager.class.getResourceAsStream("/credentials.json")));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void uploadBackupToDrive(java.io.File localFile, String userEmail) throws Exception {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    
    // We pass the userEmail here to create a specific credential folder for that user
    Credential credential = getCredentials(HTTP_TRANSPORT, userEmail);

    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();

    // Prepare File Metadata
    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
    fileMetadata.setName(localFile.getName());
    fileMetadata.setMimeType("application/json");

    // Prepare File Content
    FileContent mediaContent = new FileContent("application/json", localFile);

    // Execute Upload
    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute();

    System.out.println("Cloud Upload Success! File ID: " + uploadedFile.getId());
}

private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, String userId) throws Exception {
    InputStream in = GoogleDriveManager.class.getResourceAsStream("/credentials.json");
    if (in == null) {
        throw new FileNotFoundException("Resource not found: /credentials.json");
    }
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // The dataStoreDir saves the login token so you don't have to login EVERY time
    java.io.File dataStoreDir = new java.io.File(System.getProperty("user.home"), ".credentials/jlink-chat");
    
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE_FILE))
            .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build();

    // Use the userId (email) to find or create the specific token
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize(userId);
}
    }
