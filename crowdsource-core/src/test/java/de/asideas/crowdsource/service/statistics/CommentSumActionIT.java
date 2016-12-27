package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.AbstractIT;
import de.asideas.crowdsource.Mocks;
import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.repository.CommentRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Ignore //fixme
public class CommentSumActionIT extends AbstractIT {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentSumAction commentSumAction;

    private UserEntity commentedBy;
    private ProjectEntity commentCreatedFor;

    @Test
    @Transactional
    public void getSumComments_should_returns_same_as_count_per_day_sorted_by_day_ASC() throws Exception {

        commentedBy = userRepository.save(Mocks.user("user@email.com"));
        commentCreatedFor = projectRepository.save(Mocks.projectEntity(commentedBy));

        final DateTime today = DateTime.now();
        final DateTime yesterday = today.minusDays(1);
        final DateTime twoDaysBefore = today.minusDays(2);
        final DateTime threeDaysBefore = today.minusDays(3);

        createComments(1, threeDaysBefore);
        createComments(1, twoDaysBefore);
        createComments(1, yesterday);

        LineChartStatisticsResult result = commentSumAction.getSumComments(
                new TimeRangedStatisticsRequest(threeDaysBefore.withTimeAtStartOfDay(),
                        yesterday.millisOfDay().withMaximumValue()));

        assertThat(result.getData(), hasSize(3));

        LineChartStatisticsResult.LineChartEntry resultThreeDaysAgo = result.getData().get(0);
        LineChartStatisticsResult.LineChartEntry resultTwoDaysAgo = result.getData().get(1);
        LineChartStatisticsResult.LineChartEntry resultYesterday = result.getData().get(2);

        assertThat(resultThreeDaysAgo.getLabel(), is(StatisticsActionUtil.formatDate(threeDaysBefore)));
        assertThat(resultTwoDaysAgo.getLabel(), is(StatisticsActionUtil.formatDate(twoDaysBefore)));
        assertThat(resultYesterday.getLabel(), is(StatisticsActionUtil.formatDate(yesterday)));

        assertThat(resultThreeDaysAgo.getData(), is(1L));
        assertThat(resultTwoDaysAgo.getData(), is(1L));
        assertThat(resultYesterday.getData(), is(1L));
    }

    private void createComments(int count, DateTime dateTime) {
        for (int i = 0; i < count; i++) {
            final CommentEntity saved = commentRepository.save(Mocks.commentEntity(commentCreatedFor, commentedBy));
            // Explicitly overcome auditing enabled for dates
            saved.setCreatedDate(dateTime);
            commentRepository.save(saved);
        }
    }

}