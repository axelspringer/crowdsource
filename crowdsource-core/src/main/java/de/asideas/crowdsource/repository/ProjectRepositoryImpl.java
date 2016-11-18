package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;

public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Autowired
    public ProjectRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<BarChartStatisticsResult> sumProjectsGroupedByStatus() {

        return Collections.emptyList();
        // fixme
//        AggregationResults<ProjectPerStatusResult> aggregationResults = mongoTemplate.aggregate(
//                newAggregation(
//                        ProjectEntity.class,
//                        group("status").count().as("count")
//                ), ProjectPerStatusResult.class);
//
//        return aggregationResults.getMappedResults().stream()
//                .map(res -> new BarChartStatisticsResult(res.getId().name(), res.getId().getDisplayName(), res.getCount()))
//                .collect(Collectors.toList());
    }

    static class ProjectPerStatusResult {

        private ProjectStatus id;
        private Long count;

        private ProjectPerStatusResult() {
        }

        public ProjectPerStatusResult(ProjectStatus id, Long count) {
            this.id = id;
            this.count = count;
        }

        public ProjectStatus getId() {
            return id;
        }

        public Long getCount() {
            return count;
        }
    }

}
