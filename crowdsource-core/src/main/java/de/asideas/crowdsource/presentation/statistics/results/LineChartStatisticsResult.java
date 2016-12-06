package de.asideas.crowdsource.presentation.statistics.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LineChartStatisticsResult {

    private final String name;
    private final List<LineChartEntry> data;


    public LineChartStatisticsResult(String name, List<LineChartEntry> data) {
        this.name = name;
        this.data = data;
    }

    public LineChartStatisticsResult(String name, Map<String, Long> rawdata) {
        this.name = name;
        this.data = new ArrayList<>();
        this.data.addAll(rawdata.entrySet().stream().map(entry -> new LineChartEntry(entry.getValue(), entry.getKey())).collect(Collectors.toList()));
    }

    public String getName() {
        return name;
    }

    public List<LineChartEntry> getData() {
        return data;
    }

    public static class LineChartEntry {
        private final Long data;
        private final String label;

        private LineChartEntry(Long data, String label) {
            this.data = data;
            this.label = label;
        }

        public Long getData() {
            return data;
        }

        public String getLabel() {
            return label;
        }
    }
}
