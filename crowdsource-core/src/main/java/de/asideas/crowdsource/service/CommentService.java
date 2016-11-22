package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.Comment;
import de.asideas.crowdsource.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final UserNotificationService userNotificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository, ProjectService projectService, UserService userService, UserNotificationService userNotificationService) {
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.userNotificationService = userNotificationService;
    }

    public void addComment(Comment comment, Long projectId, String commentingUserEmail) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        final UserEntity creator = userService.getUserByEmail(commentingUserEmail);
        CommentEntity commentEntity = new CommentEntity(project, comment.getComment(), creator);
        commentRepository.save(commentEntity);

        userNotificationService.notifyCreatorOnComment(commentEntity);
    }

    public List<Comment> loadCommentsByProject(Long projectId) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        return commentRepository.findByProject(project).stream().map(Comment::new).collect(Collectors.toList());
    }
}
