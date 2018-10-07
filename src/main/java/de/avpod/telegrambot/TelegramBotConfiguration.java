package de.avpod.telegrambot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.ApiConstants;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.generics.BotSession;
import org.telegram.telegrambots.generics.LongPollingBot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@Import({GoogleDriveConfiguration.class, DynamoDBConfguration.class})
public class TelegramBotConfiguration {

    @Value("${telegram.token}")
    private String token;

    @Bean
    BotSession avpodTelegramBot(CloudWrapper cloudWrapper, PersistentStorageWrapper persistentStorageWrapper) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        // Register our bot
        return botsApi.registerBot(telegramBot(cloudWrapper, persistentStorageWrapper));
    }

    @Bean
    LongPollingBot telegramBot(CloudWrapper cloudWrapper, PersistentStorageWrapper persistentStorageWrapper) {
        return new AvpodBot(
                token,
                messageProcessors(
                        new TelegramFilesLoader(token, ApiConstants.BASE_URL + token + "/",
                                restTemplate()
                        ), cloudWrapper
                ),
                responseExecutor()
        );
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .messageConverters(Collections.emptyList())
                .setConnectTimeout(30 * 1000)
                .setReadTimeout(30 * 1000)
                .build();
    }


    private Executor responseExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    private List<MessageProcessor> messageProcessors(TelegramFilesLoader telegramFilesUploader,
                                                     CloudWrapper cloudWrapper) {
        return Arrays.asList(
                new UploadDocumentMessageProcessor(telegramFilesUploader, cloudWrapper),
                new UploadImageMessageProcessor(telegramFilesUploader, cloudWrapper),
                new FallbackTextMessageProcessor()

        );
    }
}
