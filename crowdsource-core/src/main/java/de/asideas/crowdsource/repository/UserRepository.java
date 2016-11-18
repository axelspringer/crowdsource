package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.security.Roles;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findByEmail(String email);

    @Query("select u from UserEntity u where " + Roles.ROLE_ADMIN + " member of u.roles")
    List<UserEntity> findAllAdminUsers();

    List<UserEntity> findByCreatedDateBetween(DateTime startDate, DateTime endDate);
}
