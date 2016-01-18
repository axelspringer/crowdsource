package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import org.joda.time.DateTime;

public interface CommentRepositoryCustom {
    LineChartStatisticsResult sumCommentsGroupByCreatedDate(DateTime startDate, DateTime endDate);
}
