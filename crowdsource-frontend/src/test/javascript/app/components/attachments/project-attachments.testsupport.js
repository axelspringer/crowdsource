function ProjectAttachments(element) {

    this.attachmentsForm = function () {
        return element.find('#attachments_form');
    };

    this.fileSelector = function () {
        return element.find('input[type=file]');
    };

    this.uploadButton  = function () {
        return element.find('button.upload');
    };

    this.removeFileUploadButton  = function () {
        return element.find('button.unselect-file');
    };

    this.uploadNotification_Success = function () {
        return element.find('.upload-messages__success');
    };

    this.uploadNotification_Error = function () {
        return element.find('.upload-messages__error');
    };

    this.fileInfo = function () {
        return element.find('.file-info');
    };

    this.attachmentsContainer = function () {
        return element.find('.attachments');
    };

    this.attachmentsTable = function () {
        return element.find('.attachments__list');
    };

    this.attachmentsTableRows = function () {
        return element.find('.attachments__list').find('tr');
    };

};
