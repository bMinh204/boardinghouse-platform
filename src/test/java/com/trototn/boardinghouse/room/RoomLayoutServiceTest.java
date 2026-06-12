package com.trototn.boardinghouse.room;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomSection;
import com.trototn.boardinghouse.room.repository.PhysicalRoomRepository;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import com.trototn.boardinghouse.room.repository.RoomSectionRepository;
import com.trototn.boardinghouse.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomLayoutServiceTest {
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomSectionRepository sectionRepository;
    @Mock
    private PhysicalRoomRepository physicalRoomRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Test
    void onlyOwnerOrAdminCanManageLayout() {
        User owner = user(10L, Role.LANDLORD);
        Room room = room(1L, owner);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        RoomLayoutService service = service();

        assertTrue(service.canManage(owner, 1L));
        assertTrue(service.canManage(user(99L, Role.ADMIN), 1L));
        assertFalse(service.canManage(user(20L, Role.LANDLORD), 1L));
        assertFalse(service.canManage(null, 1L));
    }

    @Test
    void duplicatePhysicalRoomNumberIsRejectedWithinProperty() {
        User owner = user(10L, Role.LANDLORD);
        Room room = room(1L, owner);
        RoomSection section = new RoomSection();
        section.setRoom(room);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));
        when(physicalRoomRepository.existsByRoomIdAndRoomNumberIgnoreCase(1L, "101")).thenReturn(true);
        RoomLayoutService service = service();

        assertThrows(IllegalArgumentException.class,
                () -> service.addPhysicalRoom(owner, 1L, 5L, null, "101", null, null));

        verify(physicalRoomRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private RoomLayoutService service() {
        return new RoomLayoutService(roomRepository, sectionRepository, physicalRoomRepository, roomTypeRepository);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Room room(Long id, User owner) {
        Room room = new Room();
        room.setId(id);
        room.setOwner(owner);
        return room;
    }
}
