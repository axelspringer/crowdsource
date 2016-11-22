package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.security.Roles;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String email;
    private String firstname;
    private String lastname;
    private String password;
    private String activationToken;
    @ElementCollection
    private List<String> roles = asList(Roles.ROLE_USER);
    private boolean activated;
    private boolean deleted;
    private BigDecimal budget = BigDecimal.ZERO;
    @ManyToMany(mappedBy = "members")
    private List<OrganisationUnitEntity> organisationUnits;
    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;

    public UserEntity() {
    }

    public UserEntity(String email, String firstname, String lastname) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public String getName() {
        return firstname + " " + lastname;
    }

    public void accountPledge(BigDecimal amount) {
        if (budget.compareTo(amount) < 0) {
            throw new IllegalArgumentException("UserEntity budget may not drop below 0");
        }

        budget = budget.subtract(amount);
    }
}
