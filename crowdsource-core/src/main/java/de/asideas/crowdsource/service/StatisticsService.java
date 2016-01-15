package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.service.statistics.CreatedProjectSumAction;
import de.asideas.crowdsource.service.statistics.ProjectPerStatusSumAction;
import de.asideas.crowdsource.service.statistics.RegisteredUserSumAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final CreatedProjectSumAction createdProjectSumAction;
    private final RegisteredUserSumAction registeredUserSumAction;
    private final ProjectPerStatusSumAction projectPerStatusSumAction;

    @Autowired
    public StatisticsService(CreatedProjectSumAction createdProjectSumAction, RegisteredUserSumAction registeredUserSumAction, ProjectPerStatusSumAction projectPerStatusSumAction) {
        this.createdProjectSumAction = createdProjectSumAction;
        this.registeredUserSumAction = registeredUserSumAction;
        this.projectPerStatusSumAction = projectPerStatusSumAction;
    }

    public List<LineChartStatisticsResult> getCurrentStatistics(TimeRangedStatisticsRequest request) {
        try {
            final Future<LineChartStatisticsResult> createdProjectSumFuture = createdProjectSumAction.getCreatedProjectSumByTimeRange(request);
            final Future<LineChartStatisticsResult> registeredUserSumFuture = registeredUserSumAction.getCountOfRegisteredUsersByTimeRange(request);
            return Arrays.asList(createdProjectSumFuture.get(), registeredUserSumFuture.get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to retrieve statistic data", e);
        }
    }

    public List<BarChartStatisticsResult> getProjectsPerStatus() {
        Map<ProjectStatus, Long> projectsPerStatus = projectPerStatusSumAction.getProjectsPerStatus();

        return projectsPerStatus.entrySet().stream().map(e -> new BarChartStatisticsResult(e.getKey().name(), e.getKey().getDisplayName(), e.getValue())).collect(Collectors.toList());
    }
}
