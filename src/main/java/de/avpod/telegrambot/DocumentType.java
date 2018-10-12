package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;

@AllArgsConstructor
@Getter
public enum DocumentType {
    UNKNOWN("Unknown", "unknown"),
    UNKNOWN_REQUESTED("Unknown requested", "unknown_requested"),
    PASSPORT("Passport", "passport"),
    WORK_BOOK("Work book", "employment_id"),
    DIPLOMA("Diploma", "diploma"),
    INN("INN", "INN"),
    SNILS("Snils", "snils"),
    FORM_182("Form 182", "form_182"),
    CARD_DATA("Bank card data", "bankdata"),
    MILITARY_ID("Military ID", "military_id");

    private final String text;
    private final String subfolderName;

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
