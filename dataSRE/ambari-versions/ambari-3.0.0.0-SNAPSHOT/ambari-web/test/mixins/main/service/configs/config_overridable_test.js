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

require('mixins/main/service/configs/config_overridable');
var testHelpers = require('test/helpers');

var configOverridable;

var configGroups = [
  Em.Object.create({name: 'configGroup1'}),
  Em.Object.create({name: 'configGroup2'}),
  Em.Object.create({name: 'configGroup3'})
];

describe('App.ConfigOverridable', function () {

  beforeEach(function () {
    configOverridable = Em.Object.create(App.ConfigOverridable, {
      controller: Em.Object.create({
        loadConfigGroups: Em.K,
        doSelectConfigGroup: Em.K
      }),
      isView: true
    });
  });

  describe('#launchConfigGroupSelectionCreationDialog()', function () {

    var testCases = [
      {
        text: 'fail validation because of disallowed symbols',
        name: '123=',
        isWarn: true,
        errorMessage: Em.I18n.t("form.validator.configGroupName"),
        option: false
      },
      {
        text: 'fail validation because of the same name',
        name: 'configGroup1',
        isWarn: true,
        errorMessage: Em.I18n.t("config.group.selection.dialog.err.name.exists"),
        option: false
      },
      {
        text: 'pass validation',
        name: '123',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: false
      },
      {
        text: 'pass validation as another option is selected',
        name: '123',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      },
      {
        text: 'pass validation because there is no value entered',
        name: '',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      },
      {
        text: 'pass validation because there is no value entered',
        name: '      ',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      }
    ];

    testCases.forEach(function (item) {
      it('should ' + item.text, function () {
        var popup = configOverridable.launchConfigGroupSelectionCreationDialog('service', configGroups, Em.Object.create());
        popup.set('newConfigGroupName', item.name);
        popup.set('optionSelectConfigGroup', item.option);
        expect(popup.get('isWarning')).to.equal(item.isWarn);
        expect(popup.get('warningMessage')).to.equal(item.errorMessage);
      });
    });
  });

  describe("#createOverrideProperty()", function () {

    beforeEach(function() {
      sinon.stub(configOverridable, 'launchConfigGroupSelectionCreationDialog');
      sinon.stub(App.config, 'createOverride');
    });

    afterEach(function() {
      configOverridable.launchConfigGroupSelectionCreationDialog.restore();
      App.config.createOverride.restore();
    });

    it("App.ModalPopup.show should be called", function() {
      var event = {
        contexts: [
          Em.Object.create({
            isUserProperty: true,
            isNotSaved: true
          })
        ]
      };
      configOverridable.set('controller.name', '');
      configOverridable.createOverrideProperty(event);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it("launchConfigGroupSelectionCreationDialogshould be called", function() {
      var event = {
        contexts: [
          Em.Object.create()
        ]
      };
      configOverridable.set('controller.selectedConfigGroup', Em.Object.create({
        isDefault: true
      }));
      configOverridable.createOverrideProperty(event);
      expect(configOverridable.launchConfigGroupSelectionCreationDialog.calledOnce).to.be.true;
    });

    it("App.config.createOverride be called", function() {
      var event = {
        contexts: [
          Em.Object.create({
            widget: true,
            value: 'val'
          })
        ]
      };
      configOverridable.set('controller.selectedConfigGroup', Em.Object.create({
        isDefault: false
      }));
      configOverridable.createOverrideProperty(event);
      expect(App.config.createOverride.calledOnce).to.be.true;
    });
  });

  describe("#postNewConfigurationGroup()", function () {

    it("App.ajax.send should be called", function() {
      var newConfigGroupData = {
        properties: [],
        name: 'cg1',
        service_id: 'S1',
        description: '',
        desired_configs: [],
        hosts: ['host1']
      };
      configOverridable.postNewConfigurationGroup(newConfigGroupData);

      var args = testHelpers.findAjaxRequest('name', 'config_groups.create');
      expect(args[0]).to.be.eql({
        name: 'config_groups.create',
        sender: configOverridable,
        data: {
          data: [{
            "ConfigGroup": {
              "group_name": 'cg1',
              "tag": 'S1',
              "service_name": "S1",
              "description": '',
              "desired_configs": [],
              "hosts": [{host_name: 'host1'}]
            }
          }],
          modelData: newConfigGroupData
        },
        success: 'postNewConfigurationGroupSuccess',
        error: 'postNewConfigurationGroupError'
      });
    });
  });

  describe("#postNewConfigurationGroupSuccess()", function () {

    beforeEach(function() {
      sinon.stub(App.store, 'safeLoad');
      sinon.stub(App.store, 'fastCommit');
      sinon.stub(App.ServiceConfigGroup, 'deleteTemporaryRecords');
      configOverridable.postNewConfigurationGroupSuccess({
        resources: [
          {
            ConfigGroup: {
              id: 'cg1'
            }
          }
        ]
      }, {}, {modelData: {}});
    });

    afterEach(function() {
      App.ServiceConfigGroup.deleteTemporaryRecords.restore();
      App.store.fastCommit.restore();
      App.store.safeLoad.restore();
    });

    it("App.store.load should be called", function() {
      expect(App.store.safeLoad.calledWith(App.ServiceConfigGroup, {id: 'cg1'})).to.be.true;
    });

    it("App.store.commit should be called", function() {
      expect(App.store.fastCommit.calledOnce).to.be.true;
    });

    it("App.ServiceConfigGroup.deleteTemporaryRecords should be called", function() {
      expect(App.ServiceConfigGroup.deleteTemporaryRecords.calledOnce).to.be.true;
    });
  });

  describe("#updateConfigurationGroup()", function () {

    beforeEach(function() {
      sinon.stub(configOverridable, 'getConfigGroupData');
    });

    afterEach(function() {
      configOverridable.getConfigGroupData.restore();
    });

    it("App.ajax.send should be called", function() {
      configOverridable.updateConfigurationGroup(Em.Object.create());
      expect(testHelpers.findAjaxRequest('name', 'config_groups.update')).to.be.exist;
    });
  });

  describe("#getConfigGroupData()", function () {

    it("should return config group data", function() {
      var configGroup = Em.Object.create({
        name: 'cg1',
        description: 'dsc',
        service: {
          id: 'S1'
        },
        hosts: ['host1'],
        desiredConfigs: [{
          site: 'type1',
          tag: 'tag1'
        }]
      });
      expect(configOverridable.getConfigGroupData(configGroup)).to.be.eql({
        ConfigGroup: {
          group_name: 'cg1',
          description: 'dsc',
          tag: 'S1',
          service_name: "S1",
          hosts: [{
            host_name: 'host1'
          }],
          desired_configs: [{
            type: 'type1',
            tag: 'tag1'
          }]
        }
      });
    });
  });

  describe("#launchSwitchConfigGroupOfHostDialog()", function () {
    var mock = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(configOverridable, 'updateConfigurationGroup');
      sinon.spy(mock, 'callback');
    });

    afterEach(function() {
      configOverridable.updateConfigurationGroup.restore();
      mock.callback.restore();
    });

    it("updateConfigurationGroup should be called", function() {
      var popup = configOverridable.launchSwitchConfigGroupOfHostDialog(Em.Object.create({
        name: 'cg1',
        isDefault: false,
        hosts: ['host1']
      }),
      [],'host1', Em.K);
      popup.onPrimary();
      expect(configOverridable.updateConfigurationGroup.calledTwice).to.be.true;
    });

    it("callback should be called", function() {
      var group = Em.Object.create({
        name: 'cg1',
        isDefault: false,
        hosts: ['host1']
      });
      var popup = configOverridable.launchSwitchConfigGroupOfHostDialog(group, [], 'host1', mock.callback);
      popup.onPrimary();
      expect(mock.callback.calledOnce).to.be.true;
    });
  });

  describe("#deleteConfigurationGroup()", function () {

    it("App.ajax.send should be called", function() {
      configOverridable.deleteConfigurationGroup(Em.Object.create());
      expect(testHelpers.findAjaxRequest('name', 'common.delete.config_group')).to.be.exist;
    });
  });

  describe("#deleteConfigurationGroupSuccess()", function () {
    var group = Em.Object.create({
      id: 'cg1',
      stateManager: Em.Object.create({
        transitionTo: Em.K
      })
    });

    beforeEach(function() {
      sinon.stub(App.ServiceConfigGroup, 'find').returns([
        group
      ]);
      sinon.stub(App.configGroupsMapper, 'deleteRecord');
    });

    afterEach(function() {
      App.ServiceConfigGroup.find.restore();
      App.configGroupsMapper.deleteRecord.restore();
    });

    it("App.configGroupsMapper.deleteRecord should be called", function() {
      configOverridable.deleteConfigurationGroupSuccess({}, {}, {id: 'cg1'});
      expect(App.configGroupsMapper.deleteRecord.calledWith(group)).to.be.true;
    });

    it("App.configGroupsMapper.deleteRecord should not be called", function() {
      configOverridable.deleteConfigurationGroupSuccess({}, {}, {id: 'cg2'});
      expect(App.configGroupsMapper.deleteRecord.called).to.be.false;
    });
  });

  describe("#saveGroupConfirmationPopup()", function () {
    var group = Em.Object.create({
      serviceName: 'S1',
      name: 'cg1'
    });
    var mock = {
      manageConfigurationGroups: Em.K
    };

    beforeEach(function() {
      sinon.stub(configOverridable.get('controller'), 'loadConfigGroups').returns({
        done: function (callback) {
          callback();
        }
      });
      sinon.stub(configOverridable.get('controller'), 'doSelectConfigGroup');
      sinon.stub(App.ServiceConfigGroup, 'find').returns([group]);
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'manageConfigurationGroups');
    });

    afterEach(function() {
      configOverridable.get('controller').loadConfigGroups.restore();
      configOverridable.get('controller').doSelectConfigGroup.restore();
      App.ServiceConfigGroup.find.restore();
      App.router.get.restore();
      mock.manageConfigurationGroups.restore();
    });

    it("onPrimary", function() {
      configOverridable.set('controller.name', 'mainServiceInfoConfigsController');
      configOverridable.set('controller.content', Em.Object.create({
        serviceName: 'S1'
      }));
      var popup = configOverridable.saveGroupConfirmationPopup('cg1');
      popup.onPrimary();
      expect(configOverridable.get('controller').doSelectConfigGroup.calledWith({context: group})).to.be.true;
    });

    it("onSecondary", function() {
      configOverridable.set('controller.content', Em.Object.create({
        serviceName: 'S1'
      }));
      var popup = configOverridable.saveGroupConfirmationPopup('cg1');
      popup.onSecondary();
      expect(mock.manageConfigurationGroups.calledWith(null, Em.Object.create({
        serviceName: 'S1'
      }))).to.be.true;
    });
  });

  describe("#persistConfigGroups()", function () {
    var mockInstaller = Em.Object.create({
      saveServiceConfigGroups: Em.K
    });
    var mockStep7 = Em.Object.create({
      content: Em.Object.create({
        controllerName: 'addServiceController'
      })
    });

    beforeEach(function() {
      sinon.stub(mockInstaller, 'saveServiceConfigGroups');
      this.mock = sinon.stub(App.router, 'get');
      sinon.stub(App.clusterStatus, 'setClusterStatus');
      this.mock.withArgs('installerController').returns(mockInstaller);
      this.mock.withArgs('wizardStep7Controller').returns(mockStep7);
      configOverridable.persistConfigGroups();
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
      App.router.get.restore();
      this.mock.restore();
      mockInstaller.saveServiceConfigGroups.restore();
    });

    it("saveServiceConfigGroups should be called", function() {
      expect(mockInstaller.saveServiceConfigGroups.calledWith(mockStep7, true)).to.be.true;
    });

    it("App.clusterStatus.setClusterStatus should be called", function() {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

});
