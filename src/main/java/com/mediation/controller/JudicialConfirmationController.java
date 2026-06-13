package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.JudicialConfirmation;
import com.mediation.entity.JudicialConfirmation.RulingResult;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.JudicialConfirmationRepository;
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
@RequestMapping("/api/judicial-confirmations")
@RequiredArgsConstructor
public class JudicialConfirmationController {

    private final JudicialConfirmationRepository judicialConfirmationRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        String agreementNo = (String) body.get("agreementNo");
        String courtName = (String) body.get("courtName");
        String applicationDateStr = (String) body.get("applicationDate");

        if (disputeId == null || agreementNo == null || courtName == null || applicationDateStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "案件ID、协议编号、申请法院、申请日期均不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        JudicialConfirmation jc = JudicialConfirmation.builder()
                .disputeId(disputeId)
                .agreementNo(agreementNo)
                .courtName(courtName)
                .applicationDate(LocalDate.parse(applicationDateStr))
                .remark(body.get("remark") != null ? (String) body.get("remark") : null)
                .build();

        JudicialConfirmation saved = judicialConfirmationRepository.save(jc);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<JudicialConfirmation>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long disputeId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (disputeId != null) {
            List<JudicialConfirmation> list = judicialConfirmationRepository.findByDisputeId(disputeId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            Page<JudicialConfirmation> paged = new org.springframework.data.domain.PageImpl<>(
                    start > list.size() ? Collections.emptyList() : list.subList(start, end),
                    pageable, list.size());
            return ResponseEntity.ok(paged);
        }

        return ResponseEntity.ok(judicialConfirmationRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return judicialConfirmationRepository.findById(id)
                .map(jc -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", jc.getId());
                    response.put("disputeId", jc.getDisputeId());
                    response.put("agreementNo", jc.getAgreementNo());
                    response.put("courtName", jc.getCourtName());
                    response.put("applicationDate", jc.getApplicationDate());
                    response.put("rulingResult", jc.getRulingResult());
                    response.put("rulingDate", jc.getRulingDate());
                    response.put("rulingNo", jc.getRulingNo());
                    response.put("remark", jc.getRemark());
                    response.put("createdAt", jc.getCreatedAt());
                    response.put("updatedAt", jc.getUpdatedAt());

                    disputeRepository.findById(jc.getDisputeId()).ifPresent(d -> {
                        response.put("caseNo", d.getCaseNo());
                        response.put("applicantName", d.getApplicantName());
                        response.put("respondentName", d.getRespondentName());
                        response.put("disputeResult", d.getResult());
                    });

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/ruling")
    public ResponseEntity<?> setRuling(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String resultStr = (String) body.get("rulingResult");
        String rulingDateStr = (String) body.get("rulingDate");
        String rulingNo = (String) body.get("rulingNo");

        if (resultStr == null || rulingDateStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "裁定结果和裁定日期不能为空"));
        }

        RulingResult rulingResult;
        try {
            rulingResult = RulingResult.valueOf(resultStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的裁定结果"));
        }

        Optional<JudicialConfirmation> opt = judicialConfirmationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        JudicialConfirmation jc = opt.get();
        jc.setRulingResult(rulingResult);
        jc.setRulingDate(LocalDate.parse(rulingDateStr));
        if (rulingNo != null) {
            jc.setRulingNo(rulingNo);
        }
        return ResponseEntity.ok(judicialConfirmationRepository.save(jc));
    }
}
