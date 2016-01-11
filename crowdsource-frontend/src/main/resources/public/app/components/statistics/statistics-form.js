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
                    if (vm.data.statisticType) {
                        // TODO replace this part by implementing more sub tasks
                        console.log("current statistic type is " + vm.data.statisticType);
                    }
                };

                vm.clearInfo = function () {
                    vm.data.info = undefined;
                }
            }
        };
    });