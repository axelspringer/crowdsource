package de.asideas.crowdsource.domain.model;

import org.junit.Ignore;

@Ignore // FIXME: 18/11/16
public class AttachmentValueTest {

//    @Test
//    public void relativeUri() throws Exception {
//        final AttachmentValue attachmentValue = givenAttachmentValue("expFileRef");
//        final ProjectEntity givenProject = givenProjectEntity("aProjectId");
//
//        final String res = attachmentValue.relativeUri(givenProject);
//
//        assertThat(res, is("/projects/" + givenProject.getId() + "/attachments/" + attachmentValue.getFileReference()));
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void relativeUri_ShouldThrowIllegalStateExceptionOnIncompleteAttachment() throws Exception {
//        final AttachmentValue attachmentValue = givenAttachmentValue(null);
//        final ProjectEntity givenProject = givenProjectEntity("aProjectId");
//
//        attachmentValue.relativeUri(givenProject);
//    }
//
//    @Test
//    public void equals_shouldMatchByFileRefOnly() throws Exception {
//
//        final AttachmentValue a_0 = givenAttachmentValue("file_ref_0");
//        AttachmentValue a_1 = new AttachmentValue("file_ref_1", "image/*", "fileName_" + 0, 12L, DateTime.now().plusDays(1));
//
//        assertThat(a_0, is(not(a_1)));
//
//        a_1 = new AttachmentValue("file_ref_0", "image/*", "fileName_" + 0, 12L, DateTime.now().plusDays(1));
//        assertThat(a_0, is(a_1));
//
//    }
//
//    @Test
//    public void hashCode_shouldBeDeterminedByFileRefOnly() throws Exception {
//
//        final AttachmentValue a_0 = givenAttachmentValue("file_ref_0");
//        AttachmentValue a_1 = new AttachmentValue("file_ref_1", "image/*", "fileName_" + 0, 12L, DateTime.now().plusDays(1));
//
//        assertThat(a_0.hashCode(), is(not(a_1.hashCode())));
//
//        a_1 = new AttachmentValue("file_ref_0", "image/*", "fileName_" + 0, 12L, DateTime.now().plusDays(1));
//        assertThat(a_0.hashCode(), is(a_1.hashCode()));
//
//    }
//
//    private AttachmentValue givenAttachmentValue(String fileRef) {
//        return new AttachmentValue(fileRef, "text/plain", "fileName_" + fileRef, 619, DateTime.now());
//    }
//
//    private ProjectEntity givenProjectEntity(String projectId) {
//        final ProjectEntity res = new ProjectEntity();
//        res.setId(projectId);
//        return res;
//    }
}