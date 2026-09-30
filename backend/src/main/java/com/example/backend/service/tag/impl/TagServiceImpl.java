package com.example.backend.service.tag.impl;

import com.example.backend.dto.response.tag.TagResponse;
import com.example.backend.entity.Tags;
import com.example.backend.exception.AppException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.mapper.TagMapper;
import com.example.backend.repository.tag.TagsRepository;
import com.example.backend.service.banned.BannedWordCacheService;
import com.example.backend.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagsRepository tagsRepository;
    private final TagMapper tagMapper;
    private final BannedWordCacheService bannedWordCacheService;

    @Override
    @Transactional
    public Tags getOrCreateTag(String tagName) {
        String cleanName = normalizeTagName(tagName);
        validateTag(cleanName);

        return tagsRepository.findByName(cleanName)
                .orElseGet(() -> createTagSafely(cleanName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> suggestTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();

        String cleanKeyword = keyword.trim().toLowerCase().replace("#", "");

        List<Tags> results = tagsRepository.findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(cleanKeyword);
        if (results.size() < 5) {
            List<Tags> containsResults = tagsRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(cleanKeyword);
            containsResults.forEach(t -> {
                if (!results.contains(t) && results.size() < 10) results.add(t);
            });
        }

        return results.stream().map(tagMapper::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public Tags createTagStrict(String tagName) {
        String cleanName = normalizeTagName(tagName);

        validateTag(cleanName);

        tagsRepository.findByName(cleanName)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.TAG_EXISTED);
                });

        return createTagSafely(cleanName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "tags")
    public List<TagResponse> getAllTags() {
        return tagsRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTags();
        }

        String cleanKeyword = keyword.trim().toLowerCase();

        if (cleanKeyword.startsWith("#")) {
            cleanKeyword = cleanKeyword.substring(1).trim();
        }

        return tagsRepository.findByNameContainingIgnoreCase(cleanKeyword).stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagResponse updateTag(String tagId, String newName) {
        Tags tag = tagsRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        String cleanName = normalizeTagName(newName);

        validateTag(cleanName);

        tagsRepository.findByName(cleanName)
                .filter(existingTag -> !existingTag.getId().equals(tag.getId()))
                .ifPresent(existingTag -> {
                    throw new AppException(ErrorCode.TAG_EXISTED);
                });

        tag.setName(cleanName);
        return tagMapper.toResponse(tagsRepository.save(tag));
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTag(String tagId) {
        Tags tag = tagsRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        tagsRepository.delete(tag);
    }

    /**
     * Kiểm tra hashtag có vi phạm không.
     *
     * ⚠️ QUAN TRỌNG: Hashtag chỉ bị chặn khi EXACT MATCH với từ cấm.
     * Không dùng CONTAINS (substring) match cho hashtag vì hashtag là 1 từ liền,
     * rất dễ false positive:
     *   - Từ cấm "dit" + hashtag "#reddit" → normalize → "reddit" CONTAINS "dit" → BỊ CHẶN SAI
     *   - Từ cấm "ca" + hashtag "#hochiminhcity" → chứa "ca" → BỊ CHẶN SAI
     *
     * Chỉ block khi người dùng nhập đúng y hệt từ bị cấm làm hashtag.
     */
    private void validateTag(String tagName) {
        String normalized = normalizeForModeration(tagName);
        if (bannedWordCacheService.isBannedExactOnly(normalized)) {
            throw new AppException(ErrorCode.INVALID_TAG);
        }
    }


    private String normalizeTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_TAG);
        }

        String cleanName = tagName.toLowerCase().trim();
        if (cleanName.startsWith("#")) {
            cleanName = cleanName.substring(1).trim();
        }

        // Loại bỏ khoảng trắng giữa các từ để hashtag liền mạch
        cleanName = cleanName.replaceAll("\\s+", "");

        if (cleanName.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_TAG);
        }
        return cleanName;
    }

    /**
     * Chuẩn hóa hashtag để kiểm tra từ cấm.
     * Chỉ bỏ dấu tiếng Việt + ký tự đặc biệt.
     * KHÔNG thay số thành chữ (tránh false positive: #photo3 → photoe → match sai).
     * Việc decode leet-speak (1→i, 0→o...) chỉ cần thiết cho caption dài,
     * không phù hợp với hashtag ngắn là 1 từ liền.
     */
    private String normalizeForModeration(String input) {
        if (input == null) {
            return "";
        }

        // Bỏ dấu tiếng Việt (NFD decompose rồi strip marks)
        String noDiacritics = java.text.Normalizer.normalize(input.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // Chỉ giữ chữ cái latin và số, bỏ ký tự đặc biệt (#, @, !, ...)
        return noDiacritics.replaceAll("[^a-z0-9]", "");
    }


    private Tags createTagSafely(String cleanName) {
        try {
            Tags newTag = new Tags();
            newTag.setName(cleanName);
            return tagsRepository.saveAndFlush(newTag);
        } catch (DataIntegrityViolationException e) {
            return tagsRepository.findByName(cleanName)
                    .orElseThrow(() -> e);
        }
    }
}