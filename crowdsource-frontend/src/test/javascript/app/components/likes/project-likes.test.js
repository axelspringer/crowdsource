describe('project likes', function () {
    var $rootScope, $compile, scope, $httpBackend;
    var likesDirectiveCompiled;

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

            scope['project'] = {
                id: "testProjectId",
                likeCount: 0,
                likeStatus: 'UNLIKE'
            };
        });

        givenCompiledDirective();
    });

    it("should show like counts with zero when not data provided on list page", function () {

        givenView("list");
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes').hasClass('project_likes_list')).toBe(true);
        expect(likesDirectiveCompiled.find('.project_likes_list_count')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('0');
    });

    it("should show like counts with zero when not data provided on detail page", function () {

        givenView("detail");
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes').hasClass('project_likes_detail')).toBe(true);
        expect(likesDirectiveCompiled.find('.project_likes_detail_count')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes_detail_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes_detail_count').text()).toBe('This project is 0 times liked!');
        expect(likesDirectiveCompiled.find('.project_likes_detail_status').text().trim()).toBe('Like');
    });

    it("should show like counts", function () {
        givenView("list");
        givenLikeCount(100);
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('100');
    });

    it("should show like status of user when no data provided on list page", function () {
        givenView("list");
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
    });

    //todo
    //it("should show like status of when no data provided on detail page", function () {
    //    givenView("detail");
    //    scope.$digest();
    //
    //    expect(likesDirectiveCompiled.find('.project_likes_detail_status')).toExist();
    //    expect(likesDirectiveCompiled.find('.project_likes_detail_status').text().trim()).toBe('Like');
    //});

    it("should show status of user after liked project on list page", function () {
        givenView("list");
        givenLikeStatus("LIKE");
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(true);
    });

    // todo like status test on detail page

    it("should show status of user after unliked project on list page", function () {
        givenView("list");

        givenLikeStatus("LIKE");
        scope.$digest();

        givenLikeStatus("UNLIKE");
        scope.$digest();

        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(false);
    });

    // todo unlike status test on detail page


    it("should be able to execute like by user on list page", function () {
        givenView("list");
        scope.$digest();

        // before click
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(false);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('0');

        givenLikeClicked();
        scope.$digest();

        // before server responded
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(true);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('1');

        var responseProject = angular.copy(likesDirectiveCompiled.isolateScope().project);
        // honors response from server
        responseProject.likeCount = 100;
        $httpBackend.expectGET('/project/' + likesDirectiveCompiled.isolateScope().project.id).respond(200, responseProject);
        $httpBackend.flush();

        //after server responded
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(true);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('100');
    });



    it("should be able to execute unlike by user on list page", function () {
        var initCount = 5;

        givenView("list");
        givenLikeCount(initCount);
        givenLikeStatus('LIKE');

        scope.$digest();

        // before click
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(true);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe("" + initCount);

        givenUnlikeClicked();
        scope.$digest();

        // before server responded
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(false);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('4');

        var responseProject = angular.copy(likesDirectiveCompiled.isolateScope().project);
        // honors response from server
        responseProject.likeCount = 100;
        $httpBackend.expectGET('/project/' + likesDirectiveCompiled.isolateScope().project.id).respond(200, responseProject);
        $httpBackend.flush();

        //after server responded
        expect(likesDirectiveCompiled.find('.project_likes_list_status')).toExist();
        expect(likesDirectiveCompiled.find('.project_likes').hasClass('liked')).toBe(false);
        expect(likesDirectiveCompiled.find('.project_likes_list_count').text()).toBe('100');
    });

    function givenCompiledDirective() {
        likesDirectiveCompiled = $compile('<project-likes project="project", view="list"></project-likes>')(scope);
        scope.$digest();
    };

    function givenView(view) {
        likesDirectiveCompiled.isolateScope().view = view;
    }

    function givenLikeCount(count) {
        likesDirectiveCompiled.isolateScope().project.likeCount = count;
    }

    function  givenLikeStatus(status) {
        likesDirectiveCompiled.isolateScope().project.likeStatus = status;
    }

    function givenLikeClicked() {
        $httpBackend.expectPOST('/projects/' + likesDirectiveCompiled.isolateScope().project.id + '/like').respond(200);
        likesDirectiveCompiled.find('.project_likes_list_status').click();
    }

    function givenUnlikeClicked() {
        $httpBackend.expectPOST('/projects/' + likesDirectiveCompiled.isolateScope().project.id + '/unlike').respond(200);
        likesDirectiveCompiled.find('.project_likes_list_status').click();
    }
});