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
require('models/cluster');
require('controllers/wizard');
require('controllers/installer');

describe('App.LoginController', function () {

  var loginController = App.LoginController.create();

  describe('#postLogin', function() {
    it ('Should set error connect', function() {
      loginController.postLogin(false, false, null);
      expect(loginController.get('errorMessage')).to.be.equal(Em.I18n.t('login.error.bad.connection'));
    });
    it ('Should set error connect with specific message', function() {
      loginController.postLogin(false, false, 'specific message');
      expect(loginController.get('errorMessage')).to.be.equal('specific message');
    });
    it ('Should set error user is disabled', function() {
      loginController.postLogin(true, false, 'User is disabled');
      expect(loginController.get('errorMessage')).to.be.equal(Em.I18n.t('login.error.disabled'));
    });
    it ('Should set bad credentials error', function() {
      loginController.postLogin(true, false, 'Authentication required');
      expect(loginController.get('errorMessage')).to.be.equal(Em.I18n.t('login.error.bad.credentials'));
    });
    it ('Should set bad credentials error, empty response', function() {
      loginController.postLogin(true, false, null);
      expect(loginController.get('errorMessage')).to.be.equal(Em.I18n.t('login.error.bad.credentials'));
    });
    it ('Should set custom error', function() {
      loginController.postLogin(true, false, 'Login Failed: Please append your domain to your username and try again.  Example: user_dup@domain');
      expect(loginController.get('errorMessage')).to.be.equal('Login Failed: Please append your domain to your username and try again.  Example: user_dup@domain');
    });
    it ('isSubmitDisabled should be false', function() {
      loginController.postLogin(true, true);
      expect(loginController.get('isSubmitDisabled')).to.be.false;
    });
  });

});
