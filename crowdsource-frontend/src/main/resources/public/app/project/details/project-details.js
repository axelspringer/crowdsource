angular.module('crowdsource')
    .controller('ProjectDetailsController', function ($window, $routeParams, $location, $q, Project, FinancingRound, Authentication) {

    var vm = this;

    vm.auth = Authentication;

    // set the project id beforehand to allow the project-comments directive
    // to already load the comments for this project, else it must wait until
    // the GET /project/:id response is finished
    vm.project = {id : $routeParams.projectId };

    activate();

    function activate () {
        var deferred = $q.defer();
        Project.get($routeParams.projectId).then(
            function (res) {
                vm.project = res;
                deferred.resolve(res);
        }, function (errorResp) {
            if (errorResp.status == 404) {
                $location.path('/error/notfound');
            }
            else if (errorResp.status == 403) {
                $location.path('/error/forbidden');
            }
            else {
                $location.path('/error/unknown');
            }
        });
        var res = deferred.promise;
        res.id = $routeParams.projectId;
        vm.project = res;
        return res;
    }

    vm.publish = function () {
        if (!$window.confirm('Willst Du das Projekt wirklich veröffentlichen?')) {
            return;
        }

        vm.publishing = true;
        Project.publish(vm.project.id).$promise
            .then(function (project) {
                vm.project = project;
            })
            .catch(function () {
                $location.path('/error/unknown');
            }).finally(function () {
            vm.publishing = false;
        });
    };

    vm.reject = function () {
        if (!$window.confirm('Willst Du das Projekt wirklich ablehnen?')) {
            return;
        }

        vm.rejecting = true;
        Project.reject(vm.project.id).$promise
            .then(function (project) {
                vm.project = project;
            })
            .catch(function () {
                $location.path('/error/unknown');
            }).finally(function () {
            vm.rejecting = false;
        });
    };

    vm.defer = function () {
        if (!$window.confirm('Willst Du das Projekt wirklich aus der nächsten Finanzierungsrunde ausschließen?')) {
            return;
        }

        vm.deferring = true;
        Project.defer(vm.project.id).$promise
            .then(function (project) {
                vm.project = project;
            })
            .catch(function () {
                $location.path('/error/unknown');
            }).finally(function () {
            vm.deferring = false;
        });
    };

        vm.goToEdit = function () {
            var path = '/project/' + vm.project.id + '/edit';
            $location.path(path);
        };

        vm.isPublishable = function () {
            if (!vm.project.$resolved) {
                return false;
            }
            return vm.project.status == 'PROPOSED' || vm.project.status == 'REJECTED' || vm.project.status =='DEFERRED';
        };

    vm.isRejectable = function () {
        if (!vm.project.$resolved) {
            return false;
        }
        return vm.project.status == 'PROPOSED' || vm.project.status == 'PUBLISHED';
    };

    vm.isDeferrable = function () {
        if (!vm.project.$resolved) {
            return false;
        }
        return (vm.project.status == 'PROPOSED' || vm.project.status == 'PUBLISHED' )
            && !FinancingRound.currentFinancingRound().active;
    };

    vm.toPledgingFormButtonDisabled = function () {
        return vm.project.status == 'FULLY_PLEDGED' || vm.project.status == 'REJECTED'
            || vm.project.status == 'DEFERRED' || vm.project.status == 'PROPOSED';
    };

        vm.editButtonVisibleForUser = function () {
            return vm.auth.currentUser.hasRole("ADMIN") || Project.isCreator(vm.project, vm.auth.currentUser);
        };

        vm.editButtonEnabled = function () {
            console.log("EditBtnEnabled: " + !FinancingRound.currentFinancingRound().active && vm.project.status !== "FULLY_PLEDGED");
            return (FinancingRound.currentFinancingRound() == undefined || !FinancingRound.currentFinancingRound().active)
                && vm.project.status !== "FULLY_PLEDGED";
        };

    });
