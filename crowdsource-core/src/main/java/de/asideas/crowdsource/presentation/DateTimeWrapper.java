package de.asideas.crowdsource.presentation;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class DateTimeWrapper {

    private DateTime dateTime;

    public DateTimeWrapper(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public DateTimeWrapper() {
    }
}
