package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UpdateProcessor {

    Optional<CompletableFuture<ProcessingResult>> processUpdate(Update update);

}
