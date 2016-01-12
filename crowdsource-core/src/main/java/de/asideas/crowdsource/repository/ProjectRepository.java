package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.joda.time.DateTime;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ProjectRepository extends MongoRepository<ProjectEntity, String> {

    List<ProjectEntity> findByStatusOrderByCreatedDateDesc(ProjectStatus projectStatus);

    List<ProjectEntity> findByFinancingRound(FinancingRoundEntity financingRound);

    List<ProjectEntity> findByCreatedDateBetween(DateTime startDate, DateTime endDate);


}
