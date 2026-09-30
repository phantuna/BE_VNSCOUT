package com.example.backend.service.post;

import com.example.backend.dto.request.post.PostCreateRequest;
import com.example.backend.dto.request.post.PostUpdateRequest;
import com.example.backend.dto.response.post.PostResponse;

import org.springframework.data.domain.Page;
import java.util.List;

public interface PostService {
    PostResponse createPost(String userId, PostCreateRequest request);
    PostResponse getPostById(String postId, String userId);
    Page<PostResponse> getAllPosts(String userId, int page, int size) ;
    Page<PostResponse> getNearbyPosts(double lat, double lng, double radiusKm, String viewerId, int page, int size);
    Page<PostResponse> getPostsByLocation(String locationId, String userId, int page, int size);
    Page<PostResponse> searchPosts(String query, String viewerId, int page, int size);
    Page<PostResponse> getPostsByUserId(String targetUserId, String viewerId, int page, int size);
    PostResponse updatePost(String postId, String userId, PostUpdateRequest request);
    void deletePost(String postId, String userId);
}