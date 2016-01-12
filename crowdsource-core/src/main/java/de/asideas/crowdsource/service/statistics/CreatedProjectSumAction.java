package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.repository.ProjectRepository;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static de.asideas.crowdsource.domain.shared.StatisticsTypes.SUM_CREATED_PROJECT;

@Async
@Service
public class CreatedProjectSumAction {

    private final ProjectRepository projectRepository;

    @Autowired
    public CreatedProjectSumAction(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Future<LineChartStatisticsResult> getCreatedProjectSumByTimeRange(TimeRangedStatisticsRequest request) {

        final List<ProjectEntity> projectEntityList = projectRepository.findByCreatedDateBetween(request.getStartDate(), request.getEndDate());

        final Map<Instant, Long> map = projectEntityList.stream().collect(Collectors.groupingBy(
                p -> p.getCreatedDate().withTimeAtStartOfDay().toInstant(),
                Collectors.reducing(
                        0L,
                        t -> 1L,
                        Long::sum
                )
        ));

        final LineChartStatisticsResult result = new LineChartStatisticsResult(SUM_CREATED_PROJECT.getDisplayName(), new ArrayList<>(map.values()));


        return new AsyncResult<>(result);
    }
}
