package de.avpod.telegrambot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TextContents {
    GREETINGS_TEXT("Hello, this is Onboarding Bot and I gonna help a little with submitting necessary documents " +
            "to make your onboarding smooth and pleasant."),
    DOCUMENT_UPLOAD_ERROR("Sorry, we got some error during upload the file, could your please repeat once again?"),
    DOCUMENT_UPLOAD_SUCCESS("Document was successfully uploaded, thank you!");
    private final String text;


}
