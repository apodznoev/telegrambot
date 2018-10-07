package de.avpod.telegrambot;

import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class FallbackTextMessageProcessor implements TextMessageProcessor {

    @Override
    public Optional<CompletableFuture<SendMessage>> processTextMessage(String messageText, Message message) {
        log.info("Processing text message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );
        CompletableFuture<SendMessage> responseFuture = new CompletableFuture<>();

        SendMessage response = new SendMessage()
                .setChatId(message.getChat().getId())
                .setText(TextContents.GREETINGS_TEXT.getText());
        responseFuture.complete(response);
        return Optional.of(responseFuture);
    }
}
