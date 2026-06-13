package com.mediation.controller;

import com.mediation.entity.CourtReferral.MediationResult;
import com.mediation.entity.CourtReferral.ReferralStatus;
import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.SourceChannel;
import com.mediation.entity.LawsuitConnection.JudgmentResult;
import com.mediation.entity.LegalAidReferral.AcceptanceResult;
import com.mediation.repository.CourtReferralRepository;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.JudicialConfirmationRepository;
import com.mediation.repository.LawsuitConnectionRepository;
import com.mediation.repository.LegalAidReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DisputeRepository disputeRepository;
    private final LawsuitConnectionRepository lawsuitConnectionRepository;
    private final CourtReferralRepository courtReferralRepository;
    private final LegalAidReferralRepository legalAidReferralRepository;
    private final JudicialConfirmationRepository judicialConfirmationRepository;

    @GetMapping("/lawsuit-connection-rate")
    public ResponseEntity<?> lawsuitConnectionRate() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long mediationFailedCount = disputeRepository.countMediationFailed();
        long lawsuitConnectionCount = lawsuitConnectionRepository.count();
        long lawsuitClosedCount = lawsuitConnectionRepository.countClosed();

        double rate = mediationFailedCount > 0
                ? Math.round((double) lawsuitConnectionCount / mediationFailedCount * 10000.0) / 100.0
                : 0.0;

        stats.put("mediationFailedTotal", mediationFailedCount);
        stats.put("lawsuitConnectionTotal", lawsuitConnectionCount);
        stats.put("lawsuitConnectionRate", rate + "%");
        stats.put("lawsuitClosedCount", lawsuitClosedCount);

        Map<String, Long> judgmentResults = new LinkedHashMap<>();
        for (JudgmentResult jr : JudgmentResult.values()) {
            judgmentResults.put(jr.name(), lawsuitConnectionRepository.countByJudgmentResult(jr));
        }
        stats.put("judgmentResultBreakdown", judgmentResults);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/court-referral")
    public ResponseEntity<?> courtReferralStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalReferrals = courtReferralRepository.count();
        long pendingCount = courtReferralRepository.countByStatus(ReferralStatus.待接收);
        long inMediationCount = courtReferralRepository.countByStatus(ReferralStatus.调解中);
        long feedbackCount = courtReferralRepository.countByStatus(ReferralStatus.已反馈);
        long withResultCount = courtReferralRepository.countWithResult();
        long successCount = courtReferralRepository.countSuccessful();

        double successRate = withResultCount > 0
                ? Math.round((double) successCount / withResultCount * 10000.0) / 100.0
                : 0.0;

        stats.put("totalReferrals", totalReferrals);
        stats.put("pendingCount", pendingCount);
        stats.put("inMediationCount", inMediationCount);
        stats.put("feedbackCount", feedbackCount);
        stats.put("withResultCount", withResultCount);
        stats.put("successCount", successCount);
        stats.put("successRate", successRate + "%");

        Map<String, Long> resultBreakdown = new LinkedHashMap<>();
        for (MediationResult mr : MediationResult.values()) {
            resultBreakdown.put(mr.name(), courtReferralRepository.countByMediationResult(mr));
        }
        stats.put("resultBreakdown", resultBreakdown);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/legal-aid")
    public ResponseEntity<?> legalAidStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalReferrals = legalAidReferralRepository.count();
        long acceptedCount = legalAidReferralRepository.countByAcceptanceResult(AcceptanceResult.已受理);
        long rejectedCount = legalAidReferralRepository.countByAcceptanceResult(AcceptanceResult.不予受理);

        long withResultCount = acceptedCount + rejectedCount;
        double acceptanceRate = withResultCount > 0
                ? Math.round((double) acceptedCount / withResultCount * 10000.0) / 100.0
                : 0.0;

        stats.put("totalReferrals", totalReferrals);
        stats.put("acceptedCount", acceptedCount);
        stats.put("rejectedCount", rejectedCount);
        stats.put("pendingDecisionCount", totalReferrals - withResultCount);
        stats.put("acceptanceRate", acceptanceRate + "%");

        Map<String, Long> handlingBreakdown = new LinkedHashMap<>();
        for (com.mediation.entity.LegalAidReferral.HandlingStatus hs :
                com.mediation.entity.LegalAidReferral.HandlingStatus.values()) {
            handlingBreakdown.put(hs.name(), legalAidReferralRepository.countByHandlingStatus(hs));
        }
        stats.put("handlingBreakdown", handlingBreakdown);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/source-channel")
    public ResponseEntity<?> sourceChannelStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = disputeRepository.count();
        Map<String, Object> channelStats = new LinkedHashMap<>();

        for (SourceChannel sc : SourceChannel.values()) {
            long count = disputeRepository.countBySourceChannel(sc);
            double percentage = total > 0
                    ? Math.round((double) count / total * 10000.0) / 100.0
                    : 0.0;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("count", count);
            detail.put("percentage", percentage + "%");
            channelStats.put(sc.name(), detail);
        }

        stats.put("totalCases", total);
        stats.put("channels", channelStats);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/comprehensive")
    public ResponseEntity<?> comprehensiveStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalCases = disputeRepository.count();
        long mediationSuccess = disputeRepository.countByStatus(DisputeStatus.调解成功);
        long mediationFailed = disputeRepository.countByStatus(DisputeStatus.调解失败);

        stats.put("totalCases", totalCases);
        stats.put("mediationSuccess", mediationSuccess);
        stats.put("mediationFailed", mediationFailed);
        stats.put("sourceChannelStats", sourceChannelStatsInternal());
        stats.put("lawsuitConnectionStats", lawsuitConnectionStatsInternal());
        stats.put("courtReferralStats", courtReferralStatsInternal());
        stats.put("legalAidStats", legalAidStatsInternal());
        stats.put("judicialConfirmationStats", judicialConfirmationStatsInternal());
        stats.put("mediationJudgmentCorrelation", mediationJudgmentCorrelationInternal());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/mediation-judgment-correlation")
    public ResponseEntity<?> mediationJudgmentCorrelation() {
        return ResponseEntity.ok(mediationJudgmentCorrelationInternal());
    }

    private Map<String, Object> sourceChannelStatsInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = disputeRepository.count();
        for (SourceChannel sc : SourceChannel.values()) {
            long count = disputeRepository.countBySourceChannel(sc);
            double pct = total > 0 ? Math.round((double) count / total * 10000.0) / 100.0 : 0.0;
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("count", count);
            d.put("percentage", pct + "%");
            result.put(sc.name(), d);
        }
        return result;
    }

    private Map<String, Object> lawsuitConnectionStatsInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        long failed = disputeRepository.countMediationFailed();
        long conn = lawsuitConnectionRepository.count();
        double rate = failed > 0 ? Math.round((double) conn / failed * 10000.0) / 100.0 : 0.0;
        result.put("mediationFailedTotal", failed);
        result.put("lawsuitConnectionTotal", conn);
        result.put("connectionRate", rate + "%");
        return result;
    }

    private Map<String, Object> courtReferralStatsInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = courtReferralRepository.count();
        long withResult = courtReferralRepository.countWithResult();
        long success = courtReferralRepository.countSuccessful();
        double rate = withResult > 0 ? Math.round((double) success / withResult * 10000.0) / 100.0 : 0.0;
        result.put("totalReferrals", total);
        result.put("withResultCount", withResult);
        result.put("successCount", success);
        result.put("successRate", rate + "%");
        return result;
    }

    private Map<String, Object> legalAidStatsInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = legalAidReferralRepository.count();
        long accepted = legalAidReferralRepository.countByAcceptanceResult(AcceptanceResult.已受理);
        long rejected = legalAidReferralRepository.countByAcceptanceResult(AcceptanceResult.不予受理);
        long withResult = accepted + rejected;
        double rate = withResult > 0 ? Math.round((double) accepted / withResult * 10000.0) / 100.0 : 0.0;
        result.put("totalReferrals", total);
        result.put("acceptedCount", accepted);
        result.put("rejectedCount", rejected);
        result.put("acceptanceRate", rate + "%");
        return result;
    }

    private Map<String, Object> judicialConfirmationStatsInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = judicialConfirmationRepository.count();
        long confirmed = judicialConfirmationRepository.countByRulingResult(
                com.mediation.entity.JudicialConfirmation.RulingResult.确认);
        long notConfirmed = judicialConfirmationRepository.countByRulingResult(
                com.mediation.entity.JudicialConfirmation.RulingResult.不予确认);
        result.put("totalApplications", total);
        result.put("confirmedCount", confirmed);
        result.put("notConfirmedCount", notConfirmed);
        result.put("pendingCount", total - confirmed - notConfirmed);
        return result;
    }

    private Map<String, Object> mediationJudgmentCorrelationInternal() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> analysisList = new ArrayList<>();

        List<com.mediation.entity.LawsuitConnection> allConnections = lawsuitConnectionRepository.findAll();
        int totalAnalyzed = 0;
        int supportMediationDirection = 0;
        int oppositeMediationDirection = 0;
        int otherCases = 0;

        for (com.mediation.entity.LawsuitConnection lc : allConnections) {
            Optional<Dispute> disputeOpt = disputeRepository.findById(lc.getDisputeId());
            if (disputeOpt.isEmpty()) continue;

            Dispute d = disputeOpt.get();
            if (lc.getJudgmentResult() == null) continue;

            totalAnalyzed++;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("caseNo", d.getCaseNo());
            item.put("mediationResult", d.getResult());
            item.put("judgmentResult", lc.getJudgmentResult().name());
            item.put("courtCaseNo", lc.getCourtCaseNo());

            JudgmentResult jr = lc.getJudgmentResult();
            if (jr == JudgmentResult.胜诉 || jr == JudgmentResult.调解结案) {
                supportMediationDirection++;
                item.put("correlation", "倾向支持调解方案方向");
            } else if (jr == JudgmentResult.败诉) {
                oppositeMediationDirection++;
                item.put("correlation", "与调解方案方向相反");
            } else {
                otherCases++;
                item.put("correlation", "撤诉/其他情况");
            }

            analysisList.add(item);
        }

        result.put("totalAnalyzed", totalAnalyzed);
        result.put("supportMediationDirection", supportMediationDirection);
        result.put("oppositeMediationDirection", oppositeMediationDirection);
        result.put("otherCases", otherCases);

        double supportRate = totalAnalyzed > 0
                ? Math.round((double) supportMediationDirection / totalAnalyzed * 10000.0) / 100.0
                : 0.0;
        result.put("supportRate", supportRate + "%");
        result.put("details", analysisList);

        return result;
    }
}
