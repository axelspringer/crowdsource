package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.domain.shared.ProjectStatus;
import lombok.Data;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import static java.math.BigDecimal.ZERO;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class FinancingRoundEntity {

    private static final Logger log = LoggerFactory.getLogger(FinancingRoundEntity.class);

    @Id
    @GeneratedValue
    private Long id;
    private DateTime startDate;
    private DateTime endDate;
    private Integer userCount;
    private BigDecimal budget;
    /**
     * The amount of money left after round has been terminated; thus it is the amount of money users did not spend on
     * pledging projects during official activity of that round. It is set during post processing of financing rounds
     * and is intended to be eventually used for pledging projects by admins. When admin users pledge after termination
     * of the round the pledging amounts are not subtracted from this member.
     */
    private BigDecimal postRoundBudget;
    private BigDecimal budgetPerUser;
    private Boolean terminationPostProcessingDone = false;
    @ManyToOne
    private OrganisationUnitEntity organisationUnit;
    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;

    /**
     * Factory for a new financing round that immediately will be in active state
     *
     * @return
     */
    public static FinancingRoundEntity newFinancingRound(int userCount, DateTime endDate, BigDecimal budget) {
        FinancingRoundEntity res = new FinancingRoundEntity();
        res.setStartDate(new DateTime());
        res.setEndDate(endDate);
        res.setBudget(budget);
        res.setUserCount(userCount);
        res.setBudgetPerUser(res.calculateBudgetPerUser());
        return res;
    }

    public boolean idenitityEquals(FinancingRoundEntity other) {
        if (other == null) {
            return false;
        }
        return this.getId().equals(other.getId());
    }

    public boolean projectEligibleForRound(ProjectEntity project) {
        return ProjectStatus.FULLY_PLEDGED != project.getStatus();
    }

    public void stopFinancingRound() {
        this.terminationPostProcessingDone = false;
        this.setEndDate(new DateTime());
    }

    /**
     * @return Whether <code>this</code> is currently active
     */
    public boolean active() {
        return startDate.isBeforeNow() && endDate.isAfterNow();
    }

    /**
     * @return whether <code>this</code> is terminated.
     */
    public boolean terminated() {
        return endDate.isBeforeNow();
    }

    public boolean terminationPostProcessingRequiredNow() {
        return terminated() && !this.terminationPostProcessingDone;
    }

    /**
     * @param postRoundPledges all pledges that have been made to projects assigned to <code>this</code>
     *                         after the round finished
     * @return how much money is left to be invested using money from this round based on <code>postRoundBudget</code>
     */
    public BigDecimal postRoundPledgableBudgetRemaining(List<PledgeEntity> postRoundPledges) {
        if (!terminationPostProcessingDone) {
            return ZERO;
        }
        if (postRoundPledges == null || postRoundPledges.isEmpty()) {
            return this.postRoundBudget;
        }
        Collections.sort(postRoundPledges, (o1, o2) -> {
            if( o1 == o2) {
                return 0;
            }
            if(o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.getCreatedDate().compareTo(o2.getCreatedDate());
        } );
        Assert.isTrue(postRoundPledges.get(0).getCreatedDate().getMillis() > this.endDate.getMillis(),
                "Method must not be called with pledgeEntities from active financing round! One entry was: " + postRoundPledges.get(0));

        final BigDecimal postRoundPledgeAmount = postRoundPledges.stream().map(PledgeEntity::getAmount).reduce(ZERO, BigDecimal::add);
        return postRoundBudget.subtract( postRoundPledgeAmount );
    }

    /**
     * Calculates and initializes the remaining budget that was not pledged by users during <code>this</code> active round
     *
     * @param pledgeAmountByUsers the total amount pledged by users during active financing round
     */
    public void initPostRoundBudget(BigDecimal pledgeAmountByUsers) {
        if (!this.terminated()) {
            throw new IllegalStateException("Cannot initialize remaining budget on not yet terminated financing round: " + this);
        }
        if (this.postRoundBudget != null) {
            throw new IllegalStateException("PostRoundBudget must not be initialized more than once!");
        }

        BigDecimal budgetRemainingAfterRound = getBudget().subtract(pledgeAmountByUsers);

        if (budgetRemainingAfterRound.compareTo(ZERO) < 0) {
            log.warn("It seems, within this financing round there were more pledges done than budget available; Setting remaining budget to 0; " +
                    "The pledge amount above budget is: {}; FinancingRound was: {}", budgetRemainingAfterRound, this);
            budgetRemainingAfterRound = ZERO;
        }
        setPostRoundBudget(budgetRemainingAfterRound);
    }

    private BigDecimal calculateBudgetPerUser() {
        return this.userCount < 1 ? ZERO : budget.divide(BigDecimal.valueOf(userCount), 0, RoundingMode.FLOOR);
    }
}
