package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.AttachmentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentEntityRepository extends JpaRepository<AttachmentEntity, Long> {

    List<AttachmentEntity> findAllByProject(ProjectEntity project);
}
