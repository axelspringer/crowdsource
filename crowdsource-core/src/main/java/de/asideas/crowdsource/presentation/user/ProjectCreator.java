package de.asideas.crowdsource.presentation.user;

import com.fasterxml.jackson.annotation.JsonView;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.project.Project;
import lombok.Data;

@Data
public class ProjectCreator {

    private Long id;

    @JsonView(Project.ProjectSummaryView.class)
    private String name;

    @JsonView(Project.ProjectSummaryView.class)
    private String email;

    public ProjectCreator(UserEntity user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
    }

    public ProjectCreator() {
    }
}
