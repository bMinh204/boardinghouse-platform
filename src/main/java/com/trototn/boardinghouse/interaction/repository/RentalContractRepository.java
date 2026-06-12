package com.trototn.boardinghouse.interaction.repository;

import com.trototn.boardinghouse.interaction.domain.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
    @EntityGraph(attributePaths = {"room", "tenant", "landlord", "physicalRoom"})
    Optional<RentalContract> findByRentalRequestId(Long rentalRequestId);
    boolean existsByRentalRequestId(Long rentalRequestId);
}
