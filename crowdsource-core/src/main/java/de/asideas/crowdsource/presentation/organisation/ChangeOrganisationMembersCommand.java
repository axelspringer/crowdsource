package de.asideas.crowdsource.presentation.organisation;

import de.asideas.crowdsource.domain.model.OrganisationUnitEntity;
import de.asideas.crowdsource.presentation.validator.ExistingEntities;
import de.asideas.crowdsource.presentation.validator.ExistingEntity;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
public class ChangeOrganisationMembersCommand {

    @ExistingEntity(entityType = OrganisationUnitEntity.class)
    private Long organisationId;

    @ExistingEntities(entityType = OrganisationUnitEntity.class)
    private List<Long> userIds;
}
