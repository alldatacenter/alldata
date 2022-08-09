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
'use strict';
describe('Ambari sign out from Admin view', function () {
  describe('Admin view', function () {
    var ptor = protractor.getInstance();
    beforeEach(function () {
      ptor.get('app/index.html');
      ptor.waitForAngular();
    });
    it('should navigate to login page on clicking "Sign out" action', function () {
      var userDropdownBtn = element(by.binding('currentUser'));
      var signOutAction = element(by.css('[ng-click="signOut()"]'));
      //Action-1: Click on user dropdown menu and
      //Action-2: Click on SignOut action link
      userDropdownBtn.click().then(function () {
        signOutAction.click().then(function () {
          //Validation
          setTimeout(function () {
            expect(ptor.getCurrentUrl()).toContain('#/login');
          }, 3000);
        });
      });
    });
  });
});


