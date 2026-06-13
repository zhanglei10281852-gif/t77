package com.mediation.controller;

import com.mediation.entity.CourtReferral;
import com.mediation.entity.CourtReferral.MediationResult;
import com.mediation.entity.CourtReferral.ReferralStatus;
import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import com.mediation.entity.Dispute.SourceChannel;
import com.mediation.repository.CourtReferralRepository;
import com.mediation.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/court-referrals")
@RequiredArgsConstructor
public class CourtReferralController {

    private final CourtReferralRepository courtReferralRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping("/receive")
    public ResponseEntity<?> receive(@RequestBody Map<String, Object> body) {
        String courtCaseNo = (String) body.get("courtCaseNo");
        String courtName = (String) body.get("courtName");
        String plaintiffName = (String) body.get("plaintiffName");
        String defendantName = (String) body.get("defendantName");
        String caseReason = (String) body.get("caseReason");
        String referralDateStr = (String) body.get("referralDate");
        String feedbackDeadlineStr = (String) body.get("feedbackDeadline");

        if (courtCaseNo == null || courtName == null || plaintiffName == null
                || defendantName == null || caseReason == null
                || referralDateStr == null || feedbackDeadlineStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "法院案号、法院名称、原告、被告、案由、委派日期、要求反馈日期均不能为空"));
        }

        CourtReferral referral = CourtReferral.builder()
                .courtCaseNo(courtCaseNo)
                .courtName(courtName)
                .plaintiffName(plaintiffName)
                .plaintiffPhone(body.get("plaintiffPhone") != null ? (String) body.get("plaintiffPhone") : null)
                .defendantName(defendantName)
                .defendantPhone(body.get("defendantPhone") != null ? (String) body.get("defendantPhone") : null)
                .caseReason(caseReason)
                .referralDate(LocalDate.parse(referralDateStr))
                .feedbackDeadline(LocalDate.parse(feedbackDeadlineStr))
                .status(ReferralStatus.待接收)
                .build();

        CourtReferral saved = courtReferralRepository.save(referral);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        Optional<CourtReferral> opt = courtReferralRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CourtReferral referral = opt.get();
        if (referral.getStatus() != ReferralStatus.待接收) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有待接收的委派才能接收"));
        }

        String caseNo = generateCaseNo();
        Dispute dispute = Dispute.builder()
                .caseNo(caseNo)
                .disputeType(DisputeType.其他)
                .sourceChannel(SourceChannel.法院委派)
                .applicantName(referral.getPlaintiffName())
                .applicantPhone(referral.getPlaintiffPhone() != null ? referral.getPlaintiffPhone() : "暂无")
                .applicantIdCard("法院委派-待补充")
                .respondentName(referral.getDefendantName())
                .respondentPhone(referral.getDefendantPhone())
                .description(referral.getCaseReason())
                .status(DisputeStatus.已受理)
                .build();

        Dispute savedDispute = disputeRepository.save(dispute);

        referral.setDisputeId(savedDispute.getId());
        referral.setStatus(ReferralStatus.调解中);
        courtReferralRepository.save(referral);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("referral", referral);
        response.put("dispute", savedDispute);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CourtReferral>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            ReferralStatus rs = ReferralStatus.valueOf(status);
            List<CourtReferral> list = courtReferralRepository.findByStatus(rs);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            Page<CourtReferral> paged = new org.springframework.data.domain.PageImpl<>(
                    start > list.size() ? Collections.emptyList() : list.subList(start, end),
                    pageable, list.size());
            return ResponseEntity.ok(paged);
        }

        return ResponseEntity.ok(courtReferralRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return courtReferralRepository.findById(id)
                .map(r -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", r.getId());
                    response.put("courtCaseNo", r.getCourtCaseNo());
                    response.put("courtName", r.getCourtName());
                    response.put("plaintiffName", r.getPlaintiffName());
                    response.put("plaintiffPhone", r.getPlaintiffPhone());
                    response.put("defendantName", r.getDefendantName());
                    response.put("defendantPhone", r.getDefendantPhone());
                    response.put("caseReason", r.getCaseReason());
                    response.put("referralDate", r.getReferralDate());
                    response.put("feedbackDeadline", r.getFeedbackDeadline());
                    response.put("disputeId", r.getDisputeId());
                    response.put("mediationResult", r.getMediationResult());
                    response.put("agreementNo", r.getAgreementNo());
                    response.put("feedbackDate", r.getFeedbackDate());
                    response.put("status", r.getStatus());
                    response.put("createdAt", r.getCreatedAt());
                    response.put("updatedAt", r.getUpdatedAt());

                    if (r.getDisputeId() != null) {
                        disputeRepository.findById(r.getDisputeId()).ifPresent(d -> {
                            response.put("caseNo", d.getCaseNo());
                            response.put("disputeStatus", d.getStatus());
                            response.put("disputeResult", d.getResult());
                            response.put("disputeAgreementNo", d.getAgreementNo());
                        });
                    }

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<?> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String resultStr = (String) body.get("mediationResult");
        if (resultStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "调解结果不能为空"));
        }

        MediationResult mediationResult;
        try {
            mediationResult = MediationResult.valueOf(resultStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的调解结果"));
        }

        Optional<CourtReferral> opt = courtReferralRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CourtReferral referral = opt.get();
        if (referral.getStatus() != ReferralStatus.调解中) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有调解中的委派才能反馈"));
        }

        referral.setMediationResult(mediationResult);
        referral.setFeedbackDate(body.get("feedbackDate") != null
                ? LocalDate.parse((String) body.get("feedbackDate"))
                : LocalDate.now());

        String agreementNo = (String) body.get("agreementNo");
        if (agreementNo != null) {
            referral.setAgreementNo(agreementNo);
        } else if (mediationResult == MediationResult.调解成功 && referral.getDisputeId() != null) {
            disputeRepository.findById(referral.getDisputeId())
                    .ifPresent(d -> referral.setAgreementNo(d.getAgreementNo()));
        }

        referral.setStatus(ReferralStatus.已反馈);
        return ResponseEntity.ok(courtReferralRepository.save(referral));
    }

    private String generateCaseNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "RM" + date + random;
    }
}
