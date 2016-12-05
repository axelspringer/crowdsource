package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.AbstractIT;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static de.asideas.crowdsource.domain.shared.ProjectStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProjectRepositoryIT extends AbstractIT {

    private static final Logger log = LoggerFactory.getLogger(ProjectRepositoryIT.class);
    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    private UserEntity projectCreator;

    @Before
    public void init() {
        if (projectCreator == null) {
            List<UserEntity> allUsers = userRepository.findAll();
            if (! allUsers.isEmpty()) {
                projectCreator = allUsers.get(0);
            } else {
                projectCreator = userRepository.save(new UserEntity("test@crowdsource.de", "firstname", "lastname"));
            }
        }
    }

    @Test
    public void sumProjectsGroupedByStatus_aggregation_returns_uptodate_data() {
        // Actually the aggregation should return same results as simply doing a count query for each separate status.
        Map<ProjectStatus, Long> initialCountsByStatus = getProjectCountsByStatus();

        int addProposed = 30;
        int addPledged = 10;
        int addPublished = 50;

        givenProjectsWithStatus(addProposed, PROPOSED);
        givenProjectsWithStatus(addPledged, FULLY_PLEDGED);
        givenProjectsWithStatus(addPublished, PUBLISHED);

        log.info("|-- ALL PROJECTS CURRENTLY IN REPO: ");
        projectRepository.findAll().stream().forEach(e -> log.info(" -- " + e));

        List<Object[]> aggregationResult = projectRepository.findProjectGroupByStatus();

        expectAggregatedCount(aggregationResult, PROPOSED, initialCountsByStatus.get(PROPOSED) + addProposed);
        expectAggregatedCount(aggregationResult, PUBLISHED, initialCountsByStatus.get(PUBLISHED) + addPublished);
        expectAggregatedCount(aggregationResult, FULLY_PLEDGED, initialCountsByStatus.get(FULLY_PLEDGED) + addPledged);

    }

    @Test
    public void sumProjectsGroupedByStatus_aggregation_returns_unchanged_data() {
        // Assure at least 1 project exists in db for each status to test.
        givenProjectsWithStatus(1, PROPOSED);
        givenProjectsWithStatus(1, FULLY_PLEDGED);
        givenProjectsWithStatus(1, PUBLISHED);

        // Actually the aggregation should return same results as simply doing a count query for each separate status.
        Map<ProjectStatus, Long> initialCountsByStatus = getProjectCountsByStatus();

        List<Object[]> aggregationResult = projectRepository.findProjectGroupByStatus();

        expectAggregatedCount(aggregationResult, PROPOSED, initialCountsByStatus.get(PROPOSED));
        expectAggregatedCount(aggregationResult, PUBLISHED, initialCountsByStatus.get(PUBLISHED));
        expectAggregatedCount(aggregationResult, FULLY_PLEDGED, initialCountsByStatus.get(FULLY_PLEDGED));
    }

    private void expectAggregatedCount(List<Object[]> aggregationResult, ProjectStatus statusToTest, long expectedCount) {
        assertThat(
                aggregationResult.stream().filter(r -> statusToTest.equals(r[0])).findFirst().get()[1],
                is(expectedCount)
        );
    }

    private Map<ProjectStatus, Long> getProjectCountsByStatus() {
        EnumMap<ProjectStatus, Long> result = new EnumMap<>(ProjectStatus.class);

        for (ProjectStatus status : ProjectStatus.values()) {
            result.put(status, (long) projectRepository.findByStatusOrderByCreatedDateDesc(status).size());
        }
        return result;
    }

    private void givenProjectsWithStatus(int desiredProjectCount, ProjectStatus desiredStatus) {

        for (int i = 0; i < desiredProjectCount; i++) {
            ProjectEntity project = new ProjectEntity();
            project.setCreator(projectCreator);
            project.setTitle("project from ProjectRepositoryIT idx" + i);
            project.setDescription("project from ProjectRepositoryIT idx" + i);
            project.setPledgeGoal(BigDecimal.valueOf(1000));
            project.setStatus(desiredStatus);

            projectRepository.save(project);
        }
    }
}
