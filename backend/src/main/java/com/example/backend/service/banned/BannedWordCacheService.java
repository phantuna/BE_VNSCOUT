package com.example.backend.service.banned;


import com.example.backend.entity.BannedWord;
import com.example.backend.repository.tag.BannedWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BannedWordCacheService {

    private final BannedWordRepository bannedWordRepository;

    private final Set<String> exactWords = new HashSet<>();
    private final Set<String> containsWords = new HashSet<>();

    @PostConstruct
    public void init() {
        reloadCache();
    }

    public void reloadCache() {
        exactWords.clear();
        containsWords.clear();

        List<BannedWord> bannedWords = bannedWordRepository.findAll();

        for (BannedWord bannedWord : bannedWords) {
            String normalized = normalizeForFilter(bannedWord.getWord());

            if ("CONTAINS".equalsIgnoreCase(bannedWord.getType())) {
                containsWords.add(normalized);
            } else {
                exactWords.add(normalized);
            }
        }
    }

    public boolean isBanned(String input) {
        String normalized = normalizeForFilter(input);

        if (normalized.isBlank()) {
            return false;
        }

        if (exactWords.contains(normalized)) {
            return true;
        }

        for (String banned : containsWords) {
            if (normalized.contains(banned)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Chỉ kiểm tra exact match — KHÔNG dùng CONTAINS substring.
     * Dùng cho hashtag validation để tránh false positive:
     *   "#reddit" không bị chặn dù có từ cấm "dit" trong containsWords.
     *   "#hochiminhcity" không bị chặn dù có từ cấm "ca" trong containsWords.
     *
     * Chỉ block hashtag bằng đúng y hệt một từ cấm trong exactWords.
     */
    public boolean isBannedExactOnly(String input) {
        String normalized = normalizeForFilter(input);
        if (normalized.isBlank()) return false;
        return exactWords.contains(normalized);
    }


    public void addToCache(BannedWord bannedWord) {
        String normalized = normalizeForFilter(bannedWord.getWord());

        if ("CONTAINS".equalsIgnoreCase(bannedWord.getType())) {
            containsWords.add(normalized);
        } else {
            exactWords.add(normalized);
        }
    }

    public void removeFromCache(BannedWord bannedWord) {
        String normalized = normalizeForFilter(bannedWord.getWord());

        if ("CONTAINS".equalsIgnoreCase(bannedWord.getType())) {
            containsWords.remove(normalized);
        } else {
            exactWords.remove(normalized);
        }
    }

    private String normalizeForFilter(String input) {
        if (input == null) {
            return "";
        }

        String normalized = input.toLowerCase().trim();

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        normalized = normalized
                .replaceAll("#", "")
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("1", "i")
                .replaceAll("0", "o")
                .replaceAll("3", "e")
                .replaceAll("4", "a")
                .replaceAll("5", "s")
                .replaceAll("7", "t");

        return normalized;
    }
}
