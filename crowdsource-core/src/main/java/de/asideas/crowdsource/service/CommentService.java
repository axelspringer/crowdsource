package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.Comment;
import de.asideas.crowdsource.repository.CommentRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserNotificationService userNotificationService;
    private final UserRepository userRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, ProjectService projectService, UserRepository userRepository, UserNotificationService userNotificationService) {
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userNotificationService = userNotificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addComment(Comment comment, Long projectId, String commentingUserEmail) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        final UserEntity creator = userRepository.findByEmail(commentingUserEmail);
        CommentEntity commentEntity = new CommentEntity(project, comment.getComment(), creator);
        commentRepository.save(commentEntity);

        userNotificationService.notifyCreatorOnComment(commentEntity);
    }

    @Transactional
    public List<Comment> loadCommentsByProject(Long projectId) {
        final ProjectEntity project = projectService.loadProjectEntity(projectId);
        return commentRepository.findByProject(project).stream().map(Comment::new).collect(Collectors.toList());
    }
}
