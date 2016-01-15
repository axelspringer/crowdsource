package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import org.joda.time.DateTime;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectRepository extends MongoRepository<ProjectEntity, String>, ProjectRepositoryCustom {

    List<ProjectEntity> findByFinancingRound(FinancingRoundEntity financingRound);

    List<ProjectEntity> findByCreatedDateBetween(DateTime startDate, DateTime endDate);


}
