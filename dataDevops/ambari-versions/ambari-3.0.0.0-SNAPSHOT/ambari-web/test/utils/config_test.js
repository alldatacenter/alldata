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
require('config');
require('utils/configs_collection');
require('utils/config');
require('models/service/hdfs');
var testHelpers = require('test/helpers');
var setups = require('test/init_model_test');

describe('App.config', function() {

  describe('#getOriginalFileName', function() {
    var tests = [
      { input: 'someFileName.xml', output: 'someFileName.xml', m: 'do not add extra ".xml" '},
      { input: 'someFileNamexml', output: 'someFileNamexml.xml' , m: 'add ".xml" '},
      { input: 'alert_notification', output: 'alert_notification', m: 'do not add ".xml" to special file names' }
    ];

    tests.forEach(function(t) {
        it(t.m, function() {
          expect(App.config.getOriginalFileName(t.input)).to.equal(t.output);
        });
    });
  });

  describe('#getConfigTagFromFileName', function() {
    var tests = [
      { input: 'someFileName.xml', output: 'someFileName', m: 'remove ".xml"'},
      { input: 'someFileNamexml', output: 'someFileNamexml' , m: 'leave as is'}
    ];

    tests.forEach(function(t) {
      it(t.m, function() {
        expect(App.config.getConfigTagFromFileName(t.input)).to.equal(t.output);
      });
    });
  });

  describe('#configId', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getConfigTagFromFileName', function (fName) {
        return fName;
      });
    });
    afterEach(function() {
      App.config.getConfigTagFromFileName.restore();
    });
    it('generates config id', function() {
      expect(App.config.configId('name', 'fName')).to.equal('name__fName');
    });
  });

  describe('#getDefaultConfig', function() {
    beforeEach(function () {
      sinon.stub(App.configsCollection, 'getConfigByName', function (name, fileName) {
        if (name === 'inCollection') {
          return { name: name, filename: fileName, fromCollection: true };
        }
        return null;
      });

      sinon.stub(App.config, 'createDefaultConfig', function (name, fileName) {
        return { name: name, filename: fileName, fromStack: false, generatedDefault: true }
      });
    });
    afterEach(function () {
      App.configsCollection.getConfigByName.restore();
      App.config.createDefaultConfig.restore();
    });

    it('get return config from collection' , function () {
      expect(App.config.getDefaultConfig('inCollection', 'f1')).to.eql({ name: 'inCollection', filename: 'f1', fromCollection: true })
    });

    it('get return config not from collection' , function () {
      expect(App.config.getDefaultConfig('custom', 'f1')).to.eql({ name: 'custom', filename: 'f1', fromStack: false, generatedDefault: true })
    });

    it('get return extended with custom object' , function () {
      expect(App.config.getDefaultConfig('inCollection', 'f1', { additionalProperty: true})).to.eql({ name: 'inCollection', filename: 'f1', fromCollection: true, additionalProperty: true })
    });
  });

  describe('#trimProperty',function() {
    var testMessage = 'displayType `{0}`, value `{1}`{3} should return `{2}`';
    var tests = [
      {
        config: {
          displayType: 'directory',
          value: ' /a /b /c'
        },
        e: '/a,/b,/c'
      },
      {
        config: {
          displayType: 'directories',
          value: ' /a /b '
        },
        e: '/a,/b'
      },
      {
        config: {
          displayType: 'directories',
          name: 'dfs.datanode.data.dir',
          value: ' [DISK]/a [SSD]/b '
        },
        e: '[DISK]/a,[SSD]/b'
      },
      {
        config: {
          displayType: 'directories',
          name: 'dfs.datanode.data.dir',
          value: '/a,/b, /c\n/d,\n/e  /f'
        },
        e: '/a,/b,/c,/d,/e,/f'
      },
      {
        config: {
          displayType: 'host',
          value: ' localhost '
        },
        e: 'localhost'
      },
      {
        config: {
          displayType: 'password',
          value: ' passw ord '
        },
        e: ' passw ord '
      },
      {
        config: {
          displayType: 'string',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'string',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'string',
          value: 'http://localhost ',
          name: 'javax.jdo.option.ConnectionURL'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'string',
          value: 'http://localhost    ',
          name: 'oozie.service.JPAService.jdbc.url'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'custom',
          value: ' custom value '
        },
        e: ' custom value'
      },
      {
        config: {
          displayType: 'componentHosts',
          value: ['host1.com', 'host2.com']
        },
        e: ['host1.com', 'host2.com']
      }
    ];

    tests.forEach(function(test) {
      it(testMessage.format(test.config.displayType, test.config.value, test.e, !!test.config.name ? ', name `' + test.config.name + '`' : ''), function() {
        expect(App.config.trimProperty(test.config)).to.eql(test.e);
        expect(App.config.trimProperty(Em.Object.create(test.config), true)).to.eql(test.e);
      });
    });
  });

  describe.skip('#preDefinedSiteProperties-bigtop', function () {
    before(function() {
      setups.setupStackVersion(this, 'BIGTOP-0.8');
    });

    it('bigtop should use New PostgreSQL Database as its default hive metastore database', function () {
      App.StackService.createRecord({serviceName: 'HIVE'});
      expect(App.config.get('preDefinedSiteProperties').findProperty('recommendedValue', 'New PostgreSQL Database')).to.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#generateConfigPropertiesByName', function() {
    var tests = [
      {
        names: ['property_1', 'property_2'],
        properties: undefined,
        e: {
          keys: ['name']
        },
        m: 'Should generate base property object without additional fields'
      },
      {
        names: ['property_1', 'property_2'],
        properties: { category: 'SomeCat', serviceName: 'SERVICE_NAME' },
        e: {
          keys: ['name', 'category', 'serviceName']
        },
        m: 'Should generate base property object without additional fields'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).length).to.eql(test.names.length);
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).map(function(property) {
          return Em.keys(property);
        }).reduce(function(p, c) {
          return p.concat(c);
        }).uniq()).to.eql(test.e.keys);
      });
    });

  });

  describe('#setPreDefinedServiceConfigs', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function() {
        return [
          Em.Object.create({
            id: 'HDFS',
            serviceName: 'HDFS',
            configTypes: {
              'hadoop-env': {},
              'hdfs-site': {}
            }
          }),
          Em.Object.create({
            id: 'OOZIE',
            serviceName: 'OOZIE',
            configTypes: {
              'oozie-env': {},
              'oozie-site': {}
            }
          })
        ];
      });
      App.config.setPreDefinedServiceConfigs(true);
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should include service MISC', function() {
      expect(App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC')).to.be.ok;
    });

    it('should include -env config types according to stack services', function() {
      var miscCategory = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC');
      expect(Em.keys(miscCategory.get('configTypes'))).to.eql(['cluster-env', 'hadoop-env', 'oozie-env']);
    });
  });

  describe('#isManagedMySQLForHiveAllowed', function () {

    var cases = [
      {
        osFamily: 'redhat5',
        expected: false
      },
      {
        osFamily: 'redhat6',
        expected: true
      },
      {
        osFamily: 'suse11',
        expected: false
      }
    ],
      title = 'should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osFamily), function () {
        expect(App.config.isManagedMySQLForHiveAllowed(item.osFamily)).to.equal(item.expected);
      });
    });

  });

  describe('#preDefinedSiteProperties', function () {
    var allPreDefinedSiteProperties = [
      { name: 'p1', serviceName: 's1' },
      { name: 'p1', serviceName: 's2' },
      { name: 'p1', serviceName: 'MISC' }
    ];
    beforeEach(function () {
      sinon.stub(App.config, 'get', function (param) {
        if (param === 'allPreDefinedSiteProperties') {
          return allPreDefinedSiteProperties;
        }
        return Em.get(App.config, param);
      });
      sinon.stub(App.StackService, 'find').returns([{serviceName: 's1'}]);
    });
    afterEach(function () {
      App.config.get.restore();
      App.StackService.find.restore();
    });

    it('returns map with secure configs', function () {
      expect(App.config.get('preDefinedSiteProperties')).to.eql([
        { name: 'p1', serviceName: 's1' },
        { name: 'p1', serviceName: 'MISC' }
      ]);
    })
  });

  describe('#preDefinedSitePropertiesMap', function () {
    beforeEach(function () {
      sinon.stub(App.config, 'get', function (param) {
        if (param === 'preDefinedSiteProperties') {
          return [
            {name: 'sc1', filename: 'fn1', otherProperties: true},
            {name: 'sc2', filename: 'fn2', otherProperties: true}
          ];
        }
        return Em.get(App.config, param);
      });
      sinon.stub(App.config, 'configId', function (name, filename) {
        return name+filename;
      });
    });
    afterEach(function () {
      App.config.get.restore();
      App.config.configId.restore();
    });
    it('returns map with secure configs', function () {
      expect(App.config.get('preDefinedSitePropertiesMap')).to.eql({
        'sc1fn1': {name: 'sc1', filename: 'fn1', otherProperties: true},
        'sc2fn2': {name: 'sc2', filename: 'fn2', otherProperties: true}
      });
    });
  });

  describe('#shouldSupportFinal', function () {

    var cases = [
      {
        shouldSupportFinal: false,
        title: 'no service name specified'
      },
      {
        serviceName: 's0',
        shouldSupportFinal: false,
        title: 'no filename specified'
      },
      {
        serviceName: 'MISC',
        shouldSupportFinal: false,
        title: 'MISC'
      },
      {
        serviceName: 's0',
        filename: 's0-site',
        shouldSupportFinal: true,
        title: 'final attribute supported'
      },
      {
        serviceName: 's0',
        filename: 's0-env',
        shouldSupportFinal: false,
        title: 'final attribute not supported'
      },
      {
        serviceName: 'Cluster',
        filename: 'krb5-conf.xml',
        shouldSupportFinal: false,
        title: 'kerberos descriptor identities don\'t support final'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.StackService, 'find').returns([
        {
          serviceName: 's0'
        }
      ]);
      sinon.stub(App.config, 'getConfigTypesInfoFromService').returns({
        supportsFinal: ['s0-site']
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.config.getConfigTypesInfoFromService.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.shouldSupportFinal(item.serviceName, item.filename)).to.equal(item.shouldSupportFinal);
      });
    });

  });

  describe('#shouldSupportAddingForbidden', function () {

    var cases = [
      {
        shouldSupportAddingForbidden: false,
        title: 'no service name specified'
      },
      {
        serviceName: 's0',
        shouldSupportAddingForbidden: false,
        title: 'no filename specified'
      },
      {
        serviceName: 'MISC',
        shouldSupportAddingForbidden: false,
        title: 'MISC'
      },
      {
        serviceName: 's0',
        filename: 's0-site',
        shouldSupportAddingForbidden: true,
        title: 'adding forbidden supported'
      },
      {
        serviceName: 's0',
        filename: 's0-properties',
        shouldSupportAddingForbidden: false,
        title: 'adding forbidden not supported'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 's0',
          configTypes: {
            's0-size': {},
            's0-properties': {}
          }
        })
      ]);
      sinon.stub(App.config, 'getConfigTypesInfoFromService').returns({
        supportsAddingForbidden: ['s0-site']
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.config.getConfigTypesInfoFromService.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.shouldSupportAddingForbidden(item.serviceName, item.filename)).to.equal(item.shouldSupportAddingForbidden);
      });
    });

  });

  describe('#getConfigTypesInfoFromService', function () {
    it('get service config types info', function () {
      var input = Em.Object.create({
        configTypes: {
          't1': {
            'supports': {
              'final': 'true'
            }
          },
          't2': {
            'supports': {
              'adding_forbidden': 'true'
            }
          }
        }
      });
      var configTypesInfo = {
        items: ['t1', 't2'],
        supportsFinal: ['t1'],
        supportsAddingForbidden: ['t2']
      };
      expect(App.config.getConfigTypesInfoFromService(input)).to.eql(configTypesInfo);
    });
  });

  describe('#addYarnCapacityScheduler', function () {
    var input, res, configs, csConfig;
    res = {
      'value': 'n1=v1\nn2=v2\n',
      'serviceName': 'YARN',
      'savedValue': 'n1=sv1\nn2=sv2\n',
      'recommendedValue': 'n1=rv1\nn2=rv2\n',
      'isFinal': true,
      'savedIsFinal': true,
      'recommendedIsFinal': true,
      'category': 'CapacityScheduler',
      'displayName': 'Capacity Scheduler',
      'description': 'Capacity Scheduler properties',
      'displayType': 'capacityScheduler'
    };
    beforeEach(function () {
      sinon.stub(App.config, 'getPropertiesFromTheme').returns([]);
      input = [
        Em.Object.create({
          name: 'n1',
          value: 'v1',
          savedValue: 'sv1',
          recommendedValue: 'rv1',
          isFinal: true,
          savedIsFinal: true,
          recommendedIsFinal: true,
          filename: 'capacity-scheduler.xml'
        }),
        Em.Object.create({
          name: 'n2',
          value: 'v2',
          savedValue: 'sv2',
          recommendedValue: 'rv2',
          filename: 'capacity-scheduler.xml'
        }),
        Em.Object.create({
          name: 'n3',
          value: 'v3',
          savedValue: 'sv3',
          recommendedValue: 'sv2',
          filename: 'not-capacity-scheduler.xml'
        })
      ];

      configs = App.config.addYarnCapacityScheduler(input);
      csConfig = configs.findProperty('category', 'CapacityScheduler');
    });
    afterEach(function () {
      App.config.getPropertiesFromTheme.restore();
    });

    describe('check result config', function () {
      Object.keys(res).forEach(function (k) {
        it(k, function () {
          expect(csConfig.get(k)).to.eql(res[k]);
        });
      });
    });
  });

  describe('#textareaIntoFileConfigs', function () {
    var res, cs;
    beforeEach(function () {
      var stackConfigs = [
        Em.Object.create({
          name: 'n1',
          value: 'v1',
          savedValue: 'v1',
          serviceName: 'YARN',
          filename: 'capacity-scheduler.xml',
          isFinal: true,
          group: null
        })
      ];
      sinon.stub(App.configsCollection, 'getAll').returns(stackConfigs);
      res = [
        Em.Object.create({
          name: 'n1',
          value: 'v1',
          savedValue: 'v1',
          serviceName: 'YARN',
          filename: 'capacity-scheduler.xml',
          isFinal: true,
          isUserProperty: false,
          group: null
        }),
        Em.Object.create({
          name: 'n2',
          value: 'v2',
          savedValue: 'v2',
          serviceName: 'YARN',
          filename: 'capacity-scheduler.xml',
          isFinal: true,
          isUserProperty: true,
          group: null
        })
      ];

      cs = Em.Object.create({
        'value': 'n1=v1\nn2=v2',
        'serviceName': 'YARN',
        'savedValue': 'n1=sv1\nn2=sv2',
        'recommendedValue': 'n1=rv1\nn2=rv2',
        'isFinal': true,
        'savedIsFinal': true,
        'recommendedIsFinal': true,
        'name': 'capacity-scheduler',
        'category': 'CapacityScheduler',
        'displayName': 'Capacity Scheduler',
        'description': 'Capacity Scheduler properties',
        'displayType': 'capacityScheduler'
      });
    });

    afterEach(function () {
      App.configsCollection.getAll.restore();
    });

    it('generate capacity scheduler', function () {
      expect(App.config.textareaIntoFileConfigs([cs], 'capacity-scheduler.xml')).to.eql(res);
    });
  });

  describe('#removeRangerConfigs', function () {

    it('should remove ranger configs and categories', function () {
      var configs = [
        Em.Object.create({
          configs: [
            Em.Object.create({filename: 'filename'}),
            Em.Object.create({filename: 'ranger-filename'})
          ],
          configCategories: [
            Em.Object.create({name: 'ranger-name'}),
            Em.Object.create({name: 'name'}),
            Em.Object.create({name: 'also-ranger-name'})
          ]
        })
      ];
      App.config.removeRangerConfigs(configs);
      expect(configs).eql(
          [
            Em.Object.create({
              configs: [
                Em.Object.create({filename: 'filename'})
              ],
              configCategories: [
                Em.Object.create({name: 'name'})
              ]
            })
          ]
      );
    });

  });

  describe("#createOverride", function() {
    var template = {
      name: "p1",
      filename: "f1",
      value: "v1",
      recommendedValue: "rv1",
      savedValue: "sv1",
      isFinal: true,
      recommendedIsFinal: false,
      savedIsFinal: true
    };

    var configProperty = App.ServiceConfigProperty.create(template);

    var group = Em.Object.create({name: "group1", properties: []});

    Object.keys(template).forEach(function (key) {
      it(key, function () {
        var override = App.config.createOverride(configProperty, {}, group);
        if (['savedValue', 'savedIsFinal'].contains(key)) {
          expect(override.get(key)).to.equal(null);
        } else {
          expect(override.get(key)).to.equal(template[key]);
        }
      });
    });

    describe('overrides some values that should be different for override', function() {
      var override;
      beforeEach(function () {
        override = App.config.createOverride(configProperty, {}, group);
      });
      it('isOriginalSCP is false', function () {
        expect(override.get('isOriginalSCP')).to.be.false;
      });
      it('overrides is null', function () {
        expect(override.get('overrides')).to.be.null;
      });
      it('group is valid', function () {
        expect(override.get('group')).to.eql(group);
      });
      it('parentSCP is valid', function () {
        expect(override.get('parentSCP')).to.eql(configProperty);
      });
    });

    var overriddenTemplate = {
      value: "v2",
      recommendedValue: "rv2",
      savedValue: "sv2",
      isFinal: true,
      recommendedIsFinal: false,
      savedIsFinal: true
    };

    Object.keys(overriddenTemplate).forEach(function (key) {
      it('overrides some specific values `' + key + '`', function () {
        var override = App.config.createOverride(configProperty, overriddenTemplate, group);
        expect(override.get(key)).to.equal(overriddenTemplate[key]);
      });
    });

    it('throws error due to undefined configGroup', function() {
      expect(App.config.createOverride.bind(App.config, configProperty, {}, null)).to.throw(App.EmberObjectTypeError);
    });

    it('throws error due to undefined originalSCP', function() {
      expect(App.config.createOverride.bind(App.config, null, {}, group)).to.throw(App.ObjectTypeError);
    });

    describe('updates originalSCP object ', function() {

      var overridenTemplate2;
      var override;

      beforeEach(function () {
        configProperty.set('overrides', null);
        configProperty.set('overrideValues', []);
        configProperty.set('overrideIsFinalValues', []);
        overridenTemplate2 = {
          value: "v12",
          recommendedValue: "rv12",
          savedValue: "sv12",
          isFinal: true,
          recommendedIsFinal: false,
          savedIsFinal: false
        };
        override = App.config.createOverride(configProperty, overridenTemplate2, group);
      });

      it('overrides.0 is valid', function () {
        expect(configProperty.get('overrides')[0]).to.be.eql(override);
      });
      it('overrideValues is valid', function () {
        expect(configProperty.get('overrideValues')).to.be.eql([overridenTemplate2.savedValue]);
      });
      it('overrideIsFinalValues is valid', function () {
        expect(configProperty.get('overrideIsFinalValues')).to.be.eql([overridenTemplate2.savedIsFinal]);
      });

    });

    describe('overrides with empty string values', function () {

      beforeEach(function () {
        configProperty.set('overrides', [
          {
            savedValue: null,
            savedIsFinal: true
          },
          {
            savedValue: '',
            savedIsFinal: false
          },
          {
            savedValue: '1',
            savedIsFinal: false
          }
        ]);
        App.config.createOverride(configProperty, null, group);
      });

      it('values', function () {
        expect(configProperty.get('overrideValues')).to.eql(['', '1']);
      });

      it('isFinal', function () {
        expect(configProperty.get('overrideIsFinalValues')).to.eql([false, false]);
      });

    });
  });

  describe('#createCustomGroupConfig', function() {
    var override, configGroup, result;
    beforeEach(function () {
      sinon.stub(App.config, 'createDefaultConfig', function (name, filename) {
        return { propertyName: name, filename: filename };
      });
      configGroup = Em.Object.create({ name: 'cfgGroup1'});
      override = {
        propertyName: 'p1',
        filename: 'f1'
      };
      result = App.config.createCustomGroupConfig(override, configGroup);
    });
    afterEach(function () {
      App.config.createDefaultConfig.restore();
    });
    describe('createsCustomOverride', function () {
      var expected = {
        propertyName: 'p1',
        filename: 'f1',
        isOriginalSCP: false,
        overrides: null,
        parentSCP: null
      };
      Object.keys(expected).forEach(function (k) {
        it(k, function () {
          expect(result.get(k)).to.be.eql(expected[k]);
        });
      });
      it('config group is valid', function () {
        expect(result.get('group')).to.be.eql(configGroup);
      });
    });

    it('updates configGroup properties', function () {
      expect(configGroup.get('properties').findProperty('propertyName', 'p1')).to.eql(result);
    });

    it('throws error when input is not objects', function () {
      expect(App.config.createCustomGroupConfig.bind(App.config)).to.throw(App.ObjectTypeError);
      expect(App.config.createCustomGroupConfig.bind(App.config, {}, {})).to.throw(App.EmberObjectTypeError);
    });
  });

  describe('#getIsSecure', function() {
    var secureConfigs = App.config.get('secureConfigs');
    before(function() {
      App.config.set('secureConfigs', [{name: 'secureConfig'}]);
    });
    after(function() {
      App.config.set('secureConfigs', secureConfigs);
    });

    it('config is secure', function() {
      expect(App.config.getIsSecure('secureConfig')).to.equal(true);
    });
    it('config is not secure', function() {
      expect(App.config.getIsSecure('NotSecureConfig')).to.equal(false);
    });
  });

  describe('#getDefaultCategory', function() {
    it('returns custom category', function() {
      expect(App.config.getDefaultCategory(null, 'filename.xml')).to.equal('Custom filename');
    });
    it('returns advanced category', function() {
      expect(App.config.getDefaultCategory(Em.Object.create, 'filename.xml')).to.equal('Advanced filename');
    });
  });

  describe('#getDefaultDisplayType', function() {
    it('returns singleLine displayType', function() {
      expect(App.config.getDefaultDisplayType('v1')).to.equal('string');
    });
    it('returns multiLine displayType', function() {
      expect(App.config.getDefaultDisplayType('v1\nv2')).to.equal('multiLine');
    });
  });

  describe('#formatPropertyValue', function() {
    it('formatValue for componentHosts', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'componentHosts', value: "['h1','h2']"});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.eql(['h1','h2']);
    });

    it('formatValue for int', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4.0'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for int with m', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4m'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for float', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'float', value: '0.40'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('0.4');
    });

    it('formatValue for kdc_type', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'kdc_type', value: 'mit-kdc'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal(Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
    });

    it('don\'t format value', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'any', displayType: 'any', value: 'any'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('any');
    });
  });

  describe('#getPropertyIfExists', function() {
    [
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '1',
        m: 'use value from first object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '2',
        m: 'use value from second object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someOtherProperty: '2' },
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: false },
        secondObject: { someProperty: true },
        res: false,
        m: 'use value from first object, check booleans'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: 0 },
        secondObject: { someProperty: 1 },
        res: 0,
        m: 'use value from first object, check 0'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: '' },
        secondObject: { someProperty: '1' },
        res: '',
        m: 'use value from first object, check empty string'
      }
    ].forEach(function (t) {
      it(t.m, function () {
        expect(App.config.getPropertyIfExists(t.propertyName, t.defaultValue, t.firstObject, t.secondObject)).to.equal(t.res);
      })
    });
  });

  describe('#createDefaultConfig', function() {
    before(function() {
      sinon.stub(App.config, 'getDefaultDisplayType', function() {
        return 'pDisplayType';
      });
      sinon.stub(App.config, 'getDefaultCategory', function() {
        return 'pCategory';
      });
      sinon.stub(App.config, 'getIsSecure', function() {
        return false;
      });
      sinon.stub(App.config, 'shouldSupportFinal', function() {
        return true;
      });
      sinon.stub(App.config, 'get', function(param) {
        if (param === 'serviceByConfigTypeMap') {
          return { 'pFileName': Em.Object.create({serviceName: 'pServiceName' }) };
        }
        return Em.get(App.config, param);
      });
    });

    after(function() {
      App.config.getDefaultDisplayType.restore();
      App.config.getDefaultCategory.restore();
      App.config.getIsSecure.restore();
      App.config.shouldSupportFinal.restore();
      App.config.get.restore();
    });

    var res = {
      /** core properties **/
      id: 'pName__pFileName',
      name: 'pName',
      filename: 'pFileName.xml',
      value: '',
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: true,
      supportsAddingForbidden: false,
      serviceName: 'pServiceName',
      displayName: 'pName',
      displayType: 'pDisplayType',
      description: '',
      category: 'pCategory',
      isSecureConfig: false,
      showLabel: true,
      isVisible: true,
      isUserProperty: false,
      isRequired: true,
      group: null,
      isRequiredByAgent:  true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: Infinity,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      widgetType: null,
      errorMessage: '',
      warnMessage: ''
    };
    it('create default config object', function () {
      expect(App.config.createDefaultConfig('pName', 'pFileName', true)).to.eql(res);
    });
    it('getDefaultDisplayType is called', function() {
      expect(App.config.getDefaultDisplayType.called).to.be.true;
    });
    it('getDefaultCategory is called with correct arguments', function() {
      expect(App.config.getDefaultCategory.calledWith(true, 'pFileName')).to.be.true;
    });
    it('getIsSecure is called with correct arguments', function() {
      expect(App.config.getIsSecure.calledWith('pName')).to.be.true;
    });
    it('shouldSupportFinal is called with correct arguments', function() {
      expect(App.config.shouldSupportFinal.calledWith('pServiceName', 'pFileName')).to.be.true;
    });
  });

  describe('#mergeStaticProperties', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getPropertyIfExists', function(key, value) {return 'res_' + value});
    });

    afterEach(function() {
      App.config.getPropertyIfExists.restore();
    });

    var template = {
      name: 'pName',
      filename: 'pFileName',
      value: 'pValue',
      savedValue: 'pValue',
      isFinal: true,
      savedIsFinal: true,

      serviceName: 'pServiceName',
      displayName: 'pDisplayName',
      displayType: 'pDisplayType',
      category: 'pCategory'
    };

    var result = {
      name: 'pName',
      filename: 'pFileName',
      value: 'pValue',
      savedValue: 'pValue',
      isFinal: true,
      savedIsFinal: true,

      serviceName: 'res_pServiceName',
      displayName: 'res_pDisplayName',
      displayType: 'res_pDisplayType',
      category: 'res_pCategory'
    };

    it('called generate property object', function () {
      expect(App.config.mergeStaticProperties(template, {}, {})).to.eql(result);
    });
  });

  describe('#updateHostsListValue', function() {
    var tests = [
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1,host2'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        propertyType: 'yarn-site',
        hostsList: 'host1',
        e: 'host1'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        propertyType: 'yarn-site',
        hostsList: 'host2:10,host1:10',
        e: 'host1:10,host2:10'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10,host3:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        propertyType: 'yarn-site',
        hostsList: 'host2:10,host1:10',
        e: 'host2:10,host1:10'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10,host3:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        propertyType: 'yarn-site',
        hostsList: 'host2:10,host1:10,host3:10,host4:11',
        e: 'host2:10,host1:10,host3:10,host4:11'
      },
      {
        siteConfigs: {
          'hive.zookeeper.quorum': 'host1'
        },
        propertyName: 'some.new.property',
        propertyType: 'hive-site',
        hostsList: 'host2,host1:10',
        e: 'host2,host1:10'
      },
      {
        siteConfigs: {
          'some.new.property': '[\'host1\',\'host2\']'
        },
        propertyName: 'some.new.property',
        propertyType: 'property-type',
        hostsList: '[\'host1\',\'host2\']',
        isArray: true,
        e: '[\'host1\',\'host2\']',
        message: 'array-formatted property value with no changes'
      },
      {
        siteConfigs: {
          'some.new.property': '[\'host2\',\'host1\']'
        },
        propertyName: 'some.new.property',
        propertyType: 'property-type',
        hostsList: '[\'host1\',\'host2\']',
        isArray: true,
        e: '[\'host2\',\'host1\']',
        message: 'array-formatted property value with different hosts order'
      },
      {
        siteConfigs: {
          'some.new.property': '[\'host1\',\'host2\']'
        },
        propertyName: 'some.new.property',
        propertyType: 'property-type',
        hostsList: '[\'host3\',\'host4\']',
        isArray: true,
        e: '[\'host3\',\'host4\']',
        message: 'array-formatted property value with changes'
      },
      {
        siteConfigs: {
          'templeton.hive.properties': 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
        },
        propertyName: 'templeton.hive.properties',
        propertyType: 'webhcat-site',
        hostsList: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        e: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        message: 'templeton.hive.properties, no changes'
      },
      {
        siteConfigs: {
          'templeton.hive.properties': 'hive.metastore.local=false,hive.metastore.uris=thrift://host2:9083\\,thrift://host1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
        },
        propertyName: 'templeton.hive.properties',
        propertyType: 'webhcat-site',
        hostsList: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        e: 'hive.metastore.local=false,hive.metastore.uris=thrift://host2:9083\\,thrift://host1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        message: 'templeton.hive.properties, different hosts order'
      },
      {
        siteConfigs: {
          'templeton.hive.properties': 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9082\\,thrift://host2:9082,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
        },
        propertyName: 'templeton.hive.properties',
        propertyType: 'webhcat-site',
        hostsList: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        e: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        message: 'templeton.hive.properties, different ports'
      },
      {
        siteConfigs: {
          'templeton.hive.properties': 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
        },
        propertyName: 'templeton.hive.properties',
        propertyType: 'webhcat-site',
        hostsList: 'hive.metastore.local=false,hive.metastore.uris=thrift://host3:9083\\,thrift://host4:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        e: 'hive.metastore.local=false,hive.metastore.uris=thrift://host3:9083\\,thrift://host4:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        message: 'templeton.hive.properties, different hosts'
      },
      {
        siteConfigs: {
          'templeton.hive.properties': 'hive.metastore.local=false'
        },
        propertyName: 'templeton.hive.properties',
        propertyType: 'hive-site',
        hostsList: 'hive.metastore.local=true',
        e: 'hive.metastore.local=true',
        message: 'custom templeton.hive.properties'
      }
    ];

    tests.forEach(function(test) {
      var message = test.message
        || 'ZK located on {0}, current prop value is "{1}" "{2}" value should be "{3}"'
          .format(test.hostsList, ''+test.siteConfigs[test.propertyName], test.propertyName, test.e);

      describe(message, function () {
        var result;

        beforeEach(function () {
          result = App.config.updateHostsListValue(test.siteConfigs, test.propertyType, test.propertyName, test.hostsList, test.isArray);
        });

        it('returned value', function() {
          expect(result).to.be.eql(test.e);
        });

        it('value in configs object', function() {
          expect(test.siteConfigs[test.propertyName]).to.be.eql(test.e);
        });
      });
    });
  });

  describe('#createHostNameProperty', function () {
    it('create host property', function () {
      expect(App.config.createHostNameProperty('service1', 'component1', ['host1'], Em.Object.create({
        isMultipleAllowed: false,
        displayName: 'display name'
      }))).to.eql({
            "id": 'component1_host__service1-site',
            "name": 'component1_host',
            "displayName": 'display name host',
            "value": ['host1'],
            "recommendedValue": ['host1'],
            "description": "The host that has been assigned to run display name",
            "displayType": "componentHost",
            "isOverridable": false,
            "isRequiredByAgent": false,
            "serviceName": 'service1',
            "filename": "service1-site.xml",
            "category": 'component1',
            "index": 0
          })
    });

    it('create hosts property', function () {
      expect(App.config.createHostNameProperty('service1', 'component1', ['host1'], Em.Object.create({
        isMultipleAllowed: true,
        displayName: 'display name'
      }))).to.eql({
            "id": 'component1_hosts__service1-site',
            "name": 'component1_hosts',
            "displayName": 'display name host',
            "value": ['host1'],
            "recommendedValue": ['host1'],
            "description": "The hosts that has been assigned to run display name",
            "displayType": "componentHosts",
            "isOverridable": false,
            "isRequiredByAgent": false,
            "serviceName": 'service1',
            "filename": "service1-site.xml",
            "category": 'component1',
            "index": 0
          })
    });
  });

  describe("#truncateGroupName()", function() {

    it("name is empty", function() {
      expect(App.config.truncateGroupName('')).to.be.empty;
    });

    it("name has less than max chars", function() {
      expect(App.config.truncateGroupName('group1')).to.equal('group1');
    });

    it("name has more than max chars", function() {
      expect(App.config.truncateGroupName('group_has_more_than_max_characters')).to.equal('group_has...haracters');
    });
  });

  describe('#getComponentName', function () {
    [
      { configName: 'somename_host', componentName: 'SOMENAME' },
      { configName: 'somename_hosts', componentName: 'SOMENAME' },
      { configName: 'somenamehost', componentName: '' },
      { configName: 'somenamehosts', componentName: '' }
    ].forEach(function (t) {
      it('format config name ' + t.configName + ' to component ', function() {
        expect(App.config.getComponentName(t.configName)).to.equal(t.componentName);
      });
    });
  });

  describe('#getDescription', function () {

    it('should add extra-message to the description for `password`-configs', function () {
      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      expect(App.config.getDescription('', 'password')).to.contain(extraMessage);
    });

    it('should not add extra-message to the description if it already contains it', function () {

      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      var res = App.config.getDescription(extraMessage, 'password');
      expect(res).to.contain(extraMessage);
      expect(res).to.contain(extraMessage);
      var subd = res.replace(extraMessage, '');
      expect(subd).to.not.contain(extraMessage);
    });

    it('should add extra-message to the description if description is not defined', function () {

      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      expect(App.config.getDescription(undefined, 'password')).to.contain(extraMessage);
    });

  });

  describe('#parseDescriptor', function() {
    var input = {
      KerberosDescriptor: {
        kerberos_descriptor: {
          services: [
            {
              serviceName: 'serviceName',
              components: [
                { componentName: 'componentName2' },
                { componentName: 'componentName2' }
              ]
            }
          ]
        }
      }
    };


    beforeEach(function() {
      sinon.stub(App.config, 'parseIdentities');
      App.config.parseDescriptor(input);
    });

    afterEach(function() {
      App.config.parseIdentities.restore();
    });

    it('`parseIdentities` called 3 times', function () {
      expect(App.config.parseIdentities.calledThrice).to.be.true;
    });

    it('1st call', function () {
      expect(App.config.parseIdentities.calledWith({
        "serviceName": "serviceName",
        "components": [{"componentName": "componentName2"}, {"componentName": "componentName2"}]
      }, {})).to.be.true;
    });

    it('2nd call', function () {
      expect(App.config.parseIdentities.calledWith({ componentName: 'componentName2' })).to.be.true;
    });

    it('3rd call', function () {
      expect(App.config.parseIdentities.calledWith({ componentName: 'componentName2' })).to.be.true;
    });
  });

  describe('#parseIdentities', function() {
    var testObject = {
      identities: [
        {
          name: "/spnego"
        },
        {
          principal: {
            configuration: "hbase-env/hbase_principal_name",
            type: "user",
            local_username: "${hbase-env/hbase_user}",
            value: "${hbase-env/hbase_user}${principal_suffix}@${realm}"
          },
          name: "hbase",
          keytab: {
            owner: {
              access: "r",
              name: "${hbase-env/hbase_user}"
            },
            file: "${keytab_dir}/hbase.headless.keytab",
            configuration: "hbase-env/hbase_user_keytab",
            group: {
              access: "r",
              name: "${cluster-env/user_group}"
            }
          }
        },
        {
          name: "/smokeuser"
        }
      ]
    };
    var result = {
      "hbase_principal_name__hbase-env": true,
      "hbase_user_keytab__hbase-env": true
    };

    it('generates map with identities', function() {
      expect(App.config.parseIdentities(testObject, {})).to.eql(result);
    });
  });

  describe('#kerberosIdentitiesDescription', function () {
    it('update empty description', function() {
      expect(App.config.kerberosIdentitiesDescription()).to.eql(Em.I18n.t('services.service.config.secure.additionalDescription'));
    });

    it('update description for identities (without dot)', function() {
      expect(App.config.kerberosIdentitiesDescription('some text')).to.eql('some text. '
        + Em.I18n.t('services.service.config.secure.additionalDescription'));
    });

    it('update description for identities (with dot)', function() {
      expect(App.config.kerberosIdentitiesDescription('some text.')).to.eql('some text. '
        + Em.I18n.t('services.service.config.secure.additionalDescription'));
    });

    it('update description for identities (with dot and spaces at the end)', function() {
      expect(App.config.kerberosIdentitiesDescription('some text. ')).to.eql('some text. '
        + Em.I18n.t('services.service.config.secure.additionalDescription'));
    });
  });

  describe('#getTempletonHiveHosts', function () {
    var testCases = [
      {
        value: 'hive.metastore.local=false,hive.metastore.uris=thrift://host0:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        result: ['thrift://host0:9083'],
        message: 'one host'
      },
      {
        value: 'hive.metastore.local=false,hive.metastore.uris=thrift://host0:9083\\,thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        result: ['thrift://host0:9083', 'thrift://host1:9083', 'thrift://host2:9083'],
        message: 'several hosts'
      },
      {
        value: 'thrift://host0:9083\\,thrift://host1:9083\\,thrift://host2:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        result: ['thrift://host0:9083', 'thrift://host1:9083', 'thrift://host2:9083'],
        message: 'no leading text'
      },
      {
        value: 'hive.metastore.local=false,hive.metastore.uris=thrift://host0:9083\\,thrift://host1:9083\\,thrift://host2:9083',
        result: ['thrift://host0:9083', 'thrift://host1:9083', 'thrift://host2:9083'],
        message: 'no trailing text'
      },
      {
        value: 'hive.metastore.local=false',
        result: [],
        message: 'no hosts list'
      }
    ];

    testCases.forEach(function (test) {
      it(test.message, function () {
        expect(App.config.getTempletonHiveHosts(test.value)).to.eql(test.result);
      });
    });
  });

  describe('#isDirHeterogeneous', function () {
    it ('retruns true for dfs.datanode.data.dir', function () {
      expect(App.config.isDirHeterogeneous('dfs.datanode.data.dir')).to.be.true;
    });
    it ('retruns false not for dfs.datanode.data.dir', function () {
      expect(App.config.isDirHeterogeneous('any')).to.be.false;
    });
  });

  describe('#formatValue', function () {
    it('parses float', function () {
      expect(App.config.formatValue('0.400')).to.equal('0.4');
    });
    it('not parses float', function () {
      expect(App.config.formatValue('0.40x')).to.equal('0.40x');
    });
  });

  describe('#getStepConfigForProperty', function () {
    var input = [Em.Object.create({ configTypes: ['f1'] }), Em.Object.create({ configTypes: ['f2'] })];
    var output = input[0];
    it('returns stepConfig for fileName', function () {
      expect(App.config.getStepConfigForProperty(input, 'f1')).to.eql(output);
    });
  });

  describe('#sortConfigs', function () {
    var input = [{name: 'a', 'index': 2}, {name: 'b', 'index': 2}, {name: 'd', 'index': 1}];
    var output = [{name: 'd', 'index': 1}, {name: 'a', 'index': 2}, {name: 'b', 'index': 2}];
    it ('sort configs by index and name', function () {
      expect(App.config.sortConfigs(input)).to.eql(output);
    });
  });

  describe('#getViewClass', function () {});
  describe('#getErrorValidator', function () {});
  describe('#getWarningValidator', function () {});

  describe('#createServiceConfig', function () {
    var predefined = Em.Object.create({
      serviceName: 'serviceName1',
      displayName: 'displayName1',
      configCategories: [{name: 'configCategories1'}]
    });

    var configs = [Em.Object.create({name: 'c1'})];
    var configGroups = [Em.Object.create({name: 'cat1'})];

    beforeEach(function () {
      sinon.stub(App.config, 'get', function (param) {
        if (param === 'preDefinedServiceConfigs') {
          return [predefined];
        }
        return Em.get(App.config, param);
      });
    });

    afterEach(function () {
      App.config.get.restore();
    });

    describe('create service config object based on input', function () {
      var res = {
        serviceName: 'serviceName1',
        displayName: 'displayName1',
        configs: configs,
        configGroups: configGroups,
        initConfigsLength: 1,
        dependentServiceNames: []
      };
      Object.keys(res).forEach(function (k) {
        it(k, function () {
          expect(App.config.createServiceConfig('serviceName1', configGroups, configs, 1).get(k)).to.eql(res[k]);
        });
      });
      it('configCategories', function () {
        expect(App.config.createServiceConfig('serviceName1', configGroups, configs, 1).get('configCategories').mapProperty('name')).to.eql(['configCategories1']);
      });
    });

    describe('create default service config object', function () {
      var res = {
        serviceName: 'serviceName1',
        displayName: 'displayName1',
        configGroups: [],
        initConfigsLength: 0,
        dependentServiceNames: []
      };
      Object.keys(res).forEach(function (k) {
        it(k, function() {
          expect(App.config.createServiceConfig('serviceName1').get(k)).to.eql(res[k]);
        });
      });
      it('configCategories', function () {
        expect(App.config.createServiceConfig('serviceName1', configGroups, configs, 1).get('configCategories').mapProperty('name')).to.eql(['configCategories1']);
      });
    });
  });

  describe('#loadConfigsByTags', function () {
    it("App.ajax.send should be called", function() {
      App.config.loadConfigsByTags([
        { siteName: 't1', tagName: 'tag1' },
        { siteName: 't2', tagName: 'tag2' }
      ]);
      var args = testHelpers.findAjaxRequest('name', 'config.on_site');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(App.config);
      expect(args[0].data).to.be.eql({
        params : '(type=t1&tag=tag1)|(type=t2&tag=tag2)'
      });
    });
  });

  describe('#loadClusterConfigsFromStack', function () {
    it("App.ajax.send should be called", function() {
      App.config.loadClusterConfigsFromStack();
      var args = testHelpers.findAjaxRequest('name', 'configs.stack_configs.load.cluster_configs');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(App.config);
      expect(args[0].data).to.be.eql({
        stackVersionUrl: App.get('stackVersionURL')
      });
    });
  });

  describe('#loadConfigsFromStack', function () {
    it("App.ajax.send should be called with services", function() {
      App.config.loadConfigsFromStack(['s1']);
      var args = testHelpers.findAjaxRequest('name', 'configs.stack_configs.load.services');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(App.config);
      expect(args[0].data).to.be.eql({
        stackVersionUrl: App.get('stackVersionURL'),
        serviceList: 's1'
      });
    });

    it("App.ajax.send should be called all services", function() {
      App.config.loadConfigsFromStack();
      var args = testHelpers.findAjaxRequest('name', 'configs.stack_configs.load.all');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(App.config);
      expect(args[0].data).to.be.eql({
        stackVersionUrl: App.get('stackVersionURL'),
        serviceList: ''
      });
    });
  });

  describe('#saveConfigsToModel', function () {
    beforeEach(function () {
      sinon.stub(App.stackConfigPropertiesMapper, 'map');
    });
    afterEach(function () {
      App.stackConfigPropertiesMapper.map.restore();
    });
    it('runs mapper', function () {
      App.config.saveConfigsToModel({configs: 'configs'});
      expect(App.stackConfigPropertiesMapper.map.calledWith({configs: 'configs'})).to.be.true;
    });
  });

  describe('#findConfigProperty', function () {
    var stepConfigs = [
      Em.Object.create({
        configs: [
          { name: 'p1', filename: 'f1'},
          { name: 'p2', filename: 'f2'}
        ]
      }),
      Em.Object.create({
        configs: [
          { name: 'p3', filename: 'f3'}
        ]
      })
    ];

    it('find property', function () {
      expect(App.config.findConfigProperty(stepConfigs, 'p1', 'f1')).to.eql({ name: 'p1', filename: 'f1'});
    });

    it('do nothing because of whong params', function () {
      expect(App.config.findConfigProperty(stepConfigs)).to.be.false;
    });
  });

  describe('#getPropertiesFromTheme', function () {
    beforeEach(function () {
      sinon.stub(App.Tab , 'find').returns([
        Em.Object.create({
          serviceName: 'sName',
          isAdvanced: true,
          sections: [Em.Object.create({
            subSections: [
              Em.Object.create({ configProperties: [{name: 'p1'}] })
            ]
          })]
        }),
        Em.Object.create({
          serviceName: 'sName',
          isAdvanced: false,
          sections: [Em.Object.create({
            subSections: [
              Em.Object.create({ configProperties: [{name: 'p2'}] }),
              Em.Object.create({ configProperties: [{name: 'p3'}] })
            ]
          })]
        })
      ])
    });

    afterEach(function () {
      App.Tab.find.restore();
    });
    it('gets theme properties', function () {
      expect(App.config.getPropertiesFromTheme('sName')).to.eql([{name: 'p2'}, {name: 'p3'}])
    })
  })
});
