package com.trototn.boardinghouse.interaction.repository;

import com.trototn.boardinghouse.interaction.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByTenantIdOrLandlordId(Long tenantId, Long landlordId);
    Optional<Conversation> findByTenantIdAndLandlordIdAndRoomId(Long tenantId, Long landlordId, Long roomId);
}
