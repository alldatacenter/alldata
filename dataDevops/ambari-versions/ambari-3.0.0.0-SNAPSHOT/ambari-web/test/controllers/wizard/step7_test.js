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
require('mixins/common/localStorage');
require('controllers/wizard/step7_controller');
var testHelpers = require('test/helpers');

var installerStep7Controller,
  issuesFilterCases = [
    {
      transitionInProgress: false,
      hasStepConfigIssues: true,
      issuesFilterSelected: true,
      issuesFilterText: Em.I18n.t('installer.step7.showingPropertiesWithIssues'),
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, has property issues, submit not clicked'
    },
    {
      transitionInProgress: true,
      hasStepConfigIssues: true,
      issuesFilterSelected: true,
      issuesFilterText: '',
      issuesFilterLinkText: '',
      title: 'issues filter on, has property issues, submit clicked'
    },
    {
      transitionInProgress: false,
      hasStepConfigIssues: false,
      issuesFilterSelected: true,
      issuesFilterText: Em.I18n.t('installer.step7.showingPropertiesWithIssues'),
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, no property issues, submit not clicked'
    },
    {
      transitionInProgress: false,
      hasStepConfigIssues: true,
      issuesFilterSelected: false,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showPropertiesWithIssues'),
      title: 'issues filter off, has property issues, submit not clicked'
    },
    {
      transitionInProgress: false,
      hasStepConfigIssues: true,
      issuesFilterSelected: false,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showPropertiesWithIssues'),
      title: 'issues filter off, has property issues, submit not clicked'
    },
    {
      transitionInProgress: false,
      hasStepConfigIssues: true,
      issuesFilterSelected: false,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showPropertiesWithIssues'),
      title: 'issues filter off, has property issues, submit not clicked'
    }
  ],
  issuesFilterTestSetup = function (controller, testCase) {
    controller.set('submitButtonClicked', testCase.submitButtonClicked);
    controller.reopen({
      isSubmitDisabled: testCase.isSubmitDisabled,
      transitionInProgress: testCase.transitionInProgress,
      issuesFilterSelected: testCase.issuesFilterSelected,
      hasStepConfigIssues: testCase.hasStepConfigIssues
    });
  };

function getController() {
  return App.WizardStep7Controller.create({
    content: Em.Object.create({
      services: [],
      advancedServiceConfig: [],
      serviceConfigProperties: []
    })
  });
}

describe('App.InstallerStep7Controller', function () {

  beforeEach(function () {
    sinon.stub(App.config, 'setPreDefinedServiceConfigs', Em.K);
    installerStep7Controller = getController();
    App.set('router.nextBtnClickInProgress', false);
  });

  afterEach(function() {
    App.set('router.nextBtnClickInProgress', false);
    App.config.setPreDefinedServiceConfigs.restore();
    installerStep7Controller.destroy();
  });

  App.TestAliases.testAsComputedAlias(getController(), 'masterComponentHosts', 'content.masterComponentHosts', 'array');

  App.TestAliases.testAsComputedAlias(getController(), 'slaveComponentHosts', 'content.slaveGroupProperties', 'array');

  App.TestAliases.testAsComputedAnd(getController(), 'isConfigsLoaded', ['wizardController.stackConfigsLoaded', 'isAppliedConfigLoaded']);

  describe('#installedServiceNames', function () {

    var tests = Em.A([
      {
        content: Em.Object.create({
          controllerName: 'installerController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'SQOOP'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['SQOOP', 'HDFS'],
        m: 'installerController with SQOOP'
      },
      {
        content: Em.Object.create({
          controllerName: 'installerController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HIVE'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['HIVE', 'HDFS'],
        m: 'installerController without SQOOP'
      },
      {
        content: Em.Object.create({
          controllerName: 'addServiceController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HIVE'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['HIVE', 'HDFS'],
        m: 'addServiceController without SQOOP'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep7Controller.set('content', test.content);
        expect(installerStep7Controller.get('installedServiceNames')).to.include.members(test.e);
        expect(test.e).to.include.members(installerStep7Controller.get('installedServiceNames'));
      });
    });

  });

  describe('#isSubmitDisabled', function () {
    it('should be true if miscModalVisible', function () {
      installerStep7Controller.reopen({miscModalVisible: true});
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(true);
    });
    it('should be true if some of stepConfigs has errors', function () {
      installerStep7Controller.reopen({
        miscModalVisible: false,
        stepConfigs: [
          {
            showConfig: true,
            errorCount: 1
          }
        ]
      });
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(true);
    });
    it('should be false if all of stepConfigs don\'t have errors and miscModalVisible is false', function () {
      installerStep7Controller.reopen({
        miscModalVisible: false,
        stepConfigs: [
          {
            showConfig: true,
            errorCount: 0
          }
        ]
      });
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(false);
    });
  });

  describe('#selectedServiceNames', function () {
    it('should use content.services as source of data', function () {
      installerStep7Controller.set('content', {
        services: [
          {isSelected: true, isInstalled: false, serviceName: 's1'},
          {isSelected: false, isInstalled: false, serviceName: 's2'},
          {isSelected: true, isInstalled: true, serviceName: 's3'},
          {isSelected: false, isInstalled: false, serviceName: 's4'},
          {isSelected: true, isInstalled: false, serviceName: 's5'},
          {isSelected: false, isInstalled: false, serviceName: 's6'},
          {isSelected: true, isInstalled: true, serviceName: 's7'},
          {isSelected: false, isInstalled: false, serviceName: 's8'}
        ]
      });
      var expected = ['s1', 's5'];
      expect(installerStep7Controller.get('selectedServiceNames')).to.eql(expected);
    });
  });

  describe('#allSelectedServiceNames', function () {
    it('should use content.services as source of data', function () {
      installerStep7Controller.set('content', {
        services: [
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's1'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's2'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's3'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's4'}),
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's5'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's6'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's7'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's8'})
        ]
      });
      var expected = ['s1', 's3', 's5', 's7'];
      expect(installerStep7Controller.get('allSelectedServiceNames')).to.eql(expected);
    });
  });

  describe('#checkDatabaseConnectionTest', function () {

    beforeEach(function () {
      installerStep7Controller.set('content', {
        services: Em.A([
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 'OOZIE', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 'HIVE', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's3', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's4', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's5', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's6', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's7', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's8', ignored: []})
        ])
      });
      var obj = Em.Object.create({name:'oozie_database',value:"aa"});
      installerStep7Controller.set('stepConfigs',Em.A([Em.Object.create({serviceName: 'OOZIE', configs: Em.A([obj]) })]));
      this.deffer = installerStep7Controller.checkDatabaseConnectionTest();
    });

    it('should return promise in process', function () {
      expect(this.deffer.isResolved()).to.equal(false);
      this.deffer.resolve(true);
      this.deffer.done(function(data) {
        expect(data).to.equal(true);
      });
    });
  });

  describe.skip('#submit', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('supports.preInstallChecks').returns(false);
    });

    afterEach(function () {
      App.get.restore();
    });

    it('should return false if submit disabled', function () {
      installerStep7Controller.set('isSubmitDisabled',true);
      expect(installerStep7Controller.submit()).to.be.false;
    });
    it('sumbit button should be unclicked if no configs', function () {
      installerStep7Controller.set('isSubmitDisabled',false);
      installerStep7Controller.submit();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });
    it('if Next button is clicked multiple times before the next step renders, it must not be processed',function(){
      installerStep7Controller.submit();
      expect(App.router.send.calledWith('next')).to.equal(true);

      App.router.send.reset();
      installerStep7Controller.submit();
      expect(App.router.send.calledWith('next')).to.equal(false);
    });
  });

  describe('#getConfigTagsSuccess', function () {
    beforeEach(function(){
      sinon.stub(App.StackService, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 's0',
            isInstalled: true,
            configTypes: {
              site3: true,
              site1: true
            }
          }),
          Em.Object.create({
            serviceName: 's1',
            isInstalled: true,
            configTypes: {
              site1: true,
              site2: true
            }
          })
        ];
      });
    });
    afterEach(function(){
      App.StackService.find.restore();
    });

    it('should return serviceConfigTags', function () {
      var desiredConfigs = {
        site1: {
          tag: "tag1"
        },
        site2: {
          tag: "tag2"
        },
        site3: {
          tag: "tag3"
        }
      };
      var data = {
        Clusters: {
          desired_configs: desiredConfigs
        }
      };
      installerStep7Controller.getConfigTagsSuccess(data);
      expect(installerStep7Controller.get('serviceConfigTags')).to.eql([
        {
          "siteName": "site1",
          "tagName": "tag1",
          "newTagName": null
        },
        {
          "siteName": "site2",
          "tagName": "tag2",
          "newTagName": null
        },
        {
          "siteName": "site3",
          "tagName": "tag3",
          "newTagName": null
        }
      ]);
      expect(installerStep7Controller.get('isAppliedConfigLoaded')).to.equal(true);
    });
  });

  describe('#clearStep', function () {

    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'abortRequests');
    });

    afterEach(function () {
      installerStep7Controller.abortRequests.restore();
    });

    it('should clear stepConfigs', function () {
      installerStep7Controller.set('stepConfigs', [
        {},
        {}
      ]);
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('stepConfigs.length')).to.equal(0);
    });
    it('should clear filter', function () {
      installerStep7Controller.set('filter', 'filter');
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('filter')).to.equal('');
    });
    it('should set for each filterColumns "selected" false', function () {
      installerStep7Controller.set('filterColumns', [
        {selected: true},
        {selected: false},
        {selected: true}
      ]);
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('filterColumns').everyProperty('selected', false)).to.equal(true);
    });
    it('should call abortRequests', function () {
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.abortRequests.calledOnce).to.be.true;
    });
  });

  describe('#getConfigTags', function () {
    it('should do ajax-request', function () {
      installerStep7Controller.getConfigTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args).exists;
    });
  });

  describe('#setGroupsToDelete', function () {
    beforeEach(function () {
      installerStep7Controller.set('wizardController', Em.Object.create(App.LocalStorage, {name: 'tdk'}));
    });
    it('should add new groups to groupsToDelete', function () {
      var groupsToDelete = [
          {id: '1'},
          {id: '2'}
        ],
        groups = [
          Em.Object.create({id: '3', isTemporary: false}),
          Em.Object.create({id: '4', isTemporary: true}),
          Em.Object.create({id: '5', isTemporary: false})
        ],
        expected = [
          {id: "1"},
          {id: "2"},
          {id: "3"},
          {id: "5"}
        ];
      installerStep7Controller.set('groupsToDelete', groupsToDelete);
      installerStep7Controller.setGroupsToDelete(groups);
      expect(installerStep7Controller.get('groupsToDelete')).to.eql(expected);
      expect(installerStep7Controller.get('wizardController').getDBProperty('groupsToDelete')).to.eql(expected);
    });
  });

  describe('#checkMySQLHost', function () {
    it('should send query', function () {
      installerStep7Controller.checkMySQLHost();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service');
      expect(args).exists;
    });
  });

  describe('#selectConfigGroup', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({content: {services: []}});
      sinon.stub(installerStep7Controller, 'switchConfigGroupConfigs', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.switchConfigGroupConfigs.restore();
    });
    it('should set selectedConfigGroup', function () {
      var group = {':': []};
      installerStep7Controller.selectConfigGroup({context: group});
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(group);
    });
  });

  describe('#selectedServiceObserver', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({content: {services: []}});
      sinon.stub(installerStep7Controller, 'switchConfigGroupConfigs', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.switchConfigGroupConfigs.restore();
    });
    it('shouldn\'t do nothing if App.supports.hostOverridesInstaller is false', function () {
      App.set('supports.hostOverridesInstaller', false);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('shouldn\'t do nothing if selectedService is null', function () {
      App.set('supports.hostOverridesInstaller', true);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({selectedService: null, configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('shouldn\'t do nothing if selectedService.serviceName is MISC', function () {
      App.set('supports.hostOverridesInstaller', true);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({selectedService: {serviceName: 'MISC'}, configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('should update configGroups and selectedConfigGroup', function () {
      App.set('supports.hostOverridesInstaller', true);
      var defaultGroup = {isDefault: true, n: 'n2'},
        configGroups = [
          {isDefault: false, n: 'n1'},
          defaultGroup,
          {n: 'n3'}
        ];
      installerStep7Controller.reopen({selectedService: {serviceName: 's1', configGroups: configGroups}});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups').mapProperty('n')).to.eql(['n2', 'n1', 'n3']);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(defaultGroup);
    });
  });

  describe('#loadConfigGroups', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({
        wizardController: Em.Object.create({
          allHosts: [
            {hostName: 'h1'},
            {hostName: 'h2'},
            {hostName: 'h3'}
          ]
        })
      });
    });
    afterEach(function () {
      App.ServiceConfigGroup.find().clear();
    });
    it('shouldn\'t do nothing if only MISC available', function () {
      var configGroups = [
        {}
      ];
      installerStep7Controller.reopen({
        stepConfigs: [Em.Object.create({serviceName: 'MISC', configGroups: configGroups})]
      });
      installerStep7Controller.loadConfigGroups([]);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups')).to.eql(configGroups);
    });
  });

  describe('#_getDisplayedConfigGroups', function () {
    it('should return [] if no selected group', function () {
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: null
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql([]);
    });
    it('should return default config group if another selected', function () {
      var defaultGroup = Em.Object.create({isDefault: false});
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: defaultGroup
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql([defaultGroup]);
    });
    it('should return other groups if default selected', function () {
      var defaultGroup = Em.Object.create({isDefault: true}),
        cfgG = Em.Object.create({isDefault: true}),
        configGroups = Em.A([
          Em.Object.create({isDefault: false}),
          Em.Object.create({isDefault: false}),
          cfgG,
          Em.Object.create({isDefault: false})
        ]);
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: defaultGroup,
        selectedService: {configGroups: configGroups}
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql(configGroups.without(cfgG));
    });
  });

  describe('#_setEditableValue', function () {
    it('shouldn\'t update config if no selectedConfigGroup', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null
      });
      var config = Em.Object.create({isEditable: null});
      var updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.be.null;
    });
    it('should set isEditable equal to selectedGroup.isDefault if service not installed', function () {
      var isDefault = true;
      installerStep7Controller.reopen({
        installedServiceNames: [],
        selectedService: {serviceName: 'abc'},
        selectedConfigGroup: Em.Object.create({isDefault: isDefault})
      });
      var config = Em.Object.create({isEditable: true});
      var updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.equal(isDefault);
      installerStep7Controller.toggleProperty('selectedConfigGroup.isDefault');
      updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.equal(!isDefault);
    });
    Em.A([
        {
          isEditable: false,
          isReconfigurable: false,
          isDefault: true,
          e: false
        },
        {
          isEditable: true,
          isReconfigurable: true,
          isDefault: true,
          e: true
        },
        {
          isEditable: false,
          isReconfigurable: true,
          isDefault: false,
          e: false
        },
        {
          isEditable: true,
          isReconfigurable: false,
          isDefault: false,
          e: false
        }
      ]).forEach(function (test) {
        it('service installed, isEditable = ' + test.isEditable.toString() + ', isReconfigurable = ' + test.isReconfigurable.toString(), function () {
          var config = Em.Object.create({
            isReconfigurable: test.isReconfigurable,
            isEditable: test.isEditable
          });
          installerStep7Controller.reopen({
            installedServiceNames: Em.A(['a']),
            selectedService: Em.Object.create({serviceName: 'a'}),
            selectedConfigGroup: Em.Object.create({isDefault: test.isDefault})
          });
          var updateConfig = installerStep7Controller._setEditableValue(config);
          expect(updateConfig.get('isEditable')).to.equal(test.e);
        });
      });
  });

  describe('#_setOverrides', function () {

    it('shouldn\'t update config if no selectedConfigGroup', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null
      });
      var config = Em.Object.create({overrides: null});
      var updatedConfig = installerStep7Controller._setOverrides(config, []);
      expect(updatedConfig.get('overrides')).to.be.null;
    });

    describe('no overrideToAdd', function () {
      var isDefault;
      beforeEach(function () {
        isDefault = true;
        var id = 'n1',
          config = Em.Object.create({overrides: null, id: id, flag: 'flag'}),
          overrides = Em.A([
            Em.Object.create({id: id, value: 'v1'}),
            Em.Object.create({id: id, value: 'v2'}),
            Em.Object.create({id: 'n2', value: 'v3'})
          ]);
        installerStep7Controller.reopen({
          overrideToAdd: null,
          selectedConfigGroup: Em.Object.create({
            isDefault: isDefault
          })
        });
        this.updatedConfig = installerStep7Controller._setOverrides(config, overrides);
      });

      it('2 overrides', function () {
        expect(this.updatedConfig.get('overrides.length')).to.equal(2);
      });
      it('each isEditable is ' + !isDefault, function () {
        expect(this.updatedConfig.get('overrides').everyProperty('isEditable', !isDefault)).to.equal(true);
      });
      it('each parentSCP.flag is `flag`', function () {
        expect(this.updatedConfig.get('overrides').everyProperty('parentSCP.flag', 'flag')).to.equal(true);
      });
    });

    describe('overrideToAdd exists', function () {
      var isDefault = true;
      beforeEach(function () {
        var id = 'n1',
            config = Em.Object.create({overrides: null, id: id, flag: 'flag'}),
            overrides = Em.A([
              Em.Object.create({id: id, value: 'v1'}),
              Em.Object.create({id: id, value: 'v2'}),
              Em.Object.create({id: 'n2', value: 'v3'})
            ]);
        installerStep7Controller.reopen({
          overrideToAdd: Em.Object.create({id: id}),
          selectedService: {configGroups: [Em.Object.create({id: 'n', properties: []})]},
          selectedConfigGroup: Em.Object.create({
            isDefault: isDefault,
            id: 'n'
          })
        });
        this.updatedConfig = installerStep7Controller._setOverrides(config, overrides);
      });

      it('3 overrides', function () {
        expect(this.updatedConfig.get('overrides.length')).to.equal(3);
      });
      it('each isEditable is ' + !isDefault, function () {
        expect(this.updatedConfig.get('overrides').everyProperty('isEditable', !isDefault)).to.equal(true);
      });
      it('each parentSCP.flag is `flag`', function () {
        expect(this.updatedConfig.get('overrides').everyProperty('parentSCP.flag', 'flag')).to.equal(true);
      });
    });
  });

  describe('#switchConfigGroupConfigs', function () {

    it('if selectedConfigGroup is null, serviceConfigs shouldn\'t be changed', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null,
        content: {services: []},
        serviceConfigs: {configs: [
          {overrides: []},
          {overrides: []}
        ]}
      });
      installerStep7Controller.switchConfigGroupConfigs();
      expect(installerStep7Controller.get('serviceConfigs.configs').everyProperty('overrides.length', 0)).to.equal(true);
    });

    describe('should set configs for serviceConfigs', function () {

      var configGroups = [
        Em.Object.create({
          properties: [
            {id: 'g1', value: 'v1'},
            {id: 'g2', value: 'v2'}
          ]
        })
      ];

      beforeEach(function () {
        sinon.stub(installerStep7Controller, '_getDisplayedConfigGroups', function () {
          return configGroups;
        });
        sinon.stub(installerStep7Controller, '_setEditableValue', function (config) {
          config.set('isEditable', true);
          return config;
        });
        installerStep7Controller.reopen({
          selectedConfigGroup: Em.Object.create({isDefault: true, name: 'g1'}),
          content: {services: []},
          selectedService: {configs: Em.A([Em.Object.create({id: 'g1', overrides: [], properties: []}), Em.Object.create({id: 'g2', overrides: []})])},
          serviceConfigs: {configs: [Em.Object.create({id: 'g1'})]}
        });
        installerStep7Controller.switchConfigGroupConfigs();
        this.configs = installerStep7Controller.get('selectedService.configs');
      });

      afterEach(function () {
        installerStep7Controller._getDisplayedConfigGroups.restore();
        installerStep7Controller._setEditableValue.restore();
      });

      it('g1 has 1 override', function () {
        expect(this.configs.findProperty('id', 'g1').get('overrides').length).to.equal(1);
      });

      it('g2 has 1 override', function () {
        expect(this.configs.findProperty('id', 'g2').get('overrides').length).to.equal(1);
      });

      it('all configs are editable', function () {
        expect(this.configs.everyProperty('isEditable', true)).to.equal(true);
      });

    });

  });

  describe('#selectProperService', function () {
    Em.A([
        {
          name: 'addServiceController',
          stepConfigs: [
            {selected: false, name: 'n1'},
            {selected: true, name: 'n2'},
            {selected: true, name: 'n3'}
          ],
          tabs: [
            Em.Object.create({isActive: true, selectedServiceName: null})
          ],
          e: 'n2'
        },
        {
          name: 'installerController',
          stepConfigs: [
            {showConfig: false, name: 'n1'},
            {showConfig: false, name: 'n2'},
            {showConfig: true, name: 'n3'}
          ],
          tabs: [
            Em.Object.create({isActive: true, selectedServiceName: null})
          ],
          e: 'n3'
        },
        {
          name: 'addServiceController',
          stepConfigs: [
            {selected: true, name: 'n1'},
            {selected: true, name: 'n2'},
            {selected: true, name: 'n3'}
          ],
          tabs: [
            Em.Object.create({isActive: true, selectedServiceName: 'n1'})
          ],
          e: 'n1'
        },
        {
          name: 'installerController',
          stepConfigs: [
            {showConfig: true, name: 'n1'},
            {showConfig: false, name: 'n2'},
            {showConfig: true, name: 'n3'}
          ],
          tabs: [
            Em.Object.create({isActive: true, selectedServiceName: 'n1'})
          ],
          e: 'n1'
        }
      ]).forEach(function (test) {
        describe(test.name, function () {

          beforeEach(function () {
            sinon.stub(installerStep7Controller, 'selectedServiceObserver', Em.K);
            installerStep7Controller.reopen({
              wizardController: Em.Object.create({
                name: test.name
              }),
              stepConfigs: test.stepConfigs,
              tabs: test.tabs
            });
            installerStep7Controller.selectProperService();
          });

          afterEach(function () {
            installerStep7Controller.selectedServiceObserver.restore();
          });

          it('selected service name is valid', function () {
            expect(installerStep7Controller.get('selectedService.name')).to.equal(test.e);
          });
        });
      });
  });

  describe.skip('#setStepConfigs', function () {
    var serviceConfigs;
    beforeEach(function () {
      installerStep7Controller.reopen({
        content: {services: []},
        wizardController: Em.Object.create({
          getDBProperty: function (key) {
            return this.get(key);
          }
        })
      });
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
        return serviceConfigs;
      });
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      installerStep7Controller.renderConfigs.restore();
      App.get.restore();
    });

    it('if wizard isn\'t addService, should set output of installerStep7Controller.renderConfigs', function () {
      serviceConfigs = Em.A([
        {serviceName:'HDFS', configs: []},
        {}
      ]);
      installerStep7Controller.set('wizardController.name', 'installerController');
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs')).to.eql(serviceConfigs);
    });

    it('addServiceWizard used', function () {
      serviceConfigs = Em.A([Em.Object.create({serviceName: 'HDFS', configs: []}), Em.Object.create({serviceName: 's2'})]);
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['s2']});
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').everyProperty('showConfig', true)).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 's2').get('selected')).to.equal(true);
    });

    it('addServiceWizard used, HA enabled', function () {
      this.stub.withArgs('isHaEnabled').returns(true);
      serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            Em.Object.create({category: 'SECONDARY_NAMENODE'}),
            Em.Object.create({category: 'SECONDARY_NAMENODE'}),
            Em.Object.create({category: 'NameNode'}),
            Em.Object.create({category: 'NameNode'}),
            Em.Object.create({category: 'SECONDARY_NAMENODE'})
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').everyProperty('showConfig', true)).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('selected')).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(5);
    });

    it('not windows stack', function () {

      this.stub.withArgs('isHadoopWindowsStack').returns(false);
      this.stub.withArgs('isHaEnabled').returns(false);

      serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            {category: 'NameNode'},
            {category: 'NameNode'}
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );
      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(2);
    });

    it('windows stack', function () {

      this.stub.withArgs('isHadoopWindowsStack').returns(true);
      this.stub.withArgs('isHaEnabled').returns(false);

      serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            {category: 'NameNode'},
            {category: 'NameNode'}
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );

      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      installerStep7Controller.set('installedServiceNames',['HDFS', 's2', 's3']);
      installerStep7Controller.setStepConfigs([], []);

      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(2);

    });

  });

  describe('#loadStep', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({
        content: {services: []},
        wizardController: Em.Object.create({
          getDBProperty: function (k) {
            return this.get(k);
          },
          stackConfigsLoaded: true
        })
      });
      sinon.stub(installerStep7Controller, 'clearStep', Em.K);
      sinon.stub(installerStep7Controller, 'getConfigTags', Em.K);
      sinon.stub(installerStep7Controller, 'setInstalledServiceConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'checkHostOverrideInstaller', Em.K);
      sinon.stub(installerStep7Controller, 'selectProperService', Em.K);
      sinon.stub(installerStep7Controller, 'applyServicesConfigs', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.clearStep.restore();
      installerStep7Controller.getConfigTags.restore();
      installerStep7Controller.setInstalledServiceConfigs.restore();
      installerStep7Controller.checkHostOverrideInstaller.restore();
      installerStep7Controller.selectProperService.restore();
      installerStep7Controller.applyServicesConfigs.restore();
      App.router.send.restore();
    });
    it('should call clearStep', function () {
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.clearStep.calledOnce).to.equal(true);
    });
    it('shouldn\'t do nothing if isAdvancedConfigLoaded is false', function () {
      installerStep7Controller.set('wizardController.stackConfigsLoaded', false);
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.clearStep.called).to.equal(false);
    });
    it('should call setInstalledServiceConfigs for addServiceController', function () {
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.setInstalledServiceConfigs.calledOnce).to.equal(true);
    });
  });

  describe('#applyServicesConfigs', function() {
    beforeEach(function() {
      installerStep7Controller.reopen({
        allSelectedServiceNames: []
      });
      sinon.stub(installerStep7Controller, 'loadConfigRecommendations', function(c, callback) {
        return callback();
      });
      sinon.stub(installerStep7Controller, 'checkHostOverrideInstaller', Em.K);
      sinon.stub(installerStep7Controller, 'selectProperService', Em.K);
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(App.StackService, 'find', function () {
        return {
          findProperty: function () {
            return Em.Object.create({
              isInstalled: true,
              isSelected: false
            });
          },
          filter: function () {
            return [];
          }
        }
      });
      installerStep7Controller.applyServicesConfigs([{name: 'configs'}]);
    });

    afterEach(function () {
      installerStep7Controller.loadConfigRecommendations.restore();
      installerStep7Controller.checkHostOverrideInstaller.restore();
      installerStep7Controller.selectProperService.restore();
      App.router.send.restore();
      App.StackService.find.restore();
    });

    it('loadConfigRecommendations is called once' , function () {
     expect(installerStep7Controller.loadConfigRecommendations.calledOnce).to.equal(true);
    });
    it('isRecommendedLoaded is true' , function () {
     expect(installerStep7Controller.get('isRecommendedLoaded')).to.equal(true);
    });
    it('checkHostOverrideInstalleris called once' , function () {
     expect(installerStep7Controller.checkHostOverrideInstaller.calledOnce).to.equal(true);
    });
    it('selectProperServiceis called once' , function () {
     expect(installerStep7Controller.selectProperService.calledOnce).to.equal(true);
    });

  });

  describe('#removeHawqStandbyHostAddressConfig', function() {
    installerStep7Controller = App.WizardStep7Controller.create({
      content: Em.Object.create({}),
    });
    var testHawqSiteConfigs = [
      {
        name: 'hawq_standby_address_host',
        value: 'h2'
      },
      {
        name: 'hawq_master_address_host',
        value: 'h1'
      }
    ];
    var oldHawqSiteLength = testHawqSiteConfigs.length;

    it('hawq_standby_address_host should be removed on single node cluster', function() {
      var hawqSiteConfigs = testHawqSiteConfigs.slice();
      installerStep7Controller.set('content.hosts', {'hostname': 'h1'});
      var updatedHawqSiteConfigs = installerStep7Controller.updateHawqConfigs(hawqSiteConfigs);
      expect(updatedHawqSiteConfigs.length).to.be.equal(oldHawqSiteLength-1);
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_standby_address_host')).to.not.exist;
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_master_address_host').value).to.be.equal('h1');
    });

    it('hawq_standby_address_host should not be removed on multi node clusters', function() {
      var hawqSiteConfigs = testHawqSiteConfigs.slice();
      installerStep7Controller.set('content.hosts', Em.A([{'hostname': 'h1'}, {'hostname': 'h2'}]));
      var updatedHawqSiteConfigs = installerStep7Controller.updateHawqConfigs(hawqSiteConfigs);
      expect(updatedHawqSiteConfigs.length).to.be.equal(oldHawqSiteLength);
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_standby_address_host').value).to.be.equal('h2');
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_master_address_host').value).to.be.equal('h1');
    });

  });

  describe('#_updateIsEditableFlagForConfig', function () {
    Em.A([
        {
          isAdmin: false,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: false,
          m: 'false for non-admin users',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: false,
          m: 'false if defaultGroupSelected is false and isHostsConfigsPage is true',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: true,
          m: 'false if defaultGroupSelected is true and isHostsConfigsPage is true',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: false,
          defaultGroupSelected: false,
          m: 'false if defaultGroupSelected is false and isHostsConfigsPage is false',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep7Controller.reopen({isHostsConfigsPage: test.isHostsConfigsPage});
          var serviceConfigProperty = Em.Object.create({
            isReconfigurable: test.isReconfigurable,
            isEditable: true
          });
          installerStep7Controller._updateIsEditableFlagForConfig(serviceConfigProperty, test.defaultGroupSelected);
          expect(serviceConfigProperty.get('isEditable')).to.equal(test.e);
        });
      });
  });

  describe('#setInstalledServiceConfigs', function () {

    var controller = App.WizardStep7Controller.create({
        installedServiceNames: ['HBASE', 'AMBARI_METRICS']
      }),
      configs = [
        {
          name: 'hbase.client.scanner.caching',
          value: '1000',
          serviceName: 'HBASE',
          filename: 'hbase-site.xml'
        },
        {
          name: 'hbase.client.scanner.caching',
          value: '2000',
          serviceName: 'AMBARI_METRICS',
          filename: 'ams-hbase-site.xml'
        }
      ],
      configsByTags = [
        {
          type: 'hbase-site',
          tag: 'version2',
          properties: {
            'hbase.client.scanner.caching': '1500'
          }
        },
        {
          type: 'ams-hbase-site',
          tag: 'version2',
          properties: {
            'hbase.client.scanner.caching': '2500'
          }
        },
        {
          type: 'site-without-properties',
          tag: 'version1'
        }
      ],
      installedServiceNames = ['HBASE', 'AMBARI_METRICS'];

    describe('should handle properties with the same name', function () {
      var properties;
      beforeEach(function () {
        controller.setInstalledServiceConfigs(configs, configsByTags, installedServiceNames);
        properties = configs.filterProperty('name', 'hbase.client.scanner.caching');
      });
      it('there are 2 properties', function () {
        expect(properties).to.have.length(2);
      });
      it('hbase-site/ value is valid', function () {
        expect(properties.findProperty('filename', 'hbase-site.xml').value).to.equal('1500');
      });
      it('hbase-site/ savedValue is valid', function () {
        expect(properties.findProperty('filename', 'hbase-site.xml').savedValue).to.equal('1500');
      });
      it('ams-hbase-site/ value is valid', function () {
        expect(properties.findProperty('filename', 'ams-hbase-site.xml').value).to.equal('2500');
      });
      it('ams-hbase-site/ savedValue is valid', function () {
        expect(properties.findProperty('filename', 'ams-hbase-site.xml').savedValue).to.equal('2500');
      });
    });

  });

  describe('#getAmbariDatabaseSuccess', function () {

    var controller = App.WizardStep7Controller.create({
        stepConfigs: [
          {
            serviceName: 'HIVE',
            configs: [
              {
                name: 'javax.jdo.option.ConnectionURL',
                value: 'jdbc:mysql://h0/db_name?createDatabaseIfNotExist=true',
                filename: 'hive-site.xml'
              }
            ]
          }
        ]
      }),
      cases = [
        {
          data: {
            hostComponents: []
          },
          mySQLServerConflict: false,
          title: 'no Ambari Server host components'
        },
        {
          data: {
            hostComponents: [
              {
                RootServiceHostComponents: {
                  host_name: 'h0',
                  properties: {
                    'server.jdbc.database': 'postgres'
                  }
                }
              }
            ]
          },
          mySQLServerConflict: false,
          title: 'Ambari MySQL Server and Hive Server are on the same host but different database types'
        },
        {
          data: {
            hostComponents: [
              {
                RootServiceHostComponents: {
                  host_name: 'h0',
                  properties: {
                    'server.jdbc.database': 'mysql'
                  }
                }
              }
            ]
          },
          mySQLServerConflict: true,
          title: 'Ambari MySQL Server and Hive Server are on the same host'
        },
        {
          data: {
            hostComponents: [
              {
                RootServiceHostComponents: {
                  host_name: 'h1',
                  properties: {
                    'server.jdbc.database': 'mysql'
                  }
                }
              }
            ]
          },
          mySQLServerConflict: false,
          title: 'Ambari MySQL Server and Hive Server are on different hosts'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.getAmbariDatabaseSuccess(item.data);
        expect(controller.get('mySQLServerConflict')).to.equal(item.mySQLServerConflict);
      });
    });

  });

  describe('#showDatabaseConnectionWarningPopup', function () {

    var cases = [
        {
          method: 'onSecondary',
          submitButtonClicked: false,
          isRejected: true,
          title: 'Cancel button clicked'
        },
        {
          method: 'onPrimary',
          submitButtonClicked: true,
          isResolved: true,
          title: 'Proceed Anyway button clicked'
        }
      ],
      dfd,
      testObject,
      serviceNames = ['HIVE', 'OOZIE'],
      bodyMessage = 'HIVE, OOZIE';

    beforeEach(function () {
      installerStep7Controller.set('submitButtonClicked', true);
      dfd = $.Deferred(function (d) {
        d.done(function () {
          testObject.isResolved = true;
        });
        d.fail(function () {
          testObject.isRejected = true;
        })
      });
      testObject = {};
    });

    cases.forEach(function (item) {
      describe(item.title, function () {
        var popup;
        beforeEach(function () {
          popup = installerStep7Controller.showDatabaseConnectionWarningPopup(serviceNames, dfd);
        });

        it('popup body is valid', function () {
          expect(popup.get('body')).to.equal(Em.I18n.t('installer.step7.popup.database.connection.body').format(bodyMessage));
        });

        it('after ' + item.method + ' execution', function () {
          popup[item.method]();
          expect(testObject.isResolved).to.equal(item.isResolved);
          expect(testObject.isRejected).to.equal(item.isRejected);
          expect(installerStep7Controller.get('submitButtonClicked')).to.equal(item.submitButtonClicked);
        });
      });
    });

  });

  describe('#issuesFilterText', function () {

    issuesFilterCases.forEach(function (item) {
      it(item.title, function () {
        issuesFilterTestSetup(installerStep7Controller, item);
        expect(installerStep7Controller.get('issuesFilterText')).to.equal(item.issuesFilterText);
      })
    });

  });

  describe.skip('#loadServiceTagsSuccess', function () {
    it('should create ClusterSiteToTagMap', function () {
      var params = Em.Object.create({
        serviceName: "OOZIE",
        serviceConfigsDef: Em.Object.create({
          configTypes: Em.Object.create({
            site3: true,
            site2: true,
            site1: true
          })
        })
      });
      var wizardController = Em.Object.create({
          allHosts: [
            {hostName: 'h1'},
            {hostName: 'h2'},
            {hostName: 'h3'}
          ]
      });
      installerStep7Controller.set('wizardController', wizardController);
      installerStep7Controller.set('stepConfigs', Em.A([Em.Object.create({serviceName: 'OOZIE', configs: Em.A([]) })]));
      var desiredConfigs = {
        site1: {
          tag: "tag1"
        },
        site2: {
          tag: "tag2"
        },
        site3: {
          tag: "tag3"
        }
      };
      var data = {
        config_groups: Em.A([Em.Object.create({
          ConfigGroup: Em.Object.create({
            tag: 'OOZIE',
            hosts: Em.A([Em.Object.create({host_name: 'h1'})]),
            id: 1,
            group_name: "",
            description: "",
            desired_configs: Em.A([Em.Object.create({
              type: '1',
              tag: 'h1'
            })])
          })
        })]),
        Clusters: {
          desired_configs: desiredConfigs
        }
      };
      installerStep7Controller.loadServiceTagsSuccess(data, {}, params);
      var result = installerStep7Controller.get("loadedClusterSiteToTagMap");
      expect(JSON.parse(JSON.stringify(result))).to.eql(JSON.parse(JSON.stringify({"site1":"tag1","site2":"tag2","site3":"tag3"})));
    })
  });

  describe('#issuesFilterLinkText', function () {

    issuesFilterCases.forEach(function (item) {
      it(item.title, function () {
        issuesFilterTestSetup(installerStep7Controller, item);
        expect(installerStep7Controller.get('issuesFilterLinkText')).to.equal(item.issuesFilterLinkText);
      });
    });

  });

  describe('#toggleIssuesFilter', function () {
    it('should toggle issues filter', function () {
      var issuesFilter = installerStep7Controller.get('filterColumns').findProperty('attributeName', 'hasIssues');
      issuesFilter.set('selected', false);
      installerStep7Controller.toggleIssuesFilter();
      expect(issuesFilter.get('selected')).to.be.true;
      installerStep7Controller.toggleIssuesFilter();
      expect(issuesFilter.get('selected')).to.be.false;
    });
    it('selected service should be changed', function () {
      installerStep7Controller.setProperties({
        selectedService: {
          serviceName: 'service1',
          errorCount: 0,
          configGroups: [],
          showConfig: true
        },
        stepConfigs: [
          Em.Object.create({
            serviceName: 'service2',
            errorCount: 1,
            configGroups: [],
            showConfig: true
          }),
          Em.Object.create({
            serviceName: 'service3',
            errorCount: 2,
            configGroups: [],
            showConfig: true
          })
        ]
      });
      installerStep7Controller.toggleIssuesFilter();
      expect(installerStep7Controller.get('selectedService.serviceName')).to.be.equal('service2');
    });
  });

  describe('#addKerberosDescriptorConfigs', function() {

    beforeEach(function() {
      sinon.stub(App.config, 'kerberosIdentitiesDescription');
      installerStep7Controller.set('content.services', [
        {isSelected: true, serviceName: 's1'}
      ]);
    });

    afterEach(function() {
      App.config.kerberosIdentitiesDescription.restore();
    });

    var configs = [
      { name: 'prop1', displayName: 'Prop1', description: 'd1', serviceName: 's1' },
      { name: 'prop2', displayName: 'Prop2', description: 'd1', serviceName: 's2' },
      { name: 'prop3', displayName: 'Prop3', description: 'd1', serviceName: 's2' },
      { name: 'prop4', displayName: 'Prop4', description: 'd1', serviceName: 's3' }
    ];
    var descriptor = [
      Em.Object.create({ name: 'prop4', filename: 'file-1', serviceName: 's2'}),
      Em.Object.create({ name: 'prop1', filename: 'file-1', serviceName: 's1'})
    ];
    var propertiesAttrTests = [
      {
        attr: 'isUserProperty', val: false,
        m: 'descriptor properties should not be marked as custom'
      },
      {
        attr: 'category', val: 'Advanced file-1',
        m: 'descriptor properties should be added to Advanced category'
      },
      {
        attr: 'isOverridable', val: false,
        m: 'descriptor properties should not be overriden'
      }
    ];

    propertiesAttrTests.forEach(function(test) {
      it(test.m, function() {
        installerStep7Controller.addKerberosDescriptorConfigs(configs, descriptor);
        expect(configs.findProperty('name', 'prop1')[test.attr]).to.be.eql(test.val);
      });
    });
  });

  describe('#addHawqConfigsOnNnHa', function () {
    var configs = [
      {
        id: 'dfs.nameservices__hdfs-site',
        description: 'dfs.nameservices__hdfs-site',
        displayName: 'dfs.nameservices',
        displayType: 'string',
        name: 'dfs.nameservices',
        value: 'haservice',
        recommendedValue: 'haservice'
      },
      {
        id: 'dfs.ha.namenodes.haservice__hdfs-site',
        description: 'dfs.ha.namenodes.haservice__hdfs-site',
        displayName: 'dfs.ha.namenodes.haservice',
        displayType: 'string',
        name: 'dfs.ha.namenodes.haservice',
        value: 'nn1,nn2',
        recommendedValue: 'nn1,nn2'
      },
      {
        id: 'dfs.namenode.rpc-address.haservice.nn1__hdfs-site',
        description: 'dfs.namenode.rpc-address.haservice.nn1__hdfs-site',
        displayName: 'dfs.namenode.rpc-address.haservice.nn1',
        displayType: 'string',
        name: 'dfs.namenode.rpc-address.haservice.nn1',
        value: 'c6401.ambari.apache.org:8020',
        recommendedValue: 'c6401.ambari.apache.org:8020'
      },
      {
        id: 'dfs.namenode.rpc-address.haservice.nn2__hdfs-site',
        description: 'dfs.namenode.rpc-address.haservice.nn2__hdfs-site',
        displayName: 'dfs.namenode.rpc-address.haservice.nn2',
        displayType: 'string',
        name: 'dfs.namenode.rpc-address.haservice.nn2',
        value: 'c6402.ambari.apache.org:8020',
        recommendedValue: 'c6402.ambari.apache.org:8020'
      },
      {
        id: 'dfs.namenode.http-address.haservice.nn1__hdfs-site',
        description: 'dfs.namenode.http-address.haservice.nn1__hdfs-site',
        displayName: 'dfs.namenode.http-address.haservice.nn1',
        displayType: 'string',
        name: 'dfs.namenode.http-address.haservice.nn1',
        value: 'c6401.ambari.apache.org:50070',
        recommendedValue: 'c6401.ambari.apache.org:50070'
      },
      {
        id: 'dfs.namenode.http-address.haservice.nn2__hdfs-site',
        description: 'dfs.namenode.http-address.haservice.nn2__hdfs-site',
        displayName: 'dfs.namenode.http-address.haservice.nn2',
        displayType: 'string',
        name: 'dfs.namenode.http-address.haservice.nn2',
        value: 'c6402.ambari.apache.org:50070',
        recommendedValue: 'c6402.ambari.apache.org:50070'
      }
    ];
    var oldConfigs = configs.slice();

    it('should copy properties from hdfs-site to hdfs-client for HAWQ', function() {
      installerStep7Controller.addHawqConfigsOnNnHa(configs);
      // ensure 6 new configs were added
      expect(configs.length).to.be.equal(oldConfigs.length + 6);
    });

    describe('find the same property in hdfs-client for HAWQ and see if attribute value matches with the corresponding property\'s attribute value in hdfs-site', function () {
      oldConfigs.forEach(function(property) {
        var id = property.name + '__hdfs-client';
        it(id, function () {
          expect(configs.findProperty('id', id).description).to.be.equal(property.description);
          expect(configs.findProperty('id', id).displayName).to.be.equal(property.displayName);
          expect(configs.findProperty('id', id).value).to.be.equal(property.value);
          expect(configs.findProperty('id', id).recommendedValue).to.be.equal(property.recommendedValue);
        });
      });
    });
  });

  describe('#addHawqConfigsOnRMHa', function () {
    var configs = [
      {
        id: 'yarn.resourcemanager.hostname.rm1__yarn-site',
        name: 'yarn.resourcemanager.hostname.rm1',
        value: 'c6401.ambari.apache.org',
        recommendedValue: 'c6401.ambari.apache.org'
      },
      {
        id: 'yarn.resourcemanager.hostname.rm2__yarn-site',
        name: 'yarn.resourcemanager.hostname.rm2',
        value: 'c6402.ambari.apache.org',
        recommendedValue: 'c6402.ambari.apache.org'
      }
    ];

    beforeEach(function () {
      this.inputConfigsCount = configs.length;
      installerStep7Controller.addHawqConfigsOnRMHa(configs);
      this.yarnRmDetails = configs.findProperty('id', 'yarn.resourcemanager.ha__yarn-client');
      this.yarnRmSchedulerDetails = configs.findProperty('id', 'yarn.resourcemanager.scheduler.ha__yarn-client');
    });

    it('should update properties in yarn-client for HAWQ if yarn ha is enabled', function() {
      var noOfConfigsAdded = 2;
      expect(configs.length).to.be.equal(this.inputConfigsCount + noOfConfigsAdded);
    });

    it('yarn.resourcemanager.ha__yarn-client', function() {
      var expectedYarnRmHaValue = 'c6401.ambari.apache.org:8032,c6402.ambari.apache.org:8032';
      expect(this.yarnRmDetails.value).to.be.equal(expectedYarnRmHaValue);
      expect(this.yarnRmDetails.recommendedValue).to.be.equal(expectedYarnRmHaValue);
      expect(this.yarnRmDetails.displayName).to.be.equal('yarn.resourcemanager.ha');
      expect(this.yarnRmDetails.description).to.be.equal('Comma separated yarn resourcemanager host addresses with port');
    });

    it('yarn.resourcemanager.scheduler.ha__yarn-client', function() {
      var expectedYarnRmSchedulerValue = 'c6401.ambari.apache.org:8030,c6402.ambari.apache.org:8030';
      expect(this.yarnRmSchedulerDetails.value).to.be.equal(expectedYarnRmSchedulerValue);
      expect(this.yarnRmSchedulerDetails.recommendedValue).to.be.equal(expectedYarnRmSchedulerValue);
      expect(this.yarnRmSchedulerDetails.displayName).to.be.equal('yarn.resourcemanager.scheduler.ha');
      expect(this.yarnRmSchedulerDetails.description).to.be.equal('Comma separated yarn resourcemanager scheduler addresses with port');
    });
  });

  describe('#errorsCount', function () {

    it('should ignore configs with isInDefaultTheme=false', function () {

      installerStep7Controller.reopen({selectedService: Em.Object.create({
          configsWithErrors: Em.A([
            Em.Object.create({isInDefaultTheme: true}),
            Em.Object.create({isInDefaultTheme: null})
          ])
        })
      });

      expect(installerStep7Controller.get('errorsCount')).to.equal(1);

    });

  });

  describe('#_reconfigureServicesOnNnHa', function () {

    var dfsNameservices = 'some_cluster';

    Em.A([
      {
        serviceName: 'HBASE',
        configToUpdate: 'hbase.rootdir',
        oldValue: 'hdfs://nameserv:8020/apps/hbase/data',
        expectedNewValue: 'hdfs://' + dfsNameservices + '/apps/hbase/data'
      },
      {
        serviceName: 'ACCUMULO',
        configToUpdate: 'instance.volumes',
        oldValue: 'hdfs://localhost:8020/apps/accumulo/data',
        expectedNewValue: 'hdfs://' + dfsNameservices + '/apps/accumulo/data'
      },
      {
        serviceName: 'HAWQ',
        configToUpdate: 'hawq_dfs_url',
        oldValue: 'localhost:8020/hawq_data',
        expectedNewValue: dfsNameservices + '/hawq_data'
      }
    ]).forEach(function (test) {

      var serviceConfigs = [App.ServiceConfig.create({
        serviceName: test.serviceName,
        configs: [
          Em.Object.create({
            name: test.configToUpdate,
            value: test.oldValue
          })
        ]
      }),
        App.ServiceConfig.create({
          serviceName: 'HDFS',
          configs: [
            Em.Object.create({
              name: 'dfs.nameservices',
              value: dfsNameservices
            })
          ]
        })];

      it(test.serviceName + ' ' + test.configToUpdate, function () {
        installerStep7Controller.reopen({
          selectedServiceNames: [test.serviceName, 'HDFS']
        });
        serviceConfigs = installerStep7Controller._reconfigureServicesOnNnHa(serviceConfigs);
        expect(serviceConfigs.findProperty('serviceName', test.serviceName).configs.findProperty('name', test.configToUpdate).get('value')).to.equal(test.expectedNewValue);
      });
    });

  });

  describe('#showOozieDerbyWarning', function() {
    var controller;

    beforeEach(function() {
      controller = App.WizardStep7Controller.create({});
    });

    Em.A([
      {
        selectedServiceNames: ['HDFS', 'OOZIE'],
        databaseType: Em.I18n.t('installer.step7.oozie.database.new'),
        e: true,
        m: 'Oozie selected with derby database, warning popup should be shown'
      },
      {
        selectedServiceNames: ['HDFS'],
        databaseType: Em.I18n.t('installer.step7.oozie.database.new'),
        e: false,
        m: 'Oozie not selected warning popup should be skipped'
      },
      {
        selectedServiceNames: ['HDFS', 'OOZIE'],
        databaseType: 'New Mysql Database',
        e: false,
        m: 'Oozie selected, mysql database used, warning popup should be sk'
      }
    ]).forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          sinon.stub(App.config, 'findConfigProperty').returns(Em.Object.create({ value: test.databaseType}));
          controller.reopen({
            selectedServiceNames: test.selectedServiceNames
          });
          controller.showOozieDerbyWarningPopup(Em.K);
        });

        afterEach(function () {
          App.config.findConfigProperty.restore();
        });

        it('modal popup is shown needed number of times', function () {
          expect(App.ModalPopup.show.calledOnce).to.equal(test.e);
        });
      });
    });
  });

  describe('#addHostNamesToConfigs', function() {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return Em.Object.create({
          id: 'NAMENODE',
          displayName: 'NameNode'
        });
      });
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    it('should not create duplicate configs', function () {
      var serviceConfig = Em.Object.create({
        configs: [],
        serviceName: 'HDFS',
        configCategories: [
          {
            showHost: true,
            name: 'NAMENODE'
          }
        ]
      });
      var masterComponents = [
        {component: 'NAMENODE', hostName: 'h1'}
      ];
      var slaveComponents = [];
      installerStep7Controller.addHostNamesToConfigs(serviceConfig, masterComponents, slaveComponents);
      expect(serviceConfig.get('configs').filterProperty('name', 'namenode_host').length).to.equal(1);
      installerStep7Controller.addHostNamesToConfigs(serviceConfig, masterComponents, slaveComponents);
      expect(serviceConfig.get('configs').filterProperty('name', 'namenode_host').length).to.equal(1);
    });

  });

  describe('#resolveHiveMysqlDatabase', function () {

    beforeEach(function () {
      installerStep7Controller.get('content').setProperties({
        services: Em.A([
          Em.Object.create({serviceName: 'HIVE', isSelected: true, isInstalled: false})
        ])
      });
      installerStep7Controller.setProperties({
        stepConfigs: Em.A([
          Em.Object.create({serviceName: 'HIVE', configs: [{name: 'hive_database', value: 'New MySQL Database'}]})
        ]),
        mySQLServerConflict: true
      });
      sinon.stub(installerStep7Controller, 'moveNext', Em.K);
      sinon.stub(installerStep7Controller, 'checkMySQLHost', function () {
        return $.Deferred().resolve();
      });
    });

    afterEach(function () {
      installerStep7Controller.moveNext.restore();
      installerStep7Controller.checkMySQLHost.restore();
    });

    it('no HIVE service', function () {
      installerStep7Controller.set('content.services', Em.A([]));
      installerStep7Controller.resolveHiveMysqlDatabase();
      expect(installerStep7Controller.moveNext.calledOnce).to.be.true;
      expect(App.ModalPopup.show.called).to.be.false;
    });

    it('if mySQLServerConflict, popup is shown', function () {
      installerStep7Controller.resolveHiveMysqlDatabase();
      expect(installerStep7Controller.moveNext.called).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

  });

  describe('#mySQLWarningHandler', function () {

    beforeEach(function () {
      installerStep7Controller.set('mySQLServerConflict', true);
      sinon.stub(App.router, 'get').returns({gotoStep: Em.K});
      sinon.stub(App.router.get(), 'gotoStep', Em.K);
    });

    afterEach(function () {
      App.router.get().gotoStep.restore();
      App.router.get.restore();
    });

    it('warning popup is shown', function () {
      installerStep7Controller.mySQLWarningHandler();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it('submitButtonClicked is set to false on primary click', function () {
      installerStep7Controller.mySQLWarningHandler().onPrimary();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });

    it('second popup is shown on secondary click', function () {
      installerStep7Controller.mySQLWarningHandler().onSecondary();
      expect(App.ModalPopup.show.calledTwice).to.be.true;
    });

    it('submitButtonClicked is set to false on secondary click on the second popup', function () {
      installerStep7Controller.mySQLWarningHandler().onSecondary().onSecondary();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });

    it('user is moved to step5 on primary click on the second popup (installerController)', function () {
      installerStep7Controller.set('content.controllerName', 'installerController');
      installerStep7Controller.mySQLWarningHandler().onSecondary().onPrimary();
      expect(App.router.get('installerController').gotoStep.calledWith(5, true)).to.be.true;
    });

    it('user is moved to step2 on primary click on the second popup (addSeviceController)', function () {
      installerStep7Controller.set('content.controllerName', 'addServiceController');
      installerStep7Controller.mySQLWarningHandler().onSecondary().onPrimary();
      expect(App.router.get('addSeviceController').gotoStep.calledWith(2, true)).to.be.true;
    });

  });

  describe('#supportsPreInstallChecks', function () {

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      this.stub.restore();
    });

    Em.A([
      {preInstallChecks: true, controllerName: 'installerController', e: true},
      {preInstallChecks: true, controllerName: '', e: false},
      {preInstallChecks: false, controllerName: 'installerController', e: false},
      {preInstallChecks: false, controllerName: '', e: false}
    ]).forEach(function (test) {

      it(JSON.stringify(test), function () {
        this.stub.withArgs('supports.preInstallChecks').returns(test.preInstallChecks);
        installerStep7Controller.set('content', {controllerName: test.controllerName});
        installerStep7Controller.propertyDidChange('supportsPreInstallChecks');

        expect(installerStep7Controller.get('supportsPreInstallChecks')).to.be.equal(test.e);
      });

    });

  });

  describe('#getHash', function () {

    var stepConfigs = [
      {
        configs: [
          Em.Object.create({name: 's1c1', isFinal: true, value: 'v11'}),
          Em.Object.create({name: 's1c2', isFinal: false, value: 'v12', overrides: []}),
          Em.Object.create({name: 's1c3', isFinal: true, value: 'v13', overrides: [
            Em.Object.create({value: 'v131'})
          ]})
        ]
      },
      {
        configs: [
          Em.Object.create({name: 's2c1', isFinal: true, value: 'v21'}),
          Em.Object.create({name: 's2c2', isFinal: false, value: 'v22', overrides: []}),
          Em.Object.create({name: 's2c3', isFinal: true, value: 'v23', overrides: [
            Em.Object.create({value: 'v231'})
          ]})
        ]
      }
    ];

    beforeEach(function () {
      installerStep7Controller.set('stepConfigs', stepConfigs);
      this.hash = installerStep7Controller.getHash();
    });

    it('should map value, isFinal and overrides values', function () {
      var expected = JSON.stringify({
        s1c1: {
          value: 'v11',
          overrides: [],
          isFinal: true
        },
        s1c2: {
          value: 'v12',
          overrides: [],
          isFinal: false
        },
        s1c3: {
          value: 'v13',
          overrides: ['v131'],
          isFinal: true
        },
        s2c1: {
          value: 'v21',
          overrides: [],
          isFinal: true
        },
        s2c2: {
          value: 'v22',
          overrides: [],
          isFinal: false
        },
        s2c3: {
          value: 'v23',
          overrides: ['v231'],
          isFinal: true
        }
      });
      expect(this.hash).to.be.equal(expected);
    });

  });

  describe('#updateHostOverrides', function () {

    var configProperty;
    var storedConfigProperty;

    beforeEach(function () {
      configProperty = Em.Object.create({});
      storedConfigProperty = {
        overrides: [
          {value: 'v1'}
        ]
      };
      installerStep7Controller.updateHostOverrides(configProperty, storedConfigProperty);
    });

    it('override is valid', function () {
      var override = configProperty.get('overrides.0');
      expect(override.get('value')).to.be.equal('v1');
      expect(override.get('isOriginalSCP')).to.be.false;
      expect(override.get('parentSCP')).to.be.eql(configProperty);
    });

  });

  describe('#allowUpdateProperty', function () {

    it('true if it is installer', function () {
      installerStep7Controller.set('wizardController', {name: 'installerController'});
      expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.true;
    });

    it('true if it is parentProperties are not empty', function () {
      installerStep7Controller.set('wizardController', {name: 'some'});
      expect(installerStep7Controller.allowUpdateProperty([{}], '', '')).to.be.true;
    });

    describe('#addServiceController', function () {

      beforeEach(function () {
        installerStep7Controller.set('wizardController', {name: 'addServiceController'});
        this.stub = sinon.stub(App.configsCollection, 'getConfigByName');
        sinon.stub(App.config, 'get').withArgs('serviceByConfigTypeMap').returns({
          't1': Em.Object.create({serviceName: 's1'}),
          't2': Em.Object.create({serviceName: 's2'})
        })
      });

      afterEach(function () {
        App.configsCollection.getConfigByName.restore();
        App.config.get.restore();
      });

      it('stackProperty does not exist', function () {
        this.stub.returns(null);
        expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.true;
      });

      it('installedServices does not contain stackProperty.serviceName', function () {
        this.stub.returns({serviceName: 's1'});
        installerStep7Controller.set('installedServices', {});
        expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.true;
      });

      it('stackProperty.propertyDependsOn is empty', function () {
        installerStep7Controller.reopen({installedServices: {s1: true}});
        this.stub.returns({serviceName: 's1', propertyDependsOn: []});

        expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.false;
      });

      it('stackProperty.propertyDependsOn is not empty', function () {
        installerStep7Controller.reopen({installedServices: {s1: true}});
        this.stub.returns({serviceName: 's1', propertyDependsOn: [
          {type: 't1'},
          {type: 't2'}
        ]});

        expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.true;
      });

      it('stackProperty.propertyDependsOn is not empty (2)', function () {
        installerStep7Controller.reopen({installedServices: {s1: true}});
        this.stub.returns({serviceName: 's1', propertyDependsOn: [
          {type: 't1'},
          {type: 't1'}
        ]});

        expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.false;
      });

      it('stackProperty was not customized', function () {
        installerStep7Controller.reopen({installedServices: {s1: true}});
        this.stub.returns({
          serviceName: 's1',
          propertyDependsOn: [],
          recommendedValue: '1'
        });

        expect(installerStep7Controller.allowUpdateProperty([], '', '', null, '1')).to.be.true;
      });

    });

    it('true if it is not installer or addService', function () {
      installerStep7Controller.set('wizardController', {name: 'some'});
      expect(installerStep7Controller.allowUpdateProperty([], '', '')).to.be.true;
    });

  });

  describe('#initTabs', function () {

    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'setSkippedTabs', Em.K);
    });

    afterEach(function () {
      installerStep7Controller.setSkippedTabs.restore();
    });

    it('should call setSkippedTabs', function () {
      installerStep7Controller.initTabs();
      expect(installerStep7Controller.setSkippedTabs.calledOnce).to.be.true;
    });

  });

  describe('#setSkippedTabs', function () {

    beforeEach(function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({
            name: 'credentials',
            displayName: 'Credentials',
            icon: 'glyphicon-lock',
            isActive: false,
            isDisabled: false,
            isSkipped: false,
            validateOnSwitch: false,
            tabView: App.CredentialsTabOnStep7View
          }),
          Em.Object.create({
            name: 'databases',
            displayName: 'Databases',
            icon: 'glyphicon-align-justify',
            isActive: false,
            isDisabled: false,
            isSkipped: false,
            validateOnSwitch: false,
            tabView: App.DatabasesTabOnStep7View
          }),
        ],
      });
      installerStep7Controller.set('content.selectedServiceNames', Em.A(["HDFS", "YARN", "MAPREDUCE2", "TEZ", "ZOOKEEPER", "AMBARI_METRICS", "SMARTSENSE"]));
    });

    afterEach(function () {
      App.Tab.find.restore();
    });

    it('set tab "credentials"\'s properties "isDisabled" and "isSkipped" to false', function () {
      sinon.stub(App.Tab, 'find').returns([
        Em.Object.create({themeName: 'credentials', serviceName: 'AMBARI_METRICS',}),
        Em.Object.create({themeName: 'credentials', serviceName: 'SMARTSENSE',})
      ]);
      installerStep7Controller.setSkippedTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'credentials').get('isDisabled')).to.be.equal(false);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'credentials').get('isSkipped')).to.be.equal(false);
    });

    it('set tab "credentials"\'s properties "isDisabled" and "isSkipped" to true', function () {
      sinon.stub(App.Tab, 'find').returns([
        Em.Object.create({themeName: 'credentials'}),
        Em.Object.create({themeName: 'credentials'})
      ]);
      installerStep7Controller.setSkippedTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'credentials').get('isDisabled')).to.be.equal(true);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'credentials').get('isSkipped')).to.be.equal(true);
    });

    it('set tab "databases"\'s properties "isDisabled" and "isSkipped" to false', function () {
      sinon.stub(App.Tab, 'find').returns([
        Em.Object.create({themeName: 'database', serviceName: 'AMBARI_METRICS'}),
        Em.Object.create({themeName: 'database', serviceName: 'SMARTSENSE'})
      ]);
      installerStep7Controller.setSkippedTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isDisabled')).to.be.equal(false);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isSkipped')).to.be.equal(false);
    });

    it('set tab "databases"\'s properties "isDisabled" and "isSkipped" to true', function () {
      sinon.stub(App.Tab, 'find').returns([
        Em.Object.create({themeName: 'database'}),
        Em.Object.create({themeName: 'database'})
      ]);
      installerStep7Controller.setSkippedTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isDisabled')).to.be.equal(true);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isSkipped')).to.be.equal(true);
    })

  });

  describe('#currentTabIndex', function () {

    it('get current Tabs Index return 0 because first tab is active', function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({name: 'credentials', isActive: true}),
          Em.Object.create({name: 'databases', isActive: false}),
        ],
      });
      expect(installerStep7Controller.get('currentTabIndex')).to.be.equal(0);
    });

    it('get current Tabs Index return 1 because second tab is active', function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({name: 'credentials', isActive: false}),
          Em.Object.create({name: 'databases', isActive: true}),
        ],
      });
      expect(installerStep7Controller.get('currentTabIndex')).to.be.equal(1);
    });

    it('get current Tab Index return -1', function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({name: 'credentials', isActive: false}),
          Em.Object.create({name: 'databases', isActive: false}),
        ],
      });
      expect(installerStep7Controller.get('currentTabIndex')).to.be.equal(-1);
    })

  });

  describe('#currentTabName', function () {

    it('should return current tab name', function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({name: 'credentials', isActive: true,}),
          Em.Object.create({name: 'databases', isActive: false}),
        ],
      });
      expect(installerStep7Controller.get('currentTabName')).to.be.equal('credentials');
    });

    it('should return current tab name', function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({name: 'credentials', isActive: false,}),
          Em.Object.create({name: 'databases', isActive: true}),
        ],
      });
      expect(installerStep7Controller.get('currentTabName')).to.be.equal('databases');
    });

  });

  describe('#selectTab', function () {

    it('tab "isActive" property stay "false"', function () {
      var event = {context: Ember.Object.create({isDisabled: true, isActive: false})};
      installerStep7Controller.selectTab(event);
      expect(event.context.get('isActive')).to.be.equal(false);
    });

    it('tab "isActive" property changed to "true"', function () {
      var event = {
        context: Ember.Object.create({isDisabled: false, isActive: false})
      };
      installerStep7Controller.selectTab(event);
      expect(event.context.get('isActive')).to.be.equal(true);
    });

  });

  describe('#isNextDisabled', function () {

    it('if tabName is "credentials" return value based on credentialsTabNextEnabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'credentials', isActive: true})], credentialsTabNextEnabled: false});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(true);
    });

    it('if tabName is "credentials" return value based on credentialsTabNextEnabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'credentials', isActive: true})], credentialsTabNextEnabled: true});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(false);
    });

    it('if tabName is "databases" return value based on databasesTabNextEnabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'databases', isActive: true})], databasesTabNextEnabled: false});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(true);
    });

    it('if tabName is "databases" return value based on databasesTabNextEnabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'databases', isActive: true})], databasesTabNextEnabled: true});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(false);
    });

    it('if tabName is "all-configurations" return value based on isSubmitDisabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'all-configurations', isActive: true})], isSubmitDisabled: true});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(true);
    });

    it('if tabName is "all-configurations" return value based on isSubmitDisabled', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'all-configurations', isActive: true})], isSubmitDisabled: false});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(false);
    });

    it('if tabName is not "credentials", "databases", "all-configurations" return false', function () {
      installerStep7Controller.reopen({tabs: [Em.Object.create({name: 'directories', isActive: true})]});
      expect(installerStep7Controller.get('isNextDisabled')).to.be.equal(false);
    });

  });

  describe('#disableTabs', function () {

    beforeEach(function () {
      installerStep7Controller.reopen({
        tabs: [
          Em.Object.create({
            name: 'credentials',
            isActive: false,
            isDisabled: false,
            isSkipped: true,
          }),
          Em.Object.create({
            name: 'databases',
            isActive: true,
            isDisabled: false,
            isSkipped: false,
          }),
          Em.Object.create({
            name: 'all-configurations',
            isActive: true,
            isDisabled: false,
            isSkipped: false,
          }),
        ],
      });
    });

    it('set "isDisabled" properties to false in all tabs except with name "credentials"', function () {
      installerStep7Controller.set('credentialsTabNextEnabled', false);
      installerStep7Controller.disableTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isDisabled')).to.be.equal(true);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'all-configurations').get('isDisabled')).to.be.equal(true);
    });

    it('don\'t set "isDisabled" properties to false in all tabs because of credentialsTabNextEnabled', function () {
      installerStep7Controller.set('credentialsTabNextEnabled', true);
      installerStep7Controller.disableTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isDisabled')).to.be.equal(false);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'all-configurations').get('isDisabled')).to.be.equal(false);
    });

    it('don\'t set "isDisabled" properties to false in all tabs because of credentials "isDisabled" is true', function () {
      installerStep7Controller.get('tabs').findProperty('name', 'credentials').set('isDisabled', true);
      installerStep7Controller.set('credentialsTabNextEnabled', true);
      installerStep7Controller.disableTabs();
      expect(installerStep7Controller.get('tabs').findProperty('name', 'databases').get('isDisabled')).to.be.equal(false);
      expect(installerStep7Controller.get('tabs').findProperty('name', 'all-configurations').get('isDisabled')).to.be.equal(false);
    });

  });

  describe('#showConfigProperty', function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'next', Em.K);
    });

    afterEach(function() {
      Em.run.next.restore();
    });

    it('set props "isActive" to true in entered showConfigProperty event and "false" in all another stepConfigs', function() {
      var event = {context: Ember.Object.create({
          serviceName: 'AMBARI_METRICS',
          propertyName: "all-configurations",
          name: 'all-configurations',
          isActive: false,
      })};
      installerStep7Controller.set('stepConfigs', Em.A([
        Em.Object.create({serviceName: 'HDFS', isActive: true, configGroups: [{},{}]}),
        Em.Object.create({serviceName: 'AMBARI_METRICS', isActive: false, configGroups: [{},{}]}),
        Em.Object.create({serviceName: 'MAPREDUCE2', isActive: true, configGroups: [{},{}]}),
      ]));
      installerStep7Controller.set('filterColumns', [
        Em.Object.create({attributeName:"isOverridden", attributeValue:true, name:"Overridden properties",selected:true}),
        Em.Object.create({attributeName:"isFinal", attributeValue:true, name:"Final properties", selected:false}),
        Em.Object.create({attributeName:"hasIssues", attributeValue:true, name:"Show property issues", selected:false}),
      ]);
      installerStep7Controller.showConfigProperty(event);
      expect(installerStep7Controller.get('filterColumns').everyProperty('selected', false)).to.be.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('isActive')).to.be.equal(false);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'AMBARI_METRICS').get('isActive')).to.be.equal(true);
    });

  });

  describe('#setIssues', function () {

    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'ringBell', Em.K);
    });

    afterEach(function() {
      installerStep7Controller.ringBell.restore();
    });

    it('should not ring bell if validations counter doesn\'t  change', function() {
      installerStep7Controller.set('validationsCounter', 3);
      installerStep7Controller.set('stepConfigs', [
        {configsWithErrors: {length: 1}},
        {configsWithErrors: {length: 2}}
      ]);
      installerStep7Controller.setIssues();
      expect(installerStep7Controller.ringBell.called).to.be.equal(false);
    });

    it('should ring bell if validations counter changes', function() {
      installerStep7Controller.set('validationsCounter', 4);
      installerStep7Controller.set('stepConfigs', [
        {configsWithErrors: {length: 2}},
        {configsWithErrors: {length: 3}}
      ]);
      installerStep7Controller.setIssues();
      expect(installerStep7Controller.ringBell.called).to.be.equal(true);
    });

  });

  App.TestAliases.testAsComputedEqual(installerStep7Controller, 'isInstallWizard', 'content.controllerName', 'installerController');

  App.TestAliases.testAsComputedEqual(installerStep7Controller, 'isAddServiceWizard', 'content.controllerName', 'addServiceController');

});
