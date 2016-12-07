angular.module('crowdsource')

/**
 * A form group (label + input field) for the crowdsource email input where the user
 * only has to enter the local part of the email address
 *
 * Label
 * +----------------+------------------+
 * |        foo.bar | @axelspringer.de |
 * +----------------+------------------+
 */
    .directive('emailFormGroup', function () {
        return {
            restrict: 'E',
            require: '^form',
            scope: {
                model: '=',
                fieldName: '@'
            },
            templateUrl: 'app/components/form/form-group/email/email-form-group.html',
            link: function (scope, element, attributes, form) {
                scope.form = form;
            }
        };
    });