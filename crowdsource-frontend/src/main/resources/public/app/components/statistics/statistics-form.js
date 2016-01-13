angular.module('crowdsource')
    .directive('statisticsForm', function () {
        return {
            restrict: 'E',
            scope: {
                data: '='
            },
            templateUrl: 'app/components/statistics/statistics-form.html',
            controller: function ($scope, Statistics) {
                var vm = $scope;

                vm.statisticTypeChangeHandler = function () {
                    if (vm.data.statisticType === vm.data.availablePageOptions.CURRENT.name) {
                        var now = new Date(),
                            startDate = vm.data.startDate || new Date(now.getFullYear(), now.getMonth(), now.getDate() - 14),
                            endDate = vm.data.endDate || now;

                        Statistics.getCurrentStatistics({startDate: startDate, endDate: endDate}).then(function (response) {

                        }, function () {
                            vm.data.info = "Ooooops!";
                        });
                    }
                };

                vm.clearInfo = function () {
                    vm.data.info = undefined;
                };
                
                vm.statisticTimePrecisionChangeHandler = function () {

                };

                vm.shouldShowDatePickerWithPrecision = function () {
                    return vm.data.statisticType === vm.data.availablePageOptions.CURRENT.name;
                };
            }
        };
    });