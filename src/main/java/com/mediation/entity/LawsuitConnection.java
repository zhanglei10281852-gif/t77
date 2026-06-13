package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lawsuit_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawsuitConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "court_name", nullable = false)
    private String courtName;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_category", nullable = false)
    private CaseCategory caseCategory;

    @Column(name = "court_case_no")
    private String courtCaseNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "judgment_result")
    private JudgmentResult judgmentResult;

    @Column(name = "closing_date")
    private LocalDate closingDate;

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

    public enum CaseCategory {
        民事诉讼, 行政诉讼, 劳动仲裁
    }

    public enum JudgmentResult {
        胜诉, 败诉, 调解结案, 撤诉
    }
}
