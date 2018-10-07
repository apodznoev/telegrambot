package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Message;

import java.io.File;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
@AllArgsConstructor
public class UploadDocumentMessageProcessor implements DocumentMessageProcessor {
    private final TelegramFilesLoader filesLoader;
    private final CloudWrapper cloudWrapper;

    @Override
    public Optional<CompletableFuture<SendMessage>> processDocumentMessage(Document document, Message message) {
        log.info("Processing document message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );
        long chatId = message.getChat().getId();

        CompletableFuture<SendMessage> responseFuture = new CompletableFuture<>();
        try {
            File downloadedFile = filesLoader.downloadFile(document.getFileId());
            String fileName = message.getFrom().getUserName() + "_" + document.getFileName();
            String cloudIdentifier = uploadToCloud(message.getFrom().getUserName(),
                    fileName, document.getMimeType(), downloadedFile
            );
            responseFuture.complete(new SendMessage()
                    .setChatId(chatId)
                    .setText(TextContents.DOCUMENT_UPLOAD_SUCCESS.getText())
            );
        } catch (Exception e) {
            log.error("Cannot download/upload document {}", document.getFileId(), e);
            responseFuture.complete(new SendMessage()
                    .setChatId(chatId)
                    .setText(TextContents.DOCUMENT_UPLOAD_ERROR.getText())
            );
        }


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
