package de.avpod.telegrambot;

public enum FlowStatus {
    NEW,
    WAITING_FILES,
    MANDATORY_DOCUMENTS_SUBMITTED,
    WAITING_DOCUMENT_RECOGNITION,
    FINISHED
}
