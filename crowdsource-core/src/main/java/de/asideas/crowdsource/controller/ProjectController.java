package de.asideas.crowdsource.controller;

import com.fasterxml.jackson.annotation.JsonView;
import de.asideas.crowdsource.domain.exception.ForbiddenException;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.presentation.Pledge;
import de.asideas.crowdsource.domain.presentation.project.Project;
import de.asideas.crowdsource.domain.presentation.project.ProjectStatusUpdate;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.ProjectService;
import de.asideas.crowdsource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import static de.asideas.crowdsource.domain.shared.ProjectStatus.FULLY_PLEDGED;
import static de.asideas.crowdsource.domain.shared.ProjectStatus.PUBLISHED;

@RestController
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Secured({Roles.ROLE_TRUSTED_ANONYMOUS, Roles.ROLE_USER})
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    @JsonView(Project.ProjectSummaryView.class)
    public List<Project> getProjects(Authentication auth) {
        UserEntity userEntity = userFromAuthentication(auth);

        final List<Project> projects = projectService.getProjects(userEntity);
        // filter projects. only return projects that are published, fully pledged or created by the requesting user (or if requestor is admin)
        return projects.stream().filter(project -> mayViewProjectFilter(project, auth)).collect(Collectors.toList());
    }

    @Secured({Roles.ROLE_TRUSTED_ANONYMOUS, Roles.ROLE_USER})
    @RequestMapping(value = "/project/{projectId}", method = RequestMethod.GET)
    public Project getProject(@PathVariable String projectId, Authentication auth) {
        UserEntity userEntity = userFromAuthentication(auth);

        final Project project = projectService.getProject(projectId, userEntity);
        if (!mayViewProjectFilter(project, auth)) {
            throw new ForbiddenException();
        }
        return project;
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/project", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project addProject(@RequestBody @Valid Project project, Principal principal) {
        UserEntity userEntity = userByPrincipal(principal);
        return projectService.addProject(project, userEntity);
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/project/{projectId}/pledges", method = RequestMethod.POST)
    public void pledgeProject(@PathVariable String projectId, @RequestBody @Valid Pledge pledge, Principal principal) {
        projectService.pledge(projectId, userByPrincipal(principal), pledge);
    }

    @Secured(Roles.ROLE_ADMIN)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}/status", method = RequestMethod.PATCH)
    public Project modifyProjectStatus(@PathVariable("projectId") String projectId, @RequestBody @Valid @NotNull ProjectStatusUpdate newStatus, Principal principal) {
        return projectService.modifyProjectStatus(projectId, newStatus.status, userByPrincipal(principal));
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}", method = RequestMethod.PUT)
    public Project modifyProjectMasterdata(@PathVariable("projectId") String projectId, @RequestBody @Valid @NotNull Project modifiedProject, Principal principal) {
        return projectService.modifyProjectMasterdata(projectId, modifiedProject, userByPrincipal(principal));
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}/like", method = RequestMethod.POST)
    public void likeProject(@PathVariable("projectId") String projectId, Principal principal) {
        projectService.likeProject(projectId, userByPrincipal(principal));
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/project/{projectId}/unlike", method = RequestMethod.POST)
    public void unlikeProject(@PathVariable("projectId") String projectId, Principal principal) {
        projectService.unlikeProject(projectId, userByPrincipal(principal));
    }

    private boolean mayViewProjectFilter(Project project, Authentication auth) {
        // fully pledged and published are always visible
        final ProjectStatus status = project.getStatus();
        if (status == FULLY_PLEDGED || status == PUBLISHED) {
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

    private UserEntity userByPrincipal(Principal principal) {
        return userService.getUserByEmail(principal.getName());
    }

    private UserEntity userFromAuthentication(Authentication auth) {
        UserEntity userEntity = null;

        if (auth != null && auth.isAuthenticated()) {
            if (auth.getAuthorities().contains(new SimpleGrantedAuthority(Roles.ROLE_TRUSTED_ANONYMOUS))) {
                return null;
            }
            userEntity = userService.getUserByEmail(auth.getName());
        }
        return userEntity;
    }
}
