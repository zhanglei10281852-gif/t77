package com.mediation.repository;

import com.mediation.entity.CourtReferral;
import com.mediation.entity.CourtReferral.MediationResult;
import com.mediation.entity.CourtReferral.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourtReferralRepository extends JpaRepository<CourtReferral, Long> {

    Optional<CourtReferral> findByDisputeId(Long disputeId);

    List<CourtReferral> findByStatus(ReferralStatus status);

    long countByStatus(ReferralStatus status);

    @Query("SELECT COUNT(cr) FROM CourtReferral cr WHERE cr.mediationResult = '调解成功'")
    long countSuccessful();

    @Query("SELECT COUNT(cr) FROM CourtReferral cr WHERE cr.mediationResult IS NOT NULL")
    long countWithResult();

    long countByMediationResult(MediationResult mediationResult);
}
