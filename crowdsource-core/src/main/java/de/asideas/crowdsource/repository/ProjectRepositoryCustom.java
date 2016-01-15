package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;

import java.util.List;

public interface ProjectRepositoryCustom {

    List<BarChartStatisticsResult> sumProjectsGroupedByStatus();
}
