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
require('controllers/global/wizard_watcher_controller');

var controller;

describe('App.wizardWatcherController', function () {
  beforeEach(function() {
    controller = App.WizardWatcherController.create();
  });

  describe("#isWizardRunning", function() {
    it("wizardUser is null", function() {
      controller.set('wizardUser', null);
      controller.propertyDidChange('isWizardRunning');
      expect(controller.get('isWizardRunning')).to.be.false;
    });
    it("wizardUser is correct", function() {
      controller.set('wizardUser', 'admin');
      controller.propertyDidChange('isWizardRunning');
      expect(controller.get('isWizardRunning')).to.be.true;
    });
  });

  describe("#wizardDisplayName", function() {
    beforeEach(function () {
      controller.set('wizardUser', 'tdk');
      sinon.stub(App.router, 'get').returns(Em.Object.create({displayName: 'Wizard'}));
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("controllerName is null", function() {
      controller.set('controllerName', null);
      controller.propertyDidChange('wizardDisplayName');
      expect(controller.get('wizardDisplayName')).to.be.empty;
    });
    it("controllerName is correct", function() {
      controller.set('controllerName', 'ctrl1');
      controller.propertyDidChange('wizardDisplayName');
      expect(controller.get('wizardDisplayName')).to.equal(Em.I18n.t('wizard.inProgress').format('Wizard', 'tdk'));
    });
  });


  describe("#isNonWizardUser", function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("isWizardRunning is false", function() {
      controller.reopen({
        isWizardRunning: false
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.false;
    });
    it("isWizardRunning is true, wizardUser is admin", function() {
      controller.setProperties({
        isWizardRunning: true,
        wizardUser: 'admin'
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.false;
    });
    it("isWizardRunning is true, wizardUser is admin2", function() {
      controller.setProperties({
        isWizardRunning: true,
        wizardUser: 'admin2'
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.true;
    });
  });

  describe("#setUser()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'postUserPref', Em.K);
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function () {
      controller.postUserPref.restore();
      App.router.get.restore();
    });
    it("post user pref", function() {
      controller.setUser('ctrl1');
      expect(controller.postUserPref.calledWith(controller.get('PREF_KEY'), {
        userName: 'admin',
        controllerName: 'ctrl1'
      })).to.be.true;
    });
  });

  describe("#resetUser()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'postUserPref', Em.K);
    });
    afterEach(function () {
      controller.postUserPref.restore();
    });
    it("post user pref", function() {
      controller.resetUser('ctrl1');
      expect(controller.postUserPref.calledWith(controller.get('PREF_KEY'), null)).to.be.true;
    });
  });

  describe("#getUser()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'getUserPref', Em.K);
    });
    afterEach(function () {
      controller.getUserPref.restore();
    });
    it("get user pref", function() {
      controller.getUser('ctrl1');
      expect(controller.getUserPref.calledWith(controller.get('PREF_KEY'))).to.be.true;
    });
  });

  describe("#getUserPrefSuccessCallback()", function() {
    it("data is null", function() {
      controller.getUserPrefSuccessCallback(null);
      expect(controller.get('wizardUser')).to.be.null;
      expect(controller.get('controllerName')).to.be.null;
    });
    it("data is correct", function() {
      controller.getUserPrefSuccessCallback({userName: 'admin', controllerName: 'ctrl1'});
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });

  describe("#getUserPrefErrorCallback()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'resetUser', Em.K);
    });
    afterEach(function () {
      controller.resetUser.restore();
    });
    it("reset wizard-data", function() {
      controller.getUserPrefErrorCallback();
      expect(controller.resetUser.calledOnce).to.be.true;
    });
  });
});
