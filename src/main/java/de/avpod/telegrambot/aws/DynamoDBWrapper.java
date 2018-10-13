package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import de.avpod.telegrambot.DocumentType;
import de.avpod.telegrambot.FlowStatus;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.telegram.UnrecognizedDocumentInfo;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Log4j2
public class DynamoDBWrapper implements PersistentStorageWrapper {
    private final DynamoDBMapper mapper;
    private final DynamoDBMapperConfig mapperConfig;

    @Override
    public FlowStatus getFlowStatus(String userName) {
        log.info("Getting flow status for user {}", userName);
        UserInfo userInfo = mapper.load(UserInfo.class, userName, mapperConfig);

        if (userInfo == null)
            return FlowStatus.NEW;

        return FlowStatus.valueOf(userInfo.getStatus());
    }

    @Override
    public String saveDocumentInfo(String userName,
                                   String telegramFileId,
                                   String cloudIdentifier,
                                   Optional<String> originalFileName,
                                   String cloudFileName,
                                   Optional<String> telegramThumbnailId) {
        String id = UUID.randomUUID().toString();
        log.info("Saving document with cloudId {} and UUID {} for user {}", cloudIdentifier, id, userName);
        UserInfo userInfo = mapper.load(UserInfo.class, userName, mapperConfig);
        if (userInfo == null)
            throw new IllegalStateException("Cannot insert document for non-existing user:" + userName);

        StoredDocument document = StoredDocument.builder()
                .id(id)
                .telegramThumbnailId(telegramThumbnailId.orElse(null))
                .telegramFileId(telegramFileId)
                .cloudIdentifier(cloudIdentifier)
                .documentType(DocumentType.UNKNOWN.name())
                .savedFilename(cloudFileName)
                .build();
        userInfo.getDocuments().add(document);
        log.info("Amount of documents for user: {}", userInfo.getDocuments().size());
        mapper.save(userInfo, new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.APPEND_SET,
                mapperConfig.getConsistentReads(),
                mapperConfig.getTableNameOverride()
        ));
        return id;
    }

    @Override
    public void insertUser(String userName, String firstName, String lastName, Long chatId, FlowStatus flowStatus) {
        log.info("Inserting new user {}", userName);
        UserInfo userInfo = UserInfo.builder()
                .username(userName)
                .firstName(firstName)
                .lastName(lastName)
                .chatId(chatId)
                .status(flowStatus.name())
                .documents(Collections.emptyList())
                .build();
        mapper.save(userInfo, new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES,
                mapperConfig.getConsistentReads(),
                mapperConfig.getTableNameOverride()
        ));
    }

    @Override
    public void updateFlowStatus(String userName, FlowStatus flowStatus) {
        log.info("Updating flow status for user {}", userName);
        UserInfo userInfo = UserInfo.builder().username(userName).status(flowStatus.name()).build();
        mapper.save(userInfo, new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES,
                mapperConfig.getConsistentReads(),
                mapperConfig.getTableNameOverride()
        ));
    }

    @Override
    public Collection<UserInfo> queryUsersForImageRecognition() {
        log.info("Querying users with unrecognized documents");
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":finished_status", new AttributeValue().withS("FINISHED"));
        eav.put(":new_status", new AttributeValue().withS(FlowStatus.NEW.name()));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("user_status <> :finished_status and user_status <> :new_status")
                .withExpressionAttributeValues(eav);

        List<UserInfo> scanResult = mapper.scan(UserInfo.class, scanExpression, mapperConfig);
        log.info("Found {} users with not new and not finished state", scanResult.size());
        return scanResult.stream()
                .filter((userInfo) ->
                        userInfo.getDocuments().stream().anyMatch((document) ->
                                document.getDocumentType().equals(DocumentType.UNKNOWN.name())
                        )).collect(Collectors.toList());
    }

    @Override
    public List<UnrecognizedDocumentInfo> queryUnrecognizedDocuments(String username) {
        log.info("Getting unrecognized documents status for user {}", username);
        UserInfo userInfo = mapper.load(UserInfo.class, username, mapperConfig);
        return userInfo.getDocuments()
                .stream()
                .filter((document) -> document.getDocumentType().equals(DocumentType.UNKNOWN.name()))
                .map((document) ->
                        new UnrecognizedDocumentInfo(
                                document.getId(),
                                userInfo.getChatId(),
                                document.getCloudIdentifier(),
                                document.getTelegramFileId(),
                                Optional.ofNullable(document.getTelegramThumbnailId()),
                                Optional.ofNullable(document.getOriginalFilename())))
                .collect(Collectors.toList());
    }

    @Override
    public void markDocumentAsNotifiedForRecognition(String username, String documentId) {
        log.info("Marking document with id {} as requested for user {}", documentId, username);
        doUpdateDocumentType(username, documentId, DocumentType.UNKNOWN_REQUESTED);
    }

    @Override
    public UserInfo getFullInfo(String userName) {
        log.info("Getting full user info for user {}", userName);
        return mapper.load(UserInfo.class, userName, mapperConfig);
    }

    @Override
    public FlowStatus deleteDocument(String userName, String documentId) {
        log.info("Deleting document {} for user {}", documentId, userName);
        UserInfo userInfo = getFullInfo(userName);
        if (!userInfo.getDocuments().removeIf((document) -> document.getId().equals(documentId))) {
            log.warn("Cannot delete document with id {} not found for user data {}", documentId, userInfo);
            return FlowStatus.valueOf(userInfo.getStatus());
        }
        FlowStatus flowStatus = calculateUserStatus(userInfo);
        userInfo.setStatus(flowStatus.name());
        mapper.save(userInfo, mapperConfig);
        return flowStatus;
    }

    @Override
    public FlowStatus updateDocumentType(String userName, String documentId, DocumentType documentType) {
        return doUpdateDocumentType(userName, documentId, documentType);
    }

    private FlowStatus calculateUserStatus(UserInfo userInfo) {
        log.info("Checking if user {} has finished the flow", userInfo.getUsername());
        boolean allDocumentsFound;
        Map<DocumentType, Boolean> documentsPresent = new HashMap<>();
        for (DocumentType documentType : DocumentType.realDocuments()) {
            documentsPresent.put(documentType, false);
        }
        for (StoredDocument document : userInfo.getDocuments()) {
            documentsPresent.computeIfPresent(DocumentType.valueOf(document.getDocumentType()), (type, found) -> true);
        }
        allDocumentsFound = documentsPresent.values().stream().reduce(true, (previous, next) -> previous && next);

        FlowStatus newStatus;
        if (allDocumentsFound) {
            log.info("All documents found for user {}", userInfo.getUsername());
            newStatus = FlowStatus.FINISHED;
        } else if (userInfo.getDocuments()
                .stream()
                .map(StoredDocument::getDocumentType)
                .map(DocumentType::valueOf)
                .noneMatch((documentType ->
                        documentType == DocumentType.UNKNOWN || documentType == DocumentType.UNKNOWN_REQUESTED))) {
            boolean allMandatorySubmitted = true;
            for (DocumentType documentType : DocumentType.mandatoryDocuments()) {
                allMandatorySubmitted &= documentsPresent.get(documentType);
            }

            if (allMandatorySubmitted)
                newStatus = FlowStatus.MANDATORY_DOCUMENTS_SUBMITTED;
            else
                newStatus = FlowStatus.WAITING_FILES;

            log.info("There is no more unrecognized documents for user {}  updating status to {}",
                    userInfo.getUsername(), newStatus);
        } else {
            newStatus = FlowStatus.WAITING_DOCUMENT_RECOGNITION;
        }
        return newStatus;
    }

    private FlowStatus doUpdateDocumentType(String username, String documentId, DocumentType documentType) {
        log.info("Updating document with id {} with type {} for user {}", documentId, documentType, username);
        UserInfo userInfo = getFullInfo(username);
        userInfo.getDocuments()
                .stream()
                .filter((document) -> document.getId().equals(documentId))
                .forEach((documentToModify) -> documentToModify.setDocumentType(documentType.name()));
        FlowStatus newFlowStatus = calculateUserStatus(userInfo);
        userInfo.setStatus(newFlowStatus.name());
        mapper.save(userInfo, mapperConfig);
        return newFlowStatus;
    }
}
