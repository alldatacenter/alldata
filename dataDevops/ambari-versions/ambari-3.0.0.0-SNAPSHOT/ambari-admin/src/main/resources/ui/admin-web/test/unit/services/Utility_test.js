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

describe('Utility Service', function () {

  var Utility, httpBackend, scope, ctrl, deferred,
    obj = {
      property: 'value'
    };

  beforeEach(function () {
    module('ambariAdminConsole', function ($provide, $routeProvider) {
      $provide.value('$window', {
        localStorage: {
          getItem: function() {return '{}';},
          setItem: function() {}
        },
        location: {}
      });
      $routeProvider.otherwise(function(){return false;});
    });
    inject(function (_Utility_, _$httpBackend_, $rootScope, $controller, _Cluster_, _$q_) {
      Utility = _Utility_;
      httpBackend = _$httpBackend_;
      deferred = _$q_.defer();
      spyOn(_Cluster_, 'getStatus').and.returnValue(deferred.promise);
      deferred.resolve({
        Clusters: {
          provisioning_state: 'INIT'
        }
      });
      scope = $rootScope.$new();
      scope.$apply();
      ctrl = $controller('AppCtrl', {
        $scope: scope
      });
      httpBackend.whenGET(/\/persist\/user-pref-.*/).respond(200, {});
      httpBackend.whenGET(/api\/v1\/services\/AMBARI\/components\/AMBARI_SERVER.+/).respond(200, {
        RootServiceComponents: {
          component_version: 2.2,
          properties: {
            'user.inactivity.timeout.default': 20
          }
        }
      });
      httpBackend.whenGET(/\/api\/v1\/views.+/).respond(200, {
        items: []
      });
      httpBackend.whenGET("views/clusters/clusterInformation.html").respond(200, {});
    });
  });

  describe('#getUserPref', function () {

    var mock = {
      callback: angular.noop
    };

    beforeEach(function () {
      spyOn(mock, 'callback');
      httpBackend.whenGET(/api\/v1\/persist\/key/).respond(200, obj);
      httpBackend.whenGET(/\/api\/v1\/users\/.*\/authorizations.*/).respond(200, {
        items: []
      });
      Utility.getUserPref('key').then(mock.callback);
      httpBackend.flush();
    });

    it('should pass the received value', function () {
      expect(mock.callback.calls.mostRecent().args[0].data).toEqual(obj);
    });

  });

  describe('#postUserPref', function () {

    var mock = {
        successCallback: angular.noop,
        errorCallback: angular.noop
      },
      cases = [
        {
          status: 200,
          items: [],
          successCallbackCallCount: 0,
          errorCallbackCallCount: 1,
          title: 'user can\'t persist data'
        },
        {
          status: 200,
          items: [
            {
              AuthorizationInfo: {
                authorization_id: 'CLUSTER.MANAGE_USER_PERSISTED_DATA'
              }
            }
          ],
          successCallbackCallCount: 1,
          errorCallbackCallCount: 0,
          title: 'user can persist data, POST request succeeds'
        },
        {
          status: 500,
          items: [
            {
              AuthorizationInfo: {
                authorization_id: 'CLUSTER.MANAGE_USER_PERSISTED_DATA'
              }
            }
          ],
          successCallbackCallCount: 0,
          errorCallbackCallCount: 1,
          title: 'user can persist data, POST request fails'
        }
      ];

    angular.forEach(cases, function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          httpBackend.whenGET(/\/api\/v1\/users\/.*\/authorizations.*/).respond(200, {
            items: item.items
          });
          httpBackend.whenPOST(/api\/v1\/persist/).respond(item.status);
          spyOn(mock, 'successCallback');
          spyOn(mock, 'errorCallback');
          Utility.postUserPref('key', obj).then(mock.successCallback, mock.errorCallback);
          httpBackend.flush();
        });

        it('success callback', function () {
          expect(mock.successCallback.calls.count()).toEqual(item.successCallbackCallCount);
        });

        it('error callback', function () {
          expect(mock.errorCallback.calls.count()).toEqual(item.errorCallbackCallCount);
        });

      });

    });

  });

});