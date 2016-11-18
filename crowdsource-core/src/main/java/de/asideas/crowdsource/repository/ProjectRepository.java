package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, ProjectRepositoryCustom {

    List<ProjectEntity> findByStatusOrderByCreatedDateDesc(ProjectStatus projectStatus);

    List<ProjectEntity> findByFinancingRound(FinancingRoundEntity financingRound);

    List<ProjectEntity> findByCreatedDateBetween(DateTime startDate, DateTime endDate);


}
