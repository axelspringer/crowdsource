package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.Comment;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Secured(Roles.ROLE_USER)
@RequestMapping("/project/{projectId}")
public class CommentController {

    @Autowired
    private CommentService commentService;


    @RequestMapping(value = "/comments", method = RequestMethod.GET)
    public List<Comment> comments(@PathVariable Long projectId) {
        return commentService.loadCommentsByProject(projectId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/comment", method = RequestMethod.POST)
    public void storeComment(@PathVariable Long projectId, @Valid @RequestBody Comment comment) {
        commentService.addComment(comment, projectId);
    }

}
