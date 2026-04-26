package com.grid07.grid07.service;


import com.grid07.grid07.Repository.CommentRepository;
import com.grid07.grid07.Repository.PostRepository;
import com.grid07.grid07.entity.Comment;
import com.grid07.grid07.entity.Post;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StringRedisTemplate redisTemplate;


    public Post createPost(Long authorId, String authorType, String content) {
        Post post = Post.builder()
                .authorId(authorId)
                .authorType(authorType)
                .content(content)
                .build();
        return postRepository.save(post);
    }


    public String likePost(Long postId) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, 20);
        return "Post liked! Virality +20";
    }


    public String addComment(Long postId, Long authorId, String authorType,
                             String content, Integer depthLevel, Long botId, Long humanId) {


        if (depthLevel > 20) {
            return "REJECTED: Comment thread too deep (max 20 levels)";
        }

        if ("BOT".equalsIgnoreCase(authorType)) {


            String botCountKey = "post:" + postId + ":bot_count";

            String luaScript =
                    "local current = redis.call('INCR', KEYS[1]) " +
                            "if current > tonumber(ARGV[1]) then " +
                            "  redis.call('DECR', KEYS[1]) " +
                            "  return -1 " +
                            "else " +
                            "  return current " +
                            "end";

            Long result = redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList(botCountKey),
                    "100"
            );

            if (result == null || result == -1) {
                return "REJECTED: Bot reply limit reached for this post (max 100)";
            }


            String cooldownKey = "cooldown:bot_" + botId + ":human_" + humanId;
            Boolean cooldownExists = redisTemplate.hasKey(cooldownKey);
            if (Boolean.TRUE.equals(cooldownExists)) {
                return "REJECTED: Bot is in cooldown period for this user (10 min)";
            }

            redisTemplate.opsForValue().set(cooldownKey, "1",
                    java.time.Duration.ofMinutes(10));


            redisTemplate.opsForValue().increment("post:" + postId + ":virality_score");


            handleNotification(humanId, "Bot " + botId + " replied to your post");
        }


        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .authorType(authorType)
                .content(content)
                .depthLevel(depthLevel)
                .build();
        commentRepository.save(comment);

        return "Comment added successfully!";
    }


    private void handleNotification(Long userId, String message) {
        String cooldownKey = "notif_cooldown:user_" + userId;
        String pendingKey = "user:" + userId + ":pending_notifs";

        Boolean onCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(onCooldown)) {

            redisTemplate.opsForList().rightPush(pendingKey, message);
        } else {

            System.out.println("Push Notification Sent to User " + userId + ": " + message);
            redisTemplate.opsForValue().set(cooldownKey, "1",
                    java.time.Duration.ofMinutes(15));
        }
    }
}
