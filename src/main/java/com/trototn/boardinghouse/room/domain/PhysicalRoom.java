package com.trototn.boardinghouse.room.domain;

import com.trototn.boardinghouse.auth.domain.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "physical_rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "room_number"}))
public class PhysicalRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private RoomSection section;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PhysicalRoomStatus status = PhysicalRoomStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_id")
    private User heldBy;

    private Instant holdExpiresAt;

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomSection getSection() {
        return section;
    }

    public void setSection(RoomSection section) {
        this.section = section;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public PhysicalRoomStatus getStatus() {
        return status;
    }

    public void setStatus(PhysicalRoomStatus status) {
        this.status = status;
    }

    public User getHeldBy() {
        return heldBy;
    }

    public void setHeldBy(User heldBy) {
        this.heldBy = heldBy;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Instant holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
