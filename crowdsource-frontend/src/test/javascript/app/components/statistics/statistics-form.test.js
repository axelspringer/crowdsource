describe('statistics form', function () {
    var $rootScope, $compile, scope;
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
                info: undefined
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

});