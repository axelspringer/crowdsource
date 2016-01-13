angular.module('crowdsource')
    .directive('statisticsResult', function () {
        return {
            restrict: 'E',
            scope: {
                data: '='
            },
            templateUrl: 'app/components/statistics/statistics-result.html',
            controller: function ($scope) {
                var vm = $scope;

                vm.getData = function () {
                    return [
                        [65, 59, 80, 81, 56, 55, 40],
                        [28, 48, 40, 19, 86, 27, 90]
                    ];
                };

                vm.getLabels = function () {
                    return ['2006', '2007', '2008', '2009', '2010', '2011', '2012'];
                };

                vm.showLegend = function () {
                    return true;
                };

                vm.getSeries = function () {
                    return ['Series A', 'Series B'];
                };

                vm.chart = {};
                vm.chart.data = vm.getData();
                vm.chart.labels = vm.getLabels();
                vm.chart.series = vm.getSeries();
            }
        };
    });