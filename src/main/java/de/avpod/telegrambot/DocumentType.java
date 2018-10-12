package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DocumentType {
    PASSPORT("Passport");
    private final String name;
}
