package com.mediation.repository;

import com.mediation.entity.JudicialConfirmation;
import com.mediation.entity.JudicialConfirmation.RulingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JudicialConfirmationRepository extends JpaRepository<JudicialConfirmation, Long> {

    List<JudicialConfirmation> findByDisputeId(Long disputeId);

    Optional<JudicialConfirmation> findByDisputeIdAndId(Long disputeId, Long id);

    long countByRulingResult(RulingResult rulingResult);
}
