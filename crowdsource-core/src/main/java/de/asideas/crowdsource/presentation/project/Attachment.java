package de.asideas.crowdsource.presentation.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.InputStream;

@Data
public class Attachment {

    private String id;
    private String name;
    private Long size;
    private String type;
    private DateTime created;
    @JsonIgnore
    private transient InputStream payload;
}
