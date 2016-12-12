package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.presentation.project.Attachment;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.AttachmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@Secured(Roles.ROLE_USER)
@RequestMapping("/attachments")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);

    private final AttachmentService attachmentService;

    @Value("#{T(org.springframework.http.MediaType).parseMediaTypes('${de.asideas.crowdsource.attachment.allowedmediatypes}')}")
    private List<MediaType> attachmentTypesAllowed;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/")
    public List<Attachment> getProjectAttachments(@RequestParam("projectId") Long projectId) {
        return attachmentService.findAttachmentsByProjectId(projectId);
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Attachment addProjectAttachment(@RequestParam("projectId") Long projectId, @RequestParam("file") MultipartFile file, Principal principal) throws IOException {

        if (file.isEmpty()) {
            throw InvalidRequestException.fileMustNotBeEmpty();
        }
        if (!contentTypeAllowed(file.getContentType())) {
            throw InvalidRequestException.filetypeNotAllowed();
        }

        Attachment attachment = Attachment.asCreationCommand(file.getOriginalFilename(), file.getContentType(), file.getInputStream());
        return attachmentService.addProjectAttachment(projectId, attachment, principal.getName());
    }

    @Secured({Roles.ROLE_USER})
    @RequestMapping(value = "/{attachmentId}/content", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> serveProjectAttachment(@PathVariable("attachmentId") Long attachmentId) throws IOException {

        final Attachment attachment = attachmentService.getAttachmentWithContent(Attachment.asLookupByIdCommand(attachmentId));

        return ResponseEntity.ok()
                .contentLength(attachment.getSize())
                .contentType(MediaType.valueOf(attachment.getType()))
                .body(new InputStreamResource(attachment.getPayload()));
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.DELETE)
    public void deleteProjectAttachment(@PathVariable("attachmentId") Long attachmentId, Principal principal) {
        attachmentService.deleteAttachment(Attachment.asLookupByIdCommand(attachmentId), principal.getName());
    }

    private boolean contentTypeAllowed(String contentType) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            log.warn("Couldn't parse media type {}", contentType, e);
            return false;
        }
        for (MediaType el : attachmentTypesAllowed) {
            if (el.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }
}
