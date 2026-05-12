package com.trototn.boardinghouse.interaction.repository;

import com.trototn.boardinghouse.interaction.domain.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByRoomId(Long roomId);
    List<Survey> findByRoomIdAndUserId(Long roomId, Long userId);
}
