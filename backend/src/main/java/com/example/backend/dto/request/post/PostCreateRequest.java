package com.example.backend.dto.request.post;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateRequest {

    @NotNull(message = "Địa điểm không được để trống")
    private String locationId;

    private String caption;

    private String shootingTip;

    private Boolean forceCreate;
    
    private Double manualLatitude;
    private Double manualLongitude;
    
    private List<String> tags;
    private List<String> photoIds;

    /**
     * Phạm vi hiển thị: PUBLIC | FOLLOWERS_ONLY | PRIVATE
     * Mặc định null → backend sẽ dùng PUBLIC
     */
    private String visibility;
}