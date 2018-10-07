package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;

import java.io.File;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Log4j2
@AllArgsConstructor
public class UploadImageMessageProcessor implements ImageMessageProcessor {
    private final TelegramFilesLoader filesLoader;
    private final CloudWrapper cloudWrapper;
    private Executor handlerExecutor;

    @Override
    public Optional<CompletableFuture<SendMessage>> processImageMessage(List<PhotoSize> thumbnails, Message message) {
        log.info("Processing image message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );
        CompletableFuture<SendMessage> responseFuture = new CompletableFuture<>();

        handlerExecutor.execute(() -> {
            long chatId = message.getChat().getId();
            thumbnails.sort(Comparator.comparingInt(PhotoSize::getFileSize).reversed());
            PhotoSize biggestImageInfo = thumbnails.get(0);
            try {
                File downloadedFile = filesLoader.downloadFile(biggestImageInfo.getFileId());
                String fileName = message.getFrom().getUserName() + "_" + downloadedFile.getPath();
                String mimeType = URLConnection.guessContentTypeFromName(downloadedFile.getName());
                String cloudIdentifier = uploadToCloud(
                        message.getFrom().getUserName(), fileName, mimeType, downloadedFile
                );
                responseFuture.complete(new SendMessage()
                        .setChatId(chatId)
                        .setText(TextContents.DOCUMENT_UPLOAD_SUCCESS.getText())
                );
            } catch (Exception e) {
                log.error("Cannot download/upload image {}", biggestImageInfo.getFileId(), e);
                responseFuture.complete(new SendMessage()
                        .setChatId(chatId)
                        .setText(TextContents.DOCUMENT_UPLOAD_ERROR.getText())
                );
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
