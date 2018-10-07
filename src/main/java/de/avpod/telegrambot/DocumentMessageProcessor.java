package de.avpod.telegrambot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DocumentMessageProcessor extends MessageProcessor {

    @Override
    default Optional<CompletableFuture<SendMessage>> processMessage(Message message) {
        if (!message.hasDocument())
            return Optional.empty();
        return processDocumentMessage(message.getDocument(), message);
    }

    Optional<CompletableFuture<SendMessage>> processDocumentMessage(Document document, Message message);
}
