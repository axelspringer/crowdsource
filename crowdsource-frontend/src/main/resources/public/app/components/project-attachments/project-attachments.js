angular.module('crowdsource')
    /**
     * Manages project file attachments. Supports display and Upload.
     */
    .directive('projectAttachments', function () {
        return {
            restrict: 'E',
            scope: {
                project: '=',
                uploadEnabled: '@'
            },
            templateUrl: 'app/components/project-attachments/project-attachments.html',

            controller: function ($scope, Upload, $timeout, RemoteFormValidation, Project) {
                var vm = $scope;

                vm.lastUploadSuccessful = undefined;

                $scope.$watch("project", function(project){
                    if(project == undefined){
                        return;
                    }
                    // Resolve project when deferred by parent scope in order to refresh this form correctly
                    vm.project = project;
                });

                vm.uploadAttachment = function(file) {
                    console.log("Upload triggered for file: " + file.name);
                    file.upload = Upload.upload({
                        url: '/projects/' + vm.project.id + '/attachments',
                        data: {file: file, id: vm.project.id},
                    });

                    file.upload.then(function (response) {
                        //$timeout(function () {
                        console.log("Upload finished. " + response.data);
                        file.result = response.data;
                        vm.lastUploadSuccessful = true;
                        vm.currentAttachment = undefined;
                        vm.reloadProject();
                        //});
                    }, function (response) {
                        if (response.status > 0){
                            vm.lastUploadSuccessful = false;
                            console.log("Retrieved response status: " + response);
                            RemoteFormValidation.applyServerErrorResponse(vm, vm.attachments_form, response);
                            vm.errorMsg = response.data;
                        }
                    }, function (evt) {
                        // Math.min is to fix IE which reports 200% sometimes
                        file.progress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
                    });
                };

                vm.clearErrors = function () {
                    RemoteFormValidation.clearRemoteErrors();
                };

                vm.reloadProject = function () {
                    Project.get(vm.project.id).then(function (resolvedProject){
                        angular.copy(resolvedProject, vm.project);
                    })
                }

            }
        };
    });