package de.asideas.crowdsource.repository;

// FIXME: 18/11/16
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = {MongoDBConfig.class, CrowdSourceTestConfig.class})
//@IntegrationTest
public class ProjectAttachmentRepositoryIT {
//
//    @Autowired
//    AttachmentEntityRepository attachmentEntityRepository;
//
//    @Test
//    public void storeAttachment_storedContentShouldBeLoadable() throws Exception {
//        AttachmentValue testFileMeta = givenPlaintextFileMeta("a_testfile_name_storage");
//        String plaintextPayload = "I am payload to be saved and read";
//        InputStream testFileBinary = givenPlaintextPayload(plaintextPayload);
//
//        AttachmentValue res = attachmentEntityRepository.storeAttachment(testFileMeta, testFileBinary);
//
//        thenCreatedFileMetaIsComplete(testFileMeta, res, plaintextPayload);
//
//        InputStream readPayload = attachmentEntityRepository.loadAttachment(res);
//        assertThat(IOUtils.toString(readPayload), is(plaintextPayload));
//    }
//
//    @Test
//    public void deleteAttachment_FileShouldNotBeReadableAnymoreEventually() throws Exception {
//        AttachmentValue testFileMeta = givenPlaintextFileMeta("a_testfile_name_deletion");
//        String plaintextPayload = "I am payload to be saved and deleted";
//        InputStream testFileBinary = givenPlaintextPayload(plaintextPayload);
//
//        AttachmentValue persisted = attachmentEntityRepository.storeAttachment(testFileMeta, testFileBinary);
//        attachmentEntityRepository.deleteAttachment(persisted);
//
//        assertThat(attachmentEntityRepository.loadAttachment(persisted), is(nullValue()));
//    }
//
//    private InputStream givenPlaintextPayload(String plaintextPayload) {
//        return new ByteArrayInputStream(plaintextPayload.getBytes());
//    }
//
//    private AttachmentValue givenPlaintextFileMeta(String filename) {
//        return new AttachmentValue(filename, "text/plain");
//    }
//
//    private void thenCreatedFileMetaIsComplete(AttachmentValue creationMeta, AttachmentValue actualResult, String originalPayload) {
//        assertThat(actualResult.getContentType(), is("text/plain"));
//        assertThat(actualResult.getFilename(), is(creationMeta.getFilename()));
//        assertThat(actualResult.getCreated().toDate(), sameMinute(new Date()));
//        assertThat(actualResult.getFileReference(), is(notNullValue()));
//        assertThat(actualResult.getSize(), is((long) originalPayload.getBytes().length));
//    }
}
