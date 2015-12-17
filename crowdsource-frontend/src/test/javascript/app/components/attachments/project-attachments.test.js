describe('project attachements', function () {

    var $rootScope, $compile, projectAttachments, scope, $httpBackend, $filter, FinancingRound, Authentication;
    var attachmentsDirectiveCompiled;

    beforeEach(function () {
        module('crowdsource.templates');
        module('crowdsource');
        module(function (_$analyticsProvider_) {
            _$analyticsProvider_.virtualPageviews(false);
            _$analyticsProvider_.firstPageview(false);
            _$analyticsProvider_.developerMode(true);
        });

        inject(function (_$compile_, _$rootScope_, _$httpBackend_, _$filter_, _FinancingRound_, _Authentication_) {
            $rootScope = _$rootScope_;
            scope = $rootScope.$new();
            $compile = _$compile_;
            $httpBackend = _$httpBackend_;
            FinancingRound = _FinancingRound_;
            Authentication = _Authentication_;
            $filter = _$filter_;

            scope['status'] = {};
            scope['project'] = {
                id: "testProjectId"
            };
            scope['uploadEnabled'] = true;

            compileDirective();

            //currentUser = {
            //    loggedIn: false,
            //    budget: 17,
            //    hasRole: function (roleReq) {
            //        return false;
            //    }
            //};
            //currentRound = {
            //    active: true,
            //    postRoundBudgetDistributable: false,
            //    postRoundBudgetRemaining: 0
            //};
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
        thenFileSelectorIsVisible();
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
    });

    // TODO: Implement on upcoming task ticket(s)
    it("should display attachments of project", function () {

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

    function givenAFileHasBeenSelected() {
        var res = {
            name: "filename.JPG",
            size: 802880,
            type: "image/jpeg",
            webkitRelativePath: ""
        };
        attachmentsDirectiveCompiled.isolateScope().currentAttachment = res;
        return res;
    }

    // WHEN
    function whenUploadButtonIsClickedAndUploadSuccessful(project, file) {
        var responseFile = angular.copy(file);
        var responseProject = angular.copy(project);
        responseProject.attachments = [responseFile];
        responseFile['id'] = "test_fileId";
        $httpBackend.expectPOST('/projects/' + project.id + '/attachments').respond(200, responseFile);
        $httpBackend.expectGET('/project/' + project.id).respond(200, responseProject);
        projectAttachments.uploadButton().click();
        $httpBackend.flush();
    }

    function whenUploadButtonIsClickedAndUploadFailed(project) {
        var errorResponse = {
            errorCode: "unknown_error"
        };
        $httpBackend.expectPOST('/projects/' + project.id + '/attachments').respond(400, errorResponse);
        projectAttachments.uploadButton().click();
        $httpBackend.flush();

    }

    // THEN
    function thenUploadSuccessNotificationIsVisible() {
        expect(projectAttachments.uploadNotification_Success().hasClass('ng-hide')).toBe(false);
        expect(projectAttachments.uploadNotification_Success().text()).toBe("Upload erfolgreich");
        expect(projectAttachments.uploadNotification_Error()).not.toExist();
    }

    function thenFileSelectorIsVisible() {
        expect(projectAttachments.fileSelector()).not.toBeVisible();
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

});