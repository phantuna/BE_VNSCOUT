package com.example.backend.mapper;

import com.example.backend.dto.response.user.UserResponse;
import com.example.backend.dto.response.location.LocationsResponse;
import com.example.backend.dto.request.photo.PhotosRequest;
import com.example.backend.dto.response.post.PostResponse;
import com.example.backend.entity.Locations;
import com.example.backend.entity.Photos;
import com.example.backend.entity.Posts;
import com.example.backend.repository.comment.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

    @Autowired
    private CommentRepository commentRepository;

    public PostResponse toResponse(Posts post, boolean liked, boolean saved) {
        if (post == null) return null;

        // Tính pendingReason để FE hiển thị thông báo phù hợp
        String pendingReason = null;
        if (post.getStatus() != null && post.getStatus().name().equals("PENDING_REVIEW")) {
            boolean hasNoGps = post.getPhotos() != null && post.getPhotos().stream().anyMatch(p -> {
                var meta = p.getMetadata();
                return meta == null || meta.getGpsLatitude() == null || meta.getGpsLongitude() == null;
            });
            pendingReason = hasNoGps ? "NO_GPS" : "LOW_LEVEL";
        }

        return PostResponse.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .shootingTip(post.getShootingTip())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0L)
                .liked(liked)
                .isSaved(saved)
                .createdDate(post.getCreatedDate())
                .manualLatitude(post.getManualLatitude())
                .manualLongitude(post.getManualLongitude())
                .status(post.getStatus() != null ? post.getStatus().name() : "ACTIVE")
                .pendingReason(pendingReason)
                .visibility(post.getVisibility() != null ? post.getVisibility().name() : "PUBLIC")
                .author(mapAuthor(post))
                .location(mapLocation(post))
                .tags(post.getTags() != null
                        ? post.getTags().stream().map(tag -> tag.getName()).toList()
                        : List.of())
                .photos(post.getPhotos() != null
                        ? post.getPhotos().stream().map(this::mapPhoto).toList()
                        : List.of())
                .build();
    }


    private UserResponse mapAuthor(Posts post) {
        if (post.getUser() == null) return null;

        return UserResponse.builder()
                .id(post.getUser().getId().toString())
                .username(post.getUser().getUsername())
                .avatarUrl(post.getUser().getAvatarUrl())
                .build();
    }

    private LocationsResponse mapLocation(Posts post) {
        if (post.getLocation() == null) return null;

        Locations loc = post.getLocation();
        return LocationsResponse.builder()
                .id(loc.getId())
                .name(loc.getName())
                .nameWithType(loc.getNameWithType())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .level(loc.getLevel())
                .province(extractProvinceName(loc))
                .category(loc.getCategory())
                .goldenHour(loc.getGoldenHour())
                .checkInCount(loc.getCheckInCount())
                .postCount(loc.getPostCount())
                .code(loc.getCode())
                .slug(loc.getSlug())
                .build();
    }

    private String extractProvinceName(Locations location) {
        if (location == null) return null;
        Locations current = location;
        while (current != null) {
            if (current.getLevel() != null && current.getLevel() == 0) {
                return current.getName();
            }
            current = current.getParent();
        }
        return null;
    }

    private PhotosRequest mapPhoto(Photos photo) {
        PhotosRequest dto = new PhotosRequest();
        dto.setId(photo.getId());
        dto.setImageUrl(photo.getImageUrl());
        dto.setWidth(photo.getWidth());
        dto.setHeight(photo.getHeight());

        dto.setIsLocationVerified(photo.getIsLocationVerified());
        dto.setModerationStatus(photo.getModerationStatus());

        if (photo.getMetadata() != null) {
            dto.setCameraMake(photo.getMetadata().getCameraMake());
            dto.setCameraModel(photo.getMetadata().getCameraModel());
            dto.setLensModel(photo.getMetadata().getLensModel());
            dto.setIso(photo.getMetadata().getIso());
            dto.setAperture(photo.getMetadata().getAperture());
            dto.setShutterSpeed(photo.getMetadata().getShutterSpeed());
            dto.setFocalLength(photo.getMetadata().getFocalLength());
            dto.setGpsLatitude(photo.getMetadata().getGpsLatitude());
            dto.setGpsLongitude(photo.getMetadata().getGpsLongitude());
            if (photo.getMetadata().getDateTaken() != null) {
                dto.setDateTaken(photo.getMetadata().getDateTaken().toString());
            }
        }

        return dto;
    }
}