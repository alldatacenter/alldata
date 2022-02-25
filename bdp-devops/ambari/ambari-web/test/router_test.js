/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
var testHelpers = require('test/helpers');

require('router');

describe('App.Router', function () {
  var router = App.Router.create();

  describe('#initAdmin()', function () {

    var cases = [
      {
        user: {
          admin: true
        },
        isAdmin: true,
        isOperator: false,
        isPermissionDataLoaded: true,
        title: 'admin'
      },
      {
        user: {
          operator: true
        },
        isAdmin: false,
        isOperator: true,
        isPermissionDataLoaded: true,
        title: 'operator'
      },
      {
        user: {},
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: true,
        title: 'read only access'
      },
      {
        user: null,
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false,
        title: 'no user'
      }
    ];

    beforeEach(function () {
      this.getUser = sinon.stub(App.db, 'getUser');
      App.setProperties({
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false
      });
    });

    afterEach(function () {
      this.getUser.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        this.getUser.returns(item.user);
        router.initAdmin();
        expect(App.get('isAdmin')).to.equal(item.isAdmin);
        expect(App.get('isOperator')).to.equal(item.isOperator);
        expect(App.get('isPermissionDataLoaded')).to.equal(item.isPermissionDataLoaded);
      });
    });

  });

  describe('#adminViewInfoSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(window.location, 'replace', Em.K);
    });
    afterEach(function () {
      window.location.replace.restore();
    });

    var tests = [{
      mockData: {
        components: [{
          'RootServiceComponents': {
            'component_version': '1.9.0'
          }
        }, {
          'RootServiceComponents': {
            'component_version': '2.0.0_MyBuild'
          }
        }]
      },
      expected: '/views/ADMIN_VIEW/2.0.0/INSTANCE/#/'
    }, {
      mockData: {
        components: [{
          'RootServiceComponents': {
            'component_version': '1.9.0'
          }
        }, {
          'RootServiceComponents': {
            'component_version': '2.1.0'
          }
        }, {
          'RootServiceComponents': {
            'component_version': '2.0.0'
          }
        }]
      },
      expected: '/views/ADMIN_VIEW/2.1.0/INSTANCE/#/'
    }, {
      mockData: {
        components: [{
          'RootServiceComponents': {
            component_version: '2.1.0_MyBuild'
          }
        }]
      },
      expected: '/views/ADMIN_VIEW/2.1.0/INSTANCE/#/'
    }];

    tests.forEach(function (data, index) {
      it('should redirect to the latest version of admin view ("' + data.expected + '") #' + (index + 1), function () {
        router.adminViewInfoSuccessCallback(data.mockData);
        expect(window.location.replace.calledWith(data.expected)).to.be.true;
      });
    });
  });

  describe.skip("#savePreferedPath()", function () {
    beforeEach(function () {
      router.set('preferedPath', null);
    });
    it("has no key", function () {
      router.savePreferedPath('path');
      expect(router.get('preferedPath')).to.equal('path');
    });
    it("path does not contain key", function () {
      router.savePreferedPath('path', 'key');
      expect(router.get('preferedPath')).to.be.null;
    });
    it("path contains key", function () {
      router.savePreferedPath('key=path', 'key=');
      expect(router.get('preferedPath')).to.equal('path');
    });
  });

  describe.skip("#restorePreferedPath()", function () {
    it("preferedPath is null", function () {
      router.set('preferedPath', null);
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '/relativeURL'", function () {
      router.set('preferedPath', '/relativeURL');
      expect(router.restorePreferedPath()).to.be.true;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '#/relativeURL'", function () {
      router.set('preferedPath', '#/relativeURL');
      expect(router.restorePreferedPath()).to.be.true;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '#/login'", function () {
      router.set('preferedPath', '#/login');
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is 'http://absoluteURL'", function () {
      router.set('preferedPath', 'http://absoluteURL');
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
  });

  describe.skip("#loginGetClustersSuccessCallback()", function () {
    var mock = {dataLoading: Em.K};
    beforeEach(function () {
      sinon.stub(router, 'setClusterInstalled', Em.K);
      sinon.stub(router, 'transitionToApp', Em.K);
      sinon.stub(router, 'transitionToViews', Em.K);
      sinon.stub(router, 'transitionToAdminView', Em.K);
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'dataLoading');
      App.setProperties({
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false
      });
    });
    afterEach(function () {
      router.setClusterInstalled.restore();
      router.transitionToApp.restore();
      router.transitionToViews.restore();
      router.transitionToAdminView.restore();
      App.router.get.restore();
      mock.dataLoading.restore();
    });
    it("cluster exists, OPERATOR privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'CLUSTER.ADMINISTRATOR'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.true;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });

    it("cluster exists, READ privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'CLUSTER.USER'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster exists, ADMIN privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'AMBARI.ADMINISTRATOR'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster exists, no privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: []
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToViews.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster not installed, ADMIN privileges", function () {
      var clusterData = {
        items: []
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'AMBARI.ADMINISTRATOR'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.transitionToAdminView.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster not installed, non-admin privileges", function () {
      var clusterData = {
        items: []
      };
      var params = {
        loginData: {
          privileges: []
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.transitionToViews.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
  });

  describe("#transitionToAdminView()", function () {

    it("valid request is sent", function () {
      router.transitionToAdminView();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service.load_server_version');
      expect(args[0]).to.exists;
    });
  });

  describe("#transitionToApp()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(router, 'restorePreferedPath');
      sinon.stub(router, 'getSection', function (callback) {
        callback('route');
      });
      sinon.stub(router, 'transitionTo');
    });
    afterEach(function () {
      this.mock.restore();
      router.getSection.restore();
      router.transitionTo.restore();
    });
    it("has restore path", function () {
      this.mock.returns(true);
      router.transitionToApp();
      expect(router.getSection.called).to.be.false;
      expect(router.transitionTo.called).to.be.false;
    });
    it("does not have restore path", function () {
      this.mock.returns(false);
      router.transitionToApp();
      expect(router.getSection.calledOnce).to.be.true;
      expect(router.transitionTo.calledWith('route')).to.be.true;
    });
  });

  describe("#transitionToViews()", function () {
    var mock = {loadAmbariViews: Em.K};
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(router, 'transitionTo');
      sinon.spy(mock, 'loadAmbariViews');
    });
    afterEach(function () {
      App.router.get.restore();
      router.transitionTo.restore();
      mock.loadAmbariViews.restore();
    });
    it("transitionTo called with corrent route", function () {
      router.transitionToViews();
      expect(mock.loadAmbariViews.calledOnce).to.be.true;
      expect(router.transitionTo.calledWith('main.views.index')).to.be.true;
    });
  });

  describe("#adminViewInfoErrorCallback()", function () {
    beforeEach(function () {
      sinon.stub(router, 'transitionToViews');
    });
    afterEach(function () {
      router.transitionToViews.restore();
    });
    it("transitionToViews called once", function () {
      router.adminViewInfoErrorCallback();
      expect(router.transitionToViews.calledOnce).to.be.true;
    });
  });

  describe("#getAuthenticated", function () {
    beforeEach(function () {
      router = App.Router.create();
      this.mockGetCurrentLocationUrl = sinon.stub(router, 'getCurrentLocationUrl');
      sinon.stub(router, 'redirectByURL', Em.K);
    });

    afterEach(function () {
      router.getCurrentLocationUrl.restore();
      router.redirectByURL.restore();
      this.mockGetCurrentLocationUrl.restore();
    });

    [
      {
        lastSetURL: '/login/local',
        isResolved: false,
        responseData: {
          responseText: "",
          status: 403
        },
        redirectCalled: false,
        m: 'no jwtProviderUrl in auth response, no redirect'
      },
      {
        lastSetURL: '/main/dashboard',
        isResolved: false,
        responseData: {
          responseText: JSON.stringify({jwtProviderUrl: 'http://some.com?originalUrl='}),
          status: 403
        },
        redirectCalled: true,
        m: 'jwtProviderUrl is present, current location not local login url, redirect according to jwtProviderUrl value'
      },
      {
        lastSetURL: '/login/local',
        isResolved: false,
        responseData: {
          responseText: JSON.stringify({jwtProviderUrl: 'http://some.com?originalUrl='}),
          status: 403
        },
        redirectCalled: false,
        m: 'jwtProviderUrl is present, current location is local login url, no redirect'
      },
      {
        lastSetURL: '/main/dashboard',
        isResolved: false,
        responseData: {
          responseText: JSON.stringify({jwtProviderUrl: 'http://some.com?originalUrl='}),
          status: 401
        },
        redirectCalled: true,
        m: 'jwtProviderUrl is present, current location not local login url, redirect according to jwtProviderUrl value'
      },
      {
        lastSetURL: '/login/local',
        isResolved: false,
        responseData: {
          responseText: JSON.stringify({jwtProviderUrl: 'http://some.com?originalUrl='}),
          status: 401
        },
        redirectCalled: false,
        m: 'jwtProviderUrl is present, current location is local login url, no redirect'
      }
    ].forEach(function (test) {
      describe(test.m, function () {
        var mockCurrentUrl;
        beforeEach(function () {
          mockCurrentUrl = 'http://localhost:3333/#/some/hash';
          router.set('location.lastSetURL', test.lastSetURL);
          App.ajax.send.restore(); // default ajax-mock can't be used here
          sinon.stub(App.ajax, 'send', function () {
            if (!test.isResolved) {
              router.onAuthenticationError(test.responseData);
            }
            return {
              complete: function () {
              }
            };
          });
          this.mockGetCurrentLocationUrl.returns(mockCurrentUrl);
          router.getAuthenticated();
        });

        it('redirectByURL is ' + (test.redirectCalled ? '' : 'not') + ' called', function () {
          expect(router.redirectByURL.calledOnce).to.be.eql(test.redirectCalled);
        });


        if (test.redirectCalled) {
          it('redirectByURL is correct', function () {
            expect(router.redirectByURL.args[0][0]).to.be.eql(JSON.parse(test.responseData.responseText).jwtProviderUrl + encodeURIComponent(mockCurrentUrl));
          });
        }
      });
    });

  });

  describe('#setClusterData', function () {

    var data = {
        loginName: 'user',
        loginData: {
          PrivilegeInfo: {}
        }
      },
      clusterData = {
        items: []
      },
      cases = [
        {
          clusterData: clusterData,
          callbackCallCount: 1,
          isAjaxCalled: false,
          title: 'cluster data available'
        },
        {
          clusterData: null,
          callbackCallCount: 0,
          isAjaxCalled: true,
          title: 'no cluster data'
        }
      ];

    beforeEach(function () {
      sinon.stub(router, 'loginGetClustersSuccessCallback', Em.K);
    });

    afterEach(function () {
      router.loginGetClustersSuccessCallback.restore();
    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        var ajaxCallArgs;

        beforeEach(function () {
          router.set('clusterData', item.clusterData);
          router.setClusterData({}, {}, data);
          ajaxCallArgs = testHelpers.findAjaxRequest('name', 'router.login.clusters');
        });

        it('loginGetClustersSuccessCallback', function () {
          expect(router.loginGetClustersSuccessCallback.callCount).to.equal(item.callbackCallCount);
        });

        if (item.isAjaxCalled) {
          it('App.ajax.send is called', function () {
            expect(ajaxCallArgs).to.have.length(1);
          });
          it('data for AJAX request', function () {
            expect(ajaxCallArgs).to.eql([
              {
                name: 'router.login.clusters',
                sender: router,
                data: data,
                success: 'loginGetClustersSuccessCallback'
              }
            ]);
          });
        } else {
          it('App.ajax.send is not called', function () {
            expect(ajaxCallArgs).to.be.undefined;
          });
          it('arguments for callback', function () {
            expect(router.loginGetClustersSuccessCallback.firstCall.args).to.eql([clusterData, {}, data]);
          });
        }

      });

    });

  });

});

describe('App.StepRoute', function () {

  beforeEach(function () {
    this.route = App.StepRoute.create();
    this.nextTransitionSpy = sinon.spy(this.route, 'nextTransition');
    this.backTransitionSpy = sinon.spy(this.route, 'backTransition');
    this.appGetStub = sinon.stub(App, 'get');
    this.appSetStub = sinon.stub(App, 'set');
    this.runNextStub = sinon.stub(Em.run, 'next', Em.clb);
  });

  afterEach(function () {
    this.nextTransitionSpy.restore();
    this.backTransitionSpy.restore();
    this.appGetStub.restore();
    this.appSetStub.restore();
    this.runNextStub.restore();
  });

  describe('#back', function () {

    [
      {
        btnClickInProgress: true,
        backBtnClickInProgressIsSet: false,
        backTransitionIsCalled: false,
        m: 'backTransition is not called'
      },
      {
        btnClickInProgress: false,
        backBtnClickInProgressIsSet: true,
        backTransitionIsCalled: true,
        m: 'backTransition is called'
      }
    ].forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          this.appGetStub.withArgs('router.btnClickInProgress').returns(test.btnClickInProgress);
          this.route.back({});
        });

        it('backTransition call', function () {
          expect(this.backTransitionSpy.called).to.be.equal(test.backTransitionIsCalled);
        });

        it('backBtnClickInProgress is set', function () {
          expect(this.appSetStub.calledWith('router.backBtnClickInProgress')).to.be.equal(test.backBtnClickInProgressIsSet);
        });

      });
    });

  });

  describe('#next', function () {

    [
      {
        btnClickInProgress: true,
        nextBtnClickInProgressIsSet: false,
        nextTransitionIsCalled: false,
        m: 'nextTransition is not called'
      },
      {
        btnClickInProgress: false,
        nextBtnClickInProgressIsSet: true,
        nextTransitionIsCalled: true,
        m: 'nextTransition is called'
      }
    ].forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          this.appGetStub.withArgs('router.btnClickInProgress').returns(test.btnClickInProgress);
          this.route.next({});
        });

        it('nextTransition call', function () {
          expect(this.nextTransitionSpy.called).to.be.equal(test.nextTransitionIsCalled);
        });

        it('nextBtnClickInProgress is set', function () {
          expect(this.appSetStub.calledWith('router.nextBtnClickInProgress')).to.be.equal(test.nextBtnClickInProgressIsSet);
        });

      });
    });
  });

});