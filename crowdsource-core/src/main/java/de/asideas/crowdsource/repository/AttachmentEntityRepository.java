package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentEntityRepository extends JpaRepository<AttachmentEntity, Long> {
    
    // // FIXME: 18/11/16
    
//
//    @Autowired
//    GridFsOperations gridFsOperations;
//
//    public AttachmentValue storeAttachment(AttachmentValue fileMetadata, InputStream binaryData) {
//        GridFSFile res = gridFsOperations.store(binaryData, fileMetadata.getFilename(), fileMetadata.getContentType());
//        return new AttachmentValue(res.getId().toString(), res.getContentType(), res.getFilename(), res.getLength(), new DateTime(res.getUploadDate()));
//    }
//
//    public InputStream loadAttachment(AttachmentValue attachment) {
//        Assert.notNull(attachment.getFileReference(), "AttachmentValue.fileReference must not be null");
//        GridFSDBFile res = gridFsOperations.findOne(new Query(Criteria.where("_id").is(attachment.getFileReference())));
//        if(res == null){
//            return null;
//        }
//        return res.getInputStream();
//    }
//
//    public void deleteAttachment(AttachmentValue attachment) {
//        gridFsOperations.delete(new Query(Criteria.where("_id").is(attachment.getFileReference())));
//    }
}
