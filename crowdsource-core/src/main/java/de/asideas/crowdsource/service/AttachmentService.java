package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.AttachmentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.repository.AttachmentEntityRepository;
import de.asideas.crowdsource.repository.ProjectRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.*;
import static org.apache.commons.io.IOUtils.toByteArray;

@Service
public class AttachmentService {

    private final AttachmentEntityRepository attachmentEntityRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AttachmentService(AttachmentEntityRepository attachmentEntityRepository, UserRepository userRepository, ProjectRepository projectRepository) {
        this.attachmentEntityRepository = attachmentEntityRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Attachment addProjectAttachment(Long projectId, Attachment attachment, String userEmail) throws IOException {
        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        final UserEntity creator = loadUserEntity(userEmail);

        final AttachmentEntity attachmentEntity = new AttachmentEntity(
                attachment.getName(), toByteArray(attachment.getPayload()), attachment.getType(), projectEntity, creator);
        attachmentEntityRepository.save(attachmentEntity);
        return Attachment.withoutPayload(attachmentEntity);
    }

    @Transactional
    public List<Attachment> findAttachmentsByProjectId(Long projectId) {
        final ProjectEntity projectEntity = loadProjectEntity(projectId);
        return attachmentEntityRepository.findAllByProject(projectEntity).stream().map(Attachment::withoutPayload).collect(toList());
    }

    @Transactional
    public Attachment getAttachmentWithContent(Attachment attachment) {
        final AttachmentEntity attachmentEntity = attachmentEntityRepository.findOne(attachment.getId());
        if (attachmentEntity == null) {
            return null;
        } else {
            return Attachment.withPayload(attachmentEntity);
        }
    }

    @Transactional
    @PreAuthorize("authentication.name == #name or hasRole('ROLE_ADMIN')")
    public void deleteAttachment(Attachment attachment, String name) {
        attachmentEntityRepository.delete(attachment.getId());
    }

    private ProjectEntity loadProjectEntity(Long projectId) {
        ProjectEntity projectEntity = projectRepository.findOne(projectId);
        if (projectEntity == null) {
            throw new ResourceNotFoundException();
        }
        return projectEntity;
    }

    private UserEntity loadUserEntity(String userEmail) {
        UserEntity userEntity = userRepository.findByEmail(userEmail);
        if (userEntity == null) {
            throw new ResourceNotFoundException();
        }
        return userEntity;
    }
}
