package de.avpod.telegrambot.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import de.avpod.telegrambot.PersistentStorageWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class DynamoDBConfguration {
    private static final String TABLE_NAME = "TelegramBot";

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public PersistentStorageWrapper amazonDynamobDbWrapper(AmazonDynamoDB amazonDynamoDB) throws InterruptedException {
        log.info("Creating wrapper for DynamoDB table");

        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);

        log.info("Attempting to get table; please wait...");
        Table table = dynamoDB.getTable(TABLE_NAME);
        table.waitForActive();
        log.info("Success getting connection to table {}.  Table status: {}",
                TABLE_NAME, table.getDescription().getTableStatus()
        );
        return new DynamoDBWrapper(table);
    }

    @Bean
    @ConditionalOnProperty(value = "aws.s3.localStart", havingValue = "true",matchIfMissing = false)
    public AmazonDynamoDB amazonCredentialsDynamobDbWrapper() {
        log.info("Creating credentials wrapper for DynamoDB");
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)
                ))
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "aws.s3.localStart", havingValue = "false", matchIfMissing = true)
    public AmazonDynamoDB amazonSafeDynamobDbWrapper() {
        log.info("Creating EC2 wrapper for DynamoDB");
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new EC2ContainerCredentialsProviderWrapper())
                .build();
    }

}
