package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.*;
import de.avpod.telegrambot.aws.TelegramInlineCallbackData;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@AllArgsConstructor
@Log4j2
public class CallbackUpdateProcessor implements UpdateProcessor {
    private final CloudWrapper cloudWrapper;
    private final PersistentStorageWrapper persistentStorage;
    private final CallbackDataStorage callbackDataStorage;
    private final Executor handlerExecutor;

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processUpdate(Update update) {
        if (!update.hasCallbackQuery())
            return Optional.empty();

        CallbackQuery callback = update.getCallbackQuery();
        log.info("Processing callback query {}", callback.getData());
        TelegramInlineCallbackData callbackData = callbackDataStorage.loadCallbackInfo(callback.getData());
        if (callbackData == null) {
            log.warn("Cannot deserialize callback {}", callback);
            return Optional.empty();
        }
        CompletableFuture<ProcessingResult> responseFuture = new CompletableFuture<>();
        long chatId = callback.getMessage().getChatId();
        handlerExecutor.execute(() -> {
            ProcessingResult result = new ProcessingResult(
                    Optional.empty(),
                    Optional.of(() -> {
                        FlowStatus newFlowStatus;
                        if (callbackData.isDelete()) {
                            log.info("Requested deleting of document with id {}", callbackData.getDocumentId());
                            cloudWrapper.deleteDocument(callbackData.getCloudId());
                            newFlowStatus = persistentStorage.deleteDocument(
                                    callback.getFrom().getUserName(),
                                    callbackData.getDocumentId()
                            );
                        } else {
                            log.info("Processing recognized document type {}", callbackData.getDocumentType());
                            cloudWrapper.recognizeDocument(callbackData.getCloudId(), callbackData.getDocumentType());
                            newFlowStatus = persistentStorage.updateDocumentType(
                                    callback.getFrom().getUserName(),
                                    callbackData.getDocumentId(),
                                    callbackData.getDocumentType()
                            );
                        }

                        Integer messageToDelete = callback.getMessage().getMessageId();
                        DeleteMessage deleteAnsweredMessageResponse =
                                new DeleteMessage(chatId, messageToDelete);

                        if (newFlowStatus == FlowStatus.FINISHED) {
                            return Arrays.asList(
                                    deleteAnsweredMessageResponse,
                                    new SendMessage()
                                            .setChatId(chatId)
                                            .setText(TextContents.ALL_DOCUMENTS_RECEIVED.getText())
                            );
                        }

                        if (newFlowStatus == FlowStatus.MANDATORY_DOCUMENTS_SUBMITTED) {
                            KeyboardRow row1 = new KeyboardRow();
                            row1.add(TextContents.ANSWER_YES_DOCUMENT_WILL_BE_SUBMITTED_YET.getText());
                            KeyboardRow row2 = new KeyboardRow();
                            row2.add(TextContents.ANSWER_NO_ALL_DOCUMENTS_ARE_THERE.getText());
                            return Arrays.asList(
                                    deleteAnsweredMessageResponse,
                                    new SendMessage()
                                            .setChatId(chatId)
                                            .setReplyMarkup(new ReplyKeyboardMarkup()
                                                    .setResizeKeyboard(true)
                                                    .setOneTimeKeyboard(true)
                                                    .setKeyboard(Arrays.asList(
                                                            row1, row2
                                                    ))
                                            )
                                            .setText(TextContents.ARE_YOU_FINISHED.getText())
                            );
                        }

                        return Collections.singletonList( deleteAnsweredMessageResponse);
                    })
            );
            responseFuture.complete(result);
        });


        return Optional.of(responseFuture);
    }
}
