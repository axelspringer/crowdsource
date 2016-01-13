package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static de.asideas.crowdsource.service.statistics.StatisticsActionUtil.*;
import static de.asideas.crowdsource.service.statistics.StatisticsActionUtil.getDefaultMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StatisticsActionUtilTest {

    @Test
    public void getDefaultMap_should_return_one_by_same_day() {
        assertThat(getDefaultMap(new TimeRangedStatisticsRequest(DateTime.now(), DateTime.now())).size(), is(1));
    }

    @Test
    public void getDefaultMap_should_return_map_with_entry_for_each_day_bounds_inclusive() {
        DateTime startDate = DateTime.now();
        DateTime endDate = startDate.plusDays(10);

        Map<String, Long> result = getDefaultMap(new TimeRangedStatisticsRequest(startDate, endDate));

        assertThat(result.size(), is(11));

        // Bounds (start / enddate) should be included.
        assertThat(result, hasKey(startDate.toString("yyyy-MM-dd")));
        assertThat(result, hasKey(endDate.toString("yyyy-MM-dd")));

        for(Map.Entry entry : result.entrySet()) {
            assertThat(entry.getValue(), is(0L));
        }
    }

    @Test
    public void getDefaultMap_should_return_data_with_proper_order() throws Exception {
        DateTime startDate = DateTime.now();
        DateTime endDate = startDate.plusDays(10);

        Map<String, Long> result = getDefaultMap(new TimeRangedStatisticsRequest(startDate, endDate));

        assertThat(result.size(), is(11));

        int i = 0;
        for (String s : result.keySet()) {
            assertThat(s, is(formatDate(startDate.plusDays(i))));
            i++;
        }
    }

    @Test(expected = NullPointerException.class)
    public void getDefaultMap_should_throw_nullpointerexception_on_null_parameter() {
        getDefaultMap(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fillMap_should_throw_exception_if_defaultMap_is_smaller_than_mapToFill() {
        final Map<String, Long> defaultMap = new HashMap<>();
        defaultMap.put("1", 0L);
        final Map<String, Long> mapToFill = new HashMap<>();
        mapToFill.put("2", 0L);
        mapToFill.put("3", 0L);
        fillMap(defaultMap, mapToFill);
    }

    @Test(expected = NullPointerException.class)
    public void fillMap_should_throw_exception_if_defaultMap_is_null() {
        final Map<String, Long> mapToFill = new HashMap<>();
        mapToFill.put("2", 0L);
        mapToFill.put("3", 0L);
        fillMap(null, mapToFill);
    }

    @Test(expected = NullPointerException.class)
    public void fillMap_should_throw_exception_if_mapToFill_is_null() {
        final Map<String, Long> defaultMap = new HashMap<>();
        defaultMap.put("1", 0L);
        fillMap(defaultMap, null);
    }

    @Test
    public void fillMap_should_return_default_map_if_mapToFill_is_empty() {
        final Map<String, Long> defaultMap = new HashMap<>();
        defaultMap.put("1", 0L);
        final Map<String, Long> mapToFill = new HashMap<>();

        Map<String, Long> result = fillMap(defaultMap, mapToFill);

        assertThat(result.entrySet(), hasSize(1));
    }

    @Test
    public void fillMap_should_return_merged_map_from_both_maps() {
        final Map<String, Long> defaultMap = new HashMap<>();
        defaultMap.put("1", 0L);
        defaultMap.put("2", 0L);
        defaultMap.put("3", 0L);
        final Map<String, Long> mapToFill = new HashMap<>();
        mapToFill.put("2", 2L);

        Map<String, Long> result = fillMap(defaultMap, mapToFill);

        assertThat(result.entrySet(), hasSize(3));
        assertThat(result.get("2"), is(2L));
        assertThat(result.get("1"), is(0L));
        assertThat(result.get("3"), is(0L));
    }
}