package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
public class PledgeEntity {

    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    private ProjectEntity project;
    @ManyToOne
    private FinancingRoundEntity financingRound;
    @Column
    private BigDecimal amount;

    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;
    @CreatedBy
    private UserEntity creator;

    public PledgeEntity() {
    }
    // // FIXME: 18/11/16 user should be automatically added
    public PledgeEntity(ProjectEntity projectEntity, UserEntity userEntity, BigDecimal amount, FinancingRoundEntity financingRoundEntity) {
        this.project = projectEntity;
        this.creator = userEntity;
        this.financingRound = financingRoundEntity;
        this.amount = amount;
    }
    /** Copy Constructor */
    private PledgeEntity(BigDecimal amount, DateTime createdDate, FinancingRoundEntity financingRound, Long id, DateTime lastModifiedDate, ProjectEntity project, UserEntity user) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.financingRound = financingRound;
        this.id = id;
        this.lastModifiedDate = lastModifiedDate;
        this.project = project;
        this.creator = user;
    }

    /**
     * Adds the <code>other</code> pledge's amount to <code>this</code> returning a new object, leaving this as is.
     * In case the <code>users</code> differ it will be set to <code>null</code>.
     * In case <code>this</code>' user is null but the <code>other</code>'s user is set the user will be set to the <code>other</code>'s
     * The <code>id</code> will always be set to null.
     * In case <code>financingRound</code>s differ, it will be set to <code>null</code>, set otherwise
     * In case <code>this</code>' financingRound is null but the <code>other</code>'s financingRound is set the financingRound will be set to the <code>other</code>'s
     * The <code>createdDate</code> and <code>lastModifiedDate</code> will be set to <code>null</code>
     * The <code>project</code> will be <code>null</code> if different, set otherwise
     * In case <code>this</code>' project is null but the <code>other</code>'s project is set the financingRound will be set to the <code>other</code>'s
     *
     * @param other the other to add
     * @return the summed pledge; if <code>other</code> is <code>null</code> will return a copy of <code>this</code>
     */
    public PledgeEntity add(PledgeEntity other){
        if(other == null) {
            return new PledgeEntity(amount, null, financingRound, null, null, project, creator);
        }

        PledgeEntity res = new PledgeEntity();
        res.setAmount(this.amount.add(other.amount));
        if(this.creator == null || this.creator.equals(other.creator)) {
            res.creator = other.creator;
        }

        if(this.financingRound == null || this.financingRound.equals(other.financingRound)) {
            res.financingRound = other.financingRound;
        }
        if(this.project == null || this.project.equals(other.project)) {
            res.project = other.project;
        }
        return res;
    }
}
