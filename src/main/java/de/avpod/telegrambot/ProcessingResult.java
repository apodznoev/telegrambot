package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.glassfish.jersey.internal.util.Producer;
import org.telegram.telegrambots.api.methods.send.SendMessage;

import java.util.Optional;

@Getter
@AllArgsConstructor
public class ProcessingResult {
    private final Optional<SendMessage> messageAcceptedResponse;
    private final Optional<Producer<Optional<SendMessage>>> stateUpdate;
}
