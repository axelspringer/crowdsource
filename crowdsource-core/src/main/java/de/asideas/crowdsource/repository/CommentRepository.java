package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long>, CommentRepositoryCustom {

    List<CommentEntity> findByProject(ProjectEntity projectEntity);

}
