package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.FinancingRound;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.security.Roles;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
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

    private static final BigDecimal PLEDGE_GOAL = BigDecimal.valueOf(200);
    final int COUNT_POST_ROUND_PLEDGES = 5;
    BigDecimal postRoundPledgedAmount;

    private ProjectEntity projectEntity;
    private UserEntity aUser;
    private UserEntity adminUser;
    private UserEntity projectCreator;

    @Before
    public void setUp() {
        postRoundPledgedAmount = BigDecimal.ZERO;
        UserEntity creator = new UserEntity();
        creator.setId(100L);

        aUser = new UserEntity("aUser@xyz.com", "firstname", "lastname");
        aUser.setId(101L);
        aUser.setBudget(BigDecimal.valueOf(100));
        adminUser = new UserEntity("adminUser@xyz.com", "firstname", "lastname");
        adminUser.setId(102L);
        adminUser.setBudget(BigDecimal.valueOf(200));
        adminUser.setRoles(Arrays.asList(Roles.ROLE_USER, Roles.ROLE_ADMIN));
        projectCreator = new UserEntity("projectCreator@xyz.com", "firstname", "lastname");
        projectCreator.setId(103L);

        Project project = new Project();
        projectEntity = new ProjectEntity(project.getTitle(), project.getShortDescription(), project.getDescription(), project.getPledgeGoal(), anActiveFinancingRound());
        projectEntity.setPledgeGoal(PLEDGE_GOAL);
        projectEntity.setCreator(projectCreator);
    }

    /**
     * For a full integration test, checking all edge cases see {@link de.asideas.crowdsource.service.ProjectServiceTest}
     *
     * @throws Exception
     */
    @Test
    public void pledge() throws Exception {
        final List<PledgeEntity> pledgesDoneBefore = bunchOfPledgesDone();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(40));
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        BigDecimal userBudgetBefore = aUser.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledge(pledge.getAmount(), aUser, pledgesDoneBefore);

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, aUser, pledge.getAmount(), projectEntity.getFinancingRound())));
        assertThat(aUser.getBudget(), is(userBudgetBefore.subtract(pledge.getAmount())));
        assertThat(projectEntity.pledgeGoalAchieved(), is(true));
    }

    @Test
    public void pledge_reverse() throws Exception {
        final List<PledgeEntity> pledgesDoneBefore = bunchOfPledgesDone();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-10));
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        BigDecimal userBudgetBefore = aUser.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledge(pledge.getAmount(), aUser, pledgesDoneBefore);

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, aUser, pledge.getAmount(), projectEntity.getFinancingRound())));
        assertThat(aUser.getBudget(), is(userBudgetBefore.add(pledge.getAmount().abs())));
    }

    @Test
    public void pledge_reversePledgeThrowsInvalidRequestExWhenExceedingPledgeAmountAlreadyMade() throws Exception {
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(BigDecimal.valueOf(4)));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.reversePledgeExceeded(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_reversePledgeThrowsInvalidRequestExWhenAlreadyFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(BigDecimal.valueOf(4)));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), aUser, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnPledgeGoalIsExceeded() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnZeroPledge() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.ZERO);

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.zeroPledgeNotValid(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectThatIsAlreadyFullyPledged() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal()));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), aUser, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectIsDeferred() {
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(BigDecimal.ZERO));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), aUser, budgetBeforePledge, ProjectStatus.DEFERRED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnProjectIsNotPublished() {
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), aUser, budgetBeforePledge, ProjectStatus.PROPOSED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnUserBudgetIsExceeded() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(aUser.getBudget().add(BigDecimal.valueOf(10)));

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.userBudgetExceeded(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnNoFinancingRound() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        projectEntity.setFinancingRound(null);


        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.noFinancingRoundCurrentlyActive(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledge_throwsInvalidRequestExOnInactiveFinancingRound() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));
        projectEntity.setFinancingRound(aTerminatedFinancingRound());

        InvalidRequestException res = null;
        try {
            projectEntity.pledge(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.noFinancingRoundCurrentlyActive(), aUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }


    @Test
    public void pledgeUsingPostroundBudget() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        final List<PledgeEntity> pledgesDoneBefore = bunchOfWithPostRoundPledgesDone();
        final Pledge pledge = new Pledge(projectEntity.getPledgeGoal().subtract(projectEntity.pledgedAmount(pledgesDoneBefore)));
        BigDecimal userBudgetBefore = adminUser.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesDoneBefore, BigDecimal.valueOf(400));

        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, adminUser, pledge.getAmount(), projectEntity.getFinancingRound())));
        assertThat(adminUser.getBudget(), is(userBudgetBefore));
        assertThat(projectEntity.pledgeGoalAchieved(), is(true));
    }

    @Test
    public void pledgeUsingPostroundBudget_reverse() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final List<PledgeEntity> pledgesInRound = bunchOfPledgesDone();
        final List<PledgeEntity> postRoundPledges = bunchOfPostRoundPledges();
        final List<PledgeEntity> pledgesDone = new ArrayList<>(pledgesInRound);
        pledgesDone.addAll(postRoundPledges);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-10));
        BigDecimal userBudgetBefore = adminUser.getBudget();

        final PledgeEntity pledgeRes = projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesDone,
                round.postRoundPledgableBudgetRemaining(postRoundPledges));


        assertThat(pledgeRes, is(new PledgeEntity(projectEntity, adminUser, pledge.getAmount(), projectEntity.getFinancingRound())));
        assertThat(adminUser.getBudget(), is(userBudgetBefore));
        assertThat(projectEntity.pledgeGoalAchieved(), is(false));

        postRoundPledges.add(pledgeRes);
        pledgeRes.setCreatedDate(new DateTime());
        assertThat(round.postRoundPledgableBudgetRemaining(postRoundPledges), is(round.getPostRoundBudget().subtract(postRoundPledgedAmount).subtract(pledge.getAmount())));
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
        final Pledge pledge = new Pledge(postRoundPledgedAmount.add(BigDecimal.ONE).multiply(BigDecimal.valueOf(-1)));
        BigDecimal userBudgetBefore = adminUser.getBudget();

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesDone,
                    round.postRoundPledgableBudgetRemaining(postRoundPledges));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.reversePledgeExceeded(), adminUser, userBudgetBefore, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_reversePledgeThrowsInvalidRequestExWhenAlreadyFullyPledged() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final BigDecimal budgetBeforePledge = aUser.getBudget();
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        final Pledge pledge = new Pledge(BigDecimal.valueOf(-5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(BigDecimal.valueOf(10)), BigDecimal.valueOf(Integer.MAX_VALUE));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), aUser, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectThatIsAlreadyFullyPledged() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal()), BigDecimal.valueOf(Integer.MAX_VALUE));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectAlreadyFullyPledged(), adminUser, budgetBeforePledge, ProjectStatus.FULLY_PLEDGED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnPledgeGoalIsExceeded() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final List<PledgeEntity> pledgesDone = bunchOfWithPostRoundPledgesDone();
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(projectEntity.getPledgeGoal().add(BigDecimal.ONE));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesDone, BigDecimal.valueOf(Integer.MAX_VALUE));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.pledgeGoalExceeded(), adminUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnZeroPledge() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(round);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.ZERO);

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(Integer.MAX_VALUE));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.zeroPledgeNotValid(), adminUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectIsDeferred() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        projectEntity.setFinancingRound(round);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(BigDecimal.ZERO), BigDecimal.ZERO);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), adminUser, budgetBeforePledge, ProjectStatus.DEFERRED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnProjectIsNotPublished() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(5));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(BigDecimal.ONE), BigDecimal.ZERO);
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.projectNotPublished(), adminUser, budgetBeforePledge, ProjectStatus.PROPOSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExOnNoFinancingRound() {
        projectEntity.setFinancingRound(null);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(Integer.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExceptionIfCalledByNonAdminUser() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), aUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(Integer.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pledgeUsingPostroundBudget_throwsIllegalArgumentExceptionOnNotTerminatedRound() {
        final FinancingRoundEntity round = anActiveFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final Pledge pledge = new Pledge(BigDecimal.valueOf(4));

        projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(Integer.MAX_VALUE));
    }


    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnUserBudgetIsExceeded() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(10));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(5));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.postRoundBudgetExceeded(), adminUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
    }

    @Test
    public void pledgeUsingPostroundBudget_throwsInvalidRequestExOnFinancingRoundNotPostProcessed() {
        final FinancingRoundEntity round = aTerminatedFinancingRound();
        round.setTerminationPostProcessingDone(false);
        projectEntity.setFinancingRound(round);
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        final BigDecimal budgetBeforePledge = adminUser.getBudget();
        final Pledge pledge = new Pledge(BigDecimal.valueOf(10));

        InvalidRequestException res = null;
        try {
            projectEntity.pledgeUsingPostRoundBudget(pledge.getAmount(), adminUser, pledgesAlreadyDone(projectEntity.getPledgeGoal().subtract(BigDecimal.valueOf(4))), BigDecimal.valueOf(Integer.MAX_VALUE));
            fail("InvalidRequestException expected!");
        } catch (InvalidRequestException e) {
            res = e;
        }

        assertPledgeNotExecuted(res, InvalidRequestException.financingRoundNotPostProcessedYet(), adminUser, budgetBeforePledge, ProjectStatus.PUBLISHED);
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
        assertThat(projectEntity.pledgedAmount(bunchOfPledgesDone()), is(BigDecimal.valueOf(160)));
    }

    @Test
    public void pledgedAmount_isZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.pledgedAmount(new ArrayList<>()), is(BigDecimal.ZERO));
    }

    @Test
    public void countBackers() throws Exception {
        assertThat(projectEntity.countBackers(bunchOfPledgesDone()), is(2L));
    }

    @Test
    public void countBackers_isZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.countBackers(new ArrayList<>()), is(0L));
    }

    @Test
    public void pledgedAmountOfUser() throws Exception {
        assertThat(projectEntity.pledgedAmountOfUser(bunchOfPledgesDone(), aUser), is(BigDecimal.valueOf(30)));
    }

    @Test
    public void pledgedAmountOfUser_ReturnsZeroOnNullUser() throws Exception {
        assertThat(projectEntity.pledgedAmountOfUser(bunchOfPledgesDone(), null), is(BigDecimal.ZERO));
    }

    @Test
    public void pledgedAmountOfUser_ReturnsZeroOnEmptyPledges() throws Exception {
        assertThat(projectEntity.pledgedAmountOfUser(new ArrayList<>(), aUser), is(BigDecimal.ZERO));
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
    public void modifyStatus_settingToPublishedDeferred_notPossibleWhenAlreadyPublishedInRound() throws Exception {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(anActiveFinancingRound());

        try {
            projectEntity.modifyStatus(ProjectStatus.PUBLISHED_DEFERRED);
            fail("Expected InvalidRequestException was not thrown");
        } catch (InvalidRequestException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.is(InvalidRequestException.projectAlreadyInFinancingRound().getMessage()));
        }
    }

    @Test
    public void modifyStatus_settingToPublishedDeferred_PossibleOnActiveRound() throws Exception {
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        projectEntity.setFinancingRound(anActiveFinancingRound());

        boolean res = projectEntity.modifyStatus(ProjectStatus.PUBLISHED_DEFERRED);

        assertThat(res, is(true));
        assertThat(projectEntity.getStatus(), is(ProjectStatus.PUBLISHED_DEFERRED));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingNoFinancingRound() throws Exception {
        projectEntity.setFinancingRound(null);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingNotActiveFinancingRound() throws Exception {
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
    public void masterdataModificationAllowed_shouldReturnFalseHavingStatusFullyPledged() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(false));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingStatusProposed() throws Exception {
        projectEntity.setFinancingRound(anActiveFinancingRound());
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataModificationAllowed_shouldReturnTrueHavingStatusDeferred() throws Exception {
        projectEntity.setFinancingRound(anActiveFinancingRound());
        projectEntity.setStatus(ProjectStatus.DEFERRED);
        assertThat(projectEntity.masterdataModificationAllowed(), is(true));
    }

    @Test
    public void masterdataChanged_returnsTrueIfOneFieldChanges() throws Exception {
        Project modifiedProject;
        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), aUser);
        // When
        modifiedProject.setTitle(projectEntity.getTitle() + "_CHANGED");
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject.getTitle(), modifiedProject.getDescription(), modifiedProject.getShortDescription(), modifiedProject.getPledgeGoal()), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), aUser);
        // When
        modifiedProject.setDescription(null);
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject.getTitle(), modifiedProject.getDescription(), modifiedProject.getShortDescription(), modifiedProject.getPledgeGoal()), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), aUser);
        // When
        modifiedProject.setShortDescription(projectEntity.getShortDescription() + "_CHANGED");
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject.getTitle(), modifiedProject.getDescription(), modifiedProject.getShortDescription(), modifiedProject.getPledgeGoal()), is(true));

        // Given
        prepareMasterData(projectEntity);
        modifiedProject = new Project(projectEntity, Collections.emptyList(), aUser);
        // When
        modifiedProject.setPledgeGoal(projectEntity.getPledgeGoal().add(BigDecimal.valueOf(25)));
        // Then
        assertThat(projectEntity.masterdataChanged(modifiedProject.getTitle(), modifiedProject.getDescription(), modifiedProject.getShortDescription(), modifiedProject.getPledgeGoal()), is(true));

    }

    @Test
    public void masterdataChanged_returnsFalseOnNoChange() throws Exception {
        prepareMasterData(projectEntity);
        Project unmodifiedProject = new Project(projectEntity, Collections.emptyList(), aUser);

        assertThat(projectEntity.masterdataChanged(unmodifiedProject.getTitle(), unmodifiedProject.getDescription(), unmodifiedProject.getShortDescription(), unmodifiedProject.getPledgeGoal()), is(false));
    }

    @Test
    public void modifyMasterData_returnsTrueWhenSuccessfullyModifiedByOwner() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), aUser);
        modCmd.setDescription("CHANGED_DESCRIPTION");

        assertThat(projectEntity.modifyMasterdata(projectCreator, modCmd.getTitle(), modCmd.getDescription(), modCmd.getShortDescription(), modCmd.getPledgeGoal()), is(true));
    }

    @Test
    public void modifyMasterData_returnsTrueWhenSuccessfullyModifiedByAdmin() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), adminUser);
        modCmd.setDescription("CHANGED_DESCRIPTION");

        assertThat(projectEntity.modifyMasterdata(adminUser, modCmd.getTitle(), modCmd.getDescription(), modCmd.getShortDescription(), modCmd.getPledgeGoal()), is(true));
    }

    @Test
    public void modifyMasterData_returnsFalseWhenNothingChanged() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), aUser);

        assertThat(projectEntity.modifyMasterdata(projectCreator, modCmd.getTitle(), modCmd.getDescription(), modCmd.getShortDescription(), modCmd.getPledgeGoal()), is(false));
    }

    @Test(expected = NotAuthorizedException.class)
    public void modifyMasterData_throwsNotAuthorizedExceptionOnIllegalUser() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        Project modCmd = new Project(projectEntity, Collections.emptyList(), aUser);

        assertThat(projectEntity.modifyMasterdata(aUser, modCmd.getTitle(), modCmd.getDescription(), modCmd.getShortDescription(), modCmd.getPledgeGoal()), is(false));
    }

    @Test
    public void modifyMasterData_throwsInvalidReqExWhenChangeNotAllowed() throws Exception {
        projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        Project modCmd = new Project(projectEntity, Collections.emptyList(), aUser);
        modCmd.setDescription("CHANGED_DESCR");

        try {
            projectEntity.modifyMasterdata(projectCreator, modCmd.getTitle(), modCmd.getDescription(), modCmd.getShortDescription(), modCmd.getPledgeGoal());
            fail("Exception expected to be thrown");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
        assertThat(projectEntity.getDescription(), not(equalTo(modCmd.getDescription())));

    }

    @Test
    public void onFinancingRoundTerminated_ProjectPublishedHavingStatusDeferred() throws Exception {
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
    public void onFinancingRoundTerminated_ProjectPublishedHavingStatusPublishedDeferred() throws Exception{
        projectEntity.setStatus(ProjectStatus.PUBLISHED_DEFERRED);

        projectEntity.onFinancingRoundTerminated(projectEntity.getFinancingRound());

        assertThat(projectEntity.getStatus(), is(ProjectStatus.PUBLISHED));
        assertThat(projectEntity.getFinancingRound(), is(nullValue()));
    }

    @Test
    public void pledgedAmountPostRound() throws Exception {
        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(bunchOfWithPostRoundPledgesDone()), is(BigDecimal.valueOf(1 + 2 + 3 + 4 + 5)));
    }

    @Test
    public void pledgedAmountPostRound_returnsZeroOnNoRoundNoTerminatedRoundAndNullOrEmptyPledges() throws Exception {
        List<PledgeEntity> pledges = bunchOfPledgesDone();

        projectEntity.setFinancingRound(null);
        assertThat(projectEntity.pledgedAmountPostRound(pledges), is(BigDecimal.ZERO));

        projectEntity.setFinancingRound(anActiveFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(pledges), is(BigDecimal.ZERO));

        projectEntity.setFinancingRound(aTerminatedFinancingRound());
        assertThat(projectEntity.pledgedAmountPostRound(null), is(BigDecimal.ZERO));

        assertThat(projectEntity.pledgedAmountPostRound(Collections.emptyList()), is(BigDecimal.ZERO));
    }

    @Test
    public void addAttachmentAllowed_DoNothingIfAllowed() {
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        projectEntity.addAttachmentAllowed(projectCreator);
    }

    @Test(expected = NotAuthorizedException.class)
    public void addAttachmentAllowed_ThrowsNotAuthorizeddExceptionWhenUserForbidden() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(anActiveFinancingRound());

        projectEntity.addAttachmentAllowed(aUser);
    }

    @Test
    public void addAttachmentAllowed_ThrowInvalidRequestExceptionWhenMasterdataChangesForbidden() {
        try {
            projectEntity.setStatus(ProjectStatus.PUBLISHED);
            projectEntity.setFinancingRound(anActiveFinancingRound());
            projectEntity.addAttachmentAllowed(adminUser);
            fail("Exception expected to be thrown");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
    }

    @Ignore // FIXME: 18/11/16
    @Test
    public void findAttachmentByReference_ShouldReturnExistingAttachment() {
//        final String existingFileRef = "an_Existing_File_Ref";
//        final AttachmentValue expAttachmentValue = anAttachmentValue(existingFileRef);
//
//        projectEntity.setAttachments(Arrays.asList(
//                anAttachmentValue("non_Matching_File_Ref"),
//                expAttachmentValue,
//                anAttachmentValue("another_non_Matching_File_Ref")));
//
//        assertThat(projectEntity.findAttachmentByReference(Attachment.asLookupByIdCommand(existingFileRef)), is(expAttachmentValue));

    }

    @Ignore // FIXME: 18/11/16
    @Test(expected = IllegalArgumentException.class)
    public void findAttachmentByReference_ShouldThrowIllegalArgExceptionOnIncompleteRequestObject() {
//        projectEntity.findAttachmentByReference(Attachment.asLookupByIdCommand(null));
    }

    @Ignore // FIXME: 18/11/16
    @Test(expected = ResourceNotFoundException.class)
    public void findAttachmentByReference_ShouldThrowResourceNotFoundExceptionOnNonExistingAttachment() {
//        projectEntity.setAttachments(Arrays.asList(
//                anAttachmentValue("non_Matching_File_Ref"),
//                anAttachmentValue("another_non_Matching_File_Ref")));
//
//        projectEntity.findAttachmentByReference(Attachment.asLookupByIdCommand("realyNotMatching!"));
    }

    @Test
    public void deleteAttachmentAllowed_DoNothingIfAllowed() {
        projectEntity.setStatus(ProjectStatus.PROPOSED);
        projectEntity.deleteAttachmentAllowed(projectCreator);
    }

    @Test(expected = NotAuthorizedException.class)
    public void deleteAttachmentAllowed_ThrowsNotAuthorizeddExceptionWhenUserForbidden() {
        projectEntity.setStatus(ProjectStatus.PUBLISHED);
        projectEntity.setFinancingRound(anActiveFinancingRound());

        projectEntity.deleteAttachmentAllowed(aUser);
    }

    @Test
    public void deleteAttachmentAllowed_ThrowInvalidRequestExceptionWhenMasterdataChangesForbidden() {
        try {
            projectEntity.setStatus(ProjectStatus.PUBLISHED);
            projectEntity.setFinancingRound(anActiveFinancingRound());
            projectEntity.deleteAttachmentAllowed(adminUser);
            fail("Exception expected to be thrown");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage(), is(InvalidRequestException.masterdataChangeNotAllowed().getMessage()));
        }
    }

    @Ignore // FIXME: 18/11/16
    @Test
    public void deleteAttachment_ShouldDeleteByFileReferenceOnly() throws Exception {
//        final String fileRef2Del = "file_ref_1";
//        projectEntity.setStatus(ProjectStatus.PROPOSED);
//        projectEntity.addAttachment(anAttachmentValue("file_ref_0"));
//        projectEntity.addAttachment(anAttachmentValue(fileRef2Del));
//
//        projectEntity.deleteAttachment(new AttachmentValue(fileRef2Del, null, "", 17L, DateTime.now()));
//
//        assertThat(projectEntity.getAttachments().size(), is(1));
//        assertThat(projectEntity.getAttachments().get(0).getFileReference(), is(not(fileRef2Del)));
    }

    private FinancingRoundEntity aTerminatedFinancingRound() {
        FinancingRoundEntity res = aFinancingRound(new DateTime().minusDays(1));
        assertThat(res.active(), is(false));
        res.setId(100L);
        res.setCreatedDate(new DateTime().minusDays(3));

        res.initPostRoundBudget(bunchOfPledgesDone().stream().map(PledgeEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        res.setTerminationPostProcessingDone(true);
        return res;
    }

    private FinancingRoundEntity anActiveFinancingRound() {
        FinancingRoundEntity res = aFinancingRound(new DateTime().plusDays(1));
        assertThat(res.active(), is(true));
        res.setId(123L);
        return res;
    }

    private List<PledgeEntity> pledgesAlreadyDone(BigDecimal pledgeAmount) {
        if (pledgeAmount.compareTo(projectEntity.getPledgeGoal()) == 0) {
            projectEntity.setStatus(ProjectStatus.FULLY_PLEDGED);
        }
        return Collections.singletonList(new PledgeEntity(projectEntity, aUser, pledgeAmount, projectEntity.getFinancingRound()));
    }

    private void assertPledgeNotExecuted(RuntimeException actualEx, RuntimeException expEx, UserEntity user, BigDecimal userBudgetBeforePledge, ProjectStatus expStatus) {
        assertThat(actualEx.getMessage(), is(expEx.getMessage()));
        assertThat(user.getBudget(), is(userBudgetBeforePledge));
        assertThat(projectEntity.getStatus(), is(expStatus));
    }

    private FinancingRoundEntity aFinancingRound(DateTime endDate) {
        FinancingRoundEntity res = FinancingRoundEntity.newFinancingRound(7, endDate, BigDecimal.valueOf(1000));
        res.setStartDate(new DateTime().minusDays(2));
        return res;
    }

    private List<PledgeEntity> bunchOfPledgesDone() {
        List<PledgeEntity> res = new ArrayList<>();
        res.add(new PledgeEntity(projectEntity, aUser, BigDecimal.valueOf(10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, adminUser, BigDecimal.valueOf(60), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(180), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, aUser, BigDecimal.valueOf(20), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, adminUser, BigDecimal.valueOf(70),projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, aUser, BigDecimal.valueOf(10),projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, aUser, BigDecimal.valueOf(-10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(-180), projectEntity.getFinancingRound()));

        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(-10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(110),projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(10), projectEntity.getFinancingRound()));
        res.add(new PledgeEntity(projectEntity, projectCreator, BigDecimal.valueOf(-110), projectEntity.getFinancingRound()));
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
            BigDecimal amount = BigDecimal.valueOf(i);
            pledge = new PledgeEntity(projectEntity, adminUser, amount, projectEntity.getFinancingRound());
            pledge.setCreatedDate(projectEntity.getFinancingRound().getEndDate().plusHours(2 * i));
            postRoundPledgedAmount = amount.add(postRoundPledgedAmount);
            res.add(pledge);
        }
        return res;
    }

    private ProjectEntity prepareMasterData(ProjectEntity projectEntity) {
        projectEntity.setTitle("test_Title");
        projectEntity.setDescription("test_Description");
        projectEntity.setShortDescription("test_shortDescription");
        projectEntity.setPledgeGoal(BigDecimal.valueOf(17));
        return projectEntity;
    }

//    private AttachmentValue anAttachmentValue(String fileReference) {
//        return new AttachmentValue(fileReference, MediaType.TEXT_PLAIN_VALUE, "fileName_" + fileReference, 617, DateTime.now()
//        );
//    }
}