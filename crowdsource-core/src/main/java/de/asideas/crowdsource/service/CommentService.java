package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.Comment;
import de.asideas.crowdsource.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private CommentRepository commentRepository;
    private ProjectService projectService;
    private UserNotificationService userNotificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository, ProjectService projectService, UserNotificationService userNotificationService) {
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userNotificationService = userNotificationService;
    }

    public void addComment(Comment comment, Long projectId) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        CommentEntity commentEntity = new CommentEntity(project, comment.getComment());
        commentRepository.save(commentEntity);

        userNotificationService.notifyCreatorOnComment(commentEntity);
    }

    public List<Comment> loadCommentsByProject(Long projectId) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        return commentRepository.findByProject(project).stream().map(Comment::new).collect(Collectors.toList());
    }
}
