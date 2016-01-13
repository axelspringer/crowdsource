angular.module('crowdsource')

    .factory('Statistics', function ($resource) {
        var service = {};

        service.getCurrentStatistics = function (data) {
            return $resource("/statistics/current").query(data).$promise;
        };
        
        return service;
    });