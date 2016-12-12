package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.model.OrganisationUnitEntity;
import de.asideas.crowdsource.presentation.organisation.*;
import de.asideas.crowdsource.repository.OrganisationUnitRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.Valid;

import static de.asideas.crowdsource.presentation.organisation.OrganisationUnit.detailView;
import static de.asideas.crowdsource.presentation.organisation.OrganisationUnit.onlyIdentity;

@Service
public class OrganisationUnitService {

    private final OrganisationUnitRepository organisationUnitRepository;
    private final UserRepository userRepository;

    public OrganisationUnitService(OrganisationUnitRepository organisationUnitRepository, UserRepository userRepository) {
        this.organisationUnitRepository = organisationUnitRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrganisationUnit createOrganisationUnit(CreateOrganisationCommand cmd) {
        return onlyIdentity(organisationUnitRepository.save(new OrganisationUnitEntity(cmd.getName())));
    }

    @Transactional
    public void editOrganisationUnit(EditOrganisationCommand cmd) {
        final OrganisationUnitEntity entity = organisationUnitRepository.findOne(cmd.getId());
        entity.setName(cmd.getName());

        organisationUnitRepository.save(entity);
    }

    @Transactional
    public void changeMembers(ChangeOrganisationMembersCommand cmd) {
        final OrganisationUnitEntity entity = organisationUnitRepository.findOne(cmd.getOrganisationId());
        entity.setMembers(userRepository.findAll(cmd.getUserIds()));

        organisationUnitRepository.save(entity);
    }

    @Transactional
    public Page<OrganisationUnit> listOrganisations(Pageable pageable) {
        return organisationUnitRepository.findAll(pageable).map(OrganisationUnit::listView);
    }

    @Transactional
    public OrganisationUnit fetchOrganisation(@Valid FetchOrganisationQuery query) {
        return detailView(organisationUnitRepository.findOne(query.getId()));
    }

}
