package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MessageProcessor extends UpdateProcessor {

    default Optional<CompletableFuture<ProcessingResult>> processUpdate(Update update){
        if(!update.hasMessage())
            return Optional.empty();
        return processMessage(update.getMessage());
    }

    Optional<CompletableFuture<ProcessingResult>> processMessage(Message message);

}
