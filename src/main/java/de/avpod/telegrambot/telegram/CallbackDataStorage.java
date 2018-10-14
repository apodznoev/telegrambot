package de.avpod.telegrambot.telegram;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import de.avpod.telegrambot.aws.TelegramInlineCallbackData;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@AllArgsConstructor
@Log4j2
public class CallbackDataStorage {
    private final DynamoDBMapper mapper;

    public void persistCallbacks(List<TelegramInlineCallbackData> callbacks) {
        log.info("Persisting callbacks count: {}", callbacks.size());
        mapper.batchSave(callbacks);
        log.info("Callbacks persisted");
    }

    public TelegramInlineCallbackData loadCallbackInfo(String id) {
        log.info("Loading callback data for id {}", id);
        return mapper.load(TelegramInlineCallbackData.class, id );
    }
}
