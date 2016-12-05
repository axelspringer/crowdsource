package de.asideas.crowdsource.presentation.statistics.results;

import de.asideas.crowdsource.domain.shared.ProjectStatus;

public class BarChartStatisticsResult {

    private final String id;
    private final String name;
    private final Long count;


    private BarChartStatisticsResult(String id, String name, Long count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public BarChartStatisticsResult(ProjectStatus status, Long count) {
        this(status.name(), status.getDisplayName(), count);
    }

    public BarChartStatisticsResult(Object[] objects) {
        this((ProjectStatus) objects[0], (Long)objects[1]);
    }

    public String getName() {
        return name;
    }

    public Long getCount() {
        return count;
    }

    public String getId() {
        return id;
    }
}
