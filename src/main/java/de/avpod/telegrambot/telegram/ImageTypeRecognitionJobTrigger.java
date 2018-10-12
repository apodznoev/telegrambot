package de.avpod.telegrambot.telegram;

import lombok.Setter;

@Setter
public class ImageTypeRecognitionJobTrigger {
    private Runnable listener;

    public void scheduleRecognition() {
        if (listener != null)
            listener.run();
    }

    public void addListener(Runnable runnable) {
        this.listener = runnable;
    }
}
