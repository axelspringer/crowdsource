package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.AttachmentValue;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.PledgeEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.FinancingRound;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.repository.FinancingRoundRepository;
import de.asideas.crowdsource.repository.PledgeRepository;
import de.asideas.crowdsource.repository.ProjectAttachmentRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.security.Roles;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("Duplicates")
public class ProjectServiceTest {

    private static final String USER_EMAIL = "user@some.host";
    private static final String ADMIN1_EMAIL = "admin1@some.host";
    private static final String ADMIN2_EMAIL = "admin2@some.host";
    public static final int USER_BUDGED = 4000;
    public static final int FINANCING_ROUND_BUDGET = 10000;

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private PledgeRepository pledgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private FinancingRoundRepository financingRoundRepository;

    @Mock
    private FinancingRoundService financingRoundService;

    @Mock
    private ProjectService thisInstance;

    @Mock
    private ProjectAttachmentRepository projectAttachmentRepository;


    @Before
    public void init() {
        ReflectionTestUtils.setField(projectService, "thisInstance", thisInstance);
        reset(projectRepository, pledgeRepository, userRepository, financingRoundRepository, thisInstance);
        when(pledgeRepository.findByProjectAndFinancingRound(any(ProjectEntity.class), any(FinancingRoundEntity.class))).thenReturn(new ArrayList<>());
        when(userRepository.findAllAdminUsers()).thenReturn(Arrays.asList(admin(ADMIN1_EMAIL), admin(ADMIN2_EMAIL)));
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
    }


    @Test
    public void addProject() throws Exception {
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", 50, ProjectStatus.PROPOSED);
        final ArgumentCaptor<ProjectEntity> projectEntity = ArgumentCaptor.forClass(ProjectEntity.class);

        when(projectRepository.save(projectEntity.capture())).thenAnswer(a -> a.getArgumentAt(0, ProjectEntity.class));
        prepareActiveFinanzingRound(null);

        Project res = projectService.addProject(project, user(USER_EMAIL));
        assertThat(res, is(new Project(projectEntity.getValue(), new ArrayList<>(), null)));
        verify(userNotificationService, atLeastOnce()).notifyAdminOnProjectCreation(eq(projectEntity.getValue()), anyString());
    }

    @Test
    public void addProject_shouldWorkIfNoFinancingRoundIsCurrentlyActive() throws Exception {
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", 50, ProjectStatus.PROPOSED);
        final ArgumentCaptor<ProjectEntity> projectEntity = ArgumentCaptor.forClass(ProjectEntity.class);

        when(financingRoundRepository.findActive(any())).thenReturn(null);
        when(projectRepository.save(projectEntity.capture())).thenAnswer(a -> a.getArgumentAt(0, ProjectEntity.class));

        Project res = projectService.addProject(project, user(USER_EMAIL));
        assertThat(res, is(new Project(projectEntity.getValue(), new ArrayList<>(), null)));
        verify(userNotificationService, atLeastOnce()).notifyAdminOnProjectCreation(eq(projectEntity.getValue()), anyString());
    }

    @Test
    public void createProjectTriggersAdminNotification() throws Exception {
        final Project newProject = new Project();

        projectService.addProject(newProject, user("some@mail.com"));

        verify(userNotificationService).notifyAdminOnProjectCreation(any(ProjectEntity.class), eq(ADMIN1_EMAIL));
        verify(userNotificationService).notifyAdminOnProjectCreation(any(ProjectEntity.class), eq(ADMIN2_EMAIL));
        verify(userNotificationService, times(2)).notifyAdminOnProjectCreation(any(ProjectEntity.class), anyString());
    }

    @Test
    public void pledge_shouldThrowIllegalArgumentExceptionWhenCurrentRoundAllowsPostPledgesButDoesntEqualsProjectsRound() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        FinancingRoundEntity mostRecentRound = prepareInactiveFinancingRound(project);
        when(financingRoundService.mostRecentRoundEntity()).thenReturn(mostRecentRound);
        project.getFinancingRound().setTerminationPostProcessingDone(true);

        projectService.pledge(projectId, user, pledge);

        verify(thisInstance).pledgeProjectUsingPostRoundBudget(eq(project), eq(user), eq(pledge));
    }

    @Test
    public void pledge_shouldDispatchToPledgeProjectInRoundIfMostRecentRoundIsNullOrNotTerminatedOrNotPostProcessed() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);

        project.setFinancingRound(null);
        projectService.pledge(projectId, user, pledge);
        verify(thisInstance).pledgeProjectInFinancingRound(eq(project), eq(user), eq(pledge));
        reset(thisInstance);

        prepareActiveFinanzingRound(project);
        projectService.pledge(projectId, user, pledge);
        verify(thisInstance).pledgeProjectInFinancingRound(eq(project), eq(user), eq(pledge));
        reset(thisInstance);

        prepareInactiveFinancingRound(project);
        projectService.pledge(projectId, user, pledge);
        verify(thisInstance).pledgeProjectInFinancingRound(eq(project), eq(user), eq(pledge));
    }

    @Test
    public void pledge_shouldDispatchToPledgeProjectInRoundIfRoundTerminatedAndPostProcessedButUserIsNoAdmin() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        prepareInactiveFinancingRound(project);
        project.getFinancingRound().setTerminationPostProcessingDone(true);

        projectService.pledge(projectId, user, pledge);

        verify(thisInstance).pledgeProjectInFinancingRound(eq(project), eq(user), eq(pledge));
    }

    @Test
    public void pledge_shouldDispatchToPledgeProjectUsingPostRoundBudgetOnTerminatedPostProcessedRoundAndAdminUser() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        prepareInactiveFinancingRound(project);
        FinancingRoundEntity mostRecentRound = prepareInactiveFinancingRound(null);
        mostRecentRound.setTerminationPostProcessingDone(true);
        mostRecentRound.setId("test_roundId_another");

        when(financingRoundService.mostRecentRoundEntity()).thenReturn(mostRecentRound);
        project.getFinancingRound().setTerminationPostProcessingDone(true);

        try {
            projectService.pledge(projectId, user, pledge);
            fail("Exception expected to be thrown");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.projectTookNotPartInLastFinancingRond().getMessage()));
        }
    }

    @Test
    public void pledge_throwsResourceNotFoundExOnNotExistingProject() {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        final int budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal() - 4);
        when(projectRepository.findOne(anyString())).thenReturn(null);

        ResourceNotFoundException res = null;
        try {
            projectService.pledge(projectId, user, pledge);
            fail("InvalidRequestException expected!");
        } catch (ResourceNotFoundException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, new ResourceNotFoundException(), project, user, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeProjectInFinancingRound() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final int budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(project.getPledgeGoal() - 4);

        FinancingRoundEntity financingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge, financingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge - pledge.getAmount()));
        assertThat(project.getStatus(), is(not(ProjectStatus.FULLY_PLEDGED)));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    @Test
    public void pledgeProjectInFinancingRound_reverse() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final int budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(-4);

        pledgedAssertionProject(project, user, 4);
        FinancingRoundEntity financingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge, financingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge + 4));
        assertThat(project.getStatus(), is(not(ProjectStatus.FULLY_PLEDGED)));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    @Test
    public void pledgeProjectInFinancingRound_settingStatusToFullyPledgedShouldPersistProjectToo() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        final int budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal() - 4);
        FinancingRoundEntity finanzingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge, finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge - pledge.getAmount()));
        assertThat(project.getStatus(), is(ProjectStatus.FULLY_PLEDGED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository).save(project);
    }

    @Test
    public void pledgeProjectInFinancingRound_errorOnPledgingShouldNotCauseAnyPersistenceActions() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final int budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(45);

        prepareActiveFinanzingRound(project);
        pledgedAssertionProject(project, user, 4);

        InvalidRequestException res = null;
        try {
            projectService.pledgeProjectInFinancingRound(project, user, pledge);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), project, user, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeProjectUsingPostRoundBudget() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(3);
        final int budgetBeforePledge = user.getBudget();


        pledgedAssertionProject(project, user, project.getPledgeGoal() - 4);
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(6000);
        finanzingRound.setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(finanzingRound, finanzingRound.getEndDate())).thenReturn(Collections.emptyList());

        projectService.pledgeProjectUsingPostRoundBudget(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge, finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge));
        assertThat(project.getStatus(), is(ProjectStatus.PUBLISHED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(project);
    }

    @Test
    public void pledgeProjectUsingPostRoundBudget_settingStatusToFullyPledgedShouldPersistProjectToo() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(4);
        final int budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal() - 4);
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(6000);
        finanzingRound.setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(finanzingRound, finanzingRound.getEndDate())).thenReturn(Collections.emptyList());

        projectService.pledgeProjectUsingPostRoundBudget(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge, finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge));
        assertThat(project.getStatus(), is(ProjectStatus.FULLY_PLEDGED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository).save(project);
    }

    @Test
    public void pledgeProjectUsingPostRoundBudget_errorOnPledgingShouldNotCauseAnyPersistenceActions() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final String projectId = "some_id";
        final ProjectEntity project = projectEntity(user, projectId, "title", 44, "short description", "description", ProjectStatus.PUBLISHED, null);
        final int budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(45);

        pledgedAssertionProject(project, user, project.getPledgeGoal() - 4);
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(6000);
        finanzingRound.setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(finanzingRound, finanzingRound.getEndDate())).thenReturn(Collections.emptyList());


        InvalidRequestException res = null;
        try {
            projectService.pledgeProjectUsingPostRoundBudget(project, user, pledge);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), project, user, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void modifyProjectStatus_updatedStateTriggersUserNotificationAndPeristence() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final ProjectEntity projectEntity = projectEntity("some_id", ProjectStatus.PROPOSED, user);

        projectService.modifyProjectStatus("some_id", ProjectStatus.PUBLISHED, user);

        verify(projectRepository).save(projectEntity);
        verify(userNotificationService).notifyCreatorOnProjectStatusUpdate(any(ProjectEntity.class));
    }

    @Test
    public void modifyProjectStatus_nonUpdatedStateDoesNotTriggerUserNotificationAndNoPersistence() throws Exception {
        UserEntity user = user(USER_EMAIL);
        projectEntity("some_id", ProjectStatus.PROPOSED, user);

        projectService.modifyProjectStatus("some_id", ProjectStatus.PROPOSED, user);

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorOnProjectStatusUpdate(any(ProjectEntity.class));
    }

    @Test
    public void modifyProjectMasterdata_notifiesAndSavesOnSuccessfulModification() throws Exception {
        final String projectId = "test_ID";
        final UserEntity user = user(USER_EMAIL);
        final ProjectEntity project = projectEntity(projectId, ProjectStatus.PROPOSED, user);
        final Project projectCmd = project("title", "descr", "descrShort", 17, ProjectStatus.FULLY_PLEDGED);

        projectService.modifyProjectMasterdata(projectId, projectCmd, user);

        ArgumentCaptor<ProjectEntity> captProject = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captProject.capture());
        verify(userNotificationService).notifyCreatorAndAdminOnProjectModification(project, user);
        assertThat(captProject.getValue().getDescription(), is(projectCmd.getDescription()));
        assertThat(captProject.getValue().getStatus(), not(equalTo(projectCmd.getStatus())));
    }

    @Test
    public void modifyProjectMasterdata_doesNotNotifyAndSaveOnNoModification() throws Exception {
        final String projectId = "test_ID";
        final UserEntity user = user(USER_EMAIL);
        final Project projectCmd = project("title", "descr", "descrShort", 17, ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity(user, projectCmd, null);

        when(projectRepository.findOne(projectId)).thenReturn(project);
        projectService.modifyProjectMasterdata(projectId, projectCmd, user);

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorAndAdminOnProjectModification(any(ProjectEntity.class), any(UserEntity.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void modifyProjectMasterdata_ThrowsResourceNotFoundExOnNotExistingProject() throws Exception {
        final String projectId = "test_ID";
        when(projectRepository.findOne(projectId)).thenReturn(null);

        projectService.modifyProjectMasterdata(projectId, project(null, null, null, 17, null), user("blub"));

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorAndAdminOnProjectModification(any(ProjectEntity.class), any(UserEntity.class));
    }

    @Test
    public void addAttachment_ShouldStoreAttachmentAndProject() throws Exception {
        final String projectId = "test_ID";
        final UserEntity projectCreator = user(USER_EMAIL);
        final Project projectCmd = project("title", "descr", "descrShort", 17, ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity(projectCreator, projectCmd, null);
        final Attachment attachmentSaveCmd = aStoringRequestAttachment();
        final AttachmentValue expAttachmentValue = aPersitedAttachment("fileRef");

        ArgumentCaptor<AttachmentValue> writeAttachmentCaptor = ArgumentCaptor.forClass(AttachmentValue.class);
        when(projectRepository.findOne(projectId)).thenReturn(project);
        when(projectAttachmentRepository.storeFile(writeAttachmentCaptor.capture(), any(InputStream.class))).thenReturn(expAttachmentValue);
        Attachment res = projectService.addProjectAttachment(projectId, attachmentSaveCmd, projectCreator);

        ArgumentCaptor<ProjectEntity> projectCaptor = ArgumentCaptor.forClass(ProjectEntity.class);
        assertThat(writeAttachmentCaptor.getValue().getContentType(), is(attachmentSaveCmd.getType()));
        assertThat(writeAttachmentCaptor.getValue().getFilename(), is(attachmentSaveCmd.getName()));
        verify(projectRepository).save(projectCaptor.capture());
        assertThat("Project should contain new attachment", projectCaptor.getValue().getAttachments().contains(expAttachmentValue));
        assertPresentationAttachmentConformsPersited(res, expAttachmentValue);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void addAttachment_ThrowsResourceNotFoundExOnNotExistingProject() throws Exception {
        final String projectId = "test_ID";
        when(projectRepository.findOne(projectId)).thenReturn(null);

        projectService.addProjectAttachment(projectId, aStoringRequestAttachment(), user("blub"));
    }

    @Test
    public void addAttachment_shouldThrowExceptionWhenChangesNotAllowed() throws Exception {
        final String projectId = "test_ID";
        final UserEntity projectCreator = user(USER_EMAIL);
        final Project projectCmd = project("title", "descr", "descrShort", 17, ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity(projectCreator, projectCmd, null);
        project.setStatus(ProjectStatus.FULLY_PLEDGED);
        when(projectRepository.findOne(projectId)).thenReturn(project);

        try {
            projectService.addProjectAttachment(projectId, aStoringRequestAttachment(), projectCreator);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
    }


    @Test
    public void loadProjectAttachment_shouldCallProjectAttachmentRepository() throws Exception {
        final String fileRef = "attachId_0";
        final ProjectEntity projectEntity = givenAProjectEntityWithAttachments(fileRef, "anotherFileRef");
        final InputStream expInputStream = mockedInputStream();
        final ArgumentCaptor<AttachmentValue> repoReqCapture = ArgumentCaptor.forClass(AttachmentValue.class);

        when(projectAttachmentRepository.loadAttachment(repoReqCapture.capture())).thenReturn(expInputStream);
        final Attachment res = projectService.loadProjectAttachment(projectEntity.getId(),  Attachment.asLookupByIdCommand(fileRef));

        assertThat(repoReqCapture.getValue().getFileReference(), is(fileRef));
        assertThat("Expected specific input stream in result", res.getPayload() == expInputStream);
        assertPresentationAttachmentConformsPersited(res, projectEntity.findAttachmentByReference(Attachment.asLookupByIdCommand(fileRef)));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void loadProjectAttachment_shouldThrowResourceNotFoundExceptionOnNullPayload() throws Exception {
        final String fileRef = "attachId_0";
        final ProjectEntity projectEntity = givenAProjectEntityWithAttachments(fileRef, "anotherFileRef");

        when(projectAttachmentRepository.loadAttachment(any(AttachmentValue.class))).thenReturn(null);
        projectService.loadProjectAttachment(projectEntity.getId(),  Attachment.asLookupByIdCommand(fileRef));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void loadProjectAttachment_shouldThrowResourceNotFoundExceptionOnProjectNotExisting() throws Exception {
        projectService.loadProjectAttachment("notExistingProjectId",  Attachment.asLookupByIdCommand("aFileRef"));
    }


    private ProjectEntity givenAProjectEntityWithAttachments(String... attachmentFileReferences) {
        final ProjectEntity projectEntity = projectEntity("test_id", ProjectStatus.PROPOSED, user("a_user"));
        if(attachmentFileReferences != null){
            Arrays.asList(attachmentFileReferences).stream().forEach( r ->
                projectEntity.addAttachment(aPersitedAttachment(r))
            );
        }
        return projectEntity;
    }


    private void assertPledgeNotExecuted(RuntimeException actualEx, RuntimeException expEx, ProjectEntity project, UserEntity user, int userBudgetBeforePledge, ProjectStatus expStatus) {
        assertThat(actualEx.getMessage(), is(expEx.getMessage()));
        assertThat(user.getBudget(), is(userBudgetBeforePledge));
        assertThat(project.getStatus(), is(expStatus));
        verify(pledgeRepository, never()).save(any(PledgeEntity.class));
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    private void assertPresentationAttachmentConformsPersited(Attachment presentationAttachment, AttachmentValue persisted) throws Exception {
        assertThat(presentationAttachment.getCreated(), is(persisted.getCreated()));
        assertThat(presentationAttachment.getId(), is(persisted.getFileReference()));
        assertThat(presentationAttachment.getName(), is(persisted.getFilename()));
        assertThat(presentationAttachment.getSize(), is(persisted.getSize()));
        assertThat(presentationAttachment.getType(), is(persisted.getContentType()));
    }

    private void pledgedAssertionProject(ProjectEntity project, UserEntity user, int amount) {

        when(pledgeRepository.findByProjectAndFinancingRound(eq(project), any()))
                .thenReturn(Collections.singletonList(new PledgeEntity(project, user, new Pledge(amount), new FinancingRoundEntity())));

        if (project.getPledgeGoal() == amount) {
            project.setStatus(ProjectStatus.FULLY_PLEDGED);
        }
    }

    private ProjectEntity projectEntity(UserEntity userEntity, String id, String title, int pledgeGoal, String shortDescription, String description, ProjectStatus status, DateTime lastModifiedDate) {
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setId(id);
        projectEntity.setTitle(title);
        projectEntity.setPledgeGoal(pledgeGoal);
        projectEntity.setShortDescription(shortDescription);
        projectEntity.setDescription(description);
        projectEntity.setCreator(userEntity);
        projectEntity.setStatus(status);
        projectEntity.setLastModifiedDate(lastModifiedDate);
        when(projectRepository.findOne(id)).thenReturn(projectEntity);
        return projectEntity;
    }

    private ProjectEntity projectEntity(String id, ProjectStatus status, UserEntity user) {
        final ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setCreator(user);
        project.setStatus(status);
        when(projectRepository.findOne(id)).thenReturn(project);
        return project;
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

    private UserEntity admin(String email) {
        final UserEntity userEntity = user(email);
        userEntity.setRoles(Collections.singletonList(Roles.ROLE_ADMIN));
        return userEntity;
    }

    private UserEntity user(String email) {
        UserEntity userEntity = new UserEntity(email);
        userEntity.setId("id_" + email);
        userEntity.setBudget(USER_BUDGED);
        return userEntity;
    }

    private FinancingRoundEntity prepareActiveFinanzingRound(ProjectEntity project) {
        FinancingRoundEntity res = aFinancingRound(new DateTime().plusDays(1));
        res.setId(UUID.randomUUID().toString());
        if (project != null) {
            project.setFinancingRound(res);
        }

        when(financingRoundRepository.findActive(any())).thenReturn(res);
        Assert.assertThat(res.active(), Is.is(true));
        return res;
    }

    private FinancingRoundEntity prepareInactiveFinancingRound(ProjectEntity project) {
        FinancingRoundEntity res = aFinancingRound(new DateTime().minusDays(1));
        res.setId("test_roundId");
        if (project != null) {
            project.setFinancingRound(res);
        }

        when(financingRoundRepository.findActive(any())).thenReturn(res);
        Assert.assertThat(res.active(), Is.is(false));
        return res;
    }

    private FinancingRoundEntity aFinancingRound(DateTime endDate) {
        FinancingRound creationCmd = new FinancingRound();
        creationCmd.setEndDate(endDate);
        creationCmd.setBudget(FINANCING_ROUND_BUDGET);
        FinancingRoundEntity res = FinancingRoundEntity.newFinancingRound(creationCmd, 7);
        res.setStartDate(new DateTime().minusDays(2));
        return res;
    }

    private AttachmentValue aPersitedAttachment(String fileRef) {
        return new AttachmentValue(fileRef, "text/plain", "a_filename", 17, DateTime.now());
    }

    private Attachment aStoringRequestAttachment() {
        return Attachment.asCreationCommand("test_filename", "text/plain", mockedInputStream());
    }

    private InputStream mockedInputStream() {
        return mock(InputStream.class);
    }
}