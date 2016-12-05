package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.AttachmentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.repository.AttachmentEntityRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import static de.asideas.crowdsource.Mocks.USER_EMAIL;
import static de.asideas.crowdsource.Mocks.project;
import static de.asideas.crowdsource.Mocks.user;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AttachmentEntityRepository attachmentEntityRepository;
    @InjectMocks
    private AttachmentService attachmentService;

    @Test
    public void addAttachment_ShouldStoreAttachment() throws Exception {
        final Long projectId = 123L;
        final UserEntity projectCreator = user(USER_EMAIL);
        final ProjectEntity project = new ProjectEntity("title", "shortDesc", "desc", BigDecimal.valueOf(17), null, projectCreator);
        final Attachment attachmentSaveCmd = aStoringRequestAttachment();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(projectCreator);
        when(projectRepository.findOne(projectId)).thenReturn(project);
        attachmentService.addProjectAttachment(projectId, attachmentSaveCmd, USER_EMAIL);

        verify(attachmentEntityRepository).save(any(AttachmentEntity.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void addAttachment_ThrowsResourceNotFoundExOnNotExistingProject() throws Exception {
        final Long projectId = 123L;
        when(projectRepository.findOne(projectId)).thenReturn(null);
        when(userRepository.findByEmail("blub")).thenReturn(user("blub"));

        attachmentService.addProjectAttachment(projectId, aStoringRequestAttachment(), "blub");
    }

    @Ignore // FIXME: ask tom what this is
    @Test
    public void addAttachment_should_Throw_Exception_When_Changes_Not_Allowed_Due_To_Project_Status() throws Exception {
        final Long projectId = 123L;
        final UserEntity projectCreator = user(USER_EMAIL);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(projectCreator);

        final Project projectCmd = project("title", "descr", "descrShort", BigDecimal.valueOf(17), ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity(projectCmd.getTitle(), projectCmd.getShortDescription(), projectCmd.getDescription(), projectCmd.getPledgeGoal(), null, projectCreator);
        project.setStatus(ProjectStatus.FULLY_PLEDGED);
        when(projectRepository.findOne(projectId)).thenReturn(project);

        try {
            attachmentService.addProjectAttachment(projectId, aStoringRequestAttachment(), USER_EMAIL);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
    }

    private Attachment aStoringRequestAttachment() {
        return Attachment.asCreationCommand("test_filename", "text/plain", mockedInputStream("content"));
    }

    private InputStream mockedInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

}