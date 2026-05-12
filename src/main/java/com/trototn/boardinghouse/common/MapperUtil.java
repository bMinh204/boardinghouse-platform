package com.trototn.boardinghouse.common;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.domain.Conversation;
import com.trototn.boardinghouse.interaction.domain.Message;
import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import com.trototn.boardinghouse.interaction.domain.Survey;
import com.trototn.boardinghouse.room.domain.Room;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapperUtil {
    private static final String DEFAULT_ROOM_IMAGE = "https://picsum.photos/seed/trototn-room/900/600";

    public static Responses.UserView toUserView(User user) {
        if (user == null) return null;
        return new Responses.UserView(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getAddress(), user.getRole(), user.isActive(), user.isLocked());
    }

    public static Responses.UserDetail toUserDetail(User user) {
        if (user == null) return null;
        return new Responses.UserDetail(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getAddress(), user.getRole(), user.isActive(), user.isLocked(), user.getCreatedAt());
    }

    public static Responses.SurveyView toSurveyView(Survey survey) {
        return new Responses.SurveyView(
                survey.getId(),
                survey.getUser().getFullName(),
                survey.getCleanlinessRating(),
                survey.getSecurityRating(),
                survey.getConvenienceRating(),
                survey.getComment(),
                survey.getCreatedAt()
        );
    }

    public static Responses.RoomView toRoomView(Room room, boolean favorite, List<Survey> surveys) {
        List<Responses.SurveyView> surveyViews = surveys == null ? Collections.emptyList() :
                surveys.stream().map(MapperUtil::toSurveyView).collect(Collectors.toList());
        List<String> imageUrls = safeImageUrls(room.getImageUrls());
        List<String> amenities = safeStringList(room.getAmenities());
        return new Responses.RoomView(
                room.getId(),
                room.getPropertyName(),
                room.getTitle(),
                room.getAddress(),
                room.getAreaName(),
                room.getPrice(),
                room.getSize(),
                room.getCapacity(),
                amenities,
                safeFeaturedImage(room.getFeaturedImage(), imageUrls),
                imageUrls,
                room.getDescription(),
                room.getContractNote(),
                room.getMapQuery(),
                room.getContactPhone(),
                room.getOwner() != null ? room.getOwner().getFullName() : null,
                room.getOwner() != null ? room.getOwner().getPhone() : null,
                room.getStatus(),
                room.getModerationStatus(),
                room.getAvailableFrom(),
                room.getViewCount(),
                room.getContactCount(),
                room.getSurveyAverage(),
                room.getSurveyCount(),
                favorite,
                surveyViews
        );
    }

    public static Responses.MiniRoom toMiniRoom(Room room) {
        return new Responses.MiniRoom(room.getId(), room.getTitle(), room.getPropertyName(), room.getAddress(), room.getAreaName());
    }

    private static String safeFeaturedImage(String featuredImage, List<String> imageUrls) {
        if (isSafeImageUrl(featuredImage)) return featuredImage;
        if (imageUrls != null && !imageUrls.isEmpty()) return imageUrls.get(0);
        return DEFAULT_ROOM_IMAGE;
    }

    private static List<String> safeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) return Collections.emptyList();
        return imageUrls.stream()
                .filter(MapperUtil::isSafeImageUrl)
                .collect(Collectors.toList());
    }

    private static List<String> safeStringList(List<String> values) {
        if (values == null) return Collections.emptyList();
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toList());
    }

    private static boolean isSafeImageUrl(String value) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("http://")
                || trimmed.startsWith("https://")
                || trimmed.startsWith("/uploads/")
                || trimmed.startsWith("data:image/");
    }

    public static Responses.MiniUser toMiniUser(User user) {
        return new Responses.MiniUser(user.getId(), user.getFullName(), user.getEmail());
    }

    public static Responses.RentalRequestView toRentalView(RentalRequest r) {
        return new Responses.RentalRequestView(
                r.getId(),
                toMiniRoom(r.getRoom()),
                toMiniUser(r.getTenant()),
                toMiniUser(r.getLandlord()),
                r.getMoveInDate(),
                r.getNote(),
                r.getStatus(),
                r.getUpdatedAt()
        );
    }

    public static Responses.MessageView toMessageView(Message m) {
        return new Responses.MessageView(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getContent(),
                m.getCreatedAt()
        );
    }

    public static Responses.ConversationView toConversationView(Conversation c, List<Message> messages) {
        return new Responses.ConversationView(
                c.getId(),
                toMiniRoom(c.getRoom()),
                toMiniUser(c.getTenant()),
                toMiniUser(c.getLandlord()),
                c.getUpdatedAt(),
                messages.stream().map(MapperUtil::toMessageView).collect(Collectors.toList())
        );
    }
}
