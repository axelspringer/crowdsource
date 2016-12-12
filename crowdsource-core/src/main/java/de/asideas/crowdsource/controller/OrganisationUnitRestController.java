package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.organisation.*;
import de.asideas.crowdsource.service.OrganisationUnitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/organisationUnits")
public class OrganisationUnitRestController {

    private final OrganisationUnitService service;

    public OrganisationUnitRestController(OrganisationUnitService service) {
        this.service = service;
    }

    @RequestMapping("/")
    public Page<OrganisationUnit> listOrganisation(Pageable pageable) {
        return service.listOrganisations(pageable);
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public OrganisationUnit createOrganisation(@Valid @RequestBody CreateOrganisationCommand command) {
        return service.createOrganisationUnit(command);
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT)
    public void editOrganisation(@Valid @RequestBody EditOrganisationCommand command) {
        service.editOrganisationUnit(command);
    }

    @RequestMapping("/{id}")
    public OrganisationUnit fetchOrganisation(@PathVariable Long id) {
        return service.fetchOrganisation(new FetchOrganisationQuery(id));
    }

    @RequestMapping(value = "/members/", method = RequestMethod.POST)
    public void changeOrganisationUnitMembers(ChangeOrganisationMembersCommand command) {
        service.changeMembers(command);
    }
}
