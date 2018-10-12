package de.avpod.telegrambot.telegram;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
public class UnrecognizedDocumentInfo {
    private final String id;
    private final long chatId;
    private final String cloudFileId;
    private final String telegramFileId;
    private final Optional<String> telegramThumbnailId;
    private final Optional<String> originalFileName;
}
