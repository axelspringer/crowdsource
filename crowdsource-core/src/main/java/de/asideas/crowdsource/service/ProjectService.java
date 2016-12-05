package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.*;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.domain.shared.LikeStatus;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.repository.*;
import de.asideas.crowdsource.security.Roles;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static de.asideas.crowdsource.domain.shared.LikeStatus.LIKE;
import static de.asideas.crowdsource.domain.shared.LikeStatus.UNLIKE;
import static java.util.stream.Collectors.toList;

@Service
public class ProjectService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    private final AttachmentEntityRepository attachmentEntityRepository;
    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final UserRepository userRepository;
    private final FinancingRoundRepository financingRoundRepository;
    private final UserNotificationService userNotificationService;
    private final FinancingRoundService financingRoundService;
    private final LikeRepository likeRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                          PledgeRepository pledgeRepository,
                          UserRepository userRepository,
                          FinancingRoundRepository financingRoundRepository,
                          UserNotificationService userNotificationService,
                          FinancingRoundService financingRoundService,
                          LikeRepository likeRepository,
                          AttachmentEntityRepository attachmentEntityRepository) {

        this.projectRepository = projectRepository;
        this.pledgeRepository = pledgeRepository;
        this.userRepository = userRepository;
        this.financingRoundRepository = financingRoundRepository;
        this.userNotificationService = userNotificationService;
        this.financingRoundService = financingRoundService;
        this.attachmentEntityRepository = attachmentEntityRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public Project getProject(Long projectId, String email) {
        final UserEntity requestingUser = userRepository.findByEmail(email);
        return project(loadProjectEntity(projectId), requestingUser);
    }

    @Transactional
    public List<Project> getProjects(String email) {
        final UserEntity requestingUser = userRepository.findByEmail(email);

        final List<ProjectEntity> projects = projectRepository.findAll();
        return projects.stream().map(p -> project(p, requestingUser)).collect(toList());
    }

    @Transactional
    public Project addProject(Project project, String userEmail) {
        Assert.notNull(project);

        final UserEntity creator = userRepository.findByEmail(userEmail);

        Assert.notNull(creator);

        ProjectEntity projectEntity = new ProjectEntity(project.getTitle(), project.getShortDescription(), project.getDescription(), project.getPledgeGoal(), currentFinancingRound(), creator);
        projectEntity = projectRepository.save(projectEntity);

        notifyAdminsOnNewProject(new Project(projectEntity));

        LOG.debug("Project added: {}", projectEntity);
        return project(projectEntity, creator);
    }

    @Transactional
    public Project modifyProjectStatus(Long projectId, ProjectStatus newStatusToApply, String email) {

        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        final UserEntity requestingUser = userRepository.findByEmail(email);

        if (projectEntity.modifyStatus(newStatusToApply)) {
            projectRepository.save(projectEntity);
            userNotificationService.notifyCreatorOnProjectStatusUpdate(projectEntity);
        }

        return project(projectEntity, requestingUser);
    }

    @Transactional
    public Project modifyProjectMasterdata(Long projectId, Project modifiedProject, String email) {
        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        final UserEntity requestingUser = userRepository.findByEmail(email);


        if (projectEntity.modifyMasterdata(requestingUser, modifiedProject.getTitle(), modifiedProject.getDescription(), modifiedProject.getShortDescription(), modifiedProject.getPledgeGoal())) {
            projectRepository.save(projectEntity);
            userNotificationService.notifyCreatorAndAdminOnProjectModification(projectEntity, requestingUser);
            LOG.debug("Project updated: {}", projectEntity);
        }

        return project(projectEntity, requestingUser);
    }

    @Transactional
    public void  pledge(Long projectId, String email, Pledge pledge) {

        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        final UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) {
            throw new NotAuthorizedException("user with email: " + email + " cannot be found");
        }


        FinancingRoundEntity financingRound = financingRoundService.mostRecentRoundEntity();
        if (financingRound != null &&
                financingRound.terminated() &&
                financingRound.getTerminationPostProcessingDone() &&
                userEntity.getRoles().contains(Roles.ROLE_ADMIN)) {

            if (!financingRound.idenitityEquals(projectEntity.getFinancingRound())) {
                throw InvalidRequestException.projectTookNotPartInLastFinancingRond();
            }
            pledgeProjectUsingPostRoundBudget(projectEntity, userEntity, pledge);
        } else {
            pledgeProjectInFinancingRound(projectEntity, userEntity, pledge);
        }
    }

    @Deprecated
    @Transactional
    public Attachment addProjectAttachment(Long projectId, Attachment attachment, String email) throws IOException {

        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        final UserEntity creator = userRepository.findByEmail(email);

        final AttachmentEntity attachmentEntity = new AttachmentEntity(attachment.getName(), IOUtils.toByteArray(attachment.getPayload()), attachment.getType(), projectEntity, creator);
        attachmentEntityRepository.save(attachmentEntity);

        return Attachment.withoutPayload(attachmentEntity);
    }

    @Deprecated
    @Transactional
    public Attachment loadProjectAttachment(Attachment attachment) {

        final AttachmentEntity attachmentEntity = attachmentEntityRepository.findOne(attachment.getId());
        return Attachment.withPayload(attachmentEntity);
    }

    @Deprecated
    @Transactional
    public void deleteProjectAttachment(Attachment attachment) {
        attachmentEntityRepository.delete(attachment.getId());
    }

    @Transactional
    public void likeProject(Long projectId, String email) {
        final UserEntity user = userRepository.findByEmail(email);

        final ProjectEntity project = projectRepository.findOne(projectId);
        toggleStatus(project, user, LIKE);
    }

    @Transactional
    public void unlikeProject(Long projectId, String email) {
        final UserEntity user = userRepository.findByEmail(email);

        final ProjectEntity project = projectRepository.findOne(projectId);
        toggleStatus(project, user, UNLIKE);
    }

    @Transactional
    public ProjectEntity loadProjectEntity(Long projectId) {
        ProjectEntity projectEntity = projectRepository.findOne(projectId);
        if (projectEntity == null) {
            throw new ResourceNotFoundException();
        }
        return projectEntity;
    }

    void pledgeProjectInFinancingRound(ProjectEntity projectEntity, UserEntity userEntity, Pledge pledge) {
        List<PledgeEntity> pledgesSoFar = pledgeRepository.findByProjectAndFinancingRound(
                projectEntity, projectEntity.getFinancingRound());

        // potential problem: race condition. Two simultaneous requests could lead to "over-pledging"
        PledgeEntity pledgeEntity = projectEntity.pledge(
                pledge.getAmount(), userEntity, pledgesSoFar);

        // potential problem: no transaction -> no rollback -- Possible Solution -> sort of mini event sourcing?
        if (projectEntity.pledgeGoalAchieved()) {
            projectRepository.save(projectEntity);
        }
        userRepository.save(userEntity);
        pledgeRepository.save(pledgeEntity);

        LOG.debug("Project pledged: {}", pledgeEntity);
    }

    void pledgeProjectUsingPostRoundBudget(ProjectEntity projectEntity, UserEntity userEntity, Pledge pledge) {
        FinancingRoundEntity financingRound = projectEntity.getFinancingRound();

        List<PledgeEntity> postRoundPledges = pledgeRepository.findByFinancingRoundAndCreatedDateGreaterThan(
                financingRound, financingRound.getEndDate());

        BigDecimal postRoundPledgableBudget = financingRound.postRoundPledgableBudgetRemaining(postRoundPledges);

        List<PledgeEntity> pledgesSoFar = pledgeRepository.findByProjectAndFinancingRound(
                projectEntity, projectEntity.getFinancingRound());

        PledgeEntity pledgeResult = projectEntity.pledgeUsingPostRoundBudget(
                pledge.getAmount(), userEntity, pledgesSoFar, postRoundPledgableBudget);

        if (projectEntity.pledgeGoalAchieved()) {
            projectRepository.save(projectEntity);
        }
        userRepository.save(userEntity);
        pledgeRepository.save(pledgeResult);

        LOG.debug("Project pledged using post round budget: {}", pledgeResult);
    }

    private Project project(ProjectEntity projectEntity, UserEntity requestingUser) {
        List<PledgeEntity> pledges = pledgeRepository.findByProjectAndFinancingRound(projectEntity, projectEntity.getFinancingRound());
        final Optional<LikeEntity> likeEntity = likeRepository.findOneByProjectAndCreator(projectEntity, requestingUser);
        final long likeCount = likeRepository.countByProjectAndStatus(projectEntity, LIKE);
        return new Project(projectEntity, pledges, requestingUser, likeCount, likeEntity.map(LikeEntity::getStatus).orElse(UNLIKE));
    }

    private FinancingRoundEntity currentFinancingRound() {
        return financingRoundRepository.findActive(DateTime.now());
    }

    private void notifyAdminsOnNewProject(final Project project) {
        userRepository.findAllAdminUsers().stream()
                .map(UserEntity::getEmail)
                .forEach(emailAddress -> userNotificationService.notifyAdminOnProjectCreation(project, emailAddress));
    }

    private void toggleStatus(ProjectEntity project, UserEntity user, LikeStatus status) {
        final Optional<LikeEntity> likeEntityOptional = likeRepository.findOneByProjectAndCreator(project, user);
        if (likeEntityOptional.isPresent()) {
            final LikeEntity likeEntity = likeEntityOptional.get();
            likeEntity.setStatus(status);
            likeRepository.save(likeEntity);
        } else {
            final LikeEntity likeEntity = new LikeEntity(status, project, user);
            likeRepository.save(likeEntity);
        }
    }
}
