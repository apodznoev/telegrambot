package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DocumentUpdateProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<ProcessingResult>> processMessage(Message message) {
        if (!message.hasDocument())
            return Optional.empty();
        return processDocumentMessage(message.getDocument(), message);
    }

    Optional<CompletableFuture<ProcessingResult>> processDocumentMessage(Document document, Message message);
}
