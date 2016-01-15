package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ProjectRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Map<ProjectStatus, Long> sumProjectsGroupedByStatus() {
        EnumMap<ProjectStatus, Long> result = new EnumMap<>(ProjectStatus.class);

        List<AggregationOperation> operations = Collections.singletonList(group("status").count().as("count"));

        AggregationResults<ProjectPerStatusResult> aggregationResults = mongoTemplate.aggregate(newAggregation(operations), ProjectEntity.class, ProjectPerStatusResult.class);

        aggregationResults.getMappedResults().stream().forEach(p -> result.put(p.getId(), p.getCount()));

        return result;
    }

    public class ProjectPerStatusResult {

        private ProjectStatus id;

        private Long count;

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
