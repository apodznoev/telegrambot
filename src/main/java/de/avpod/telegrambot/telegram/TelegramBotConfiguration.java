package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.AvpodBot;
import de.avpod.telegrambot.CloudWrapper;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.UserAwareExecutor;
import de.avpod.telegrambot.aws.DynamoDBConfguration;
import de.avpod.telegrambot.google.GoogleDriveConfiguration;
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
    BotSession avpodTelegramBot(CloudWrapper cloudWrapper,
                                PersistentStorageWrapper persistentStorageWrapper) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        // Register our bot
        return botsApi.registerBot(telegramBot(cloudWrapper, persistentStorageWrapper));
    }

    @Bean
    AvpodBot telegramBot(CloudWrapper cloudWrapper, PersistentStorageWrapper persistentStorageWrapper) {
        ImageTypeRecognitionJobTrigger imageTypeRecognitionJobTrigger = imageTypeRecognitionJobTrigger();
        return new AvpodBot(
                token,
                messageProcessors(
                        new TelegramFilesLoader(token, ApiConstants.BASE_URL + token + "/",
                                restTemplate()
                        ),
                        cloudWrapper,
                        handlerExecutor(),
                        persistentStorageWrapper,
                        imageTypeRecognitionJobTrigger
                ),
                userAwareResponseExecutor(),
                persistentStorageWrapper
        );
    }

    @Bean
    ImageTypeRecognitionJobTrigger imageTypeRecognitionJobTrigger() {
        return new ImageTypeRecognitionJobTrigger();
    }

    @Bean
    ImageTypeRecognitionJob imageTypeRecognitionJob(AvpodBot bot,
                                                    ImageTypeRecognitionJobTrigger recognitionJobTrigger,
                                                    PersistentStorageWrapper persistentStorageWrapper) {
        return new ImageTypeRecognitionJob(bot, recognitionJobTrigger, persistentStorageWrapper);
    }

    private Executor handlerExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .messageConverters(Collections.emptyList())
                .setConnectTimeout(30 * 1000)
                .setReadTimeout(30 * 1000)
                .build();
    }


    private UserAwareExecutor userAwareResponseExecutor() {
        return new UserAwareExecutor(4);
    }

    private List<UpdateProcessor> messageProcessors(TelegramFilesLoader telegramFilesUploader,
                                                    CloudWrapper cloudWrapper,
                                                    Executor handlerExecutor,
                                                    PersistentStorageWrapper persistentStorageWrapper,
                                                    ImageTypeRecognitionJobTrigger imageTypeRecognitionJob) {
        return Arrays.asList(
                new CallbackUpdateProcessor(cloudWrapper, persistentStorageWrapper),
                new CustomKeyboardTextUpdateProcessor(),
                new UploadDocumentUpdateProcessor(telegramFilesUploader, cloudWrapper,
                        handlerExecutor, persistentStorageWrapper, imageTypeRecognitionJob),
                new UploadImageUpdateProcessor(telegramFilesUploader, cloudWrapper,
                        handlerExecutor, persistentStorageWrapper, imageTypeRecognitionJob),
                new ManualFinishTextUpdateProcessor(persistentStorageWrapper),
                new FallbackTextUpdateProcessor()

        );
    }
}
