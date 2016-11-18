package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;

@Data
@Entity
public class AttachmentEntity {

    @Id
    @GeneratedValue
    private Long id;
    @Column
    private String fileReference;
    @Column
    private String filename;
    @Column
    private long size;
    @Column
    private String contentType;
    @ManyToOne
    private ProjectEntity project;
    @CreatedDate
    private DateTime created;
    @CreatedBy
    private UserEntity creator;
}
