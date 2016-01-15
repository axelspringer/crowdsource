angular.module('crowdsource')

    .factory('Statistics', function ($resource) {
        var service = {};

        service.getCurrentStatistics = function (data) {
            return $resource("/statistics/current").query(data).$promise;
        };

        service.getProjectsPerStatus = function () {
            return $resource("/statistics/projects_per_status").query().$promise;
        };
        return service;
    });