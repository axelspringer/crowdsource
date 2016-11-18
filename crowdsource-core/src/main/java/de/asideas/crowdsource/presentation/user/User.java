package de.asideas.crowdsource.presentation.user;

import de.asideas.crowdsource.domain.model.UserEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class User {

    private String email;
    private List<String> roles;
    private BigDecimal budget;
    private String name;

    public User(final UserEntity userEntity) {
        this.email = userEntity.getEmail();
        this.budget = userEntity.getBudget();
        this.roles = userEntity.getRoles();
        this.name = userEntity.getName();
    }
}
