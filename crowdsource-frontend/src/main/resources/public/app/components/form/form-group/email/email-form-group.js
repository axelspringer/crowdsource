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
    .directive('emailFormGroup', function (emailDomain) {
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
                scope.EMAIL_DOMAIN = emailDomain;
            }
        };
    })

/**
 * When applied to an <input type="email"> field, this causes the input's model to be
 * enriched with the value of the emailDomain value service. The user only has to enter
 * the local part of the email address (everything before the '@') and the model
 * will automatically contain the full email address.
 */
    .directive('localEmailPartInput', function (emailDomain) {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, element, attributes, ngModel) {
                ngModel.$parsers.push(function (value) {
                    if (value) {
                        return value + emailDomain;
                    }
                    return value;
                });
                ngModel.$formatters.push(function (value) {
                    if (value) {
                        return value.substr(0, value.indexOf(emailDomain));
                    }
                    return value;
                });
            }
        };
    })

/**
 * Custom validator that does not allow the local email part to contain "_extern"
 */
    .directive('nonExternalEmail', function (emailBlacklistPatterns) {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, element, attributes, ngModel) {
                ngModel.$validators.non_blacklisted_email = function (modelValue) {
                    if (!modelValue) {
                        return true;
                    }

                    for (var i = 0; i < emailBlacklistPatterns.length; i++) {
                        if (modelValue.indexOf(emailBlacklistPatterns[i]) >= 0) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        };
    });