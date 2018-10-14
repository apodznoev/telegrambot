package de.avpod.telegrambot;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class UserAwareExecutor {
    private final List<Executor> availableExecutors;
    private final AtomicInteger roundRobinCounter;
    private final ConcurrentHashMap<String, Executor> assignedExecutors;

    public UserAwareExecutor(int threadsCount) {
        availableExecutors = new ArrayList<>();
        for (int i = 0; i < threadsCount; i++) {
            availableExecutors.add(Executors.newSingleThreadExecutor(new CustomizableThreadFactory("executor-user")));
        }
        assignedExecutors = new ConcurrentHashMap<>();
        roundRobinCounter = new AtomicInteger();
    }

    public Executor getExecutor(String username) {
        return this.assignedExecutors.computeIfAbsent(username, (u) -> {
            log.info("Calculating executor index for username {}", u);
            int index = roundRobinCounter.getAndIncrement();
            if (index >= availableExecutors.size()) {
                index = 0;
                roundRobinCounter.set(index);
            }
            log.info("Target index is {}", index);
            return availableExecutors.get(index);
        });
    }

    void cleanupForUser(String username) {
        log.info("Cleanup executor assignment for user {}", username);
        assignedExecutors.remove(username);
    }

}
