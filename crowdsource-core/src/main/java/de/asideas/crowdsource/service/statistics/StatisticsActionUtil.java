package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

public final class StatisticsActionUtil {

    public static String formatDate(DateTime dateTime) {
        return dateTime.toString("yyyy-MM-dd");
    }

    public static Map<String, Long> getDefaultMap(TimeRangedStatisticsRequest request) {
        Validate.notNull(request);

        final int duration = Days.daysBetween(request.getStartDate().toInstant(), request.getEndDate().toInstant()).getDays();
        final Map<String, Long> resultMap = new LinkedHashMap<>();
        for (int i = 0; i < duration; i++) {
            resultMap.put(formatDate(request.getStartDate().plusDays(i)), 0L);
        }

        return resultMap;
    }

    public static Map<String, Long> fillMap(Map<String, Long> defaultMap, Map<String, Long> mapToFill) {
        notNull(defaultMap, "defaultMap should not be null");
        notNull(mapToFill, "mapToFill should not be null");
        isTrue(defaultMap.size() >= mapToFill.size(), "default map should not have less element than mapToFill, so that the result should contain all the elements of both maps");
        final Map<String, Long> resultMap = new LinkedHashMap<>(mapToFill);

        // merging default map and map from db to fill up days, which no data exists
        defaultMap.forEach((k, v) -> resultMap.merge(k, v, Long::max));

        return resultMap;
    }
}
