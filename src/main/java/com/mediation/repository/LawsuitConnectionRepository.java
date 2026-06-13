package com.mediation.repository;

import com.mediation.entity.LawsuitConnection;
import com.mediation.entity.LawsuitConnection.JudgmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawsuitConnectionRepository extends JpaRepository<LawsuitConnection, Long> {

    List<LawsuitConnection> findByDisputeId(Long disputeId);

    Optional<LawsuitConnection> findByDisputeIdAndId(Long disputeId, Long id);

    long countByJudgmentResult(JudgmentResult judgmentResult);

    @Query("SELECT COUNT(lc) FROM LawsuitConnection lc WHERE lc.judgmentResult IS NOT NULL")
    long countClosed();
}
