package de.asideas.crowdsource.presentation;

import com.fasterxml.jackson.annotation.JsonView;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.PledgeEntity;
import de.asideas.crowdsource.presentation.project.PublicFinancingRoundInformationView;
import de.asideas.crowdsource.util.validation.financinground.FinancingRoundNotColliding;
import lombok.Data;
import org.joda.time.DateTime;

import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
@FinancingRoundNotColliding
public class FinancingRound {

    @JsonView(PublicFinancingRoundInformationView.class)
    private Long id;

    @JsonView(PublicFinancingRoundInformationView.class)
    private DateTime startDate = new DateTime();

    @NotNull
    @Future(message = "end-date-in-future")
    @JsonView(PublicFinancingRoundInformationView.class)
    private DateTime endDate;

    @NotNull
    @Min(value = 1l, message = "at-least-one-dollar")
    private BigDecimal budget;

    @JsonView(PublicFinancingRoundInformationView.class)
    private BigDecimal postRoundBudget;

    @JsonView(PublicFinancingRoundInformationView.class)
    private BigDecimal postRoundBudgetRemaining;

    @JsonView(PublicFinancingRoundInformationView.class)
    private boolean active;

    @JsonView(PublicFinancingRoundInformationView.class)
    private boolean postRoundBudgetDistributable;


    public FinancingRound(FinancingRoundEntity financingRoundEntity, List<PledgeEntity> postRoundPledges) {
        this.id = financingRoundEntity.getId();
        this.startDate = financingRoundEntity.getStartDate();
        this.endDate = financingRoundEntity.getEndDate();
        this.budget = financingRoundEntity.getBudget();
        this.postRoundBudget = financingRoundEntity.getPostRoundBudget();
        this.active = financingRoundEntity.active();
        this.postRoundBudgetRemaining = financingRoundEntity.postRoundPledgableBudgetRemaining(postRoundPledges);
        this.postRoundBudgetDistributable = financingRoundEntity.terminated() && financingRoundEntity.getTerminationPostProcessingDone();
    }

    public FinancingRound() {
    }
}
