package com.trototn.boardinghouse.room.repository;

import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.auth.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByOwner(User owner);

    @EntityGraph(attributePaths = "owner")
    List<Room> findByModerationStatus(ModerationStatus status);
}
