angular.module('crowdsource')

    .controller('ProjectFormController', function ($location, $routeParams, Project, RemoteFormValidation) {

        var vm = this;

        vm.isEditMode = function () {
            return $routeParams.projectId !== undefined;
        };

        vm.isCreateMode = function () {
            return !vm.isEditMode();
        };

        vm.init = function () {
            if (vm.isEditMode()) {
                vm.project = Project.get($routeParams.projectId);
                vm.project.$promise.catch(function (response) {
                    if (response.status == 404) {
                        $location.path('/error/notfound');
                    }
                    else if (response.status == 403) {
                        $location.path('/error/forbidden');
                    }
                    else {
                        $location.path('/error/unknown');
                    }
                });
            }
        };

        vm.submitProjectIdea = function () {
            if (!vm.form.$valid) {
                return;
            }

            RemoteFormValidation.clearRemoteErrors(vm);
            vm.loading = true;

            var projectRequest;
            if (vm.isCreateMode()) {
                projectRequest = Project.add(vm.project);
            } else {
                projectRequest = Project.edit(vm.project);
            }

            projectRequest.then(function (savedProject) {
                $location.path('/project/new/' + savedProject.id);
            }).catch(function (response) {
                RemoteFormValidation.applyServerErrorResponse(vm, vm.form, response);
            }).finally(function () {
                vm.loading = false;
            });
        };

    });
