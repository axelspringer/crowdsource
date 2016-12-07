package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.repository.ProjectRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CommentCountPerProjectActionTest {

    @InjectMocks
    private CommentCountPerProjectAction instance;

    @Mock
    private ProjectRepository projectRepository;

    @Test
    public void getCommentCountPerProjectStatistic_should_call_repository_method() {
        instance.getCommentCountPerProjectStatistic(5);

        verify(projectRepository, only()).countCommentsGroupingByProject(new PageRequest(0, 5));
    }

}