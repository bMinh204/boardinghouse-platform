package com.trototn.boardinghouse.chatbot;

import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {
    private final RoomRepository roomRepository;
    private final ChatbotAiClient chatbotAiClient;

    public ChatbotService(RoomRepository roomRepository, ChatbotAiClient chatbotAiClient) {
        this.roomRepository = roomRepository;
        this.chatbotAiClient = chatbotAiClient;
    }

    @Transactional(readOnly = true)
    public Responses.ChatbotReply reply(String prompt) {
        String normalizedPrompt = normalize(prompt);
        BudgetRange budgetRange = extractBudgetRange(normalizedPrompt);
        Long budget = budgetRange.target();
        Long minBudget = budgetRange.min();
        Long maxBudget = budgetRange.max();
        String area = extractArea(normalizedPrompt);
        String amenity = extractAmenity(normalizedPrompt);
        Integer capacity = extractCapacity(normalizedPrompt);
        List<String> keywords = extractKeywords(normalizedPrompt, area, amenity);
        final Long minBudgetFinal = minBudget;
        final Long maxBudgetFinal = maxBudget;
        Double budgetUpper = null;
        if (maxBudgetFinal == null && minBudgetFinal == null && budget != null) {
            budgetUpper = budget * 1.2;
        }
        final Double budgetUpperFinal = budgetUpper;

        List<Room> approvedRooms = roomRepository.findAll().stream()
                .filter(r -> r.getModerationStatus() == ModerationStatus.APPROVED)
                .filter(r -> r.getStatus() != null)
                .peek(this::initializeRoom)
                .collect(Collectors.toList());

        List<Room> filtered = approvedRooms.stream()
                .filter(r -> minBudgetFinal == null || (r.getPrice() != null && r.getPrice() >= minBudgetFinal))
                .filter(r -> maxBudgetFinal == null || (r.getPrice() != null && r.getPrice() <= maxBudgetFinal))
                .filter(r -> budgetUpperFinal == null || (r.getPrice() != null && r.getPrice() <= budgetUpperFinal))
                .filter(r -> area == null || (r.getAreaName() != null && normalize(r.getAreaName()).contains(area)))
                .filter(r -> capacity == null || (r.getCapacity() != null && r.getCapacity() >= capacity))
                .filter(r -> amenity == null || hasAmenity(r, amenity))
                .filter(r -> keywords.isEmpty() || keywordScore(r, keywords) > 0)
                .collect(Collectors.toList());

        List<Room> ranked = filtered.stream()
                .sorted((a, b) -> Double.compare(scoreRoom(b, budget, minBudgetFinal, maxBudgetFinal, budgetUpperFinal, area, amenity, keywords),
                    scoreRoom(a, budget, minBudgetFinal, maxBudgetFinal, budgetUpperFinal, area, amenity, keywords)))
                .limit(5)
                .toList();

        boolean hasConstraints = minBudgetFinal != null || maxBudgetFinal != null || area != null || amenity != null || capacity != null || !keywords.isEmpty();
        boolean fallback = ranked.isEmpty() && !hasConstraints;
        boolean approximateFallback = ranked.isEmpty() && hasConstraints;
        if (fallback) {
            ranked = approvedRooms.stream()
                    .sorted(Comparator.comparing(Room::getPrice, Comparator.nullsLast(Long::compareTo)))
                    .limit(3)
                    .toList();
        } else if (approximateFallback) {
            ranked = approvedRooms.stream()
                    .sorted((a, b) -> Double.compare(scoreRoom(b, budget, minBudgetFinal, maxBudgetFinal, budgetUpperFinal, area, amenity, keywords),
                            scoreRoom(a, budget, minBudgetFinal, maxBudgetFinal, budgetUpperFinal, area, amenity, keywords)))
                    .limit(5)
                    .toList();
        }

        StringBuilder replyText = new StringBuilder("Mình đã lọc phòng theo yêu cầu");
        if (minBudget != null && maxBudget != null) {
            replyText.append(", giá ").append(minBudget).append("-").append(maxBudget);
        } else if (minBudget != null) {
            replyText.append(", giá từ ").append(minBudget);
        } else if (maxBudget != null) {
            replyText.append(", giá đến ").append(maxBudget);
        } else if (budget != null) {
            replyText.append(", ngân sách mục tiêu ").append(budget);
        }
        if (area != null) replyText.append(", khu vực ").append(area);
        if (amenity != null) replyText.append(", tiện nghi chứa '").append(amenity).append("'");
        if (!keywords.isEmpty()) replyText.append(", từ khóa ").append(String.join(", ", keywords));
        replyText.append(". Có ").append(ranked.size()).append(" gợi ý phù hợp");
        if (budget != null && minBudget == null && maxBudget == null && !ranked.isEmpty()) {
            long over = ranked.stream().filter(r -> r.getPrice() != null && r.getPrice() > budget).count();
            if (over > 0) replyText.append(", một số phòng cao hơn ≤20% ngân sách.");
            else replyText.append(".");
        } else {
            replyText.append(".");
        }
        if (fallback) {
            replyText.append(" Không tìm thấy phòng khớp đủ tiêu chí, mình gợi ý vài phòng giá tốt đang có.");
        } else if (approximateFallback && !ranked.isEmpty()) {
            replyText.append(" Không có phòng khớp tuyệt đối, dưới đây là các phòng gần đúng nhất.");
        } else if (ranked.isEmpty()) {
            replyText.append(" Không tìm thấy phòng phù hợp.");
        }

        List<Responses.RoomView> suggestions = ranked.stream()
                .map(r -> MapperUtil.toRoomView(r, false, List.of()))
                .toList();

        String finalReply = chatbotAiClient
                .generateReply(prompt, replyText.toString(), suggestions)
                .orElse(replyText.toString());
        return new Responses.ChatbotReply(finalReply, budget, area, amenity, suggestions);
    }

    private BudgetRange extractBudgetRange(String prompt) {
        BudgetRange range = extractExplicitRange(prompt);
        if (range != null) {
            return range;
        }
        Long minBudget = extractBudgetWithQualifier(prompt, true);
        Long maxBudget = extractBudgetWithQualifier(prompt, false);
        if (minBudget == null && maxBudget == null) {
            Long budget = extractBudget(prompt);
            return new BudgetRange(null, null, budget);
        }
        Long target = null;
        if (minBudget != null && maxBudget != null) {
            target = (minBudget + maxBudget) / 2;
        } else if (minBudget != null) {
            target = minBudget;
        } else if (maxBudget != null) {
            target = maxBudget;
        }
        return new BudgetRange(minBudget, maxBudget, target);
    }

    private BudgetRange extractExplicitRange(String prompt) {
        String normalized = normalize(prompt);
        Matcher m = Pattern.compile("(?:tu\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu)?\\s+(?:den|toi|-)\\s+(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu)")
                .matcher(normalized);
        if (m.find()) {
            Long min = parseMillion(m.group(1));
            Long max = parseMillion(m.group(3));
            return new BudgetRange(min, max, (min + max) / 2);
        }
        Matcher m2 = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu)\\s*[-–]\\s*(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu)")
                .matcher(normalized);
        if (m2.find()) {
            Long min = parseMillion(m2.group(1));
            Long max = parseMillion(m2.group(3));
            return new BudgetRange(min, max, (min + max) / 2);
        }
        return null;
    }

    private Long extractBudgetWithQualifier(String prompt, boolean isMin) {
        String normalized = normalize(prompt);
        String qualifier = isMin
                ? "(tren|tu|it nhat|toi thieu|>=|lon hon|tren hon)"
                : "(duoi|toi da|khong qua|<=|nho hon|duoi hon)";
        Matcher m = Pattern.compile(qualifier + "\\s*(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu)?")
                .matcher(normalized);
        if (m.find()) {
            String number = m.group(2);
            String unit = m.group(3);
            return parseBudgetToken(number, unit);
        }
        Matcher m2 = Pattern.compile(qualifier + "\\s*(\\d{6,9})").matcher(normalized.replace(".", ""));
        if (m2.find()) {
            try {
                return Long.parseLong(m2.group(2));
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private Long extractBudget(String prompt) {
        Matcher m = Pattern.compile("(\\d{6,9})").matcher(prompt.replace(".", ""));
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) { }
        }
        // số + tr / triệu
        Matcher m2 = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(tr|trieu|triệu)").matcher(prompt);
        if (m2.find()) {
            return parseMillion(m2.group(1));
        }
        return null;
    }

    private Long parseBudgetToken(String number, String unit) {
        if (number == null) return null;
        try {
            if (unit != null) {
                return parseMillion(number);
            }
            String normalized = number.replace(".", "").replace(",", "");
            if (normalized.length() >= 6) {
                return Long.parseLong(normalized);
            }
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private Long parseMillion(String raw) {
        double million = Double.parseDouble(raw.replace(",", "."));
        return (long)(million * 1_000_000);
    }

    private String extractArea(String prompt) {
        String normalized = normalize(prompt);
        int idx = normalized.indexOf("gan");
        if (idx >= 0) {
            String tail = normalized.substring(idx + 3).trim();
            String[] parts = tail.split("[,.;]");
            if (parts.length > 0 && parts[0].length() > 0) {
                String area = parts[0]
                        .split("\\b(duoi|toi da|khong qua|tren|tu|it nhat|toi thieu|gia|ngan sach|co|can|cho)\\b")[0]
                        .trim();
                return area.isBlank() ? null : area;
            }
        }
        return null;
    }

    private String extractAmenity(String prompt) {
        for (String key : new String[]{"wifi", "may giat", "dieu hoa", "ban cong", "khep kin", "noi that"}) {
            if (prompt.contains(key)) return key;
        }
        return null;
    }

    private List<String> extractKeywords(String prompt, String area, String amenity) {
        Set<String> stopWords = Set.of(
                "tim", "phong", "tro", "nha", "goi", "y", "can", "toi", "minh", "cho",
                "gan", "co", "gia", "re", "duoi", "tren", "tu", "den", "khong", "qua",
                "it", "nhat", "thieu", "da", "nguoi", "tr", "trieu", "ngan", "sach",
                "phu", "hop", "va", "hoac", "o", "thue"
        );
        String cleaned = prompt
                .replaceAll("\\d+(?:[.,]\\d+)?", " ")
                .replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !stopWords.contains(token))
                .filter(token -> area == null || !area.contains(token))
                .filter(token -> amenity == null || !normalize(amenity).contains(token))
                .distinct()
                .limit(6)
                .toList();
    }

    private Integer extractCapacity(String prompt) {
        Matcher m = Pattern.compile("(\\d+)\\s*(người|nguoi|people|ppl)").matcher(prompt);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean hasAmenity(Room room, String amenity) {
        if (room.getAmenities() == null) return false;
        final String target = normalize(amenity);
        return room.getAmenities().stream().anyMatch(a -> a != null && normalize(a).contains(target));
    }

    private double scoreRoom(Room room, Long budget, Long minBudget, Long maxBudget, Double budgetUpper, String area, String amenity, List<String> keywords) {
        double score = 0;
        if (room.getPrice() != null) {
            long price = room.getPrice();
            if (minBudget != null && maxBudget == null) {
                if (price >= minBudget) {
                    score += 1.0;
                    score += Math.max(0, 0.8 - Math.abs(price - minBudget) / Math.max(minBudget, 1.0));
                }
            } else if (maxBudget != null && minBudget == null) {
                if (price <= maxBudget) {
                    score += 1.0;
                    score += Math.max(0, 0.8 - Math.abs(price - maxBudget) / Math.max(maxBudget, 1.0));
                }
            } else if (minBudget != null && maxBudget != null) {
                if (price >= minBudget && price <= maxBudget) {
                    score += 1.2;
                }
                long mid = (minBudget + maxBudget) / 2;
                score += Math.max(0, 0.6 - Math.abs(price - mid) / Math.max(mid, 1.0));
            } else if (budget != null) {
                if (price <= budget) {
                    score += 1.0;
                } else if (budgetUpper != null && price <= budgetUpper) {
                    double delta = (price - budget) / budgetUpper; // 0..0.2
                    score += Math.max(0.2, 1.0 - delta * 2);
                }
                double denom = budgetUpper != null ? budgetUpper : Math.max(budget, 1.0);
                score += Math.max(0, 0.8 - Math.abs(price - budget) / denom);
            }
        }
        if (area != null && room.getAreaName() != null && normalize(room.getAreaName()).contains(area)) {
            score += 1.0;
        }
        if (amenity != null && hasAmenity(room, amenity)) {
            score += 1.0;
        }
        score += keywordScore(room, keywords);
        if (room.getSurveyAverage() != null) {
            score += Math.min(room.getSurveyAverage() / 5.0, 1.0);
        }
        score += room.getStatus() == RoomStatus.AVAILABLE ? 0.2 : 0;
        return score;
    }

    private double keywordScore(Room room, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;
        String searchable = normalize(String.join(" ",
                safe(room.getTitle()),
                safe(room.getPropertyName()),
                safe(room.getAddress()),
                safe(room.getAreaName()),
                safe(room.getDescription()),
                room.getAmenities() == null ? "" : String.join(" ", room.getAmenities())
        ));
        double score = 0;
        for (String keyword : keywords) {
            if (searchable.contains(keyword)) {
                score += 0.7;
            }
        }
        return score;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Ensure lazily-loaded fields are initialized while session is open.
     */
    private void initializeRoom(Room room) {
        if (room.getOwner() != null) {
            room.getOwner().getId();
            room.getOwner().getFullName();
        }
        if (room.getAmenities() != null) {
            room.getAmenities().size();
        }
        if (room.getImageUrls() != null) {
            room.getImageUrls().size();
        }
    }

    private String normalize(String input) {
        if (input == null) return "";
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private record BudgetRange(Long min, Long max, Long target) {}
}
