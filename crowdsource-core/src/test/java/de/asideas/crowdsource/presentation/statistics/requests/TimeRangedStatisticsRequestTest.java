package de.asideas.crowdsource.presentation.statistics.requests;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TimeRangedStatisticsRequestTest {

    @Test(expected = NullPointerException.class)
    public void constructor_should_check_null_on_startDate() {
        new TimeRangedStatisticsRequest(null, new DateTime());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_should_check_null_on_endDate() {
        new TimeRangedStatisticsRequest(new DateTime(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_should_check_start_date_before_end_date() {
        final DateTime startDate = DateTime.now();
        final DateTime endDate = DateTime.now().minusDays(1);

        new TimeRangedStatisticsRequest(startDate, endDate);
    }

    @Test
    public void constructor_should_be_able_to_create_valid_object() {
        final DateTime startDate = DateTime.now();
        final DateTime endDate = DateTime.now().plusDays(1);

        final TimeRangedStatisticsRequest request = new TimeRangedStatisticsRequest(startDate, endDate);

        assertThat(request.getStartDate(), is(startDate.withTimeAtStartOfDay()));
        assertThat(request.getEndDate(), is(endDate.plusDays(1).withTimeAtStartOfDay()));
    }

    @Test
    public void constructor_should_be_able_to_handle_equal_start_and_end_date() {
        final DateTime dateTime = DateTime.now();

        final TimeRangedStatisticsRequest request = new TimeRangedStatisticsRequest(dateTime, dateTime);

        assertThat(request.getStartDate(), is(dateTime.withTimeAtStartOfDay()));
        assertThat(request.getEndDate(), is(dateTime.plusDays(1).withTimeAtStartOfDay()));
    }
}