package de.asideas.crowdsource.domain.model;

import de.asideas.crowdsource.domain.shared.LikeStatus;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;

@Data
@Entity
public class LikeEntity {

    @Id
    @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
    @TableGenerator(name = "table", allocationSize = 10)
    private Long id;
    private LikeStatus status = LikeStatus.LIKE;
    @ManyToOne
    private ProjectEntity project;
    @CreatedDate
    private DateTime createdDate;
    @LastModifiedDate
    private DateTime lastModifiedDate;
    @ManyToOne
    private UserEntity creator;

    public LikeEntity() {
    }

    public LikeEntity(LikeStatus status, ProjectEntity project, UserEntity creator) {
        this.status = status;
        this.project = project;
        this.creator = creator;
    }
}
