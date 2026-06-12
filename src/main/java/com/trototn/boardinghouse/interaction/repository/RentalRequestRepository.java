package com.trototn.boardinghouse.interaction.repository;

import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, Long> {
    List<RentalRequest> findByTenantId(Long tenantId);
    List<RentalRequest> findByLandlordId(Long landlordId);
    List<RentalRequest> findByLandlordIdOrTenantId(Long landlordId, Long tenantId);
    List<RentalRequest> findByTenantIdAndRoomId(Long tenantId, Long roomId);
    List<RentalRequest> findByStatusAndExpiresAtBefore(RentalStatus status, Instant expiresAt);
    boolean existsByPhysicalRoomIdAndStatus(Long physicalRoomId, RentalStatus status);
    Optional<RentalRequest> findFirstByPhysicalRoomIdAndStatus(Long physicalRoomId, RentalStatus status);
}
