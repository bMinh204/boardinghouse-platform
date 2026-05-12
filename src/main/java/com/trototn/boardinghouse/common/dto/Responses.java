package com.trototn.boardinghouse.common.dto;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.RoomStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class Responses {
    public record UserView(Long id, String fullName, String email, String phone, String address, Role role,
                           boolean active, boolean locked) {}

    public record UserDetail(Long id, String fullName, String email, String phone, String address, Role role,
                             boolean active, boolean locked, Instant createdAt) {}

    public record SurveyView(Long id, String userName, int cleanlinessRating, int securityRating,
                             int convenienceRating, String comment, Instant createdAt) {}

    public record RoomView(
            Long id,
            String propertyName,
            String title,
            String address,
            String areaName,
            Long price,
            Double size,
            Integer capacity,
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

    public record RentalRequestView(Long id, MiniRoom room, MiniUser tenant, MiniUser landlord,
                                    LocalDate moveInDate, String note, RentalStatus status, Instant updatedAt) {}

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

    public record ChatbotReply(String reply, Long budget, String area, String amenity, List<RoomView> suggestions) {}
}
