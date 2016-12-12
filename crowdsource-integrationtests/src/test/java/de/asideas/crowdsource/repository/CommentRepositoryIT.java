package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.AbstractIT;
import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.service.statistics.CommentSumAction;
import de.asideas.crowdsource.service.statistics.StatisticsActionUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CommentRepositoryIT extends AbstractIT {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentSumAction commentSumAction;

    @PersistenceContext
    private EntityManager entityManager;

    private UserEntity commentedBy;
    private ProjectEntity commentCreatedFor;

    @Before
    public void init() {
        // Setup test environment
        if (commentedBy == null) {
            List<UserEntity> allUsers = userRepository.findAll();
            if (! allUsers.isEmpty()) {
                commentedBy = allUsers.get(0);
            } else {
                commentedBy = userRepository.save(new UserEntity("test@crowdsource.de", "firstname", "lastname"));
            }
        }
        if (commentCreatedFor == null) {
            List<ProjectEntity> allProjects = projectRepository.findAll();
            if (! allProjects.isEmpty()) {
                commentCreatedFor = allProjects.get(0);
            } else {
                ProjectEntity projectEntity = new ProjectEntity();
                projectEntity.setCreator(commentedBy);
                projectEntity.setPledgeGoal(BigDecimal.valueOf(100));
                projectEntity.setDescription("abc");
                projectEntity.setTitle("abc");
                commentCreatedFor = projectRepository.save(projectEntity);
            }
        }
    }

    @Test
    public void sumCommentsGroupByCreatedDate_returns_same_as_count_per_day_sorted_by_day_ASC() {

        DateTime today = DateTime.now(DateTimeZone.UTC);
        DateTime dayBefore = today.minusDays(1);
        DateTime twoDaysBefore = today.minusDays(2);

        long commentsCreatedAtThisDay = getCountCommentsAtDayOf(today);
        long commentsCreatedAtTheDayBefore = getCountCommentsAtDayOf(dayBefore);
        long commentsCreatedAtTwoDaysAgo = getCountCommentsAtDayOf(twoDaysBefore);

        createComments(5, today);
        createComments(3, dayBefore);
        createComments(1, twoDaysBefore);

        LineChartStatisticsResult result = commentSumAction.getSumComments(
                new TimeRangedStatisticsRequest(twoDaysBefore.withTimeAtStartOfDay(),
                today.plusDays(1).withTimeAtStartOfDay()));

        LineChartStatisticsResult.LineChartEntry resultTwoDaysAgo = result.getData().get(0);
        LineChartStatisticsResult.LineChartEntry resultOneDayAgo = result.getData().get(1);
        LineChartStatisticsResult.LineChartEntry resultToday = result.getData().get(2);

        assertThat(resultTwoDaysAgo.getLabel(), is(StatisticsActionUtil.formatDate(twoDaysBefore)));
        assertThat(resultTwoDaysAgo.getData(), is(commentsCreatedAtTwoDaysAgo + 1));
        assertThat(resultOneDayAgo.getLabel(), is(StatisticsActionUtil.formatDate(dayBefore)));
        assertThat(resultOneDayAgo.getData(), is(commentsCreatedAtTheDayBefore + 3));
        assertThat(resultToday.getLabel(), is(StatisticsActionUtil.formatDate(today)));
        assertThat(resultToday.getData(), is(commentsCreatedAtThisDay + 5));
    }

    @Test
    public void sumCommentsGroupByCreatedDate_should_respect_timerange_bounds_inclusive () {

        DateTime today = DateTime.now(DateTimeZone.UTC);
        DateTime dayBefore = today.minusDays(1);
        DateTime twoDaysBefore = today.minusDays(2);
        DateTime threeDaysBefore = today.minusDays(3);

        long commentsCreatedAt1DayAgo = getCountCommentsAtDayOf(dayBefore);
        long commentsCreatedAt2DaysAgo = getCountCommentsAtDayOf(twoDaysBefore);

        createComments(5, today);
        createComments(3, dayBefore);
        createComments(1, twoDaysBefore);
        createComments(17, threeDaysBefore);

        LineChartStatisticsResult result = commentSumAction.getSumComments(
                new TimeRangedStatisticsRequest(twoDaysBefore.withTimeAtStartOfDay(),
                twoDaysBefore.plusDays(2).withTimeAtStartOfDay()));

        LineChartStatisticsResult.LineChartEntry resultTwoDaysAgo = result.getData().get(0);
        LineChartStatisticsResult.LineChartEntry resultOneDayAgo = result.getData().get(1);

        assertThat(resultTwoDaysAgo.getLabel(), is(StatisticsActionUtil.formatDate(twoDaysBefore)));
        assertThat(resultTwoDaysAgo.getData(), is(commentsCreatedAt2DaysAgo + 1));
        assertThat(resultOneDayAgo.getLabel(), is(StatisticsActionUtil.formatDate(dayBefore)));
        assertThat(resultOneDayAgo.getData(), is(commentsCreatedAt1DayAgo + 3));
    }

    private void createComments(int count, DateTime dateTime) {
        for (int i = 0; i < count; i++) {
            CommentEntity entity = new CommentEntity();
            entity.setComment("HELLO");
            entity.setProject(commentCreatedFor);
            entity.setCreator(commentedBy);
            CommentEntity saved = commentRepository.save(entity);
            // Explicitly overcome auditing enabled for dates
            saved.setCreatedDate(dateTime);
            commentRepository.save(saved);
        }
    }

    private long getCountCommentsAtDayOf(DateTime date) {
        DateTime start = date.withTimeAtStartOfDay();
        DateTime end = date.plusDays(1).withTimeAtStartOfDay();
        return entityManager.createQuery("COUNT(c.id) from CommentEntity c where c.createdDate > ?1 AND c.createDate < ?2", Long.class).setParameter(1, start).setParameter(2, end).getSingleResult();
    }

}