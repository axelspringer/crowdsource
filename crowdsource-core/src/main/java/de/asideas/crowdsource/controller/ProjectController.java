package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.annotation.JsonView;
import de.asideas.crowdsource.domain.exception.ForbiddenException;
import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.Pledge;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.presentation.project.ProjectStatusUpdate;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import static de.asideas.crowdsource.domain.shared.ProjectStatus.*;

@RestController
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Value("#{T(org.springframework.http.MediaType).parseMediaTypes('${de.asideas.crowdsource.attachment.allowedmediatypes}')}")
    private List<MediaType> attachmentTypesAllowed;

    @Secured({Roles.ROLE_USER})
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    @JsonView(Project.ProjectSummaryView.class)
    public List<Project> getProjects(Authentication auth) {

        final List<Project> projects = projectService.getProjects(auth.getName());
        // filter projects. only return projects that are published, fully pledged or created by the requesting user (or if requestor is admin)
        return projects.stream().filter(project -> mayViewProjectFilter(project, auth)).collect(Collectors.toList());
    }

    @Secured({Roles.ROLE_USER})
    @RequestMapping(value = "/project/{projectId}", method = RequestMethod.GET)
    public Project getProject(@PathVariable Long projectId, Authentication auth) {

        final Project project = projectService.getProject(projectId, auth.getName());
        if (!mayViewProjectFilter(project, auth)) {
            throw new ForbiddenException();
        }
        return project;
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/project", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project addProject(@RequestBody @Valid Project project, Principal principal) {
        return projectService.addProject(project, principal.getName());
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/project/{projectId}/pledges", method = RequestMethod.POST)
    public void pledgeProject(@PathVariable Long projectId, @RequestBody @Valid Pledge pledge, Principal principal) {
        projectService.pledge(projectId, principal.getName(), pledge);
    }

    @Secured(Roles.ROLE_ADMIN)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}/status", method = RequestMethod.PATCH)
    public Project modifyProjectStatus(@PathVariable("projectId") Long projectId, @RequestBody @Valid @NotNull ProjectStatusUpdate newStatus, Principal principal) {
        return projectService.modifyProjectStatus(projectId, newStatus.status, principal.getName());
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}", method = RequestMethod.PUT)
    public Project modifyProjectMasterdata(@PathVariable("projectId") Long projectId, @RequestBody @Valid @NotNull Project modifiedProject, Principal principal) {
        return projectService.modifyProjectMasterdata(projectId, modifiedProject, principal.getName());
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/projects/{projectId}/likes", method = RequestMethod.POST)
    public void likeProject(@PathVariable("projectId") Long projectId, Principal principal) {
        projectService.likeProject(projectId, principal.getName());
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/projects/{projectId}/likes", method = RequestMethod.DELETE)
    public void unlikeProject(@PathVariable("projectId") Long projectId, Principal principal) {
        projectService.unlikeProject(projectId, principal.getName());
    }

    @Deprecated
    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/projects/{projectId}/attachments", method = RequestMethod.POST)
    public Attachment addProjectAttachment(@PathVariable("projectId") Long projectId, @RequestParam("file") MultipartFile file, Principal principal) {

        if (file.isEmpty()) {
            throw InvalidRequestException.fileMustNotBeEmpty();
        }
        if (!contentTypeAllowed(file.getContentType())) {
            throw InvalidRequestException.filetypeNotAllowed();
        }

        try ( InputStream inputStream = file.getInputStream() ) {
            Attachment attachment = Attachment.asCreationCommand(file.getOriginalFilename(), file.getContentType(), inputStream);

            return projectService.addProjectAttachment(projectId, attachment, principal.getName());

        } catch (IOException e) {
            log.warn("Couldn' process file input, due to stream threw IOException; ProjectId: {}", projectId, e);
            throw new RuntimeException("Internal error, couldn't process file stream", e);
        }
    }

    @Deprecated
    @Secured({Roles.ROLE_USER})
    @RequestMapping(value = "/projects/{projectId}/attachments/{fileReference}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> serveProjectAttachment(@PathVariable("projectId") String projectId, @PathVariable("fileReference") Long fileReference) throws IOException {

        final Attachment attachment = projectService.loadProjectAttachment(Attachment.asLookupByIdCommand(fileReference));

        return ResponseEntity.ok()
                .contentLength(attachment.getSize())
                .contentType(MediaType.valueOf(attachment.getType()))
                .body(new InputStreamResource(attachment.getPayload()));
    }

    @Deprecated
    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/projects/{projectId}/attachments/{fileReference}", method = RequestMethod.DELETE)
    public void deleteProjectAttachment(@PathVariable("projectId") String projectId, @PathVariable("fileReference") Long fileReference, Principal principal) {
        projectService.deleteProjectAttachment(Attachment.asLookupByIdCommand(fileReference));
    }

    private boolean contentTypeAllowed(String contentType) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            log.warn("Couldn't parse media type {}", contentType, e);
            return false;
        }
        for (MediaType el : attachmentTypesAllowed) {
            if (el.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    private boolean mayViewProjectFilter(Project project, Authentication auth) {
        // fully pledged and published are always visible
        final ProjectStatus status = project.getStatus();
        if (status == FULLY_PLEDGED || status == PUBLISHED || status == PUBLISHED_DEFERRED) {
            return true;
        }

        if (auth == null) {
            return false;
        }

        // admins may do everything
        for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
            if (Roles.ROLE_ADMIN.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }

        // the creator always may see his project
        return project.getCreator().getEmail().equals(auth.getName());
    }
}
