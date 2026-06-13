package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "judicial_confirmations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudicialConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "agreement_no", nullable = false)
    private String agreementNo;

    @Column(name = "court_name", nullable = false)
    private String courtName;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ruling_result")
    private RulingResult rulingResult;

    @Column(name = "ruling_date")
    private LocalDate rulingDate;

    @Column(name = "ruling_no")
    private String rulingNo;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RulingResult {
        确认, 不予确认
    }
}
