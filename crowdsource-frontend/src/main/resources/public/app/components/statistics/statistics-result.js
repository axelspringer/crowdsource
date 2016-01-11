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
            }
        };
    });