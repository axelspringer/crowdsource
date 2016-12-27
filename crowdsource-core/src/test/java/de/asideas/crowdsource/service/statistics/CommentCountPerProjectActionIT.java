package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.AbstractIT;
import de.asideas.crowdsource.Mocks;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.repository.CommentRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@Ignore //fixme
public class CommentCountPerProjectActionIT extends AbstractIT {

    @Autowired
    private CommentCountPerProjectAction commentCountPerProjectAction;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    public void getCommentCountPerProjectStatistic_should_honor_project_count() throws Exception {
        final UserEntity userEntity = userRepository.save(Mocks.user("user@email.com"));
        final ProjectEntity projectEntity1 = projectRepository.save(Mocks.projectEntity(userEntity));
        final ProjectEntity projectEntity2 = projectRepository.save(Mocks.projectEntity(userEntity));

        IntStream.range(0, 3).forEach(i -> commentRepository.save(Mocks.commentEntity(projectEntity1, userEntity)));
        IntStream.range(0, 5).forEach(i -> commentRepository.save(Mocks.commentEntity(projectEntity2, userEntity)));

        final List<BarChartStatisticsResult> commentCountPerProjectStatistic = commentCountPerProjectAction.getCommentCountPerProjectStatistic(3);

        assertThat(commentCountPerProjectStatistic, hasSize(2));
    }

}