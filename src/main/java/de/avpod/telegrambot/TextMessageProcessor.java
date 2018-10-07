package de.avpod.telegrambot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TextMessageProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<SendMessage>> processMessage(Message message) {
        if (!message.hasText())
            return Optional.empty();
        return processTextMessage(message.getText(), message);
    }

    Optional<CompletableFuture<SendMessage>> processTextMessage(String messageText, Message message);
}
