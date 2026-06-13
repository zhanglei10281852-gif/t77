package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.LegalAidReferral;
import com.mediation.entity.LegalAidReferral.AcceptanceResult;
import com.mediation.entity.LegalAidReferral.EconomicStatus;
import com.mediation.entity.LegalAidReferral.HandlingStatus;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.LegalAidReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/legal-aid-referrals")
@RequiredArgsConstructor
public class LegalAidReferralController {

    private final LegalAidReferralRepository legalAidReferralRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        String partyName = (String) body.get("partyName");
        String partyPhone = (String) body.get("partyPhone");
        String economicStatusStr = (String) body.get("economicStatus");
        String referralReason = (String) body.get("referralReason");
        String referralDateStr = (String) body.get("referralDate");

        if (disputeId == null || partyName == null || partyPhone == null
                || economicStatusStr == null || referralReason == null || referralDateStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "案件ID、当事人姓名、联系方式、经济状况、转介原因、转介日期均不能为空"));
        }

        if (!disputeRepository.existsById(disputeId)) {
            return ResponseEntity.notFound().build();
        }

        EconomicStatus economicStatus;
        try {
            economicStatus = EconomicStatus.valueOf(economicStatusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的经济状况"));
        }

        LegalAidReferral referral = LegalAidReferral.builder()
                .disputeId(disputeId)
                .partyName(partyName)
                .partyPhone(partyPhone)
                .economicStatus(economicStatus)
                .economicDetail(body.get("economicDetail") != null ? (String) body.get("economicDetail") : null)
                .referralReason(referralReason)
                .referralDate(LocalDate.parse(referralDateStr))
                .build();

        LegalAidReferral saved = legalAidReferralRepository.save(referral);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<LegalAidReferral>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long disputeId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (disputeId != null) {
            List<LegalAidReferral> list = legalAidReferralRepository.findByDisputeId(disputeId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            Page<LegalAidReferral> paged = new org.springframework.data.domain.PageImpl<>(
                    start > list.size() ? Collections.emptyList() : list.subList(start, end),
                    pageable, list.size());
            return ResponseEntity.ok(paged);
        }

        return ResponseEntity.ok(legalAidReferralRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return legalAidReferralRepository.findById(id)
                .map(r -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", r.getId());
                    response.put("disputeId", r.getDisputeId());
                    response.put("partyName", r.getPartyName());
                    response.put("partyPhone", r.getPartyPhone());
                    response.put("economicStatus", r.getEconomicStatus());
                    response.put("economicDetail", r.getEconomicDetail());
                    response.put("referralReason", r.getReferralReason());
                    response.put("referralDate", r.getReferralDate());
                    response.put("acceptanceResult", r.getAcceptanceResult());
                    response.put("rejectionReason", r.getRejectionReason());
                    response.put("acceptanceDate", r.getAcceptanceDate());
                    response.put("legalAidCaseNo", r.getLegalAidCaseNo());
                    response.put("assignedLawyer", r.getAssignedLawyer());
                    response.put("handlingStatus", r.getHandlingStatus());
                    response.put("handlingResult", r.getHandlingResult());
                    response.put("createdAt", r.getCreatedAt());
                    response.put("updatedAt", r.getUpdatedAt());

                    disputeRepository.findById(r.getDisputeId()).ifPresent(d -> {
                        response.put("caseNo", d.getCaseNo());
                        response.put("applicantName", d.getApplicantName());
                        response.put("respondentName", d.getRespondentName());
                        response.put("disputeStatus", d.getStatus());
                    });

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/acceptance")
    public ResponseEntity<?> setAcceptance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String resultStr = (String) body.get("acceptanceResult");
        if (resultStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "受理结果不能为空"));
        }

        AcceptanceResult acceptanceResult;
        try {
            acceptanceResult = AcceptanceResult.valueOf(resultStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的受理结果"));
        }

        Optional<LegalAidReferral> opt = legalAidReferralRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LegalAidReferral r = opt.get();
        r.setAcceptanceResult(acceptanceResult);

        if (acceptanceResult == AcceptanceResult.不予受理 && body.get("rejectionReason") != null) {
            r.setRejectionReason((String) body.get("rejectionReason"));
        }

        if (body.get("acceptanceDate") != null) {
            r.setAcceptanceDate(LocalDate.parse((String) body.get("acceptanceDate")));
        } else if (acceptanceResult == AcceptanceResult.已受理) {
            r.setAcceptanceDate(LocalDate.now());
        }

        if (acceptanceResult == AcceptanceResult.已受理) {
            r.setHandlingStatus(HandlingStatus.待分配);
        }

        return ResponseEntity.ok(legalAidReferralRepository.save(r));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<LegalAidReferral> opt = legalAidReferralRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LegalAidReferral r = opt.get();

        if (body.get("legalAidCaseNo") != null) {
            r.setLegalAidCaseNo((String) body.get("legalAidCaseNo"));
        }

        if (body.get("assignedLawyer") != null) {
            r.setAssignedLawyer((String) body.get("assignedLawyer"));
            if (r.getHandlingStatus() == HandlingStatus.待分配) {
                r.setHandlingStatus(HandlingStatus.办理中);
            }
        }

        if (body.get("handlingStatus") != null) {
            try {
                r.setHandlingStatus(HandlingStatus.valueOf((String) body.get("handlingStatus")));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (body.get("handlingResult") != null) {
            r.setHandlingResult((String) body.get("handlingResult"));
        }

        return ResponseEntity.ok(legalAidReferralRepository.save(r));
    }
}
