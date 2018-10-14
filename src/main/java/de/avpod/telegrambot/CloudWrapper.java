package de.avpod.telegrambot;

public interface CloudWrapper {

    String uploadFile(UploadFile uploadFile);

    void recognizeDocument(String cloudId, DocumentType documentType);

    void deleteDocument(String cloudId);
}
