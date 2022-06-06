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
require('controllers/main/admin/highAvailability/resourceManager/step3_controller');
var testHelpers = require('test/helpers');
var blueprintUtils = require('utils/blueprint');

describe('App.RMHighAvailabilityWizardStep3Controller', function () {
  var controller;

  beforeEach(function() {
    controller = App.RMHighAvailabilityWizardStep3Controller.create({
      content: Em.Object.create({})
    });
  });

  describe('#isSubmitDisabled', function () {

    var cases = [
        {
          isLoaded: false,
          isSubmitDisabled: true,
          title: 'wizard step content not loaded'
        },
        {
          isLoaded: true,
          isSubmitDisabled: false,
          title: 'wizard step content loaded'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isLoaded', item.isLoaded);
        expect(controller.get('isSubmitDisabled')).to.equal(item.isSubmitDisabled);
      });
    });

  });

  describe('#loadConfigTagsSuccessCallback', function () {

    it('should send proper ajax request', function () {
      controller.loadConfigTagsSuccessCallback({
        'Clusters': {
          'desired_configs': {
            'zoo.cfg': {
              'tag': 1
            },
            'yarn-site': {
              'tag': 1
            },
            'yarn-env': {
              'tag': 1
            }
          }
        }
      }, {}, {
        'serviceConfig': {}
      });
      var data = testHelpers.findAjaxRequest('name', 'reassign.load_configs')[0].data;
      expect(data.urlParams).to.equal('(type=zoo.cfg&tag=1)|(type=yarn-site&tag=1)|(type=yarn-env&tag=1)');
      expect(data.serviceConfig).to.eql({});
    });

  });

  describe('#loadConfigsSuccessCallback', function () {

    var cases = [
        {
          'items': [],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'webAddressPort' : ':8088',
          'httpsWebAddressPort' : ':8090',
          'title': 'empty response'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg'
            },
            {
              'type': 'yarn-site'
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'webAddressPort' : ':8088',
          'httpsWebAddressPort' : ':8090',
          'title': 'no zoo.cfg properties received'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg',
              'properties': {
                'n': 'v'
              }
            },
            {
              'type': 'yarn-site',
              'properties': {
                'n': 'v'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'webAddressPort' : ':8088',
          'httpsWebAddressPort' : ':8090',
          'title': 'no clientPort property received'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg',
              'properties': {
                'clientPort': '2182'
              }
            },
            {
              'type': 'yarn-site',
              'properties': {
                'yarn.resourcemanager.webapp.address' : 'c6402.ambari.apache.org:7777',
                'yarn.resourcemanager.webapp.https.address' : 'c6402.ambari.apache.org:8888'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2182',
          'webAddressPort' : ':7777',
          'httpsWebAddressPort' : ':8888',
          'title': 'clientPort property received'
        }
      ];

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.loadConfigsSuccessCallback({
          items: item.items
        }, {}, item.params);
        expect(controller.get('selectedService')).to.eql({});
        expect(controller.get('isLoaded')).to.be.true;
      });
    });

  });

  describe('#loadConfigsSuccessCallback=loadConfigsErrorCallback(we have one callback for bouth cases)', function () {

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    it('should proceed with default value', function () {
      controller.loadConfigsSuccessCallback({}, {}, {}, {}, {
        serviceConfig: {}
      });
      expect(controller.get('selectedService')).to.eql({});
      expect(controller.get('isLoaded')).to.be.true;
    });

  });

  describe('#setDynamicConfigValues', function () {

    var data = {
      items: [
        {
          type: 'zoo.cfg',
          properties: {
            clientPort: 2222
          }
        },
        {
          type: 'yarn-env',
          properties: {
            yarn_user: 'yarn'
          }
        },
        {
          type: 'yarn-site',
          properties: {
            'yarn.resourcemanager.webapp.address': 'lclhst:1234',
            'yarn.resourcemanager.webapp.https.address': 'lclhst:4321'
          }
        }
      ]
    };

    var configs = {
        configs: [
          Em.Object.create({
            name: 'yarn.resourcemanager.hostname.rm1'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.hostname.rm2'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.zk-address'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.webapp.address.rm1'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.webapp.address.rm2'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.webapp.https.address.rm1'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.webapp.https.address.rm2'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.ha'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.scheduler.ha'
          }),
          Em.Object.create({
            name: 'hadoop.proxyuser.yarn.hosts'
          })
        ]
      };

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
              hostName: 'h2'
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
              hostName: 'h3'
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
              hostName: 'h4'
          })
        ];
      });
      controller.set('content', Em.Object.create({
        masterComponentHosts: [
          {component: 'RESOURCEMANAGER', hostName: 'h0', isInstalled: true},
          {component: 'RESOURCEMANAGER', hostName: 'h1', isInstalled: false},
          {component: 'ZOOKEEPER_SERVER', hostName: 'h2', isInstalled: true},
          {component: 'ZOOKEEPER_SERVER', hostName: 'h3', isInstalled: true}
        ],
        slaveComponentHosts: [],
        hosts: {},
        rmHosts: {
          currentRM: 'h0',
          additionalRM: 'h1'
        }
      }));
      controller.setDynamicConfigValues(configs, data);
    });

    afterEach(function () {
      App.HostComponent.find.restore();
    });

    it('yarn.resourcemanager.hostname.rm1 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm1').get('value')).to.equal('h0');
    });
    it('yarn.resourcemanager.hostname.rm1 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm1').get('recommendedValue')).to.equal('h0');
    });
    it('yarn.resourcemanager.hostname.rm2 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm2').get('value')).to.equal('h1');
    });
    it('yarn.resourcemanager.hostname.rm2 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm2').get('recommendedValue')).to.equal('h1');
    });
    it('yarn.resourcemanager.webapp.address.rm1 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.address.rm1').get('value')).to.equal('h0:1234');
    });
    it('yarn.resourcemanager.webapp.address.rm1 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.address.rm1').get('recommendedValue')).to.equal('h0:1234');
    });
    it('yarn.resourcemanager.webapp.address.rm2 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.address.rm2').get('value')).to.equal('h1:1234');
    });
    it('yarn.resourcemanager.webapp.address.rm2 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.address.rm2').get('recommendedValue')).to.equal('h1:1234');
    });
    it('yarn.resourcemanager.webapp.https.address.rm1 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm1').get('value')).to.equal('h0:4321');
    });
    it('yarn.resourcemanager.webapp.https.address.rm1 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm1').get('recommendedValue')).to.equal('h0:4321');
    });
    it('yarn.resourcemanager.webapp.https.address.rm2 value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm2').get('value')).to.equal('h1:4321');
    });
    it('yarn.resourcemanager.webapp.https.address.rm2 recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm2').get('recommendedValue')).to.equal('h1:4321');
    });
    it('yarn.resourcemanager.zk-address value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.zk-address').get('value')).to.equal('h2:2222,h3:2222');
    });
    it('yarn.resourcemanager.zk-address recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.zk-address').get('recommendedValue')).to.equal('h2:2222,h3:2222');
    });
    it('yarn.resourcemanager.ha value', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.ha').get('value')).to.equal('h0:8032,h1:8032');
    });
    it('yarn.resourcemanager.ha recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.scheduler.ha').get('recommendedValue')).to.equal('h0:8030,h1:8030');
    });

    it('hadoop.proxyuser.yarn.hosts value', function () {
      expect(configs.configs.findProperty('name', 'hadoop.proxyuser.yarn.hosts').get('value')).to.equal('h0,h1');
    });

    it('hadoop.proxyuser.yarn.hosts recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'hadoop.proxyuser.yarn.hosts').get('recommendedValue')).to.equal('h0,h1');
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'renderConfigs');
    });

    afterEach(function() {
      controller.renderConfigs.restore();
    });

    it("renderConfigs should be called", function() {
      controller.loadStep();
      expect(controller.renderConfigs.calledOnce).to.be.true;
    });
  });

  describe("#renderConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'renderConfigProperties');
      sinon.stub(App.Service, 'find').returns([Em.Object.create({
        serviceName: 'S1'
      })]);
      controller.renderConfigs();
    });

    afterEach(function() {
      controller.renderConfigProperties.restore();
      App.Service.find.restore();
    });

    it("renderConfigProperties should be called", function() {
      expect(controller.renderConfigProperties.getCall(0).args[1]).to.be.an('object').and.have.property('serviceName').equal('MISC');
    });

    it("App.ajax.send should be called", function() {
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0].data.serviceConfig).to.be.an('object').and.have.property('serviceName').equal('MISC');
    });
  });

  describe("#renderConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(App.ServiceConfigProperty, 'create', function(obj) {
        return obj;
      });
    });

    afterEach(function() {
      App.ServiceConfigProperty.create.restore();
    });

    it("config should be added", function() {
      var componentConfig = {
        configs: []
      };
      var _componentConfig = {
        configs: [
          Em.Object.create({
            isReconfigurable: true
          })
        ]
      };
      controller.renderConfigProperties(_componentConfig, componentConfig);
      expect(componentConfig.configs[0].get('isEditable')).to.be.true;
    });
  });

  describe("#submit()", function () {
    var container = {
      getKDCSessionState: Em.clb
    };

    beforeEach(function() {
      sinon.stub(App, 'get').returns(container);
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      App.get.restore();
      App.router.send.restore();
    });

    it("App.router.send should not be called", function() {
      controller.reopen({
        isSubmitDisabled: true
      });
      controller.submit();
      expect(App.router.send.called).to.be.false;
    });

    it("App.router.send should be called", function() {
      controller.reopen({
        isSubmitDisabled: false
      });
      controller.submit();
      expect(App.router.send.calledOnce).to.be.true;
    });
  });

  describe("#loadRecommendations()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getCurrentMasterSlaveBlueprint').returns({
        blueprint: {}
      });
      sinon.stub(blueprintUtils, 'getHostGroupByFqdn').returns('g1');
      sinon.stub(blueprintUtils, 'addComponentToHostGroup');
      sinon.stub(App.Service, 'find').returns([{serviceName: 'S1'}]);
      this.mockGet = sinon.stub(App, 'get');
      this.mockGet.withArgs('allHostNames').returns(['host1']);
      this.mockGet.withArgs('stackVersionURL').returns('/stacks/HDP/versions/2.2');
    });

    afterEach(function() {
      controller.getCurrentMasterSlaveBlueprint.restore();
      blueprintUtils.getHostGroupByFqdn.restore();
      blueprintUtils.addComponentToHostGroup.restore();
      App.Service.find.restore();
      this.mockGet.restore();
    });

    it("addComponentToHostGroup should be called", function() {
      controller.loadRecommendations({});
      expect(blueprintUtils.addComponentToHostGroup.getCall(0).args).to.be.eql([
        {blueprint: {configurations:{}}}, 'RESOURCEMANAGER', 'g1'
      ]);
    });

    it("App.ajax.send should be called", function() {
      controller.loadRecommendations({});
      var args = testHelpers.findAjaxRequest('name', 'config.recommendations');
      expect(JSON.stringify(args[0])).to.be.equal(JSON.stringify({
        name: 'config.recommendations',
        sender: controller,
        data: {
          stackVersionUrl: "/stacks/HDP/versions/2.2",
          dataToSend: {
            recommend: 'configurations',
            hosts: ["host1"],
            services: ['S1'],
            recommendations: {blueprint: {configurations:{}}}
          }
        }
      }));
    });
  });

  describe("#applyRecommendedConfigurations()", function () {
    var configurations = Em.Object.create({
        items: [
          {
            type: 'yarn-env',
            properties: {
              'yarn_user': 'user1'
            }
          }
        ]
      }),
      recommendations = {
        resources: [{
          recommendations: {
            blueprint: {
              configurations: {
                'core-site': {
                  properties: {
                    'hadoop.proxyuser.user1.hosts': 'host1',
                    'hadoop.proxyuser.user2.hosts': 'host1'
                  }
                }
              }
            }
          }
        }]
      };

    beforeEach(function() {
      sinon.stub(App.config, 'createDefaultConfig').returns({});
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('file1');
      sinon.stub(App.ServiceConfigProperty, 'create', function(obj) {
        return obj;
      });
    });

    afterEach(function() {
      App.config.createDefaultConfig.restore();
      App.config.getConfigTagFromFileName.restore();
      App.ServiceConfigProperty.create.restore();
    });

    it("config value should be set", function() {
      var stepConfigs = Em.Object.create({
        configs: [
          Em.Object.create({
            name: 'hadoop.proxyuser.user1.hosts'
          })
        ]
      });
      controller.applyRecommendedConfigurations(recommendations, configurations, stepConfigs);
      expect(stepConfigs.get('configs')[0].get('recommendedValue')).to.be.equal('host1');
      expect(stepConfigs.get('configs')[0].get('value')).to.be.equal('host1');
    });

    it("new property should be added", function() {
      var stepConfigs = Em.Object.create({
        configs: [
          Em.Object.create({
            name: 'hadoop.proxyuser.user2.hosts'
          })
        ]
      });
      controller.applyRecommendedConfigurations(recommendations, configurations, stepConfigs);
      expect(App.config.createDefaultConfig.getCall(0).args).to.be.eql(['hadoop.proxyuser.user1.hosts', 'core-site', false, {
        category : "HDFS",
        isUserProperty: false,
        isEditable: false,
        isOverridable: false,
        serviceName: 'MISC',
        value: 'host1',
        recommendedValue: 'host1'
      }]);
      expect(stepConfigs.get('configs')[1]).to.be.eql({
        filename: 'file1'
      });
    });
  });

});
