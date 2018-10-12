package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.CloudWrapper;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.ProcessingResult;
import de.avpod.telegrambot.RecognizeDocumentCallbackData;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
@Log4j2
public class CallbackUpdateProcessor implements UpdateProcessor {
    private final CloudWrapper cloudWrapper;
    private final PersistentStorageWrapper persistentStorage;

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processUpdate(Update update) {
        if (!update.hasCallbackQuery())
            return Optional.empty();

        CallbackQuery callback = update.getCallbackQuery();
        log.info("Processing callback query {}", callback.getData());
        RecognizeDocumentCallbackData callbackData = RecognizeDocumentCallbackData.deserialize(callback.getData());
        if (callbackData == null) {
            log.warn("Cannot deserialize callback {}", callback);
            return Optional.empty();
        }
        CompletableFuture<ProcessingResult> responseFuture = new CompletableFuture<>();

        responseFuture.complete(new ProcessingResult(
                Optional.empty(),
                Optional.of(() -> {
                    boolean finished;
                    if (callbackData.isDeleteRequested()) {
                        log.info("Requested deleting of document with id {}", callbackData.getDocumentId());
                        cloudWrapper.deleteDocument(callbackData.getCloudDocumentId());
                        finished = persistentStorage.deleteDocument(
                                update.getCallbackQuery().getFrom().getUserName(),
                                callbackData.getDocumentId()
                        );
                    } else {
                        log.info("Processing recognized document type {}", callbackData.getDocumentType());
                        cloudWrapper.recognizeDocument(callbackData.getCloudDocumentId(), callbackData.getDocumentType());
                        finished = persistentStorage.updateDocumentType(
                                update.getCallbackQuery().getFrom().getUserName(),
                                callbackData.getDocumentId(),
                                callbackData.getDocumentType()
                        );
                    }
                    if (finished) {
                        //todo trigger notify job
                        //todo create job to check if some documents are still missing and gently ask for confirmation
                        //todo if nothing more to submit
                    }

                })
        ));


        return Optional.of(responseFuture);
    }
}
