package com.trototn.boardinghouse.room.repository;

import com.trototn.boardinghouse.room.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByRoomIdOrderByDisplayOrderAscIdAsc(Long roomId);
    boolean existsByRoomIdAndNameIgnoreCase(Long roomId, String name);
    long countByRoomId(Long roomId);
}
