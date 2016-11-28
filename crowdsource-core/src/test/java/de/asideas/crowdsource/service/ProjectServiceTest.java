package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.*;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.repository.*;
import de.asideas.crowdsource.security.Roles;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static de.asideas.crowdsource.domain.shared.LikeStatus.LIKE;
import static de.asideas.crowdsource.domain.shared.LikeStatus.UNLIKE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("Duplicates")
public class ProjectServiceTest {

    private static final String USER_EMAIL = "user@some.host";
    private static final String ADMIN1_EMAIL = "admin1@some.host";
    private static final String ADMIN2_EMAIL = "admin2@some.host";
    public static final BigDecimal USER_BUDGED = BigDecimal.valueOf(4000);
    public static final BigDecimal FINANCING_ROUND_BUDGET = BigDecimal.valueOf(10000);

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
    private LikeRepository likeRepository;

    @Mock
    private UserService userService;

    @Mock
    private FinancingRoundRepository financingRoundRepository;

    @Mock
    private FinancingRoundService financingRoundService;

    @Mock
    private AttachmentEntityRepository attachmentEntityRepository;


    @Before
    public void init() {
        reset(projectRepository, pledgeRepository, userRepository, financingRoundRepository, likeRepository, attachmentEntityRepository);
        when(pledgeRepository.findByProjectAndFinancingRound(any(ProjectEntity.class), any(FinancingRoundEntity.class))).thenReturn(new ArrayList<>());
        when(userRepository.findAllAdminUsers()).thenReturn(Arrays.asList(admin(ADMIN1_EMAIL), admin(ADMIN2_EMAIL)));
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(likeRepository.countByProjectAndStatus(any(ProjectEntity.class), eq(LIKE))).thenReturn(0L);
        when(likeRepository.findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class))).thenReturn(Optional.of(new LikeEntity()));
    }

    @Test
    public void addProject() throws Exception {
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", BigDecimal.valueOf(50), ProjectStatus.PROPOSED);
        final ArgumentCaptor<ProjectEntity> projectEntity = ArgumentCaptor.forClass(ProjectEntity.class);

        UserEntity user = user(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(user);
        when(projectRepository.save(projectEntity.capture())).thenAnswer(a -> {
            ProjectEntity pe = a.getArgumentAt(0, ProjectEntity.class);
            pe.setCreator(user);
            return pe;
        });
        prepareActiveFinanzingRound(null);

        Project res = projectService.addProject(project, USER_EMAIL);
        assertThat(res, is(new Project(projectEntity.getValue(), new ArrayList<>(), null, 0, LIKE)));
        verify(userNotificationService, atLeastOnce()).notifyAdminOnProjectCreation(eq(new Project(projectEntity.getValue())), anyString());
    }

    @Test
    public void addProject_shouldWorkIfNoFinancingRoundIsCurrentlyActive() throws Exception {
        final Project project = project("myTitle", "theFullDescription", "theShortDescription", BigDecimal.valueOf(50), ProjectStatus.PROPOSED);
        final ArgumentCaptor<ProjectEntity> projectEntity = ArgumentCaptor.forClass(ProjectEntity.class);
        final UserEntity projectCreator = user(USER_EMAIL);

        when(financingRoundRepository.findActive(any())).thenReturn(null);
        when(projectRepository.save(projectEntity.capture())).thenAnswer(a -> {
            ProjectEntity answer = a.getArgumentAt(0, ProjectEntity.class);
            answer.setCreator(projectCreator);
            return answer;
        });

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(projectCreator);

        Project res = projectService.addProject(project, USER_EMAIL);
        assertThat(res, is(new Project(projectEntity.getValue(), new ArrayList<>(), null, 0, LIKE)));
        verify(userNotificationService, atLeastOnce()).notifyAdminOnProjectCreation(eq(new Project(projectEntity.getValue())), anyString());
    }

    @Test
    public void createProjectTriggersAdminNotification() throws Exception {
        final Project newProject = new Project();

        UserEntity user = user("some@mail.com");
        when(userRepository.findByEmail("some@mail.com")).thenReturn(user);
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(a -> {
            ProjectEntity answer = a.getArgumentAt(0, ProjectEntity.class);
            answer.setCreator(user);
            return answer;
        });

        projectService.addProject(newProject, "some@mail.com");

        verify(userNotificationService).notifyAdminOnProjectCreation(any(Project.class), eq(ADMIN1_EMAIL));
        verify(userNotificationService).notifyAdminOnProjectCreation(any(Project.class), eq(ADMIN2_EMAIL));
        verify(userNotificationService, times(2)).notifyAdminOnProjectCreation(any(Project.class), anyString());
    }

    @Test
    public void pledge_should_Dispatch_To_Pledge_Project_In_Round_If_Most_Recent_Round_Is_Null() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        project.setFinancingRound(null);
        try {
            projectService.pledge(projectId, USER_EMAIL, pledge);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(InvalidRequestException.class));
            assertThat(e, hasProperty("message", equalTo("no_financing_round_currently_active")));
        }
    }

    @Test
    @Ignore // TODO ask tom.
    public void pledge_should_Dispatch_To_Pledge_Project_In_Round_If_Recent_Round_Not_Terminated() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(user);

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        prepareActiveFinanzingRound(project);
        projectService.pledge(projectId, USER_EMAIL, pledge);

        verify(pledgeRepository).save(any(PledgeEntity.class));
    }

    @Test
    public void pledge_should_Dispatch_To_Pledge_Project_In_Round_If_Most_Recent_Round_Is_Not_Post_Processed() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        prepareInactiveFinancingRound(project);
        try {
            projectService.pledge(projectId, USER_EMAIL, pledge);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(InvalidRequestException.class));
            assertThat(e, hasProperty("message", equalTo("no_financing_round_currently_active")));
        }
    }

    @Test
    public void pledge_should_Dispatch_To_Pledge_Project_In_Round_If_Round_Terminated_And_Post_Processed_But_User_Is_No_Admin() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        prepareInactiveFinancingRound(project);
        project.getFinancingRound().setTerminationPostProcessingDone(true);

        try {
            projectService.pledge(projectId, USER_EMAIL, pledge);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(InvalidRequestException.class));
            assertThat(e, hasProperty("message", equalTo("no_financing_round_currently_active")));
        }
    }

    @Test
    public void pledge_shouldDispatchToPledgeProjectUsingPostRoundBudgetOnTerminatedPostProcessedRoundAndAdminUser() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final Long projectId = 123L;

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(user);

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        prepareInactiveFinancingRound(project);
        FinancingRoundEntity mostRecentRound = prepareInactiveFinancingRound(null);
        mostRecentRound.setTerminationPostProcessingDone(true);
        mostRecentRound.setId(124L);

        when(financingRoundService.mostRecentRoundEntity()).thenReturn(mostRecentRound);
        project.getFinancingRound().setTerminationPostProcessingDone(true);

        try {
            projectService.pledge(projectId, USER_EMAIL, pledge);
            fail("Exception expected to be thrown");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.projectTookNotPartInLastFinancingRond().getMessage()));
        }
    }

    @Test
    public void pledge_throwsResourceNotFoundExOnNotExistingProject() {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        final BigDecimal budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));
        when(projectRepository.findOne(anyLong())).thenReturn(null);

        ResourceNotFoundException res = null;
        try {
            projectService.pledge(projectId, USER_EMAIL, pledge);
            fail("InvalidRequestException expected!");
        } catch (ResourceNotFoundException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, new ResourceNotFoundException(), project, user, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeProjectInFinancingRound() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final BigDecimal budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));

        FinancingRoundEntity financingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge.getAmount(), financingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge.subtract(pledge.getAmount())));
        assertThat(project.getStatus(), is(not(ProjectStatus.FULLY_PLEDGED)));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    @Test
    public void pledgeProjectInFinancingRound_reverse() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final BigDecimal budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-4));

        pledgedAssertionProject(project, user, BigDecimal.valueOf(4));
        FinancingRoundEntity financingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge.getAmount(), financingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge.add(BigDecimal.valueOf(4))));
        assertThat(project.getStatus(), is(not(ProjectStatus.FULLY_PLEDGED)));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    @Test
    public void pledgeProjectInFinancingRound_settingStatusToFullyPledgedShouldPersistProjectToo() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;

        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        final BigDecimal budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));
        FinancingRoundEntity finanzingRound = prepareActiveFinanzingRound(project);

        projectService.pledgeProjectInFinancingRound(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge.getAmount(), finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge.subtract(pledge.getAmount())));
        assertThat(project.getStatus(), is(ProjectStatus.FULLY_PLEDGED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository).save(project);
    }

    @Test
    public void pledgeProjectInFinancingRound_errorOnPledgingShouldNotCauseAnyPersistenceActions() throws Exception {
        final UserEntity user = user(USER_EMAIL);
        final Long projectId = 123L;
        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final BigDecimal budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(45));

        prepareActiveFinanzingRound(project);
        pledgedAssertionProject(project, user, BigDecimal.valueOf(4));

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
        final Long projectId = 123L;
        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(3));
        final BigDecimal budgetBeforePledge = user.getBudget();


        pledgedAssertionProject(project, user, project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(BigDecimal.valueOf(6000));
        finanzingRound.setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(finanzingRound, finanzingRound.getEndDate())).thenReturn(Collections.emptyList());

        projectService.pledgeProjectUsingPostRoundBudget(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge.getAmount(), finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge));
        assertThat(project.getStatus(), is(ProjectStatus.PUBLISHED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository, never()).save(project);
    }

    @Test
    public void pledgeProjectUsingPostRoundBudget_settingStatusToFullyPledgedShouldPersistProjectToo() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final Long projectId = 123L;
        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        final BigDecimal budgetBeforePledge = user.getBudget();

        pledgedAssertionProject(project, user, project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(BigDecimal.valueOf(6000));
        finanzingRound.setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(finanzingRound, finanzingRound.getEndDate())).thenReturn(Collections.emptyList());

        projectService.pledgeProjectUsingPostRoundBudget(project, user, pledge);

        PledgeEntity pledgeEntity = new PledgeEntity(project, user, pledge.getAmount(), finanzingRound);
        assertThat(user.getBudget(), is(budgetBeforePledge));
        assertThat(project.getStatus(), is(ProjectStatus.FULLY_PLEDGED));
        verify(pledgeRepository).save(pledgeEntity);
        verify(userRepository).save(user);
        verify(projectRepository).save(project);
    }

    @Test
    public void pledgeProjectUsingPostRoundBudget_errorOnPledgingShouldNotCauseAnyPersistenceActions() throws Exception {
        final UserEntity user = admin(USER_EMAIL);
        final Long projectId = 123L;
        final ProjectEntity project = projectEntity(user, projectId, "title", BigDecimal.valueOf(44), "short description", "description", ProjectStatus.PUBLISHED, null);
        final BigDecimal budgetBeforePledge = user.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(45));

        pledgedAssertionProject(project, user, project.getPledgeGoal().subtract(BigDecimal.valueOf(4)));
        FinancingRoundEntity finanzingRound = prepareInactiveFinancingRound(project);
        finanzingRound.initPostRoundBudget(BigDecimal.valueOf(6000));
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
        final ProjectEntity projectEntity = projectEntity(1L, ProjectStatus.PROPOSED, user);
        projectEntity.setCreator(user);

        when(projectRepository.findOne(anyLong())).thenReturn(projectEntity);

        projectService.modifyProjectStatus(2L, ProjectStatus.PUBLISHED, USER_EMAIL);

        verify(projectRepository).save(projectEntity);
        verify(userNotificationService).notifyCreatorOnProjectStatusUpdate(any(ProjectEntity.class));
    }

    @Test
    public void modifyProjectStatus_nonUpdatedStateDoesNotTriggerUserNotificationAndNoPersistence() throws Exception {
        UserEntity user = user(USER_EMAIL);
        projectEntity(1L, ProjectStatus.PROPOSED, user);

        projectService.modifyProjectStatus(1L, ProjectStatus.PROPOSED, USER_EMAIL);

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorOnProjectStatusUpdate(any(ProjectEntity.class));
    }

    @Test
    public void modifyProjectMasterdata_notifiesAndSavesOnSuccessfulModification() throws Exception {
        final Long projectId = 123L;
        final UserEntity user = user(USER_EMAIL);
        final ProjectEntity project = projectEntity(projectId, ProjectStatus.PROPOSED, user);
        final Project projectCmd = project("title", "descr", "descrShort", BigDecimal.valueOf(17), ProjectStatus.FULLY_PLEDGED);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(user);

        projectService.modifyProjectMasterdata(projectId, projectCmd, USER_EMAIL);

        ArgumentCaptor<ProjectEntity> captProject = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captProject.capture());
        verify(userNotificationService).notifyCreatorAndAdminOnProjectModification(project, user);
        assertThat(captProject.getValue().getDescription(), is(projectCmd.getDescription()));
        assertThat(captProject.getValue().getStatus(), not(equalTo(projectCmd.getStatus())));
    }

    @Test
    public void modifyProjectMasterdata_doesNotNotifyAndSaveOnNoModification() throws Exception {
        final Long projectId = 123L;
        final UserEntity user = user(USER_EMAIL);
        final Project projectCmd = project("title", "descr", "descrShort", BigDecimal.valueOf(17), ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity(projectCmd.getTitle(), projectCmd.getShortDescription(), projectCmd.getDescription(), projectCmd.getPledgeGoal(), null, user);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(user);
        when(projectRepository.findOne(projectId)).thenReturn(project);
        projectService.modifyProjectMasterdata(projectId, projectCmd, USER_EMAIL);

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorAndAdminOnProjectModification(any(ProjectEntity.class), any(UserEntity.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void modifyProjectMasterdata_ThrowsResourceNotFoundExOnNotExistingProject() throws Exception {
        final Long projectId = 123L;
        when(projectRepository.findOne(projectId)).thenReturn(null);

        projectService.modifyProjectMasterdata(projectId, project(null, null, null, BigDecimal.valueOf(17), null), "blub");

        verify(projectRepository, never()).save(any(ProjectEntity.class));
        verify(userNotificationService, never()).notifyCreatorAndAdminOnProjectModification(any(ProjectEntity.class), any(UserEntity.class));
    }

    @Test
    public void addAttachment_ShouldStoreAttachment() throws Exception {
        final Long projectId = 123L;
        final UserEntity projectCreator = user(USER_EMAIL);
        final Project projectCmd = project("title", "descr", "descrShort", BigDecimal.valueOf(17), ProjectStatus.PROPOSED);
        final ProjectEntity project = new ProjectEntity("title", "shortDesc", "desc", BigDecimal.valueOf(17), null, projectCreator);
        final Attachment attachmentSaveCmd = aStoringRequestAttachment();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(projectCreator);
        when(projectRepository.findOne(projectId)).thenReturn(project);
        Attachment res = projectService.addProjectAttachment(projectId, attachmentSaveCmd, USER_EMAIL);

        verify(attachmentEntityRepository).save(any(AttachmentEntity.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void addAttachment_ThrowsResourceNotFoundExOnNotExistingProject() throws Exception {
        final Long projectId = 123L;
        when(projectRepository.findOne(projectId)).thenReturn(null);
        when(userRepository.findByEmail("blub")).thenReturn(user("blub"));

        projectService.addProjectAttachment(projectId, aStoringRequestAttachment(), "blub");
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
            projectService.addProjectAttachment(projectId, aStoringRequestAttachment(), USER_EMAIL);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
    }

    @Test
    public void likeProject_shouldCreateLikeEntityIfNotExists() throws Exception {
        when(likeRepository.findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class))).thenReturn(Optional.empty());

        projectService.likeProject(1L, anyString());

        final ArgumentCaptor<LikeEntity> captor = ArgumentCaptor.forClass(LikeEntity.class);

        verify(likeRepository, times(1)).findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class));
        verify(likeRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getStatus(), is(LIKE));
    }

    @Test
    public void likeProject_shouldCreateLikeEntityIfExists() throws Exception {
        when(likeRepository.findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class))).thenReturn(Optional.of(new LikeEntity()));

        projectService.likeProject(1L, anyString());

        final ArgumentCaptor<LikeEntity> captor = ArgumentCaptor.forClass(LikeEntity.class);

        verify(likeRepository, times(1)).findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class));
        verify(likeRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getStatus(), is(LIKE));
    }

    @Test
    public void unlikeProject_shouldCreateLikeEntityIfNotExists() throws Exception {
        when(likeRepository.findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class))).thenReturn(Optional.empty());

        projectService.unlikeProject(1L, anyString());

        final ArgumentCaptor<LikeEntity> captor = ArgumentCaptor.forClass(LikeEntity.class);

        verify(likeRepository, times(1)).findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class));
        verify(likeRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getStatus(), is(UNLIKE));
    }

    @Test
    public void unlikeProject_shouldCreateLikeEntityIfExists() throws Exception {
        when(likeRepository.findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class))).thenReturn(Optional.of(new LikeEntity()));

        projectService.unlikeProject(1L, anyString());

        final ArgumentCaptor<LikeEntity> captor = ArgumentCaptor.forClass(LikeEntity.class);

        verify(likeRepository, times(1)).findOneByProjectAndCreator(any(ProjectEntity.class), any(UserEntity.class));
        verify(likeRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getStatus(), is(UNLIKE));
    }

    private void assertPledgeNotExecuted(RuntimeException actualEx, RuntimeException expEx, ProjectEntity project, UserEntity user, BigDecimal userBudgetBeforePledge, ProjectStatus expStatus) {
        assertThat(actualEx.getMessage(), is(expEx.getMessage()));
        assertThat(user.getBudget(), is(userBudgetBeforePledge));
        assertThat(project.getStatus(), is(expStatus));
        verify(pledgeRepository, never()).save(any(PledgeEntity.class));
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }

    private void pledgedAssertionProject(ProjectEntity project, UserEntity user, BigDecimal amount) {

        when(pledgeRepository.findByProjectAndFinancingRound(eq(project), any()))
                .thenReturn(Collections.singletonList(new PledgeEntity(project, user, amount, new FinancingRoundEntity())));

        if (project.getPledgeGoal().compareTo(amount) == 0) {
            project.setStatus(ProjectStatus.FULLY_PLEDGED);
        }
    }

    private ProjectEntity projectEntity(UserEntity userEntity, Long id, String title, BigDecimal pledgeGoal, String shortDescription, String description, ProjectStatus status, DateTime lastModifiedDate) {
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

    private ProjectEntity projectEntity(Long id, ProjectStatus status, UserEntity creator) {
        final ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setCreator(creator);
        project.setStatus(status);
        when(projectRepository.findOne(id)).thenReturn(project);
        return project;
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

    private UserEntity admin(String email) {
        final UserEntity userEntity = user(email);
        userEntity.setRoles(Collections.singletonList(Roles.ROLE_ADMIN));
        return userEntity;
    }

    private UserEntity user(String email) {
        UserEntity userEntity = new UserEntity(email, "firstname", "lastname");
        userEntity.setId(new Random(Long.MAX_VALUE).nextLong());
        userEntity.setBudget(USER_BUDGED);
        return userEntity;
    }

    private FinancingRoundEntity prepareActiveFinanzingRound(ProjectEntity project) {
        FinancingRoundEntity res = aFinancingRound(new DateTime().plusDays(1));
        res.setId(new Random(Long.MAX_VALUE).nextLong());
        if (project != null) {
            project.setFinancingRound(res);
        }

        when(financingRoundRepository.findActive(any())).thenReturn(res);
        Assert.assertThat(res.active(), Is.is(true));
        return res;
    }

    private FinancingRoundEntity prepareInactiveFinancingRound(ProjectEntity project) {
        FinancingRoundEntity res = aFinancingRound(new DateTime().minusDays(1));
        res.setId(123L);
        if (project != null) {
            project.setFinancingRound(res);
        }

        when(financingRoundRepository.findActive(any())).thenReturn(res);
        Assert.assertThat(res.active(), Is.is(false));
        return res;
    }

    private FinancingRoundEntity aFinancingRound(DateTime endDate) {
        FinancingRoundEntity res = FinancingRoundEntity.newFinancingRound(7, endDate, FINANCING_ROUND_BUDGET);
        res.setStartDate(new DateTime().minusDays(2));
        return res;
    }

    private Attachment aStoringRequestAttachment() {
        return Attachment.asCreationCommand("test_filename", "text/plain", mockedInputStream("content"));
    }

    private InputStream mockedInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}