package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.LikeEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.shared.LikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    Optional<LikeEntity> findOneByProjectAndUser(ProjectEntity project, UserEntity user);
    long countByProjectAndStatus(ProjectEntity project, LikeStatus status);
}
