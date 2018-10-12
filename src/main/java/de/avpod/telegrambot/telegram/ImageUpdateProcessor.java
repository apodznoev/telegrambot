package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ImageUpdateProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<ProcessingResult>> processMessage(Message message) {
        if (!message.hasPhoto())
            return Optional.empty();
        return processImageMessage(message.getPhoto(), message);
    }

    Optional<CompletableFuture<ProcessingResult>> processImageMessage(List<PhotoSize> document, Message message);
}
