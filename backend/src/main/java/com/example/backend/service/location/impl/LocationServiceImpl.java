package com.example.backend.service.location.impl;

import com.example.backend.dto.request.location.LocationsRequest;
import com.example.backend.dto.response.location.LocationClusterResponse;
import com.example.backend.dto.response.location.LocationsResponse;
import com.example.backend.dto.response.location.VietMapLocationResponse;
import com.example.backend.entity.Locations;
import com.example.backend.enums.LocationType;
import com.example.backend.exception.AppException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.location.LocationsRepository;
import com.example.backend.service.location.LocationService;
import com.example.backend.mapper.LocationMapper;
import com.example.backend.service.location.VietMapLocationService;
import com.example.backend.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final VietMapLocationService vietMapLocationService;
    private final LocationsRepository locationsRepository;
    private final LocationMapper locationMapper;

    private static final double HARD_MIN_DISTANCE    = 20.0;
    private static final double CONDITIONAL_SPOT_DIST = 3000.0;
    private static final double CONDITIONAL_SERV_DIST = 500.0;

    @Override
    @Transactional
    @CacheEvict(value = "locations", allEntries = true)
    public LocationsResponse createLocation(LocationsRequest request, String creatorId) {
        LocationType locationType = request.getLocationType() != null
                ? request.getLocationType()
                : LocationType.SPOT;

        double conditionalDist = locationType == LocationType.SERVICE
                ? CONDITIONAL_SERV_DIST
                : CONDITIONAL_SPOT_DIST;

        List<Locations> existingLocations = locationsRepository.findAll();
        for (Locations existing : existingLocations) {
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                continue;
            }
            boolean isCheckinPoint = existing.getLevel() != null && existing.getLevel() >= 2;
            
            if (isCheckinPoint && existing.getLatitude() != null && existing.getLongitude() != null) {
                double dist = GeoUtils.distanceInMeters(
                        request.getLatitude().doubleValue(), request.getLongitude().doubleValue(),
                        existing.getLatitude().doubleValue(), existing.getLongitude().doubleValue()
                );
                if (dist < HARD_MIN_DISTANCE) {
                    throw new AppException(ErrorCode.LOCATION_TOO_CLOSE);
                }
                if (dist < conditionalDist) {
                    if (isNameSimilar(request.getName(), existing.getName())) {
                        throw new AppException(ErrorCode.LOCATION_TOO_CLOSE);
                    }
                }
            }
        }

        Locations newLocation = new Locations();
        newLocation.setName(request.getName());
        newLocation.setLatitude(request.getLatitude());
        newLocation.setLongitude(request.getLongitude());
        newLocation.setDescription(request.getDescription());
        newLocation.setCategory(request.getCategory());
        newLocation.setLocationType(locationType);
        newLocation.setCreatorId(creatorId); 

        if (request.getLatitude() != null && request.getLongitude() != null) {
            try {
                VietMapLocationResponse resolved = vietMapLocationService.reverse(request.getLatitude(), request.getLongitude());
                
                if (resolved != null) {
                    if (resolved.getWard() != null) {
                        locationsRepository.findFirstByNameWithTypeContainingAndLevel(resolved.getWard(), Integer.valueOf(1))
                                .ifPresent(newLocation::setParent);
                    }   
                    if (newLocation.getParent() == null && resolved.getDistrict() != null) {
                        locationsRepository.findFirstByNameWithTypeContainingAndLevel(resolved.getDistrict(), Integer.valueOf(1))
                                .ifPresent(newLocation::setParent);
                    }
                    if (newLocation.getParent() == null && resolved.getProvince() != null) {
                        locationsRepository.findFirstByNameWithTypeContainingAndLevel(resolved.getProvince(), Integer.valueOf(0))
                                .ifPresent(newLocation::setParent);
                    }             
                    newLocation.setNameWithType(request.getName());
                }
            } catch (Exception e) {
                System.err.println("Lỗi tự động tra cứu địa giới: " + e.getMessage());
            }
        }
        if (request.getParentId() != null) {
            locationsRepository.findById(request.getParentId())
                    .ifPresent(newLocation::setParent);
        }
        newLocation.setLevel(2); 
        newLocation.setType("check-in"); 
        newLocation.setCode("LOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        if (newLocation.getNameWithType() == null) {
            newLocation.setNameWithType(request.getName());
        }
        String baseSlug = request.getName().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
        newLocation.setSlug(baseSlug + "-" + System.currentTimeMillis() % 100000);

        Locations savedLocation = locationsRepository.save(newLocation);
        return locationMapper.toResponse(savedLocation);
    }

    @Override
    public LocationsResponse getLocationById(String id) {

        Locations location = locationsRepository.findById(id)
                .filter(l -> l.getDeleted() == null || l.getDeleted() == 0)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        return locationMapper.toResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "locations", key = "#page + '-' + #size + '-' + (#level != null ? #level : 'all')")
    public Page<LocationsResponse> getAllLocations(int page, int size, Integer level) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Locations> locationsPage;
        if (level != null) {
            locationsPage = locationsRepository.findByDeletedAndLevel(0, level, pageable);
        } else {
            locationsPage = locationsRepository.findByDeleted(0, pageable);
        }

        return locationsPage.map(loc -> {
            LocationsResponse res = locationMapper.toResponse(loc);
            if (size >= 1000) {
                res.setParent(null);
                res.setDescription(null);
            }
            return res;
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "locations", allEntries = true)
    public LocationsResponse updateLocation(
            String id,
            String name,
            String province,
            String district,
            BigDecimal latitude,
            BigDecimal longitude,
            String description
    ) {

        Locations location = locationsRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        VietMapLocationResponse resolved = null;
        if (latitude != null && longitude != null) {
            resolved = vietMapLocationService.reverse(latitude, longitude);
        }

        location.setName(name);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setDescription(description);

        return locationMapper.toResponse(location);
    }

    @Override
    @Transactional
    @CacheEvict(value = "locations", allEntries = true)
    public void deleteLocation(String id) {

        Locations location = locationsRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        locationsRepository.delete(location);
    }

    private boolean isNameSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        String n1 = normalizeString(name1);
        String n2 = normalizeString(name2);
        return n1.contains(n2) || n2.contains(n1);
    }

    private String normalizeString(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "location-clusters", key = "#zoom")
    public List<LocationClusterResponse> getLocationClusters(double zoom) {
        // Zoom >= 11: FE vẽ marker riêng lẻ, không cần cluster
        if (zoom >= 11) return List.of();

        // zoom < 8  → gom theo tỉnh (level 0)
        // zoom 8-10 → gom theo huyện (level 1)
        int parentLevel = (zoom < 8) ? 0 : 1;

        List<Object[]> rows = locationsRepository.findClustersRaw(parentLevel);

        return rows.stream().map(row -> LocationClusterResponse.builder()
                .id(row[0] != null ? row[0].toString() : null)
                .name(row[1] != null ? row[1].toString() : null)
                .latitude(row[2] != null ? new BigDecimal(row[2].toString()) : null)
                .longitude(row[3] != null ? new BigDecimal(row[3].toString()) : null)
                .code(row[4] != null ? row[4].toString() : null)
                .spotCount(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                .serviceCount(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                .totalPostCount(row[7] != null ? ((Number) row[7]).longValue() : 0L)
                .level(parentLevel)
                .build()
        ).collect(Collectors.toList());
    }
}
