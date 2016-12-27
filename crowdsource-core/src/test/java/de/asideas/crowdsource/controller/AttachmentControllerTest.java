package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.AttachmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.*;

import static de.asideas.crowdsource.Mocks.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableWebMvc
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AttachmentControllerTest.Config.class)
public class AttachmentControllerTest {

    private MockMvc mockMvc;
    @Autowired
    private AttachmentController attachmentController;
    @MockBean
    private AttachmentService attachmentService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController).build();
    }

    @Test
    public void addProjectAttachment_shouldDelegateToProjectService() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        givenControllerSupportsMediaTypes(Collections.singletonList(MediaType.parseMediaType(("text/plain"))));

        final Attachment expectedAttachment = attachment(Optional.empty());

        MockMultipartFile content = mockedMultipart("somecontent", "test_filename", "text/plain");
        MediaType mediaType = mediaType();

        when(attachmentService.addProjectAttachment(eq(123L), eq(Attachment.asCreationCommand("test_filename", "text/plain", content.getInputStream())), eq(email)))
                .thenReturn(expectedAttachment);

        MvcResult mvcRes = mockMvc.perform(fileUpload("/attachments/?projectId={projectId}", 123L)
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
        final Attachment expectedAttachment = attachment(Optional.empty());

        givenControllerSupportsMediaTypes(Collections.singletonList(MediaType.parseMediaType(("text/plain"))));

        MockMultipartFile content = new ProjectControllerTest.MockedInputStreamMultipartFile("file", "test_filename", "text/plain");
        MediaType mediaType = mediaType();

        when(attachmentService.addProjectAttachment(eq(123L), eq(Attachment.asCreationCommand("test_filename", "text/plain", content.getInputStream())), eq(email)))
                .thenReturn(expectedAttachment);

        MvcResult mvcRes = mockMvc.perform(fileUpload("/attachments/?projectId={projectId}", 123L)
                .file(content)
                .principal(authentication(user))
                .contentType(mediaType))
                .andExpect(status().isOk())
                .andReturn();

        Attachment res = mapper.readValue(mvcRes.getResponse().getContentAsString(), Attachment.class);
        assertAttachmentsEqual(expectedAttachment, res);
    }

    @Test
    public void addProjectAttachment_shouldThrowBadRequestOnEmptyFile() throws Exception {
        final String email = "some@mail.com";
        final UserEntity user = userEntity(email, Roles.ROLE_USER, Roles.ROLE_ADMIN);

        MockMultipartFile content = mockedMultipart("", "test_filename", "text/plain");
        MediaType mediaType = mediaType();

        mockMvc.perform(fileUpload("/attachments/?projectId={projectId}",123L)
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

        mockMvc.perform(fileUpload("/attachments/?projectId={projectId}", 123L)
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

        mockMvc.perform(fileUpload("/attachments/?projectId={projectId}", 123L)
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
        final ProjectEntity project = projectEntity(user);
        final Attachment expectedAttachment = attachment(Optional.of(expContent));

        when(attachmentService.getAttachmentWithContent(Attachment.asLookupByIdCommand(expectedAttachment.getId())))
                .thenReturn(expectedAttachment);

        mockMvc.perform(get("/attachments/{attachmentId}/content", expectedAttachment.getId())
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
        final ProjectEntity project = projectEntity(user);
        final Attachment expectedAttachment = attachment(Optional.of(expContent));

        mockMvc.perform(delete("/attachments/{attachmentId}", expectedAttachment.getId())
                .principal(authentication(user)))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(attachmentService).deleteAttachment(Attachment.asLookupByIdCommand(expectedAttachment.getId()), "u@s.er");
    }

    private void givenControllerSupportsMediaTypes(List<MediaType> mediaTypes) {
        ReflectionTestUtils.setField(attachmentController, "attachmentTypesAllowed", mediaTypes);
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

    @Configuration
    static class Config {
        @Bean
        public ControllerExceptionAdvice controllerExceptionAdvice() {
            return new ControllerExceptionAdvice();
        }

        @Bean
        public AttachmentController attachmentController() {
            return new AttachmentController(
                    Arrays.asList(MediaType.ALL),
                    attachmentService());
        }

        @Bean
        public AttachmentService attachmentService() {
            return mock(AttachmentService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }

}