package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class CommentCountPerProjectAction {

    private final ProjectRepository projectRepository;

    @Autowired
    public CommentCountPerProjectAction(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<BarChartStatisticsResult> getCommentCountPerProjectStatistic(int projectCount) {
        return projectRepository.test(new PageRequest(0, projectCount))
                .stream()
                .map(p -> new BarChartStatisticsResult(p.getStatus(), Integer.valueOf(p.getComments().size()).longValue()))
                .collect(toList());
    }
}
