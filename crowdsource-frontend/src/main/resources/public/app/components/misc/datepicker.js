angular.module('crowdsource')

    .directive('datepicker', function () {
        return {
            require: 'ngModel',

            link: function (scope, elem, attrs, ngModel) {

                var future = attrs.future === 'true';

                $(elem).fdatepicker({
                    format: "dd.mm.yyyy",

                    onRender: function (date) {
                        if (future) {
                            //allow only dates starting from tomorrow
                            var nowTemp = new Date();
                            var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate() + 1, 0, 0, 0, 0);
                            return date.valueOf() < now.valueOf() ? 'disabled' : '';
                        }
                    }
                });

                ngModel.$parsers.push(function (stringValue) {
                    if (stringValue) {

                        var date = moment(stringValue, "DD.MM.YYYY");
                        date = date.tz('Europe/Berlin');

                        date.hour(23);
                        date.minute(59);
                        date.second(59);

                        return date.toDate();
                    }
                    return null;
                });
                ngModel.$formatters.push(function (date) {
                    if (date) {
                        return '' + date.getDate() + '.' + (date.getMonth() + 1) + '.' + date.getFullYear();
                    }
                    return '';
                });
            }
        }
    });