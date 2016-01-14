angular.module('crowdsource')
    .directive('statisticsForm', function () {
        var CONST = {
            "DEFAULT_FROM_DAYS": 14
        }, updateResultData = function (vm) {

            vm.data.chart.data = [];
            vm.data.chart.labels = [];
            vm.data.chart.series = [];

            vm.data.statisticsResponse.forEach(function (seriesEntry) {
                var data = [];
                var labels = [];

                vm.data.chart.series.push(seriesEntry.name);


                seriesEntry.data.forEach(function (dataEntry) {
                    labels.push(dataEntry.label);
                    data.push(dataEntry.data);
                });

                vm.data.chart.data.push(data);

                // NOTE: labels is set twice
                vm.data.chart.labels = labels;
            });
        };

        return {
            restrict: 'E',
            scope: {
                data: '='
            },
            templateUrl: 'app/components/statistics/statistics-form.html',
            controller: function ($scope, Statistics) {
                var vm = $scope;

                vm.data.chart = {};

                vm.statisticTypeChanged = function () {
                    if (vm.data.statisticType.name === vm.data.availablePageOptions.CURRENT.name) {
                        var now = new Date();
                        vm.data.startDate = vm.data.startDate || new Date(now.getFullYear(), now.getMonth(), now.getDate() - CONST.DEFAULT_FROM_DAYS);
                        vm.data.endDate = vm.data.endDate || now;

                        Statistics.getCurrentStatistics({startDate: vm.data.startDate, endDate: vm.data.endDate}).then(
                            function (response) {
                                vm.data.statisticsResponse = response;
                                updateResultData(vm);
                            },
                            function () {
                                vm.data.info = "Ooooops!";
                            });
                    }
                };

                vm.dateChanged = function () {
                    console.log("test:" + vm.data.startDate);
                    vm.statisticTypeChanged();
                };

                vm.clearInfo = function () {
                    vm.data.info = undefined;
                };

                vm.statisticTimePrecisionChangeHandler = function () {
                    updateResultData(vm);
                };

                vm.shouldShowDatePickerWithPrecision = function () {
                    return vm.data.statisticType !== undefined
                        && vm.data.availablePageOptions.CURRENT.name === vm.data.statisticType.name;
                };
            }
        };
    });