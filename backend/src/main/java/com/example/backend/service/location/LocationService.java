package com.example.backend.service.location;

import com.example.backend.dto.request.location.LocationsRequest;
import com.example.backend.dto.response.location.LocationClusterResponse;
import com.example.backend.dto.response.location.LocationsResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;


public interface LocationService {

    LocationsResponse createLocation(LocationsRequest request, String creatorId);

    LocationsResponse getLocationById(String id);

    Page<LocationsResponse> getAllLocations(int page, int size, Integer level);

    LocationsResponse updateLocation(
            String id,
            String name,
            String province,
            String district,
            BigDecimal latitude,
            BigDecimal longitude,
            String description
    );

    void deleteLocation(String id);

    /**
     * Trả về danh sách cluster địa điểm để hiển thị bubble trên bản đồ khi zoom xa.
     * @param zoom zoom level của bản đồ (VietMap GL)
     *             zoom < 8  → cluster theo tỉnh (level 0)
     *             zoom 8-10 → cluster theo huyện (level 1)
     *             zoom >= 11 → không cluster (trả empty, FE sẽ vẽ marker riêng lẻ)
     */
    List<LocationClusterResponse> getLocationClusters(double zoom);
}