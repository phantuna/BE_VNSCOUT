package com.example.backend.controller.location;

import com.example.backend.dto.request.location.LocationsRequest;
import com.example.backend.dto.response.location.LocationClusterResponse;
import com.example.backend.dto.response.location.LocationsResponse;
import com.example.backend.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public LocationsResponse createLocation(
            @RequestBody LocationsRequest request,
            @AuthenticationPrincipal String userId
    ) {
        return locationService.createLocation(request, userId);
    }

    @GetMapping("/{id}")
    public LocationsResponse getLocation(@PathVariable String id) {
        return locationService.getLocationById(id);
    }

    @GetMapping
    public Page<LocationsResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer level
    ) {
        return locationService.getAllLocations(page, size, level);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        locationService.deleteLocation(id);
    }

    /**
     * Trả về danh sách cluster địa điểm để vẽ bubble trên bản đồ khi zoom xa.
     * FE gọi khi zoom thay đổi qua ngưỡng (< 8 hoặc 8–10).
     * Không cần auth vì đây là thông tin công khai.
     *
     * @param zoom zoom level hiện tại của bản đồ (mặc định 5.5 — zoom quốc gia)
     */
    @GetMapping("/clusters")
    public List<LocationClusterResponse> getClusters(
            @RequestParam(defaultValue = "5.5") double zoom
    ) {
        return locationService.getLocationClusters(zoom);
    }
}
