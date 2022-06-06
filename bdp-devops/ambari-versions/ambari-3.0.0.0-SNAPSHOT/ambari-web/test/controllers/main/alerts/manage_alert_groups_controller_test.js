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

var manageAlertGroupsController;

function getController() {
  return App.ManageAlertGroupsController.create({
    selectedAlertGroup: Em.Object.create({
      name: ''
    })
  });
}

describe('App.ManageAlertGroupsController', function () {

  beforeEach(function () {
    manageAlertGroupsController = getController();
  });

  App.TestAliases.testAsComputedFilterBy(getController(), 'alertGlobalNotifications', 'alertNotifications', 'global', true);

  describe('#duplicateAlertGroup', function () {
    beforeEach(function () {
      var group = Ember.Object.create({
        name: 'test'
      });

      manageAlertGroupsController.set('selectedAlertGroup', group);
      manageAlertGroupsController.duplicateAlertGroup();
    });

    describe("#validate", function () {
      it("should display no warning if user duplicate an existed group", function () {
        manageAlertGroupsController.addGroupPopup.set('alertGroupName', 'test Copy');

        expect(manageAlertGroupsController.addGroupPopup.warningMessage).to.be.empty;
      });
    });
  });

  describe('#deleteDefinitions', function () {
    var definitions = [
      Em.Object.create({
        name: 'def1',
        serviceName: 'HDFS',
        label: "Alert Definition 1",
        id: 1
      }),
      Em.Object.create({
        name: 'def2',
        serviceName: 'HDFS',
        label: "Alert Definition 2",
        id: 2
      }),
      Em.Object.create({
        name: 'def3',
        serviceName: 'HDFS',
        label: "Alert Definition 3",
        id: 3
      })
    ];

    beforeEach(function () {
      manageAlertGroupsController = App.ManageAlertGroupsController.create({});
    });

    var createAlertGroupMock = function (groupDefs) {
      return Em.Object.create({
        definitions: groupDefs,
        name: 'group'
      });
    };

    var tests = [
      {
        selectedDefinitions: definitions.slice(0, 1),
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: definitions.slice(1)
      },
      {
        selectedDefinitions: definitions.slice(0, 2),
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: definitions.slice(2)
      },
      {
        selectedDefinitions: definitions,
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: []
      }
    ];

    tests.forEach(function (test) {
      it('delete definitions length {0} definitions'.format(test.selectedDefinitions.slice(0).length), function () {
        manageAlertGroupsController.reopen({
          selectedDefinitions: test.selectedDefinitions,
          selectedAlertGroup: test.selectedAlertGroup
        });
        manageAlertGroupsController.deleteDefinitions();
        expect(manageAlertGroupsController.get('selectedAlertGroup.definitions').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#addDefinitionsCallback', function () {

    var definitions = [
      Em.Object.create({
        name: 'def1',
        serviceName: 'HDFS',
        label: "Alert Definition 1",
        id: 1
      }),
      Em.Object.create({
        name: 'def2',
        serviceName: 'HDFS',
        label: "Alert Definition 2",
        id: 2
      }),
      Em.Object.create({
        name: 'def3',
        serviceName: 'HDFS',
        label: "Alert Definition 3",
        id: 3
      })
    ];

    var definitionsToAdd = [
      Em.Object.create({
        name: 'def4',
        serviceName: 'HDFS',
        label: "Alert Definition 4",
        id: 4
      }),
      Em.Object.create({
        name: 'def5',
        serviceName: 'HDFS',
        label: "Alert Definition 5",
        id: 5
      }),
      Em.Object.create({
        name: 'def6',
        serviceName: 'HDFS',
        label: "Alert Definition 6",
        id: 6
      })
    ];

    beforeEach(function () {
      manageAlertGroupsController = App.ManageAlertGroupsController.create({});
    });

    var createAlertGroupMock = function (groupDefs) {
      return Em.Object.create({
        definitions: groupDefs,
        name: 'group'
      });
    };

    var result = function (originalDefs, addedDefs) {
      return originalDefs.concat(addedDefs);
    };

    var tests = [
      {
        selectedDefinitions: definitionsToAdd.slice(0, 1),
        selectedAlertGroup: createAlertGroupMock(definitions.slice(0, 1)),
        e: result(definitions.slice(0, 1), definitionsToAdd.slice(0, 1))
      },
      {
        selectedDefinitions: definitionsToAdd.slice(0, 2),
        selectedAlertGroup: createAlertGroupMock(definitions.slice(0, 2)),
        e: result(definitions.slice(0, 2), definitionsToAdd.slice(0, 2))
      },
      {
        selectedDefinitions: definitionsToAdd,
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: result(definitions, definitionsToAdd)
      }
    ];

    tests.forEach(function (test) {
      it('add Definitions length {0} definitions'.format(test.selectedDefinitions.slice(0).length), function () {
        manageAlertGroupsController.set('selectedAlertGroup', test.selectedAlertGroup);
        manageAlertGroupsController.addDefinitionsCallback(test.selectedDefinitions);
        expect(manageAlertGroupsController.get('selectedAlertGroup.definitions').toArray()).to.eql(test.e);
      });
    });

  });

  App.TestAliases.testAsComputedAnd(getController(), 'isDefsModified', ['isLoaded', 'isDefsModifiedAlertGroups']);

  App.TestAliases.testAsComputedOr(getController(), 'isDefsModifiedAlertGroups', ['defsModifiedAlertGroups.toSet.length', 'defsModifiedAlertGroups.toCreate.length', 'defsModifiedAlertGroups.toDelete.length'])

  describe('#addAlertGroup', function () {

    function getAppGroupPopup() {
      var c = getController();
      c.addAlertGroup();
      return c.get('addGroupPopup');
    }

    App.TestAliases.testAsComputedOr(getAppGroupPopup(), 'disablePrimary', ['alertGroupNameIsEmpty', 'warningMessage']);

  });

  describe('#renameAlertGroup', function () {

    function getRenamePopup() {
      var c = getController();
      c.renameAlertGroup();
      return c.get('renameGroupPopup');
    }

    App.TestAliases.testAsComputedOr(getRenamePopup(), 'disablePrimary', ['alertGroupNameIsEmpty', 'warningMessage']);

  });

  describe('#alertNotifications', function () {
    var alertNotifications;
    beforeEach(function () {
      sinon.stub(App.AlertNotification, 'find').returns([
        Em.Object.create({id: 1, name: 'n1', description: 'n1d', type: 'EMAIL', global: true}),
        Em.Object.create({id: 2, name: 'n2', description: 'n2d', type: 'SNMP', global: false})
      ]);
      manageAlertGroupsController.set('isLoaded', true);
      alertNotifications = manageAlertGroupsController.get('alertNotifications');
    });

    afterEach(function () {
      App.AlertNotification.find.restore();
    });

    it('should be mapped from App.AlertNotification (1)', function () {
      expect(alertNotifications).to.have.property('length').to.be.equal(2);
    });

    it('should be mapped from App.AlertNotification (2)', function () {
      expect(JSON.parse(JSON.stringify(alertNotifications[0]))).to.be.eql({id: 1, name: 'n1', description: 'n1d', type: 'EMAIL', global: true});
    });

    it('should be mapped from App.AlertNotification (3)', function () {
      expect(JSON.parse(JSON.stringify(alertNotifications[1]))).to.be.eql({id: 2, name: 'n2', description: 'n2d', type: 'SNMP', global: false});
    });

  });

});

