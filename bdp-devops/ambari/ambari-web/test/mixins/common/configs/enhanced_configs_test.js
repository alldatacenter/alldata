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
var blueprintUtils = require('utils/blueprint');

describe('App.EnhancedConfigsMixin', function () {
  var mixin;
  var mixinClass = Em.Controller.extend(App.EnhancedConfigsMixin);

  beforeEach(function () {
    mixin = Em.Object.create(App.EnhancedConfigsMixin, {
      isRecommendationsAutoComplete: false,
      content: Em.Object.create({
        serviceName: 'S1'
      })
    });
  });

  describe('#removeCurrentFromDependentList()', function () {
    it('update some fields', function () {
      mixin.get('recommendations').pushObject({
        saveRecommended: true,
        saveRecommendedDefault: true,
        configGroup: "Default",
        propertyName: 'p1',
        propertyFileName: 'f1',
        value: 'v1'
      });
      mixin.removeCurrentFromDependentList(Em.Object.create({name: 'p1', filename: 'f1.xml', value: 'v2'}));
      expect(mixin.get('recommendations')[0]).to.eql({
        saveRecommended: false,
        saveRecommendedDefault: false,
        configGroup: "Default",
        propertyName: 'p1',
        propertyFileName: 'f1',
        value: 'v1'
      });
    });
  });

  describe('#buildConfigGroupJSON()', function () {
    it('generates JSON based on config group info', function () {
      var configGroup = Em.Object.create({
        name: 'group1',
        id: '1',
        isDefault: false,
        hosts: ['host1', 'host2']
      });
      var configs = [
        App.ServiceConfigProperty.create({
          name: 'p1',
          filename: 'f1',
          overrides: [
            App.ServiceConfigProperty.create({
              group: configGroup,
              value: 'v1'
            })
          ]
        }),
        App.ServiceConfigProperty.create({
          name: 'p2',
          filename: 'f1',
          overrides: [
            App.ServiceConfigProperty.create({
              group: configGroup,
              value: 'v2'
            })
          ]
        }),
        App.ServiceConfigProperty.create({
          name: 'p3',
          filename: 'f2'
        })
      ];
      expect(mixin.buildConfigGroupJSON(configs, configGroup)).to.eql({
        "configurations": [
          {
            "f1": {
              "properties": {
                "p1": "v1",
                "p2": "v2"
              }
            }
          }
        ],
        "hosts": ['host1', 'host2'],
        "group_id": 1
      })
    });

    it('throws error as group is null', function () {
      expect(mixin.buildConfigGroupJSON.bind(mixin)).to.throw(Error, 'configGroup can\'t be null');
    });
  });

  describe("#dependenciesMessage", function () {
    var mixinInstance = mixinClass.create({
      changedProperties: []
    });
    it("no properties changed", function () {
      mixinInstance.set('changedProperties', []);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(0) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.plural').format(0)
      )
    });
    it("single property changed", function () {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.singular').format(1) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.singular').format(1)
      )
    });
    it("two properties changed", function () {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        }),
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(2) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.singular').format(1)
      )
    });
    it("two properties changed, from different services", function () {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        }),
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S2'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(2) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.plural').format(2)
      )
    });
  });

  describe("#changedDependentGroup", function () {
    var mixinInstance;

    beforeEach(function () {
      mixinInstance = mixinClass.create({
        selectedService: {
          serviceName: 'test',
          dependentServiceNames: ['test1', 'test2', 'test3'],
          configGroups: [
            {name: 'testCG'},
            {name: 'notTestCG'}
          ]
        },
        stepConfigs: [
          Em.Object.create({serviceName: 'test1'}),
          Em.Object.create({serviceName: 'test2'}),
          Em.Object.create({serviceName: 'test3'}),
          Em.Object.create({serviceName: 'test4'}),
          Em.Object.create({serviceName: 'test5'})
        ],
        selectedConfigGroup: {name: 'testCG'},
        recommendations: [1, 2, 3]
      });

      sinon.stub(App, 'showSelectGroupsPopup', Em.K);
      sinon.stub(App.Service, 'find').returns([
        {serviceName: 'test2'},
        {serviceName: 'test3'},
        {serviceName: 'test4'}
      ]);
    });

    afterEach(function () {
      App.showSelectGroupsPopup.restore();
      App.Service.find.restore();
    });

    it("should call showSelectGroupsPopup with appropriate arguments", function () {
      mixinInstance.changedDependentGroup();
      expect(App.showSelectGroupsPopup.calledWith(
        'test',
        {name: 'testCG'},
        [
          Em.Object.create({serviceName: 'test2'}),
          Em.Object.create({serviceName: 'test3'})
        ],
        [1, 2, 3]
      )).to.be.true;
    });
  });

  describe("#setDependentGroups()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(mixin, 'shouldSetDependentConfigs');
      sinon.stub(mixin, 'setDependentConfigGroups');
    });

    afterEach(function() {
      this.mock.restore();
      mixin.setDependentConfigGroups.restore();
    });

    it("shouldSetDependentConfigs return false", function() {
      this.mock.returns(false);
      mixin.setDependentGroups();
      expect(mixin.setDependentConfigGroups.called).to.be.false;
    });

    it("service depends on group", function() {
      this.mock.returns(true);
      mixin.setProperties({
        selectedService: Em.Object.create({
          dependentServiceNames: ['S1']
        }),
        selectedConfigGroup: Em.Object.create({
          dependentConfigGroups: {S1: {}}
        })
      });
      expect(mixin.setDependentConfigGroups.called).to.be.false;
    });

    it("no stepConfigs", function() {
      this.mock.returns(true);
      mixin.setProperties({
        selectedService: Em.Object.create({
          dependentServiceNames: ['S1']
        }),
        selectedConfigGroup: Em.Object.create({
          dependentConfigGroups: {}
        }),
        stepConfigs: []
      });
      expect(mixin.setDependentConfigGroups.called).to.be.false;
    });

    it("setDependentConfigGroups should be called", function() {
      this.mock.returns(true);
      mixin.setProperties({
        selectedService: Em.Object.create({
          dependentServiceNames: ['S1']
        }),
        selectedConfigGroup: Em.Object.create({
          dependentConfigGroups: {}
        }),
        stepConfigs: [Em.Object.create({
          serviceName: 'S1'
        })]
      });
      expect(mixin.setDependentConfigGroups.calledWith(Em.Object.create({
        serviceName: 'S1'
      }), 'S1')).to.be.true;
    });
  });

  describe("#setDependentConfigGroups()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'setDependentGroups');
      mixin.set('selectedConfigGroup', Em.Object.create({
        dependentConfigGroups: {}
      }));
      mixin.set('selectedService', Em.Object.create({
        configGroups: [Em.Object.create({
          dependentConfigGroups: {'S1': 'g1'},
          isDefault: false
        })]
      }))
    });

    afterEach(function() {
      mixin.setDependentGroups.restore();
    });

    it("default group", function() {
      var stepConfig = Em.Object.create({
        configGroups: [
          Em.Object.create({
            isDefault: true
          })
        ]
      });
      mixin.setDependentConfigGroups(stepConfig, 'S1');
      expect(mixin.get('selectedConfigGroup.dependentConfigGroups')).to.be.empty;
    });

    it("dependent group", function() {
      var stepConfig = Em.Object.create({
        configGroups: [
          Em.Object.create({
            isDefault: false,
            name: 'g1'
          })
        ]
      });
      mixin.setDependentConfigGroups(stepConfig, 'S1');
      expect(mixin.get('selectedConfigGroup.dependentConfigGroups')).to.be.empty;
    });

    it("not dependent group", function() {
      var stepConfig = Em.Object.create({
        configGroups: [
          Em.Object.create({
            isDefault: false,
            name: 'g2'
          })
        ]
      });
      mixin.setDependentConfigGroups(stepConfig, 'S1');
      expect(mixin.get('selectedConfigGroup.dependentConfigGroups')).to.be.eql({
        'S1': 'g2'
      });
    });
  });

  describe("#shouldSetDependentConfigs()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'setDependentGroups');
    });

    afterEach(function() {
      mixin.setDependentGroups.restore();
    });

    var testCases = [
      {
        selectedConfigGroup: null,
        isControllerSupportsEnhancedConfigs: false,
        dependentServiceNames: [],
        expected: false
      },
      {
        selectedConfigGroup: Em.Object.create({isDefault: true}),
        isControllerSupportsEnhancedConfigs: false,
        dependentServiceNames: [],
        expected: false
      },
      {
        selectedConfigGroup: Em.Object.create({isDefault: false}),
        isControllerSupportsEnhancedConfigs: false,
        dependentServiceNames: [],
        expected: false
      },
      {
        selectedConfigGroup: Em.Object.create({isDefault: false}),
        isControllerSupportsEnhancedConfigs: true,
        dependentServiceNames: [],
        expected: false
      },
      {
        selectedConfigGroup: Em.Object.create({isDefault: false}),
        isControllerSupportsEnhancedConfigs: true,
        dependentServiceNames: ['S1'],
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it("selectedConfigGroup = " + JSON.stringify(test.selectedConfigGroup) +
         "isControllerSupportsEnhancedConfigs = " + test.isControllerSupportsEnhancedConfigs +
         "dependentServiceNames = " + test.dependentServiceNames, function() {
        mixin.reopen({
          selectedConfigGroup: test.selectedConfigGroup,
          isControllerSupportsEnhancedConfigs: test.isControllerSupportsEnhancedConfigs,
          selectedService: Em.Object.create({
            dependentServiceNames: test.dependentServiceNames
          })
        });
        expect(mixin.shouldSetDependentConfigs()).to.be.equal(test.expected);
      });
    });
  });

  describe("#getGroupForService()", function () {

    it("stepConfigs is null", function() {
      mixin.set('stepConfigs', null);
      expect(mixin.getGroupForService('S1')).to.be.null;
    });

    it("stepConfigs is empty", function() {
      mixin.set('stepConfigs', []);
      expect(mixin.getGroupForService('S1')).to.be.null;
    });

    it("should return selectedConfigGroup", function() {
      mixin.setProperties({
        stepConfigs: [{}],
        selectedConfigGroup: {},
        selectedService: Em.Object.create({serviceName: 'S1'})
      });
      expect(mixin.getGroupForService('S1')).to.be.eql({});
    });

    it("service not found", function() {
      mixin.setProperties({
        stepConfigs: [Em.Object.create({serviceName: 'S2'})],
        selectedConfigGroup: {},
        selectedService: Em.Object.create({serviceName: 'S2'})
      });
      expect(mixin.getGroupForService('S1')).to.be.null;
    });

    it("selectedConfigGroup is default, with default", function() {
      mixin.setProperties({
        stepConfigs: [Em.Object.create({
          serviceName: 'S1',
          configGroups: [
            Em.Object.create({isDefault: true})
          ]
        })],
        selectedService: Em.Object.create({serviceName: 'S2'}),
        selectedConfigGroup: Em.Object.create({isDefault: true})
      });
      expect(mixin.getGroupForService('S1')).to.be.eql(Em.Object.create({isDefault: true}));
    });

    it("selectedConfigGroup is default, without default", function() {
      mixin.setProperties({
        stepConfigs: [Em.Object.create({
          serviceName: 'S1',
          configGroups: []
        })],
        selectedService: Em.Object.create({serviceName: 'S2'}),
        selectedConfigGroup: Em.Object.create({isDefault: true})
      });
      expect(mixin.getGroupForService('S1')).to.be.null;
    });

    it("selectedConfigGroup is not default, dependent", function() {
      mixin.setProperties({
        stepConfigs: [Em.Object.create({
          serviceName: 'S1',
          configGroups: [
            Em.Object.create({name: 'g1'})
          ]
        })],
        selectedService: Em.Object.create({serviceName: 'S2'}),
        selectedConfigGroup: Em.Object.create({
          isDefault: false,
          dependentConfigGroups: {'S1': 'g1'}
        })
      });
      expect(mixin.getGroupForService('S1')).to.be.eql(Em.Object.create({name: 'g1'}));
    });

    it("selectedConfigGroup is not default, not dependent", function() {
      mixin.setProperties({
        stepConfigs: [Em.Object.create({
          serviceName: 'S1',
          configGroups: []
        })],
        selectedService: Em.Object.create({serviceName: 'S2'}),
        selectedConfigGroup: Em.Object.create({
          isDefault: false
        })
      });
      expect(mixin.getGroupForService('S1')).to.be.null;
    });
  });

  describe("#clearRecommendationsInfo()", function () {

    it("recommendationsConfigs should be null", function() {
      mixin.clearRecommendationsInfo();
      expect(mixin.get('recommendationsConfigs')).to.be.null;
    });
  });

  describe('#clearRecommendations()', function () {

    beforeEach(function () {
      sinon.stub(mixin, 'clearRecommendationsInfo');
      sinon.stub(mixin, 'clearAllRecommendations');
      mixin.clearRecommendations();
    });

    afterEach(function () {
      mixin.clearRecommendationsInfo.restore();
      mixin.clearAllRecommendations.restore();
    });

    it('clearRecommendationsInfo should be called', function() {
      expect(mixin.get('clearRecommendationsInfo').calledOnce).to.be.true;
    });

    it('clearAllRecommendations should be called', function() {
      expect(mixin.get('clearAllRecommendations').calledOnce).to.be.true;
    });
  });

  describe("#loadConfigRecommendations()", function () {
    var mock = {
      onComplete: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'onComplete');
      sinon.stub(mixin, 'getConfigRecommendationsParams').returns({});
      sinon.stub(mixin, 'modifyRecommendationConfigGroups');
      sinon.stub(mixin, 'addRecommendationRequestParams');
      sinon.stub(mixin, 'getRecommendationsRequest');
      sinon.stub(mixin, 'loadAdditionalSites');
    });

    afterEach(function() {
      mock.onComplete.restore();
      mixin.getConfigRecommendationsParams.restore();
      mixin.modifyRecommendationConfigGroups.restore();
      mixin.addRecommendationRequestParams.restore();
      mixin.getRecommendationsRequest.restore();
      mixin.loadAdditionalSites.restore();
    });

    it("changedConfigs is null", function() {
      mixin.set('recommendationsConfigs', {});
      mixin.loadConfigRecommendations(null, mock.onComplete);
      expect(mock.onComplete.calledOnce).to.be.true;
    });

    it("changedConfigs is empty", function() {
      mixin.set('recommendationsConfigs', {});
      mixin.loadConfigRecommendations([], mock.onComplete);
      expect(mock.onComplete.calledOnce).to.be.true;
    });

    it("recommendationsConfigs is null, no MISC service", function() {
      mixin.set('stepConfigs', []);
      mixin.set('recommendationsConfigs', null);
      mixin.loadConfigRecommendations([], mock.onComplete);
      expect(mixin.getConfigRecommendationsParams.calledWith(false)).to.be.true;
      expect(mixin.modifyRecommendationConfigGroups.calledOnce).to.be.true;
      expect(mixin.loadAdditionalSites.calledOnce).to.be.true;
    });

    it("changedConfigs is correct, MISC service present", function() {
      mixin.set('stepConfigs', [Em.Object.create({serviceName: 'MISC'})]);
      mixin.set('recommendationsConfigs', {});
      mixin.loadConfigRecommendations([{}], mock.onComplete);
      expect(mixin.getConfigRecommendationsParams.calledWith(true)).to.be.true;
      expect(mixin.modifyRecommendationConfigGroups.calledOnce).to.be.true;
      expect(mixin.addRecommendationRequestParams.calledOnce).to.be.true;
      expect(mixin.getRecommendationsRequest.calledOnce).to.be.true;
    });
  });

  describe("#addRecommendationRequestParams()", function () {

    beforeEach(function() {
      sinon.stub(blueprintUtils, 'buildConfigsJSON').returns([{}]);
      sinon.stub(App, 'get').returns(1);
    });

    afterEach(function() {
      blueprintUtils.buildConfigsJSON.restore();
      App.get.restore();
    });

    it("recommendations should be set", function () {
      var dataToSend = {};
      mixin.addRecommendationRequestParams({blueprint: {}}, dataToSend, []);
      expect(dataToSend).to.be.eql({
        "recommendations": {
          "blueprint": {
            "configurations": [
              {}
            ]
          }
        },
        "autoComplete": "false",
        "configsResponse": "false",
        "serviceName": "S1",
        "clusterId": 1
      });
    });
  });

  describe("#loadAdditionalSites()", function () {

    beforeEach(function() {
      sinon.stub(App.router.get('configurationController'), 'getCurrentConfigsBySites').returns({
        done: function(callback) {callback([]);}
      });
      sinon.stub(mixin, 'addRecommendationRequestParams');
      sinon.stub(mixin, 'getRecommendationsRequest');
      mixin.loadAdditionalSites([], [], {}, {}, Em.K);
    });

    afterEach(function() {
      App.router.get('configurationController').getCurrentConfigsBySites.restore();
      mixin.addRecommendationRequestParams.restore();
      mixin.getRecommendationsRequest.restore();
    });

    it("getCurrentConfigsBySites should be called", function() {
      expect(App.router.get('configurationController').getCurrentConfigsBySites.calledOnce).to.be.true;
    });

    it("addRecommendationRequestParams should be called", function() {
      expect(mixin.addRecommendationRequestParams.calledWith({}, {}, [])).to.be.true;
    });

    it("getRecommendationsRequest should be called", function() {
      expect(mixin.getRecommendationsRequest.calledWith({}, Em.K)).to.be.true;
    });
  });

  describe("#modifyRecommendationConfigGroups()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'buildConfigGroupJSON').returns({});
    });

    afterEach(function() {
      mixin.buildConfigGroupJSON.restore();
    });

    it("selectedConfigGroup is null", function() {
      mixin.set('selectedConfigGroup', null);
      var recommendations = {config_groups: []};
      mixin.modifyRecommendationConfigGroups(recommendations);
      expect(recommendations).to.be.eql({});
    });

    it("selectedConfigGroup is default", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      var recommendations = {config_groups: []};
      mixin.modifyRecommendationConfigGroups(recommendations);
      expect(recommendations).to.be.eql({});
    });

    it("selectedConfigGroup has no hosts", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({hosts: [], isDefault: false}));
      var recommendations = {config_groups: []};
      mixin.modifyRecommendationConfigGroups(recommendations);
      expect(recommendations).to.be.eql({});
    });

    it("config_groups should be set", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({hosts: [{}], isDefault: false}));
      var recommendations = {config_groups: []};
      mixin.modifyRecommendationConfigGroups(recommendations);
      expect(recommendations).to.be.eql({config_groups: [{}]});
    });
  });

  describe("#getConfigRecommendationsParams()", function () {

    it("updateDependencies is true", function() {
      mixin.set('hostNames', ['host1']);
      mixin.set('serviceNames', ['S1']);
      expect(mixin.getConfigRecommendationsParams(true, [{}])).to.be.eql({
        recommend: 'configuration-dependencies',
        hosts: ['host1'],
        services: ['S1'],
        changed_configurations: [{}]
      });
    });

    it("updateDependencies is false", function() {
      mixin.set('hostNames', ['host1']);
      mixin.set('serviceNames', ['S1']);
      expect(mixin.getConfigRecommendationsParams(false, [{}])).to.be.eql({
        recommend: 'configurations',
        hosts: ['host1'],
        services: ['S1'],
        changed_configurations: undefined
      });
    });
  });

  describe("#getRecommendationsRequest()", function () {
    var mock = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'callback');
    });

    afterEach(function() {
      mock.callback.restore();
    });

    it("App.ajax.send should be called", function() {
      mixin.getRecommendationsRequest({}, mock.callback);
      expect(mixin.get('recommendationsInProgress')).to.be.true;
      var args = testHelpers.findAjaxRequest('name', 'config.recommendations');
      expect(args[0]).exists;
      args[0].callback();
      expect(mixin.get('recommendationsInProgress')).to.be.false;
      expect(mock.callback.calledOnce).to.be.true;
    });
  });

  describe("#isConfigHasInitialState()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'configId').returns('c1');
    });

    afterEach(function() {
      App.config.configId.restore();
    });

    it("selectedConfigGroup.isDefault = false", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      expect(mixin.isConfigHasInitialState()).to.be.false;
    });

    it("recommendationsConfigs is null", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin.set('recommendationsConfigs', null);
      expect(mixin.isConfigHasInitialState()).to.be.false;
    });

    it("stepConfigs is empty", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin.set('recommendationsConfigs', {});
      mixin.set('stepConfigs', []);
      expect(mixin.isConfigHasInitialState()).to.be.true;
    });

    it("changedConfigProperties is empty", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin.set('recommendationsConfigs', {});
      mixin.set('stepConfigs', [Em.Object.create({
        changedConfigProperties: []
      })]);
      expect(mixin.isConfigHasInitialState()).to.be.true;
    });

    it("changedConfigProperties has configs", function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin.set('recommendationsConfigs', {});
      mixin.set('stepConfigs', [Em.Object.create({
        changedConfigProperties: [Em.Object.create()]
      })]);
      expect(mixin.isConfigHasInitialState()).to.be.true;
    });
  });

  describe("#buildConfigGroupJSON()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('tag1');
    });

    afterEach(function() {
      App.config.getConfigTagFromFileName.restore();
    });

    var testCases = [
      {
        configs: [],
        configGroup: Em.Object.create({
          hosts: [],
          id: '1'
        }),
        autoComplete: false,
        expected: {
          configurations: [{}],
          hosts: [],
          group_id: 1
        }
      },
      {
        configs: [],
        configGroup: Em.Object.create({
          hosts: [],
          id: '1'
        }),
        autoComplete: true,
        expected: {
          group_id: 1
        }
      },
      {
        configs: [Em.Object.create({
          overrides: null
        })],
        configGroup: Em.Object.create({
          hosts: [],
          id: '2'
        }),
        autoComplete: false,
        expected: {
          configurations: [{}],
          hosts: [],
          group_id: 2
        }
      },
      {
        configs: [Em.Object.create({
          overrides: [Em.Object.create({
            group: {
              name: 'g2'
            }
          })]
        })],
        configGroup: Em.Object.create({
          name: 'g1',
          id: '3',
          hosts: []
        }),
        autoComplete: false,
        expected: {
          configurations: [{}],
          hosts: [],
          group_id: 3
        }
      },
      {
        configs: [Em.Object.create({
          name: 'c1',
          overrides: [Em.Object.create({
            value: 'val',
            group: {
              name: 'g1'
            }
          })]
        })],
        configGroup: Em.Object.create({
          name: 'g1',
          hosts: [],
          id: '4'
        }),
        autoComplete: false,
        expected: {
          configurations: [{
            "tag1": {
              "properties": {
                "c1": "val"
              }
            }
          }],
          hosts: [],
          group_id: 4
        }
      }
    ];

    testCases.forEach(function(test) {
      it("configs = " + JSON.stringify(test.configs) +
         "configGroup = " + JSON.stringify(test.configGroup) +
         "autoComplete = " + test.autoComplete, function() {
        expect(mixin.buildConfigGroupJSON(test.configs, test.configGroup, test.autoComplete)).to.be.eql(test.expected);
      });
    });
  });

  describe("#loadRecommendationsSuccess()", function () {
    var params = {dataToSend: {changed_configurations: []}};

    beforeEach(function() {
      sinon.stub(mixin, '_saveRecommendedValues');
      this.mock = sinon.stub(mixin, 'isConfigHasInitialState');
      sinon.stub(mixin, 'undoRedoRecommended');
      sinon.stub(mixin, 'clearAllRecommendations');
    });

    afterEach(function() {
      mixin._saveRecommendedValues.restore();
      this.mock.restore();
      mixin.undoRedoRecommended.restore();
      mixin.clearAllRecommendations.restore();
    });

    it("_saveRecommendedValues should be called", function() {
      mixin.loadRecommendationsSuccess({}, {}, params);
      expect(mixin._saveRecommendedValues.calledWith({}, [])).to.be.true;
    });

    it("recommendationsConfigs should be set", function() {
      var data = {
        resources: [
          {
            recommendations: {
              blueprint: {
                configurations: [{}]
              }
            }
          }
        ]
      };
      mixin.loadRecommendationsSuccess(data, {}, params);
      expect(mixin.get('recommendationsConfigs')).to.be.eql([{}]);
    });

    it("undoRedoRecommended should be called", function() {
      this.mock.returns(true);
      mixin.loadRecommendationsSuccess({}, {}, params);
      expect(mixin.undoRedoRecommended.calledTwice).to.be.true;
    });

    it("clearAllRecommendations should be called", function() {
      this.mock.returns(true);
      mixin.loadRecommendationsSuccess({}, {}, params);
      expect(mixin.clearAllRecommendations.calledOnce).to.be.true;
    });

    it("recommendations should be set", function() {
      mixin.set('initialRecommendations', [{}]);
      mixin.set('recommendations', []);
      this.mock.returns(true);
      mixin.loadRecommendationsSuccess({}, {}, params);
      expect(mixin.get('recommendations')).to.not.be.empty;
    });
  });

  describe("#changedDependentGroup()", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([Em.Object.create({
        serviceName: 'S2'
      })]);
      sinon.stub(App, 'showSelectGroupsPopup');
    });

    afterEach(function() {
      App.Service.find.restore();
      App.showSelectGroupsPopup.restore();
    });

    it("App.showSelectGroupsPopup should be called", function() {
      mixin.set('selectedService', Em.Object.create({
        serviceName: 'S3',
        configGroups: [Em.Object.create({name: 'g1'})],
        dependentServiceNames: ['S2', 'S1']
      }));
      mixin.set('stepConfigs', [Em.Object.create({
        serviceName: 'S2'
      })]);
      mixin.set('selectedConfigGroup', Em.Object.create({name: 'g1'}));
      mixin.set('recommendations', [{}]);
      mixin.changedDependentGroup();
      expect(App.showSelectGroupsPopup.getCall(0).args).to.be.eql([
        'S3',
        Em.Object.create({name: 'g1'}),
        [Em.Object.create({serviceName: 'S2'})],
        [{}]
      ]);
    });
  });

  describe("#_saveRecommendedValues()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'saveConfigGroupsRecommendations');
      sinon.stub(mixin, 'updateConfigsByRecommendations');
      sinon.stub(mixin, 'addByRecommendations');
      sinon.stub(mixin, 'cleanUpRecommendations');
    });

    afterEach(function() {
      mixin.saveConfigGroupsRecommendations.restore();
      mixin.updateConfigsByRecommendations.restore();
      mixin.addByRecommendations.restore();
      mixin.cleanUpRecommendations.restore();
    });

    it("cleanUpRecommendations should be called", function() {
      var data = {
        resources: [
          {
            recommendations: {
              blueprint: {
                configurations: {}
              }
            }
          }
        ]
      };
      mixin._saveRecommendedValues(data, []);
      expect(mixin.cleanUpRecommendations.calledOnce).to.be.true;
    });

    it("saveConfigGroupsRecommendations should be called", function() {
      var data = {
        resources: [
          {
            recommendations: {
              'config-groups': [],
              blueprint: {
                configurations: {}
              }
            }
          }
        ]
      };
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      mixin._saveRecommendedValues(data, []);
      expect(mixin.saveConfigGroupsRecommendations.calledWith({
        'config-groups': [],
        blueprint: {
          configurations: {}
        }
      }, [])).to.be.true;
    });

    it("addByRecommendations should be called, default group", function() {
      var data = {
        resources: [
          {
            recommendations: {
              'config-groups': [],
              blueprint: {
                configurations: {}
              }
            }
          }
        ]
      };
      mixin.set('stepConfigs', [Em.Object.create()]);
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      mixin._saveRecommendedValues(data, []);
      expect(mixin.updateConfigsByRecommendations.calledOnce).to.be.true;
      expect(mixin.addByRecommendations.calledWith({}, [])).to.be.true;
    });

    it("addByRecommendations should be called, group is null", function() {
      var data = {
        resources: [
          {
            recommendations: {
              'config-groups': [],
              blueprint: {
                configurations: {}
              }
            }
          }
        ]
      };
      mixin.set('stepConfigs', [Em.Object.create()]);
      mixin.set('selectedConfigGroup', null);
      mixin._saveRecommendedValues(data, []);
      expect(mixin.updateConfigsByRecommendations.calledOnce).to.be.true;
      expect(mixin.addByRecommendations.calledWith({}, [])).to.be.true;
    });

    it("addByRecommendations should be called, config-groups is undefined", function() {
      var data = {
        resources: [
          {
            recommendations: {
              blueprint: {
                configurations: {}
              }
            }
          }
        ]
      };
      mixin.set('stepConfigs', [Em.Object.create()]);
      mixin.set('selectedConfigGroup', null);
      mixin._saveRecommendedValues(data, []);
      expect(mixin.updateConfigsByRecommendations.calledOnce).to.be.true;
      expect(mixin.addByRecommendations.calledWith({}, [])).to.be.true;
    });
  });

  describe("#saveConfigGroupsRecommendations()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(mixin, 'getGroupForService');
      sinon.stub(mixin, 'updateOverridesByRecommendations');
      sinon.stub(mixin, 'toggleProperty');
      sinon.stub(mixin, 'isConfigGroupAffected').returns(true);
      mixin.set('stepConfigs', [Em.Object.create()]);
    });

    afterEach(function() {
      this.mock.restore();
      mixin.isConfigGroupAffected.restore();
      mixin.updateOverridesByRecommendations.restore();
      mixin.toggleProperty.restore();
    });

    it("updateOverridesByRecommendations should not be called", function() {
      this.mock.returns(null);
      mixin.saveConfigGroupsRecommendations({'config-groups': []});
      expect(mixin.updateOverridesByRecommendations.called).to.be.false;
    });

    it("updateOverridesByRecommendations should be called", function() {
      this.mock.returns(Em.Object.create());
      mixin.saveConfigGroupsRecommendations({'config-groups': [{}]});
      expect(mixin.updateOverridesByRecommendations.calledTwice).to.be.true;
      expect(mixin.toggleProperty.calledWith('forceUpdateBoundaries')).to.be.true;
    });
  });

  describe("#showChangedDependentConfigs()", function () {
    var mock = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(App, 'showDependentConfigsPopup', function(rec, req, callback) {
        callback();
      });
      sinon.stub(mixin, 'onSaveRecommendedPopup');
      sinon.stub(mixin, 'filterRequiredChanges').returns([]);
      sinon.stub(mock, 'callback');
    });

    afterEach(function() {
      App.showDependentConfigsPopup.restore();
      mixin.onSaveRecommendedPopup.restore();
      mixin.filterRequiredChanges.restore();
      mock.callback.restore();
    });

    it("only callback should be called", function() {
      mixin.set('changedProperties', []);
      mixin.showChangedDependentConfigs({}, mock.callback, Em.K);
      expect(mock.callback.calledOnce).to.be.true;
    });

    it("onSaveRecommendedPopup should be called", function() {
      mixin.set('recommendations', [{isEditable: true}]);
      mixin.showChangedDependentConfigs(null, mock.callback, Em.K);
      expect(mixin.onSaveRecommendedPopup.calledWith([{isEditable: true}])).to.be.true;
      expect(mock.callback.calledOnce).to.be.true;
    });
  });

  describe("#onSaveRecommendedPopup()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'undoRedoRecommended');
    });

    afterEach(function() {
      mixin.undoRedoRecommended.restore();
    });

    it("undoRedoRecommended should be called", function() {
      var recommendations = [
        Em.Object.create({
          saveRecommendedDefault: true,
          saveRecommended: false
        }),
        Em.Object.create({
          saveRecommendedDefault: false,
          saveRecommended: true
        })
      ];
      mixin.onSaveRecommendedPopup(recommendations);
      expect(mixin.undoRedoRecommended.getCall(0).args).to.be.eql([
        [
          Em.Object.create({
            saveRecommendedDefault: true,
            saveRecommended: false
          })
        ], false
      ]);
      expect(mixin.undoRedoRecommended.getCall(1).args).to.be.eql([
        [
          Em.Object.create({
            saveRecommendedDefault: false,
            saveRecommended: true
          })
        ], true
      ]);
    });
  });

  describe("#saveInitialRecommendations()", function () {

    it("initialRecommendations should be set", function() {
      mixin.set('recommendations', [{}]);
      mixin.set('initialRecommendations', []);
      mixin.saveInitialRecommendations();
      expect(mixin.get('initialRecommendations')).to.not.be.empty;
    });
  });

  describe("#removeCurrentFromDependentList()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getRecommendation').returns({});
      sinon.stub(mixin, 'saveRecommendation');
    });

    afterEach(function() {
      mixin.getRecommendation.restore();
      mixin.saveRecommendation.restore();
    });

    it("getRecommendation should be called", function() {
      mixin.removeCurrentFromDependentList(Em.Object.create({
        name: 'c1',
        filename: 'f1',
        group: {name: 'g1'}
      }), true);
      expect(mixin.getRecommendation.calledWith('c1', 'f1', 'g1')).to.be.true;
    });

    it("saveRecommendation should be called", function() {
      mixin.removeCurrentFromDependentList(Em.Object.create({
        name: 'c1',
        filename: 'f1',
        group: {name: 'g1'}
      }), true);
      expect(mixin.saveRecommendation.calledWith({}, true)).to.be.true;
    });
  });

  describe("#undoRedoRecommended()", function () {

    beforeEach(function() {
      sinon.stub(App.config, 'getOriginalFileName').returns('file1');
      sinon.stub(App.ServiceConfigGroup, 'find').returns([
        Em.Object.create({serviceName: 'S1', name: 'g1', isDefault: true}),
        Em.Object.create({serviceName: 'S1', name: 'g2', isDefault: false})
      ]);
      sinon.stub(mixin, 'setRecommendedForDefaultGroup');
      sinon.stub(mixin, 'setRecommendedForGroup');
    });

    afterEach(function() {
      App.config.getOriginalFileName.restore();
      App.ServiceConfigGroup.find.restore();
      mixin.setRecommendedForDefaultGroup.restore();
      mixin.setRecommendedForGroup.restore();
    });

    describe("redo is true, default group, setRecommendedForDefaultGroup should be called", function() {

      beforeEach(function () {
        mixin.set('stepConfigs', [Em.Object.create({
          serviceName: 'S1',
          configs: [
            Em.Object.create({ name: 'p1', filename: 'file1'})
          ]
        })]);
        var propertiesToUpdate = [Em.Object.create({
          initialValue: 'val1',
          recommendedValue: 'val2',
          serviceName: 'S1',
          propertyName: 'p1',
          configGroup: 'g1'
        })];
        mixin.undoRedoRecommended(propertiesToUpdate, true);
      });

      it('setRecommendedForDefaultGroup arg 0', function () {
        expect(mixin.setRecommendedForDefaultGroup.getCall(0).args[0]).to.be.equal('val2');
      });

      it('setRecommendedForDefaultGroup arg 1', function () {
        expect(JSON.stringify(mixin.setRecommendedForDefaultGroup.getCall(0).args[1])).to.be.eql(JSON.stringify(Em.Object.create({
          serviceName: 'S1',
          configs: [
            Em.Object.create({ name: 'p1', filename: 'file1'})
          ]
        })));
      });

      it('setRecommendedForDefaultGroup arg 2', function () {
        expect(mixin.setRecommendedForDefaultGroup.getCall(0).args[2]).to.be.eql(Em.Object.create({
          initialValue: 'val1',
          recommendedValue: 'val2',
          serviceName: 'S1',
          propertyName: 'p1',
          configGroup: 'g1'
        }));
      })

      it('setRecommendedForDefaultGroup arg 3', function () {
        expect(mixin.setRecommendedForDefaultGroup.getCall(0).args[3]).to.be.equal('val1');
      });

      it('setRecommendedForDefaultGroup arg 4', function () {
        expect(mixin.setRecommendedForDefaultGroup.getCall(0).args[4]).to.be.eql(Em.Object.create({
          name: 'p1',
          filename: 'file1'
        }));
      });

    });

    describe("redo is false, non-default group, setRecommendedForGroup should be called", function() {

      beforeEach(function () {
        mixin.set('stepConfigs', [Em.Object.create({
          serviceName: 'S1',
          configs: [
            Em.Object.create({ name: 'p1', filename: 'file1'})
          ]
        })]);
        var propertiesToUpdate = [Em.Object.create({
          initialValue: 'val1',
          recommendedValue: 'val2',
          serviceName: 'S1',
          propertyName: 'p1',
          configGroup: 'g2'
        })];
        mixin.undoRedoRecommended(propertiesToUpdate, false);
      });

      it('setRecommendedForGroup arg 0', function () {
        expect(mixin.setRecommendedForGroup.getCall(0).args[0]).to.be.equal('val1');
      });

      it('setRecommendedForGroup arg 1', function () {
        expect(mixin.setRecommendedForGroup.getCall(0).args[1]).to.be.eql(Em.Object.create({
          serviceName: 'S1',
          name: 'g2',
          isDefault: false
        }));
      });

      it('setRecommendedForGroup arg 2', function () {
        expect(mixin.setRecommendedForGroup.getCall(0).args[2]).to.be.eql(Em.Object.create({
          name: 'p1',
          filename: 'file1'
        }));
      });

      it('setRecommendedForGroup arg 3', function () {
        expect(mixin.setRecommendedForGroup.getCall(0).args[3]).to.be.equal('val2');
      });

    });
  });

  describe("#setRecommendedForGroup()", function () {
    var override = Em.Object.create();
    var config = Em.Object.create({
      getOverride: Em.K,
      overrides: [override]
    });

    beforeEach(function() {
      sinon.stub(mixin, '_addConfigOverrideRecommendation');
      sinon.stub(config, 'getOverride').returns(override);
    });

    afterEach(function() {
      mixin._addConfigOverrideRecommendation.restore();
      config.getOverride.restore();
    });

    it("recommended is null", function() {
      mixin.setRecommendedForGroup(null, Em.Object.create({name: 'g1'}), config, 'val1');
      expect(config.get('overrides')).to.be.empty;
    });

    it("_addConfigOverrideRecommendation should be called", function() {
      mixin.setRecommendedForGroup('val2', Em.Object.create({name: 'g1'}), config, null);
      expect(mixin._addConfigOverrideRecommendation.calledWith(
        config,
        'val2',
        null,
        Em.Object.create({name: 'g1'})
      )).to.be.true;
    });

    it("should set recommended value to override", function() {
      mixin.setRecommendedForGroup('val2', Em.Object.create({name: 'g1'}), config, 'val1');
      expect(override.get('value')).to.be.equal('val2');
    });
  });

  describe("#setRecommendedForDefaultGroup()", function () {
    var config = Em.Object.create();
    var stepConfig;

    beforeEach(function() {
      sinon.stub(mixin, '_createNewProperty').returns({});
      sinon.stub(App.configsCollection, 'getConfigByName').returns({propertyDependsOn: []});
      stepConfig = Em.Object.create({
        configs: [config]
      });
    });

    afterEach(function() {
      mixin._createNewProperty.restore();
      App.configsCollection.getConfigByName.restore();
    });

    it("recommended is null", function() {
      var prop = Em.Object.create({
        propertyName: 'p1',
        propertyFileName: 'file1'
      });
      mixin.setRecommendedForDefaultGroup(null, stepConfig, prop, 'val1', config);
      expect(stepConfig.get('configs')).to.be.empty;
    });

    it("initial is null", function() {
      var prop = Em.Object.create({
        propertyName: 'p1',
        propertyFileName: 'file1',
        serviceName: 'S1'
      });
      mixin.setRecommendedForDefaultGroup('val2', stepConfig, prop, null, config);
      expect(stepConfig.get('configs')).to.have.length(2);
      expect(mixin._createNewProperty.calledWith(
        'p1',
        'file1',
        'S1',
        'val2',
        []
      )).to.be.true;
    });

    it("should set config value", function() {
      var prop = Em.Object.create({
        propertyName: 'p1',
        propertyFileName: 'file1'
      });
      mixin.setRecommendedForDefaultGroup('val2', stepConfig, prop, 'val1', config);
      expect(config.get('value')).to.be.equal('val2');
    });


  });

  describe('#filterRequiredChanges', function() {

    it('all recommendations editable', function() {
      var recommendations = [
        {
          isEditable: true
        }
      ];
      expect(mixin.filterRequiredChanges(recommendations)).to.be.empty;
    });

    it('recommendations not editable when editing default config group', function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      var recommendations = [
        {
          isEditable: false
        }
      ];
      expect(mixin.filterRequiredChanges(recommendations)).to.be.eql(recommendations);
    });

    it('recommendations not editable when editing non-default config group for default group', function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      var recommendations = [
        {
          isEditable: false,
          configGroup: App.ServiceConfigGroup.defaultGroupName
        }
      ];
      expect(mixin.filterRequiredChanges(recommendations)).to.be.empty;
    });

    it('recommendations not editable when editing non-default config group for non-default group', function() {
      mixin.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      var recommendations = [
        {
          isEditable: false,
          configGroup: 'g1'
        }
      ];
      expect(mixin.filterRequiredChanges(recommendations)).to.be.eql(recommendations);
    });
  });

  describe('#isConfigGroupAffected', function() {
    it('groups have no shared hosts', function() {
      expect(mixin.isConfigGroupAffected(['host1'], ['host2'])).to.be.false;
    });
    it('groups have shared hosts', function() {
      expect(mixin.isConfigGroupAffected(['host1'], ['host2', 'host1'])).to.be.true;
    });
  });
});

