package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum DocumentType {
    UNKNOWN("Unknown", "unknown", false),
    UNKNOWN_REQUESTED("Unknown requested", "unknown_requested", false),
    PASSPORT("Passport", "passport", true),
    WORK_BOOK("Work book", "employment_id", false),
    DIPLOMA("Diploma", "diploma", false),
    INN("INN", "INN", true),
    SNILS("Snils", "snils", true),
    FORM_182("Form 182", "form_182", false),
    CARD_DATA("Bank card data", "bankdata", false),
    MILITARY_ID("Military ID", "military_id", false);

    private final String text;
    private final String subfolderName;
    private final boolean mandatory;

    public static Collection<DocumentType> mandatoryDocuments() {
        return Arrays.stream(DocumentType.values()).filter(DocumentType::isMandatory).collect(Collectors.toList());
    }

    public static Collection<DocumentType> realDocuments() {
        return Arrays.asList(
                PASSPORT,
                WORK_BOOK,
                DIPLOMA,
                INN,
                SNILS,
                FORM_182,
                CARD_DATA,
                MILITARY_ID
        );
    }
}
