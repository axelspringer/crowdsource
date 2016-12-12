package de.asideas.crowdsource.presentation.organisation;

import de.asideas.crowdsource.domain.model.OrganisationUnitEntity;
import de.asideas.crowdsource.presentation.validator.ExistingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditOrganisationCommand {

    @ExistingEntity(entityType = OrganisationUnitEntity.class)
    private Long id;
    @NotBlank
    private String name;
}
