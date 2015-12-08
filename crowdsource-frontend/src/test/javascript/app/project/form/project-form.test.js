describe('project form', function () {

    var $httpBackend, $location, mockedRouteParams, $scope, projectForm, controller, view;

    beforeEach(function () {
        module('crowdsource');
        module('crowdsource.templates');
        module(function (_$analyticsProvider_) {
            _$analyticsProvider_.virtualPageviews(false);
            _$analyticsProvider_.firstPageview(false);
            _$analyticsProvider_.developerMode(true);
        });

        localStorage.clear(); // reset

        inject(function ($compile, $rootScope, $templateCache, $controller, _$httpBackend_, _$location_, Project, RemoteFormValidation) {
            $scope = $rootScope.$new();
            $httpBackend = _$httpBackend_;
            $location = _$location_;

            mockedRouteParams = {};

            controller = $controller('ProjectFormController as projectForm', {
                $scope: $scope,
                $routeParams: mockedRouteParams,
                $location: _$location_,
                Project: Project,
                RemoteFormValidation: RemoteFormValidation
            });

            var template = $templateCache.get('app/project/form/project-form.html');
            view = $compile(template)($scope);

            $scope.$digest();
            projectForm = new ProjectForm(view);
        });
    });


    function expectValidationError(inputName, violatedRule) {
        expect(projectForm[inputName].getLabelContainer()).toHaveClass('error');
        expect(projectForm[inputName].getLabel()).toHaveClass('ng-hide');
        expect(projectForm[inputName].getErrorLabelsContainer()).not.toHaveClass('ng-hide');
        expect(projectForm[inputName].getErrorLabelForRule(violatedRule)).toExist();
    }

    function expectNoValidationError(inputName) {
        expect(projectForm[inputName].getLabelContainer()).not.toHaveClass('error');
        expect(projectForm[inputName].getLabel()).not.toHaveClass('ng-hide');
        expect(projectForm[inputName].getErrorLabelsContainer()).toHaveClass('ng-hide');
    }

    function fillAndSubmitForm(title, shortDescription, pledgeGoal, description) {
        projectForm.title.getInputField().val(title).trigger('input');
        projectForm.shortDescription.getInputField().val(shortDescription).trigger('input');
        projectForm.pledgeGoal.getInputField().val(pledgeGoal).trigger('input');
        projectForm.description.getInputField().val(description).trigger('input');

        projectForm.getSubmitButton().click();
    }

    function expectBackendCallAndRespond(statusCode, responseBody) {
        $httpBackend.expectPOST('/project', {
                "title": "Title",
                "shortDescription": "Short description",
                "pledgeGoal": 12500,
                "description": "Looong description"
            })
            .respond(statusCode, responseBody);
    }

    function reInitProjectForm(){
        projectForm = new ProjectForm(view);
    }

    it('should show a specific headline when called in creation mode', function () {
        expect(projectForm.headline.text()).toBe('Deine neue Projektidee');
    });

    it('should show no validation errors when the form is untouched', function () {
        expect(projectForm.getGeneralErrorsContainer()).not.toExist();

        expectNoValidationError('title');
        expectNoValidationError('shortDescription');
        expectNoValidationError('pledgeGoal');
        expectNoValidationError('description');
    });

    it('should show no validation errors when the form is filled correctly', function () {
        projectForm.title.getInputField().val(TestData.generateString(60)).trigger('input');
        projectForm.shortDescription.getInputField().val(TestData.generateString(140)).trigger('input');
        projectForm.pledgeGoal.getInputField().val('12500').trigger('input');
        projectForm.description.getInputField().val('Looong description').trigger('input');

        expect(projectForm.getGeneralErrorsContainer()).not.toExist();

        expectNoValidationError('title');
        expectNoValidationError('shortDescription');
        expectNoValidationError('pledgeGoal');
        expectNoValidationError('description');
    });

    it('should POST the data to the server and redirect to success page', function () {
        expectBackendCallAndRespond(200, {id: 'aabbcc'});

        fillAndSubmitForm('Title', 'Short description', '12500', 'Looong description');
        $httpBackend.flush();

        expect($location.path()).toBe('/project/new/aabbcc');
    });

    it('should disable the submit button and change it\'s text while loading', function () {
        expectBackendCallAndRespond(200, {id: 'aabbcc'});

        expect(projectForm.getSubmitButton()).toHaveText('Absenden');
        expect(projectForm.getSubmitButton()).not.toBeDisabled();
        expect(projectForm.getSubmitButton()).toHaveAttr('analytics-on');
        expect(projectForm.getSubmitButton()).toHaveAttr('analytics-category', 'Projects');
        expect(projectForm.getSubmitButton()).toHaveAttr('analytics-event', 'ProjectIdeaSubmitted');

        fillAndSubmitForm('Title', 'Short description', '12500', 'Looong description');

        expect(projectForm.getSubmitButton()).toHaveText('Absenden...');
        expect(projectForm.getSubmitButton()).toBeDisabled();

        $httpBackend.flush();

        expect(projectForm.getSubmitButton()).toHaveText('Absenden');
        expect(projectForm.getSubmitButton()).not.toBeDisabled();
    });

    it('should show an unknown error when the backend call results in 500', function () {
        expectBackendCallAndRespond(500);
        spyOn($location, 'path');

        fillAndSubmitForm('Title', 'Short description', '12500', 'Looong description');
        $httpBackend.flush();
        expect(projectForm.getGeneralErrorsContainer()).toExist();
        expect(projectForm.getGeneralError('remote_unknown')).toExist();

        expect($location.path).not.toHaveBeenCalled();
    });

    it('should show "required" validation errors when the form is submitted without touching the input fields', function () {
        projectForm.getSubmitButton().click();

        expectValidationError('title', 'required');
        expectValidationError('shortDescription', 'required');
        expectValidationError('pledgeGoal', 'required');
        expectValidationError('description', 'required');
    });

    it('should show a validation error if the title is changed to blank', function () {
        projectForm.title.getInputField().val('Title').trigger('input');
        projectForm.title.getInputField().val('').trigger('input');

        expectValidationError('title', 'required');
    });

    it('should show a validation error if the title is too long', function () {
        projectForm.title.getInputField().val(TestData.generateString(61)).trigger('input');

        expectValidationError('title', 'maxlength');
    });

    it('should show a validation error if the short description is changed to blank', function () {
        projectForm.shortDescription.getInputField().val('Short description').trigger('input');
        projectForm.shortDescription.getInputField().val('').trigger('input');

        expectValidationError('shortDescription', 'required');
    });

    it('should show a validation error if the short description is too long', function () {
        projectForm.shortDescription.getInputField().val(TestData.generateString(141)).trigger('input');

        expectValidationError('shortDescription', 'maxlength');
    });

    it('should show a validation error if the pledge goal is no number', function () {
        projectForm.pledgeGoal.getInputField().val('12.01').trigger('input');

        expectValidationError('pledgeGoal', 'pattern');
    });

    it('should show a validation error if the pledge goal is zero', function () {
        projectForm.pledgeGoal.getInputField().val('0').trigger('input');

        expectValidationError('pledgeGoal', 'min');
    });

    it('should show a validation error if the description is changed to blank', function () {
        projectForm.description.getInputField().val('Loong description').trigger('input');
        projectForm.description.getInputField().val('').trigger('input');

        expectValidationError('description', 'required');
    });

    it('should initialize form with existing project data when called in edit mode', function () {
        // Given
        var existingProject = {
            "title": "Title 2 Edit",
            "shortDescription": "Short description 2 Edit",
            "pledgeGoal": 42000,
            "description": "Looong description 2 Edit"
        };
        mockedRouteParams.projectId = 'pId';
        $httpBackend.expectGET('/project/pId').respond(200, existingProject);

        // When
        controller.init(); // Re-Init to use mocked route params
        $scope.$digest();
        $httpBackend.flush();

        // Then
        expect(projectForm.title.getInputField().val()).toBe(existingProject.title);
        expect(projectForm.shortDescription.getInputField().val()).toBe(existingProject.shortDescription);
        expect(projectForm.pledgeGoal.getInputField().val()).toBe("" + existingProject.pledgeGoal);
        expect(projectForm.description.getInputField().val()).toBe(existingProject.description);
    });

    it('should show a specific headline when called in edit mode', function () {
        // Given
        var existingProject = {
            "title": "Title 2 Edit",
            "shortDescription": "Short description 2 Edit",
            "pledgeGoal": 42000,
            "description": "Looong description 2 Edit"
        };
        mockedRouteParams.projectId = 'pId';
        $httpBackend.expectGET('/project/pId').respond(200, existingProject);

        // When
        controller.init(); // Re-Init to use mocked route params
        $scope.$digest();
        $httpBackend.flush();
        reInitProjectForm();

        expect(projectForm.headline.text()).toBe('Projekt Editieren');
    });

});
