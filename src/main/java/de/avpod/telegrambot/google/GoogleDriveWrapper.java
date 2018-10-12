package de.avpod.telegrambot.google;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import de.avpod.telegrambot.CloudWrapper;
import de.avpod.telegrambot.DocumentType;
import de.avpod.telegrambot.UploadFile;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Collections;

@AllArgsConstructor
@Log4j2
public class GoogleDriveWrapper implements CloudWrapper {
    private static final String ROOT_FOLDER_NAME = "TelegramBot";
    private final Drive drive;

    @Override
    public String uploadFile(UploadFile uploadFile) throws IOException {
        log.info("Uploading file with name {} for user {} to Google Drive",
                uploadFile.getName(), uploadFile.getUsername()
        );

        FileList rootFolderSearch = drive.files()
                .list()
                .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" + ROOT_FOLDER_NAME + "'")
                .execute();
        String rootFolderId;
        if (rootFolderSearch.getFiles().isEmpty()) {
            File driveRootFolder = new File();
            driveRootFolder.setName(ROOT_FOLDER_NAME);
            driveRootFolder.setMimeType("application/vnd.google-apps.folder");
            driveRootFolder = drive.files().create(driveRootFolder)
                    .setFields("id, parents")
                    .execute();
            rootFolderId = driveRootFolder.getId();
            log.info("Root folder was created with id {}", rootFolderId);
        } else {
            rootFolderId = rootFolderSearch.getFiles().get(0).getId();
            log.info("Root folder was found with id {}", rootFolderId);
        }

        FileList userFolderSearch = drive.files()
                .list()
                .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" + uploadFile.getUsername() + "'")
                .execute();
        String userFolderId;
        if (userFolderSearch.getFiles().isEmpty()) {
            File driveUserFolder = new File();
            driveUserFolder.setName(uploadFile.getUsername());
            driveUserFolder.setMimeType("application/vnd.google-apps.folder");
            driveUserFolder.setParents(Collections.singletonList(rootFolderId));
            driveUserFolder = drive.files().create(driveUserFolder)
                    .setFields("id, parents")
                    .execute();
            userFolderId = driveUserFolder.getId();
            log.info("User folder was created with id {}", userFolderId);
        } else {
            userFolderId = userFolderSearch.getFiles().get(0).getId();
            log.info("User folder was found with id {}", userFolderId);
        }

        File driveFile = new File();
        driveFile.setName(uploadFile.getName());
        driveFile.setMimeType(uploadFile.getMimeType());
        driveFile.setParents(Collections.singletonList(userFolderId));
        FileContent mediaContent = new FileContent(uploadFile.getMimeType(), uploadFile.getContent());
        File file = drive.files().create(driveFile, mediaContent)
                .setFields("id, parents")
                .execute();
        log.info("Uploaded file with ID: {}", file.getId());
        return file.getId();
    }

    @Override
    public void recognizeDocument(String cloudId, DocumentType documentType) {
        log.info("Moving file with id {} to folder {}", cloudId, documentType.getSubfolderName());
        try {
            File file = drive.files().get(cloudId)
                    .setFields("parents")
                    .execute();
            StringBuilder previousParents = new StringBuilder();
            for (String parent : file.getParents()) {
                previousParents.append(parent);
                previousParents.append(',');
            }
            log.info("Previous parent folders for file {} were {}", cloudId, previousParents);

            FileList givenDocumentTypeFolderSearch = drive.files()
                    .list()
                    .setQ("trashed=false and " +
                            "mimeType='application/vnd.google-apps.folder' and " +
                            "parents in '" + previousParents.toString() + "' and " +
                            "name='" + documentType.getSubfolderName() + "'")
                    .execute();
            String documentTypeFolderId;
            if (givenDocumentTypeFolderSearch.getFiles().isEmpty()) {
                log.info("There is no folder for document type {} yet, creating new", documentType);
                File driveUserFolder = new File();
                driveUserFolder.setName(documentType.getSubfolderName());
                driveUserFolder.setMimeType("application/vnd.google-apps.folder");
                driveUserFolder.setParents(file.getParents());
                driveUserFolder = drive.files().create(driveUserFolder)
                        .setFields("id, parents")
                        .execute();
                documentTypeFolderId = driveUserFolder.getId();
                log.info("Document type {} folder was created with id {}", documentType, documentTypeFolderId);
            } else {
                documentTypeFolderId = givenDocumentTypeFolderSearch.getFiles().get(0).getId();
                log.info("Document type {} folder was found with id {}", documentTypeFolderId);
            }
            file = drive.files().update(cloudId, null)
                    .setAddParents(previousParents.append(documentTypeFolderId).toString())
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
            log.info("File {} successfully moved to new directory", cloudId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDocument(String cloudId) {
        log.info("Deleting file {}", cloudId);
        try {
            drive.files().delete(cloudId);
            log.info("File {} successfully deleted", cloudId);
        } catch (IOException e) {
            log.error("Cannot delete file with id {}", cloudId, e);
        }
    }
}
