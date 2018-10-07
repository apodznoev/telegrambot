package de.avpod.telegrambot.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import de.avpod.telegrambot.CloudWrapper;
import de.avpod.telegrambot.UploadFile;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class S3Wrapper implements CloudWrapper {
    private static final String ROOT_FOLDER_NAME = "TelegramBot";
    private final String bucketName;
    private final AmazonS3 s3;

    @Override
    public String uploadFile(UploadFile uploadFile) {
        log.info("Upload file to s3 for bucket {} with name {}", bucketName, uploadFile.getName());
        PutObjectResult result = s3.putObject(
                bucketName, ROOT_FOLDER_NAME + "/" + uploadFile.getUsername() + "/" +uploadFile.getName(), uploadFile.getContent()
        );
        log.info("File was uploaded with eTag: {} md5: {}",
                result.getMetadata().getETag(), result.getContentMd5());
        return result.getContentMd5();
    }

}
