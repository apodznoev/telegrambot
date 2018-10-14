package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.waiters.WaiterParameters;
import de.avpod.telegrambot.FlowStatus;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.telegram.UnrecognizedDocumentInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        DynamoDBConfguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
public class DynamoDBWrapperTest {

    @Value("${aws.dynamodb.tablename}")
    private String tableName;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private PersistentStorageWrapper testee;

    @Qualifier("amazonCredentialsDynamobDbWrapper")
    @Autowired
    private AmazonDynamoDB dynamoDB;

    private DynamoDBMapper mapper;

    @Before
    public void loadSampleData() {
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("username").withAttributeType("S"));

        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(
                new KeySchemaElement()
                        .withAttributeName("username").withKeyType(KeyType.HASH)
        );


        CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions).withProvisionedThroughput(
                        new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        dynamoDB.createTable(request);
        dynamoDB.waiters().tableExists().run(new WaiterParameters<>(
                new DescribeTableRequest(tableName)
        ));
        mapper = new DynamoDBMapper(dynamoDB);
        mapper.save(UserInfo.builder()
                        .chatId(666L)
                        .status(FlowStatus.WAITING_FILES.name())
                        .username("test_user")
                        .firstName("Silvester")
                        .lastName("Stallone")
                        .documents(Arrays.asList(
                                StoredDocument.builder()
                                        .id("myID1")
                                        .documentType("UNKNOWN")
                                        .cloudIdentifier("1kPKGKLi0IWKqkCIw15")
                                        .originalFilename("test-file.pdf")
                                        .savedFilename("saved-file.pdf")
                                        .telegramFileId("AAAAAABBBBB")
                                        .telegramThumbnailId("CCCCAAAAAABBBBB")
                                        .build(),
                                StoredDocument.builder()
                                        .id("myID2")
                                        .documentType("PASSPORT")
                                        .cloudIdentifier("2kPKGKLi0IWKqkCIw15")
                                        .originalFilename("pass-file.pdf")
                                        .savedFilename("saved-pass-file.pdf")
                                        .telegramFileId("DDDDDAAAAAABBBBB")
                                        .telegramThumbnailId("BAAAAATMAAAAN")
                                        .build()
                        ))
                        .build(),
                new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                ));
    }

    @After
    public void clean() {
        dynamoDB.deleteTable(tableName);
        dynamoDB.waiters().tableNotExists().run(new WaiterParameters<>(
                new DescribeTableRequest(tableName)
        ));
    }

    @Test
    public void testGetStatus() {
        assertEquals(FlowStatus.NEW, testee.getFlowStatus("test_user_not_exists"));
        assertEquals(FlowStatus.WAITING_FILES, testee.getFlowStatus("test_user"));
    }

    @Test
    public void updateFlowStatus() {
        testee.updateFlowStatus("test_user", FlowStatus.FINISHED);
        assertEquals(FlowStatus.FINISHED, testee.getFlowStatus("test_user"));
    }

    @Test
    public void insertUserInfo() {
        testee.insertUser("test_user_2", "John", "Konnor", 123L, FlowStatus.WAITING_DOCUMENT_RECOGNITION);
        UserInfo userInfo = testee.getFullInfo("test_user_2");
        assertNotNull(userInfo);
        assertEquals(FlowStatus.WAITING_DOCUMENT_RECOGNITION.name(), userInfo.getStatus());
        assertEquals("John", userInfo.getFirstName());
        assertEquals("Konnor", userInfo.getLastName());
        assertEquals(123, (long)userInfo.getChatId());
        assertNotNull(userInfo.getDocuments());
        assertEquals(0, userInfo.getDocuments().size());
    }

    @Test
    public void saveDocumentNotExistingUserException() {
        try {
            testee.saveDocumentInfo("test_user_not_exists", "telegramId", "cloudId",
                    Optional.empty(), "cloud-file.pdf", Optional.of("telegramThumbnailId"));
            fail("Exception expected");
        } catch (Exception ignored) {
        }
        assertNull(testee.getFullInfo("test_user_not_exists"));
    }

    @Test
    public void saveDocumentExistingUserAppendDocument() {
        testee.saveDocumentInfo("test_user", "telegramId", "cloudId",
                Optional.empty(), "cloud-file.pdf", Optional.of("telegramThumbnailId"));
        UserInfo userInfo = testee.getFullInfo("test_user");
        assertNotNull(userInfo);
        assertEquals(FlowStatus.WAITING_FILES.name(), userInfo.getStatus());
        assertEquals("Silvester", userInfo.getFirstName());
        assertEquals("Stallone", userInfo.getLastName());
        assertEquals(666, (long)userInfo.getChatId());
        assertNotNull(userInfo.getDocuments());
        assertEquals(3, userInfo.getDocuments().size());
        StoredDocument document = userInfo.getDocuments().get(2);
        assertNotNull(document.getId());
        assertEquals("UNKNOWN", document.getDocumentType());
        assertEquals("cloudId", document.getCloudIdentifier());
        assertNull(document.getOriginalFilename());
        assertEquals("cloud-file.pdf", document.getSavedFilename());
        assertEquals("telegramThumbnailId", document.getTelegramThumbnailId());
        assertEquals("telegramId", document.getTelegramFileId());
    }

    @Test
    public void queryUsersWithImagesWithUnknwonTypes() {
        mapper.save(UserInfo.builder()
                        .chatId(111L)
                        .status(FlowStatus.WAITING_FILES.name())
                        .username("should_not_be_included_known_types")
                        .firstName("Silvester")
                        .lastName("Stallone")
                        .documents(Arrays.asList(
                                StoredDocument.builder()
                                        .id("docId1")
                                        .documentType("PASSPORT")
                                        .cloudIdentifier("2kPKGKLi0IWKqkCIw15")
                                        .originalFilename("pass-file.pdf")
                                        .savedFilename("saved-pass-file.pdf")
                                        .telegramFileId("DDDDDAAAAAABBBBB")
                                        .telegramThumbnailId("BAAAAATMAAAAN")
                                        .build()
                        ))
                        .build(),
                new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                ));
        mapper.save(UserInfo.builder()
                        .chatId(111L)
                        .status(FlowStatus.FINISHED.name())
                        .username("should_not_be_included_finished")
                        .firstName("Silvester")
                        .lastName("Stallone")
                        .documents(Arrays.asList(
                                StoredDocument.builder()
                                        .id("docId2")
                                        .documentType("UNKNOWN")
                                        .cloudIdentifier("2kPKGKLi0IWKqkCIw15")
                                        .originalFilename("pass-file.pdf")
                                        .savedFilename("saved-pass-file.pdf")
                                        .telegramFileId("DDDDDAAAAAABBBBB")
                                        .telegramThumbnailId("BAAAAATMAAAAN")
                                        .build()
                        ))
                        .build(),
                new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                ));
        mapper.save(UserInfo.builder()
                        .chatId(111L)
                        .status(FlowStatus.WAITING_DOCUMENT_RECOGNITION.name())
                        .username("should_not_be_included_no_documents")
                        .firstName("Silvester")
                        .lastName("Stallone")
                        .documents(Collections.emptyList())
                        .build(),
                new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                ));
        mapper.save(UserInfo.builder()
                        .chatId(111L)
                        .status(FlowStatus.WAITING_DOCUMENT_RECOGNITION.name())
                        .username("should_be_included")
                        .firstName("Silvester")
                        .lastName("Stallone")
                        .documents(Arrays.asList(
                                StoredDocument.builder()
                                        .id("docId3")
                                        .documentType("UNKNOWN")
                                        .cloudIdentifier("2kPKGKLi0IWKqkCIw15")
                                        .originalFilename("pass-file.pdf")
                                        .savedFilename("saved-pass-file.pdf")
                                        .telegramFileId("DDDDDAAAAAABBBBB")
                                        .telegramThumbnailId("BAAAAATMAAAAN")
                                        .build()
                        ))
                        .build(),
                new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                ));
        Collection<UserInfo> userInfos = testee.queryUsersForImageRecognition();
        Collection<String> usernames = userInfos.stream().map(UserInfo::getUsername).collect(Collectors.toList());
        assertNotNull(userInfos);
        assertEquals(usernames.toString(), 2, userInfos.size());
        assertTrue(usernames.contains("should_be_included"));
        assertTrue(usernames.toString(), usernames.contains("test_user"));
    }

    @Test
    public void queryUnrecognizedDocuments() {
        List<UnrecognizedDocumentInfo> documentInfos = testee.queryUnrecognizedDocuments("test_user");
        assertNotNull(documentInfos);
        assertEquals(1, documentInfos.size());
        assertEquals("1kPKGKLi0IWKqkCIw15", documentInfos.get(0).getCloudFileId());
    }

    @Test
    public void markDocumentAsNotifiedForRecognition() {
        testee.markDocumentAsNotifiedForRecognition("test_user", "myID1");
        UserInfo userInfo = testee.getFullInfo("test_user");
        assertEquals(2, userInfo.getDocuments().size());
        assertEquals("UNKNOWN_NOTIFIED", userInfo.getDocuments()
                .stream()
                .filter((document) -> document.getId().equals("myID1"))
                .findFirst()
                .get()
                .getDocumentType()
        );
    }
}