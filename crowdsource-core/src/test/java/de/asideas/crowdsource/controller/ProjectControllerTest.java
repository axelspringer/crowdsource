package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.ErrorResponse;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.presentation.project.ProjectStatusUpdate;
import de.asideas.crowdsource.presentation.user.ProjectCreator;
import de.asideas.crowdsource.repository.FinancingRoundRepository;
import de.asideas.crowdsource.repository.LikeRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.ProjectService;
import de.asideas.crowdsource.service.UserService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ProjectControllerTest.Config.class)
@SuppressWarnings("Duplicates")
public class ProjectControllerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectController projectController;

    @Resource
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(projectService, userRepository);
        mapper.registerModule(new JodaModule());

        givenControllerSupportsMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN));
    }

    @Test
    public void addProject_shouldReturnSuccessfully() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", BigDecimal.valueOf(50), ProjectStatus.PROPOSED);
        final Project expFullProjcet = toCreatedProject(project, user);
        when(projectService.addProject(project, user.getEmail())).thenReturn(expFullProjcet);

        MvcResult mvcResult = mockMvc.perform(post("/project")
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(expFullProjcet, is(equalTo(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class))));
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
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);

        when(projectService.getProject(anyLong(), anyString())).thenThrow(new ResourceNotFoundException());
        mockMvc.perform(get("/project/{projectId}", 123L)
                .principal(authentication(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProject_shouldRespondWith403IfTheUserMayNotSeeThisProject() throws Exception {
        final UserEntity userEntity = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final Long projectId = 4711L;
        when(projectService.getProject(eq(projectId), eq("some@mail.com")))
                .thenReturn(toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator));

        mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(authentication(userEntity))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void getProject_shouldReturnSingleProjectSuccessfullyWhenProjectIsPublished() throws Exception {
        final UserEntity userEntity = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final Long projectId = 4711L;
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED), creator);

        when(projectService.getProject(eq(projectId), eq("some@mail.com"))).thenReturn(expProjcet);

        MvcResult mvcResult = mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(authentication(userEntity)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(equalTo(expProjcet)));
    }

    @Test
    public void getProject_shouldReturnSingleProjectSuccessfullyWhenProjectIsPublishedForAnonymousToo() throws Exception {
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final long projectId = 4711L;
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED), creator);

        when(projectService.getProject(eq(projectId), anyString())).thenReturn(expProjcet);

        MvcResult mvcResult = mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(anonymousAuthentication()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project.class), is(equalTo(expProjcet)));
    }

    @Test
    public void getProject_shouldReturnProjectLikeCountAndUserLikeStatus() throws Exception {
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);
        final long projectId = 4711L;
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED), creator);

        when(projectService.getProject(eq(projectId), anyString())).thenReturn(expProjcet);

        mockMvc.perform(get("/project/{projectId}", projectId)
                .principal(anonymousAuthentication()))
                .andExpect(jsonPath("$.likeStatus", nullValue()))
                .andExpect(jsonPath("$.likeCount", is(0)))
                .andExpect(status().isOk());
    }

    @Test
    public void getProjects_shouldReturnPublishedProjectsOnly() throws Exception {
        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        final Project expProjcet_0 = toCreatedProject(project("title0", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED), creator);
        final Project unexpProjcet = toCreatedProject(project("title1", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator);
        final Project expProjcet_1 = toCreatedProject(project("title2", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED_DEFERRED), creator);
        when(projectService.getProjects(user.getEmail())).thenReturn(Arrays.asList(expProjcet_0, unexpProjcet, expProjcet_1));

        MvcResult mvcResult = mockMvc.perform(get("/projects", projectId)
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andReturn();

        toProjectSummaryViewRepresentation(expProjcet_0);
        toProjectSummaryViewRepresentation(expProjcet_1);
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class)[0], is(equalTo(expProjcet_0)));
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), Project[].class)[1], is(equalTo(expProjcet_1)));
    }

    @Test
    public void getProjects_shouldReturnPublishedProjectsForAnonymousUsersToo() throws Exception {
        final UserEntity user = userEntity("some@mail.com", Roles.ROLE_USER);
        final UserEntity creator = userEntity("creator@mail.com", Roles.ROLE_USER);

        final String projectId = "existingProjectId";
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PUBLISHED), creator);
        final Project unexpProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(anyString())).thenReturn(Arrays.asList(expProjcet, unexpProjcet));

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
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(user.getEmail())).thenReturn(Collections.singletonList(expProjcet));

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
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator);
        when(projectService.getProjects(user.getEmail())).thenReturn(Collections.singletonList(expProjcet));

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
        final Project expProjcet = toCreatedProject(project("title", "descr", "shortDescr", BigDecimal.valueOf(50), ProjectStatus.PROPOSED), creator);
        final Project nonExpProjcet = toCreatedProject(project("title2", "descr2", "shortDescr2", BigDecimal.valueOf(45), ProjectStatus.PROPOSED), anotherCreator);
        when(projectService.getProjects(creator.getEmail())).thenReturn(Arrays.asList(expProjcet, nonExpProjcet));

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
        final long projectId = 4711L;
        final UserEntity user = userEntity(email, Roles.ROLE_USER);
        Pledge pledge = new Pledge(BigDecimal.valueOf(13));

        mockMvc.perform(post("/project/{projectId}/pledges", projectId)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(pledge)))
                .andExpect(status().isCreated());

        verify(projectService).pledge(projectId, user.getEmail(), pledge);
    }

    @Test
    public void pledgeProject_shouldRespondWith400IfTheRequestObjectIsInvalid() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER);

        mockMvc.perform(post("/project/{projectId}/pledges", 123L)
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
        doThrow(InvalidRequestException.pledgeGoalExceeded()).when(projectService).pledge(anyLong(), eq(user.getEmail()), any(Pledge.class));

        MvcResult mvcResult = mockMvc.perform(post("/project/{projectId}/pledges", 123L)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Pledge(BigDecimal.valueOf(2)))))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse expError = new ErrorResponse(InvalidRequestException.pledgeGoalExceeded().getMessage());
        assertThat(mapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponse.class), is(expError));
    }

    @Test
    public void modifyProjectStatus() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", BigDecimal.valueOf(45), ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectStatus(anyLong(), eq(expProject.getStatus()), eq(email))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(patch("/project/{projectId}/status", 123L)
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
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", BigDecimal.valueOf(45), ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectStatus(anyLong(), eq(expProject.getStatus()), eq(email))).thenReturn(expProject);

        mockMvc.perform(patch("/project/{projectId}/status", 123L)
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

        mockMvc.perform(patch("/project/{projectId}/status", 123L)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"UNKNOWN_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }


    @Test
    public void modifyProjectMasterdata() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Project expProject = toCreatedProject(project("title2", "descr2", "shortDescr2", BigDecimal.valueOf(45), ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectMasterdata(anyLong(), eq(expProject), eq(email))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(put("/project/{projectId}", 123L)
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
        final Project expProject = toCreatedProject(project(null, "", "shortDescr2", BigDecimal.valueOf(45), ProjectStatus.PROPOSED), user);

        when(projectService.modifyProjectMasterdata(anyLong(), eq(expProject), eq(email))).thenReturn(expProject);

        MvcResult mvcResult = mockMvc.perform(put("/project/{projectId}", 123L)
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

        mockMvc.perform(put("/project/{projectId}", 123L)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest())
                .andReturn();

    }

    @Test
    public void addProjectAttachment_shouldDelegateToProjectService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Attachment expectedAttachment = anExpectedAttachment();

        MockMultipartFile content = mockedMultipart("somecontent", "test_filename", "text/plain");
        MediaType mediaType = mediaType();

        when(projectService.addProjectAttachment(eq(123L), eq(Attachment.asCreationCommand("test_filename", "text/plain", content.getInputStream())), eq(email)))
                .thenReturn(expectedAttachment);

        MvcResult mvcRes = mockMvc.perform(fileUpload("/projects/{projectId}/attachments", 123L)
                .file(content)
                .principal(authentication(user))
                .contentType(mediaType))
                .andExpect(status().isOk())
                .andReturn();

        Attachment res = mapper.readValue(mvcRes.getResponse().getContentAsString(), Attachment.class);
        assertAttachmentsEqual(expectedAttachment, res);
    }

    @Test
    public void addProjectAttachment_shouldCloseInputStream() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        final Attachment expectedAttachment = anExpectedAttachment();

        MockMultipartFile content = new MockedInputStreamMultipartFile("file", "test_filename", "text/plain");
        final InputStream mockedInputStream = content.getInputStream();
        MediaType mediaType = mediaType();

        when(projectService.addProjectAttachment(eq(123L), eq(Attachment.asCreationCommand("test_filename", "text/plain", content.getInputStream())), eq(email)))
                .thenReturn(expectedAttachment);

        MvcResult mvcRes = mockMvc.perform(fileUpload("/projects/{projectId}/attachments", 123L)
                .file(content)
                .principal(authentication(user))
                .contentType(mediaType))
                .andExpect(status().isOk())
                .andReturn();

        Attachment res = mapper.readValue(mvcRes.getResponse().getContentAsString(), Attachment.class);
        assertAttachmentsEqual(expectedAttachment, res);
        verify(mockedInputStream).close();
    }

    @Test
    public void addProjectAttachment_shouldThrowBadRequestOnEmptyFile() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        MockMultipartFile content = mockedMultipart("", "test_filename", "text/plain");
        MediaType mediaType = mediaType();

        mockMvc.perform(fileUpload("/projects/{projectId}/attachments",123L)
                .file(content)
                .principal(authentication(user))
                .contentType(mediaType))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void addProjectAttachment_shouldThrowBadRequestOnUnsupportedMediaType() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        givenControllerSupportsMediaTypes(Collections.singletonList(MediaType.parseMediaType(("application/pdf"))));

        MockMultipartFile content = mockedMultipart("someContent", "test_filename", "application/json");
        MediaType mediaType = mediaType();

        mockMvc.perform(fileUpload("/projects/{projectId}/attachments", 123L)
                .file(content)
                .contentType(mediaType)
                .principal(authentication(user)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void addProjectAttachment_shouldAcceptSpecificMediaTypeOnGeneralDefinition() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        givenControllerSupportsMediaTypes(Collections.singletonList(MediaType.parseMediaType(("image/*"))));

        MockMultipartFile content = mockedMultipart("someContent", "test_filename", "image/jpeg");
        MediaType mediaType = mediaType();

        mockMvc.perform(fileUpload("/projects/{projectId}/attachments", 123L)
                .file(content)
                .contentType(mediaType)
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void serveProjectAttachment_ReturnsResponseWithCorrectMediaType() throws Exception {
        final UserEntity user = userEntity("u@s.er", Roles.ROLE_USER);
        final String expContent = "someContent";
        final ProjectEntity project = aPersistedProjectEntity();
        final Attachment expectedAttachment = anExpectedAttachmentWithPayload(Optional.of(expContent));

        when(projectService.loadProjectAttachment(Attachment.asLookupByIdCommand(expectedAttachment.getId())))
                .thenReturn(expectedAttachment);

        mockMvc.perform(get("/projects/{projectId}/attachments/{fileRef}", project.getId(), expectedAttachment.getId())
                .principal(authentication(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(expectedAttachment.getType()))
                .andExpect(header().longValue("Content-Length", expectedAttachment.getSize()))
                .andExpect(content().string(expContent))
                .andReturn();

    }

    @Test
    public void deleteAttachment_callsProjectServiceAndReturnsNoContent() throws Exception {
        final UserEntity user = userEntity("u@s.er", Roles.ROLE_USER);
        final String expContent = "someContent";
        final ProjectEntity project = aPersistedProjectEntity();
        final Attachment expectedAttachment = anExpectedAttachmentWithPayload(Optional.of(expContent));

        mockMvc.perform(delete("/projects/{projectId}/attachments/{fileRef}", project.getId(), expectedAttachment.getId())
                .principal(authentication(user)))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(projectService).deleteProjectAttachment(Attachment.asLookupByIdCommand(expectedAttachment.getId()));
    }

    private void givenControllerSupportsMediaTypes(List<MediaType> mediaTypes) {
        ReflectionTestUtils.setField(projectController, "attachmentTypesAllowed", mediaTypes);
    }

    private void assertAttachmentsEqual(Attachment expected, Attachment actual) {
        assertThat(actual.getName(), is(expected.getName()));
        assertThat(actual.getType(), is(expected.getType()));
        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getCreated().getMillis(), is(expected.getCreated().getMillis()));
        assertThat(actual.getSize(), is(expected.getSize()));
    }

    private MediaType mediaType() {
        HashMap<String, String> contentTypeParams = new HashMap<>();
        contentTypeParams.put("boundary", "34643214412434354");
        return new MediaType("multipart", "form-data", contentTypeParams);
    }

    private MockMultipartFile mockedMultipart(String content, String filename, String contentType) {
        MockMultipartFile multipartFile = new MockMultipartFile("file", filename, contentType, content.getBytes());
        return multipartFile;
    }

    @Test
    public void likeProject_shouldCallLikeMethodFromService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(post("/projects/{projectId}/likes", 4711L)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService, only()).likeProject(eq(4711L), anyString());
    }

    @Test
    public void unlikeProject_shouldCallUnlikeMethodFromService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        mockMvc.perform(delete("/projects/{projectId}/likes", 4711L)
                .principal(authentication(user))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService, only()).unlikeProject(eq(4711L), anyString());
    }

    private Principal authentication(UserEntity userEntity) {
        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        userEntity.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        return new UsernamePasswordAuthenticationToken(userEntity.getEmail(), "somepassword", authorities);
    }

    private Principal anonymousAuthentication() {
        return new AnonymousAuthenticationToken("ANONYMOUS", "ANONYMOUS",
                Collections.singletonList(new SimpleGrantedAuthority("ANONYMOUS")));
    }

    private UserEntity userEntity(String email, String... roles) {

        UserEntity userEntity = new UserEntity(email, "firstname", "lastname");
        userEntity.setId((long) email.hashCode());
        userEntity.setRoles(Arrays.asList(roles));
        userEntity.setBudget(BigDecimal.valueOf(4000));
        when(userRepository.findByEmail(email)).thenReturn(userEntity);
        return userEntity;
    }

    private Project project(String title, String description, String shortDescription, BigDecimal pledgeGoal, ProjectStatus projectStatus) {
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
        res.setPledgedAmount(BigDecimal.ZERO);
        res.setPledgeGoal(project.getPledgeGoal());
        res.setShortDescription(project.getShortDescription());
        res.setStatus(project.getStatus());
        res.setTitle(project.getTitle());
        res.setPledgedAmountByRequestingUser(BigDecimal.valueOf(12));
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

    private ProjectEntity aPersistedProjectEntity() {
        final ProjectEntity res = new ProjectEntity();
        res.setId(150L);
        return res;
    }

    private Attachment anExpectedAttachment() {
        return anExpectedAttachmentWithPayload(Optional.empty());
    }

    private Attachment anExpectedAttachmentWithPayload(Optional<String> payload) {
        if (payload.isPresent()) {
            return Attachment.asResponse(1L, "name", 12L, "text/plain", DateTime.now(), new ByteArrayInputStream(payload.get().getBytes()));
        } else {
            return Attachment.asResponseWithoutPayload(1L, "name", 12L, "text/plain", DateTime.now());
        }
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
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder, FinancingRoundRepository financingRoundRepository) {
            return new UserService(userRepository, null, passwordEncoder, financingRoundRepository);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
            configurer.setLocation(new ClassPathResource("application.properties"));
            return configurer;
        }
        @Bean
        public ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        public FinancingRoundRepository financingRoundRepository() {
            return mock(FinancingRoundRepository.class);
        }

        @Bean
        public LikeRepository likeRepository() {
            return mock(LikeRepository.class);
        }
    }

    static class MockedInputStreamMultipartFile extends MockMultipartFile {

        private InputStream mockedInputStream = mock(InputStream.class);

        public MockedInputStreamMultipartFile(String name, String originalFilename, String contentType) {
            super(name, originalFilename, contentType, new byte[]{0, 1, 2, 3});
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return mockedInputStream;
        }
    }

}
