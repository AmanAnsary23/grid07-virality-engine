package com.grid07.grid07.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final StringRedisTemplate redisTemplate;


    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {

        System.out.println("CRON SWEEPER RUNNING...");


        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

        if (keys == null || keys.isEmpty()) {
            System.out.println("No pending notifications found.");
            return;
        }

        for (String key : keys) {


            String userId = key.split(":")[1];


            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

            if (messages == null || messages.isEmpty()) continue;

            int count = messages.size();
            String firstMessage = messages.get(0);


            if (count == 1) {
                System.out.println("Summarized Push Notification to User "
                        + userId + ": " + firstMessage);
            } else {
                System.out.println("Summarized Push Notification to User "
                        + userId + ": " + firstMessage
                        + " and [" + (count - 1) + "] others interacted with your posts.");
            }


            redisTemplate.delete(key);
        }
    }
}
