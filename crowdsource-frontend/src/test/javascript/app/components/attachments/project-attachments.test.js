describe('project attachements', function () {

    var $rootScope, $compile, projectAttachments, scope, $httpBackend, $filter, $timeout, $browser, $location;
    var attachmentsDirectiveCompiled;

    beforeEach(function () {
        module('crowdsource.templates');
        module('crowdsource');
        module(function (_$analyticsProvider_) {
            _$analyticsProvider_.virtualPageviews(false);
            _$analyticsProvider_.firstPageview(false);
            _$analyticsProvider_.developerMode(true);
        });

        inject(function (_$compile_, _$rootScope_, _$httpBackend_, _$filter_, _$timeout_, _$browser_, _$location_) {
            $rootScope = _$rootScope_;
            scope = $rootScope.$new();
            $compile = _$compile_;
            $httpBackend = _$httpBackend_;
            $filter = _$filter_;
            $timeout = _$timeout_;
            $browser = _$browser_;
            $location = _$location_;

            scope['status'] = {};
            scope['project'] = {
                id: "testProjectId"
            };
            scope['uploadEnabled'] = true;

            compileDirective();

        });
    });

    function compileDirective() {
        attachmentsDirectiveCompiled = $compile('<project-attachments upload-enabled="true" project="project"></project-attachments>')(scope);
        scope.$digest();
        reloadViewRepresentation();
    }

    function reloadViewRepresentation() {
        projectAttachments = new ProjectAttachments(attachmentsDirectiveCompiled);
    }

    it("should display attachments upload form when uploadEnabled", function () {
        givenUploadIsEnabled(true);

        scope.$digest();

        expect(projectAttachments.attachmentsForm()).toExist();
        expect(projectAttachments.fileSelector()).toExist();
    });

    it("should not display attachments upload form when uploadEnabled is false", function () {
        givenUploadIsEnabled(false);

        scope.$digest();

        expect(projectAttachments.attachmentsForm()).not.toExist();
        expect(projectAttachments.fileSelector()).not.toExist();
    });

    it("should invoke multipart request upon file submission and show success message", function () {
        givenUploadIsEnabled(true);
        var project = givenAProjectWasInjected();
        var mockedFileInput = givenAFileHasBeenSelected();

        scope.$digest();
        whenUploadButtonIsClickedAndUploadSuccessful(project, mockedFileInput);

        thenUploadSuccessNotificationIsVisible();
        thenFileSelectorIsVisible(true);
    });

    it("should invoke multipart request upon file submission and show error message", function () {
        givenUploadIsEnabled(true);
        var project = givenAProjectWasInjected();
        givenAFileHasBeenSelected();
        scope.$digest();

        whenUploadButtonIsClickedAndUploadFailed(project);

        thenUploadFailedNotificationIsVisible();
    });

    it("should show file name and size when file has been selected", function () {
        givenUploadIsEnabled(true);
        givenAProjectWasInjected();
        var file = givenAFileHasBeenSelected();

        scope.$digest();

        thenFileNameAndSizeAreDisplayed(file);
        thenFileSelectorIsVisible(false);
    });

    it("should remove the selected file when the remove button next to the upload button is clicked", function () {
        givenUploadIsEnabled(true);
        givenAProjectWasInjected();
        givenAFileHasBeenSelected();

        scope.$digest();
        whenRemoveFileBtnNextToUploadBtnIsClicked();

        thenFileSelectorIsVisible(true);
    });

    it("should display attachments of project", function () {
        var uploadIsEnabled = true;
        givenUploadIsEnabled(uploadIsEnabled);
        var project = givenAProjectWithAttachmentsWasInjected();

        scope.$digest();

        thenFileAttachmentsListContainerExists(uploadIsEnabled);
        thenFileAttachmentListShowsFilesOfProjectAndConcerningActionButtons(project, uploadIsEnabled);
    });

    it("should display attachments of project although upload is disabled", function () {
        givenUploadIsEnabled(false);
        var project = givenAProjectWithAttachmentsWasInjected();

        scope.$digest();

        thenFileAttachmentsListContainerExists(true);
        thenFileAttachmentListShowsFilesOfProjectAndConcerningActionButtons(project, false);
    });

    it("filter 'bytesAsMegabytes' should format correctly", function () {
        var givenBytes = 1024 * 1024 * 42;
        expect($filter('bytesAsMegabytes')(givenBytes).replace(".", ",")).toBe("42,00");
    });

    it("should output correct file link", function () {
        givenUploadIsEnabled(false);
        var project = givenAProjectWithAttachmentsWasInjected();
        var expLocation = 'http://mycrowd.com:8080';
        givenBrowserLocationIsSetTo("mycrowd.com", "8080");

        scope.$digest();

        thenExpectLinkToFileWithLocation(project.attachments[0], expLocation);
    });

    it("should output correct markdown link", function () {
        givenUploadIsEnabled(false);
        var project = givenAProjectWithAttachmentsWasInjected();
        var expLocation = 'http://mycrowd.com:8080';
        givenBrowserLocationIsSetTo("mycrowd.com", "8080");

        scope.$digest();

        thenExpectMarkdownImageSnippet(project.attachments[0], expLocation);
    });

    // GIVEN
    function givenUploadIsEnabled(enabled) {
        attachmentsDirectiveCompiled.isolateScope().uploadEnabled = enabled;
    }

    function givenAProjectWasInjected() {
        var project = {
            $resolved: true,
            id: 123,
            pledgeGoal: 100,
            pledgedAmount: 50,
            status: 'PUBLISHED'
        };
        attachmentsDirectiveCompiled.isolateScope().project = project;
        return project;
    }

    function givenAProjectWithAttachmentsWasInjected() {
        var project = givenAProjectWasInjected();
        project.attachments = [];
        project.attachments.push({
            id: "test_attachment_id_0",
            name: "test_attachment_name_0",
            size: 2 * 1024 * 1024 * 1024, // e.g. 2MB
            type: "text/plain",
            created: moment(),
            linkToFile: "/projects/test_project_id/attachments/test_attachment_id_0"
        });
        project.attachments.push({
            id: "test_attachment_id_1",
            name: "test_attachment_name_1",
            size: 2 * 1024 * 1024 * 1024, // e.g. 2MB
            type: "image/jpeg",
            created: moment(),
            linkToFile: "/projects/test_project_id/attachments/test_attachment_id_1"
        });
        return project;
    }

    function givenAFileHasBeenSelected() {
        var res = {
            name: "filename.JPG",
            size: 802880,
            type: "image/jpeg",
            webkitRelativePath: ""
        };
        attachmentsDirectiveCompiled.isolateScope().uploads.currentAttachment = res;
        return res;
    }

    function givenBrowserLocationIsSetTo(host, port) {
        spyOn($location, 'protocol').and.returnValue('http');
        spyOn($location, 'host').and.returnValue(host);
        spyOn($location, 'port').and.returnValue(port);
    }

    // WHEN
    function whenUploadButtonIsClickedAndUploadSuccessful(project, file) {
        var responseFile = angular.copy(file);
        var responseProject = angular.copy(project);
        responseProject.attachments = [responseFile];
        responseFile['id'] = "test_fileId";
        $httpBackend.expectPOST('/projects/' + project.id + '/attachments').respond(200, responseFile);
        projectAttachments.uploadButton().click();
        $httpBackend.flush();
        $httpBackend.expectGET('/project/' + project.id).respond(200, responseProject);
        $timeout.flush();
    }

    function whenUploadButtonIsClickedAndUploadFailed(project) {
        var errorResponse = {
            errorCode: "unknown_error"
        };
        $httpBackend.expectPOST('/projects/' + project.id + '/attachments').respond(400, errorResponse);
        projectAttachments.uploadButton().click();
        $httpBackend.flush();

    }

    function whenRemoveFileBtnNextToUploadBtnIsClicked() {
        projectAttachments.removeFileUploadButton().click();
        scope.$digest();
    }

    // THEN
    function thenUploadSuccessNotificationIsVisible() {

        expect(projectAttachments.uploadNotification_Success().hasClass('ng-hide')).toBe(false);
        expect(projectAttachments.uploadNotification_Success().text()).toBe("Upload erfolgreich");
        expect(projectAttachments.uploadNotification_Error()).not.toExist();
    }

    function thenFileSelectorIsVisible(visible) {
        expect(projectAttachments.fileSelector().hasClass('ng-hide')).toBe(!visible);
    }

    function thenUploadFailedNotificationIsVisible() {
        expect(projectAttachments.uploadNotification_Success().hasClass('ng-hide')).toBe(true);
        expect(projectAttachments.uploadNotification_Error()).toExist();
        expect(projectAttachments.uploadNotification_Error().text()).toContain("Upload aus unbekannten Gründen leider fehlgeschlagen.");
    }

    function thenFileNameAndSizeAreDisplayed(file) {
        expect(projectAttachments.fileInfo().text()).toContain(file.name + " - " + $filter('number')(file.size / 1024 / 1024, 2));
        expect(projectAttachments.fileSelector()).not.toBeVisible();
    }

    function thenFileAttachmentsListContainerExists(existing) {
        if(existing){
            expect(projectAttachments.attachmentsContainer()).toExist();
        }else {
            expect(projectAttachments.attachmentsContainer()).not.toExist();
        }

    }

    function thenFileAttachmentListShowsFilesOfProjectAndConcerningActionButtons(project, uploadEnabled) {
        var attachments = project.attachments;
        var attachmentRows = projectAttachments.attachmentsTableRows();

        expect(attachmentRows.length).toBe(1 + attachments.length); // +1 -> table head

        for (var i = 0; i< attachments.length; i++) {
            var currentRow = attachmentRows[i+1];
            var cells = currentRow.cells;
            var attachment = attachments[i];

            expect(cells[0].innerHTML).toContain(attachments[i].name);
            expect(cells[1].innerHTML).toContain($filter('number')(attachments[i].size / 1024 / 1024, 2) + " MB" );
            expect( $(cells[2]).find('a.delete-attachment').hasClass('ng-hide')).toBe(!uploadEnabled);
            expect( $(cells[2]).find('a.copy-attachment-link').hasClass('ng-hide')).toBe(false);
            expect( $(cells[2]).find('a.copy-attachment-md-link').hasClass('ng-hide')).toBe(
                !(attachment.type.indexOf('image/') > -1 && uploadEnabled));

        }
    }

    function thenExpectLinkToFileWithLocation(attachment, expectedLocation) {
        expect(attachmentsDirectiveCompiled.isolateScope().absoluteFileUrl(attachment))
            .toBe(expectedLocation + attachment.linkToFile);
    }

    function thenExpectMarkdownImageSnippet(attachment, expectedLocation) {
        expect(attachmentsDirectiveCompiled.isolateScope().markdownImageInclude(attachment))
            .toBe("![" + attachment.name + "](" + expectedLocation + attachment.linkToFile + ")");
    }

});