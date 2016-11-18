package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.PledgeEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PledgeRepository extends JpaRepository<PledgeEntity, Long> {
    List<PledgeEntity> findByProjectAndFinancingRound(ProjectEntity projectEntity, FinancingRoundEntity financingRoundEntity);
    List<PledgeEntity> findByFinancingRound(FinancingRoundEntity financingRoundEntity);
    List<PledgeEntity> findByFinancingRoundAndCreatedDateGreaterThan(FinancingRoundEntity financingRoundEntity, DateTime createdDate);
}
