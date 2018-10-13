package de.avpod.telegrambot;

import de.avpod.telegrambot.telegram.UpdateProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Log4j2
@AllArgsConstructor
public class AvpodBot extends TelegramLongPollingBot {

    private final String token;
    private final List<UpdateProcessor> updateProcessors;
    private final UserAwareExecutor sendResponseExecutor;
    private final PersistentStorageWrapper persistentStorage;

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() && !update.hasCallbackQuery()) {
            log.info("Got update without message or query {}", update);
            return;
        }

        User user = update.hasCallbackQuery() ?
                update.getCallbackQuery().getFrom() :
                update.getMessage().getFrom();

        sendResponseExecutor.getExecutor(user.getUserName()).execute(() -> {
            FlowStatus flowStatus = determineFlowStatus(user.getUserName());
            log.info("Got flow status {} for user {}", flowStatus, user.getUserName());

            if (flowStatus == FlowStatus.FINISHED) {
                log.info("Got update from user {} with finished flow {}, ignoring", user.getUserName(), update);
                sendResponseExecutor.cleanupForUser(user.getUserName());
                return;
            }

            if (flowStatus == FlowStatus.NEW) {
                try {
                    AvpodBot.this.execute(new SendMessage()
                            .setChatId(update.getMessage().getChatId())
                            .setText(TextContents.GREETINGS_TEXT.getText())
                    );
                    initNewUser(user, update.getMessage().getChatId(), FlowStatus.WAITING_FILES);
                } catch (Exception e) {
                    log.error("Unexpected exception during submit response", e);
                }
                return;
            }


            //todo single-thread executor per user
            //todo save to dynamodb status of the document to be processed, on process complete update the status of the document
            //todo response to user "successfully loaded/captured erroed with name"
            //todo create some scheduleRecognition on last document load to ask for recognition of the image (load thumbnail by id from dynamodb from telegram + file name from dynamodb)
            //todo scheduleRecognition should be initiated on bot start because of restarts
            //todo just start the thread which queries users with status (not completed, recognition not started) and initiate dialog
            //todo should stop if no job find. Should start on any image processed.
            //todo during recognition prevent sending other messages to user to avoid dissapearing of the keyboard (?)
            //todo on image recognition move/delete it accordinaly in google drive
            //todo check in dynamodb if some images are still not recognised and repeat steps
            //todo check the list of images and show status

            for (UpdateProcessor processor : updateProcessors) {
                Optional<CompletableFuture<ProcessingResult>> handled = processor.processUpdate(update);
                handled.map((future) -> future.thenAcceptAsync((processingResult) -> {
                    if (processingResult == null)
                        return;

                    if (processingResult.getMessageAcceptedResponse().isPresent()) {
                        try {
                            log.info("Sending processing result to user {}", update.getMessage().getFrom().getUserName());
                            Message msg = AvpodBot.this.execute(processingResult.getMessageAcceptedResponse().get());
                            log.info("Response successfully submitted", msg);
                        } catch (TelegramApiException e) {
                            log.error("Unexpected api exception", e);
                            return;
                        }
                    }

                    if (processingResult.getStateUpdate().isPresent()) {
                        try {
                            log.info("Processing state update for user {}", update.getMessage().getFrom().getUserName());
                            Optional<SendMessage> reactionMessage = processingResult.getStateUpdate().get().call();
                            reactionMessage.ifPresent(method -> {
                                try {
                                    AvpodBot.this.execute(method);
                                } catch (TelegramApiException e) {
                                    log.error("Error during sending message", e);
                                }
                            });
                            log.info("State update successfully finished");
                        } catch (Exception e) {
                            log.error("Error during processing state update", e);
                        }
                    }

                }, sendResponseExecutor.getExecutor(user.getUserName())));

                if (handled.isPresent())
                    break;
            }
        });
    }

    private void initNewUser(User user, Long chatId, FlowStatus flowStatus) {
        persistentStorage.insertUser(user.getUserName(), user.getFirstName(), user.getLastName(), chatId, flowStatus);
    }

    private FlowStatus determineFlowStatus(String userName) {
        return persistentStorage.getFlowStatus(userName);
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