package de.asideas.crowdsource.presentation.statistics.results;

import java.util.List;

public class LineChartStatisticsResult {

    private final String name;
    private final List<Long> data;

    public LineChartStatisticsResult(String name, List<Long> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public List<Long> getData() {
        return data;
    }
}
