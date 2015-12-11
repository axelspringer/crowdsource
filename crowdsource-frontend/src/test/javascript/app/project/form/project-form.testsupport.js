function ProjectForm(element) {

    this.getGeneralErrorsContainer = function () {
        return element.find('.general-error');
    };

    this.getGeneralError = function (violatedRule) {
        return this.getGeneralErrorsContainer().find('[ng-message="' + violatedRule + '"]');
    };

    this.title = new FormGroup(element.find('.form-controls-title'));

    this.shortDescription = new FormGroup(element.find('.form-controls-short-description'), 'textarea');

    this.pledgeGoal = new FormGroup(element.find('.form-controls-pledge-goal'));

    this.description = new FormGroup(element.find('.form-controls-description'), 'textarea');

    this.headline = element.find('.plist__heading');

    this.getSubmitButton = function () {
        return element.find('button[type="submit"]');
    };
}
