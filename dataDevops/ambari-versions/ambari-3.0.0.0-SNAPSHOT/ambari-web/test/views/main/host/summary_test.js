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
require('models/host');
require('models/service');
require('models/host_component');
require('mappers/server_data_mapper');
require('views/main/host/summary');

var mainHostSummaryView;
var modelSetup = require('test/init_model_test');

describe('App.MainHostSummaryView', function() {

  beforeEach(function() {
    modelSetup.setupStackServiceComponent();
    mainHostSummaryView = App.MainHostSummaryView.create({content: Em.Object.create()});
  });

  afterEach(function(){
    modelSetup.cleanStackServiceComponent();
  });

  describe("#installedServices", function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([Em.Object.create({serviceName: 'S1'})]);
    });
    afterEach(function() {
      App.Service.find.restore();
    });

    it("should return installed services", function() {
      expect(mainHostSummaryView.get('installedServices')).to.eql(['S1']);
    });
  });

  describe('#sortedComponentsFormatter()', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({componentName: 'C', displayName: 'C'}),
            Em.Object.create({componentName: 'A', displayName: 'A'}),
            Em.Object.create({componentName: 'B', displayName: 'B'}),
            Em.Object.create({componentName: 'D', displayName: 'D'})
          ])
        }),
        m: 'List of components',
        e: ['A', 'B', 'C', 'D']
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        test.content.get('hostComponents').forEach(function(component) {
          component.set('id', component.get('componentName'));
        });
        mainHostSummaryView.set('sortedComponents', []);
        mainHostSummaryView.set('content', test.content);
        mainHostSummaryView.sortedComponentsFormatter();
        expect(mainHostSummaryView.get('sortedComponents').mapProperty('componentName')).to.eql(test.e);
      });
    });

  });

  describe('#isAddComponent', function() {

    var tests = Em.A([
      {content: {healthClass: 'health-status-DEAD-YELLOW', hostComponents: Em.A([])}, e: false},
      {content: {healthClass: 'OTHER_VALUE', hostComponents: Em.A([])}, e: true}
    ]);

    tests.forEach(function(test) {
      it(test.content.healthClass, function() {
        mainHostSummaryView.set('content', test.content);
        expect(mainHostSummaryView.get('isAddComponent')).to.equal(test.e);
      });
    });

  });

  describe('#addableComponents', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(mainHostSummaryView, 'hasCardinalityConflict').returns(false);
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
      mainHostSummaryView.hasCardinalityConflict.restore();
    });

    var tests = Em.A([
      {
        addableToHostComponents: [
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'DATANODE',
            isAddableToHost: true
          }),
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'HDFS_CLIENT',
            isAddableToHost: true
          })
        ],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            })
          ])
        }),
        services: ['HDFS'],
        e: ['DATANODE'],
        m: 'some components are already installed'
      },
      {
        addableToHostComponents: [
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'HDFS_CLIENT',
            isAddableToHost: true
          })
        ],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            })
          ])
        }),
        services: ['HDFS'],
        e: [],
        m: 'all components are already installed'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        this.mock.returns(test.addableToHostComponents);
        mainHostSummaryView.set('content', test.content);
        mainHostSummaryView.reopen({
          installedServices: test.services
        });
        mainHostSummaryView.propertyDidChange('addableComponents');
        expect(mainHostSummaryView.get('addableComponents').mapProperty('componentName')).to.eql(test.e);
      });
    });
  });

  describe("#needToRestartMessage", function() {

    it("one component", function() {
      var expected = Em.I18n.t('hosts.host.details.needToRestart').format(1, Em.I18n.t('common.component').toLowerCase());
      mainHostSummaryView.set('content', Em.Object.create({
        componentsWithStaleConfigsCount: 1
      }));
      expect(mainHostSummaryView.get('needToRestartMessage')).to.equal(expected);
    });

    it("multiple components", function() {
      var expected = Em.I18n.t('hosts.host.details.needToRestart').format(2, Em.I18n.t('common.components').toLowerCase());
      mainHostSummaryView.set('content', Em.Object.create({
        componentsWithStaleConfigsCount: 2
      }));
      expect(mainHostSummaryView.get('needToRestartMessage')).to.equal(expected);
    });

  });
  
  describe("#willInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(mainHostSummaryView, 'sortedComponentsFormatter');
      sinon.stub(mainHostSummaryView, 'addObserver');
    });
    afterEach(function() {
      mainHostSummaryView.sortedComponentsFormatter.restore();
      mainHostSummaryView.addObserver.restore();
    });

    it("sortedComponentsFormatter should be called ", function() {
      mainHostSummaryView.willInsertElement();
      expect(mainHostSummaryView.sortedComponentsFormatter.calledOnce).to.be.true;
      expect(mainHostSummaryView.addObserver.calledWith('content.hostComponents.length', mainHostSummaryView, 'sortedComponentsFormatter')).to.be.true;
      expect(mainHostSummaryView.get('sortedComponents')).to.be.empty;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(mainHostSummaryView, 'addToolTip');
    });
    afterEach(function() {
      mainHostSummaryView.addToolTip.restore();
    });

    it("addToolTip should be called", function() {
      mainHostSummaryView.didInsertElement();
      expect(mainHostSummaryView.addToolTip.calledOnce).to.be.true;
    });
  });

  describe("#addToolTip()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
      mainHostSummaryView.removeObserver('addComponentDisabled', mainHostSummaryView, 'addToolTip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("addComponentDisabled is false ", function() {
      mainHostSummaryView.reopen({
        addComponentDisabled: false
      });
      mainHostSummaryView.addToolTip();
      expect(App.tooltip.called).to.be.false;
    });

    it("addComponentDisabled is true ", function() {
      mainHostSummaryView.reopen({
        addComponentDisabled: true
      });
      mainHostSummaryView.addToolTip();
      expect(App.tooltip.called).to.be.true;
    });

  });

  describe("#hasCardinalityConflict()", function () {

    beforeEach(function() {
      this.mockComponent = sinon.stub(App.HostComponent, 'getCount');
      this.mockStack = sinon.stub(App.StackServiceComponent, 'find');
    });

    afterEach(function() {
      this.mockComponent.restore();
      this.mockStack.restore();
    });

    it("totalCount equal to maxToInstall", function() {
      this.mockComponent.returns(1);
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.true;
    });

    it("totalCount more than maxToInstall", function() {
      this.mockComponent.returns(2);
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.true;
    });

    it("totalCount less than maxToInstall", function() {
      this.mockComponent.returns(0);
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.false;
    });
  });

  describe("#timeSinceHeartBeat", function () {

    beforeEach(function() {
      sinon.stub($, 'timeago').returns('1');
    });

    afterEach(function() {
      $.timeago.restore();
    });

    it("lastHeartBeatTime = null", function() {
      mainHostSummaryView.set('content.isNotHeartBeating', true);
      mainHostSummaryView.set('content.lastHeartBeatTime', null);
      mainHostSummaryView.propertyDidChange('timeSinceHeartBeat');
      expect(mainHostSummaryView.get('timeSinceHeartBeat')).to.be.empty;
    });

    it("lastHeartBeatTime = 1", function() {
      mainHostSummaryView.set('content.isNotHeartBeating', true);
      mainHostSummaryView.set('content.lastHeartBeatTime', '1');
      mainHostSummaryView.propertyDidChange('timeSinceHeartBeat');
      expect(mainHostSummaryView.get('timeSinceHeartBeat')).to.be.equal('1');
    });

    it("host has heartbeat", function() {
      mainHostSummaryView.set('content.isNotHeartBeating', false);
      mainHostSummaryView.set('content.lastHeartBeatTime', '1');
      mainHostSummaryView.propertyDidChange('timeSinceHeartBeat');
      expect(mainHostSummaryView.get('timeSinceHeartBeat')).to.be.equal(Em.I18n.t('common.minute.ago'));
    });
  });
});
