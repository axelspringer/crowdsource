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

                vm.data.chart = {};

                vm.statisticTypeChanged = function () {
                    if (vm.data.statisticType.name === vm.data.availablePageOptions.CURRENT.name) {
                        var now = new Date();
                        vm.data.startDate = vm.data.startDate || new Date(now.getFullYear(), now.getMonth(), now.getDate() - 14);
                        vm.data.endDate = vm.data.endDate || now;

                        Statistics.getCurrentStatistics({startDate: vm.data.startDate, endDate: vm.data.endDate}).then(function (response) {
                            vm.data.chart.data = [];
                            vm.data.chart.labels = [];
                            vm.data.chart.series = [];
                            response.forEach(function (seriesEntry) {
                                var data = [];
                                var labels = [];

                                vm.data.chart.series.push(seriesEntry.name);


                                seriesEntry.data.forEach(function(dataEntry) {
                                    labels.push(dataEntry.label);
                                    data.push(dataEntry.data);
                                });

                                vm.data.chart.data.push(data);

                                // NOTE: labels is set twice
                                vm.data.chart.labels = labels;
                            });

                        }, function () {
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

                };

                vm.shouldShowDatePickerWithPrecision = function () {
                    return vm.data.statisticType.name === vm.data.availablePageOptions.CURRENT.name;
                };
            }
        };
    });