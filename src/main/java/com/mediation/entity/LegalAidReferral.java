package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legal_aid_referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalAidReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "party_name", nullable = false)
    private String partyName;

    @Column(name = "party_phone", nullable = false)
    private String partyPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "economic_status", nullable = false)
    private EconomicStatus economicStatus;

    @Column(columnDefinition = "TEXT")
    private String economicDetail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String referralReason;

    @Column(name = "referral_date", nullable = false)
    private LocalDate referralDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "acceptance_result")
    private AcceptanceResult acceptanceResult;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "acceptance_date")
    private LocalDate acceptanceDate;

    @Column(name = "legal_aid_case_no")
    private String legalAidCaseNo;

    @Column(name = "assigned_lawyer")
    private String assignedLawyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "handling_status")
    private HandlingStatus handlingStatus;

    @Column(columnDefinition = "TEXT")
    private String handlingResult;

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

    public enum EconomicStatus {
        低保, 残疾, 老年, 低收入, 其他
    }

    public enum AcceptanceResult {
        已受理, 不予受理
    }

    public enum HandlingStatus {
        待分配, 办理中, 已结案
    }
}
