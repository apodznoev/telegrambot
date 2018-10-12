package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.api.methods.send.SendMessage;

import java.util.Optional;

@AllArgsConstructor
@Getter
public class ProcessingResult {
    private final Optional<SendMessage> responseMessage;
    private final Optional<Runnable> stateUpdate;
}
