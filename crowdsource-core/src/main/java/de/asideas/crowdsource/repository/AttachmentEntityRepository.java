package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentEntityRepository extends JpaRepository<AttachmentEntity, Long> {
}
