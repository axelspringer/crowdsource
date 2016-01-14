angular.module('crowdsource')
    .controller('StatisticsController', function () {

        var vm = this,

        CONST = {
            PAGES: {
                NONE: {
                    label: "-- Bitte auswählen --",
                    name: "NONE"
                },
                CURRENT: {
                    label: "Anzahl Neuregistrieung / Neu eingereichte Ideen",
                    name: "CURRENT"
                }

            },

            TIME_PRECISION_TYPE: {
                DAY: {
                    id: "DAY",
                    label: "Tag",
                    precision: 1
                },
                WEEK: {
                    id: "WEEK",
                    label: "Woche",
                    precision: 7
                },
                MONTH: {
                    id: "MONTH",
                    label: "Monat",
                    precision: 30
                }
            }
        };

        vm.data = {
            availablePageOptions: CONST.PAGES,
            availableTimePrecisionTypeOptions: CONST.TIME_PRECISION_TYPE,
            timePrecision: CONST.TIME_PRECISION_TYPE.DAY,
            statisticType: CONST.PAGES.NONE
        };
    });