describe('statistics form', function () {
    var $rootScope, $compile, scope, $httpBackend, STATISTICS_CONST, statisticsForm;
    var compiledDirective;

    beforeEach(function () {
        module('crowdsource');
        module('crowdsource.templates');
        module(function (_$analyticsProvider_) {
            _$analyticsProvider_.virtualPageviews(false);
            _$analyticsProvider_.firstPageview(false);
            _$analyticsProvider_.developerMode(true);
        });

        inject(function (_$compile_, _$rootScope_, _$httpBackend_, _STATISTICS_CONST_) {
            $compile = _$compile_;
            $rootScope = _$rootScope_;
            $httpBackend = _$httpBackend_;
            STATISTICS_CONST = _STATISTICS_CONST_;

            scope = $rootScope.$new();

            scope['data'] = {
                info: undefined
            };
        });
    });

    it("should clear info when clear info button clicked.", function () {
        givenCompiledDirective();
        givenInfoShows("test");
        scope.$digest();

        // before click
        expect(statisticsForm.getAlertBox()).toExist();

        givenClickOnClose();

        // after click
        expect(compiledDirective.find('.alert-box__statistics')).not.toExist();
    });

    it("should show data pickers when CURRENT statistic type selected", function () {
        givenCompiledDirective();
        scope.$digest();
        expect(statisticsForm.getSelectsFormForTypeCurrent()).not.toExist();

        $httpBackend.expectGET(/.*?statistics\/current?.*/g).respond(200, [
            {   "name":"Eingereichte Projekte",
                "data":[{"data":0,"label":"2016-01-01"},{"data":0,"label":"2016-01-02"},{"data":2110,"label":"2016-01-15"}]},
            {   "name":"Neuregistrierungen",
                "data":[{"data":0,"label":"2016-01-01"},{"data":0,"label":"2016-01-02"},{"data":0,"label":"2016-01-03"}]}
        ]);

        whenStatisticTypeSelected(STATISTICS_CONST.PAGES.CURRENT);
        scope.$digest();

        expect(statisticsForm.getSelectsFormForTypeCurrent()).toExist();
    });


    it("should not show data pickers when PROJECT_PER_STATUS statistic type selected", function () {
        givenCompiledDirective();
        scope.$digest();
        expect(statisticsForm.getSelectsFormForTypeCurrent()).not.toExist();

        whenStatisticTypeSelected(STATISTICS_CONST.PAGES.PROJECT_SUM_PER_STATUS);
        scope.$digest();


        expect(statisticsForm.getSelectsFormForTypeCurrent()).not.toExist();

    });

    it("should call backend when PROJECT_PER_STATUS statistic type selected", function () {
        givenCompiledDirective();

        scope.$digest();

        $httpBackend.expectGET('/statistics/projects_per_status').respond(200, [{id: "BLA", name: "A_BLA_NAME", count: 2}, {id: "BLA_2", name: "A_BLA_NAME_2", count: 3}]);

        whenStatisticTypeSelected(STATISTICS_CONST.PAGES.PROJECT_SUM_PER_STATUS);

        $httpBackend.flush();
        scope.$digest();

        expect(statisticsForm.getSelectsFormForTypeCurrent()).not.toExist();
        expect(statisticsForm.getSelectedStatisticsType()).toHaveText(STATISTICS_CONST.PAGES.PROJECT_SUM_PER_STATUS.label);
        expect(statisticsForm.getSelectedStatisticsType()).toHaveValue(STATISTICS_CONST.PAGES.PROJECT_SUM_PER_STATUS.name);

    });

    function givenCompiledDirective() {
        compiledDirective = $compile('<statistics-form data="data"></statistics-form>')(scope);
        statisticsForm = new StatisticsForm(compiledDirective);
        scope.$digest();
    };

    function givenInfoShows(infoText) {
        compiledDirective.isolateScope().data.info = infoText;
    };

    function givenClickOnClose() {
        compiledDirective.find('.close').click();
    };

    function whenStatisticTypeSelected(type) {
        compiledDirective.isolateScope().data.statisticType = type;
        compiledDirective.isolateScope().statisticTypeChanged();
    };

    function whenStartDateChanged(newDate) {
        compiledDirective.isolateScope().data.startDate = newDate;
    }

});