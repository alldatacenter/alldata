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

describe('#Cluster', function () {
  describe('StackVersionsListCtrl', function() {
    var scope, ctrl, Stack;

    beforeEach(module('ambariAdminConsole', function($provide) {

    }));

    beforeEach(function () {
      module('ambariAdminConsole');
      inject(function($rootScope, $controller) {
        scope = $rootScope.$new();
        ctrl = $controller('StackVersionsListCtrl', {$scope: scope});
      });
    });

    describe('#fetchRepos()', function () {

      var repos;

      beforeEach(inject(function(_Stack_) {
        Stack = _Stack_;
        spyOn(Stack, 'allRepos').and.returnValue({
          then: function (callback) {
            repos = callback({
              items: [{}, {}]
            });
          }
        });
        repos = [];
        scope.isLoading = true;
        scope.fetchRepos();
      }));

      it('saves list of stacks', function() {
        expect(repos.length).toEqual(2);
      });

      it('isLoading should be set to false', function() {
        expect(scope.isLoading).toBe(false);
      });

    });

  });
});
