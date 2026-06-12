package com.trototn.boardinghouse.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseNumberWordsTest {
    @Test
    void convertsRentalAmounts() {
        assertEquals("Một triệu năm trăm nghìn đồng", VietnameseNumberWords.toWords(1_500_000));
        assertEquals("Hai triệu không trăm lẻ năm nghìn đồng", VietnameseNumberWords.toWords(2_005_000));
        assertEquals("Không đồng", VietnameseNumberWords.toWords(0));
    }
}
