package com.mediation.repository;

import com.mediation.entity.LegalAidReferral;
import com.mediation.entity.LegalAidReferral.AcceptanceResult;
import com.mediation.entity.LegalAidReferral.HandlingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalAidReferralRepository extends JpaRepository<LegalAidReferral, Long> {

    List<LegalAidReferral> findByDisputeId(Long disputeId);

    Optional<LegalAidReferral> findByDisputeIdAndId(Long disputeId, Long id);

    long countByAcceptanceResult(AcceptanceResult acceptanceResult);

    long countByHandlingStatus(HandlingStatus handlingStatus);
}
