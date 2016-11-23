package de.asideas.crowdsource.presentation.user;

import de.asideas.crowdsource.domain.model.UserEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserMetrics {

    private int count;

    private BigDecimal remainingBudget;

    public UserMetrics(List<UserEntity> users) {
        count = users.size();

        remainingBudget = users.stream()
                .map(user -> user.getBudget() == null ? BigDecimal.ZERO : user.getBudget())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
