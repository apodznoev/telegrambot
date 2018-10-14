package de.avpod.telegrambot.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import de.avpod.telegrambot.PersistentStorageWrapper;
import de.avpod.telegrambot.telegram.CallbackDataStorage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class DynamoDBConfguration {

    @Value("${aws.dynamodb.tablename}")
    private String tableName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public PersistentStorageWrapper amazonDynamobDbWrapper(DynamoDBMapper dynamoDBMapper,
                                                           DynamoDBMapperConfig mapperConfig) {
        log.info("Creating wrapper for DynamoDB table");

        return new DynamoDBWrapper(dynamoDBMapper, mapperConfig);
    }

    @Bean
    public DynamoDBMapperConfig mapperConfig() {
        return new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName));
    }

    @Bean
    @ConditionalOnProperty(value = "aws.s3.localStart", havingValue = "true", matchIfMissing = false)
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB dynamoDB) {
        return new DynamoDBMapper(dynamoDB);
    }

    @Bean
    public CallbackDataStorage callbackDataStorage(DynamoDBMapper dynamoDBMapper) {
        return new CallbackDataStorage(dynamoDBMapper);
    }
}
