package de.asideas.crowdsource.presentation.statistics.requests;

import org.joda.time.DateTime;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

public class TimeRangedStatisticsRequest {

    private final DateTime startDate;
    private final DateTime endDate;

    public TimeRangedStatisticsRequest(DateTime startDate, DateTime endDate) {
        notNull(startDate, "start date is not allowed to be null");
        notNull(endDate, "end date is not allowed to be null");
        isTrue(!startDate.isAfter(endDate), "start date must before end date");

        // to begin of the same day
        this.startDate = startDate.withTimeAtStartOfDay();
        // to begin of the next day
        this.endDate = endDate.plusDays(1).withTimeAtStartOfDay();
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }
}
