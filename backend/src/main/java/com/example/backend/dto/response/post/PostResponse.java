package com.example.backend.dto.response.post;

import com.example.backend.dto.response.user.UserResponse;
import com.example.backend.dto.response.location.LocationsResponse;
import com.example.backend.dto.request.photo.PhotosRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {

    private String id;
    private String caption;
    private String shootingTip;
    private Long likeCount;
    private Long commentCount;
    private Boolean liked;
    private Boolean isSaved;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
    
    private Double manualLatitude;
    private Double manualLongitude;

    /** ACTIVE | PENDING_REVIEW | HIDDEN */
    private String status;

    /**
     * Lý do bài đang PENDING_REVIEW (nếu có).
     * "NO_GPS" — ảnh thiếu GPS. "LOW_LEVEL" — level thấp.
     */
    private String pendingReason;

    /** PUBLIC | FOLLOWERS_ONLY | PRIVATE */
    private String visibility;


    private UserResponse author;
    private LocationsResponse location;
    private List<String> tags;
    private List<PhotosRequest> photos;

}