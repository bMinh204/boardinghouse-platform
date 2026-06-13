package com.trototn.boardinghouse.common.dto;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class Responses {
    public record UserView(Long id, String fullName, String email, String phone, String address, String cccd,
                           LocalDate dateOfBirth, Role role, boolean active, boolean locked) {}

    public record UserDetail(Long id, String fullName, String email, String phone, String address, String cccd,
                             LocalDate dateOfBirth, Role role, boolean active, boolean locked, Instant createdAt) {}

    public record SurveyView(Long id, String userName, int cleanlinessRating, int securityRating,
                             int convenienceRating, String comment, Instant createdAt) {}

    public record RoomView(
            Long id,
            String propertyName,
            String title,
            String address,
            String areaName,
            Long price,
            Long minPrice,
            Long maxPrice,
            Double size,
            Integer capacity,
            Integer totalRooms,
            Integer availableRooms,
            List<String> amenities,
            String featuredImage,
            List<String> imageUrls,
            String description,
            String contractNote,
            String mapQuery,
            String contactPhone,
            String ownerName,
            String ownerPhone,
            RoomStatus status,
            ModerationStatus moderationStatus,
            LocalDate availableFrom,
            Long viewCount,
            Long contactCount,
            Double surveyAverage,
            Integer surveyCount,
            Boolean favorite,
            List<SurveyView> surveys
    ) {}

    public record MiniRoom(Long id, String title, String propertyName, String address, String areaName) {}

    public record MiniUser(Long id, String fullName, String email) {}

    public record RoomTypeView(Long id, String name, Long price, Double size, Integer capacity,
                               List<String> amenities, String featuredImage, String description,
                               Integer displayOrder, int totalRooms, int availableRooms) {}

    public record PhysicalRoomView(Long id, String roomNumber, Integer displayOrder,
                                   RoomTypeView roomType,
                                   PhysicalRoomStatus status, Instant holdExpiresAt,
                                   boolean heldByCurrentUser) {}

    public record RoomSectionView(Long id, String name, Integer displayOrder,
                                  List<PhysicalRoomView> rooms) {}

    public record RoomLayoutView(Long roomId, String propertyName, List<RoomTypeView> roomTypes, int totalRooms,
                                 int availableRooms, int heldRooms, int occupiedRooms,
                                 int expiringSoonRooms, int maintenanceRooms,
                                 List<RoomSectionView> sections) {}

    public record RentalRequestView(Long id, MiniRoom room, MiniUser tenant, MiniUser landlord,
                                    Long physicalRoomId, String physicalRoomNumber,
                                    LocalDate moveInDate, String note, RentalStatus status,
                                    boolean contractAvailable, Instant expiresAt, Instant updatedAt) {}

    public record ContractDraft(
            Long rentalRequestId,
            String landlordName,
            LocalDate landlordDateOfBirth,
            String landlordAddress,
            String landlordCccd,
            String landlordPhone,
            String tenantName,
            LocalDate tenantDateOfBirth,
            String tenantAddress,
            String tenantCccd,
            String tenantPhone,
            String roomTitle,
            String roomAddress,
            Double roomSize,
            Integer roomCapacity,
            Long suggestedRent,
            LocalDate suggestedStartDate
    ) {}

    public record MessageView(Long id, Long senderId, String senderName, String content, Instant createdAt) {}

    public record ConversationView(Long id, MiniRoom room, MiniUser tenant, MiniUser landlord, Instant updatedAt,
                                   List<MessageView> messages) {}

    public record ReportItem(String label, long value) {}

    public record ReportRentalItem(String roomTitle, String tenantName, String landlordName,
                                   RentalStatus status, Instant updatedAt) {}

    public record LandlordDashboard(long totalRooms, long availableRooms, long occupiedRooms, long maintenanceRooms,
                                    long expiringSoonRooms, long totalViews, long totalContacts, long pendingModeration) {}

    public record AdminDashboard(long totalUsers, long tenantCount, long landlordCount,
                                 long approvedRooms, long pendingRooms, long occupiedRooms, long totalViews,
                                 long totalConversations, List<ReportRentalItem> recentRentalRequests,
                                 List<ReportItem> monthlyReport, List<ReportItem> quarterlyReport,
                                 List<ReportItem> yearlyReport) {}

    public record ChatbotReply(String reply, Long budget, Long minBudget, Long maxBudget,
                                String area, String amenity, List<RoomView> suggestions) {}
}
