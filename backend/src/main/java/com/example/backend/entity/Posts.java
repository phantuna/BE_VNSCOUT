package com.example.backend.entity;

import jakarta.persistence.*;
import com.example.backend.enums.PostStatus;
import com.example.backend.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Posts extends Base {

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String shootingTip;

    private Long likeCount;
    
    @Column(name = "comment_count")
    private Long commentCount = 0L;
    
    private Double manualLatitude;
    
    private Double manualLongitude;

    @ManyToOne(fetch = FetchType.LAZY)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Locations location;

    @ManyToMany (fetch = FetchType.LAZY )
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tags> tags = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photos> photos = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.ACTIVE;

    /**
     * Phạm vi hiển thị bài viết — độc lập với status.
     * PUBLIC:         mọi người thấy (mặc định)
     * FOLLOWERS_ONLY: chỉ người có follow 2 chiều mới thấy
     * PRIVATE:        chỉ bản thân tác giả thấy; bài PRIVATE sẽ không qua kiểm duyệt admin
     */
    @Column(name = "visibility")
    @Enumerated(EnumType.STRING)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "average_rating")
    private Float averageRating = 0.0f;

    @Column(name = "total_ratings")
    private Integer totalRatings = 0;
}

