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

var view;

function getView() {
  return App.MainAlertsManageAlertGroupView.create({
    controller: Em.Object.create()
  });
}

describe('App.MainAlertsManageAlertGroupView', function () {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedIfThenElse(getView(), 'removeButtonTooltip', 'controller.isRemoveButtonDisabled', Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeButtonDisabled'), Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeButton'))


  describe("#onGroupSelect()", function() {

    beforeEach(function() {
      view.removeObserver('selectedAlertGroup', view, 'onGroupSelect');
      view.set('controller', Em.Object.create({selectedAlertGroup: null}));
    });

    it("selectedAlertGroup is null", function() {
      view.set('selectedAlertGroup', null);
      view.onGroupSelect();
      expect(view.get('selectedAlertGroup')).to.be.null;
      expect(view.get('controller.selectedAlertGroup')).to.be.null;
      expect(view.get('controller.selectedDefinitions')).to.be.empty;
    });

    it("selectedAlertGroup is empty array", function() {
      view.set('selectedAlertGroup', []);
      view.onGroupSelect();
      expect(view.get('selectedAlertGroup')).to.be.empty;
      expect(view.get('controller.selectedAlertGroup')).to.be.null;
      expect(view.get('controller.selectedDefinitions')).to.be.empty;
    });

    it("selectedAlertGroup is array with single element", function() {
      view.set('selectedAlertGroup', [1]);
      view.onGroupSelect();
      expect(view.get('selectedAlertGroup')).to.eql([1]);
      expect(view.get('controller.selectedAlertGroup')).to.equal(1);
      expect(view.get('controller.selectedDefinitions')).to.be.empty;
    });

    it("selectedAlertGroup is array with two elements", function() {
      view.set('selectedAlertGroup', [1, 2]);
      view.onGroupSelect();
      expect(view.get('selectedAlertGroup')).to.equal(2);
      expect(view.get('controller.selectedAlertGroup')).to.equal(2);
      expect(view.get('controller.selectedDefinitions')).to.be.empty;
    });
  });

  describe("#setGroupInController()", function() {

    beforeEach(function() {
      view.removeObserver('controller.selectedAlertGroup', view, 'setGroupInController');
      view.set('controller', Em.Object.create({selectedAlertGroup: 1}));
      view.set('selectedAlertGroup', null);
      sinon.stub(view, 'onGroupSelect');
    });

    afterEach(function() {
      view.onGroupSelect.restore();
    });

    it("controller.isLoaded is false", function() {
      view.set('controller.isLoaded', false);
      view.setGroupInController();
      expect(view.get('selectedAlertGroup')).to.be.null;
    });

    it("controller.isLoaded is true", function() {
      view.set('controller.isLoaded', true);
      view.setGroupInController();
      expect(view.get('selectedAlertGroup')).to.eql([1]);
    });
  });

  describe("#onLoad()", function() {

    beforeEach(function() {
      view.removeObserver('controller.isLoaded', view, 'onLoad');
      view.set('controller', Em.Object.create({alertGroups: Em.A([{}])}));
      view.set('selectedAlertGroup', null);
      sinon.stub(view, 'setTooltips');
    });

    afterEach(function() {
      view.setTooltips.restore();
    });

    it("controller.isLoaded is false", function() {
      view.set('controller.isLoaded', false);
      view.onLoad();
      expect(view.get('selectedAlertGroup')).to.be.null;
      expect(view.setTooltips.called).to.be.false;
    });

    it("controller.isLoaded is true", function() {
      view.set('controller.isLoaded', true);
      view.onLoad();
      expect(view.get('selectedAlertGroup')).to.eql([{}]);
      expect(view.setTooltips.calledOnce).to.be.true;
    });
  });

  describe("#willInsertElement()", function() {

    beforeEach(function() {
      view.set('controller', Em.Object.create({loadAlertNotifications: Em.K}));
      sinon.spy(view.get('controller'), 'loadAlertNotifications');
    });
    afterEach(function() {
      view.get('controller').loadAlertNotifications.restore();
    });

    it("loadAlertNotifications should be called", function() {
      view.willInsertElement();
      expect(view.get('controller').loadAlertNotifications.calledOnce).to.be.true;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'onLoad');
    });
    afterEach(function() {
      view.onLoad.restore();
    });

    it("loadAlertNotifications should be called", function() {
      view.didInsertElement();
      expect(view.onLoad.calledOnce).to.be.true;
    });
  });

  describe("#setTooltips()", function() {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(App, 'tooltip');
      view.setTooltips();
    });
    afterEach(function() {
      Em.run.next.restore();
      App.tooltip.restore();
    });

    it("Em.run.next should be called", function() {
      expect(Em.run.next.calledOnce).to.be.true;
    });

    it("App.tooltip should be called twice", function() {
      expect(App.tooltip.calledTwice).to.be.true;
    });
  });

  describe("#addDefinitionTooltip", function() {

    beforeEach(function() {
      view.set('controller', Em.Object.create({selectedAlertGroup: Em.Object.create()}));
    });

    it("controller.selectedAlertGroup.default is true", function() {
      view.set('controller.selectedAlertGroup.default', true);
      expect(view.get('addDefinitionTooltip')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionToDefault'));
    });

    it("controller.selectedAlertGroup.isAddDefinitionsDisabled is true", function() {
      view.set('controller.selectedAlertGroup.isAddDefinitionsDisabled', true);
      expect(view.get('addDefinitionTooltip')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionDisabled'));
    });

    it("controller.selectedAlertGroup is null", function() {
      view.set('controller.selectedAlertGroup', null);
      expect(view.get('addDefinitionTooltip')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinition'));
    });
  });

  describe("#removeDefinitionTooltip", function() {

    beforeEach(function() {
      view.set('controller', Em.Object.create({selectedAlertGroup: Em.Object.create()}));
    });

    it("controller.selectedAlertGroup.default is true", function() {
      view.set('controller.selectedAlertGroup.default', true);
      expect(view.get('removeDefinitionTooltip')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeDefinitionDisabled'));
    });

    it("controller.isDeleteDefinitionsDisabled is true", function() {
      view.set('controller.isDeleteDefinitionsDisabled', true);
      expect(view.get('removeDefinitionTooltip')).to.equal(Em.I18n.t('common.nothingToDelete'));
    });

    it("isDeleteDefinitionsDisabled & selectedAlertGroup.default are false", function() {
      view.set('controller.selectedAlertGroup.default', false);
      view.set('controller.isDeleteDefinitionsDisabled', false);
      expect(view.get('removeDefinitionTooltip')).to.equal(Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeDefinition'));
    });
  });
});
