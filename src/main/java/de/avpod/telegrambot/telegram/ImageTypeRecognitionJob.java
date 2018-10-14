package de.avpod.telegrambot.telegram;

import de.avpod.telegrambot.DocumentType;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.TextContents;
import de.avpod.telegrambot.aws.TelegramInlineCallbackData;
import de.avpod.telegrambot.aws.UserInfo;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.AbsSender;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Log4j2
public class ImageTypeRecognitionJob {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final PersistentStorageWrapper persistentStorage;
    private final CallbackDataStorage callbackDataStorage;
    private final AbsSender bot;

    ImageTypeRecognitionJob(AbsSender bot,
                            ImageTypeRecognitionJobTrigger recognitionJobTrigger,
                            PersistentStorageWrapper persistentStorage,
                            CallbackDataStorage callbackDataStorage) {
        this.bot = bot;
        this.persistentStorage = persistentStorage;
        this.callbackDataStorage = callbackDataStorage;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (!active.get())
                return;

            log.info("Querying users with yet unrecognised images");
            try {
                Collection<UserInfo> userInfos = persistentStorage.queryUsersForImageRecognition();
                log.info("Found {} users with not recognised images", userInfos.size());
                if (userInfos.isEmpty()) {
                    active.set(false);
                } else {
                    active.set(true);
                    for (UserInfo userInfo : userInfos) {
                        processUserWithUnrecognizedImages(userInfo);
                    }

                }
            } catch (Exception e) {
                log.error("Exceptin during processing image recognition job", e);

            }


        }, 60, 30, TimeUnit.SECONDS);
        recognitionJobTrigger.addListener(this::scheduleRecognition);
    }

    private void processUserWithUnrecognizedImages(UserInfo userInfo) {
        String username = userInfo.getUsername();
        try {
            List<UnrecognizedDocumentInfo> unrecognizedDocumentInfoList =
                    userInfo.getDocuments()
                            .stream()
                            .filter((document) -> document.getDocumentType().equals(DocumentType.UNKNOWN.name()))
                            .map((document) -> new UnrecognizedDocumentInfo(
                                    document.getId(), userInfo.getChatId(), document.getCloudIdentifier(),
                                    document.getTelegramFileId(), Optional.ofNullable(document.getTelegramThumbnailId()),
                                    Optional.ofNullable(document.getOriginalFilename())
                            )).collect(Collectors.toList());
            log.info("Got {} images with unrecognized types for user {}", unrecognizedDocumentInfoList.size(), username);
            Map<UnrecognizedDocumentInfo, List<TelegramInlineCallbackData>> callbacks =
                    prepareCallbacks(unrecognizedDocumentInfoList);

            for (Map.Entry<UnrecognizedDocumentInfo, List<TelegramInlineCallbackData>> entry : callbacks.entrySet()) {
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                for (TelegramInlineCallbackData callbackData : entry.getValue()) {
                    buttons.add(Collections.singletonList(
                            new InlineKeyboardButton(callbackData.getText())
                                    .setCallbackData(callbackData.getId())
                            )
                    );
                }

                UnrecognizedDocumentInfo unrecognizedDocumentInfo = entry.getKey();
                if (unrecognizedDocumentInfo.getTelegramThumbnailId().isPresent()) {
                    bot.sendPhoto(new SendPhoto()
                            .setChatId(unrecognizedDocumentInfo.getChatId())
                            .setReplyMarkup(new InlineKeyboardMarkup()
                                    .setKeyboard(buttons)
                            )
                            .setPhoto(unrecognizedDocumentInfo.getTelegramThumbnailId().get())
                            .setCaption(TextContents.RECOGNISE_IMAGE_TEXT.getText())
                    );


                } else {
                    bot.sendDocument(new SendDocument()
                            .setChatId(unrecognizedDocumentInfo.getChatId())
                            .setReplyMarkup(new InlineKeyboardMarkup()
                                    .setKeyboard(buttons)
                            )
                            .setDocument(unrecognizedDocumentInfo.getTelegramFileId())
                            .setCaption(TextContents.RECOGNISE_DOCUMENT_TEXT.getText() +
                                    ":" +
                                    unrecognizedDocumentInfo.getOriginalFileName().orElse(""))
                    );
                }
                persistentStorage.markDocumentAsNotifiedForRecognition(username, unrecognizedDocumentInfo.getId());
            }
        } catch (Exception e) {
            log.error("Got exception during processing images for user {}", username, e);
        }
    }

    private Map<UnrecognizedDocumentInfo, List<TelegramInlineCallbackData>> prepareCallbacks(
            List<UnrecognizedDocumentInfo> unrecognizedDocumentInfoList) {
        Map<UnrecognizedDocumentInfo, List<TelegramInlineCallbackData>> callbacksPerDocument =
                new HashMap<>();

        for (UnrecognizedDocumentInfo documentInfo : unrecognizedDocumentInfoList) {
            List<TelegramInlineCallbackData> callbacks = new ArrayList<>();
            for (DocumentType documentType : DocumentType.realDocuments()) {
                String id = UUID.randomUUID().toString();
                callbacks.add(new TelegramInlineCallbackData(
                        id,
                        documentType,
                        documentInfo.getId(),
                        documentInfo.getCloudFileId(),
                        documentType.getText()
                ));
            }
            String id = UUID.randomUUID().toString();
            callbacks.add(new TelegramInlineCallbackData(
                    id,
                    null,
                    documentInfo.getId(),
                    documentInfo.getCloudFileId(),
                    TextContents.UNKNOWN_DOCUMENT_TYPE_ANSWER.getText()
            ));
            callbacksPerDocument.put(documentInfo, callbacks);
        }
        List<TelegramInlineCallbackData> allCallbacks = callbacksPerDocument.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        log.info("Persisting inline callbacks into storage with ids {}",
                allCallbacks.stream().map(TelegramInlineCallbackData::getId));
        callbackDataStorage.persistCallbacks(allCallbacks);
        log.info("Callbacks were persisted");
        return callbacksPerDocument;
    }


    private void scheduleRecognition() {
        log.info("Triggered image recognition job");
        active.set(true);
    }
}
