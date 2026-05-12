package com.trototn.boardinghouse.interaction.repository;

import com.trototn.boardinghouse.interaction.domain.Favorite;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByTenantAndRoom(User tenant, Room room);
    List<Favorite> findByTenantId(Long tenantId);
}
