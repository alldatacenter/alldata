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

describe('App.NameNodeFederationWizardStep3Controller', function () {
  var controller;
  beforeEach(function () {
    controller = App.NameNodeFederationWizardStep3Controller.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#clearStep', function () {
    it('should clear stepConfigs field', function () {
      controller.set('stepConfigs', [{}]);
      controller.clearStep();
      expect(controller.get('stepConfigs').length).to.equal(0);
    });

    it('should set serverConfigData to empty object', function () {
      controller.set('serverConfigData', {test: 1});
      controller.clearStep();
      expect(controller.get('serverConfigData')).to.eql({});
    });

    it('should set loading flags to false', function () {
      sinon.stub(App.router, 'get').returns(null);
      controller.set('isConfigsLoaded', true);
      controller.set('isLoaded', true);
      controller.clearStep();
      expect(controller.get('isConfigsLoaded')).to.equal(false);
      expect(controller.get('isLoaded')).to.equal(false);
      App.router.get.restore();
    });
  });

  describe('#loadStep', function () {
    it('should call clearStep and loadConfigsTags method', function () {
      sinon.stub(controller, 'clearStep');
      sinon.stub(controller, 'loadConfigsTags');
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
      controller.clearStep.restore();
      controller.loadConfigsTags.restore();
    });
  });

  describe('#loadConfigsTags', function() {
    it('should send config.tags request', function () {
      controller.loadConfigsTags();
      expect(App.ajax.send.calledWith({
        name: 'config.tags',
        sender: controller,
        success: 'onLoadConfigsTags'
      })).to.be.true;
    });
  });

  describe('#onLoadConfigsTags', function () {
    var data = {
      Clusters: {
        desired_configs: {
          'hdfs-site': {
            tag: 'test1'
          },
          'core-site': {
            tag: 'test2'
          },
          'ranger-tagsync-site': {
            tag: 'test3'
          },
          'ranger-hdfs-security': {
            tag: 'test4'
          },
          'accumulo-site': {
            tag: 'test5'
          }
        }
      }
    };

    afterEach(function(){
      App.Service.find.restore();
    });

    it('should build short url params if no Ranger present', function () {
      sinon.stub(App.Service, 'find').returns([]);
      controller.onLoadConfigsTags(data);
      expect(App.ajax.send.calledWith({
        name: 'admin.get.all_configurations',
        sender: controller,
        data: {
          urlParams: '(type=hdfs-site&tag=test)'
        },
        success: 'onLoadConfigs'
      }));
    });

    it('should build long url params if Ranger is present', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'RANGER'}]);
      controller.onLoadConfigsTags(data);
      expect(App.ajax.send.calledWith({
        name: 'admin.get.all_configurations',
        sender: controller,
        data: {
          urlParams: '(type=hdfs-site&tag=test)|(type=core-site&tag=test1)|(type=ranger-tagsync-site&tag=test2)|(type=ranger-hdfs-security&tag=test3)'
        },
        success: 'onLoadConfigs'
      }));
    });

    it('should build long url params if Accumulo is present', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'ACCUMULO'}]);
      controller.onLoadConfigsTags(data);
      expect(App.ajax.send.calledWith({
        name: 'admin.get.all_configurations',
        sender: controller,
        data: {
          urlParams: '(type=hdfs-site&tag=test)|(type=accumulo-site&tag=test5)'
        },
        success: 'onLoadConfigs'
      }));
    });
  });

  describe('#onLoadConfigs', function () {
    it('should set serverConfigData to param and isConfigsLoaded to true', function () {
      sinon.stub(App.router, 'get').returns(null);
      controller.onLoadConfigs('test');
      expect(controller.get('serverConfigData')).to.equal('test');
      expect(controller.get('isConfigsLoaded')).to.equal(true);
      App.router.get.restore();
    });
  });

  describe('#onLoad', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({});
      sinon.stub(controller, 'removeConfigs');
      sinon.stub(controller, 'renderServiceConfigs');
      sinon.stub(controller, 'tweakServiceConfigs');
    });
    afterEach(function () {
      App.router.get.restore();
      controller.removeConfigs.restore();
      controller.renderServiceConfigs.restore();
      controller.tweakServiceConfigs.restore();
    });
    it('should call removeConfigs and renderServiceConfigs and set isLoaded to true if isConfigsLoaded and isHDFSNameSpacesLoaded are truthly', function () {
      controller.set('isConfigsLoaded', true);
      expect(controller.tweakServiceConfigs.called).to.be.true;
      expect(controller.removeConfigs.calledWith(controller.get('configsToRemove'), controller.get('serverConfigData'))).to.be.true;
      expect(controller.renderServiceConfigs.calledOnce).to.be.true;
      expect(controller.get('isLoaded')).to.equal(true);
    });

    it('should do nothing when isConfigsLoaded is falsy', function(){
      controller.set('isLoaded', false);
      controller.set('isConfigsLoaded', false);
      expect(controller.tweakServiceConfigs.called).to.be.false;
      expect(controller.removeConfigs.calledOnce).to.be.false;
      expect(controller.renderServiceConfigs.calledOnce).to.be.false;
      expect(controller.get('isLoaded')).to.equal(false);
    })
  });

  var dependenices = {
    clustername: 'test',
    nameServicesList: 'test1,test2',
    nameservice1: 'test1',
    newNameservice: 'testId1',
    namenode1: 'test1',
    namenode2: 'test2',
    newNameNode1Index: 'nn1',
    newNameNode2Index: 'nn2',
    newNameNode1: 'test1',
    newNameNode2: 'test2',
    journalnodes: 'test1:8485',
    nnHttpPort: 50070,
    nnHttpsPort: 50470,
    nnRpcPort: 8020,
    journalnode_edits_dir: 'test'
  };
  describe('#prepareDependencies', function () {
    it('should build dependencies object', function () {
      sinon.stub(App.HDFSService, 'find').returns([Ember.Object.create({
        masterComponentGroups: [{name: 'test1'}, {name: 'test2'}]
      })]);
      sinon.stub(App.HostComponent, 'find').returns([Ember.Object.create({componentName: 'JOURNALNODE', hostName: 'test1'})]);
      sinon.stub(App, 'get').returns('test');
      controller.set('content', Ember.Object.create({
        nameServiceId: 'testId1',
        masterComponentHosts: [{component: 'NAMENODE', isInstalled: false, hostName: 'test1'}, {component: 'NAMENODE', isInstalled: false, hostName: 'test2'}]
      }));
      controller.set('serverConfigData', Ember.Object.create({
        items: [{type: 'hdfs-site', properties: {
            'dfs.namenode.rpc-address.test1.nn1': 'test1:test1',
            'dfs.namenode.rpc-address.test1.nn2': 'test2:test2',
            'dfs.journalnode.edits.dir': 'test'
          }}]
      }));

      expect(controller.prepareDependencies()).to.eql(dependenices);
      App.HostComponent.find.restore();
      App.HDFSService.find.restore();
      App.get.restore();
    });
  });

  describe('#tweakServiceConfigs', function () {
    var configs = [{
      name: 'test1',
      displayName: 'test1',
      value: 'test1',
      recommendedValue: 'test1'
    }, {
      name: 'test2',
      displayName: 'test2',
      value: 'test2',
      recommendedValue: 'test2'
    }, {
      name: 'dfs.namenode.servicerpc-address.{{nameservice1}}.nn1',
      displayName: 'test2',
      value: 'test2',
      recommendedValue: 'test2'
    }];

    beforeEach(function () {
      sinon.stub(controller, 'prepareDependencies').returns(dependenices);
      controller.set('serverConfigData', {items: [{type: 'hdfs-site', properties: {}}]})
    });
    afterEach(function () {
      controller.prepareDependencies.restore();
      App.HDFSService.find.restore();
      App.Service.find.restore();
    });

    it('should return empty array when no configs, no hdfsSiteConfigs', function () {
      sinon.stub(App.HDFSService, 'find').returns([Ember.Object.create({masterComponentGroups: []})]);
      sinon.stub(App.Service, 'find').returns([]);
      expect(controller.tweakServiceConfigs([])).to.eql([]);
    });

    it('should return mapped configs object when no hdfsSiteConfigs', function () {
      sinon.stub(App.HDFSService, 'find').returns([Ember.Object.create({masterComponentGroups: []})]);
      sinon.stub(App.Service, 'find').returns([]);
      expect(controller.tweakServiceConfigs(configs)).to.eql([{
        name: 'test1',
        displayName: 'test1',
        value: 'test1',
        recommendedValue: 'test1',
        isOverridable: false
      }, {
        name: 'test2',
        displayName: 'test2',
        value: 'test2',
        recommendedValue: 'test2',
        isOverridable: false
      }]);
    });
  });

  describe('#createRangerServiceProperty', function () {
    it('should create valid property depending from input params', function () {
      expect(controller.createRangerServiceProperty('testService', 'testPrefix-', 'testProp')).to.eql({
        "name": 'testProp',
        "displayName": 'testProp',
        "isReconfigurable": false,
        "recommendedValue": 'testPrefix-testService',
        "value": 'testPrefix-testService',
        "category": "RANGER",
        "filename": "ranger-tagsync-site",
        "serviceName": 'MISC'
      });
    });
  });

  describe('#replaceDependencies', function () {
    it('should not change anything if no deps finded', function () {
      expect(controller.replaceDependencies('testval-{{test}}', {})).to.equal('testval-{{test}}');
    });

    it('should not change value if no dep is finded', function () {
      expect(controller.replaceDependencies('testval-{{test}}', {test: 'abc'})).to.equal('testval-abc');
    });
  });

  describe('#renderServiceConfigs', function() {
    var config = {serviceName: 'test1', displayName: 'test2', configCategories: [{
      name: 'service1'
    }, {
      name: 'service2'
    }]};
    beforeEach(function () {
      sinon.stub(controller, 'loadComponentConfigs');
    });
    afterEach(function () {
      App.Service.find.restore();
      controller.loadComponentConfigs.restore();
    });
    it('should call loadComponentConfigs with empty configCategories if no services', function () {
      sinon.stub(App.Service, 'find').returns([]);

      controller.renderServiceConfigs(config);
      expect(controller.loadComponentConfigs.calledWith(config, App.ServiceConfig.create({
        serviceName: 'test1',
        displayName: 'test2',
        configCategories: [],
        showConfig: true,
        configs: []
      }))).to.be.true;
    });

    it('should call loadComponentConfigs with non empty configCategories services are present', function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'service1'}]);

      controller.renderServiceConfigs(config);
      expect(controller.loadComponentConfigs.calledWith(config, App.ServiceConfig.create({
        serviceName: 'test1',
        displayName: 'test2',
        configCategories: [{name: 'service1'}],
        showConfig: true,
        configs: []
      }))).to.be.true;
    });
  });

  describe('#isNextDisabled', function() {
    it('should disable button when isLoaded is false', function () {
      controller.set('isLoaded', false);
      expect(controller.get('isNextDisabled')).to.be.true;
    });

    it('should disable button when some of configs is invalid', function () {
      controller.set('isLoaded', true);
      controller.set('selectedService', {configs: [{isValid: true}, {isValid: false}]})
      expect(controller.get('isNextDisabled')).to.be.true;
    });

    it('should enable button when isLoaded and all configs are valid', function () {
      controller.set('isLoaded', true);
      controller.set('selectedService', {configs: [{isValid: true}, {isValid: true}]})
      expect(controller.get('isNextDisabled')).to.be.false;
    });
  });
});