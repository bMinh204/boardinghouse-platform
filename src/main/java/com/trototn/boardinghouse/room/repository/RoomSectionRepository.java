package com.trototn.boardinghouse.room.repository;

import com.trototn.boardinghouse.room.domain.RoomSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomSectionRepository extends JpaRepository<RoomSection, Long> {
    List<RoomSection> findByRoomIdOrderByDisplayOrderAscIdAsc(Long roomId);
    long countByRoomId(Long roomId);
}
