package com.trototn.boardinghouse.room.repository;

import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PhysicalRoomRepository extends JpaRepository<PhysicalRoom, Long> {
    List<PhysicalRoom> findByRoomIdOrderBySectionDisplayOrderAscDisplayOrderAscIdAsc(Long roomId);
    List<PhysicalRoom> findBySectionIdOrderByDisplayOrderAscIdAsc(Long sectionId);
    List<PhysicalRoom> findByRoomTypeId(Long roomTypeId);
    boolean existsByRoomIdAndRoomNumberIgnoreCase(Long roomId, String roomNumber);
    long countByRoomId(Long roomId);
    long countByRoomIdAndStatus(Long roomId, PhysicalRoomStatus status);
    long countByRoomTypeId(Long roomTypeId);
    long countByRoomTypeIdAndStatus(Long roomTypeId, PhysicalRoomStatus status);
    void deleteBySectionId(Long sectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhysicalRoom> findWithLockById(Long id);
}
