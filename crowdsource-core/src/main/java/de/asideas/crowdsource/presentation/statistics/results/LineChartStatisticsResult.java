package de.asideas.crowdsource.presentation.statistics.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LineChartStatisticsResult {

    private final String name;
    private final List<LineChartEntry> data;

    public LineChartStatisticsResult(String name, Map<String, Long> rawdata) {
        this.name = name;
        data = new ArrayList<>();
        for (Map.Entry<String, Long> entry : rawdata.entrySet()) {
            data.add(new LineChartEntry(entry.getValue(), entry.getKey()));
        }
    }

    public String getName() {
        return name;
    }

    public List<LineChartEntry> getData() {
        return data;
    }

    public class LineChartEntry {
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
