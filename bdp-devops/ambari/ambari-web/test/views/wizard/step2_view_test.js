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
require('views/wizard/step2_view');

var view, controller = Em.Object.create({
  clusterNameError: ''
});

function getView() {
  return App.WizardStep2View.create({'controller': controller});
}

describe('App.WizardStep0View', function () {

  beforeEach(function() {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'sshKeyState', 'controller.content.installOptions.manualInstall', 'string');

  describe('#didInsertElement', function() {
    beforeEach(function () {
      sinon.stub(App, 'popover', Em.K);
      sinon.stub(App, 'tooltip', Em.K);
      view.set('controller.hostsError', 'some text');
      view.set('controller.sshKeyError', 'some text');
    });
    afterEach(function () {
      App.popover.restore();
      App.tooltip.restore();
    });
    it('should clean hostsError', function () {
      view.didInsertElement();
      expect(view.get('controller.hostsError')).to.be.null;
    });
    it('should clean sshKeyError', function () {
      view.didInsertElement();
      expect(view.get('controller.sshKeyError')).to.be.null;
    });
    it('should create popover', function () {
      view.didInsertElement();
      expect(App.popover.calledOnce).to.equal(true);
    });
    it('should create tooltip', function () {
      view.didInsertElement();
      expect(App.tooltip.calledOnce).to.equal(true);
    });
  });

  describe('#providingSSHKeyRadioButton', function() {
    var v;

    beforeEach(function() {
      v = view.get('providingSSHKeyRadioButton').create({
        controller: Em.Object.create({
          content: {
            installOptions: {
              useSsh: true,
              manualInstall: true
            }
          }
        })
      });
    });

    describe('#checked', function() {
      it('should be equal to controller.content.installOptions.useSsh', function () {
        v.set('controller.content.installOptions.useSsh', false);
        expect(v.get('checked')).to.equal(false);
        v.set('controller.content.installOptions.useSsh', true);
        expect(v.get('checked')).to.equal(true);
      });
    });

    describe('#click', function() {
      it('should update controller.content.installOptions.useSsh', function () {
        v.set('controller.content.installOptions.useSsh', false);
        v.click();
        expect(v.get('controller.content.installOptions.useSsh')).to.equal(true);
      });
      it('should update controller.content.installOptions.manualInstall', function () {
        v.set('controller.content.installOptions.manualInstall', true);
        v.click();
        expect(v.get('controller.content.installOptions.manualInstall')).to.equal(false);
      });
    });

  });

  describe('#manualRegistrationRadioButton', function() {
    var v;

    beforeEach(function() {
      v = view.get('manualRegistrationRadioButton').create({
        controller: Em.Object.create({
          content: {
            installOptions: {
              useSsh: true,
              manualInstall: true
            }
          }
        })
      });
    });

    describe('#checked', function() {
      it('should be equal to controller.content.installOptions.manualInstall', function () {
        v.set('controller.content.installOptions.manualInstall', false);
        expect(v.get('checked')).to.equal(false);
        v.set('controller.content.installOptions.manualInstall', true);
        expect(v.get('checked')).to.equal(true);
      });
    });

    describe('#click', function() {
      it('should update controller.content.installOptions.useSsh', function () {
        v.set('controller.content.installOptions.useSsh', true);
        v.click();
        expect(v.get('controller.content.installOptions.useSsh')).to.equal(false);
      });
      it('should update controller.content.installOptions.manualInstall', function () {
        v.set('controller.content.installOptions.manualInstall', false);
        v.click();
        expect(v.get('controller.content.installOptions.manualInstall')).to.equal(true);
      });
    });

  });

  describe('#textFieldView', function() {
    var v;

    beforeEach(function() {
      v = view.get('textFieldView').create();
    });

    describe('#disabled', function() {
      it('should be inverted to isEnabled', function () {
        v.set('isEnabled', false);
        expect(v.get('disabled')).to.equal(true);
        v.set('isEnabled', true);
        expect(v.get('disabled')).to.equal(false);
      });
    });

  });

});