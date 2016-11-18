package de.asideas.crowdsource.service;


import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.PledgeEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.financinground.FinancingRoundPostProcessor;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.FinancingRound;
import de.asideas.crowdsource.repository.FinancingRoundRepository;
import de.asideas.crowdsource.repository.PledgeRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.exparity.hamcrest.date.DateMatchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FinancingRoundServiceTest {

    private static final Long ROUND_ID = 4711L;
    public static final BigDecimal POST_ROUND_BUDGET = BigDecimal.valueOf(400);
    public static final BigDecimal ROUND_BUDGET = BigDecimal.valueOf(1000);

    private DateTime fixedDate;
    private List<FinancingRoundEntity> financingRoundEntities;

    @InjectMocks
    private FinancingRoundService financingRoundService;

    @Mock
    private FinancingRoundRepository financingRoundRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FinancingRoundPostProcessor financingRoundPostProcessor;

    @Mock
    private TaskScheduler crowdScheduler;

    @Mock
    private PledgeRepository pledgeRepository;

    @Before
    public void init() {

        reset(financingRoundRepository, userRepository, projectRepository);

        financingRoundEntities = new ArrayList<>();
        fixedDate = DateTime.parse("2015-01-10T10:10:10Z");
        financingRoundEntities.add(financingRoundEntity(ROUND_ID, fixedDate.minusDays(100), fixedDate.minusDays(50)));
        financingRoundEntities.add(financingRoundEntity(ROUND_ID + 2L, fixedDate.minusDays(40), fixedDate.minusDays(30)));
        when(financingRoundRepository.findAll()).thenReturn(financingRoundEntities);

        when(financingRoundRepository.save(any(FinancingRoundEntity.class))).thenAnswer(invocationOnMock -> {
            FinancingRoundEntity round = (FinancingRoundEntity) invocationOnMock.getArguments()[0];
            round.setId(ROUND_ID);
            return round;
        });
        when(financingRoundPostProcessor.postProcess(any(FinancingRoundEntity.class))).thenAnswer(i -> i.getArguments()[0]);
    }


    @Test
    public void allFinancingRounds() throws Exception {
        final BigDecimal pledgedAmountAfterTermination = BigDecimal.valueOf(10);
        financingRoundEntities.get(0).setTerminationPostProcessingDone(true);
        financingRoundEntities.get(1).setTerminationPostProcessingDone(true);
        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(financingRoundEntities.get(1), financingRoundEntities.get(1).getEndDate()))
                .thenReturn(Collections.singletonList(aPledgeEntity(financingRoundEntities.get(1), pledgedAmountAfterTermination)));

        final List<FinancingRound> res = financingRoundService.allFinancingRounds();

        verify(financingRoundRepository, times(1)).findAll();
        verify(pledgeRepository, times(2)).findByFinancingRoundAndCreatedDateGreaterThan(any(FinancingRoundEntity.class), any(DateTime.class));

        assertFinancingRoundDto(financingRoundEntities.get(0), res.get(0), POST_ROUND_BUDGET);
        assertFinancingRoundDto(financingRoundEntities.get(1), res.get(1), POST_ROUND_BUDGET.subtract(pledgedAmountAfterTermination));
    }

    @Test
    public void currentlyActiveRound() throws Exception {
        final FinancingRoundEntity financingRoundEntity = financingRoundEntity(1L, fixedDate.minusDays(100), fixedDate.minusDays(50));
        when(financingRoundRepository.findActive(any()))
                .thenReturn(financingRoundEntity);

        final FinancingRound res = financingRoundService.currentlyActiveRound();

        assertFinancingRoundDto(financingRoundEntity, res, BigDecimal.ZERO);
        verify(financingRoundRepository, times(1)).findActive(any());

    }

    @Test(expected = ResourceNotFoundException.class)
    public void currentlyActiveRound_ThrowsResournceNotFoundExceptionIfNotFonud() throws Exception {

        when(financingRoundRepository.findActive(any(DateTime.class))).thenReturn(null);

        financingRoundService.currentlyActiveRound();
    }

    @Test
    public void mostRecentRound() throws Exception {
        final FinancingRoundEntity expFinancingRound = financingRoundEntity(1L, DateTime.now().minusDays(2), DateTime.now().minusDays(1));

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        Page mockedPageAnswer = mock(Page.class);
        when(financingRoundRepository.findAll(pageRequestCaptor.capture())).thenReturn(mockedPageAnswer);
        when(mockedPageAnswer.getNumberOfElements()).thenReturn(1);
        when(mockedPageAnswer.getContent()).thenReturn(Collections.singletonList(expFinancingRound));

        final FinancingRound res = financingRoundService.mostRecentRound();


        assertThat(res, is(new FinancingRound(expFinancingRound, null)));
        assertThat(pageRequestCaptor.getValue().getSort().getOrderFor("createdDate"), is(new Sort.Order(Sort.Direction.DESC, "createdDate")));
        assertThat(pageRequestCaptor.getValue().getPageSize(), is(1));
        assertThat(pageRequestCaptor.getValue().getPageNumber(), is(0));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void mostRecentRound_throwsExceptionIfNoFinancingRoundsExist() throws Exception {
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        Page mockedPageAnswer = mock(Page.class);

        when(financingRoundRepository.findAll(pageRequestCaptor.capture())).thenReturn(mockedPageAnswer);
        when(mockedPageAnswer.getNumberOfElements()).thenReturn(0);

        financingRoundService.mostRecentRound();
    }

    @Test
    public void startFinancingRound_succeeds() throws Exception {
        givenTwoUsersInDatabase();

        final FinancingRound financingRoundCreationCmd = financingRound(new DateTime().plusDays(1), BigDecimal.valueOf(99));
        ProjectEntity proposedProject = project(ProjectStatus.PROPOSED);
        ProjectEntity deferredProject = project(ProjectStatus.DEFERRED);
        ProjectEntity publishedProject = project(ProjectStatus.PUBLISHED);
        ProjectEntity rejectedProject = project(ProjectStatus.REJECTED);
        when(projectRepository.findAll()).thenReturn(Arrays.asList(
                proposedProject, rejectedProject, deferredProject, project(ProjectStatus.FULLY_PLEDGED), publishedProject, project(ProjectStatus.FULLY_PLEDGED)
        ));
        ArgumentCaptor<UserEntity> userEntityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<ProjectEntity> projectCaptor = ArgumentCaptor.forClass(ProjectEntity.class);
        ArgumentCaptor<FinancingRoundEntity> financingRoundCaptor = ArgumentCaptor.forClass(FinancingRoundEntity.class);

        final FinancingRound res = financingRoundService.startNewFinancingRound(financingRoundCreationCmd);

        assertThat(res.isActive(), is(true));
        assertThat(res.getBudget(), is(BigDecimal.valueOf(99)));
        assertThat(res.getEndDate().getMillis(), is(financingRoundCreationCmd.getEndDate().getMillis()));
        assertThat(res.getStartDate().toDate(), DateMatchers.sameMinute(financingRoundCreationCmd.getStartDate().toDate()));
        assertThat(res.getPostRoundBudget(), is(nullValue()));

        verify(financingRoundRepository, times(1)).save(financingRoundCaptor.capture());
        verify(userRepository).findAll();
        verify(userRepository, times(2)).save(userEntityCaptor.capture());
        userEntityCaptor.getAllValues().stream().forEach(u -> assertThat(u.getBudget(), is(financingRoundCaptor.getValue().getBudgetPerUser())));

        verify(projectRepository, times(4)).save(projectCaptor.capture());
        List<ProjectEntity> updatedProjects = projectCaptor.getAllValues();
        assertTrue("Deferred project has been updated", updatedProjects.contains(deferredProject));
        assertTrue("Proposed project has been updated", updatedProjects.contains(proposedProject));
        assertTrue("Published project has been updated", updatedProjects.contains(publishedProject));
        assertTrue("Rejected project has been updated", updatedProjects.contains(rejectedProject));
        updatedProjects.stream().forEach(p -> assertThat(p.getFinancingRound(), is(financingRoundCaptor.getValue())));

        verify(crowdScheduler).schedule(any(Runnable.class), eq(res.getEndDate().toDate()));
    }


    @Test
    public void startFinancingRound_withSomeDeletedUsers_ShouldOnlyUseNotDeletedUsers() throws Exception {
        final FinancingRound financingRoundCreationCmd = financingRound(new DateTime().plusDays(1), BigDecimal.valueOf(99));
        ProjectEntity publishedProject = project(ProjectStatus.PUBLISHED);
        when(projectRepository.findAll()).thenReturn(Arrays.asList(publishedProject));


        UserEntity user = createUser("test1@mail.com");
        UserEntity deletedUser = createDeletedUser("test2@mail.com");

        givenUsersInDatabase(user, deletedUser);

        final FinancingRound res = financingRoundService.startNewFinancingRound(financingRoundCreationCmd);

        assertEquals(BigDecimal.valueOf(99), user.getBudget());
        assertEquals(BigDecimal.valueOf(0), deletedUser.getBudget());
    }

    @Test
    public void stopFinancingRound() throws Exception {
        final DateTime futureDate = fixedDate.plusDays(5000);
        when(financingRoundRepository.findOne(ROUND_ID)).thenReturn(financingRoundEntity(ROUND_ID, fixedDate.minusDays(100), futureDate));

        FinancingRound res = financingRoundService.stopFinancingRound(ROUND_ID);

        ArgumentCaptor<FinancingRoundEntity> entityCaptor = ArgumentCaptor.forClass(FinancingRoundEntity.class);
        verify(financingRoundRepository, atLeastOnce()).save(entityCaptor.capture());
        verify(financingRoundPostProcessor).postProcess(entityCaptor.getValue());

        assertThat(entityCaptor.getValue().getEndDate(), not(futureDate));
        assertThat(res.isActive(), is(false));
        assertThat(res.getBudget(), is(ROUND_BUDGET));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void stopFinancingRound_missingRoundThrowsResourceNotFoundException() throws Exception {
        Long roundId = 1337L;
        when(financingRoundRepository.findOne(roundId)).thenReturn(null);
        financingRoundService.stopFinancingRound(roundId);
    }

    @Test
    public void stopFinancingRound_alreadyStoppedThrowsInvalidRequestException() throws Exception {
        Long roundId = 1337L;
        when(financingRoundRepository.findOne(anyLong())).thenReturn(financingRoundEntity(null, fixedDate.minusDays(100), fixedDate.minusDays(50)));
        try {
            financingRoundService.stopFinancingRound(roundId);
            fail("Expected InvalidRequestException did not occur");
        } catch (Exception e) {
            assertTrue("Expected an InvalidRequestException", e instanceof InvalidRequestException);
            assertThat(e.getMessage(), is(InvalidRequestException.financingRoundAlreadyStopped().getMessage()));
            verify(financingRoundRepository, never()).save(any(FinancingRoundEntity.class));
        }
    }

    @Test
    public void schedulePostProcessing() throws Exception {
        final FinancingRoundEntity round = financingRoundEntities.get(0);

        prepareSynchronizedCrowdScheduler();
        when(financingRoundRepository.findOne(round.getId())).thenReturn(round);

        financingRoundService.schedulePostProcessing(round);

        verify(financingRoundRepository).findOne(eq(round.getId()));
        verify(financingRoundPostProcessor).postProcess(round);
    }

    @Test
    public void schedulePostProcessing_doesNothinOnNotExistingEntity() throws Exception {
        final FinancingRoundEntity round = financingRoundEntities.get(0);

        prepareSynchronizedCrowdScheduler();
        when(financingRoundRepository.findOne(round.getId())).thenReturn(null);

        financingRoundService.schedulePostProcessing(round);

        verify(financingRoundRepository).findOne(eq(round.getId()));
        verify(financingRoundPostProcessor, never()).postProcess(round);
    }

    @Test
    public void reschedulePostProcessingOfFinancingRounds() throws Exception {
        ArgumentCaptor<Date> scheduleDateCaptor = ArgumentCaptor.forClass(Date.class);

        financingRoundService.reschedulePostProcessingOfFinancingRounds();

        verify(crowdScheduler, times(financingRoundEntities.size())).schedule(any(Runnable.class), scheduleDateCaptor.capture());

        final List<Date> capturedScheduleDates = scheduleDateCaptor.getAllValues();
        assertThat(capturedScheduleDates.size(), is(financingRoundEntities.size()));
        for (int i = 0; i < capturedScheduleDates.size(); i++) {
            assertTrue("Should have captured actual end dates as scheduler dates", capturedScheduleDates.contains(financingRoundEntities.get(i).getEndDate().toDate()));
        }
    }

    @Test
    public void financingRound_ShouldCallRepositoryOnTerminatedPostProcessedRound() throws Exception {
        final FinancingRoundEntity financingRoundEntity = financingRoundEntity(1L, DateTime.now().minusDays(3), DateTime.now().minusDays(2));
        financingRoundEntity.setTerminationPostProcessingDone(true);

        when(pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(any(FinancingRoundEntity.class), any(DateTime.class)))
                .thenReturn(Collections.singletonList(aPledgeEntity(financingRoundEntity, BigDecimal.valueOf(100))));


        final FinancingRound res = financingRoundService.financingRound(financingRoundEntity);
        assertFinancingRoundDto(financingRoundEntity, res, BigDecimal.valueOf(300));
    }

    @Test
    public void financingRound_ShouldNotCallRepositoryOnNonTerminatedNonPostProcessedRound() throws Exception {
        final FinancingRoundEntity financingRoundEntity = financingRoundEntity(1L, DateTime.now().minusDays(2), DateTime.now().plusDays(1));
        final FinancingRound res = financingRoundService.financingRound(financingRoundEntity);

        assertFinancingRoundDto(financingRoundEntity, res, BigDecimal.ZERO);

        verify(pledgeRepository, never()).findByFinancingRoundAndCreatedDateGreaterThan(any(FinancingRoundEntity.class), any(DateTime.class));
    }

    private void givenUsersInDatabase(UserEntity... users) {
        when(userRepository.findAll()).thenReturn(Arrays.asList(users));
    }


    private void givenTwoUsersInDatabase() {
        List<UserEntity> userEntities = new ArrayList<>();
        userEntities.add(new UserEntity("test1@mail.com", "firstname", "lastname"));
        userEntities.add(new UserEntity("test2@mail.com", "firstname", "lastname"));
        when(userRepository.findAll()).thenReturn(userEntities);
    }

    private void assertFinancingRoundDto(FinancingRoundEntity financingRoundEntity, FinancingRound res, BigDecimal expPostRoundBudgetRemaining) {
        assertThat(res.getBudget(), is(financingRoundEntity.getBudget()));
        assertThat(res.getPostRoundBudget(), is(financingRoundEntity.getPostRoundBudget()));
        assertThat(res.getPostRoundBudgetRemaining(), is(expPostRoundBudgetRemaining));
        assertThat(res.getStartDate(), is(financingRoundEntity.getStartDate()));
        assertThat(res.getEndDate(), is(financingRoundEntity.getEndDate()));
        assertThat(res.isActive(), is(financingRoundEntity.active()));
        assertThat(res.getId(), is(financingRoundEntity.getId()));
    }

    private void prepareSynchronizedCrowdScheduler() {
        when(crowdScheduler.schedule(any(Runnable.class), any(Date.class))).thenAnswer(i -> {
            ((Runnable) i.getArguments()[0]).run();
            return null;
        });
    }

    private FinancingRound financingRound(DateTime end, BigDecimal budget) {
        FinancingRound financingRound = new FinancingRound();
        financingRound.setEndDate(end);
        financingRound.setBudget(budget);
        return financingRound;
    }

    private FinancingRoundEntity financingRoundEntity(Long roundId, DateTime start, DateTime end) {
        FinancingRoundEntity res = new FinancingRoundEntity();
        res.setId(roundId);
        res.setStartDate(start);
        res.setEndDate(end);
        res.setBudget(ROUND_BUDGET);
        res.setPostRoundBudget(POST_ROUND_BUDGET);
        return res;
    }

    private ProjectEntity project(ProjectStatus status) {
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setStatus(status);
        projectEntity.setId(new Random(Long.MAX_VALUE).nextLong());
        return projectEntity;
    }

    private PledgeEntity aPledgeEntity(FinancingRoundEntity financingRoundEntity, BigDecimal pledgeAmount) {
        final PledgeEntity res = new PledgeEntity(new ProjectEntity(), null, pledgeAmount, financingRoundEntity);
        res.setCreatedDate(DateTime.now());
        return res;
    }

    private UserEntity createDeletedUser(String email) {
        UserEntity user = createUser(email);
        user.setDeleted(true);
        return user;

    }

    private UserEntity createUser(String email) {
        return new UserEntity(email, "firstname", "lastname");
    }

}