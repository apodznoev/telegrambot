package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.FlowStatus;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.ProcessingResult;
import de.avpod.telegrambot.TextContents;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Log4j2
@AllArgsConstructor
public class ManualFinishTextUpdateProcessor implements TextUpdateProcessor {
    private final PersistentStorageWrapper persistentStorage;

    @Override
    public boolean isResponsible(String messageText) {
        return messageText.equals(TextContents.ANSWER_NO_ALL_DOCUMENTS_ARE_THERE.getText()) ||
                messageText.equals(TextContents.ANSWER_YES_DOCUMENT_WILL_BE_SUBMITTED_YET.getText());
    }

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processTextMessage(String messageText, Message message) {
        log.info("Processing response {} about force finish flow from username: {}",
                messageText,
                message.getFrom().getUserName()
        );

        CompletableFuture<ProcessingResult> completableFuture = new CompletableFuture<>();

        if (messageText.equals(TextContents.ANSWER_NO_ALL_DOCUMENTS_ARE_THERE.getText())) {
            completableFuture.complete(new ProcessingResult(
                    Optional.empty(),
                    Optional.of(() -> {
                        persistentStorage.updateFlowStatus(message.getFrom().getUserName(),
                                FlowStatus.FINISHED
                        );
                        return Collections.singletonList(
                                new SendMessage()
                                        .setChatId(message.getChatId())
                                        .setText(TextContents.ALL_DOCUMENTS_RECEIVED.getText())
                        );
                    })
            ));

        } else {
            completableFuture.complete(new ProcessingResult(
                    Optional.empty(), Optional.empty()
            ));
        }

        return Optional.of(completableFuture);
    }
}