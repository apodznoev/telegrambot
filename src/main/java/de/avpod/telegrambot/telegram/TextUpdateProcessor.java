package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TextUpdateProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<ProcessingResult>> processMessage(Message message) {
        if (!message.hasText())
            return Optional.empty();

        if (!isResponsible(message.getText()))
            return Optional.empty();

        return processTextMessage(message.getText(), message);
    }

    boolean isResponsible(String messageText);

    Optional<CompletableFuture<ProcessingResult>> processTextMessage(String messageText, Message message);
}
