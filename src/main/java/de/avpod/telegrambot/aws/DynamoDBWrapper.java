package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
    private static final String UNKNOWN_DOCUMENT_TYPE = "UNKNOWN";
    private static final String UNKNOWN_NOTIFIED_DOCUMENT_TYPE = "UNKNOWN_NOTIFIED";
    private static final String REQUESTED_DOCUMENT_TYPE = "REQUESTED";
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
                .documentType(UNKNOWN_DOCUMENT_TYPE)
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
//        PutItemOutcome outcome = table
//                .putItem(new Item()
//                        .withPrimaryKey(USERNAME_COLUMN, userName)
//                        .withString(":st", flowStatus.name())
//                        .withString(":first", firstName)
//                        .withString(":last", lastName)
//                        .withNumber(":chat", chatId)
//                        .withList(DOCUMENTS_COLUMN, Collections.emptyList()));
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
//        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
//                .withPrimaryKey(new PrimaryKey(USERNAME_COLUMN, userName))
//                .withUpdateExpression("set #st = :st")
//                .withNameMap(new NameMap()
//                        .with("#st", STATUS_COLUMN)
//                )
//                .withValueMap(new ValueMap()
//                        .withString(":st", flowStatus.name())
//                )
//                .withReturnValues(ReturnValue.UPDATED_NEW);
//        UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
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
                                document.getDocumentType().equals(UNKNOWN_DOCUMENT_TYPE)
                        )).collect(Collectors.toList());
    }

    @Override
    public List<UnrecognizedDocumentInfo> queryUnrecognizedDocuments(String username) {
        log.info("Getting unrecognized documents status for user {}", username);
        UserInfo userInfo = mapper.load(UserInfo.class, username, mapperConfig);
        return userInfo.getDocuments()
                .stream()
                .filter((document) -> document.getDocumentType().equals(UNKNOWN_DOCUMENT_TYPE))
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
        UserInfo userInfo = getFullInfo(username);
        userInfo.getDocuments()
                .stream()
                .filter((document) -> document.getId().equals(documentId))
                .forEach((documentToModify) -> documentToModify.setDocumentType(UNKNOWN_NOTIFIED_DOCUMENT_TYPE));
        mapper.save(userInfo, mapperConfig);
    }

    @Override
    public UserInfo getFullInfo(String userName) {
        log.info("Getting full user info for user {}", userName);
        return mapper.load(UserInfo.class, userName, mapperConfig);
    }
}
