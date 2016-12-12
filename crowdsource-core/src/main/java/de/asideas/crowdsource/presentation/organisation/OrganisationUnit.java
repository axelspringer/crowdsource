package de.asideas.crowdsource.presentation.organisation;

import de.asideas.crowdsource.domain.model.OrganisationUnitEntity;
import de.asideas.crowdsource.presentation.user.User;
import lombok.Value;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.Validate.notNull;

@Value
public class OrganisationUnit {

    private final Long id;
    private final String name;
    private final List<User> users;

    public static OrganisationUnit onlyIdentity(OrganisationUnitEntity entity) {
        notNull(entity, "OrganisationUnit cannot be null");
        return new OrganisationUnit(entity.getId(), null, Collections.emptyList());
    }

    public static OrganisationUnit listView(OrganisationUnitEntity entity) {
        notNull(entity, "OrganisationUnit cannot be null");
        return new OrganisationUnit(entity.getId(), entity.getName(), Collections.emptyList());
    }

    public static OrganisationUnit detailView(OrganisationUnitEntity entity) {
        notNull(entity, "OrganisationUnit cannot be null");
        return new OrganisationUnit(entity.getId(), entity.getName(), entity.getMembers().stream().map(User::new).collect(toList()));
    }
}
