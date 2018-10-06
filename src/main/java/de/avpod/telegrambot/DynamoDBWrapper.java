package de.avpod.telegrambot;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DynamoDBWrapper implements PersistentStorageWrapper {
    private static final String DRIVE_FILE_ID_COLUMN = "drive_file_id";
    private static final String USERNAME_COLUMN = "username";
    private final Table table;

    @Override
    public String getDriveFileId(String userName) {
        GetItemSpec spec = new GetItemSpec().withPrimaryKey(USERNAME_COLUMN, userName);
        Item outcome = table.getItem(spec);

        return outcome.getString(DRIVE_FILE_ID_COLUMN);
    }
}
