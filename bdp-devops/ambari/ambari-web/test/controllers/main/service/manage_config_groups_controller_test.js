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
var c;

describe('App.ManageConfigGroupsController', function() {

  beforeEach(function() {
    c = App.ManageConfigGroupsController.create({});
  });

  var manageConfigGroupsController = App.ManageConfigGroupsController.create({});

  describe('#addConfigGroup', function() {
    beforeEach(function() {
      manageConfigGroupsController.addConfigGroup();
    });

    describe("#validate", function() {
      it("should display no warning if user inputs valid characters into group name", function() {

        manageConfigGroupsController.addGroupPopup.set('configGroupName', 'hello');

        expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function() {
        manageConfigGroupsController.addGroupPopup.set('configGroupName', '/{"!@#$%');

        expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });
    });
  });

  describe('#renameConfigGroup', function() {
    beforeEach(function() {
      var configGroup = Ember.Object.create ({
        name: 'name',
        description: 'description'
      });

      manageConfigGroupsController.set('selectedConfigGroup', configGroup);
      manageConfigGroupsController.renameConfigGroup();
    });

    describe("#validate", function() {
      it("should display no warning if user inputs valid characters into group name", function() {
        manageConfigGroupsController.renameGroupPopup.set('configGroupName', 'hello');

        expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function() {
        manageConfigGroupsController.renameGroupPopup.set('configGroupName', '/{"!@#$%');

        expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });
    });
  });

  describe('#addHostsCallback', function() {

    beforeEach(function() {

      c.reopen({
        selectedConfigGroup: Em.Object.create({
          hosts: ['h1'],
          parentConfigGroup: Em.Object.create({
            hosts: ['h2', 'h3']
          })
        })
      });
    });

    it('should set hosts to selectedConfigGroup and remove them form default group', function () {

      c.addHostsCallback(['h2', 'h3']);

      expect(c.get('selectedConfigGroup.hosts')).to.include.members(['h1','h2','h3']);
      expect(c.get('selectedConfigGroup.parentConfigGroup.hosts').toArray()).to.be.empty;
    });

  });

  describe('#isHostsModified', function () {

    Em.A([
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [],
          toCreate: []
        },
        e: false
      },
      {
        o: {
          toClearHosts: [{}],
          toDelete: [],
          toSetHosts: [],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [{}],
          toSetHosts: [],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [{}],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [],
          toCreate: [{}]
        },
        e: true
      }
    ]).forEach(function (test, index) {
      it('test #' + index, function () {
        c.reopen({
          isLoaded: true,
          hostsModifiedConfigGroups: test.o
        });
        expect(c.get('isHostsModified')).to.equal(test.e);
      });
    });

  });

  describe('#deleteConfigGroup', function () {

    beforeEach(function() {

      var defaultGroup = Em.Object.create({
        hosts: ['h2', 'h3'],
        isDefault: true
      });

      var selectedGroup = Em.Object.create({
        hosts: ['h1'],
        parentConfigGroup: defaultGroup
      });

      c.reopen({
        configGroups: [defaultGroup, selectedGroup],
        selectedConfigGroup: selectedGroup
      });

      sinon.stub(App.configGroupsMapper, 'deleteRecord', Em.K);
    });

    afterEach(function(){
      App.configGroupsMapper.deleteRecord.restore();
    });

    it('after deleting some config group, Default should be selected', function () {

      c.deleteConfigGroup();

      expect(c.get('configGroups.length')).to.equal(1);
      expect(c.get('selectedConfigGroup.hosts')).to.include.members(['h1','h2','h3']);
      expect(c.get('selectedConfigGroup.isDefault')).to.be.true;
    });

  });

  describe("#manageConfigurationGroups", function () {
    var service = Em.Object.create({});
    manageConfigGroupsController.set('hostsModifiedConfigGroups', {});
    describe("#controller passed", function () {
      var popup = manageConfigGroupsController.manageConfigurationGroups(Em.Object.create({
        content: Em.Object.create()
      }), service);

      describe("#onPrimary()", function () {
        beforeEach(function () {
          sinon.stub(popup, 'onPrimaryWizard', Em.K);
        });
        afterEach(function () {
          popup.onPrimaryWizard.restore();
        });
        it("onPrimaryWizard is called", function () {
          popup.onPrimary();
          expect(popup.onPrimaryWizard.calledOnce).to.be.true;
        });
      });

      describe("#onPrimaryWizard()", function () {

        var ctrl = Em.Object.create({
          selectedService: Em.Object.create({
            selected: false
          }),
          selectedServiceObserver: Em.K,
          setGroupsToDelete: Em.K
        });

        beforeEach(function () {
          sinon.spy(ctrl, 'selectedServiceObserver');
          sinon.spy(ctrl, 'setGroupsToDelete');
          sinon.stub(manageConfigGroupsController, 'persistConfigGroups', Em.K);
          sinon.stub(popup, 'updateConfigGroupOnServicePage', Em.K);
          sinon.stub(popup, 'hide', Em.K);
        });

        afterEach(function () {
          ctrl.setGroupsToDelete.restore();
          ctrl.selectedServiceObserver.restore();
          manageConfigGroupsController.persistConfigGroups.restore();
          popup.updateConfigGroupOnServicePage.restore();
          popup.hide.restore();
        });

        describe("groups deleted on 7th step", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            popup.onPrimaryWizard(ctrl, {toDelete: [1]});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is called with [1]', function () {
            expect(ctrl.setGroupsToDelete.calledWith([1])).to.be.true;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard not on 7th step", function () {

          beforeEach(function () {
            ctrl.set('name', '');
            popup.onPrimaryWizard(ctrl, {});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });

          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });

          it('persistConfigGroups is not called', function () {
            expect(manageConfigGroupsController.persistConfigGroups.called).to.be.false;
          });

          it('updateConfigGroupOnServicePage is not called', function () {
            expect(popup.updateConfigGroupOnServicePage.called).to.be.false;
          });

          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard on 7th step, service selected", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            ctrl.set('selectedService.selected', true);
            popup.onPrimaryWizard(ctrl, {toDelete: [1]});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard on 7th step, no groups to delete", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            ctrl.set('selectedService.selected', false);
            popup.onPrimaryWizard(ctrl, {toDelete: []});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });

        });

      });
    });

    describe("#controller not passed", function () {
      var popup = manageConfigGroupsController.manageConfigurationGroups(null, service);

      describe("#onPrimary()", function () {
        beforeEach(function () {
          sinon.stub(popup, 'runClearCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'runModifyCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'runCreateCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'updateConfigGroupOnServicePage', Em.K);
          sinon.stub(popup, 'hide', Em.K);
          manageConfigGroupsController.set('hostsModifiedConfigGroups', {toCreate: []});
          popup.onPrimary();
        });
        afterEach(function () {
          popup.runCreateCGQueue.restore();
          popup.runModifyCGQueue.restore();
          popup.runClearCGQueue.restore();
          popup.updateConfigGroupOnServicePage.restore();
          popup.hide.restore();
        });
        it("runClearCGQueue is called", function () {
          expect(popup.runClearCGQueue.calledOnce).to.be.true;
        });
        it("runModifyCGQueue is called", function () {
          expect(popup.runModifyCGQueue.calledOnce).to.be.true;
        });
        it("runCreateCGQueue is called", function () {
          expect(popup.runCreateCGQueue.calledOnce).to.be.true;
        });
        it("updateConfigGroupOnServicePage is called", function () {
          expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
        });
        it("hide is called", function () {
          expect(popup.hide.calledOnce).to.be.true;
        });
      });
      describe("#runClearCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'updateConfigurationGroup', Em.K);
          sinon.stub(manageConfigGroupsController, 'deleteConfigurationGroup', Em.K);
          popup.runClearCGQueue(Em.K, {
            initialGroups: [],
            toClearHosts: [Em.Object.create()],
            toDelete: [1]
          });
        });
        afterEach(function () {
          manageConfigGroupsController.updateConfigurationGroup.restore();
          manageConfigGroupsController.deleteConfigurationGroup.restore();
        });
        it("updateConfigurationGroup is called once", function () {
          expect(manageConfigGroupsController.updateConfigurationGroup.calledOnce).to.be.true;
        });
        it("deleteConfigurationGroup is called once", function () {
          expect(manageConfigGroupsController.deleteConfigurationGroup.calledOnce).to.be.true;
        });
      });
      describe("#runModifyCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'updateConfigurationGroup', Em.K);
        });
        afterEach(function () {
          manageConfigGroupsController.updateConfigurationGroup.restore();
        });
        it("updateConfigurationGroup is called once", function () {
          popup.runModifyCGQueue(Em.K, {toSetHosts: [1]});
          expect(manageConfigGroupsController.updateConfigurationGroup.calledOnce).to.be.true;
        });
      });
      describe("#runCreateCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'postNewConfigurationGroup', Em.K);
        });
        afterEach(function () {
          manageConfigGroupsController.postNewConfigurationGroup.restore();
        });
        it("postNewConfigurationGroup is called once", function () {
          popup.runCreateCGQueue(Em.K, {toCreate: [1]});
          expect(manageConfigGroupsController.postNewConfigurationGroup.calledOnce).to.be.true;
        });
      });
    });
  });

  describe('#_onLoadPropertiesSuccess', function () {

    var data = {
      items: [
        {
          type: 'type1',
          tag: 'tag1',
          properties: {
            prop1: 'val1',
            prop2: 'val2'
          }
        },
        {
          type: 'type1',
          tag: 'tag2',
          properties: {
            prop3: 'val3'
          }
        },
        {
          type: 'type2',
          tag: 'tag1',
          properties: {
            prop4: 'val4'
          }
        }
      ]
    };
    var params = {
      typeTagToGroupMap: {
        'type1///tag1': 'group1',
        'type1///tag2': 'group2',
        'type2///tag1': 'group3'
      }
    };
    var configGroups = [
      Em.Object.create({
        name: 'group1',
        properties: []
      }),
      Em.Object.create({
        name: 'group2',
        properties: []
      }),
      Em.Object.create({
        name: 'group3',
        properties: []
      }),
      Em.Object.create({
        name: 'group4',
        properties: []
      })
    ];

    beforeEach(function () {
      sinon.stub(c, 'resortConfigGroup', Em.K);
    });

    afterEach(function () {
      c.resortConfigGroup.restore();
    });

    it('should set properties to config groups', function () {
      c.set('configGroups', configGroups);
      c._onLoadPropertiesSuccess(data, null, params);
      expect(JSON.stringify(c.get('configGroups'))).to.equal(JSON.stringify([
        Em.Object.create({
          properties: [
            {
              name: 'prop1',
              value: 'val1',
              type: 'type1'
            },
            {
              name: 'prop2',
              value: 'val2',
              type: 'type1'
            }
          ],
          name: 'group1'
        }),
        Em.Object.create({
          properties: [
            {
              name: 'prop3',
              value: 'val3',
              type: 'type1'
            }
          ],
          name: 'group2'
        }),
        Em.Object.create({
          properties: [
            {
              name: 'prop4',
              value: 'val4',
              type: 'type2'
            }
          ],
          name: 'group3'
        }),
        Em.Object.create({
          properties: [],
          name: 'group4'
        })
      ]));
    });

  });

  describe('#componentsForFilter', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 'HDFS'
          }),
          Em.Object.create({
            serviceName: 'noHDFS'
          }),
          Em.Object.create({
            serviceName: 'HDFS'
          })
        ];
      });
      c.set('serviceName', 'HDFS');
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    it('should map components for current service', function () {
      expect(c.get('componentsForFilter')).to.have.property('length').equal(2);
    });

    it('no one is selected', function () {
      expect(c.get('componentsForFilter').mapProperty('selected')).to.be.eql([false, false]);
    });

  });

  describe('#getNewlyAddedHostComponentsMap', function() {
    beforeEach(function() {
      this.mockGet = sinon.stub(App.router, 'get');
      this.mockGet.withArgs('addServiceController.content.clients').returns([
        {
          isInstalled: true,
          component_name: 'Client1',
          display_name: 'client1'
        },
        {
          isInstalled: false,
          component_name: 'Client2',
          display_name: 'client2'
        }
      ]);
      this.mockGet.withArgs('addServiceController.content.masterComponentHosts').returns([
        {
          isInstalled: true,
          hostName: 'host1',
          component: 'Master1',
          display_name: 'master1'
        },
        {
          isInstalled: false,
          hostName: 'host2',
          component: 'Master2',
          display_name: 'master2'
        }
      ]);
      this.mockGet.withArgs('addServiceController.content.slaveComponentHosts').returns([
        {
          componentName: 'Slave1',
          displayName: 'slave1',
          hosts: [
            {
              hostName: 'host1',
              isInstalled: false
            }
          ]
        },
        {
          componentName: 'Slave2',
          displayName: 'slave2',
          hosts: [
            {
              hostName: 'host2',
              isInstalled: true
            }
          ]
        },
        {
          componentName: 'CLIENT',
          displayName: 'client',
          hosts: [
            {
              hostName: 'host1',
              isInstalled: false
            }
          ]
        }
      ]);
    });
    afterEach(function() {
      this.mockGet.restore();
    });

    it('should return host-components map', function() {
      expect(JSON.stringify(c.getNewlyAddedHostComponentsMap())).to.be.equal(JSON.stringify({
        "host2": [
          {
            "componentName": "Master2",
            "displayName": "master2"
          }
        ],
        "host1": [
          {
            "componentName": "Slave1",
            "displayName": "slave1"
          },
          {
            "componentName": "Client2",
            "displayName": "client2"
          }
        ]
      }));
    });
  });

});
