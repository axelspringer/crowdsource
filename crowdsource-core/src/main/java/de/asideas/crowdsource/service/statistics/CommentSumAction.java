package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentSumAction {

    static final String CHART_NAME_SUM_COMMENTS = "Summe Kommentare";

    private final CommentRepository commentRepository;

    @Autowired
    public CommentSumAction(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public LineChartStatisticsResult getSumComments(TimeRangedStatisticsRequest request) {
        final Map<String, Long> aggregateResult = commentRepository.findByCreatedDateBetween(request.getStartDate(), request.getEndDate())
                .stream()
                .map(c -> c.getCreatedDate().toString("yyyy-MM-dd"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));


        return new LineChartStatisticsResult(CHART_NAME_SUM_COMMENTS, StatisticsActionUtil.fillMap(StatisticsActionUtil.getDefaultMap(request), aggregateResult));
    }

}
