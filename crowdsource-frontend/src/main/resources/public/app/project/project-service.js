angular.module('crowdsource')

    .factory('Project', function ($resource) {

        var service = {};

        var projectResource = $resource('/project/:id');
        var projectsResource = $resource('/projects');
        var projectPledgeResource = $resource('/project/:id/pledges');
        var projectStatusResource = $resource('/project/:id/status', {}, {
            patch: {
                method: 'PATCH'
            }
        });

        service.add = function (project) {
            return projectResource.save(project).$promise;
        };

        service.getAll = function () {
            return projectsResource.query();
        };

        service.get = function (projectId) {
            return projectResource.get({id: projectId}).$promise;
        };

        service.pledge = function (projectId, pledge) {
            return projectPledgeResource.save({id: projectId}, pledge);
        };

        service.publish = function (projectId) {
            return projectStatusResource.patch({id: projectId}, {status: 'PUBLISHED'});
        };

        service.reject = function (projectId) {
            return projectStatusResource.patch({id: projectId}, {status: 'REJECTED'});
        };

        service.defer = function (projectId) {
            return projectStatusResource.patch({id: projectId}, {status: 'DEFERRED'} );
        };

        return service;
    });