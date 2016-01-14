describe('statistics form', function () {
    var $rootScope, $compile, scope, $httpBackend;
    var compiledDirective;

    beforeEach(function () {
        module('crowdsource');
        module('crowdsource.templates');
        module(function (_$analyticsProvider_) {
            _$analyticsProvider_.virtualPageviews(false);
            _$analyticsProvider_.firstPageview(false);
            _$analyticsProvider_.developerMode(true);
        });

        inject(function (_$compile_, _$rootScope_, _$httpBackend_) {
            $compile = _$compile_;
            $rootScope = _$rootScope_;
            $httpBackend = _$httpBackend_;

            scope = $rootScope.$new();

            scope['data'] = {
                info: undefined,
                availablePageOptions: {
                    CURRENT: {
                        label: "Anzahl Neuregistrieung / Neu eingereichte Ideen",
                        name: "CURRENT"
                    }

                }
            };
        });
    });

    it("should clear info when clear info button clicked.", function () {
        givenCompiledDirective();
        givenInfoShows("test");
        scope.$digest();

        // before click
        expect(compiledDirective.find('.alert-box__statistics')).toExist();

        givenClickOnClose();

        // after click
        expect(compiledDirective.find('.alert-box__statistics')).not.toExist();
    });

    it("should show data pickers when CURRENT statistic type selected", function () {
        givenCompiledDirective();
        scope.$digest();
        expect(compiledDirective.find(".statistics-form-current")).not.toExist();

        whenStatisticTypeSelected(compiledDirective.isolateScope().data.availablePageOptions.CURRENT);
        scope.$digest();

        expect(compiledDirective.find(".statistics-form-current")).toExist();
    });

    it("should call backend when CURRENT statistic type selected");

    function givenCompiledDirective() {
        compiledDirective = $compile('<statistics-form data="data"></statistics-form>')(scope);
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
    };

    function whenStartDateChanged(newDate) {
        compiledDirective.isolateScope().data.startDate = newDate;
    }

});