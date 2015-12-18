package de.asideas.crowdsource.domain.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AttachmentValueTest {

    @Test
    public void relativeUri() throws Exception {
        final AttachmentValue attachmentValue = givenAttachmentValue("expFileRef");
        final ProjectEntity givenProject = givenProjectEntity("aProjectId");

        final String res = attachmentValue.relativeUri(givenProject);

        assertThat(res, is("/projects/" + givenProject.getId() + "/attachments/" + attachmentValue.getFileReference()));
    }

    @Test(expected = IllegalStateException.class)
    public void relativeUri_ShouldThrowIllegalStateExceptionOnIncompleteAttachment() throws Exception {
        final AttachmentValue attachmentValue = givenAttachmentValue(null);
        final ProjectEntity givenProject = givenProjectEntity("aProjectId");

        attachmentValue.relativeUri(givenProject);
    }


    private AttachmentValue givenAttachmentValue(String fileRef) {
        return new AttachmentValue(fileRef, "text/plain", "fileName_" + fileRef, 619, DateTime.now());
    }

    private ProjectEntity givenProjectEntity(String projectId) {
        final ProjectEntity res = new ProjectEntity();
        res.setId(projectId);
        return res;
    }
}