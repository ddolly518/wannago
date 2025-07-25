package com.wannago.post.service;

import com.wannago.common.exception.CustomErrorCode;
import com.wannago.common.exception.CustomException;

import com.wannago.member.entity.Member;
import com.wannago.post.dto.*;

import com.wannago.post.entity.Post;
import com.wannago.post.entity.Tag;
import com.wannago.post.repository.BookmarkRepository;
import com.wannago.post.repository.PostLikeRepository;
import com.wannago.post.repository.PostRepository;
import com.wannago.post.repository.TagRepository;
import com.wannago.post.service.mapper.PostMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service @Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final String DEFAULT_POST_SORT_CRITERIA = "createdDate";
    private static final int PAGE_SIZE = 15;

    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;

    // 게시글 등록
    @Override
    public Long insertPost(PostRequest postRequest, Member member) {
        Post post = postMapper.getPost(postRequest, member, true);
        if(postRequest.getTags() != null && !postRequest.getTags().isEmpty()) {
            setTags(post, postRequest.getTags());
        }

        try {
            post = postRepository.save(post);
        } catch(Exception e) {
            log.error("Fail to write post", e.getMessage());
            throw new CustomException(CustomErrorCode.FAIL_TO_WRITE_POST);
        }

        return post.getId();
    }

    @Override
    public PostsResponse getPosts(int pageNo, String criteria) {
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, criteria));
        Page<Post> postPage = postRepository.findAll(pageable);

        return getPostsResponseWithPostStatus(postPage);
    }

    @Override
    public PostsResponse getMyPosts(int pageNo, Member member) {
        // 사용자가 작성한 게시글 목록 조회 (최신순 정렬)
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_POST_SORT_CRITERIA));
        Page<Post> posts = postRepository.findByAuthorOrderByCreatedDateDesc(member.getLoginId(), pageable);
        return getPostsResponseWithPostStatus(posts);
    }

    @Override
    public PostsResponse getPostsOrderByLikeCount(int pageNo) {
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE);
        Page<PostWithLikeCount> postWithLikeCounts = postRepository.findAllByLikeCount(pageable);
        Map<Long, List<String>> tagsMap = new HashMap<>();
        postWithLikeCounts.forEach(postWithLikeCount -> {
            List<String> tags = tagRepository.getTagsByPost(postWithLikeCount.getPost().getId());
            tagsMap.put(postWithLikeCount.getPost().getId(), tags);
        });

        return postMapper.getPostsResponse(postWithLikeCounts, tagsMap);
    }

    @Override
    public PostsResponse getPostsByBookmark(int pageNo, Member member) {
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE);
        Page<Post> posts = postRepository.findAllByBookmark(member.getLoginId(), pageable);
        return getPostsResponseWithPostStatus(posts);
    }

    @Override
    public PostResponse getPostById(Long postId, Member member) {
        Post post = postRepository
                    .findByIdWithSchedules(postId)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));
        List<String> tags = tagRepository.getTagsByPost(postId);
        PostStatusInfo statusInfo = getPostStatusInfo(postId, member);
        return postMapper.getPostResponse(post, tags, statusInfo);
    }

    @Override
    public PostResponse updatePost(Long postId, PostRequest postRequest, Member member) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));

        if(!post.getAuthor().equals(member.getLoginId())) {
            throw new CustomException(CustomErrorCode.NOT_POST_AUTHOR_FOR_UPDATE);
        }

        // request값으로 post수정
        post.updateTitle(postRequest.getTitle());
        post.updateContents(postRequest.getContents());

        // 기존의 연관관계 배열값들 다 삭제
        post.clearSchedules();
        post.clearTags();

        // 새로운 데이터로 넣어줌
        postMapper.addSchedules(post, postRequest.getSchedules());
        setTags(post, postRequest.getTags());

        List<String> tags = post.getTags().stream()
                .map(postTag -> postTag.getTag().getName()).toList();
        PostStatusInfo statusInfo = getPostStatusInfo(postId, member);
        return postMapper.getPostResponse(post, tags, statusInfo);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String loginId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_ALREADY_DELETED));

        //회원정보와 게시글 작성자 비교하여 자신 글이 맞는지 확인
        if (!post.getAuthor().equals(loginId)) {
            throw new CustomException(CustomErrorCode.NOT_POST_AUTHOR_FOR_DELETE);
        }

        postRepository.delete(post);
    }

    private PostStatusInfo getPostStatusInfo(Long postId, Member member) {
        int likeCount = postLikeRepository.countByPost_Id(postId);

        if(member == null) return new PostStatusInfo(likeCount, false, false);
        boolean isLiked = postLikeRepository.existsByPost_IdAndMember_Id(postId, member.getId());
        boolean isBookmarked = bookmarkRepository.existsByPost_IdAndMember_Id(postId, member.getId());
        return new PostStatusInfo(likeCount, isLiked, isBookmarked);
    }

    public PostsResponse getPostsByAuthor(String loginId, int pageNo) {
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_POST_SORT_CRITERIA));
        Page<Post> postPage = postRepository.findAllByAuthor(loginId, pageable);
        return getPostsResponseWithPostStatus(postPage);
    }

    private void setTags(Post post, List<String> inputTags) {
        for(String inputTag : inputTags) {
            String name = inputTag.trim();
            Optional<Tag> tagOptional = tagRepository.findByName(name);
            Tag tag = tagOptional.orElseGet(() -> tagRepository.save(new Tag(name)));
            post.addTag(tag);
        }
    }

    private PostsResponse getPostsResponseWithPostStatus(Page<Post> postPage) {
        Map<Long, List<String>> tagsMap = new HashMap<>();
        Map<Long, Integer> likeMap = new HashMap<>();
        postPage.stream().forEach(post -> {
            List<String> tag = tagRepository.getTagsByPost(post.getId());
            tagsMap.put(post.getId(), tag);
            int likeCount = postLikeRepository.countByPost_Id(post.getId());
            likeMap.put(post.getId(), likeCount);
        });

        return postMapper.getPostsResponse(postPage, tagsMap, likeMap);
    }


    public PostsResponse searchPosts(String keyword, int pageNo) {
        // 키워드 유효성 검증
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new CustomException(CustomErrorCode.INVALID_SEARCH_KEYWORD);
        }

        String trimmedKeyword = keyword.trim();
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_POST_SORT_CRITERIA));

        // 제목으로만 검색
        Page<Post> postPage = postRepository.findByTitleContainingIgnoreCase(trimmedKeyword, pageable);

        return getPostsResponseWithPostStatus(postPage);
    }
}


