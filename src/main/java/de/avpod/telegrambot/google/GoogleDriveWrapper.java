package de.avpod.telegrambot.google;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import de.avpod.telegrambot.*;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;

@AllArgsConstructor
@Log4j2
public class GoogleDriveWrapper implements CloudWrapper {
    private static final String ROOT_FOLDER_NAME = "TelegramBot";
    private static final int MAX_RETRIES = 5;
    private final Drive drive;

    @Override
    public String uploadFile(UploadFile uploadFile) {
        return doRetryable(() -> {
            log.info("Uploading file with name {} for user {} to Google Drive",
                    uploadFile.getName(), uploadFile.getUsername()
            );

            String rootFolderId = getRootFolderId();
            String userFolderId = getUserFolderId(rootFolderId, uploadFile.getUsername());

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
        });
    }

    @Override
    public void recognizeDocument(String cloudId, DocumentType documentType) {
        doRetryable(() -> {
            log.info("Moving file with id {} to folder {}", cloudId, documentType.getSubfolderName());
            File file = drive.files().get(cloudId)
                    .setFields("parents")
                    .execute();
            StringBuilder previousParents = new StringBuilder();
            for (String parent : file.getParents()) {
                previousParents.append(parent);
                previousParents.append(',');
            }
            previousParents.deleteCharAt(previousParents.length() - 1);
            log.info("Previous parent folders for file {} were {}", cloudId, previousParents);

            String documentTypeFolderId = getDocumentFolderId(previousParents.toString(), documentType);


            log.info("Moving file {} to parents {}", cloudId, documentTypeFolderId);
            file = drive.files().update(cloudId, null)
                    .setAddParents(documentTypeFolderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
            log.info("File {} successfully moved to new directory", cloudId);
        });
    }

    @Override
    public void deleteDocument(String cloudId) {
        doRetryable(() -> {
            log.info("Deleting file {}", cloudId);
            drive.files().delete(cloudId);
            log.info("File {} successfully deleted", cloudId);
        });
    }

    private void doRetryable(ThrowingRunnable runnable) {
        this.doRetryable((ThrowingSupplier<Void>) () -> {
            runnable.runThrows();
            return null;
        });
    }

    private <T> T doRetryable(ThrowingSupplier<T> runnable) {
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            try {
                return runnable.getThrows();
            } catch (SocketException | SocketTimeoutException e) {
                log.warn("Cannot process operation in Google Drive due to socket error, retrying");
            } catch (Exception e) {
                log.error("Cannot process operation in Google Drive due to unknown error", e);
                throw new RuntimeException(e);
            }
        }
        log.error("Retries exceeded, giving up");
        throw new RuntimeException("Retries exceeded, giving up");
    }

    private String getRootFolderId() throws IOException {
        String rootFolderId;
        FileList rootFolderSearch = drive.files()
                .list()
                .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" + ROOT_FOLDER_NAME + "'")
                .execute();

        if (rootFolderSearch.getFiles().isEmpty()) {
            synchronized (GoogleDriveWrapper.this) {
                rootFolderSearch = drive.files()
                        .list()
                        .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" + ROOT_FOLDER_NAME + "'")
                        .execute();
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
            }
        } else {
            rootFolderId = rootFolderSearch.getFiles().get(0).getId();
            log.info("Root folder was found with id {}", rootFolderId);
        }
        return rootFolderId;
    }

    private String getUserFolderId(String rootFolderId, String username) throws IOException {
        String userFolderId;
        FileList userFolderSearch = drive.files()
                .list()
                .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" +
                        username + "'")
                .execute();

        if (userFolderSearch.getFiles().isEmpty()) {
            synchronized (GoogleDriveWrapper.this) {
                userFolderSearch = drive.files()
                        .list()
                        .setQ("trashed=false and mimeType='application/vnd.google-apps.folder' and name='" +
                                username + "'")
                        .execute();
                //TODO concurrency issues. lock on username here?
                if (userFolderSearch.getFiles().isEmpty()) {
                    File driveUserFolder = new File();
                    driveUserFolder.setName(username);
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
            }
        } else {
            userFolderId = userFolderSearch.getFiles().get(0).getId();
            log.info("User folder was found with id {}", userFolderId);
        }
        return userFolderId;
    }

    private String getDocumentFolderId(String parents, DocumentType documentType) throws IOException {
        String documentTypeFolderId;

        FileList givenDocumentTypeFolderSearch = drive.files()
                .list()
                .setQ("trashed=false and " +
                        "mimeType='application/vnd.google-apps.folder' and " +
                        "parents in '" + parents + "' and " +
                        "name='" + documentType.getSubfolderName() + "'")
                .execute();

        if (givenDocumentTypeFolderSearch.getFiles().isEmpty()) {
            synchronized (GoogleDriveWrapper.this) {
                //todo lock on username
                givenDocumentTypeFolderSearch = drive.files()
                        .list()
                        .setQ("trashed=false and " +
                                "mimeType='application/vnd.google-apps.folder' and " +
                                "parents in '" + parents + "' and " +
                                "name='" + documentType.getSubfolderName() + "'")
                        .execute();

                if (givenDocumentTypeFolderSearch.getFiles().isEmpty()) {
                    log.info("There is no folder for document type {} yet, creating new", documentType);
                    File driveUserFolder = new File();
                    driveUserFolder.setName(documentType.getSubfolderName());
                    driveUserFolder.setMimeType("application/vnd.google-apps.folder");
                    driveUserFolder.setParents(Collections.singletonList(parents));
                    driveUserFolder = drive.files().create(driveUserFolder)
                            .setFields("id, parents")
                            .execute();
                    documentTypeFolderId = driveUserFolder.getId();
                    log.info("Document type {} folder was created with id {}", documentType, documentTypeFolderId);
                } else {
                    documentTypeFolderId = givenDocumentTypeFolderSearch.getFiles().get(0).getId();
                    log.info("Document type {} folder was found with id {}", documentTypeFolderId);
                }
            }
        } else {
            documentTypeFolderId = givenDocumentTypeFolderSearch.getFiles().get(0).getId();
            log.info("Document type {} folder was found with id {}", documentTypeFolderId);
        }
        return documentTypeFolderId;
    }
}
