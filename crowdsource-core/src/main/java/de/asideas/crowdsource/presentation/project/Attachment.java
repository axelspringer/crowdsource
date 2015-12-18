package de.asideas.crowdsource.presentation.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.asideas.crowdsource.domain.model.AttachmentValue;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import org.joda.time.DateTime;

import java.io.InputStream;

/**
 * Client representation of an {@link AttachmentValue}
 */
public class Attachment {

    private String id;
    private String name;
    private Long size;
    private String type;
    private DateTime created;
    private String linkToFile;
    @JsonIgnore
    private InputStream payload;

    private Attachment() {
    }

    public Attachment(AttachmentValue attachmentValue, ProjectEntity parentProject) {
        this.created = attachmentValue.getCreated();
        this.id = attachmentValue.getFileReference();
        this.name = attachmentValue.getFilename();
        this.size = attachmentValue.getSize();
        this.type = attachmentValue.getContentType();
        this.linkToFile = attachmentValue.relativeUri(parentProject);
    }

    /**
     * Full payload constructor
     * @param attachmentValue
     * @param payload
     */
    public Attachment(AttachmentValue attachmentValue, ProjectEntity parentProject, InputStream payload) {
        this(attachmentValue, parentProject);
        setPayload(payload);
    }

    public static Attachment asCreationCommand(String name, String type) {
        Attachment res = new Attachment();
        res.name = name;
        res.type = type;
        return res;
    }

    public static Attachment asLookupByIdCommand(String id) {
        Attachment res = new Attachment();
        res.id = id;
        return res;
    }


    public DateTime getCreated() {
        return created;
    }

    public String getId() {
        return id;
    }

    public String getLinkToFile() {
        return linkToFile;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public InputStream getPayload() {
        return payload;
    }

    private void setCreated(DateTime created) {
        this.created = created;
    }

    private void setId(String id) {
        this.id = id;
    }

    private void setLinkToFile(String linkToFile) {
        this.linkToFile = linkToFile;
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setSize(Long size) {
        this.size = size;
    }

    private void setType(String type) {
        this.type = type;
    }

    public void setPayload(InputStream payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Attachment that = (Attachment) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (size != null ? !size.equals(that.size) : that.size != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (created != null ? !created.equals(that.created) : that.created != null) {
            return false;
        }
        return !(linkToFile != null ? !linkToFile.equals(that.linkToFile) : that.linkToFile != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (linkToFile != null ? linkToFile.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "created=" + created +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", type='" + type + '\'' +
                ", linkToFile='" + linkToFile + '\'' +
                '}';
    }
}
