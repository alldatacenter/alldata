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
  describe('StackVersionsCreateCtrl', function() {
    var scope, ctrl;

    beforeEach(module('ambariAdminConsole', function($provide) {}));
    beforeEach(inject(function($rootScope, $controller) {
      scope = $rootScope.$new();
      ctrl = $controller('StackVersionsCreateCtrl', {$scope: scope});
    }));

    describe('Test repository subversion input validation', function () {
      it('1 digit', function() {
        var input = "1";
        var input2 = "11";
        var regex = /^\d+\.\d+(-\d+)?$/;
        expect(regex.test(input)).toBe(false);
        expect(regex.test(input2)).toBe(false);
      });

      it('1 digit dot 1 digit', function() {
        var input = "1.2";
        var input2 = "1.22";
        var regex = /^\d+\.\d+(-\d+)?$/;
        expect(regex.test(input)).toBe(true);
        expect(regex.test(input2)).toBe(true);
      });

      it('1 digit dot 1 digit dash 4 digits', function() {
        var input = "1.2-3456";
        var input2 = "1000.1000-12345";
        var input3 = "1.1-123";
        var invalidInput = "1.1-abcd";
        var invalidInput2 = "1.2.3";
        var regex = /^\d+\.\d+(-\d+)?$/;
        expect(regex.test(input)).toBe(true);
        expect(regex.test(input2)).toBe(true);
        expect(regex.test(input3)).toBe(true);
        expect(regex.test(invalidInput)).toBe(false);
        expect(regex.test(invalidInput2)).toBe(false);
      });
    });
  });
});
