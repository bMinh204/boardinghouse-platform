package com.trototn.boardinghouse.interaction.domain;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.room.domain.Room;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "surveys")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int cleanlinessRating;
    private int securityRating;
    private int convenienceRating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getCleanlinessRating() {
        return cleanlinessRating;
    }

    public void setCleanlinessRating(int cleanlinessRating) {
        this.cleanlinessRating = cleanlinessRating;
    }

    public int getSecurityRating() {
        return securityRating;
    }

    public void setSecurityRating(int securityRating) {
        this.securityRating = securityRating;
    }

    public int getConvenienceRating() {
        return convenienceRating;
    }

    public void setConvenienceRating(int convenienceRating) {
        this.convenienceRating = convenienceRating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
