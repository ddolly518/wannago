package com.wannago.post.controller;

// import com.wannago.auth.LoginUser; // ← auth? 구현되면
import com.wannago.member.entity.Member;
import com.wannago.post.service.PostLikeService;
import com.wannago.post.dto.PostsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostLikeController {

    private final PostLikeService postLikeService;

    // 단일 게시글에 대한 좋아요 수 & 좋아요 상태 조회
    @GetMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> getLikeInfo(
            @PathVariable Long postId,
            @AuthenticationPrincipal Member member
    ) {
        int likeCount = postLikeService.getLikeCount(postId);
        boolean liked = member != null && postLikeService.hasLiked(postId,member);

        return ResponseEntity.ok(Map.of(
                "postId", postId,
                "likeCount", likeCount,
                "liked", liked
        ));
    }

    // 좋아요 토글 요청 (좋아요 등록/취소)
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal Member member
    ) {
        boolean liked = postLikeService.toggleLike(postId, member);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

}

