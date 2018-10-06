package de.avpod.telegrambot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Value;
import org.telegram.telegrambots.api.interfaces.BotApiObject;

import java.io.Serializable;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelegramRestResponse<T extends BotApiObject> implements Serializable {
    private final boolean ok;
    private final T result;

}
