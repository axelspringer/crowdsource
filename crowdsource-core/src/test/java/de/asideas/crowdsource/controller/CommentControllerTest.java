package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.presentation.Comment;
import de.asideas.crowdsource.domain.presentation.project.Project;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.repository.CommentRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CommentControllerTest.Config.class)
public class CommentControllerTest {

    public static final String EXISTING_USER_MAIL = "test.name@test.de";
    public static final String NON_EXISTING_USER_MAIL = "i_dont_exist@test.de";
    private final static String EXISTING_PROJECT_ID = "TEST_PROJECT_ID";
    private final static String NON_EXISTING_PROJECT_ID = "I_DONT_EXIST";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Resource
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void init() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        reset(projectRepository);
        reset(commentRepository);
        reset(userRepository);

        final UserEntity userEntity = new UserEntity("test.name@test.de", "password");
        final ProjectEntity projectEntity = new ProjectEntity(userEntity, new Project(), new FinancingRoundEntity());
        when(userRepository.findByEmail(EXISTING_USER_MAIL)).thenReturn(userEntity);
        when(projectRepository.findOne(EXISTING_PROJECT_ID)).thenReturn(projectEntity);
        when(commentRepository.findByProject(projectEntity)).thenReturn(Collections.singletonList(new CommentEntity(projectEntity, userEntity, "some comment")));
    }

    @Test
    public void testComments() throws Exception {

        final MvcResult mvcResult = mockMvc.perform(get("/project/" + EXISTING_PROJECT_ID + "/comments"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), is("[{\"created\":null,\"userName\":\"Test Name\",\"comment\":\"some comment\"}]"));

        verify(projectRepository).findOne(EXISTING_PROJECT_ID);
        verify(commentRepository).findByProject(any(ProjectEntity.class));
    }

    @Test
    public void testStoreCommentValidComment() throws Exception {

        Comment comment = new Comment();
        comment.setComment("message");
        mockMvc.perform(post("/project/" + EXISTING_PROJECT_ID + "/comment")
                .principal(new UsernamePasswordAuthenticationToken(EXISTING_USER_MAIL, "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(comment)))
                .andExpect(status().isCreated());

        verify(projectRepository).findOne(EXISTING_PROJECT_ID);
        verify(userRepository).findByEmail(EXISTING_USER_MAIL);
        verify(commentRepository).save(any(CommentEntity.class));
    }

    @Test
    public void testStoreInvalidComment() throws Exception {

        Comment comment = new Comment();
        comment.setComment("");

        final MvcResult mvcResult = mockMvc.perform(post("/project/" + EXISTING_PROJECT_ID + "/comment")
                .principal(new UsernamePasswordAuthenticationToken(EXISTING_USER_MAIL, "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(comment)))
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), is("{\"errorCode\":\"field_errors\",\"fieldViolations\":{\"comment\":\"may not be empty\"}}"));

        verify(projectRepository, never()).findOne(EXISTING_PROJECT_ID);
        verify(userRepository, never()).findByEmail(EXISTING_USER_MAIL);
        verify(commentRepository, never()).save(any(CommentEntity.class));
    }

    @Test
    public void testStoreCommentInvalidProjectId() throws Exception {

        Comment comment = new Comment();
        comment.setComment("this is an example comment that respects the length constraint");
        mockMvc.perform(post("/project/" + NON_EXISTING_PROJECT_ID + "/comment")
                .principal(new UsernamePasswordAuthenticationToken(EXISTING_USER_MAIL, "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(comment)))
                .andExpect(status().isNotFound());

        verify(projectRepository).findOne(NON_EXISTING_PROJECT_ID);
        verify(userRepository, never()).findByEmail(EXISTING_USER_MAIL);
        verify(commentRepository, never()).save(any(CommentEntity.class));
    }

    @Test
    public void testStoreCommentUserNotFound() throws Exception {

        Comment comment = new Comment();
        comment.setComment("this is an example comment that respects the length constraint");
        mockMvc.perform(post("/project/" + EXISTING_PROJECT_ID + "/comment")
                .principal(new UsernamePasswordAuthenticationToken(NON_EXISTING_USER_MAIL, "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(comment)))
                .andExpect(status().isUnauthorized());

        verify(projectRepository).findOne(EXISTING_PROJECT_ID);
        verify(userRepository).findByEmail(NON_EXISTING_USER_MAIL);
        verify(commentRepository, never()).save(any(CommentEntity.class));
    }


    @Configuration
    @EnableWebMvc
    static class Config {

        @Bean
        public ControllerExceptionAdvice controllerExceptionAdvice() {
            return new ControllerExceptionAdvice();
        }

        @Bean
        public CommentController commentController() {
            return new CommentController();
        }

        @Bean
        public CommentRepository commentRepository() {
            return mock(CommentRepository.class);
        }

        @Bean
        public ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public UserNotificationService userNotificationService() {
            return mock(UserNotificationService.class);
        }

        @Bean
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        public UserService userService(UserRepository userRepository, UserNotificationService userNotificationService) {
            return new UserService(userRepository, userNotificationService);
        }

        @Bean
        public Expression activationEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression newProjectEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression passwordForgottenEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectPublishedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectRejectedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectDeferredEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectModifiedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public AsyncTaskExecutor taskExecutorSmtp() {
            return mock(AsyncTaskExecutor.class);
        }
    }
}