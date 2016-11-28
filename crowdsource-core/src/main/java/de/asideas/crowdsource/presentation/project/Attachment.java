package de.asideas.crowdsource.presentation.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.asideas.crowdsource.domain.model.AttachmentEntity;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
public class Attachment {

    private Long id;
    private String name;
    private Long size;
    private String type;
    private DateTime created;
    @JsonIgnore
    private transient InputStream payload;

    public Attachment() {
    }

    private Attachment(Long id) {
        this.id = id;
    }

    private Attachment(Long id, String name, Long size, String type, DateTime created) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.created = created;
    }

    private Attachment(Long id, String name, Long size, String type, DateTime created, InputStream payload) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.created = created;
        this.payload = payload;
    }

    private Attachment(String name, String type, InputStream payload) {
        this.name = name;
        this.type = type;
        this.payload = payload;
    }
    public static Attachment asLookupByIdCommand(Long id) {
        return new Attachment(id);
    }
    public static Attachment asCreationCommand(String name, String type, InputStream inputStream) {
        return new Attachment(name, type, inputStream);
    }

    public static Attachment asResponse(Long id, String name, Long size, String type, DateTime created, InputStream payload) {
        return new Attachment(id, name, size, type, created, payload);
    }

    public static Attachment asResponseWithoutPayload(Long id, String name, Long size, String type, DateTime created) {
        return new Attachment(id, name, size, type, created);
    }

    public static Attachment withoutPayload(AttachmentEntity entity) {
        return new Attachment(entity.getId(), entity.getFilename(), entity.getSize(), entity.getContentType(), entity.getCreated());
    }

    public static Attachment withPayload(AttachmentEntity entity) {
        final InputStream inputStream = new ByteArrayInputStream(entity.getContent());
        return new Attachment(entity.getId(), entity.getFilename(), entity.getSize(), entity.getContentType(), entity.getCreated(), inputStream);
    }
}
