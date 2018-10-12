package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Getter
public class RecognizeDocumentCallbackData {
    private final DocumentType documentType;
    private final boolean deleteRequested;
    private final String documentId;
    private final String cloudDocumentId;

    public String serialize() {
        if (deleteRequested) {
            return "delete" + "_" + documentId + "_" + cloudDocumentId;
        } else {
            return documentType.name() + "_" + documentId + "_" + cloudDocumentId;
        }
    }

    public static RecognizeDocumentCallbackData deserialize(String callback) {
        try {
            String[] parts = callback.split("_");
            String part = parts[0];
            if (part.equals("delete")) {
                return new RecognizeDocumentCallbackData(
                        null, true, parts[1], parts[2]
                );
            } else {
                return new RecognizeDocumentCallbackData(
                        DocumentType.valueOf(part), false, parts[1], parts[2]
                );
            }
        } catch (Exception e) {
            log.error("Cannot deserialize callback {}", callback, e);
            return null;
        }
    }
}
