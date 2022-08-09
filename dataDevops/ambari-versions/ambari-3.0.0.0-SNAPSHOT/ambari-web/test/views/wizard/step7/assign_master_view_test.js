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
require('views/wizard/step9/hostLogPopupBody_view');
var stringUtils = require('utils/string_utils');
var view;

function getView() {
  return App.AssignMasterOnStep7View.create({
    controller: Em.Object.create()
  });
}

describe('App.AssignMasterOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });

  describe("#willInsertElement()", function () {

    beforeEach(function () {
      sinon.stub(view, 'setAlertMessage');
    });

    afterEach(function () {
      view.setAlertMessage.restore();
    });

    it("setAlertMessage should be called", function () {
      view.willInsertElement();
      expect(view.setAlertMessage.calledOnce).to.be.true;
    });
  });

  describe("#getDependentComponents()", function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        dependencies: [{
          scope: 'host',
          componentName: 'C1'
        }]
      }));
      sinon.stub(App.format, 'role', function (arg) {
        return arg;
      });
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
      App.format.role.restore();
    });

    it("should return dependent components", function () {
      expect(view.getDependentComponents([{}])).to.be.eql(['C1']);
    });
  });

  describe("#setAlertMessage()", function () {

    beforeEach(function () {
      sinon.stub(App.format, 'role', function (arg) {
        return arg;
      });
      sinon.stub(view, 'getDependentComponents').returns(['c1']);
      sinon.stub(stringUtils, 'getFormattedStringFromArray').returns('');
      this.mock = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.format.role.restore();
      view.getDependentComponents.restore();
      stringUtils.getFormattedStringFromArray.restore();
      this.mock.restore();
    });

    it("isManualKerberos false, single master", function () {

      var expected = [
        Em.I18n.t('installer.step7.assign.master.body').format('c1', Em.I18n.t('common.host').toLowerCase(), Em.I18n.t('it')),
        Em.I18n.t('installer.step7.assign.master.dependent.component.body').format('')
      ].join('<br/>');

      view.set('controller.mastersToCreate', ['c1']);
      this.mock.returns(false);

      view.setAlertMessage();
      expect(view.get('alertMessage')).to.be.equal(expected);
    });

    it("isManualKerberos false, multiple masters", function () {

      var expected = [
        Em.I18n.t('installer.step7.assign.master.body').format('c1,c2', Em.I18n.t('common.hosts').toLowerCase(), Em.I18n.t('then')),
        Em.I18n.t('installer.step7.assign.master.dependent.component.body').format('')
      ].join('<br/>');

      view.set('controller.mastersToCreate', ['c1', 'c2']);
      this.mock.returns(false);

      view.setAlertMessage();
      expect(view.get('alertMessage')).to.be.equal(expected);
    });

    it("isManualKerberos true, single master", function () {

      var expected = [
        Em.I18n.t('installer.step7.assign.master.body').format('c1', Em.I18n.t('common.host').toLowerCase(), Em.I18n.t('it')),
        Em.I18n.t('installer.step7.assign.master.dependent.component.body').format(''),
        Em.I18n.t('common.warn.message').format(Em.I18n.t('common.important.strong') + ': ' + Em.I18n.t('installer.step8.kerberors.warning'))
      ].join('<br/>');

      view.set('controller.mastersToCreate', ['c1']);
      this.mock.returns(true);

      view.setAlertMessage();
      expect(view.get('alertMessage')).to.be.equal(expected);
    });
  });

});