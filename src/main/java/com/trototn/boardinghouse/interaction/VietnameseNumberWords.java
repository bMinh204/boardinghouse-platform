package com.trototn.boardinghouse.interaction;

import java.util.ArrayList;
import java.util.List;

final class VietnameseNumberWords {
    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] UNITS = {"", " nghìn", " triệu", " tỷ"};

    private VietnameseNumberWords() {
    }

    static String toWords(long value) {
        if (value == 0) {
            return "Không đồng";
        }
        if (value < 0) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }

        List<Integer> groups = new ArrayList<>();
        while (value > 0) {
            groups.add((int) (value % 1000));
            value /= 1000;
        }

        StringBuilder result = new StringBuilder();
        for (int unit = groups.size() - 1; unit >= 0; unit--) {
            int group = groups.get(unit);
            if (group > 0) {
                boolean fullHundreds = result.length() > 0 && group < 100;
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(readGroup(group, fullHundreds)).append(UNITS[unit]);
            }
        }
        String words = result.toString().trim().replaceAll("\\s+", " ");
        return Character.toUpperCase(words.charAt(0)) + words.substring(1) + " đồng";
    }

    private static String readGroup(int number, boolean fullHundreds) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int ones = number % 10;
        StringBuilder result = new StringBuilder();

        if (hundreds > 0 || fullHundreds) {
            result.append(DIGITS[hundreds]).append(" trăm");
            if (tens == 0 && ones > 0) {
                result.append(" lẻ");
            }
        }
        if (tens > 1) {
            result.append(" ").append(DIGITS[tens]).append(" mươi");
        } else if (tens == 1) {
            result.append(" mười");
        }
        if (ones > 0) {
            result.append(" ").append(readOne(ones, tens));
        }
        return result.toString().trim();
    }

    private static String readOne(int ones, int tens) {
        if (ones == 1 && tens > 1) return "mốt";
        if (ones == 5 && tens > 0) return "lăm";
        if (ones == 4 && tens > 1) return "tư";
        return DIGITS[ones];
    }
}
