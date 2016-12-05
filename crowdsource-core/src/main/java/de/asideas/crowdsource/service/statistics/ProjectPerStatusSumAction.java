package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectPerStatusSumAction {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectPerStatusSumAction(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<BarChartStatisticsResult> getProjectsPerStatus() {
        final List<Object[]> projectGroupByStatus = projectRepository.findProjectGroupByStatus();

        return projectGroupByStatus.stream().map(BarChartStatisticsResult::new).collect(Collectors.toList());
    }
}
