package de.avpod.telegrambot.telegram;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ImageMessageProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<SendMessage>> processMessage(Message message) {
        if (!message.hasPhoto())
            return Optional.empty();
        return processImageMessage(message.getPhoto(), message);
    }

    Optional<CompletableFuture<SendMessage>> processImageMessage(List<PhotoSize> document, Message message);
}
