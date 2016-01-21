package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;

import java.util.List;

public interface CommentRepositoryCustom {
    LineChartStatisticsResult sumCommentsGroupByCreatedDate(TimeRangedStatisticsRequest request);
    List<BarChartStatisticsResult> countCommentsGroupByProject(int projectCount);
}
