package de.avpod.telegrambot;

import com.google.common.io.Files;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Log4j2
@AllArgsConstructor
public class AvpodBot extends TelegramLongPollingBot {

    private final String token;
    private final CloudWrapper cloudWrapper;
    private final RestTemplate restTemplate;

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (update.hasMessage()) {
            log.info("Processing message update from username: {} with id: {} sent at: {}",
                    message.getFrom().getUserName(), message.getFrom().getId(), new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
            );
            List<SendMessage> textResponseMessages = processTextContent(
                    Optional.ofNullable(message.getText()), message.getFrom(), message.getChat()
            );
            List<SendMessage> imagesResponseMessages = processImageContent(
                    Optional.ofNullable(message.getPhoto()), message.getFrom(), message.getChat()
            );
            List<SendMessage> documentsResponseMessages = processDocumentContent(
                    Optional.ofNullable(message.getDocument()), message.getFrom(), message.getChat()
            );

            Stream.concat(textResponseMessages.stream(),
                    Stream.concat(imagesResponseMessages.stream(), documentsResponseMessages.stream())).forEach((responseMessage -> {
                try {
                    execute(responseMessage); // Sending our message object to user
                } catch (TelegramApiException e) {
                    log.error("Unexpected api exception", e);
                }
            }));
        }
    }

    private List<SendMessage> processTextContent(Optional<String> textOpt, User from, Chat chat) {
        return textOpt.map(text -> Collections.singletonList(new SendMessage() // Create a message object object
                .setChatId(chat.getId())
                .setText(
                        String.format("Hello, this is echo %s and some random content from my bot %f", text, Math.random())
                ))).orElse(Collections.emptyList());
    }

    private List<SendMessage> processImageContent(Optional<List<PhotoSize>> photo, User from, Chat chat) {
        Optional<PhotoSize> highResPhoto = photo.flatMap(photos ->
                photos.stream().max(Comparator.comparingInt(PhotoSize::getFileSize))
        );
        return highResPhoto.map((biggestImageInfo) -> {
            try {
                long chatId = chat.getId();
                File downloadedFile = downloadFileFromTelegramServer(biggestImageInfo.getFileId());
                String fileName = from.getUserName() + "_" + downloadedFile.getPath();
                String mimeType = URLConnection.guessContentTypeFromName(downloadedFile.getName());
                String cloudIdentifier = uploadToCloud(from.getUserName(), fileName, mimeType, downloadedFile);
                return Collections.singletonList(new SendMessage()
                        .setChatId(chatId)
                        .setText(String.format("Image was successfully uploaded with identifier: %s", cloudIdentifier)));
            } catch (Exception e) {
                log.error("Cannot upload image for:" + biggestImageInfo.getFilePath(), e);
                return errorResponse(chat.getId());
            }
        }).orElse(Collections.emptyList());
    }

    private List<SendMessage> processDocumentContent(Optional<Document> documentOpt, User from, Chat chat) {
        return documentOpt.map((document) -> {
            try {
                long chatId = chat.getId();
                File downloadedFile = downloadFileFromTelegramServer(document.getFileId());
                String fileName = from.getUserName() + "_" + document.getFileName();
                String cloudIdentifier = uploadToCloud(from.getUserName(), fileName, document.getMimeType(), downloadedFile);
                return Collections.singletonList(new SendMessage()
                        .setChatId(chatId)
                        .setText(String.format("Document was successfully uploaded with md5: %s", cloudIdentifier)));
            } catch (Exception e) {
                log.error("Cannot download document {}", document.getFileId(), e);
                return errorResponse(chat.getId());
            }
        }).orElse(Collections.emptyList());
    }

    private List<SendMessage> errorResponse(Long chatId) {
        return Collections.singletonList(new SendMessage()
                .setChatId(chatId)
                .setText(String.format("Cannot upload document due to some error")));
    }

    private String uploadToCloud(String username, String filename,
                                 String mimeType, File file) throws Exception {
        log.info("Uploading file with name {} to cloud", filename);
        return cloudWrapper.uploadFile(UploadFile.builder()
                .content(file)
                .username(username)
                .name(filename)
                .mimeType(mimeType)
                .build());
    }

    private java.io.File downloadFileFromTelegramServer(String fileId) throws TelegramApiException, IOException {
        log.info("Downloading file from Telegram Server for path {}", fileId);
        String getFileCall = this.getBaseUrl() + "getFile?file_id=" + fileId;
        ParameterizedTypeReference<TelegramRestResponse<org.telegram.telegrambots.api.objects.File>> responseType = new ParameterizedTypeReference<TelegramRestResponse<org.telegram.telegrambots.api.objects.File>>() {
        };
        ResponseEntity<TelegramRestResponse<org.telegram.telegrambots.api.objects.File>> fileInfoResponse =
                restTemplate.exchange(getFileCall, HttpMethod.GET, null, responseType);
        org.telegram.telegrambots.api.objects.File telegramFile = fileInfoResponse.getBody().getResult();
        String fileExtension = tryGetExtension(telegramFile);
        java.io.File file = this.downloadFile(telegramFile.getFilePath());
        log.info("Downloaded file with size {} and name {}", file.getTotalSpace(), file.getName());
        String filename = file.getName().replaceAll("tmp",fileExtension);
        java.io.File renamed = new File(filename);
        Files.copy(file, renamed);
        return renamed;
    }

    private String tryGetExtension(org.telegram.telegrambots.api.objects.File telegramFile) {
        try {
            return telegramFile.getFilePath().split("\\.")[1];
        } catch (Exception e) {
            log.warn("Cannot get extension from path {}", telegramFile.getFilePath());
            return "unknown";
        }

    }

    @Override
    public String getBotUsername() {
        return "AVPod-Bot";
    }

    @Override
    public String getBotToken() {
        return token;
    }
}