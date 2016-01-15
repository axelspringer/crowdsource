package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProjectPerStatusSumAction {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectPerStatusSumAction(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Map<ProjectStatus, Long> getProjectsPerStatus() {
        return projectRepository.sumProjectsGroupedByStatus();
    }
}
