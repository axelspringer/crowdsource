package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.security.Roles;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

@Data
@Entity
public class ProjectEntity {

    @Id
    @GeneratedValue
    private Long id;
    @Column
    private String title;
    @Column
    private String shortDescription;
    @Column
    private String description;
    @Column
    private ProjectStatus status;
    @Column
    private BigDecimal pledgeGoal;

    @ManyToOne
    private FinancingRoundEntity financingRound;

    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;
    @CreatedBy
    private UserEntity creator;

    public ProjectEntity() {
    }

    public ProjectEntity(String title, String shortDescription, String description, BigDecimal pledgeGoal, FinancingRoundEntity financingRound) {
        this.financingRound = financingRound;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.pledgeGoal = pledgeGoal;
        this.status = ProjectStatus.PROPOSED;
    }

    /**
     * Allows pledging <code>this</code> project, using budget from the <code>pledgingUser</code> given.
     * Moreover negative pledges are supported by reducing investment for the project in which case the amount
     * is credited the <code>pledginUser</code>'s budget (only as much posssible as originally pledged by her).
     *
     * @param amount             The amount to (positively or negatively) pledge <code>this</code>
     * @param pledgingUser       the user that wants to invest and whose balance is affected
     * @param pledgesAlreadyDone all investements that have been done so far for <code>this</code>
     * @return the value describing the [reduced] investment done
     */
    public PledgeEntity pledge(BigDecimal amount, UserEntity pledgingUser, List<PledgeEntity> pledgesAlreadyDone) {

        if (this.financingRound == null || !this.financingRound.active()) {
            throw InvalidRequestException.noFinancingRoundCurrentlyActive();
        }
        if (this.status == ProjectStatus.FULLY_PLEDGED) {
            throw InvalidRequestException.projectAlreadyFullyPledged();
        }
        if (this.status != ProjectStatus.PUBLISHED) {
            throw InvalidRequestException.projectNotPublished();
        }
        if (amount.compareTo(ZERO) == 0) {
            throw InvalidRequestException.zeroPledgeNotValid();
        }
        if (amount.compareTo(pledgingUser.getBudget()) > 0) {
            throw InvalidRequestException.userBudgetExceeded();
        }
        if (pledgedAmountOfUser(pledgesAlreadyDone, pledgingUser).add(amount).compareTo(ZERO) < 0) {
            throw InvalidRequestException.reversePledgeExceeded();
        }

        final BigDecimal newPledgedAmount = pledgedAmount(pledgesAlreadyDone).add(amount);
        if (newPledgedAmount.compareTo(this.pledgeGoal) > 0) {
            throw InvalidRequestException.pledgeGoalExceeded();
        }

        if (newPledgedAmount.compareTo(this.pledgeGoal) == 0) {
            setStatus(ProjectStatus.FULLY_PLEDGED);
        }

        pledgingUser.accountPledge(amount);

        return new PledgeEntity(this, pledgingUser, amount, financingRound);
    }

    /**
     * Allows admin users to pledge <code>this</code> project after the last financing round using its remaining budget.
     * Thus, the user's budget is not debited or credited rather than <code>this</code>' financingRound's remaining budget.
     * Negative pledges are supported as well but only as much as was pledged by admin users on the terminated financing round.
     *
     * @param amount                            the amount to [negatively] pledge
     * @param pledgingUser                      the admin user executing the pledge.
     * @param pledgesAlreadyDone                all investements that have been done so far for <code>this</code>
     * @param postRoundPledgableBudgetAvailable how much budget is available from financing round to be used for investments
     * @return the value describing the [reduced] investment done using budget from <code>this</code>' financing round
     */
    public PledgeEntity pledgeUsingPostRoundBudget(BigDecimal amount, UserEntity pledgingUser, List<PledgeEntity> pledgesAlreadyDone, BigDecimal postRoundPledgableBudgetAvailable) {
        Assert.isTrue(pledgingUser.getRoles().contains(Roles.ROLE_ADMIN), "pledgeUsingPostRoundBudget(..) called with non admin user: " + pledgingUser);
        Assert.notNull(this.financingRound, "FinancingRound must not be null; ProjectEntity was: " + this);
        Assert.isTrue(this.financingRound.terminated(), "pledgeUsingPostRoundBudget(..) requires its financingRound to be terminated; ProjectEntity was: " + this);

        if (!this.financingRound.getTerminationPostProcessingDone()) {
            throw InvalidRequestException.financingRoundNotPostProcessedYet();
        }

        if (this.status == ProjectStatus.FULLY_PLEDGED) {
            throw InvalidRequestException.projectAlreadyFullyPledged();
        }
        if (this.status != ProjectStatus.PUBLISHED) {
            throw InvalidRequestException.projectNotPublished();
        }
        if (amount.compareTo(ZERO) == 0) {
            throw InvalidRequestException.zeroPledgeNotValid();
        }
        if (amount.compareTo(postRoundPledgableBudgetAvailable) > 0) {
            throw InvalidRequestException.postRoundBudgetExceeded();
        }

        if (pledgedAmountPostRound(pledgesAlreadyDone).add(amount).compareTo(ZERO) < 0) {
            throw InvalidRequestException.reversePledgeExceeded();
        }

        final BigDecimal newPledgedAmount = pledgedAmount(pledgesAlreadyDone).add(amount);
        if (newPledgedAmount.compareTo(this.pledgeGoal) > 0) {
            throw InvalidRequestException.pledgeGoalExceeded();
        }

        if (newPledgedAmount == this.pledgeGoal) {
            setStatus(ProjectStatus.FULLY_PLEDGED);
        }

        return new PledgeEntity(this, pledgingUser, amount, financingRound);
    }

    /**
     * Modifies status of <code>this</code>.
     *
     * @param newStatus
     * @return whether an actual change of the status has taken place
     * @throws InvalidRequestException in case constraints are violated
     */
    public boolean modifyStatus(ProjectStatus newStatus) throws InvalidRequestException {
        if (this.status == newStatus) {
            return false;
        }

        if (ProjectStatus.FULLY_PLEDGED == this.status) {
            throw InvalidRequestException.projectAlreadyFullyPledged();
        }

        if (ProjectStatus.DEFERRED == newStatus) {
            if (this.financingRound != null && this.financingRound.active()) {
                throw InvalidRequestException.projectAlreadyInFinancingRound();
            }
            if (ProjectStatus.REJECTED == this.status) {
                throw InvalidRequestException.setToDeferredNotPossibleOnRejected();
            }
        }

        if (ProjectStatus.PUBLISHED_DEFERRED == newStatus) {
            if (this.financingRound != null && this.financingRound.active() && this.status == ProjectStatus.PUBLISHED) {
                throw InvalidRequestException.projectAlreadyInFinancingRound();
            }
        }

        setStatus(newStatus);
        return true;
    }

    public boolean modifyMasterdata(UserEntity requestingUser, String title, String description, String shortDescription, BigDecimal pledgeGoal) {
        modificationsAllowedByUserAndState(requestingUser);
        if (!masterdataChanged(title, description, shortDescription, pledgeGoal)) {
            return false;
        }
        setTitle(title);
        setDescription(description);
        setShortDescription(shortDescription);
        setPledgeGoal(pledgeGoal);
        return true;
    }

    boolean masterdataModificationAllowed() {
        switch (this.status) {
            case FULLY_PLEDGED:
                return false;
            case PROPOSED:
            case DEFERRED:
                return true;
            default:
                break;
        }
        if (this.financingRound == null) {
            return true;
        }
        if (this.financingRound.active()) {
            return false;
        }
        return true;
    }

    public void addAttachmentAllowed(UserEntity attachmentCreator) throws NotAuthorizedException, InvalidRequestException {
        modificationsAllowedByUserAndState(attachmentCreator);
    }

    public void deleteAttachmentAllowed(UserEntity attachmentCreator) throws NotAuthorizedException, InvalidRequestException {
        modificationsAllowedByUserAndState(attachmentCreator);
    }

//    public void addAttachment(AttachmentEntity attachment) {
//        attachmentEntityList.add(attachment);
//    }

    /**
     * Retrieve the attachment file reference entry that allows association of actual binary data.
     * Currently fetching is only supported by file reference (<code>AttachmentEntity.id</code>).
     *
     * @return the attachment value if it exists
     * @throws ResourceNotFoundException in case the attachment couldn't be found
     */
//    public AttachmentEntity findAttachmentByReference(AttachmentEntity attachment) throws ResourceNotFoundException {
//        Assert.notNull(attachment.getId());
//
//        final Optional<AttachmentEntity> res = this.attachments.stream().filter(a -> a.getFileReference().equals(attachment.getId())).findFirst();
//
//        if (!res.isPresent()) {
//            throw new ResourceNotFoundException();
//        }
//        return res.get();
//    }

    boolean masterdataChanged(String title, String description, String shortDescription, BigDecimal pledgeGoal) {
        boolean changed = false;
        changed |= !Objects.equals(title, this.title);
        changed |= !Objects.equals(description, this.description);
        changed |= !Objects.equals(shortDescription, this.shortDescription);
        changed |= pledgeGoal.compareTo(this.pledgeGoal) != 0;
        return changed;
    }

    /**
     * Upon termination of its financing round the status is adapted accordingly as well as allocation of financing round.
     *
     * @return whether something actually changed.
     */
    // // FIXME: 18/11/16 a project can only join ONE finance round
    public boolean onFinancingRoundTerminated(FinancingRoundEntity financingRound) {
        if (this.financingRound == null || !this.financingRound.getId().equals(financingRound.getId())) {
            return false;
        }

        if (this.status == ProjectStatus.DEFERRED || this.status == ProjectStatus.PUBLISHED_DEFERRED) {
            modifyStatus(ProjectStatus.PUBLISHED);
            setFinancingRound(null);
            return true;
        }
        return false;
    }

    public boolean pledgeGoalAchieved() {
        return this.status == ProjectStatus.FULLY_PLEDGED;
    }

    public BigDecimal pledgedAmount(List<PledgeEntity> pledges) {
        return pledges.stream().map(PledgeEntity::getAmount).reduce(ZERO, BigDecimal::add);
    }

    public long countBackers(List<PledgeEntity> pledges) {
        return pledges.stream()
                .collect(groupingBy(PledgeEntity::getCreator, reducing(new PledgeEntity(), PledgeEntity::add)))
                .entrySet().stream()
                .mapToInt(pledgeSumByUser -> (pledgeSumByUser.getValue().getAmount().compareTo(ZERO) == 0 ? 0 : 1)).sum();
    }

    public BigDecimal pledgedAmountOfUser(List<PledgeEntity> pledges, UserEntity requestingUser) {
        if (requestingUser == null || pledges == null || pledges.isEmpty()) {
            return ZERO;
        }
        return pledges.stream().filter(p -> requestingUser.getId().equals(p.getCreator().getId()))
                .map(PledgeEntity::getAmount).reduce(ZERO, BigDecimal::add);
    }

    public BigDecimal pledgedAmountPostRound(List<PledgeEntity> pledges) {
        if (pledges == null || pledges.isEmpty() || this.financingRound == null || !this.financingRound.terminated()) {
            return ZERO;
        }
        return pledges.stream()
                .filter(p -> p.getCreatedDate() != null && p.getCreatedDate().isAfter(financingRound.getEndDate()))
                .map(PledgeEntity::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private void modificationsAllowedByUserAndState(UserEntity requestingUser) {
        if (!requestingUser.getRoles().contains(Roles.ROLE_ADMIN) && !requestingUser.equals(this.creator)) {
            throw new NotAuthorizedException("You are neither admin nor creator of that project");
        }
        if (!masterdataModificationAllowed()) {
            throw InvalidRequestException.masterdataChangeNotAllowed();
        }
    }
}
