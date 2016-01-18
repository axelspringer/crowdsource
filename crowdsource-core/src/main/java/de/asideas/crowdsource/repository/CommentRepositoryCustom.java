package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import org.joda.time.DateTime;

import java.util.List;

public interface CommentRepositoryCustom {
    LineChartStatisticsResult sumCommentsGroupByCreatedDate(DateTime startDate, DateTime endDate);
    List<BarChartStatisticsResult> countCommentsGroupByProject(int projectCount);
}
