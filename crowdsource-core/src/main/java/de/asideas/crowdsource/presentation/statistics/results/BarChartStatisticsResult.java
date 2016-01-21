package de.asideas.crowdsource.presentation.statistics.results;

public class BarChartStatisticsResult {

    private final String id;
    private final String name;
    private final Long count;


    public BarChartStatisticsResult(String id, String name, Long count) {
        this.id = id;
        this.name = name;
        this.count = count;
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
