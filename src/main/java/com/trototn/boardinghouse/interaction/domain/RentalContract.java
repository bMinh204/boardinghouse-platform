package com.trototn.boardinghouse.interaction.domain;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_request_id", nullable = false, unique = true)
    private RentalRequest rentalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_room_id")
    private PhysicalRoom physicalRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Long deposit;

    @Column(nullable = false)
    private Long rent;

    @Column(nullable = false, length = 50)
    private String paymentCycle;

    @Column(nullable = false, length = 20)
    private String tenantCccd;

    @Column(nullable = false)
    private String tenantAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RentalRequest getRentalRequest() {
        return rentalRequest;
    }

    public void setRentalRequest(RentalRequest rentalRequest) {
        this.rentalRequest = rentalRequest;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public PhysicalRoom getPhysicalRoom() {
        return physicalRoom;
    }

    public void setPhysicalRoom(PhysicalRoom physicalRoom) {
        this.physicalRoom = physicalRoom;
    }

    public User getTenant() {
        return tenant;
    }

    public void setTenant(User tenant) {
        this.tenant = tenant;
    }

    public User getLandlord() {
        return landlord;
    }

    public void setLandlord(User landlord) {
        this.landlord = landlord;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Long getDeposit() {
        return deposit;
    }

    public void setDeposit(Long deposit) {
        this.deposit = deposit;
    }

    public Long getRent() {
        return rent;
    }

    public void setRent(Long rent) {
        this.rent = rent;
    }

    public String getPaymentCycle() {
        return paymentCycle;
    }

    public void setPaymentCycle(String paymentCycle) {
        this.paymentCycle = paymentCycle;
    }

    public String getTenantCccd() {
        return tenantCccd;
    }

    public void setTenantCccd(String tenantCccd) {
        this.tenantCccd = tenantCccd;
    }

    public String getTenantAddress() {
        return tenantAddress;
    }

    public void setTenantAddress(String tenantAddress) {
        this.tenantAddress = tenantAddress;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
