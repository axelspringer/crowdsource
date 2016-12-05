package de.asideas.crowdsource;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.presentation.project.Project;
import de.asideas.crowdsource.security.Roles;
import org.joda.time.DateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

public final class Mocks {

    public static final String USER_EMAIL = "user@some.host";

    public static UserEntity admin(String email) {
        final UserEntity userEntity = user(email);
        userEntity.setRoles(Collections.singletonList(Roles.ROLE_ADMIN));
        return userEntity;
    }

    public static UserEntity user(String email) {
        UserEntity userEntity = new UserEntity(email, "firstname", "lastname");
        userEntity.setId(new Random(Long.MAX_VALUE).nextLong());
        userEntity.setBudget(BigDecimal.valueOf(4000));
        return userEntity;
    }

    public static UserEntity userEntity(String email, String... roles) {

        UserEntity userEntity = new UserEntity(email, "firstname", "lastname");
        userEntity.setId((long) email.hashCode());
        userEntity.setRoles(Arrays.asList(roles));
        userEntity.setBudget(BigDecimal.valueOf(4000));
        return userEntity;
    }

    public static Project project(String title, String description, String shortDescription, BigDecimal pledgeGoal, ProjectStatus projectStatus) {
        final Project project = new Project();
        project.setTitle(title);
        project.setDescription(description);
        project.setShortDescription(shortDescription);
        project.setPledgeGoal(pledgeGoal);
        project.setStatus(projectStatus);

        return project;
    }

    public static ProjectEntity projectEntity() {
        final ProjectEntity res = new ProjectEntity();
        res.setId(150L);
        return res;
    }

    public static Principal authentication(UserEntity userEntity) {
        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        userEntity.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        return new UsernamePasswordAuthenticationToken(userEntity.getEmail(), "somepassword", authorities);
    }

    public static Principal anonymousAuthentication() {
        return new AnonymousAuthenticationToken("ANONYMOUS", "ANONYMOUS",
                Collections.singletonList(new SimpleGrantedAuthority(Roles.ROLE_TRUSTED_ANONYMOUS)));
    }

    public static Attachment attachment(Optional<String> payload) {
        if (payload.isPresent()) {
            return Attachment.asResponse(1L, "name", 12L, "text/plain", DateTime.now(), new ByteArrayInputStream(payload.get().getBytes()));
        } else {
            return Attachment.asResponseWithoutPayload(1L, "name", 12L, "text/plain", DateTime.now());
        }
    }
}
