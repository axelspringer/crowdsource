package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.security.Roles;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<UserEntity, String> {

    UserEntity findByEmail(String email);

    @Query("{roles: {$in: " + Roles.ROLE_ADMIN + "}}")
    List<UserEntity> findAllAdminUsers();
}
