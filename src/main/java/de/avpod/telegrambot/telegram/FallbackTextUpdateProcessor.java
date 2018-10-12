package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class FallbackTextUpdateProcessor implements TextUpdateProcessor {

    @Override
    public boolean isResponsible(String messageText) {
        return true;
    }

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processTextMessage(String messageText, Message message) {
        log.info("Processing text message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );

        return Optional.empty();
    }
}
