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
var testHelpers = require('test/helpers');

function getController() {
  return App.KerberosWizardStep4Controller.create({
    wizardController: Em.Object.create({
      name: ''
    })
  });
}

describe('App.KerberosWizardStep4Controller', function() {
  var c;

  beforeEach(function() {
    c = getController();
  });

  describe("#clearStep()", function () {

    beforeEach(function() {
      c.clearStep();
    });

    it("isRecommendedLoaded should be set to false", function() {
      expect(c.get('isRecommendedLoaded')).to.be.false;
    });

    it("selectedService should be set to null", function() {
      expect(c.get('selectedService')).to.be.null;
    });

    it("stepConfigs should be set to false", function() {
      expect(c.get('stepConfigs')).to.be.empty;
    });
  });

  describe('#isSubmitDisabled', function() {
    var controller, configs;
    beforeEach(function() {
      controller = App.KerberosWizardStep4Controller.create({});
      configs = Em.A([
        App.ServiceConfigProperty.create({
          name: 'prop1',
          value: 'someVal1',
          identityType: 'user',
          category: 'Ambari Principals',
          serviceName: 'Cluster'
        })
      ]);
      controller.set('stepConfigs', controller.createServiceConfig(configs));
    });

    it('configuration errors are absent, submit should be not disabled', function() {
      expect(controller.get('stepConfigs')[0].get('errorCount')).to.be.equal(0);
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });

    it('config has invalid value, submit should be disabled', function() {
      var serviceConfig = controller.get('stepConfigs')[0];
      serviceConfig.get('configs').findProperty('name', 'prop1').set('value', '');
      serviceConfig.setActivePropertiesOnce();
      serviceConfig.setConfigsWithErrorsOnce();
      expect(serviceConfig.get('errorCount')).to.be.equal(1);
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
  });

  describe('#createServiceConfig', function() {
    var controller = App.KerberosWizardStep4Controller.create({});
    it('should create instance of App.ServiceConfig', function() {
      var configs = controller.createServiceConfig([], []);
      expect(configs).to.have.property('length').equal(2);
      expect(configs[0]).to.be.instanceof(App.ServiceConfig);
      expect(configs[1]).to.be.instanceof(App.ServiceConfig);
    });
  });

  describe('#prepareConfigProperties', function() {

    var properties = Em.A([
      Em.Object.create({ name: 'realm', value: '', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'spnego_keytab', value: 'spnego_keytab_value', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'hdfs_keytab', value: '', serviceName: 'HDFS', identityType: 'user', observesValueFrom: 'spnego_keytab' }),
      Em.Object.create({ name: 'falcon_keytab', value: 'falcon_keytab_value', serviceName: 'FALCON' }),
      Em.Object.create({ name: 'mapreduce_keytab', value: 'mapreduce_keytab_value', serviceName: 'MAPREDUCE2' }),
      Em.Object.create({ name: 'hdfs_principal', value: 'hdfs_principal_value', identityType: 'user', serviceName: 'HDFS' }),
      Em.Object.create({ name: 'hadoop.security.auth_to_local', serviceName: 'HDFS' }),
      App.ServiceConfigProperty.create({ name: 'null_value', serviceName: 'HDFS', value: null, isVisible: true }),
      App.ServiceConfigProperty.create({ name: 'null_value_observed_value_ok', serviceName: 'HDFS', value: null, isVisible: false, observesValueFrom: 'spnego_keytab' })
    ]);

    var propertyValidationCases = [
      {
        property: 'spnego_keytab',
        e: [
          { key: 'category', value: 'Global' },
          { key: 'observesValueFrom', absent: true }
        ]
      },
      {
        property: 'realm',
        e: [
          { key: 'category', value: 'Global' },
          { key: 'value', value: 'realm_value' }
        ]
      },
      {
        property: 'hdfs_keytab',
        e: [
          { key: 'category', value: 'Ambari Principals' },
          { key: 'value', value: 'spnego_keytab_value' },
          { key: 'observesValueFrom', value: 'spnego_keytab' }
        ]
      },
      {
        property: 'hadoop.security.auth_to_local',
        e: [
          { key: 'displayType', value: 'multiLine' }
        ]
      },
      {
        property: 'null_value',
        e: [
          { key: 'isVisible', value: false }
        ]
      },
      {
        property: 'null_value_observed_value_ok',
        e: [
          { key: 'isVisible', value: true }
        ]
      }
    ];

    var absentPropertiesTest = ['falcon_keytab', 'mapreduce_keytab'];

    before(function() {
      var controller = App.KerberosWizardStep4Controller.create({
        wizardController: {
          content: {
            serviceConfigProperties: Em.A([
              Em.Object.create({ name: 'realm', value: 'realm_value' })
            ])
          },
          loadCachedStepConfigValues: function() {
            return null;
          }
        }
      });
      sinon.stub(App.Service, 'find').returns(Em.A([
        { serviceName: 'HDFS' }
      ]));
      sinon.stub(App.configsCollection, 'getAll').returns([
        {
          name: 'hadoop.security.auth_to_local',
          displayType: 'multiLine'
        }
      ]);
      sinon.stub(App.router, 'get').withArgs('mainAdminKerberosController.isManualKerberos').returns(false);
      this.result = controller.prepareConfigProperties(properties);
    });

    after(function() {
      App.Service.find.restore();
      App.configsCollection.getAll.restore();
      App.router.get.restore();
    });

    it('should contains properties only for installed services', function() {
      expect(this.result.mapProperty('serviceName').uniq()).to.be.eql(['Cluster', 'HDFS']);
    });

    absentPropertiesTest.forEach(function(item) {
      it('property `{0}` should be absent'.format(item), function() {
        expect(this.result.findProperty('name', item)).to.be.undefined;
      });
    }, this);

    propertyValidationCases.forEach(function(test) {
      it('property {0} should be created'.format(test.property), function() {
        expect(this.result.findProperty('name', test.property)).to.be.ok;
      });
      test.e.forEach(function(expected) {
        it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
          if (!!expected.absent) {
            expect(this.result.findProperty('name', test.property)).to.not.have.deep.property(expected.key);
          } else {
            expect(this.result.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
          }
        }, this);
      }, this);
    });
  });

  describe("#createCategoryForServices()", function() {
    var controller = App.KerberosWizardStep4Controller.create({
      wizardController: {
        name: 'addServiceController'
      }
    });
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS'
        })
      ]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS',
          isInstalled: true
        }),
        Em.Object.create({
          serviceName: 'MAPREDUCE2',
          displayName: 'MapReduce 2',
          isInstalled: false,
          isSelected: true
        })
      ]);
    });

    afterEach(function() {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it('for add service', function() {
      expect(controller.createCategoryForServices()).to.eql([App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS', collapsedByDefault: true}),
        App.ServiceConfigCategory.create({ name: 'MAPREDUCE2', displayName: 'MapReduce 2', collapsedByDefault: true})]);
    });

    it('for kerberos wizard', function() {
      controller.set('wizardController.name', 'KerberosWizard');
      expect(controller.createCategoryForServices()).to.eql([App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS', collapsedByDefault: true})]);
    });
  });

  describe('#mergeDescriptorToConfigurations', function() {
    var genAppConfigProperty = function(name, fileName, value) {
      return App.ServiceConfigProperty.create({
        name: name,
        filename: fileName,
        value: value
      });
    };

    var genPropertyCollection = function(configsList) {
      return configsList.map(function(i) {
        return genAppConfigProperty.apply(undefined, i);
      });
    };

    var genConfigType = function(fileName, properties) {
      var configTypeObj = {};
      configTypeObj.type = fileName;
      configTypeObj.properties = properties.reduce(function(p, _c) {
        p[_c[0]] = _c[1];
        return p;
      }, {});
      return configTypeObj;
    };

    var genConfigTypeCollection = function(coll) {
      return coll.map(function(i) {
        return genConfigType(i[0], i[1]);
      });
    };

    var cases = [
      {
        kerberosDescriptor: genPropertyCollection([]),
        configurations: [],
        e: [],
        m: 'should return empty array'
      },
      {
        kerberosDescriptor: genPropertyCollection([
          ['hadoop.proxy.group', 'hadoop-env', 'val1']
        ]),
        configurations: genConfigTypeCollection([
          ['hadoop-env', [
           ['hadoop.proxy.group', 'change_me'],
           ['hadoop.proxy', 'val2']
          ]],
          ['core-site', [
            ['hadoop.proxyuser.hcat.groups', '*']
          ]]
        ]),
        e: [
          {
            type: 'hadoop-env',
            properties: {
              'hadoop.proxy.group': 'val1',
              'hadoop.proxy': 'val2'
            }
          },
          {
            type: 'core-site',
            properties: {
              'hadoop.proxyuser.hcat.groups': '*'
            }
          }
        ],
        m: 'should change value of `hadoop.proxy.group`, rest object should not be changed.'
      },
      {
        kerberosDescriptor: genPropertyCollection([
          ['hadoop.proxy.group', 'hadoop-env', 'val1'],
          ['new_site_prop', 'core-site', 'new_val']
        ]),
        configurations: genConfigTypeCollection([
          ['hadoop-env', [
            ['hadoop.proxy.group', 'val1'],
            ['hadoop.proxy', 'val2']
          ]],
          ['core-site', [
            ['hadoop.proxyuser.hcat.groups', '*']
          ]]
        ]),
        e: [
          {
            type: 'hadoop-env',
            properties: {
              'hadoop.proxy.group': 'val1',
              'hadoop.proxy': 'val2'
            }
          },
          {
            type: 'core-site',
            properties: {
              'hadoop.proxyuser.hcat.groups': '*',
              'new_site_prop': 'new_val'
            }
          }
        ],
        m: 'should add property `new_site_prop` value to `core-site` file type, rest object should not be changed.'
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        var toObj = function(res) {
          return JSON.parse(JSON.stringify(res));
        };
        expect(toObj(c.mergeDescriptorToConfigurations(test.configurations, test.kerberosDescriptor))).to.be.eql(test.e);
      });
    });
  });

  describe('#groupRecommendationProperties', function() {
    var cases, controller;
    beforeEach(function() {
      controller = App.KerberosWizardStep4Controller.create({});
    });

    afterEach(function() {
      controller.destroy();
      controller = null;
    });

    cases = [
      {
        recommendedConfigurations: {},
        servicesConfigurations: [],
        allConfigs: [],
        m: 'empty objects should not fail the code',
        e: {
          add: {},
          update: {},
          delete: {}
        }
      },
      {
        recommendedConfigurations: {
          'some-site': {
            properties: {
              // property absent from servicesConfigurations and allConfigs
              // should be added
              'new_prop1': 'val1',
              // property present in servicesConfigurations but absent in  allConfigs
              // should be skipped
              'new_prop2': 'val2',
              'existing-prop': 'updated_val2'
            },
            property_attributes: {
              'delete_prop1': {
                'delete': true
              }
            }
          }
        },
        servicesConfigurations: [
          {
            type: 'some-site',
            properties: {
              'existing-prop': 'val2',
              'new_prop2': 'val2'
            }
          }
        ],
        allConfigs: [
          Em.Object.create({ name: 'existing-prop', value: 'val3', filename: 'some-site'}),
          Em.Object.create({ name: 'delete_prop1', value: 'val', filename: 'some-site'})
        ],
        m: 'should add "new_prop1", remove "delete_prop1", skip adding "new_prop2" and update value for "existing-prop"',
        e: {
          update: {
            'some-site': {
              'existing-prop': 'updated_val2'
            }
          },
          add: {
            'some-site': {
              'new_prop1': 'val1'
            }
          },
          delete: {
            'some-site': {
              'delete_prop1': ''
            }
          }
        }
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        expect(controller.groupRecommendationProperties(test.recommendedConfigurations, test.servicesConfigurations, test.allConfigs))
          .to.be.eql(test.e);
      });
    });
  });

  describe("#getDescriptor()", function () {
    var mock = {
      then: Em.K
    };

    beforeEach(function() {
      sinon.stub(c, 'loadClusterDescriptorConfigs').returns(mock);
      sinon.stub(mock, 'then');
    });

    afterEach(function() {
      c.loadClusterDescriptorConfigs.restore();
      mock.then.restore();
    });

    it("loadClusterDescriptorConfigs should be called", function() {
      c.getDescriptor();
      expect(c.loadClusterDescriptorConfigs.calledOnce).to.be.true;
    });

    it("then should be called", function() {
      c.getDescriptor();
      expect(mock.then.calledOnce).to.be.true;
    });

  });

  describe("#tweakConfigProperty()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.HostComponent, 'find');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("config value should not be set", function() {
      this.mock.returns([
        Em.Object.create({
          componentName: 'HIVE_METASTORE',
          hostName: 'host1'
        })
      ]);
      var config = Em.Object.create({
        name: 'templeton.hive.properties',
        value: 'thrift://host1:9000\,,',
        recommendedValue: ''
      });
      c.tweakConfigProperty(config);
      expect(config.get('value')).to.be.equal('thrift://host1:9000\,,');
      expect(config.get('recommendedValue')).to.be.equal('');
    });

    it("config value should be set", function() {
      this.mock.returns([
        Em.Object.create({
          componentName: 'HIVE_METASTORE',
          hostName: 'host1'
        }),
        Em.Object.create({
          componentName: 'HIVE_METASTORE',
          hostName: 'host2'
        })
      ]);
      var config = Em.Object.create({
        name: 'templeton.hive.properties',
        value: 'thrift://host1:9000\,,'
      });
      c.tweakConfigProperty(config);
      expect(config.get('value')).to.be.equal("thrift://host1:9000\\,thrift://host2:9000,,");
      expect(config.get('recommendedValue')).to.be.equal("thrift://host1:9000\\,thrift://host2:9000,,");
    });
  });

  describe("#spnegoPropertiesObserver()", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'once', function(context, callback) {
        callback();
      });
    });

    afterEach(function() {
      Em.run.once.restore();
    });

    it("value should not be changed", function() {
      var configProperty = Em.Object.create({
        name: 'n1',
        value: 'new',
        recommendedValue: 'new'
      });
      var config = Em.Object.create({
        observesValueFrom: 'n2',
        value: '',
        recommendedValue: ''
      });
      c.set('stepConfigs', [Em.Object.create({
        name: 'ADVANCED',
        configs: [config]
      })]);
      c.spnegoPropertiesObserver(configProperty);
      expect(config.get('value')).to.be.empty;
      expect(config.get('recommendedValue')).to.be.empty;
    });

    it("value should be changed", function() {
      var configProperty = Em.Object.create({
        name: 'n1',
        value: 'new',
        recommendedValue: 'new'
      });
      var config = Em.Object.create({
        observesValueFrom: 'n1',
        value: '',
        recommendedValue: ''
      });
      c.set('stepConfigs', [Em.Object.create({
        name: 'ADVANCED',
        configs: [config]
      })]);
      c.spnegoPropertiesObserver(configProperty);
      expect(config.get('value')).to.be.equal('new');
      expect(config.get('recommendedValue')).to.be.equal('new');
    });
  });

  describe("#submit()", function () {

    beforeEach(function() {
      sinon.stub(c, 'saveConfigurations');
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      c.saveConfigurations.restore();
      App.router.send.restore();
    });

    it("saveConfigurations should be called", function() {
      c.submit();
      expect(c.saveConfigurations.calledOnce).to.be.true;
    });

    it("App.router.send should be called", function() {
      c.submit();
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });

  describe("#saveConfigurations()", function () {
    var mock = {
      saveKerberosDescriptorConfigs: Em.K
    };

    beforeEach(function() {
      sinon.stub(c, 'updateKerberosDescriptor');
      sinon.stub(App, 'get').returns(mock);
      sinon.spy(mock, 'saveKerberosDescriptorConfigs');
      c.set('kerberosDescriptor', {});
      c.set('stepConfigs', [
        Em.Object.create({
          configs: [{}]
        }),
        Em.Object.create({
          configs: [{}]
        })
      ]);
      c.saveConfigurations();
    });

    afterEach(function() {
      c.updateKerberosDescriptor.restore();
      App.get.restore();
      mock.saveKerberosDescriptorConfigs.restore();
    });

    it("updateKerberosDescriptor should be called", function() {
      expect(c.updateKerberosDescriptor.calledWith({}, [{}, {}])).to.be.true;
    });

    it("saveKerberosDescriptorConfigs should be called", function() {
      expect(mock.saveKerberosDescriptorConfigs.calledWith({})).to.be.true;
    });
  });

  describe("#loadServerSideConfigsRecommendations()", function () {

    it("App.ajax.send should be called", function() {
      c.loadServerSideConfigsRecommendations([]);
      var args = testHelpers.findAjaxRequest('name', 'config.recommendations');
      expect(args[0]).to.be.exists;
    });
  });

  describe("#applyServiceConfigs()", function () {

    it("isRecommendedLoaded should be true", function() {
      c.applyServiceConfigs([Em.Object.create({configGroups: []})]);
      expect(c.get('isRecommendedLoaded')).to.be.true;
    });

    it("selectedService should be set", function() {
      c.applyServiceConfigs([Em.Object.create({configGroups: []})]);
      expect(c.get('selectedService')).to.be.eql(Em.Object.create({configGroups: []}));
    });
  });

  describe("#bootstrapRecommendationPayload()", function () {

    beforeEach(function() {
      sinon.stub(c, 'getServicesConfigurations').returns({
        then: function(callback) {
          callback([{}]);
        }
      });
      sinon.stub(c, 'getBlueprintPayloadObject').returns({blueprint: {
        configurations: []
      }});
      c.bootstrapRecommendationPayload({});
    });

    afterEach(function() {
      c.getServicesConfigurations.restore();
      c.getBlueprintPayloadObject.restore();
    });

    it("getServicesConfigurations should be called", function() {
      expect(c.getServicesConfigurations.calledOnce).to.be.true;
    });

    it("getBlueprintPayloadObject should be called", function() {
      expect(c.getBlueprintPayloadObject.calledWith([{}], {})).to.be.true;
    });

    it("servicesConfigurations should be set", function() {
      expect(c.get('servicesConfigurations')).to.be.eql([{}]);
    });

    it("initialConfigValues should be set", function() {
      expect(c.get('initialConfigValues')).to.be.eql([]);
    });
  });

  describe("#getBlueprintPayloadObject()", function () {

    beforeEach(function() {
      sinon.stub(c, 'mergeDescriptorToConfigurations').returns([{
        type: 't1',
        properties: []
      }]);
      sinon.stub(c, 'createServicesStackDescriptorConfigs');
    });

    afterEach(function() {
      c.createServicesStackDescriptorConfigs.restore();
      c.mergeDescriptorToConfigurations.restore();
    });

    it("should return recommendations", function () {
      c.reopen({
        hostGroups: {
          blueprint: {
            configurations: []
          }
        }
      });
      expect(c.getBlueprintPayloadObject([], {})).to.be.eql({
        "blueprint": {
          "host_groups": [],
          "configurations": {
            "t1": {
              "properties": []
            }
          }
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      });
    });
  });

  describe("#getServicesConfigObject()", function () {

    it("should return ADVANCED step config", function() {
      c.set('stepConfigs', [{name: 'ADVANCED'}]);
      expect(c.getServicesConfigObject()).to.be.eql({name: 'ADVANCED'});
    });
  });

  describe("#getServiceByFilename()", function () {

    beforeEach(function() {
      this.mockService = sinon.stub(App.Service, 'find');
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'S1',
          configTypes: {
            site1: {}
          }
        })
      ]);
    });

    afterEach(function() {
      this.mockService.restore();
      App.StackService.find.restore();
    });

    it("should return 'HDFS' ", function() {
      this.mockService.returns([{serviceName: 'HDFS'}]);
      expect(c.getServiceByFilename('core-site')).to.be.equal('HDFS');
    });

    it("should return 'S1' ", function() {
      expect(c.getServiceByFilename('site1')).to.be.equal('S1');
    });

    it("should return empty", function() {
      expect(c.getServiceByFilename('site2')).to.be.empty;
    });
  });
});
