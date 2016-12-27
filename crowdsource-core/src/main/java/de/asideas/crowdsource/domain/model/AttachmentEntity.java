package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AttachmentEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String fileReference;
    private String filename;
    private long size;
    @Lob
    private byte[] content;
    private String contentType;
    @ManyToOne
    private ProjectEntity project;
    @CreatedDate
    @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime created;
    @ManyToOne
    private UserEntity creator;

    public AttachmentEntity() {
    }

    public AttachmentEntity(String filename, byte[] content, String contentType, ProjectEntity project, UserEntity creator) {
        this.filename = filename;
        this.content = content;
        this.contentType = contentType;
        this.project = project;
        this.creator = creator;
    }
}
