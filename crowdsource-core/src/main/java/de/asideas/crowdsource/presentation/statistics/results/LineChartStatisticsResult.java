package de.asideas.crowdsource.presentation.statistics.results;

import java.util.Map;

public class LineChartStatisticsResult {

    private final String name;
    private final Map<String, Long> data;

    public LineChartStatisticsResult(String name, Map<String, Long> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public Map<String, Long> getData() {
        return data;
    }
}
