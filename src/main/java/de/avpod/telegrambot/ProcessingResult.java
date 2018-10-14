package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;

import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class ProcessingResult {
    private final Optional<SendMessage> messageAcceptedResponse;
    private final Optional<ThrowingSupplier<List<BotApiMethod>>> stateUpdate;
}
