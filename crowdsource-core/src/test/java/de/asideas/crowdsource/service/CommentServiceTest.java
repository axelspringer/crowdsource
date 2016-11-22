package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.Comment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.repository.CommentRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

    public static final String EXISTING_USER_MAIL = "test.name@test.de";
    private final static Long EXISTING_PROJECT_ID = 123L;

    private ProjectEntity projectEntity;
    private UserEntity userEntity;
    private CommentEntity aComment;

    @InjectMocks
    private CommentService commentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private UserService userService;

    @Before
    public void init(){
        reset(projectService, userService, commentRepository);
        Project project = new Project();
        projectEntity = new ProjectEntity(project.getTitle(), project.getShortDescription(), project.getDescription(), project.getPledgeGoal(), new FinancingRoundEntity(), userEntity);
        userEntity = new UserEntity("test.name@test.de", "firstname", "lastname");
        aComment = new CommentEntity(projectEntity, "some comment", userEntity);
    }

    @Test
    public void addComment_ShouldPersistCommentAndNotifyCreator() throws Exception {
        Comment comment = new Comment(null, null, "Awesome project, dude!");

        prepareProjectServiceMock();
        prepareUserServiceMock();

        userEntity = new UserEntity("test.name@test.de", "firstname", "lastname");

        commentService.addComment(comment, EXISTING_PROJECT_ID, "test.name@test.de");

        final CommentEntity expectedComment2BPersisted = new CommentEntity(projectEntity, comment.getComment(), userEntity);
        verify(commentRepository).save(expectedComment2BPersisted);
        verify(userNotificationService).notifyCreatorOnComment(expectedComment2BPersisted);
    }

    @Test
    public void loadCommentsByProject_ShouldReturnViewRepresentation() throws Exception {
        prepareProjectServiceMock();
        prepareCommentRepositoryMock();

        final List<Comment> res = commentService.loadCommentsByProject(EXISTING_PROJECT_ID);

        assertThat(res.size(), is(1));
        assertThat(res.get(0), is(new Comment(aComment)));
    }

    private void prepareUserServiceMock() {
        when(userService.getUserByEmail(EXISTING_USER_MAIL)).thenReturn(userEntity);
    }

    private void prepareCommentRepositoryMock() {
        when(commentRepository.findByProject(projectEntity)).thenReturn(Collections.singletonList(aComment));
    }

    private void prepareProjectServiceMock() {
        when(projectService.loadProjectEntity(EXISTING_PROJECT_ID)).thenReturn(projectEntity);
    }
}