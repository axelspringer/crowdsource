package de.asideas.crowdsource.presentation;

import de.asideas.crowdsource.domain.model.CommentEntity;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

@Data
public class Comment {

    private DateTime created;

    private String userName;

    @NotEmpty
    private String comment;

    public Comment(CommentEntity commentEntity) {
        this.created = commentEntity.getCreatedDate();
        this.userName = commentEntity.getCreator().getName();
        this.comment = commentEntity.getComment();
    }

    @java.beans.ConstructorProperties({"created", "userName", "comment"})
    public Comment(DateTime created, String userName, String comment) {
        this.created = created;
        this.userName = userName;
        this.comment = comment;
    }

    public Comment() {
    }
}
