package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import com.trototn.boardinghouse.interaction.domain.RentalContract;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.interaction.repository.ConversationRepository;
import com.trototn.boardinghouse.interaction.repository.FavoriteRepository;
import com.trototn.boardinghouse.interaction.repository.MessageRepository;
import com.trototn.boardinghouse.interaction.repository.RentalContractRepository;
import com.trototn.boardinghouse.interaction.repository.RentalRequestRepository;
import com.trototn.boardinghouse.interaction.repository.SurveyRepository;
import com.trototn.boardinghouse.interaction.repository.TemporaryResidenceRepository;
import com.trototn.boardinghouse.room.RoomLayoutService;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.repository.PhysicalRoomRepository;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RoomHoldServiceTest {
    @Mock FavoriteRepository favoriteRepository;
    @Mock RoomRepository roomRepository;
    @Mock RentalRequestRepository rentalRequestRepository;
    @Mock RentalContractRepository rentalContractRepository;
    @Mock TemporaryResidenceRepository temporaryResidenceRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock SurveyRepository surveyRepository;
    @Mock RealtimeEventService realtimeEventService;
    @Mock PhysicalRoomRepository physicalRoomRepository;
    @Mock RoomLayoutService roomLayoutService;

    @Test
    void holdingAvailableRoomCreatesPendingRequestAndLocksRoomFor24Hours() {
        User landlord = user(1L, Role.LANDLORD);
        User tenant = user(2L, Role.TENANT);
        Room room = new Room();
        room.setId(10L);
        room.setOwner(landlord);
        room.setModerationStatus(ModerationStatus.APPROVED);
        PhysicalRoom physicalRoom = new PhysicalRoom();
        physicalRoom.setRoom(room);
        physicalRoom.setRoomNumber("101");
        physicalRoom.setStatus(PhysicalRoomStatus.AVAILABLE);
        when(physicalRoomRepository.findWithLockById(20L)).thenReturn(Optional.of(physicalRoom));

        InteractionService service = service();
        service.holdPhysicalRoom(tenant, 10L, 20L, LocalDate.now().plusDays(3), "Thuê 12 tháng");

        assertEquals(PhysicalRoomStatus.HELD, physicalRoom.getStatus());
        assertEquals(tenant, physicalRoom.getHeldBy());
        assertNotNull(physicalRoom.getHoldExpiresAt());
        ArgumentCaptor<RentalRequest> captor = ArgumentCaptor.forClass(RentalRequest.class);
        verify(rentalRequestRepository).save(captor.capture());
        assertEquals(physicalRoom, captor.getValue().getPhysicalRoom());
        assertNotNull(captor.getValue().getExpiresAt());
        verify(roomLayoutService).syncRoomCounts(10L);
    }

    @Test
    void approvedLegacyRequestWithoutContractCanCreateMissingContract() {
        User landlord = user(1L, Role.LANDLORD);
        landlord.setAddress("Thái Nguyên");
        User tenant = user(2L, Role.TENANT);
        tenant.setCccd("012345678901");
        tenant.setAddress("Bắc Ninh");
        Room room = new Room();
        room.setId(10L);
        room.setOwner(landlord);
        RentalRequest request = new RentalRequest();
        request.setId(30L);
        request.setRoom(room);
        request.setLandlord(landlord);
        request.setTenant(tenant);
        request.setStatus(RentalStatus.APPROVED);
        when(rentalRequestRepository.findById(30L)).thenReturn(Optional.of(request));
        when(rentalContractRepository.findByRentalRequestId(30L)).thenReturn(Optional.empty());

        InteractionController.RentalStatusRequest payload =
                new InteractionController.RentalStatusRequest(
                        RentalStatus.APPROVED,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2027, 7, 1),
                        1_000_000L,
                        1_500_000L,
                        "Ngày 05 hàng tháng",
                        tenant.getCccd(),
                        tenant.getAddress(),
                        tenant.getAddress(),
                        null);

        service().updateRentalStatus(landlord, 30L, payload);

        verify(rentalContractRepository).save(any(RentalContract.class));
    }

    private InteractionService service() {
        return new InteractionService(
                favoriteRepository, roomRepository, rentalRequestRepository, rentalContractRepository,
                temporaryResidenceRepository, conversationRepository, messageRepository, surveyRepository,
                realtimeEventService, physicalRoomRepository, roomLayoutService);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
