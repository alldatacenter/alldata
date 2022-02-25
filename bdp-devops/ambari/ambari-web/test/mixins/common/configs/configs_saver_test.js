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

describe('App.ConfigsSaverMixin', function() {
  var mixin;

  beforeEach(function() {
    mixin = Em.Object.create(App.ConfigsSaverMixin, {
      content: Em.Object.create(),
      showChangedDependentConfigs: Em.K,
      serverSideValidation: Em.K,
      getHash: Em.K,
      clearAllRecommendations: Em.K
    });
  });

  describe("#currentServices", function () {

    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns({});
    });

    afterEach(function() {
      App.StackService.find.restore();
    });

    it("should return currentServices", function() {
      mixin.set('content.serviceName', 'S1');
      expect(mixin.get('currentServices')).to.be.eql([{}]);
      expect(App.StackService.find.calledWith('S1')).to.be.true;
    });
  });

  describe('#allowSaveCoreSite()', function () {
    var allowedType = 'CORETYPE';
    var allowedService = ['S1'];
    var stackServices = [
      Em.Object.create({
        serviceName: 'S4',
        serviceType: 'CORETYPE'
      }),
      Em.Object.create({
        serviceName: 'S1',
        serviceType: 'SOMEOTHERTYPE'
      }),
      Em.Object.create({
        serviceName: 'S2',
        serviceType: 'SOMEOTHERTYPE'
      })
    ];
    beforeEach(function () {
      mixin.setProperties({
        'content': {},
        'coreSiteServiceType': allowedType,
        'coreSiteServiceNames': allowedService
      });
    });

    [{
      currentServices: stackServices[0],
      res: true,
      m: 'service type is ok'
    }, {
      currentServices: stackServices[1],
      res: true,
      m: 'service name is ok'
    }, {
      currentServices: stackServices[2],
      res: false,
      m: 'not ok'
    }].forEach(function (c, index) {
        describe(c.m, function () {
          beforeEach(function () {
            mixin.reopen({
              currentServices: c.currentServices
            });
            it('test #' + index, function () {
              expect(mixin.allowSaveCoreSite()).to.be.equal(c.res);
            });
          });
        });
      });
  });

  describe('allowSaveSite', function() {
    [
      { fName: 'mapred-queue-acls', res: false, m: 'file name is restricted to be saved' },
      { fName: 'core-site', res: true, allowSaveCoreSite: true, m: 'core site is allowed to be saved' },
      { fName: 'core-site', res: false, allowSaveCoreSite: false, m: 'core site is not allowed to be saved' },
      { fName: 'other-file-name', res: true, m: 'file name has not restriction rule, so can be saved' }
    ].forEach(function (c, index) {
        describe(c.m, function () {
          beforeEach(function() {
            sinon.stub(mixin, 'allowSaveCoreSite').returns(c.allowSaveCoreSite);
          });
          afterEach(function() {
            mixin.allowSaveCoreSite.restore();
          });
          it('test #' + index, function () {
            expect(mixin.allowSaveSite(c.fName)).to.equal(c.res);
          });
        });
      });
  });

  describe('#createDesiredConfig()', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'formatValueBeforeSave', function(property) {
        return property.get('value');
      })
    });
    afterEach(function() {
      mixin.formatValueBeforeSave.restore();
    });

    it('generates config wil throw error', function() {
      expect(mixin.createDesiredConfig.bind(mixin)).to.throw(Error, 'assertion failed');
    });

    it('generates config without properties', function() {
      expect(mixin.createDesiredConfig('type1')).to.eql({
        "type": 'type1',
        "properties": {},
        "service_config_version_note": ""
      })
    });

    it('generates config with properties', function() {
      expect(mixin.createDesiredConfig('type1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true}), Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: true})], "note")).to.eql({
        "type": 'type1',
        "properties": {
          "p1": 'v1',
          "p2": 'v2'
        },
        "service_config_version_note": 'note'
      })
    });

    it('generates config with properties and skip isRequiredByAgent', function() {
      expect(mixin.createDesiredConfig('type1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true}), Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: false})], "note")).to.eql({
        "type": 'type1',
        "properties": {
          p1: 'v1'
        },
        "service_config_version_note": 'note'
      })
    });

    it('generates config with properties and skip service_config_version_note', function() {
      expect(mixin.createDesiredConfig('type1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true})], "note", true)).to.eql({
        "type": 'type1',
        "properties": {
          p1: 'v1'
        }
      })
    });

    it('generates config with final, password, user, group, text, additional_user_property, not_managed_hdfs_path, value_from_property_file', function() {
      expect(mixin.createDesiredConfig('type1', [
          Em.Object.create({name: 'p1', value: 'v1', isFinal: true, isRequiredByAgent: true}),
          Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: true}),
          Em.Object.create({name: 'p3', value: 'v3', isRequiredByAgent: true, propertyType: ["PASSWORD", "USER", "GROUP"]}),
          Em.Object.create({name: 'p4', value: 'v4', isRequiredByAgent: true, propertyType: ["PASSWORD", "TEXT", "ADDITIONAL_USER_PROPERTY"]}),
          Em.Object.create({name: 'p5', value: 'v5', isRequiredByAgent: true, propertyType: ["NOT_MANAGED_HDFS_PATH"]}),
          Em.Object.create({name: 'p6', value: 'v6', isRequiredByAgent: true, propertyType: ["TEXT", "VALUE_FROM_PROPERTY_FILE"]}),
          Em.Object.create({name: 'p7', value: 'v7', isRequiredByAgent: true, propertyType: ["PASSWORD"]})
        ], "note")).to.eql({
        "type": 'type1',
        "properties": {
          p1: 'v1',
          p2: 'v2',
          p3: 'v3',
          p4: 'v4',
          p5: 'v5',
          p6: 'v6',
          p7: 'v7'
        },
        "properties_attributes": {
          final: {
            'p1': "true"
          },
          password: {
            "p3": "true",
            "p4": "true",
            "p7": "true"
          },
          user: {
            "p3": "true"
          },
          group: {
            "p3": "true"
          },
          text: {
            "p4": "true",
            "p6": "true"
          },
          additional_user_property: {
            "p4": "true"
          },
          not_managed_hdfs_path: {
            "p5": "true"
          },
          value_from_property_file: {
            "p6": "true"
          }
        },
        "service_config_version_note": 'note'
      })
    })
  });

  describe('#generateDesiredConfigsJSON()', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'createDesiredConfig', function(type) {
        return 'desiredConfig_' + type;
      });
      sinon.stub(mixin, 'allowSaveSite', function() {
        return true;
      });

    });
    afterEach(function() {
      mixin.createDesiredConfig.restore();
      mixin.allowSaveSite.restore();
    });

    it('generates empty array as data is missing', function() {
      expect(mixin.generateDesiredConfigsJSON()).to.eql([]);
      expect(mixin.generateDesiredConfigsJSON(1,1)).to.eql([]);
      expect(mixin.generateDesiredConfigsJSON([],[])).to.eql([]);
    });

    it('generates array with desired configs', function() {
      expect(mixin.generateDesiredConfigsJSON([Em.Object.create({'name': 'p1', 'fileName': 'f1.xml'})], ['f1'])).to.eql(['desiredConfig_f1']);
      expect(mixin.createDesiredConfig).to.be.calledOnce
    })
  });

  describe('#getModifiedConfigs', function () {
    var configs = [
      Em.Object.create({
        name: 'p1',
        filename: 'f1',
        isNotDefaultValue: true,
        value: 'v1'
      }),
      Em.Object.create({
        name: 'p2',
        filename: 'f1',
        isNotDefaultValue: false,
        value: 'v2'
      }),
      Em.Object.create({
        name: 'p3',
        filename: 'f2',
        isNotSaved: true,
        value: 'v4'
      }),
      Em.Object.create({
        name: 'p4',
        filename: 'f3',
        isNotDefaultValue: false,
        isNotSaved: false,
        value: 'v4'
      })
    ];
    it('filter out changed configs', function () {
      expect(mixin.getModifiedConfigs(configs).mapProperty('name')).to.eql(['p1','p2','p3']);
      expect(mixin.getModifiedConfigs(configs).mapProperty('filename').uniq()).to.eql(['f1','f2']);
    });

    it('filter out changed configs and modifiedFileNames', function () {
      mixin.set('modifiedFileNames', ['f3']);
      expect(mixin.getModifiedConfigs(configs).mapProperty('name')).to.eql(['p1','p2','p3','p4']);
      expect(mixin.getModifiedConfigs(configs).mapProperty('filename').uniq()).to.eql(['f1','f2','f3']);
    });
  });

  describe("#getGroupFromModel()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.ServiceConfigGroup, 'find');
    });

    afterEach(function() {
      App.ServiceConfigGroup.find.restore();
    });

    it("should return selectedConfigGroup", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S1'
      }));
      mixin.set('selectedConfigGroup', Em.Object.create({name: 'g1'}));
      expect(mixin.getGroupFromModel('S1')).to.be.eql(Em.Object.create({name: 'g1'}));
    });

    it("selectedConfigGroup.isDefault=true, service has no groups", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S1'
      }));
      this.mock.returns([]);
      mixin.set('selectedConfigGroup', Em.Object.create({name: 'g1', isDefault: true}));
      expect(mixin.getGroupFromModel('S2')).to.be.null;
    });

    it("selectedConfigGroup.isDefault=true, service has groups", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S1'
      }));
      this.mock.returns([Em.Object.create({serviceName: 'S2', isDefault: true})]);
      mixin.set('selectedConfigGroup', Em.Object.create({name: 'g1', isDefault: true}));
      expect(mixin.getGroupFromModel('S2')).to.be.eql(Em.Object.create({serviceName: 'S2', isDefault: true}));
    });

    it("selectedConfigGroup.isDefault=false, service has no groups", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S1'
      }));
      this.mock.returns([]);
      mixin.set('selectedConfigGroup', Em.Object.create({name: 'g1', isDefault: false}));
      expect(mixin.getGroupFromModel('S2')).to.be.null;
    });

    it("selectedConfigGroup.isDefault=false, service has groups", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S1'
      }));
      this.mock.returns([Em.Object.create({serviceName: 'S2', isDefault: true, name: 'g2'})]);
      mixin.set('selectedConfigGroup', Em.Object.create({
        name: 'g1',
        isDefault: false,
        dependentConfigGroups: {
          'S2': 'g2'
        }
      }));
      expect(mixin.getGroupFromModel('S2')).to.be.eql(Em.Object.create({
        serviceName: 'S2',
        isDefault: true,
        name: 'g2'
      }));
    });
  });

  describe("#saveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'saveConfigsForDefaultGroup');
      sinon.stub(mixin, 'saveConfigsForNonDefaultGroup');
    });

    afterEach(function() {
      mixin.saveConfigsForDefaultGroup.restore();
      mixin.saveConfigsForNonDefaultGroup.restore();
    });

    it("saveConfigsForDefaultGroup should be called", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin.saveConfigs();
      expect(mixin.saveConfigsForDefaultGroup.calledOnce).to.be.true;
    });

    it("saveConfigsForNonDefaultGroup should be called", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      mixin.saveConfigs();
      expect(mixin.saveConfigsForNonDefaultGroup.calledOnce).to.be.true;
    });
  });

  describe("#saveConfigsForNonDefaultGroup()", function () {

    beforeEach(function() {
      this.mockGroup = sinon.stub(mixin, 'getGroupFromModel');
      this.mockConfigs = sinon.stub(mixin, 'getConfigsForGroup');
      sinon.stub(mixin, 'saveGroup');
      sinon.stub(mixin, 'isOverriddenConfigsModified').returns(true);
    });

    afterEach(function() {
      this.mockGroup.restore();
      this.mockConfigs.restore();
      mixin.saveGroup.restore();
      mixin.isOverriddenConfigsModified.restore();
    });

    it("configGroup is null", function() {
      mixin.set('stepConfigs', [Em.Object.create()]);
      this.mockGroup.returns(null);
      mixin.saveConfigsForNonDefaultGroup();
      expect(mixin.saveGroup.called).to.be.false;
    });

    it("configGroup is default", function() {
      mixin.set('stepConfigs', [Em.Object.create()]);
      this.mockGroup.returns(Em.Object.create({isDefault: true}));
      mixin.saveConfigsForNonDefaultGroup();
      expect(mixin.saveGroup.called).to.be.false;
    });

    it("overriddenConfigs is null", function() {
      mixin.set('stepConfigs', [Em.Object.create()]);
      this.mockGroup.returns(Em.Object.create({isDefault: false}));
      this.mockConfigs.returns(null);
      mixin.saveConfigsForNonDefaultGroup();
      expect(mixin.saveGroup.called).to.be.false;
    });

    it("saveGroup should be called, current service", function() {
      mixin.set('stepConfigs', [Em.Object.create({serviceName: 'S1'})]);
      mixin.set('serviceConfigVersionNote', 'note');
      mixin.set('content.serviceName', 'S1');
      this.mockGroup.returns(Em.Object.create({isDefault: false}));
      this.mockConfigs.returns([{}]);
      mixin.saveConfigsForNonDefaultGroup();
      expect(mixin.saveGroup.calledWith(
        [{}],
        Em.Object.create({isDefault: false}),
        'note',
        'putConfigGroupChangesSuccess'
      )).to.be.true;
    });

    it("saveGroup should be called, another service", function() {
      mixin.set('stepConfigs', [Em.Object.create({serviceName: 'S1'})]);
      mixin.set('serviceConfigVersionNote', 'note');
      mixin.set('content.serviceName', 'S2');
      this.mockGroup.returns(Em.Object.create({isDefault: false}));
      this.mockConfigs.returns([{}]);
      mixin.saveConfigsForNonDefaultGroup();
      expect(mixin.saveGroup.calledWith(
        [{}],
        Em.Object.create({isDefault: false}),
        'note',
        null
      )).to.be.true;
    });
  });

  describe("#saveConfigsForDefaultGroup()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getServiceConfigToSave').returns({});
      sinon.stub(mixin, 'putChangedConfigurations');
      sinon.stub(mixin, 'onDoPUTClusterConfigurations');
    });

    afterEach(function() {
      mixin.getServiceConfigToSave.restore();
      mixin.putChangedConfigurations.restore();
      mixin.onDoPUTClusterConfigurations.restore();
    });

    it("putChangedConfigurations should be called", function() {
      mixin.set('stepConfigs', [Em.Object.create()]);
      mixin.saveConfigsForDefaultGroup();
      expect(mixin.putChangedConfigurations.calledWith([{}], 'doPUTClusterConfigurationSiteSuccessCallback')).to.be.true;
    });

    it("onDoPUTClusterConfigurations should be called", function() {
      mixin.set('stepConfigs', []);
      mixin.saveConfigsForDefaultGroup();
      expect(mixin.onDoPUTClusterConfigurations.calledOnce).to.be.true;
    });
  });

  describe("#completeSave()", function () {

    it("saveInProgress should be false", function() {
      mixin.completeSave();
      expect(mixin.get('saveInProgress')).to.be.false;
    });
  });

  describe("#showWarningPopupsBeforeSave()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(mixin, 'isDirChanged');
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
      sinon.stub(mixin, 'showChangedDependentConfigs', function(arg1, callback) {
        callback();
      });
      sinon.stub(mixin, 'restartServicePopup');
    });

    afterEach(function() {
      this.mock.restore();
      App.showConfirmationPopup.restore();
      mixin.showChangedDependentConfigs.restore();
      mixin.restartServicePopup.restore();
    });

    it("directory changed", function() {
      this.mock.returns(true);
      mixin.showWarningPopupsBeforeSave();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(mixin.showChangedDependentConfigs.calledOnce).to.be.true;
      expect(mixin.restartServicePopup.calledOnce).to.be.true;
    });

    it("directory didn't change", function() {
      this.mock.returns(false);
      mixin.showWarningPopupsBeforeSave();
      expect(mixin.showChangedDependentConfigs.calledOnce).to.be.true;
      expect(mixin.restartServicePopup.calledOnce).to.be.true;
    });
  });

  describe("#restartServicePopup()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'serverSideValidation').returns({
        done: Em.K,
        fail: Em.K
      });
    });

    afterEach(function() {
      mixin.serverSideValidation.restore();
    });

    it("serverSideValidation should be called", function() {
      mixin.restartServicePopup();
      expect(mixin.serverSideValidation.calledOnce).to.be.true;
    });
  });

  describe("#getConfigsForGroup()", function () {

    it("empty stepConfigs", function() {
      var stepConfigs = [];
      expect(mixin.getConfigsForGroup(stepConfigs, 'g1')).to.be.empty;
    });

    it("stepConfigs has overrides", function() {
      var stepConfigs = [Em.Object.create({
        overrides: [
          Em.Object.create({
            group: {name: 'g1'}
          })
        ]
      })];
      expect(mixin.getConfigsForGroup(stepConfigs, 'g1')).to.be.eql([
        Em.Object.create({
          group: {name: 'g1'}
        })
      ]);
    });

    it("stepConfigs has group", function() {
      var stepConfigs = [
        Em.Object.create({
          group: {name: 'g1'}
        }),
        Em.Object.create({
          group: {name: 'g2'}
        })
      ];
      expect(mixin.getConfigsForGroup(stepConfigs, 'g1')).to.be.eql([
        Em.Object.create({
          group: {name: 'g1'}
        })
      ]);
    });
  });

  describe("#saveSiteConfigs()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'formatConfigValues');
    });

    afterEach(function() {
      mixin.formatConfigValues.restore();
    });

    it("formatConfigValues should be called", function() {
      expect(mixin.saveSiteConfigs([{}])).to.be.eql([{}]);
      expect(mixin.formatConfigValues.calledWith([{}])).to.be.true;
    });
  });

  describe("#allowSaveCoreSite()", function () {

    it("empty currentServices", function() {
      mixin.set('currentServices', []);
      expect(mixin.allowSaveCoreSite()).to.be.false;
    });

    it("coreSiteServiceNames has current service", function() {
      mixin.set('coreSiteServiceNames', ['S1']);
      mixin.reopen({
        currentServices: [Em.Object.create({serviceName: 'S1'})]
      });
      expect(mixin.allowSaveCoreSite()).to.be.true;
    });

    it("coreSiteServiceType equal currentService", function() {
      mixin.set('coreSiteServiceType', 't1');
      mixin.reopen({
        currentServices: [Em.Object.create({serviceName: 'S1', serviceType: 't1'})]
      });
      expect(mixin.allowSaveCoreSite()).to.be.true;
    });
  });

  describe("#clearSaveInfo()", function () {

    it("modifiedFileNames should be empty", function() {
      mixin.clearSaveInfo();
      expect(mixin.get('modifiedFileNames')).to.be.empty;
    });
  });

  describe("#saveStepConfigs()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'startSave');
      sinon.stub(mixin, 'showWarningPopupsBeforeSave');
    });

    afterEach(function() {
      mixin.startSave.restore();
      mixin.showWarningPopupsBeforeSave.restore();
    });

    it("isSubmitDisabled = false", function() {
      mixin.set('isSubmitDisabled', false);
      mixin.saveStepConfigs();
      expect(mixin.startSave.calledOnce).to.be.true;
      expect(mixin.showWarningPopupsBeforeSave.calledOnce).to.be.true;
    });

    it("isSubmitDisabled = true", function() {
      mixin.set('isSubmitDisabled', true);
      mixin.saveStepConfigs();
      expect(mixin.startSave.called).to.be.false;
      expect(mixin.showWarningPopupsBeforeSave.called).to.be.false;
    });
  });

  describe("#startSave()", function () {

    it("saveInProgress should be true", function() {
      mixin.startSave();
      expect(mixin.get('saveInProgress')).to.be.true;
    });
  });

  describe("#hasUnsavedChanges()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getHash').returns('hash2');
    });

    afterEach(function() {
      mixin.getHash.restore();
    });

    it("hash is null", function() {
      mixin.set('hash', null);
      expect(mixin.hasUnsavedChanges()).to.be.false;
    });

    it("hash the same", function() {
      mixin.set('hash', 'hash2');
      expect(mixin.hasUnsavedChanges()).to.be.false;
    });

    it("hash different", function() {
      mixin.set('hash', 'hash1');
      expect(mixin.hasUnsavedChanges()).to.be.true;
    });
  });

  describe("#isDirChanged()", function () {

    var testCases = [
      {
        configs: [],
        serviceName: 'S1',
        expected: false
      },
      {
        configs: [],
        serviceName: 'HDFS',
        expected: false
      },
      {
        configs: [
          Em.Object.create({
            name: 'dfs.namenode.name.dir',
            isNotDefaultValue: false
          })
        ],
        serviceName: 'HDFS',
        expected: false
      },
      {
        configs: [
          Em.Object.create({
            name: 'dfs.namenode.name.dir',
            isNotDefaultValue: true
          })
        ],
        serviceName: 'HDFS',
        expected: true
      },
      {
        configs: [
          Em.Object.create({
            name: 'dfs.namenode.checkpoint.dir',
            isNotDefaultValue: true
          })
        ],
        serviceName: 'HDFS',
        expected: true
      },
      {
        configs: [
          Em.Object.create({
            name: 'dfs.datanode.data.dir',
            isNotDefaultValue: true
          })
        ],
        serviceName: 'HDFS',
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it("serviceName = " + test.serviceName +
         "configs = " + test.configs, function() {
        mixin.set('stepConfigs', [Em.Object.create({
          serviceName: test.serviceName,
          configs: test.configs
        })]);
        mixin.set('content.serviceName', test.serviceName);
        expect(mixin.isDirChanged()).to.be.equal(test.expected);
      });
    });
  });

  describe("#formatConfigValues()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'trimProperty', function(c) {
        return c.get('value');
      });
    });

    afterEach(function() {
      App.config.trimProperty.restore();
    });

    it("boolean value", function() {
      var config = Em.Object.create({value: true});
      mixin.formatConfigValues([config]);
      expect(config.get('value')).to.be.equal('true');
    });

    it("string value", function() {
      var config = Em.Object.create({value: 'val1'});
      mixin.formatConfigValues([config]);
      expect(config.get('value')).to.be.equal('val1');
    });
  });

  describe("#formatValueBeforeSave()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(mixin, 'addM').returns(false);
      sinon.stub(App.router, 'get').returns({
        't1': 'val1'
      });
      sinon.stub(App.config, 'trimProperty').returns('trimmed');
    });

    afterEach(function() {
      this.mock.restore();
      App.router.get.restore();
      App.config.trimProperty.restore();
    });

    it("should add 'm'", function() {
      this.mock.returns(true);
      var property = Em.Object.create({name: 'p1', value: 'val1'});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal('val1m');
    });

    it("boolean value", function() {
      var property = Em.Object.create({name: 'p1', value: true});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal('true');
    });

    it("kdc_type config", function() {
      var property = Em.Object.create({name: 'kdc_type', value: 'val1'});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal('t1');
    });

    it("storm.zookeeper.servers config", function() {
      var property = Em.Object.create({name: 'storm.zookeeper.servers', value: 'val1'});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal('val1');
    });

    it("nimbus.seeds config", function() {
      var property = Em.Object.create({name: 'storm.zookeeper.servers', value: ['val1']});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal("['val1']");
    });

    it("other config", function() {
      var property = Em.Object.create({name: 'p1', value: 'val1'});
      expect(mixin.formatValueBeforeSave(property)).to.be.equal("trimmed");
    });
  });

  describe("#addM()", function () {

    var testCases = [
      {
        name: 'aa_heapsize',
        value: 'val1',
        expected: true
      },
      {
        name: 'aa_heapsiz',
        value: 'val1',
        expected: false
      },
      {
        name: 'hadoop_heapsize',
        value: 'val1',
        expected: false
      },
      {
        name: 'aa_heapsize',
        value: 'val1m',
        expected: false
      }
    ];

    testCases.forEach(function(test) {
      it("name = " + test.name +
         " value = " + test.value, function() {
        expect(mixin.addM(test.name, test.value)).to.be.equal(test.expected);
      });
    });
  });

  describe("#saveGroup()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'generateDesiredConfigsJSON').returns("{}");
      sinon.stub(mixin, 'createConfigGroup');
      sinon.stub(mixin, 'updateConfigGroup');
    });

    afterEach(function() {
      mixin.generateDesiredConfigsJSON.restore();
      mixin.createConfigGroup.restore();
      mixin.updateConfigGroup.restore();
    });

    it("createConfigGroup should be called", function() {
      var group = {
        name: 'g1',
        is_temporary: true,
        service_id: 'S1',
        description: 'desc',
        hosts: ['host1']
      };
      mixin.saveGroup([], group, 'note', Em.K);
      expect(mixin.createConfigGroup.getCall(0).args[0]).to.be.eql({
        ConfigGroup: {
          "cluster_name": 'mycluster',
          "group_name": 'g1',
          "tag": 'S1',
          "description": 'desc',
          "hosts": [{
            host_name: 'host1'
          }],
          "service_config_version_note": "note",
          "desired_configs": "{}",
          "service_name": "S1",
        }
      });
    });

    it("updateConfigGroup should be called", function() {
      var group = {
        id: 'g1',
        name: 'g1',
        is_temporary: false,
        service_id: 'S1',
        description: 'desc',
        hosts: ['host1']
      };
      mixin.saveGroup([], group, null, Em.K);
      expect(mixin.updateConfigGroup.getCall(0).args[0]).to.be.eql({
        ConfigGroup: {
          "cluster_name": 'mycluster',
          "group_name": 'g1',
          "tag": 'S1',
          "description": 'desc',
          "hosts": [{
            host_name: 'host1'
          }],
          "service_config_version_note": "",
          "service_name": "S1",
          "desired_configs": "{}",
          id: 'g1'
        }
      });
    });
  });

  describe("#createConfigGroup()", function () {

    it("App.ajax.send should be called", function() {
      mixin.createConfigGroup({}, null);
      var args = testHelpers.findAjaxRequest('name', 'wizard.step8.apply_configuration_groups');
      expect(args[0]).to.be.eql({
        name: 'wizard.step8.apply_configuration_groups',
        sender: mixin,
        data: {
          data: '{}'
        }
      });
    });
  });

  describe("#updateConfigGroup()", function () {

    it("App.ajax.send should be called", function() {
      mixin.updateConfigGroup({ConfigGroup: {id: 'g1'}}, null);
      var args = testHelpers.findAjaxRequest('name', 'config_groups.update_config_group');
      expect(args[0]).to.be.eql({
        name: 'config_groups.update_config_group',
        sender: mixin,
        data: {
          id: 'g1',
          configGroup: {ConfigGroup: {id: 'g1'}}
        }
      });
    });
  });

  describe("#putChangedConfigurations()", function () {

    it("App.ajax.send should be called", function() {
      mixin.putChangedConfigurations(['S1'], null, null);
      var args = testHelpers.findAjaxRequest('name', 'common.across.services.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.across.services.configurations',
        sender: mixin,
        data: {
          data: '[S1]'
        },
        error: 'doPUTClusterConfigurationSiteErrorCallback'
      });
    });
  });

  describe("#putConfigGroupChangesSuccess()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'onDoPUTClusterConfigurations');
    });

    afterEach(function() {
      mixin.onDoPUTClusterConfigurations.restore();
    });

    it("onDoPUTClusterConfigurations should be called", function() {
      mixin.putConfigGroupChangesSuccess();
      expect(mixin.get('saveConfigsFlag')).to.be.true;
      expect(mixin.onDoPUTClusterConfigurations.calledOnce).to.be.true;
    });
  });

  describe("#doPUTClusterConfigurationSiteSuccessCallback()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'onDoPUTClusterConfigurations');
    });

    afterEach(function() {
      mixin.onDoPUTClusterConfigurations.restore();
    });

    it("onDoPUTClusterConfigurations should be called", function() {
      mixin.doPUTClusterConfigurationSiteSuccessCallback();
      expect(mixin.onDoPUTClusterConfigurations.calledWith(true)).to.be.true;
    });
  });

  describe("#doPUTClusterConfigurationSiteErrorCallback()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'onDoPUTClusterConfigurations');
    });

    afterEach(function() {
      mixin.onDoPUTClusterConfigurations.restore();
    });

    it("onDoPUTClusterConfigurations should be called", function() {
      mixin.doPUTClusterConfigurationSiteErrorCallback();
      expect(mixin.get('saveConfigsFlag')).to.be.false;
      expect(mixin.onDoPUTClusterConfigurations.calledOnce).to.be.true;
    });
  });

  describe("#onDoPUTClusterConfigurations", function () {
    var mockController = {
      updateClusterData: Em.K,
      triggerQuickLinksUpdate: Em.K,
      loadConfigs: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mockController);
      sinon.stub(App.Service, 'find').returns({});
      sinon.stub(mockController, 'updateClusterData');
      sinon.stub(mockController, 'triggerQuickLinksUpdate');
      sinon.stub(mockController, 'loadConfigs');
      sinon.stub(mixin, 'showSaveConfigsPopup');
      sinon.stub(mixin, 'getSaveConfigsPopupOptions').returns({
        header: 'h1',
        message: 'm1',
        messageClass: 'mc1',
        value: 'val1',
        urlParams: ''
      });
      sinon.stub(mixin, 'clearAllRecommendations');
      mixin.set('saveConfigsFlag', false);
      mixin.onDoPUTClusterConfigurations(true);
    });

    afterEach(function() {
      App.Service.find.restore();
      App.router.get.restore();
      mockController.updateClusterData.restore();
      mockController.triggerQuickLinksUpdate.restore();
      mockController.loadConfigs.restore();
      mixin.showSaveConfigsPopup.restore();
      mixin.getSaveConfigsPopupOptions.restore();
      mixin.clearAllRecommendations.restore();
    });

    it("updateClusterData should be called", function() {
      expect(mockController.updateClusterData.calledOnce).to.be.true;
    });

    it("triggerQuickLinksUpdate should be called", function() {
      expect(mockController.triggerQuickLinksUpdate.calledOnce).to.be.true;
    });

    it("loadConfigs should be called", function() {
      expect(mockController.loadConfigs.calledOnce).to.be.true;
    });

    it("clearAllRecommendations should be called", function() {
      expect(mixin.clearAllRecommendations.calledOnce).to.be.true;
    });

    it("showSaveConfigsPopup should be called", function() {
      expect(mixin.showSaveConfigsPopup.getCall(0).args).to.be.eql([
        'h1', false, 'm1', 'mc1', 'val1', 'unknown', '', true
      ]);
    });
  });

  describe("#getSaveConfigsPopupOptions()", function () {

    it("flag is true", function() {
      mixin.set('content.serviceName', 'HDFS');
      expect(mixin.getSaveConfigsPopupOptions({flag: true})).to.be.eql({
        header: Em.I18n.t('services.service.config.saved'),
        message: Em.I18n.t('services.service.config.saved.message'),
        messageClass: 'alert alert-success',
        urlParams: ',ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count&ServiceComponentInfo/service_name.in(HDFS)'
      });
    });

    it("flag is false", function() {
      expect(mixin.getSaveConfigsPopupOptions({flag: false, message: 'm1', value: 'val1'})).to.be.eql({
        urlParams: '',
        header: Em.I18n.t('common.failure'),
        message: 'm1',
        messageClass: 'alert alert-error',
        value: 'val1'
      });
    });
  });

  describe("#getServiceConfigToSave()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'textareaIntoFileConfigs');
      sinon.stub(App.StackService, 'find').returns(Em.Object.create({
        configTypes: {'k1': 't1'}
      }));
      sinon.stub(App.config, 'getOriginalFileName').returns('file1');
      this.mockSave = sinon.stub(mixin, 'saveSiteConfigs');
      this.mockJSON = sinon.stub(mixin, 'generateDesiredConfigsJSON');
      sinon.stub(mixin, 'getModifiedConfigs').returns([]);
    });

    afterEach(function() {
      App.config.textareaIntoFileConfigs.restore();
      App.StackService.find.restore();
      App.config.getOriginalFileName.restore();
      this.mockSave.restore();
      this.mockJSON.restore();
      mixin.getModifiedConfigs.restore();
    });

    it("App.config.textareaIntoFileConfigs should be called", function() {
      mixin.getServiceConfigToSave('YARN', [{}]);
      expect(App.config.textareaIntoFileConfigs.calledWith([{}], 'capacity-scheduler.xml')).to.be.true;
    });

    it("getModifiedConfigs should be called", function() {
      mixin.getServiceConfigToSave('S1', [{}]);
      expect(mixin.getModifiedConfigs.calledWith([{}])).to.be.true;
    });

    it("modifiedConfigs is null", function() {
      this.mockSave.returns(null);
      expect(mixin.getServiceConfigToSave('S1', [{}])).to.be.null;
    });

    it("modifiedConfigs is empty", function() {
      this.mockSave.returns([]);
      expect(mixin.getServiceConfigToSave('S1', [{}])).to.be.null;
    });

    it("configsToSave is empty", function() {
      this.mockSave.returns([{filename: 'file1'}]);
      this.mockJSON.returns([]);
      expect(mixin.getServiceConfigToSave('S1', [{}])).to.be.null;
    });

    it("configsToSave has configs", function() {
      this.mockSave.returns([{filename: 'file1'}]);
      this.mockJSON.returns([{}]);
      expect(mixin.getServiceConfigToSave('S1', [{}])).to.be.equal(JSON.stringify({
        Clusters: {
          desired_config: [{}]
        }
      }));
    });
  });

  describe('#isOverriddenConfigsModified', function() {
    it('no configs modified', function() {
      expect(mixin.isOverriddenConfigsModified([
        Em.Object.create({
         name: '1',
          savedValue: '1',
          value: '1',
          isFinal: false,
          savedIsFinal: false
        })
      ], Em.Object.create({
        properties: [
          {name: '1'}
        ]
      }))).to.be.false;
    });
    it('config value modified', function() {
      expect(mixin.isOverriddenConfigsModified([
        Em.Object.create({
          name: '2',
          savedValue: '1',
          value: '2',
          isFinal: false,
          savedIsFinal: false
        })
      ], Em.Object.create({
        properties: [
          {name: '2'}
        ]
      }))).to.be.true;
    });
    it('config isFinal modified', function() {
      expect(mixin.isOverriddenConfigsModified([
        Em.Object.create({
          name: '2',
          savedValue: '2',
          value: '2',
          isFinal: true,
          savedIsFinal: false
        })
      ], Em.Object.create({
        properties: [
          {name: '2'}
        ]
      }))).to.be.true;
    });
    it('one config removed', function() {
      expect(mixin.isOverriddenConfigsModified([
        Em.Object.create({
          name: '3',
          savedValue: '3',
          value: '3',
          isFinal: false,
          savedIsFinal: false
        })
      ], Em.Object.create({
        properties: [
          {name: '2'},
          {name: '3'}
        ]
      }))).to.be.true;
    });
  });

});

