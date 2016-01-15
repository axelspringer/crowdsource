package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.shared.ProjectStatus;

import java.util.Map;

public interface ProjectRepositoryCustom {

    Map<ProjectStatus, Long> sumProjectsGroupedByStatus();
}
