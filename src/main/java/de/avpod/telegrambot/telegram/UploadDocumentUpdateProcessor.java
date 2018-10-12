package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.*;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Message;

import java.io.File;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Log4j2
@AllArgsConstructor
public class UploadDocumentUpdateProcessor implements DocumentUpdateProcessor {
    private final TelegramFilesLoader filesLoader;
    private final CloudWrapper cloudWrapper;
    private final Executor handlerExecutor;
    private final PersistentStorageWrapper persistentStorage;
    private final ImageTypeRecognitionJobTrigger imageTypeRecognitionJobTrigger;

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processDocumentMessage(Document document, Message message) {
        log.info("Processing document message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );
        long chatId = message.getChat().getId();

        CompletableFuture<ProcessingResult> responseFuture = new CompletableFuture<>();
        handlerExecutor.execute(() -> {
            try {
                File downloadedFile = filesLoader.downloadFile(document.getFileId());
                String fileName = message.getFrom().getUserName() + "_" + document.getFileName();
                String cloudIdentifier = uploadToCloud(message.getFrom().getUserName(),
                        fileName, document.getMimeType(), downloadedFile
                );
                ProcessingResult processingResult = new ProcessingResult(
                        Optional.of(new SendMessage()
                                .setChatId(chatId)
                                .setText(TextContents.DOCUMENT_UPLOAD_SUCCESS.getText())),
                        Optional.of(() -> {
                            persistentStorage.saveDocumentInfo(
                                    message.getFrom().getUserName(),
                                    document.getFileId(),
                                    cloudIdentifier,
                                    Optional.of(document.getFileName()),
                                    fileName,
                                    Optional.empty()
                            );
                            persistentStorage.updateFlowStatus(message.getFrom().getUserName(), FlowStatus.WAITING_DOCUMENT_RECOGNITION);
                            imageTypeRecognitionJobTrigger.scheduleRecognition();
                        })
                );
                responseFuture.complete(processingResult);
            } catch (Exception e) {
                log.error("Cannot download/upload document {}", document.getFileId(), e);
                responseFuture.complete(new ProcessingResult(Optional.of(new SendMessage()
                        .setChatId(chatId)
                        .setText(TextContents.DOCUMENT_UPLOAD_ERROR.getText())
                ), Optional.empty()));
            }
        });

        return Optional.of(responseFuture);
    }

    private String uploadToCloud(String username, String filename,
                                 String mimeType, File file) throws Exception {
        log.info("Uploading file with name {} to cloud for username {}", filename, username);
        return cloudWrapper.uploadFile(UploadFile.builder()
                .content(file)
                .username(username)
                .name(filename)
                .mimeType(mimeType)
                .build());
    }
}
