package de.avpod.telegrambot.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import de.avpod.telegrambot.CloudWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import static com.google.api.services.drive.DriveScopes.DRIVE_APPDATA;
import static com.google.api.services.drive.DriveScopes.DRIVE_METADATA;

@Configuration
@Log4j2
public class GoogleDriveConfiguration {
    private static final String APPLICATION_NAME = "Avpod TelegramBot Google Drive";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_FILE, DRIVE_APPDATA, DRIVE_METADATA);
    private static final String CREDENTIALS_FILE_PATH = "/drive_credentials.json";

    @Value("${google.oauth.host:localhost}")
    private String oauthVerifierHost;
    @Value("${google.oauth.port:8090}")
    private int oauthVerifierPort;

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveConfiguration.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow,
                new LocalServerReceiver.Builder()
                        .setHost(oauthVerifierHost)
                        .setPort(oauthVerifierPort)
                        .build()
        ).authorize("user");
    }

    @Bean
    public CloudWrapper drive() throws IOException, GeneralSecurityException {
        log.info("Creating Google Drive instance");
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setHttpRequestInitializer(request -> {
                    credential.initialize(request);
                    request.setConnectTimeout(60 * 1000); // 3 minutes
                    request.setReadTimeout(60 * 1000); // 3 minutes
                })
                .setApplicationName(APPLICATION_NAME)
                .build();
        return new GoogleDriveWrapper(drive);
    }

}
