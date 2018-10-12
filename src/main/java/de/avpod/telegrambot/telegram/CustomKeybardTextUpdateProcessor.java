package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.ProcessingResult;
import de.avpod.telegrambot.TextContents;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class CustomKeybardTextUpdateProcessor implements TextUpdateProcessor {

    @Override
    public boolean isResponsible(String messageText) {
        return messageText.startsWith("/keyboard");
    }

    @Override
    public Optional<CompletableFuture<ProcessingResult>> processTextMessage(String messageText, Message message) {
        log.info("Processing text message from username: {} with id: {} sent at: {}",
                message.getFrom().getUserName(), message.getFrom().getId(),
                new Date(TimeUnit.SECONDS.toMillis(message.getDate()))
        );

        CompletableFuture<ProcessingResult> responseFuture = new CompletableFuture<>();

        SendMessage response;
        if (messageText.contains("reply")) {
            KeyboardRow row1 = new KeyboardRow();
            row1.add("Some text 1");
            KeyboardRow row2 = new KeyboardRow();
            row2.add("Some text 2");
            KeyboardRow row3 = new KeyboardRow();
            row3.add("Some text 31");
            row3.add("Some text 32");
            response = new SendMessage()
                    .setChatId(message.getChat().getId())
                    .setReplyMarkup(new ReplyKeyboardMarkup()
                            .setResizeKeyboard(true)
                            .setOneTimeKeyboard(true)
                            .setKeyboard(Arrays.asList(
                                    row1, row2, row3
                            ))
                    )
                    .setText(TextContents.RECOGNISE_IMAGE_TEXT.getText());
        } else if (messageText.contains("force")) {
            response = new SendMessage()
                    .setChatId(message.getChat().getId())
                    .setReplyMarkup(new ForceReplyKeyboard())
                    .setText(TextContents.RECOGNISE_IMAGE_TEXT.getText());
        } else if (messageText.contains("inline")) {
            response = new SendMessage()
                    .setChatId(message.getChat().getId())
                    .setReplyMarkup(new InlineKeyboardMarkup()
                            .setKeyboard(Arrays.asList(
                                    Arrays.asList(
                                            new InlineKeyboardButton("Document type INN").setCallbackData("inn"),
                                            new InlineKeyboardButton("Column 2").setCallbackData("column2")
                                    ),
                                    Arrays.asList(
                                            new InlineKeyboardButton("Document type Passport").setCallbackData("passport")
                                    ),
                                    Arrays.asList(
                                            new InlineKeyboardButton("Document type photo").setCallbackData("photo")
                                    )
                            )))
                    .setText(TextContents.RECOGNISE_IMAGE_TEXT.getText());
        } else {
            response = new SendMessage()
                    .setChatId(message.getChat().getId())
                    .setReplyMarkup(new ReplyKeyboardRemove())
                    .setText(TextContents.RECOGNISE_IMAGE_TEXT.getText());
        }

        responseFuture.complete(new ProcessingResult(Optional.of(response), Optional.empty()));
        return Optional.of(responseFuture);
    }
}
