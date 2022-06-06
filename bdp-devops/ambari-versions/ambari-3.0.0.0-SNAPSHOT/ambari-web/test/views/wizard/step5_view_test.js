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
require('views/wizard/step5_view');
var stringUtils = require('utils/string_utils');
var view;

describe('App.WizardStep5View', function() {

  beforeEach(function() {
    view = App.WizardStep5View.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe("#title", function() {
    beforeEach(function () {
      view.set('controller.content', Em.Object.create());
    });

    it("controller name is reassignMasterController", function() {
      view.set('controller.content.controllerName', 'reassignMasterController');
      view.propertyDidChange('title');
      expect(view.get('title')).to.equal(Em.I18n.t('installer.step5.reassign.header'));
    });
    it("controller name is ''", function() {
      view.set('controller.content.controllerName', '');
      view.propertyDidChange('title');
      expect(view.get('title')).to.equal(Em.I18n.t('installer.step5.header'));
    });
  });

  describe("#setCoHostedComponentText()", function () {
    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1',
          isOtherComponentCoHosted: true,
          stackService: {
            isSelected: true
          },
          coHostedComponents: ['C2']
        }),
        Em.Object.create({
          componentName: 'C2',
          displayName: 'c2',
          isOtherComponentCoHosted: false,
          stackService: {
            isSelected: true
          }
        })
      ]);
      sinon.stub(stringUtils, 'getFormattedStringFromArray', function(str){
        return str;
      });
    });
    afterEach(function () {
      App.StackServiceComponent.find.restore();
      stringUtils.getFormattedStringFromArray.restore();
    });
    it("isReassignWizard - true", function () {
      view.set('controller.isReassignWizard', true);
      view.setCoHostedComponentText();
      expect(view.get('coHostedComponentText')).to.be.empty;
    });
    it("isReassignWizard - false", function () {
      view.set('controller.isReassignWizard', false);
      view.setCoHostedComponentText();
      expect(view.get('coHostedComponentText')).to.equal('<br/>' + Em.I18n.t('installer.step5.body.coHostedComponents').format(['c1', 'c2']));
    });
  });

  describe('#didInsertElement', function() {

    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadStep', Em.K);
    });

    afterEach(function () {
      view.get('controller').loadStep.restore();
    });

    it('should call controller.loadStep', function() {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
    });
  });

  describe('#shouldUseInputs', function() {

    Em.A([
      {range: 25, e: false},
      {range: 26, e: true},
      {range: 24, e: false}
    ]).forEach(function (test) {
      it(test.e + ' for ' + test.range + ' hosts', function () {
        view.set('controller.hosts', d3.range(0, test.range).map(function() {return {};}));
        expect(view.get('shouldUseInputs')).to.be.equal(test.e);
      });

    });

  });

});

describe('App.SelectHostView', function() {
  var models = require('test/init_model_test');

  beforeEach(function() {
    view = App.SelectHostView.create({
      controller: App.WizardStep5Controller.create({}),
      $: function() {return {typeahead: function(){return {on: Em.K}}}},
      updateErrorStatus: Em.K
    });
  });

  describe('#change', function() {

    beforeEach(function() {
      sinon.stub(view, 'initContent', Em.K);
      sinon.stub(view, 'changeHandler', Em.K);
      models.setupStackServiceComponent();
    });

    afterEach(function() {
      view.initContent.restore();
      view.changeHandler.restore();
      models.cleanStackServiceComponent();
    });

    it('should call initContent', function() {
      view.change();
      expect(view.initContent.calledOnce).to.be.true;
    });
  });

  describe('#didInsertElement', function() {

    it('should set value', function() {
      view.set('value', '');
      view.set('component', {selectedHost: 'h1'});
      view.didInsertElement();
      expect(view.get('value')).to.equal('h1');
    });

  });

  describe('#changeHandler', function() {

    beforeEach(function() {
      view.get('controller').reopen({multipleComponents: ['HBASE_MASTER', 'ZOOKEEPER_SERVER']});
      view.set('component', {component_name: 'ZOOKEEPER_SERVER', serviceComponentId: 1});
      view.set('controller.hosts', [Em.Object.create({host_info: 'h1 info', host_name: 'h1'})]);
      view.set('value', 'h1 info');
      view.set('controller.rebalanceComponentHostsCounter', 0);
      view.set('controller.componentToRebalance', '');
      sinon.stub(view.get('controller'), 'assignHostToMaster', Em.K);
      sinon.stub(view.get('controller'), 'updateIsHostNameValidFlag', Em.K);
      sinon.stub(view, 'shouldChangeHandlerBeCalled', function() {return true;});
    });

    afterEach(function() {
      view.get('controller').assignHostToMaster.restore();
      view.get('controller').updateIsHostNameValidFlag.restore();
      view.shouldChangeHandlerBeCalled.restore();
    });

    it('shouldn\'t do nothing if view is destroyed', function() {
      view.set('state', 'destroyed');
      expect(view.get('controller').assignHostToMaster.called).to.be.false;
    });

    it('should call assignHostToMaster', function() {
      view.changeHandler();
      expect(view.get('controller').assignHostToMaster.args[0]).to.be.eql(['ZOOKEEPER_SERVER', 'h1 info', 1]);
    });

    it('should increment rebalanceComponentHostsCounter if component it is multiple', function() {
      view.set('component', {component_name: 'ZOOKEEPER_SERVER'});
      view.changeHandler();
      expect(view.get('controller.rebalanceComponentHostsCounter')).to.equal(1);
    });

    it('should set componentToRebalance', function() {
      view.changeHandler();
      expect(view.get('controller.componentToRebalance')).to.equal('ZOOKEEPER_SERVER');
    });

  });

});

describe('App.InputHostView', function() {

  beforeEach(function() {
    view = App.InputHostView.create({
      controller: App.WizardStep5Controller.create({}),
      $: function() {return {typeahead: function(){return {on: Em.K}}}},
      updateErrorStatus: Em.K
    });
  });

  describe('#didInsertElement', function() {

    beforeEach(function() {
      sinon.stub(view, 'initContent', Em.K);
      view.set('content', [Em.Object.create({host_name: 'h1', host_info: 'h1 info'})]);
      view.set('component', {selectedHost: 'h1'});
    });

    afterEach(function() {
      view.initContent.restore();
    });

    it('should call initContent', function() {
      view.didInsertElement();
      expect(view.initContent.calledOnce).to.equal(true);
    });

    it('should set selectedHost host_name to value', function() {
      view.set('value', '');
      view.didInsertElement();
      expect(view.get('value')).to.equal('h1');
    });

  });

  describe('#changeHandler', function() {

    beforeEach(function() {
      view.get('controller').reopen({multipleComponents: ['HBASE_MASTER', 'ZOOKEEPER_SERVER']});
      view.set('component', {component_name: 'ZOOKEEPER_SERVER', serviceComponentId: 1});
      view.set('controller.hosts', [Em.Object.create({host_info: 'h1 info', host_name: 'h1'})]);
      view.set('value', 'h1');
      view.set('controller.rebalanceComponentHostsCounter', 0);
      view.set('controller.componentToRebalance', '');
      sinon.stub(view.get('controller'), 'assignHostToMaster', Em.K);
      sinon.stub(view.get('controller'), 'updateIsHostNameValidFlag', Em.K);
      sinon.stub(view, 'shouldChangeHandlerBeCalled', function() {return true;});
    });

    afterEach(function() {
      view.get('controller').assignHostToMaster.restore();
      view.get('controller').updateIsHostNameValidFlag.restore();
      view.shouldChangeHandlerBeCalled.restore();
    });

    it('shouldn\'t do nothing if view is destroyed', function() {
      view.set('state', 'destroyed');
      expect(view.get('controller').assignHostToMaster.called).to.be.false;
    });

    it('should call assignHostToMaster', function() {
      view.changeHandler();
      expect(view.get('controller').assignHostToMaster.args[0]).to.be.eql(['ZOOKEEPER_SERVER', 'h1', 1]);
    });

    it('should increment rebalanceComponentHostsCounter if component it is multiple', function() {
      view.set('component', {component_name: 'ZOOKEEPER_SERVER'});
      view.changeHandler();
      expect(view.get('controller.rebalanceComponentHostsCounter')).to.equal(1);
    });

    it('should set componentToRebalance', function() {
      view.changeHandler();
      expect(view.get('controller.componentToRebalance')).to.equal('ZOOKEEPER_SERVER');
    });

  });

  describe('#getAvailableHosts', function() {
    var tests = Em.A([
      {
        hosts: Em.A([]),
        selectedHost: 'h2',
        componentName: 'ZOOKEEPER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'ZOOKEEPER_SERVER', selectedHost: 'h1'})
        ]),
        m: 'Empty hosts',
        e: []
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'c1',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'c2', selectedHost: 'h1'})
        ]),
        m: 'Two hosts',
        e: ['h1', 'h2']
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'ZOOKEEPER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'ZOOKEEPER_SERVER', selectedHost: 'h1'})
        ]),
        m: 'Two hosts, ZOOKEEPER_SERVER',
        e: ['h2']
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'HBASE_MASTER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'HBASE_MASTER', selectedHost: 'h1'})
        ]),
        m: 'Two hosts, HBASE_MASTER',
        e: ['h2']
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.hosts', test.hosts);
        view.get('controller').reopen({multipleComponents: ['HBASE_MASTER', 'ZOOKEEPER_SERVER']});
        view.set('component', {component_name: test.componentName});
        view.set('controller.selectedServicesMasters', test.selectedServicesMasters);
        var r = view.getAvailableHosts();
        expect(r.mapProperty('host_name')).to.eql(test.e);
      });
    });
  });

  describe('#rebalanceComponentHostsOnce', function() {
    var tests = Em.A([
      {
        componentName: 'c1',
        componentToRebalance: 'c2',
        content: [{}],
        m: 'componentName not equal to componentToRebalance',
        e: {
          initContent: false
        }
      },
      {
        componentName: 'c2',
        componentToRebalance: 'c2',
        content: [{}],
        m: 'componentName equal to componentToRebalance',
        e: {
          initContent: true
        }
      }
    ]);

    beforeEach(function () {
      sinon.stub(view, 'initContent', Em.K);
    });

    afterEach(function () {
      view.initContent.restore();
    });

    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('content', test.content);
        view.set('component', {component_name: test.componentName});
        view.set('controller.componentToRebalance', test.componentToRebalance);
        view.rebalanceComponentHostsOnce();
        expect(view.initContent.calledOnce).to.equal(test.e.initContent);
      });
    });
  });

  describe('#initContent', function() {
    var tests = Em.A([
      {
        hosts: 25,
        m: 'not lazy loading, 25 hosts, no selected host',
        e: 25
      },
      {
        hosts: 25,
        h: 4,
        m: 'not lazy loading, 25 hosts, one selected host',
        e: 25
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.reopen({getAvailableHosts: function() {return d3.range(0, test.hosts).map(function(indx){return Em.Object.create({host_name: indx})});}});
        if (test.h) {
          view.set('selectedHost', test.h);
        }
        view.initContent();
        expect(view.get('content.length')).to.equal(test.e);
      });
    });
  });

  describe('#change', function() {

    beforeEach(function() {
      sinon.stub(view, 'changeHandler', Em.K);
    });

    afterEach(function() {
      view.changeHandler.restore();
    });

    it('shouldn\'t do nothing if view is destroyed', function() {
      view.set('controller.hostNameCheckTrigger', false);
      view.set('state', 'destroyed');
      view.change();
      expect(view.get('controller.hostNameCheckTrigger')).to.equal(false);
    });

    it('should toggle hostNameCheckTrigger', function() {
      view.set('controller.hostNameCheckTrigger', false);
      view.change();
      expect(view.get('controller.hostNameCheckTrigger')).to.equal(true);
    });

  });

});

describe('App.RemoveControlView', function() {

  beforeEach(function() {
    view = App.RemoveControlView.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe('#click', function() {
    beforeEach(function() {
      sinon.stub(view.get('controller'), 'removeComponent', Em.K);
    });
    afterEach(function() {
      view.get('controller').removeComponent.restore();
    });
    it('should call removeComponent', function() {
      view.set('serviceComponentId', 1);
      view.set('componentName', 'c1');
      view.click();
      expect(view.get('controller').removeComponent.calledWith('c1', 1)).to.equal(true);
    });
  });

});

describe('App.AddControlView', function() {

  beforeEach(function() {
    view = App.AddControlView.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe('#click', function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'addComponent', Em.K);
    });

    afterEach(function() {
      view.get('controller').addComponent.restore();
    });

    it('should call addComponent', function() {
      view.set('componentName', 'c1');
      view.click();
      expect(view.get('controller').addComponent.calledWith('c1')).to.equal(true);
    });

  });

});
