package de.avpod.telegrambot.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.util.List;

@DynamoDBTable(tableName = "TelegramBot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    @DynamoDBHashKey(attributeName = "username")
    private String username;
    @DynamoDBAttribute(attributeName = "user_status")
    private String status;
    @DynamoDBAttribute(attributeName = "first_name")
    private String firstName;
    @DynamoDBAttribute(attributeName = "last_name")
    private String lastName;
    @DynamoDBAttribute(attributeName = "chat_id")
    private long chatId;
    @DynamoDBAttribute
    private List<StoredDocument> documents;



}
