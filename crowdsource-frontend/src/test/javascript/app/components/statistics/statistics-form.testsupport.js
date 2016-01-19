function StatisticsForm(element) {

    this.getSelectedStatisticsType = function () {
        return element.find(".statistics-form-type-select-dropdown option:selected");
    };

    this.getStatisticsTypeSelect = function () {
        return element.find(".statistics-form-type-select-dropdown");
    };

    this.getSelectsFormForTypeCurrent = function () {
        return element.find(".statistics-form-datepicker");
    };

    this.getSelectsFormForTypeCountPerProject = function () {
        return element.find(".statistics-form-comments-per-project");
    };

    this.getAlertBox = function () {
        return element.find('.alert-box__statistics');
    };

}
