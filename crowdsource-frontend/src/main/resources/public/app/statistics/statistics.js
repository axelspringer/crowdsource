// Expose statistics constants
var module = angular.module('crowdsource');

module.constant ("STATISTICS_CONST", {
    TIME_PRECISION_TYPE: {
        DAY: {
            id: "DAY",
            label: "Tag",
            precision: 1
        }
        //,
        //WEEK: {
        //    id: "WEEK",
        //    label: "Woche",
        //    precision: 7
        //},
        //MONTH: {
        //    id: "MONTH",
        //    label: "Monat",
        //    precision: 30
        //}
    },
    PAGES: {
        NONE: {
            label: "-- Bitte auswählen --",
                name: "NONE"
        },
        CURRENT: {
            label: "Anzahl Neuregistrieung / Neu eingereichte Ideen",
                name: "CURRENT"
        },
        PROJECT_SUM_PER_STATUS: {
            label: "Projekte je Projektstatus",
                name: "PROJECT_SUM_PER_STATUS"
        }

    }
});

module.controller('StatisticsController', function (STATISTICS_CONST) {
        var vm = this;

        vm.data = {
            availablePageOptions: STATISTICS_CONST.PAGES,
            availableTimePrecisionTypeOptions: STATISTICS_CONST.TIME_PRECISION_TYPE,
            timePrecision: STATISTICS_CONST.TIME_PRECISION_TYPE.DAY,
            statisticType: STATISTICS_CONST.PAGES.NONE
        };
    });