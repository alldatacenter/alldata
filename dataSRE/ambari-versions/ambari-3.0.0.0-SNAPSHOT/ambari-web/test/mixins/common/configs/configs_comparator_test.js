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

describe('App.ConfigsComparator', function() {
	var mixin;

	beforeEach(function() {
    mixin = Em.Object.create(App.ConfigsComparator, {
      compareServiceVersion: Em.Object.create({version: 'v1'}),
      content: Em.Object.create()
    });
	});

  describe("#loadCompareVersionConfigs()", function () {

    beforeEach(function() {
      this.mockIsDefault = sinon.stub(mixin, 'isVersionDefault');
      sinon.stub(mixin, 'getCompareVersionConfigs').returns({
        done: function(callback) {
          callback({});
          return this;
        },
        fail: Em.K
      });
      sinon.stub(mixin, 'initCompareConfig');
    });

    afterEach(function() {
      this.mockIsDefault.restore();
      mixin.getCompareVersionConfigs.restore();
      mixin.initCompareConfig.restore();
    });

    describe("#compareServiceVersion is null", function () {
      var config;

      beforeEach(function() {
        config = Em.Object.create();
        mixin.set('compareServiceVersion', null);
      });

      it("result should be true", function() {
        var dfd = mixin.loadCompareVersionConfigs([config]);
        dfd.done(function(result) {
          expect(result).to.be.false;
        })
      });

      it("isCompareMode should be false", function() {
        mixin.loadCompareVersionConfigs([config]);
        expect(mixin.get('isCompareMode')).to.be.false;
      });

      it("config.isComparison should be false", function() {
        mixin.loadCompareVersionConfigs([config]);
        expect(config.get('isComparison')).to.be.false;
      });
    });

    describe("#compareServiceVersion correct", function () {
      var config;

      beforeEach(function() {
        mixin.set('compareServiceVersion', Em.Object.create({
          version: 'v1'
        }));
        mixin.set('selectedVersion', 'v2');
        config = Em.Object.create();
      });

      it("getCompareVersionConfigs should be called, default", function() {
        this.mockIsDefault.returns(true);
        mixin.loadCompareVersionConfigs([config]);
        expect(mixin.getCompareVersionConfigs.calledWith(['v1'])).to.be.true;
      });

      it("getCompareVersionConfigs should be called, non-default", function() {
        this.mockIsDefault.returns(false);
        mixin.loadCompareVersionConfigs([config]);
        expect(mixin.getCompareVersionConfigs.calledWith(['v1', 'v2'])).to.be.true;
      });

      it("config.isEditable should be false", function() {
        mixin.loadCompareVersionConfigs([config]);
        expect(config.get('isEditable')).to.be.false;
      });

      it("initCompareConfig should be called", function() {
        mixin.loadCompareVersionConfigs([config]);
        expect(mixin.initCompareConfig.calledWith([config], {})).to.be.true;
      });

      it("isCompareMode should be true", function() {
        mixin.loadCompareVersionConfigs([config]);
        expect(mixin.get('isCompareMode')).to.be.true;
      });

      it("resolve should be true", function() {
        var dfd = mixin.loadCompareVersionConfigs([config]);
        dfd.done(function(result) {
          expect(result).to.be.true;
        });
      });
    });
  });

  describe("#initCompareConfig()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'addCompareConfigs');
      sinon.stub(mixin, 'addCompareCSConfigs');
      sinon.stub(mixin, 'getMockConfig').returns({name: 'mock'});
      mixin.setProperties({
        content: Em.Object.create({serviceName: 'S1'}),
        compareServiceVersion: Em.Object.create({version: 'v1'})
      });
    });

    afterEach(function() {
      mixin.addCompareConfigs.restore();
      mixin.addCompareCSConfigs.restore();
      mixin.getMockConfig.restore();
    });

    it("empty json", function() {
      var allConfigs = [
        {
          name: 'conf1'
        }
      ];
      mixin.initCompareConfig(allConfigs, {items: []});
      expect(mixin.addCompareConfigs.calledWith(false, allConfigs, {v1: {}})).to.be.true;
    });

    it("Capacity scheduler config", function() {
      var allConfigs = [
        {
          name: 'conf1'
        }
      ];
      var json = {
        items: [
          {
            configurations: [{
              type: 'capacity-scheduler'
            }]
          }
        ]
      };
      mixin.set('content.serviceName', 'YARN');
      mixin.set('selectedVersion', 'v2');
      mixin.initCompareConfig(allConfigs, json);
      expect(mixin.addCompareCSConfigs.calledWith({
          type: 'capacity-scheduler'
        }, {v1: {}}, {
        configurations: [{
          type: 'capacity-scheduler'
        }]
      })).to.be.true;
      expect(mixin.addCompareConfigs.calledWith(false, allConfigs, {v1: {}})).to.be.true;
    });

    it("simple config", function() {
      var allConfigs = [
        {
          name: 'prop1'
        }
      ];
      var json = {
        items: [
          {
            service_config_version: 'v1',
            configurations: [{
              type: 't1',
              tag: 'tag1',
              version: '1',
              properties: {
                prop1: 'val1'
              }
            }]
          },
          {
            service_config_version: 'v2',
            configurations: [{
              tag: 'tag2',
              type: 't2',
              version: '2',
              properties: {
                prop2: 'val2'
              },
              properties_attributes: {
                final: {
                  prop2: 'true'
                }
              }
            }]
          }
        ]
      };
      mixin.set('selectedVersion', 'v2');
      mixin.initCompareConfig(allConfigs, json);
      expect(mixin.addCompareConfigs.getCall(0).args[2]).to.be.eql({
        v1: {
          'prop1-t1': {
            filename: 't1.xml',
            name: 'prop1',
            value: 'val1',
            type: 't1',
            tag: 'tag1',
            version: '1',
            service_config_version: 'v1'
          }
        },
        v2: {
          'prop2-t2': {
            filename: 't2.xml',
            name: 'prop2',
            value: 'val2',
            type: 't2',
            tag: 'tag2',
            version: '2',
            service_config_version: 'v2',
            isFinal: true
          }
        }
      });
      expect(mixin.addCompareConfigs.getCall(0).args[1]).to.be.eql(
        [{name: 'prop1'}, {name: 'mock'}]
      );
      expect(mixin.addCompareConfigs.getCall(0).args[0]).to.be.true;
    });
  });

  describe("#addCompareConfigs()", function () {

    beforeEach(function() {
      mixin.set('compareServiceVersion.version', 'v1');
      mixin.set('selectedVersion', 'v2');
      sinon.stub(mixin, 'setCompareConfigs');
      sinon.stub(mixin, 'setCompareDefaultGroupConfig');
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('tag1');
    });

    afterEach(function() {
      mixin.setCompareConfigs.restore();
      mixin.setCompareDefaultGroupConfig.restore();
      App.config.getConfigTagFromFileName.restore();
    });

    it("non-default version, isRequiredByAgent=false", function() {
      mixin.addCompareConfigs(true, [{isRequiredByAgent: false}], {});
      expect(mixin.setCompareConfigs.called).to.be.false;
    });

    it("non-default version, isRequiredByAgent=true", function() {
      mixin.addCompareConfigs(true, [{isRequiredByAgent: true}], {});
      expect(mixin.setCompareConfigs.calledWith(
        {isRequiredByAgent: true},
        {},
        'v1',
        'v2'
      )).to.be.true;
    });

    it("default version, isRequiredByAgent=false", function() {
      mixin.addCompareConfigs(false, [{isRequiredByAgent: false}], {});
      expect(mixin.setCompareDefaultGroupConfig.called).to.be.false;
    });

    it("default version, isRequiredByAgent=true", function() {
      var serviceVersionMap = {
        'v1': {
          'c1-tag1': {
            name: 'c1'
          }
        }
      };
      mixin.addCompareConfigs(false, [{isRequiredByAgent: true, name: 'c1'}], serviceVersionMap);
      expect(mixin.setCompareDefaultGroupConfig.calledWith(
        {isRequiredByAgent: true, name: 'c1'},
        {name: 'c1'}
      )).to.be.true;
    });
  });

  describe("#method", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'getPropertiesFromTheme').returns(['prop1__type1']);
    });

    afterEach(function() {
      App.config.getPropertiesFromTheme.restore();
    });

    it("add skipped config", function() {
      var configuration = {
        properties: {
          prop1: 'val1'
        },
        tag: 'tag1',
        type: 'type1',
        version: '1'
      };
      var serviceVersionMap = {v1: {}};
      mixin.addCompareCSConfigs(configuration, serviceVersionMap, {service_config_version: 'v1'});
      expect(serviceVersionMap).to.be.eql({
        v1: {
          'prop1-type1': {
            name: 'prop1',
            value: 'val1',
            type: 'type1',
            tag: 'tag1',
            version: '1',
            service_config_version: 'v1'
          },
         'type1-type1': {
           name: 'type1',
           value: '',
           type: 'type1',
           tag: 'tag1',
           version: '1',
           service_config_version: 'v1'
         }
        }
      });
    });

    it("add config", function() {
      var configuration = {
        properties: {
          prop2: 'val2'
        },
        tag: 'tag1',
        type: 'type1',
        version: '1'
      };
      var serviceVersionMap = {v1: {}};
      mixin.addCompareCSConfigs(configuration, serviceVersionMap, {service_config_version: 'v1'});
      expect(serviceVersionMap).to.be.eql({
        v1: {
          'type1-type1': {
            name: 'type1',
            value: 'prop2=val2\n',
            type: 'type1',
            tag: 'tag1',
            version: '1',
            service_config_version: 'v1'
          }
        }
      });
    });
  });

  describe("#setCompareConfigs()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('file1');
      sinon.stub(mixin, 'getComparisonConfig', function(serviceConfig) {
        return serviceConfig;
      });
      sinon.stub(mixin, 'hasCompareDiffs').returns(false);
      sinon.stub(mixin, 'getMockComparisonConfig').returns({name: 'mock'});
    });

    afterEach(function() {
      App.config.getConfigTagFromFileName.restore();
      mixin.getComparisonConfig.restore();
      mixin.hasCompareDiffs.restore();
      mixin.getMockComparisonConfig.restore();
    });

    it("compare current with selected", function() {
      var serviceConfig = Em.Object.create({
        name: 'c1'
      });
      var serviceVersionMap = {
        'v1': {
          'c1-file1': {
            name: 'c1'
          }
        },
        'v2': {
          'c1-file1': {
            name: 'c1'
          }
        }
      };
      mixin.setCompareConfigs(serviceConfig, serviceVersionMap, 'v1', 'v2');
      expect(serviceConfig.get('compareConfigs').mapProperty('name')).to.be.eql(['c1', 'c1']);
      expect(serviceConfig.get('hasCompareDiffs')).to.be.false;
    });

    it("compare current with mock selected", function() {
      var serviceConfig = Em.Object.create({
        name: 'c1'
      });
      var serviceVersionMap = {
        'v1': {
          'c1-file1': {
            name: 'c1'
          }
        },
        'v2': {}
      };
      mixin.setCompareConfigs(serviceConfig, serviceVersionMap, 'v1', 'v2');
      expect(serviceConfig.get('compareConfigs').mapProperty('name')).to.be.eql(['c1', 'mock']);
      expect(serviceConfig.get('hasCompareDiffs')).to.be.true;
    });

    it("compare mock current with selected", function() {
      var serviceConfig = Em.Object.create({
        name: 'c1'
      });
      var serviceVersionMap = {
        'v1': {},
        'v2': {
          'c1-file1': {
            name: 'c1'
          }
        }
      };
      mixin.setCompareConfigs(serviceConfig, serviceVersionMap, 'v1', 'v2');
      expect(serviceConfig.get('compareConfigs').mapProperty('name')).to.be.eql(['mock', 'c1']);
      expect(serviceConfig.get('hasCompareDiffs')).to.be.true;
    });
  });

  describe("#getMockComparisonConfig()", function () {

    beforeEach(function() {
      sinon.stub(App.ServiceConfigVersion, 'find').returns({});
      sinon.stub(App.ServiceConfigProperty, 'create', function(object) {
        return object;
      });
    });

    afterEach(function() {
      App.ServiceConfigVersion.find.restore();
      App.ServiceConfigProperty.create.restore();
    });

    it("should return mock config object", function() {
      var serviceConfig = Em.Object.create();
      mixin.set('content.serviceName', 'S1');
      expect(mixin.getMockComparisonConfig(serviceConfig, 'v2')).to.be.eql(Em.Object.create({
        "isComparison": false,
        "isInstance": true,
        "isDestroyed": false,
        "isDestroying": false,
        "isObserverable": true,
        "isEditable": false,
        "serviceVersion": {},
        "isMock": true,
        "displayType": "label",
        "value": "Undefined"
      }));
      expect(App.ServiceConfigVersion.find.calledWith('S1_v2')).to.be.true;
    });
  });

  describe("#getComparisonConfig()", function () {

    beforeEach(function() {
      sinon.stub(App.ServiceConfigVersion, 'find').returns({});
      sinon.stub(App.ServiceConfigProperty, 'create', function(object) {
        return object;
      });
      sinon.stub(App.config, 'formatPropertyValue').returns('val');
    });

    afterEach(function() {
      App.ServiceConfigVersion.find.restore();
      App.ServiceConfigProperty.create.restore();
      App.config.formatPropertyValue.restore();
    });

    it("should return comparison config object", function() {
      var serviceConfig = Em.Object.create();
      var compareConfig = {service_config_version: 'v2', isFinal: false};
      mixin.set('content.serviceName', 'S1');

      expect(mixin.getComparisonConfig(serviceConfig, compareConfig)).to.be.eql(Em.Object.create({
        "isComparison": false,
        "isOriginalSCP": false,
        "isInstance": true,
        "isDestroyed": false,
        "isDestroying": false,
        "isObserverable": true,
        "isEditable": false,
        "serviceVersion": {},
        "isFinal": false,
        "value": "val",
        "compareConfigs": null
      }));
      expect(App.ServiceConfigVersion.find.calledWith('S1_v2')).to.be.true;
    });

    it("serviceConfig is Mock", function() {
      var serviceConfig = Em.Object.create({isMock: true});
      var compareConfig = {service_config_version: 'v2', isFinal: false};
      mixin.set('content.serviceName', 'S1');

      expect(mixin.getComparisonConfig(serviceConfig, compareConfig)).to.be.eql(Em.Object.create({
        "isComparison": false,
        "isOriginalSCP": false,
        "isMock": false,
        "isInstance": true,
        "isDestroyed": false,
        "isDestroying": false,
        "isObserverable": true,
        "isEditable": false,
        "displayType": "string",
        "serviceVersion": {},
        "isFinal": false,
        "value": "val",
        "compareConfigs": null
      }));
      expect(App.ServiceConfigVersion.find.calledWith('S1_v2')).to.be.true;
    });

    it("compareConfig is null", function() {
      var serviceConfig = Em.Object.create({isMock: true});
      mixin.set('content.serviceName', 'S1');

      expect(mixin.getComparisonConfig(serviceConfig, null)).to.be.eql(Em.Object.create({
        "isComparison": false,
        "isOriginalSCP": false,
        "isMock": true,
        "isInstance": true,
        "isDestroyed": false,
        "isDestroying": false,
        "isObserverable": true,
        "isEditable": false
      }));
      expect(App.ServiceConfigVersion.find.called).to.be.false;
    });
  });

  describe("#setCompareDefaultGroupConfig()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getComparisonConfig').returns({name: 'c1'});
      sinon.stub(mixin, 'getMockComparisonConfig').returns({name: 'mock'});
      sinon.stub(mixin, 'hasCompareDiffs').returns(true);
    });

    afterEach(function() {
      mixin.getComparisonConfig.restore();
      mixin.getMockComparisonConfig.restore();
      mixin.hasCompareDiffs.restore()
    });

    it("compareConfig correct, isMock=true", function() {
      var serviceConfig = Em.Object.create({
        isMock: true
      });
      expect(mixin.setCompareDefaultGroupConfig(serviceConfig, {})).to.be.eql(Em.Object.create({
        "isMock": true,
        "compareConfigs": [
          {name: 'c1'}
        ],
        "isComparison": true,
        "hasCompareDiffs": true
      }));
    });

    it("compareConfig correct, isMock=false", function() {
      var serviceConfig = Em.Object.create({
        isMock: false
      });
      expect(mixin.setCompareDefaultGroupConfig(serviceConfig, {})).to.be.eql(Em.Object.create({
        "isMock": false,
        "compareConfigs": [
          {name: 'c1'}
        ],
        "isComparison": true,
        "hasCompareDiffs": true
      }));
    });

    it("compareConfig is null, isUserProperty=true", function() {
      var serviceConfig = Em.Object.create({
        isUserProperty: true
      });
      expect(mixin.setCompareDefaultGroupConfig(serviceConfig, null)).to.be.eql(Em.Object.create({
        "isUserProperty": true,
        "compareConfigs": [
          {
            "name": "mock"
          }
        ],
        "isComparison": true,
        "hasCompareDiffs": true
      }));
    });

    it("compareConfig is null, isUserProperty=false", function() {
      var serviceConfig = Em.Object.create({
        isUserProperty: false,
        isRequiredByAgent: true
      });
      expect(mixin.setCompareDefaultGroupConfig(serviceConfig, null)).to.be.eql(Em.Object.create({
        "isUserProperty": false,
        "isRequiredByAgent": true,
        "compareConfigs": [
          {
            "name": "mock"
          }
        ],
        "isComparison": true,
        "hasCompareDiffs": true
      }));
    });
  });

  describe("#hasCompareDiffs()", function () {

    var testCases = [
      {
        originalConfig: Em.Object.create({
          displayType: 'password'
        }),
        compareConfig: Em.Object.create({
        }),
        title: 'original config is password',
        expected: false
      },
      {
        originalConfig: Em.Object.create({
          value: [{}]
        }),
        compareConfig: Em.Object.create({
          value: [{}]
        }),
        title: 'has equal Array values',
        expected: false
      },
      {
        originalConfig: Em.Object.create({
          value: Em.A([{}])
        }),
        compareConfig: Em.Object.create({
          value: Em.A([{}])
        }),
        title: 'has equal Em.A() values',
        expected: false
      },
      {
        originalConfig: Em.Object.create({
          value: [Em.Object.create({value: 'val1'})]
        }),
        compareConfig: Em.Object.create({
          value: [Em.Object.create({value: 'val2'})]
        }),
        title: 'has equal Em.A() values',
        expected: true
      },
      {
        originalConfig: Em.Object.create({
          value: [Em.Object.create({value: 'val1', isFinal: true})]
        }),
        compareConfig: Em.Object.create({
          value: [Em.Object.create({value: 'val1', isFinal: false})]
        }),
        title: 'has equal Em.A() values',
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it(test.title, function() {
        expect(mixin.hasCompareDiffs(test.originalConfig, test.compareConfig)).to.be.equal(test.expected);
      });
    });
  });

  describe("#getMockConfig()", function () {

    beforeEach(function() {
      sinon.stub(App.ServiceConfigProperty, 'create', function(config) {
        return config;
      });
      sinon.stub(App.config, 'getDefaultCategory').returns({});
      this.mock = sinon.stub(App.configsCollection, 'getConfigByName');
    });

    afterEach(function() {
      App.ServiceConfigProperty.create.restore();
      App.config.getDefaultCategory.restore();
      App.configsCollection.getConfigByName.restore();
    });

    it("get config from collection", function() {
      this.mock.returns({name: 'c1'});
      expect(mixin.getMockConfig('c1', 'S1', 'f1')).to.be.eql({
        name: 'c1'
      });
    });

    it("get mock config", function() {
      this.mock.returns(null);
      expect(mixin.getMockConfig('c1', 'S1', 'f1')).to.be.eql({
        description: 'c1',
        displayName: 'c1',
        isOverridable: false,
        isReconfigurable: false,
        isRequired: false,
        isRequiredByAgent: true,
        isSecureConfig: false,
        isUserProperty: true,
        isVisible: true,
        isOriginalSCP: false,
        name: 'c1',
        filename: 'f1',
        serviceName: 'S1',
        category: {},
        value: Em.I18n.t('common.property.undefined'),
        isMock: true,
        displayType: 'label'
      });
    });
  });

  describe("#getCompareVersionConfigs()", function () {

    it("App.ajax.send should be called", function() {
      mixin.set('content.serviceName', 'S1');
      mixin.getCompareVersionConfigs('v2');
      expect(testHelpers.findAjaxRequest('name', 'service.serviceConfigVersions.get.multiple')[0]).to.eql({
        name: 'service.serviceConfigVersions.get.multiple',
        sender: mixin,
        data: {
          serviceName: 'S1',
          serviceConfigVersions: 'v2'
        }
      });
      expect(mixin.get('versionLoaded')).to.be.false;
    });
  });
});

