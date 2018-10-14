package de.avpod.telegrambot;

@FunctionalInterface
public interface ThrowingRunnable extends Runnable {

    @Override
    default void run() {
        try {
           runThrows();
        } catch (final Exception e) {
            // Implement your own exception handling logic here..
            // For example:
            System.out.println("handling an exception...");
            // Or ...
            throw new RuntimeException(e);
        }
    }

    void runThrows() throws Exception;

}
