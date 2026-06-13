package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "court_referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_case_no", nullable = false)
    private String courtCaseNo;

    @Column(name = "court_name", nullable = false)
    private String courtName;

    @Column(name = "plaintiff_name", nullable = false)
    private String plaintiffName;

    @Column(name = "plaintiff_phone")
    private String plaintiffPhone;

    @Column(name = "defendant_name", nullable = false)
    private String defendantName;

    @Column(name = "defendant_phone")
    private String defendantPhone;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String caseReason;

    @Column(name = "referral_date", nullable = false)
    private LocalDate referralDate;

    @Column(name = "feedback_deadline", nullable = false)
    private LocalDate feedbackDeadline;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mediation_result")
    private MediationResult mediationResult;

    @Column(name = "agreement_no")
    private String agreementNo;

    @Column(name = "feedback_date")
    private LocalDate feedbackDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReferralStatus.待接收;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum MediationResult {
        调解成功, 调解失败, 当事人撤回, 其他
    }

    public enum ReferralStatus {
        待接收, 调解中, 已反馈
    }
}
