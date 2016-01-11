angular.module('crowdsource')

    .controller('StatisticsController', function () {

        var CONST = {
            PAGES: {
                TEST: {
                    label: "Test label",
                    name: "TEST"
                },
                ANOTHER_TEST: {
                    label: "Another test label",
                    name: "ANOTHER_TEST"
                }
            }
        },
        vm = this;

        vm.data = {
            availableOptions: CONST.PAGES
        };
    });