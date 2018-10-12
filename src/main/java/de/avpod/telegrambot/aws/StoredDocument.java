package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.*;

@DynamoDBDocument
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StoredDocument {
    @DynamoDBAttribute(attributeName = "id")
    private String id;
    @DynamoDBAttribute(attributeName = "document_type")
    private String documentType;
    @DynamoDBAttribute(attributeName = "drive_id")
    private String cloudIdentifier;
    @DynamoDBAttribute(attributeName = "original_filename")
    private String originalFilename;
    @DynamoDBAttribute(attributeName = "saved_filename")
    private String savedFilename;
    @DynamoDBAttribute(attributeName = "telegram_file_id")
    private String telegramFileId;
    @DynamoDBAttribute(attributeName = "telegram_thumbnail_id")
    private String telegramThumbnailId;
}
