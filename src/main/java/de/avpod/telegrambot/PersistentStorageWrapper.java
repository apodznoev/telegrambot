package de.avpod.telegrambot;

import de.avpod.telegrambot.aws.UserInfo;
import de.avpod.telegrambot.telegram.UnrecognizedDocumentInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersistentStorageWrapper {

    void updateFlowStatus(String userName, FlowStatus flowStatus);

    FlowStatus getFlowStatus(String userName);

    String saveDocumentInfo(String userName,
                          String telegramFileId,
                          String cloudIdentifier,
                          Optional<String> originalFileName,
                          String cloudFileName,
                          Optional<String> telegramThumbnailId);

    void insertUser(String userName, String firstName, String lastName, Long chatId, FlowStatus flowStatus);

    Collection<UserInfo> queryUsersForImageRecognition();

    List<UnrecognizedDocumentInfo> queryUnrecognizedDocuments(String username);

    void markDocumentAsNotifiedForRecognition(String username, String documentId);

    UserInfo getFullInfo(String userName);
}
