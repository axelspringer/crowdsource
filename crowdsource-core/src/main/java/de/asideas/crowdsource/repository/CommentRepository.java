package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<CommentEntity, String>, CommentRepositoryCustom {

    List<CommentEntity> findByProject(ProjectEntity projectEntity);

}
