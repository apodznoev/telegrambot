package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import de.avpod.telegrambot.DocumentType;
import lombok.*;

@DynamoDBTable(tableName = "CallbackData")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TelegramInlineCallbackData {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "document_type")
    private DocumentType documentType;

    @DynamoDBAttribute(attributeName = "document_id")
    private String documentId;

    @DynamoDBAttribute(attributeName = "cloud_file_id")
    private String cloudId;

    @DynamoDBIgnore
    private String text;

    @DynamoDBIgnore
    public boolean isDelete() {
        return documentType == null;
    }

}
