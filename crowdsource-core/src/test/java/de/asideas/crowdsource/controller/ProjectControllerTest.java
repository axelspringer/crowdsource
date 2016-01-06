package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.presentation.ErrorResponse;
import de.asideas.crowdsource.domain.presentation.Pledge;
import de.asideas.crowdsource.domain.presentation.project.Project;
import de.asideas.crowdsource.domain.presentation.project.ProjectStatusUpdate;
import de.asideas.crowdsource.domain.presentation.user.ProjectCreator;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.repository.LikeRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.ProjectService;
import de.asideas.crowdsource.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ProjectControllerTest.Config.class)
public class ProjectControllerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectService projectService;

    @Resource
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(projectService, userRepository);
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void addProject_shouldReturnSuccessfully() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", 50, ProjectStatus.PROPOSED);
        final Project expFullProjcet = toCreatedProject(project, user);
        when(projectService.addProject(project, user)).thenReturn(expFullProjcet);

        MvcResult mvcResult = mockMvc.perform(post("/project")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(expFullProjcet, is(equalTo(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class))));
    }

    @Test
    public void addProject_shouldRespondWith401IfUserWasNotFound() throws Exception {
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", 50, ProjectStatus.PROPOSED);

        mockMvc.perform(post("/project")
                .principal(new UsernamePasswordAuthenticationToken("foo@bar.de", "somepassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(project)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void addProject_shouldRespondWith400IfRequestWasInvalid() throws Exception {
        final Project project = new Project();
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);

        mockMvc.perform(post("/project")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(project)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void addProject_shouldRespondWith400IfProjectWasMissing() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);

        mockMvc.perform(post("/project")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getProject_shouldRespondWith404OnInvalidProjectId() throws Exception {
        when(projectService.getProject(anyString(), any(UserEntity.class))).thenThrow(new ResourceNotFoundException());
        mockMvc.perform(get("/project/{projectId}", "foo bah bah"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProject_shouldRespondWith403IfTheUserMayNotSeeThisProject() throws Exception {
        final UserEntity userEntity = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        when(projectService.getProject(eq(projectId), eq(userEntity)))
                .thenReturn(toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator));

        mockMvc.perform(get("/project/{projectId}", projectId)
                        .principal(authentication(userEntity))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void getProject_shouldReturnSingleProjectSuccessfullyWhenProjectIsPublished() throws Exception {
        final UserEntity userEntity = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PUBLISHED), creator);

        when(projectService.getProject(eq(projectId), eq(userEntity))).thenReturn(expProjcet);

        MvcResult mvcResult = mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(authentication(userEntity)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(equalTo(expProjcet)));
    }

    @Test
    public void getProject_shouldReturnSingleProjectSuccessfullyWhenProjectIsPublishedForAnonymousToo() throws Exception {
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PUBLISHED), creator);

        when(projectService.getProject(eq(projectId), eq(null))).thenReturn(expProjcet);

        MvcResult mvcResult = mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(anonymousAuthentication()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(equalTo(expProjcet)));
    }

    @Test
    public void getProjects_shouldReturnPublishedProjectsOnly() throws Exception {
        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PUBLISHED), creator);
        final Project unexpProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(user)).thenReturn(Arrays.asList(expProjcet, unexpProjcet));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet);
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class)[0], is(equalTo(expProjcet)));
    }

    @Test
    public void getProjects_shouldReturnPublishedProjectsForAnonymousUsersToo() throws Exception {
        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PUBLISHED), creator);
        final Project unexpProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(null)).thenReturn(Arrays.asList(expProjcet, unexpProjcet));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(anonymousAuthentication()))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet);
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class)[0], is(equalTo(expProjcet)));
    }

    @Test
    public void getProjects_shouldReturnNothingWhenProjectNotPublishedAndProjectCreatorNotRequestor() throws Exception {

        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(user)).thenReturn(Collections.singletonList(expProjcet));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet);
        assertThat(mvcResult.getResponse().getContentAsString(), is("[]"));
    }

    @Test
    public void getProjects_shouldReturnUnpublishedProjectsWhenRequestorIsAdmin() throws Exception {
        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_ADMIN);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(user)).thenReturn(Collections.singletonList(expProjcet));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet);
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class)[0], is(equalTo(expProjcet)));
    }

    @Test
    public void getProjects_shouldReturnUnpublishedProjectWhenRequestorIsCreator() throws Exception {
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final UserEntity anotherCreator = userEntity("creator2@mail.com", Roles.ROLE_USER);
        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", 44, ProjectStatus.PROPOSED), creator);
        final Project nonExpProjcet = toCreatedProject(project("title2", "descr2", "shortDescr2", 45, ProjectStatus.PROPOSED), anotherCreator);
        when(projectService.getProjects(creator)).thenReturn(Arrays.asList(expProjcet, nonExpProjcet));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(authentication(creator)))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet);
        List<Project> projects = Arrays.asList(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class));
        assertThat(projects.size(), is(1));
        assertThat("Result should contain creator's proposed project", projects.contains(expProjcet));
    }

    @Test
    public void pledgeProject() throws Exception {
        final String email = "some@mail.com";
        final String projectId = "some_id";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        Pledge pledge = new Pledge(13);

        mockMvc.perform(post("/project/{projectId}/pledges", projectId)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(pledge)))
                .andExpect(status().isCreated());

        verify(projectService).pledge(projectId, user, pledge);
    }

    @Test
    public void pledgeProject_shouldRespondWith401IfTheUserWasNotFound() throws Exception {

        mockMvc.perform(post("/project/{projectId}/pledges", "some_id")
                .principal(new UsernamePasswordAuthenticationToken("foo@bar.com", "somepassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Pledge(1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void pledgeProject_shouldRespondWith404IfTheProjectWasNotFound() throws Exception {

        final String email = "some@mail.com";
        final String projectId = "some_foo_id";
        final Pledge pledge = new Pledge(1);
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        doThrow(ResourceNotFoundException.class).when(projectService).pledge(projectId, user, pledge);

        mockMvc.perform(post("/project/{projectId}/pledges", projectId)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(pledge)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void pledgeProject_shouldRespondWith400IfTheRequestObjectIsInvalid() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);

        mockMvc.perform(post("/project/{projectId}/pledges", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"amount\":\"invalidAmount\""))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void pledgeProject_shouldRespondWith400WhenProjectServiceThrowsInvalidRequestEx() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        doThrow(InvalidRequestException.pledgeGoalExceeded()).when(projectService).pledge(anyString(), eq(user), any(Pledge.class));

        MvcResult mvcResult = mockMvc.perform(post("/project/{projectId}/pledges", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Pledge(2))))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse expError = new ErrorResponse(InvalidRequestException.pledgeGoalExceeded().getMessage());
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponse.class), is(expError));
    }

    @Test
    public void modifyProjectStatus() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", 45, ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectStatus(anyString(), eq(expProject.getStatus()), eq(user))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(patch("/project/{projectId}/status", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new ProjectStatusUpdate(expProject.getStatus()))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(expProject));
    }

    @Test
    public void modifyProjectStatus_emptyUpdateObjectThrowsBadRequest() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", 45, ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectStatus(anyString(), eq(expProject.getStatus()), eq(user))).thenReturn(expProject);

        mockMvc.perform(patch("/project/{projectId}/status", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new ProjectStatusUpdate(null))))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void modifyProjectStatus_unknownStatusThrowsBadRequest() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(patch("/project/{projectId}/status", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"UNKNOWN_STATUS\"}") )
                .andExpect(status().isBadRequest())
                .andReturn();
    }


    @Test
    public void modifyProjectMasterdata() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", 45, ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectMasterdata(anyString(), eq(expProject), eq(user))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(put("/project/{projectId}", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(expProject)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(expProject));
    }

    @Test
    public void modifyProjectMasterdata_invalidProjectThrowsBadRequestException() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project(null, "", "shortDescr2", 45, ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectMasterdata(anyString(), eq(expProject), eq(user))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(put("/project/{projectId}", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(expProject)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = mapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponse.class);
        assertThat(errorResponse.getErrorCode(), is("field_errors"));
        assertThat(errorResponse.getFieldViolations().get("description"), is("may not be empty"));
        assertThat(errorResponse.getFieldViolations().get("title"), is("may not be empty"));
    }

    @Test
    public void modifyProjectMasterdata_emptyUpdateObjectThrowsBadRequest() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(put("/project/{projectId}", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void likeProject_shouldCallLikeMethodFromService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(post("/project/{projectId}/like", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService, only()).likeProject(eq("some_id"), any(UserEntity.class));
    }

    @Test
    public void unlikeProject_shouldCallUnlikeMethodFromService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(post("/project/{projectId}/unlike", "some_id")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService, only()).unlikeProject(eq("some_id"), any(UserEntity.class));
    }

    private Principal authentication(UserEntity userEntity) {
        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        userEntity.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        return new UsernamePasswordAuthenticationToken(userEntity.getEmail(), "somepassword", authorities);
    }

    private Principal anonymousAuthentication() {
        return new AnonymousAuthenticationToken("ANONYMOUS", "ANONYMOUS",
                Collections.singletonList(new SimpleGrantedAuthority(Roles.ROLE_TRUSTED_ANONYMOUS)));
    }

    private UserEntity userEntity(String email, String... roles) {

        UserEntity userEntity = new UserEntity(email);
        userEntity.setId("id_" + email);
        userEntity.setRoles(Arrays.asList(roles));
        userEntity.setBudget(4000);
        when(userRepository.findByEmail(email)).thenReturn(userEntity);
        return userEntity;
    }

    private Project project(String title, String description, String shortDescription, int pledgeGoal, ProjectStatus projectStatus) {
        final Project project = new Project();
        project.setTitle(title);
        project.setDescription(description);
        project.setShortDescription(shortDescription);
        project.setPledgeGoal(pledgeGoal);
        project.setStatus(projectStatus);

        return project;
    }

    private Project toCreatedProject(Project project, UserEntity creator) {
        Project res = new Project();
        res.setBackers(0);
        res.setCreator(new ProjectCreator(creator));
        res.setDescription(project.getDescription());
        res.setLastModifiedDate(new Date());
        res.setId(project.getId());
        res.setPledgedAmount(0);
        res.setPledgeGoal(project.getPledgeGoal());
        res.setShortDescription(project.getShortDescription());
        res.setStatus(project.getStatus());
        res.setTitle(project.getTitle());
        res.setPledgedAmountByRequestingUser(12);
        return res;
    }

    /**
     * @param expProject to be prepared
     * @return <code>expProject</code>
     */
    private Project toProjectSummaryViewRepresentation(Project expProject) {
        // As it's not part of ProjectSummaryView we remove the concerning fields for assertion purposes
        expProject.setDescription(null);
        ReflectionTestUtils.setField(expProject.getCreator(), "id", null);
        return expProject;
    }

    @Configuration
    @EnableWebMvc
    static class Config {
        @Bean
        public ControllerExceptionAdvice controllerExceptionAdvice() {
            return new ControllerExceptionAdvice();
        }

        @Bean
        public ProjectController projectController() {
            return new ProjectController();
        }

        @Bean
        public ProjectService projectService() {
            return mock(ProjectService.class);
        }

        @Bean
        public UserService userService(UserRepository userRepository) {
            return new UserService(userRepository, null);
        }
        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }
        @Bean
        public ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }
        @Bean
        public LikeRepository likeRepository() {
            return mock(LikeRepository.class);
        }
    }
}
