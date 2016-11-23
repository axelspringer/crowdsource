package de.asideas.crowdsource.domain.model;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;

@Data
@Entity
public class CommentEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String comment;
    @ManyToOne
    private ProjectEntity project;
    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;
    @ManyToOne
    private UserEntity creator;

    public CommentEntity() {
    }

    public CommentEntity(ProjectEntity project, String comment, UserEntity creator) {
        this.project = project;
        this.comment = comment;
        this.creator = creator;
    }
}
