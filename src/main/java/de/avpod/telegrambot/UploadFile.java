package de.avpod.telegrambot;

import lombok.Builder;
import lombok.Value;

import java.io.File;

@Value
@Builder
public class UploadFile {
    private final String name;
    private final String mimeType;
    private final File content;
    private final String username;
}
