package com.example.backend.service.banned;

import com.example.backend.entity.BannedWord;
import com.example.backend.repository.tag.BannedWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadWordFilterService {

    private final BannedWordRepository bannedWordRepository;

    private List<String> badWords = new ArrayList<>();
    private List<Pattern> badWordsPatterns = new ArrayList<>();

    @PostConstruct
    public void init() {
        reloadBadWords();
    }

    public void reloadBadWords() {
        List<BannedWord> bannedWordsEntity = bannedWordRepository.findAll();
        
        badWords = bannedWordsEntity.stream()
                .map(BannedWord::getWord)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
        
        badWordsPatterns = badWords.stream()
                .map(this::buildAdvancedRegex)
                .toList();

        log.info("Loaded {} bad words from database into memory.", badWords.size());
    }

    /**
     * Tạo regex censor cho từ cấm trong caption/comment (văn bản dài).
     *
     * Nguyên tắc:
     * - Cho phép leet-speak (đ≡d, c≡k, i≡1, o≡0...)
     * - Cho phép TỐI ĐA 1 ký tự ngăn cách giữa các chữ (d.i.t, d_i_t)
     *   KHÔNG dùng [\W_]* (greedy, vô hạn) vì gây over-match:
     *   "dit" regex: d...nhiều ký tự...i...nhiều ký tự...t → match "Đà Lạt đẹp" sai
     * - Boundary (?<!\p{L})...(?!\p{L}): chỉ match khi từ không nằm TRONG chữ cái liền kề
     *   Ví dụ: "dit" khớp "dit !" nhưng KHÔNG khớp "reddit" hay "địa điểm"
     */
    private Pattern buildAdvancedRegex(String word) {
        StringBuilder patternStr = new StringBuilder();
        
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char lower = Character.toLowerCase(c);

            if (lower == 'đ' || lower == 'd') {
                patternStr.append("[đdĐD]");
            } else if (lower == 'c' || lower == 'k') {
                patternStr.append("[cKkC]");
            } else if (lower == 'i') {
                patternStr.append("[iI!1]");
            } else if (lower == 'e') {
                patternStr.append("[eE3]");
            } else if (lower == 'o') {
                patternStr.append("[oO0]");
            } else if (lower == 'a') {
                patternStr.append("[aA@4]");
            } else if ("[]\\^$.|?*+()".indexOf(c) != -1) {
                patternStr.append("\\").append(c);
            } else {
                patternStr.append(c);
            }
            
            // Tối đa 1 ký tự ngăn cách (không phải chữ/số) giữa các ký tự
            // Thay [\W_]* (greedy/vô hạn) bằng [\W_]? (optional, tối đa 1)
            if (i < word.length() - 1) {
                patternStr.append("[\\W_]?");
            }
        }
        
        // Word boundary: từ cấm không được nằm TRONG một chữ dài hơn
        return Pattern.compile("(?ui)(?<!\\p{L})" + patternStr.toString() + "(?!\\p{L})");
    }

    /**
     * Censor từ cấm trong văn bản (thay bằng ***).
     * Dùng cho caption và comment — văn bản dài, có khoảng cách giữa các từ.
     * Không ném exception, chỉ che đi.
     */
    public String censorText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String censoredText = input;

        for (int i = 0; i < badWords.size(); i++) {
            Pattern pattern = badWordsPatterns.get(i);
            String word = badWords.get(i);
            String asterisks = "*".repeat(word.length());
            censoredText = pattern.matcher(censoredText).replaceAll(asterisks);
        }

        return censoredText;
    }
}

