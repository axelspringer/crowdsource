package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
public class AttachmentEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String fileReference;
    private String filename;
    private long size;
    private String contentType;
    @ManyToOne
    private ProjectEntity project;
    @CreatedDate
    private DateTime created;
    @ManyToOne
    private UserEntity creator;
}
