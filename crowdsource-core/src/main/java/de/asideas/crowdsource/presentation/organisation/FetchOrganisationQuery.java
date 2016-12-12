package de.asideas.crowdsource.presentation.organisation;

import de.asideas.crowdsource.domain.model.OrganisationUnitEntity;
import de.asideas.crowdsource.presentation.validator.ExistingEntity;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

@Value
@Validated
public class FetchOrganisationQuery {
    @ExistingEntity(entityType = OrganisationUnitEntity.class)
    private final Long id;
}
