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

var modelSetup = require('test/init_model_test');
require('models/user');

var user,
  userData = {
    id: 'user'
  };

function getUser() {
  return App.User.createRecord(userData);
}

describe('App.User', function () {

  beforeEach(function () {
    user = getUser();
  });

  afterEach(function () {
    modelSetup.deleteRecord(user);
  });

  App.TestAliases.testAsComputedAlias(getUser(), 'id', 'userName', 'string');

  describe('#id', function () {
    it('should take value from userName', function () {
      user.set('userName', 'name');
      expect(user.get('id')).to.equal('name');
    });
  });

  describe('#isLdap', function() {
    it('User userType value is "LDAP" should return "true"', function() {
      user.set('userType', 'LDAP');
      expect(user.get('isLdap')).to.be.true;
    });
    it('User userType value is "LOCAL" should return "false"', function() {
      user.set('userType', 'LOCAL');
      expect(user.get('isLdap')).to.be.false;
    });
  });
});
