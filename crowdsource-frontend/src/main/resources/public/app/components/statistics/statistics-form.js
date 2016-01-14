angular.module('crowdsource')
    .directive('statisticsForm', function () {
        var CONST = {
            "DEFAULT_FROM_DAYS": 14
        }, updateResultData = function (vm) {
            var currentPrecision = vm.data.timePrecision,
                responseData = vm.data.statisticsResponse;

            console.log("updating result data with precision set to " + currentPrecision.id);

            vm.data.chart.data = [];
            vm.data.chart.labels = [];
            vm.data.chart.series = [];

            if (currentPrecision.id === vm.data.availableTimePrecisionTypeOptions.WEEK.id) {
                responseData.forEach(function (seriesEntry) {
                    var data = [];
                    var labels = [];
                    var temp = 0,
                        startDate = moment(vm.data.startDate);

                    var  endDate = moment(vm.data.endDate),
                        nextMondayFromStart = moment(vm.data.startDate).day(8),
                        duration = nextMondayFromStart.diff(startDate, 'days'),
                        daysBetweenStartAndEnd = endDate.diff(startDate, 'days'),
                        i = 0,
                        j = 0,
                        weekOfYearForStartDate = 1; // todo

                    console.log("startdate date" + startDate.date());
                    console.log("nextMondayFromStart date" + nextMondayFromStart.date());

                    console.log("daysBetweenStartAndEnd " + daysBetweenStartAndEnd);
                    console.log("duration " + duration);

                    //while (i < daysBetweenStartAndEnd) {
                    while (i < 3) {
                        j = 0;
                        console.log("inside week loop with i" + i);
                        while (j < duration) {
                            console.log("inside duration with i = " + i + " and duration " + duration);
                            temp += seriesEntry.data[i];
                            j++;
                            i++;
                        }

                        data.push(temp);
                        labels.push(weekOfYearForStartDate++);

                        console.log("THE REST OF DAYS: " + daysBetweenStartAndEnd - i);
                        if (daysBetweenStartAndEnd - i > 7) {
                            console.log("duration higher 7");
                            duration = 7;
                        } else {
                            duration = daysBetweenStartAndEnd - i > 7;
                            console.log("duration smaller 7: " + duration);
                        }
                    }

                    vm.data.chart.series.push(seriesEntry.name);

                    console.log("DATA " + data);
                    console.log("LABELS " + labels);

                    vm.data.chart.data.push(data);

                    // NOTE: labels is set twice
                    vm.data.chart.labels = labels;
                });
            } else {
                responseData.forEach(function (seriesEntry) {
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
            }

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
                    console.log(vm.data.timePrecision);
                    updateResultData(vm);
                };

                vm.shouldShowDatePickerWithPrecision = function () {
                    return vm.data.statisticType !== undefined
                        && vm.data.availablePageOptions.CURRENT.name === vm.data.statisticType.name;
                };
            }
        };
    });