package de.avpod.telegrambot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MessageProcessor {

    Optional<CompletableFuture<SendMessage>> processMessage(Message message);

}
