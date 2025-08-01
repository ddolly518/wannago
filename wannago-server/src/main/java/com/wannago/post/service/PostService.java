package com.wannago.post.service;


import com.wannago.member.entity.Member;
import com.wannago.post.dto.PostRequest;
import com.wannago.post.dto.PostResponse;
import com.wannago.post.dto.PostsResponse;

public interface PostService {
    Long insertPost(PostRequest postRequest, Member member);

    PostsResponse getPosts(int pageNo, String criteria);

    PostsResponse getMyPosts(int pageNo, Member member);

    PostsResponse getPostsOrderByLikeCount(int pageNo);

    PostsResponse getPostsByBookmark(int pageNo, Member member);

    PostResponse getPostById(Long postId, Member member);

    PostResponse updatePost(Long postId, PostRequest postRequest, Member member);

    void deletePost(Long postId, String loginId);

    PostsResponse getPostsByAuthor(String author, int pageNo);

    PostsResponse searchPosts(String keyword, int pageNo);
}
