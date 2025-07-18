package com.wannago.post.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Getter
@NoArgsConstructor
public class PostTag {

    @Id
    @Column(name = "post_tag_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public PostTag(Post post, Tag tag) {
        this.post = post;
        this.tag = tag;
    }
}
