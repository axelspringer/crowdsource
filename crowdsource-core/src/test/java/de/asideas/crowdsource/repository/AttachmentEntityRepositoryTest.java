package de.asideas.crowdsource.repository;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@Ignore // FIXME: 18/11/16
@RunWith(MockitoJUnitRunner.class)
public class AttachmentEntityRepositoryTest {

//    @InjectMocks
//    AttachmentEntityRepository attachmentEntityRepository;
//
//    @Mock
//    GridFsOperations gridFsOperations;
//
//
//    @Before
//    public void setUp() throws Exception {
//        reset(gridFsOperations);
//    }
//
//    @Test
//    public void storeFile_shouldInvokeGridFsOps() throws Exception {
//        final AttachmentValue attachment = aStoringRequestAttachment();
//        final InputStream binaryData = mockedInputStream();
//        final AttachmentValue expAttachment = aPersitedAttachment();
//
//        when(gridFsOperations.store(
//                eq(binaryData), eq(attachment.getFilename()), eq(attachment.getContentType())))
//                .thenReturn(mockedFsDbFileFromAttachmentValue(expAttachment));
//        AttachmentValue res = attachmentEntityRepository.storeAttachment(attachment, binaryData);
//
//        assertThat(res, is(expAttachment));
//    }
//
//    @Test
//    public void loadAttachment_shouldReturnNullWhenQueryReturnsNull() throws Exception {
//        when(gridFsOperations.findOne(any(Query.class))).thenReturn(null);
//
//        InputStream res = attachmentEntityRepository.loadAttachment(aPersitedAttachment());
//
//        assertThat(res, is(nullValue()));
//    }
//
//    @Test
//    public void loadAttachment_shouldQueryByFileReference() throws Exception {
//        AttachmentValue attachment = aPersitedAttachment();
//        InputStream expRes = mockedInputStream();
//
//        when(gridFsOperations.findOne(eq(new Query(Criteria.where("_id").is(attachment.getFileReference()))))).thenReturn(
//                new TestGridFsFile(expRes)
//        );
//        InputStream res = attachmentEntityRepository.loadAttachment(attachment);
//
//        assertTrue(res == expRes);
//    }
//
//    @Test
//    public void deleteAttachment_shouldRemoveByFileReference() throws Exception {
//        AttachmentValue attachment = aPersitedAttachment();
//
//        attachmentEntityRepository.deleteAttachment(attachment);
//        verify(gridFsOperations).delete(eq(new Query(Criteria.where("_id").is(attachment.getFileReference()))));
//    }
//
//    private AttachmentValue aPersitedAttachment(){
//        return new AttachmentValue("a_fieldRef", "text/plain", "a_filename", 17, DateTime.now());
//    }
//
//    private AttachmentValue aStoringRequestAttachment() {
//        return new AttachmentValue("test_filename", "text/plain");
//    }
//
//    private GridFSFile mockedFsDbFileFromAttachmentValue(AttachmentValue input) {
//        return new TestGridFsFile(input.getContentType(), input.getCreated().getMillis(), input.getFilename(), input.getFileReference(), input.getSize());
//    }
//
//    private InputStream mockedInputStream(){
//        return mock(InputStream.class);
//    }
//
//
//    private static class TestGridFsFile extends GridFSDBFile {
//        String id;
//        String contentType;
//        String filename;
//        long length;
//        long created;
//        InputStream inputStream;
//
//        public TestGridFsFile(String contentType, long created, String filename, String id, long length) {
//            this.contentType = contentType;
//            this.created = created;
//            this.filename = filename;
//            this.id = id;
//            this.length = length;
//        }
//        public TestGridFsFile(InputStream inputStream) {
//            this.inputStream = inputStream;
//        }
//
//        @Override
//        public String getContentType() {
//            return contentType;
//        }
//
//        public long getCreated() {
//            return created;
//        }
//
//        @Override
//        public String getFilename() {
//            return filename;
//        }
//
//        @Override
//        public String getId() {
//            return id;
//        }
//
//        @Override
//        public long getLength() {
//            return length;
//        }
//
//        @Override
//        public InputStream getInputStream() {
//            return inputStream;
//        }
//    }
}