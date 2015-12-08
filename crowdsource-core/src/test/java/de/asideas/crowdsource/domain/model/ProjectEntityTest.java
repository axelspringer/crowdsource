package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.presentation.FinancingRound;
import de.asideas.crowdsource.domain.presentation.Pledge;
import de.asideas.crowdsource.domain.presentation.project.Project;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.security.Roles;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ProjectEntityTest {

    private static final int PLEDGE_GOAL = 200;
    final int COUNT_POST_ROUND_PLEDGES = 5;
    int postRoundPledgedAmount = 0;

    private ProjectEntity projectEntity;
    private UserEntity user1;
    private UserEntity user2;
    private UserEntity user3;

    @Before
    public void setUp() {
        postRoundPledgedAmount = 0;
        UserEntity creator = new UserEntity();
        creator.setId("id");

        user1 = new UserEntity("user1@xyz.com");
        user1.setId("test_id1");
        user1.setBudget(100);
        user2 = new UserEntity("user2@xyz.com");
        user2.setId("test_id2");
        user2.setBudget(200);
        user2.setRoles(Arrays.asList(Roles.ROLE_USER, Roles.ROLE_ADMIN));
        user3 = new UserEntity("user3@xyz.com");
        user3.setId("test_id3");

        Project project = new Project();
        projectEntity = new ProjectEntity(user3, project, anActiveFinancingRound());
        projectEntity.setPledgeGoal(PLEDGE_GOAL);
    }

    /**
     * For a full integration test, checking all edge cases see {@link de.asideas.crowdsource.service.ProjectServiceTest}
     *
     * @throws Exception
     */
    @Test
    public void pledge() throws Exception {
        final List<PledgeEntity> pledgesDoneBeforw = bunchOfPledgesDone();
        final Pledge pledge = new Pledge(40);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        int userBudgetBefore = user1.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledge(pledge, user1, pledgesDoneBeforw);

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, user1, pledge, projectEntity.getFinancingRound())));
        assertThat(user1.getBudget(), is(userBudgetBefore - pledge.getAmount()));
        assertThat(projectEntity.pledgeGoalAchieved(), is(true));
    }

    @Test
    public void pledge_reverse() throws Exception {
        final List<PledgeEntity> pledgesDoneBeforw = bunchOfPledgesDone();
        final Pledge pledge = new Pledge(-10);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        int userBudgetBefore = user1.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledge(pledge, user1, pledgesDoneBeforw);

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, user1, pledge, projectEntity.getFinancingRound())));
        assertThat(user1.getBudget(), is(userBudgetBefore + Math.abs(pledge.getAmount())));
    }

    @Test
    public void pledge_reversePledgeThrowsInvalidRequestExWhenExceedingPledgeAmountAlreadyMade() throws Exception {
        final int budgetBeforePledge = user1.getBudget();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(-5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.reversePledgeExceeded(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_reversePledgeThrowsInvalidRequestExWhenAlreadyFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(-5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), user1, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnPledgeGoalIsExceeded() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnZeroPledge() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(0);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.zeroPledgeNotValid(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectThatIsAlreadyFullyPledged() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal()));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), user1, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectIsDeferred() {
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(0));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), user1, budgetBeforePledge, ProjectStatus.DEFERRED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectIsNotPublished() {
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), user1, budgetBeforePledge, ProjectStatus.PROPOSED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnUserBudgetIsExceeded() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(user1.getBudget() + 10);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.userBudgetExceeded(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnNoFinancingRound() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(4);
        projectEntity.setFinancingRound(null);


        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.noFinancingRoundCurrentlyActive(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnInactiveFinancingRound() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user1.getBudget();
        final Pledge pledge = new Pledge(4);
        projectEntity.setFinancingRound(aTerminatedFinancingRound());

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.noFinancingRoundCurrentlyActive(), user1, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }


    @Test
    public void pledgeUsingPostroundBudget(){
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        final List<PledgeEntity> pledgesDoneBefore = bunchOfWithPostRoundPledgesDone();
        final Pledge pledge = new Pledge(projectEntity.getPledgeGoal() - projectEntity.pledgedAmount(pledgesDoneBefore));
        int userBudgetBefore = user2.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesDoneBefore, 400);

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, user2, pledge, projectEntity.getFinancingRound())));
        assertThat(user2.getBudget(), is(userBudgetBefore));
        assertThat(projectEntity.pledgeGoalAchieved(), is(true));
    }

    @Test
    public void pledgeUsingPostroundBudget_reverse(){
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final List<PledgeEntity> pledgesInRound = bunchOfPledgesDone();
        final List<PledgeEntity> postRoundPledges = bunchOfPostRoundPledges();
        final List<PledgeEntity> pledgesDone = new ArrayList<>(pledgesInRound);
        pledgesDone.addAll(postRoundPledges);
        final Pledge pledge = new Pledge(-10);
        int userBudgetBefore = user2.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesDone,
                round.postRoundPledgableBudgetRemaining(postRoundPledges));


        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, user2, pledge, projectEntity.getFinancingRound())));
        assertThat(user2.getBudget(), is(userBudgetBefore));
        assertThat(projectEntity.pledgeGoalAchieved(), is(false));

        postRoundPledges.add(pledgeRes);
        pledgeRes.setCreatedDate(new DateTime());
        assertThat(round.postRoundPledgableBudgetRemaining(postRoundPledges), is(round.getPostRoundBudget() - postRoundPledgedAmount - pledge.getAmount()));
    }

    @Test
    public void pledgeUsingPostroundBudget_reversePledgeThrowsInvalidRequestExWhenExceedingPledgeAmountAlreadyMade() throws Exception {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final List<PledgeEntity> pledgesInRound = bunchOfPledgesDone();
        final List<PledgeEntity> postRoundPledges = bunchOfPostRoundPledges();
        final List<PledgeEntity> pledgesDone = new ArrayList<>(pledgesInRound);
        pledgesDone.addAll(postRoundPledges);
        final Pledge pledge = new Pledge( -postRoundPledgedAmount - 1);
        int userBudgetBefore = user2.getBudget();

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesDone,
                    round.postRoundPledgableBudgetRemaining(postRoundPledges));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.reversePledgeExceeded(), user2, userBudgetBefore, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_reversePledgeThrowsInvalidRequestExWhenAlreadyFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final int budgetBeforePledge = user1.getBudget();
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        final Pledge pledge = new Pledge(-5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(10), Integer.MAX_VALUE);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), user1, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectThatIsAlreadyFullyPledged() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal()), Integer.MAX_VALUE);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), user2, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnPledgeGoalIsExceeded() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final List<PledgeEntity> pledgesDone = bunchOfWithPostRoundPledgesDone();
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(projectEntity.getPledgeGoal() + 1);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesDone, Integer.MAX_VALUE);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), user2, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnZeroPledge() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(0);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), Integer.MAX_VALUE);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.zeroPledgeNotValid(), user2, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectIsDeferred() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        projectEntity.setFinancingRound(round);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(0), 0);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), user2, budgetBeforePledge, ProjectStatus.DEFERRED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectIsNotPublished() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(5);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(1), 0);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), user2, budgetBeforePledge, ProjectStatus.PROPOSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExOnNoFinancingRound() {
        projectEntity.setFinancingRound(null);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(4);

        projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExceptionIfCalledByNonAdminUser() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(4);

        projectEntity.pledgeUsingPostRoundBudget(pledge, user1, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExceptionOnNotTerminatedRound() {
        final FinancingRoundEntity round = anActiveFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(4);

        projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), Integer.MAX_VALUE);
    }


    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnUserBudgetIsExceeded() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(10);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), 5);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.postRoundBudgetExceeded(), user2, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnFinancingRoundNotPostProcessed() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        round.setTerminationPostProcessingDone(false);
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final int budgetBeforePledge = user2.getBudget();
        final Pledge pledge = new Pledge(10);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge, user2, pledgesAlreadyDone(projectEntity.getPledgeGoal() - 4), Integer.MAX_VALUE);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.financingRoundNotPostProcessedYet(), user2, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeGoalAchieved_ReturnsFalseIfNotFullyPledged() throws Exception {
        assertThat(projectEntity.pledgeGoalAchieved(), is(false));
    }

    @Test
    public void pledgeGoalAchieved_returnsTrueWhenFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        assertThat(projectEntity.pledgeGoalAchieved(), is(true));
    }

    @Test
    public void pledgedAmount() throws Exception {
        final FinancingRoundEntity activeFinancingRound = new FinancingRoundEntity();
        assertThat(projectEntity.pledgedAmount(bunchOfPledgesDone()), is(160));
    }

    @Test
    public void pledgedAmount_isZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.pledgedAmount(new ArrayList<>()), is(0));
    }

    @Test
    public void countBackers() throws Exception {
        final FinancingRoundEntity activeFinancingRound = new FinancingRoundEntity();
        assertThat(projectEntity.countBackers(bunchOfPledgesDone()), is(2L));
    }

    @Test
    public void countBackers_isZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.countBackers(new ArrayList<>()), is(0L));
    }

    @Test
    public void pledgedAmountOfUser() throws Exception {
        final FinancingRoundEntity activeFinancingRound = new FinancingRoundEntity();
        assertThat(projectEntity.pledgedAmountOfUser(bunchOfPledgesDone(), user1), is(30));
    }

    @Test
    public void pledgedAmountOfUser_ReturnsZeroOnNullUser() throws Exception {
        final FinancingRoundEntity activeFinancingRound = new FinancingRoundEntity();
        assertThat(projectEntity.pledgedAmountOfUser(bunchOfPledgesDone(), null), is(0));
    }

    @Test
    public void pledgedAmountOfUser_ReturnsZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.pledgedAmountOfUser(new ArrayList<>(), user1), is(0));
    }

    @Test
    public void modifyStatus() throws Exception {
        boolean res = projectEntity.modifyStatus(ProjectStatus.PUBLISHED);

        MatcherAssert.assertThat(res, Matchers.is(true));
        MatcherAssert.assertThat(projectEntity.getStatus(), Matchers.is(ProjectStatus.PUBLISHED));
    }

    @Test
    public void modifyStatus_settingToPublishedAlthoughFullyPledgedThrowsIvalidRequestEx() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);

        try {
            projectEntity.modifyStatus(ProjectStatus.PUBLISHED);
            fail("Expected InvalidRequestException was not thrown");
        } catch (InvalidRequestException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.is(InvalidRequestException.projectAlreadyFullyPledged().getMessage()));
        }
    }

    @Test
    public void modifyStatus_settingToDeferredThrowsExceptionWhenFinancingRoundAssignedAndActive() throws Exception {
        try {
            projectEntity.setFinancingRound(anActiveFinancingRound());
            projectEntity.modifyStatus(ProjectStatus.DEFERRED);
            fail("Expected InvalidRequestException was not thrown");
        } catch (InvalidRequestException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.is(InvalidRequestException.projectAlreadyInFinancingRound().getMessage()));
        }
    }

    @Test
    public void modifyStatus_settingToDeferredThrowsExceptionWhenAlreadyFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);

        try {
            projectEntity.modifyStatus(ProjectStatus.DEFERRED);
            fail("Expected InvalidRequestException was not thrown");
        } catch (InvalidRequestException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.is(InvalidRequestException.projectAlreadyFullyPledged().getMessage()));
        }
    }

    @Test
    public void modifyStatus_settingToDeferredThrowsExceptionWhenRejected() throws Exception {
        projectEntity.setStatus(ProjectStatus.REJECTED);
        projectEntity.setFinancingRound(null);

        try {
            projectEntity.modifyStatus(ProjectStatus.DEFERRED);
            fail("Expected InvalidRequestException was not thrown");
        } catch (InvalidRequestException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.is(InvalidRequestException.setToDeferredNotPossibleOnRejected().getMessage()));
        }
    }

    @Test
    public void modifyStatus_settingToPublishedPossibleWhenDeferred() throws Exception {
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.PUBLISHED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PUBLISHED));
    }

    @Test
    public void modifyStatus_settingToDeferredPossibleWhenAlreadyPublished() throws Exception {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.DEFERRED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.DEFERRED));
    }

    @Test
    public void modifyStatus_settingToDeferredPossibleWhenAssignedToNonActiveFinancingRound() throws Exception {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(aTerminatedFinancingRound());

        boolean res = projectEntity.modifyStatus(ProjectStatus.DEFERRED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.DEFERRED));
    }

    @Test
    public void modifyStatus_settingToProposedPossibleWhenAlreadyPublished() throws Exception {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.PROPOSED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PROPOSED));
    }

    @Test
    public void modifyStatus_settingToProposedPossibleWhenAlreadyDeferred() throws Exception {
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.PROPOSED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PROPOSED));
    }

    @Test
    public void modifyStatus_settingToProposedPossibleWhenAlreadyRejected() throws Exception {
        projectEntity.setStatus(ProjectStatus.REJECTED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.PROPOSED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PROPOSED));
    }

    @Test
    public void modifyStatus_settingToPublishedPossibleWhenAlreadyRejected() throws Exception {
        projectEntity.setStatus(ProjectStatus.REJECTED);
        projectEntity.setFinancingRound(null);

        boolean res = projectEntity.modifyStatus(ProjectStatus.PUBLISHED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PUBLISHED));
    }


    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingNoFinancingRound() throws Exception {
        projectEntity.setFinancingRound(null);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingNotActiveFinancingRound() throws Exception{
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));

    }

    @Test
    public void masterdataModificationAllowed_shouldReturnFalseHavingActiveFinancingRund() throws Exception {
        projectEntity.setFinancingRound(anActiveFinancingRound());
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(false));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnFalseHavingStatusFullyPledged() throws Exception{
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(false));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingStatusProposed() throws Exception{
        projectEntity.setFinancingRound(anActiveFinancingRound());
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingStatusDeferred() throws Exception{
        projectEntity.setFinancingRound(anActiveFinancingRound());
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataChanged_returnsTrueIfOneFieldChanges() throws Exception{
        Project modifiedProject;
        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), user1);
        // When
        modifiedProject.setTitle(projectEntity.getTitle() + "_CHANGED");
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), user1);
        // When
        modifiedProject.setDescription(null);
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), user1);
        // When
        modifiedProject.setShortDescription(projectEntity.getShortDescription() + "_CHANGED");
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), user1);
        // When
        modifiedProject.setPledgeGoal(projectEntity.getPledgeGoal() + 25);
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject), is(true));

    }

    @Test
    public void masterdataChanged_returnsFalseOnNoChange()throws Exception{
        prepareMasterData(projectEntity);
        Project unmodifiedProject = new Project(projectEntity, Collections.emptyList(), user1);

        assertThat(projectEntity.masterdataChanged(unmodifiedProject), is(false));
    }

    @Test
    public void modifyMasterData_returnsTrueWhenSuccessfullyModifiedByOwner() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), user1);
        modCmd.setDescription("CHANGED_DESCRIPTION");

        assertThat(projectEntity.modifyMasterdata(modCmd, user3), is(true));
    }
    @Test
    public void modifyMasterData_returnsTrueWhenSuccessfullyModifiedByAdmin() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), user2);
        modCmd.setDescription("CHANGED_DESCRIPTION");

        assertThat(projectEntity.modifyMasterdata(modCmd, user2), is(true));
    }

    @Test
    public void modifyMasterData_returnsFalseWhenNothingChanged() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), user1);

        assertThat(projectEntity.modifyMasterdata(modCmd, user3), is(false));
    }

    @Test(expected = NotAuthorizedException.class)
    public void modifyMasterData_throwsNotAuthorizedExceptionOnIllegalUser() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), user1);

        assertThat(projectEntity.modifyMasterdata(modCmd, user1), is(false));
    }

    @Test
    public void modifyMasterData_throwsInvalidReqExWhenChangeNotAllowed() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        Project modCmd = new Project(projectEntity, Collections.emptyList(), user1);
        modCmd.setDescription("CHANGED_DESCR");

        try{
            projectEntity.modifyMasterdata(modCmd, user3);
            fail("Exception expected to be thrown");
        }catch(InvalidRequestException e){
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
        assertThat(projectEntity.getDescription(), not(equalTo(modCmd.getDescription())));

    }

    @Test
    public void onFinancingRoundTerminated_ProjectPublishedWhenDeferred() throws Exception {
        projectEntity.setStatus(ProjectStatus.DEFERRED);

        projectEntity.onFinancingRoundTerminated(projectEntity.getFinancingRound());

        assertThat(projectEntity.getStatus(), is(ProjectStatus.PUBLISHED));
        assertThat(projectEntity.getFinancingRound(), is(nullValue()));
    }

    @Test
    public void onFinancingRoundTerminated_ProjectUnchangedWhenDeferredButDifferentFinancingRound() throws Exception {
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        projectEntity.onFinancingRoundTerminated(aTerminatedFinancingRound());

        assertThat(projectEntity.getStatus(), is(ProjectStatus.DEFERRED));
        assertThat(projectEntity.getFinancingRound().getId(), is(anActiveFinancingRound().getId()));
    }

    @Test
    public void pledgedAmountPostRound() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(bunchOfWithPostRoundPledgesDone()), is(1 + 2 + 3 + 4 + 5));
    }

    @Test
    public void pledgedAmountPostRound_returnsZeroOnNoRoundNoTerminatedRoundAndNullOrEmptyPledges() throws Exception {
        List<PledgeEntity> pledges = bunchOfPledgesDone();

        projectEntity.setFinancingRound(null);
        assertThat(projectEntity.pledgedAmountPostRound(pledges), is(0));

        projectEntity.setFinancingRound(anActiveFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(pledges), is(0));

        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(null), is(0));

        assertThat(projectEntity.pledgedAmountPostRound(Collections.emptyList()), is(0));
    }

    private FinancingRoundEntity aTerminatedFinancingRound() {
        FinancingRoundEntity res = aFinancingRound(new DateTime().minusDays(1));
        assertThat(res.active(), is(false));
        res.setId("test_IdInActive");
        res.setCreatedDate(new DateTime().minusDays(3));

        res.initPostRoundBudget(bunchOfPledgesDone().stream().mapToInt(p -> p.getAmount()).sum());
        res.setTerminationPostProcessingDone(true);
        return res;
    }

    private FinancingRoundEntity anActiveFinancingRound() {
        FinancingRoundEntity res = aFinancingRound(new DateTime().plusDays(1));
        assertThat(res.active(), is(true));
        res.setId("test_IdActive");
        return res;
    }

    private List<PledgeEntity> pledgesAlreadyDone(int pledgeAmount) {
        if(pledgeAmount == projectEntity.getPledgeGoal()){
            projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        }
        return Collections.singletonList(new PledgeEntity(projectEntity, user1, new Pledge(pledgeAmount), projectEntity.getFinancingRound()));
    }

    private void assertPledgeNotExecuted(RuntimeException actualEx, RuntimeException expEx, UserEntity user, int userBudgetBeforePledge, ProjectStatus expStatus) {
        assertThat(actualEx.getMessage(), is(expEx.getMessage()));
        assertThat(user.getBudget(), is(userBudgetBeforePledge));
        assertThat(projectEntity.getStatus(), is(expStatus));
    }

    private FinancingRoundEntity aFinancingRound(DateTime endDate) {
        FinancingRound creationCmd = new FinancingRound();
        creationCmd.setEndDate(endDate);
        creationCmd.setBudget(1000);
        FinancingRoundEntity res = FinancingRoundEntity.newFinancingRound(creationCmd, 7);
        res.setStartDate(new DateTime().minusDays(2));
        return res;
    }

    private List<PledgeEntity> bunchOfPledgesDone() {
        List<PledgeEntity> res = new ArrayList<>();
        res.add(new PledgeEntity(projectEntity, user1, new Pledge(10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user2, new Pledge(60), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user3, new Pledge(180), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user1, new Pledge(20), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user2, new Pledge(70), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user1, new Pledge(10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user1, new Pledge(-10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user3, new Pledge(-180), projectEntity.getFinancingRound()));

        res.add(new PledgeEntity(projectEntity, user3, new Pledge(-10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user3, new Pledge(110), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user3, new Pledge(+10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, user3, new Pledge(-110), projectEntity.getFinancingRound()));
        return res;
    }

    private List<PledgeEntity> bunchOfWithPostRoundPledgesDone() {
        List<PledgeEntity> res = bunchOfPledgesDone();
        res.addAll(bunchOfPostRoundPledges());
        return res;
    }

    private List<PledgeEntity> bunchOfPostRoundPledges() {
        List<PledgeEntity> res = new ArrayList<>();
        PledgeEntity pledge;
        for (int i = 1; i < COUNT_POST_ROUND_PLEDGES + 1; i++) {
            pledge = new PledgeEntity(projectEntity, user2, new Pledge(i), projectEntity.getFinancingRound());
            pledge.setCreatedDate(projectEntity.getFinancingRound().getEndDate().plusHours(2 * i));
            postRoundPledgedAmount += i;
            res.add(pledge);
        }
        return res;
    }

    private ProjectEntity prepareMasterData(ProjectEntity projectEntity) {
        projectEntity.setTitle("test_Title");
        projectEntity.setDescription("test_Description");
        projectEntity.setShortDescription("test_shortDescription");
        projectEntity.setPledgeGoal(17);
        return projectEntity;
    }
}