package de.avpod.telegrambot;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class S3Configuration {

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    @ConditionalOnProperty(value = "aws.s3.localStart", havingValue = "true",matchIfMissing = false)
    public AmazonS3 amazonCredentialsS3() {
        log.info("Creating static Amazon S3 B3");
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(accessKey, secretKey)
                        )
                )
                .build();

    }

    @Bean
    @ConditionalOnProperty(value = "aws.s3.localStart", havingValue = "false", matchIfMissing = true)
    public AmazonS3 amazonSafeS3() {
        log.info("Creating non-credentials Amazon S3 B3");
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .build();
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public S3Wrapper s3Wrapper(AmazonS3 amazonS3) {
        return new S3Wrapper(bucketName, amazonS3);
    }
}
