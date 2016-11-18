package de.asideas.crowdsource.presentation.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import de.asideas.crowdsource.domain.model.PledgeEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.LikeStatus;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.user.ProjectCreator;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class Project {

    // no validation here on purpose, as this is only filled on response and ignored in request.
    @JsonView(ProjectSummaryView.class)
    private Long id;

    // no validation here on purpose, as this is only filled on response and ignored in request
    @JsonView(ProjectSummaryView.class)
    private ProjectStatus status;

    @NotBlank
    @JsonView(ProjectSummaryView.class)
    private String title;

    @NotBlank
    @JsonView(ProjectSummaryView.class)
    private String shortDescription;

    @NotBlank
    private String description;

    @Min(1L)
    @JsonView(ProjectSummaryView.class)
    private BigDecimal pledgeGoal = BigDecimal.ZERO;

    // no validation here on purpose, as this is only filled on response and ignored in request
    @JsonView(ProjectSummaryView.class)
    private BigDecimal pledgedAmount = BigDecimal.ZERO;

    // no validation here on purpose, as this is only filled on response and ignored in request
    @JsonView(ProjectSummaryView.class)
    private long backers;

    // no validation here on purpose, as this is only filled on response and ignored in request. Ideally,
    // this is filled on request too and denied if a normal user tries to create a project for someone else
    @JsonView(ProjectSummaryView.class)
    private ProjectCreator creator;

    // no validation here on purpose, as this is only filled on response and ignored in request
    @JsonView(ProjectSummaryView.class)
    private Date lastModifiedDate;

    @JsonView(ProjectSummaryView.class)
    private BigDecimal pledgedAmountByRequestingUser = BigDecimal.ZERO;

    @JsonView(ProjectSummaryView.class)
    private BigDecimal pledgedAmountByPostRoundBudget = BigDecimal.ZERO;

    @JsonView(ProjectSummaryView.class)
    private long likeCount;

    @JsonProperty("likeStatus")
    @JsonView(ProjectSummaryView.class)
    private LikeStatus likeStatusOfRequestUser;

//    private List<Attachment> attachments;
// FIXME: 18/11/16 what is requesting user?
    public Project(ProjectEntity projectEntity, List<PledgeEntity> pledges, UserEntity requestingUser) {
        this.id = projectEntity.getId();
        this.status = projectEntity.getStatus();
        this.title = projectEntity.getTitle();
        this.shortDescription = projectEntity.getShortDescription();
        this.description = projectEntity.getDescription();
        this.pledgeGoal = projectEntity.getPledgeGoal();
        this.lastModifiedDate = projectEntity.getLastModifiedDate() != null ? projectEntity.getLastModifiedDate().toDate() : null;

        this.pledgedAmount = projectEntity.pledgedAmount(pledges);
        this.backers = projectEntity.countBackers(pledges);
        this.pledgedAmountByRequestingUser = projectEntity.pledgedAmountOfUser(pledges, requestingUser);
        this.pledgedAmountByPostRoundBudget = projectEntity.pledgedAmountPostRound(pledges);

        this.creator = new ProjectCreator(projectEntity.getCreator());

//        this.attachments = projectEntity.getAttachments().stream().map(a -> Attachment.asResponseWithoutPayload(a, projectEntity)).collect(Collectors.toList());
    }

    public Project(ProjectEntity projectEntity, List<PledgeEntity> pledges, UserEntity requestingUser, long likeCount, LikeStatus likeStatusOfRequestUser) {
        this(projectEntity, pledges, requestingUser);
        this.likeCount = likeCount;
        this.likeStatusOfRequestUser = likeStatusOfRequestUser;
    }

    public Project() {
    }

    /**
     * Used as Marker for {@link Project} Validation on update requests
     */
    public interface UpdateProject {
    }

    /**
     * Used for @JsonView to return a subset of a project only
     */
    public interface ProjectSummaryView {
    }
}
