package com.grid07.grid07.Controller;

import com.grid07.grid07.entity.Post;
import com.grid07.grid07.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Map<String, String> body) {
        Long authorId = Long.parseLong(body.get("authorId"));
        String authorType = body.get("authorType");
        String content = body.get("content");
        Post post = postService.createPost(authorId, authorType, content);
        return ResponseEntity.ok(post);
    }


    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId) {
        String result = postService.likePost(postId);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {

        Long authorId = Long.parseLong(body.get("authorId"));
        String authorType = body.get("authorType");
        String content = body.get("content");
        Integer depthLevel = Integer.parseInt(body.get("depthLevel"));
        Long botId = body.get("botId") != null ? Long.parseLong(body.get("botId")) : null;
        Long humanId = body.get("humanId") != null ? Long.parseLong(body.get("humanId")) : null;

        String result = postService.addComment(postId, authorId, authorType,
                content, depthLevel, botId, humanId);

        if (result.startsWith("REJECTED")) {
            return ResponseEntity.status(429).body(result);
        }
        return ResponseEntity.ok(result);
    }
}