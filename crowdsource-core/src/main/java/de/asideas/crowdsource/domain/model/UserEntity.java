package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.security.Roles;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;

@Data
@Entity
public class UserEntity {

    @Id
    @GeneratedValue
    private Long id;
    @Column
    private String email;
    @Column
    private String firstname;
    @Column
    private String lastname;
    @Column
    private String password;
    @Column
    private String activationToken;
    @Column
    private List<String> roles = asList(Roles.ROLE_USER);
    @Column
    private boolean activated;
    @Column
    private boolean deleted;
    @Column
    private BigDecimal budget;
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
