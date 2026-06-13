package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.LawsuitConnection;
import com.mediation.entity.LawsuitConnection.CaseCategory;
import com.mediation.entity.LawsuitConnection.JudgmentResult;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.LawsuitConnectionRepository;
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
@RequestMapping("/api/lawsuit-connections")
@RequiredArgsConstructor
public class LawsuitConnectionController {

    private final LawsuitConnectionRepository lawsuitConnectionRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        String courtName = (String) body.get("courtName");
        String caseCategoryStr = (String) body.get("caseCategory");
        String transferDateStr = (String) body.get("transferDate");

        if (disputeId == null || courtName == null || caseCategoryStr == null || transferDateStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "案件ID、受案法院、案件类别和转诉日期不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.调解失败) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有调解失败的案件才能进行诉调衔接"));
        }

        CaseCategory caseCategory;
        try {
            caseCategory = CaseCategory.valueOf(caseCategoryStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的案件类别"));
        }

        LocalDate transferDate = LocalDate.parse(transferDateStr);

        LawsuitConnection lc = LawsuitConnection.builder()
                .disputeId(disputeId)
                .courtName(courtName)
                .caseCategory(caseCategory)
                .transferDate(transferDate)
                .build();

        if (body.get("courtCaseNo") != null) {
            lc.setCourtCaseNo((String) body.get("courtCaseNo"));
        }

        LawsuitConnection saved = lawsuitConnectionRepository.save(lc);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<LawsuitConnection>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long disputeId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (disputeId != null) {
            List<LawsuitConnection> list = lawsuitConnectionRepository.findByDisputeId(disputeId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            Page<LawsuitConnection> paged = new org.springframework.data.domain.PageImpl<>(
                    start > list.size() ? Collections.emptyList() : list.subList(start, end),
                    pageable, list.size());
            return ResponseEntity.ok(paged);
        }

        return ResponseEntity.ok(lawsuitConnectionRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return lawsuitConnectionRepository.findById(id)
                .map(lc -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", lc.getId());
                    response.put("disputeId", lc.getDisputeId());
                    response.put("transferDate", lc.getTransferDate());
                    response.put("courtName", lc.getCourtName());
                    response.put("caseCategory", lc.getCaseCategory());
                    response.put("courtCaseNo", lc.getCourtCaseNo());
                    response.put("judgmentResult", lc.getJudgmentResult());
                    response.put("closingDate", lc.getClosingDate());
                    response.put("createdAt", lc.getCreatedAt());
                    response.put("updatedAt", lc.getUpdatedAt());

                    disputeRepository.findById(lc.getDisputeId()).ifPresent(d -> {
                        response.put("caseNo", d.getCaseNo());
                        response.put("applicantName", d.getApplicantName());
                        response.put("respondentName", d.getRespondentName());
                        response.put("mediationResult", d.getResult());
                    });

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/fill-case-no")
    public ResponseEntity<?> fillCourtCaseNo(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String courtCaseNo = body.get("courtCaseNo");
        if (courtCaseNo == null || courtCaseNo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "法院案号不能为空"));
        }

        Optional<LawsuitConnection> opt = lawsuitConnectionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LawsuitConnection lc = opt.get();
        lc.setCourtCaseNo(courtCaseNo);
        return ResponseEntity.ok(lawsuitConnectionRepository.save(lc));
    }

    @PutMapping("/{id}/judgment-result")
    public ResponseEntity<?> setJudgmentResult(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String resultStr = (String) body.get("judgmentResult");
        String closingDateStr = (String) body.get("closingDate");

        if (resultStr == null || closingDateStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "判决结果和结案日期不能为空"));
        }

        JudgmentResult judgmentResult;
        try {
            judgmentResult = JudgmentResult.valueOf(resultStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的判决结果"));
        }

        Optional<LawsuitConnection> opt = lawsuitConnectionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LawsuitConnection lc = opt.get();
        lc.setJudgmentResult(judgmentResult);
        lc.setClosingDate(LocalDate.parse(closingDateStr));
        return ResponseEntity.ok(lawsuitConnectionRepository.save(lc));
    }
}
