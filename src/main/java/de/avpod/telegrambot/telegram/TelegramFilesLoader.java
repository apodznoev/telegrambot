package de.avpod.telegrambot.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.api.objects.File;

import java.io.IOException;
import java.net.URL;

@Log4j2
@AllArgsConstructor
public class TelegramFilesLoader {
    private final String botToken;
    private final String telegramApiBaseUrl;
    private final RestTemplate restTemplate;

    public java.io.File downloadFile(String fileId) throws IOException {
        log.info("Downloading file from Telegram Server for path {}", fileId);

        String getFileCall = telegramApiBaseUrl + "getFile?file_id=" + fileId;
        ParameterizedTypeReference<TelegramRestResponse<File>> responseType =
                new ParameterizedTypeReference<TelegramRestResponse<File>>() {
                };
        ResponseEntity<TelegramRestResponse<File>> fileInfoResponse =
                restTemplate.exchange(getFileCall, HttpMethod.GET, null, responseType);

        if (!fileInfoResponse.hasBody() || !fileInfoResponse.getBody().isOk()) {
            log.warn("Cannot find file on Telegram servers with id: {}, response {}", fileId, fileInfoResponse);
            throw new IOException("Cannot get file info from the path:" + getFileCall);
        }
        log.info("Loaded file path info {}, loading content", fileInfoResponse.getBody().getResult());

        File telegramFile = fileInfoResponse.getBody().getResult();
        String fileExtension = tryGetExtension(telegramFile);
        String url = File.getFileUrl(botToken, telegramFile.getFilePath());
        java.io.File downloadedContent = new java.io.File(telegramFile.getFileId() + "." + fileExtension);
        FileUtils.copyURLToFile(new URL(url), downloadedContent);

        log.info("Downloaded file content with size {} and name {}",
                downloadedContent.getTotalSpace(), downloadedContent.getName());

        return downloadedContent;
    }

    private String tryGetExtension(File telegramFile) {
        try {
            return telegramFile.getFilePath().split("\\.")[1];
        } catch (Exception e) {
            log.warn("Cannot get extension from path {}", telegramFile.getFilePath());
            return "unknown";
        }

    }
}
