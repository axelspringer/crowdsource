package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
public class OrganisationUnitEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @ManyToMany
    @JoinTable(
            name = "ORG_USER",
            joinColumns=@JoinColumn(name="ORG_ID", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="USER_ID", referencedColumnName="id")
    )
    private List<UserEntity> members;


    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;
}
