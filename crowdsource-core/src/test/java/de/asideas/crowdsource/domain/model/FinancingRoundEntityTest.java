package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.presentation.FinancingRound;
import org.exparity.hamcrest.date.DateMatchers;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FinancingRoundEntityTest {

    @Test
    public void budgetPerUserClearRounding() {
        assertThat(newFinancingRound(BigDecimal.valueOf(100), 10).getBudgetPerUser(), is(10));
    }

    @Test
    public void budgetPerUserNonClearRounding() {
        assertThat(newFinancingRound(BigDecimal.valueOf(109), 10).getBudgetPerUser(), is(10));
    }

    @Test
    public void newFinancingRoundCorrectlyInitialized() {
        final int countUsers = 10;
        final FinancingRound creationCmd = new FinancingRound();
        creationCmd.setBudget(BigDecimal.valueOf(1000));
        creationCmd.setEndDate(new DateTime(0L));
        creationCmd.setId(12L);

        final FinancingRoundEntity res = FinancingRoundEntity.newFinancingRound(countUsers, creationCmd.getEndDate(), creationCmd.getBudget());

        assertThat(res.getId(), is(nullValue()));
        assertThat(res.getBudgetPerUser(), is(100));
        assertThat(res.getBudget(), is(creationCmd.getBudget()));
        assertThat(res.getEndDate(), is(creationCmd.getEndDate()));
        assertThat(res.getStartDate().toDate(), DateMatchers.sameSecond(new Date()));
        assertThat(res.getUserCount(), is(countUsers));
    }

    @Test
    public void active_shouldReturnTrueWhenEndDateInFuture() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(1));
        round.setStartDate(new DateTime().minusDays(1));

        assertThat(round.active(), is(true));
    }

    @Test
    public void active_shouldReturnFalseWhenStartDateAndEndDateInFuture() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(2));
        round.setStartDate(new DateTime().plusDays(1));

        assertThat(round.active(), is(false));
    }

    @Test
    public void active_shouldReturnFalseWhenEndDateInPast() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().minusDays(1));
        round.setStartDate(new DateTime().minusDays(2));

        assertThat(round.active(), is(false));
    }

    @Test
    public void active_shouldReturnFalseWhenStartDateInFuture() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(2));
        round.setStartDate(new DateTime().plusDays(1));

        assertThat(round.active(), is(false));
    }

    @Test
    public void terminated_shouldReturnTrueWhenEndDateInPast() {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().minusDays(1));
        round.setStartDate(new DateTime().minusDays(2));

        assertThat(round.terminated(), is(true));
    }

    @Test
    public void terminated_shouldReturnFalseWhenStillActive() {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(1));
        round.setStartDate(new DateTime().minusDays(1));

        assertThat(round.terminated(), is(false));
    }

    @Test
    public void terminated_shouldReturnFalseWhenNotYetStarted() {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(2));
        round.setStartDate(new DateTime().plusDays(1));

        assertThat(round.terminated(), is(false));
    }

    @Test
    public void terminationPostProcessingRequiredNow_shouldReturnTrueWhenTerminated() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().minusHours(1));

        assertThat(financingRound.terminationPostProcessingRequiredNow(), is(true));
    }

    @Test
    public void terminationPostProcessingRequiredNow_shouldReturnFalseWhenTerminatedButAlreadyProcessed() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().minusHours(1));
        financingRound.setTerminationPostProcessingDone(true);

        assertThat(financingRound.terminationPostProcessingRequiredNow(), is(false));
    }

    @Test
    public void terminationPostProcessingRequiredNow_shouldReturnFalseWhenActiveButAlreadyProcessed() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(1));
        financingRound.setTerminationPostProcessingDone(true);

        assertThat(financingRound.terminationPostProcessingRequiredNow(), is(false));
    }

    @Test
    public void initPostRoundBudget() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().minusHours(1));

        financingRound.initPostRoundBudget(BigDecimal.valueOf(17));
        assertThat(financingRound.getPostRoundBudget(), is(983));
    }

    @Test(expected = IllegalStateException.class)
    public void initPostRoundBudget_shouldThrowExceptionOnNotTerminatedRound() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(2));
        financingRound.setStartDate(new DateTime().plusDays(1));

        financingRound.initPostRoundBudget(BigDecimal.valueOf(17));
    }

    @Test(expected = IllegalStateException.class)
    public void initPostRoundBudget_shouldThrowExceptionWhenAlreadyInitialized() throws Exception {
        final FinancingRoundEntity financingRound = newFinancingRound(BigDecimal.valueOf(100), 10, new DateTime().plusDays(2));
        financingRound.setPostRoundBudget(BigDecimal.ZERO);

        financingRound.initPostRoundBudget(BigDecimal.valueOf(17));
    }

    @Test
    public void postRoundPledgableBudgetRemaining() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 12, new DateTime().minusDays(2));
        round.setPostRoundBudget(BigDecimal.valueOf(400));
        round.setTerminationPostProcessingDone(true);

        assertThat(round.postRoundPledgableBudgetRemaining(preparePostRoundPledges(round)), is(400 - (1 + 2 + 3 + 4 + 5)));
    }

    @Test
    public void postRoundPledgableBudgetRemaining_returnsZeroOnNonTerminatedRound() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 12, new DateTime().minusDays(2));
        round.setTerminationPostProcessingDone(false);
        assertThat(round.postRoundPledgableBudgetRemaining(preparePostRoundPledges(round)), is(0));
    }
    @Test
    public void postRoundPledgableBudgetRemaining_shouldReturnPostRoundBudgetOnNullOrEmptyPledges() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 12, new DateTime().minusDays(2));
        round.setPostRoundBudget(BigDecimal.valueOf(400));
        round.setTerminationPostProcessingDone(true);

        assertThat(round.postRoundPledgableBudgetRemaining(null), is(400));
        assertThat(round.postRoundPledgableBudgetRemaining(Collections.emptyList()), is(400));
    }

    @Test(expected = IllegalArgumentException.class)
    public void postRoundPledgableBudgetRemaining_shouldThrowExIfPledgesFromActiveStateSupplied() throws Exception {
        FinancingRoundEntity round = newFinancingRound(BigDecimal.valueOf(100), 12, new DateTime().minusDays(2));
        round.setStartDate(new DateTime().minusDays(4));
        round.setPostRoundBudget(BigDecimal.valueOf(400));
        round.setTerminationPostProcessingDone(true);
        List<PledgeEntity> pledges = preparePostRoundPledges(round);

        PledgeEntity malicousPledge = new PledgeEntity(new ProjectEntity(), null, BigDecimal.valueOf(1), round);
        malicousPledge.setCreatedDate(round.getStartDate().plusMinutes(10));
        pledges.add(3, malicousPledge);

        round.postRoundPledgableBudgetRemaining(pledges);
    }


    private FinancingRoundEntity newFinancingRound(BigDecimal budget, int countUsers) {
        return newFinancingRound(budget, countUsers, new DateTime().plusDays(1));
    }

    private FinancingRoundEntity newFinancingRound(BigDecimal budget, int countUsers, DateTime endDate) {
        return FinancingRoundEntity.newFinancingRound(countUsers, endDate, budget);
    }

    private List<PledgeEntity> preparePostRoundPledges(FinancingRoundEntity financingRound) {
        List<PledgeEntity> res = new ArrayList<>();
        PledgeEntity pledge;
        ProjectEntity projectEntity;

        for (long i = 1; i < 6; i++) {
            projectEntity = new ProjectEntity();
            projectEntity.setId(i);
            pledge = new PledgeEntity(projectEntity, null, BigDecimal.valueOf(i), financingRound);
            pledge.setCreatedDate(financingRound.getEndDate().plusHours(2 * Long.valueOf(i).intValue()));
            res.add(pledge);
        }
        return res;
    }
}