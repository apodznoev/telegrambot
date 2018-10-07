package de.avpod.telegrambot;

import de.avpod.telegrambot.telegram.MessageProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
@AllArgsConstructor
public class AvpodBot extends TelegramLongPollingBot {

    private final String token;
    private final List<MessageProcessor> messageProcessors;
    private final Executor sendResponseExecutor;

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            log.info("Got update without message {}", update);
            return;
        }

        //todo save to dynamodb status of the document to be processed, on process complete update the status of the document
        //todo response to user "successfully loaded/captured erroed with name"
        //todo create some trigger on last document load to ask for recognition of the image (load thumbnail by id from dynamodb from telegram + file name from dynamodb)
        //todo trigger should be initiated on bot start because of restarts
        //todo just start the thread which queries users with status (not completed, recognition not started) and initiate dialog
        //todo should stop if no job find. Should start on any image processed.
        //todo during recognition prevent sending other messages to user to avoid dissapearing of the keyboard (?)
        //todo on image recognition move/delete it accordinaly in google drive
        //todo check in dynamodb if some images are still not recognised and repeat steps
        //todo check the list of images and show status

        for (MessageProcessor processor : messageProcessors) {
            Optional<CompletableFuture> handled = processor.processMessage(update.getMessage())
                    .map((future) -> future.thenAcceptAsync((responseMessage) -> {
                        try {
                            log.info("Sending processing result to user {}", update.getMessage().getFrom().getUserName());
                            Message msg = AvpodBot.this.execute(responseMessage);
                            log.info("Response successfully submitted", msg);
                        } catch (TelegramApiException e) {
                            log.error("Unexpected api exception", e);
                        }
                    }, sendResponseExecutor));

            if (handled.isPresent())
                break;
        }
    }

    @Override
    public String getBotUsername() {
        return "AVPod-Bot";
    }

    @Override
    public String getBotToken() {
        return token;
    }
}