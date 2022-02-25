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
var stringUtils = require('utils/string_utils');
var blueprintUtils = require('utils/blueprint');
var testHelpers = require('test/helpers');
require('models/stack_service_component');

describe('App.AssignMasterOnStep7Controller', function () {
  var view;

  beforeEach(function () {
    view = App.AssignMasterOnStep7Controller.create();
  });

  describe("#content", function () {

    it("content is correct", function () {
      view.set('configWidgetContext.controller', Em.Object.create({
        content: {'name': 'name'}
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.eql({'name': 'name'});
    });

    it("content is null", function () {
      view.set('configWidgetContext.controller', Em.Object.create({
        content: null
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.empty;
    });
  });

  describe("#execute()", function () {
    var context = Em.Object.create({
      controller: {
        content: {}
      }
    });

    beforeEach(function() {
      sinon.stub(view, 'showPopup');
      sinon.stub(view, 'removeMasterComponent');
      sinon.stub(view, 'getPendingBatchRequests');
     });

    afterEach(function() {
      view.getPendingBatchRequests.restore();
      view.showPopup.restore();
      view.removeMasterComponent.restore();
    });

    it("should set configWidgetContext", function() {
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.get('configWidgetContext')).to.be.eql(context);
    });

    it("should set content", function() {
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.get('content')).to.be.eql({});
    });

    it("should set configActionComponent", function() {
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.get('configActionComponent')).to.be.eql({componentName: 'C1'});
    });

    it("should call showPopup when action is ADD", function() {
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.showPopup.calledWith({componentName: 'C1'})).to.be.true;
    });

    it("should call getPendingBatchRequests when action is ADD and HIVE_SERVER_INTERACTIVE", function() {
      view.execute(context, 'ADD', {componentName: 'HIVE_SERVER_INTERACTIVE'});
      expect(view.getPendingBatchRequests.calledWith({componentName: 'HIVE_SERVER_INTERACTIVE'})).to.be.true;
    });

    it("should call removeMasterComponent when action is DELETE", function() {
      view.execute(context, 'DELETE', {componentName: 'C1'});
      expect(view.removeMasterComponent.calledOnce).to.be.true;
      expect(view.get('mastersToCreate')).to.be.eql(['C1']);
    });
  });

  describe("#showAssignComponentPopup()", function () {

    beforeEach(function() {
      sinon.stub(view, 'loadMasterComponentHosts');
    });

    afterEach(function() {
      view.loadMasterComponentHosts.restore();
    });

    it("loadMasterComponentHosts should be called", function() {
      view.reopen({
        content: {
          controllerName: null
        }
      });
      view.showAssignComponentPopup();
      expect(view.loadMasterComponentHosts.calledOnce).to.be.true;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it("loadMasterComponentHosts should not be called", function() {
      view.reopen({
        content: {
          controllerName: 'ctrl1'
        }
      });
      view.showAssignComponentPopup();
      expect(view.loadMasterComponentHosts.called).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#showInstallServicesPopup()", function () {
    var mock = Em.Object.create({
      config: Em.Object.create({
        initialValue: 'init',
        value: '',
        displayName: 'c1'
      }),
      setValue: Em.K,
      toggleProperty: Em.K,
      sendRequestRorDependentConfigs: Em.K
    });

    beforeEach(function() {
      sinon.stub(stringUtils, 'getFormattedStringFromArray');
      sinon.stub(mock, 'setValue');
      sinon.stub(mock, 'toggleProperty');
      sinon.stub(mock, 'sendRequestRorDependentConfigs');
    });

    afterEach(function() {
      stringUtils.getFormattedStringFromArray.restore();
      mock.setValue.restore();
      mock.toggleProperty.restore();
      mock.sendRequestRorDependentConfigs.restore();
    });

    it("test", function() {
      view.set('configWidgetContext', mock);
      var popup = view.showInstallServicesPopup();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(mock.get('config.value')).to.be.equal('init');
      expect(mock.setValue.calledWith('init')).to.be.true;
    });
  });

  describe("#removeMasterComponent()", function () {
    var mock = {
      setDBProperty: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'setDBProperty');
      sinon.stub(view, 'clearComponentsToBeAdded');
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.router.get.restore();
      mock.setDBProperty.restore();
      view.clearComponentsToBeAdded.restore();
      App.HostComponent.find.restore();
    });

    it("should set masterComponentHosts", function() {
      view.reopen({
        content: Em.Object.create({
          controllerName: 'ctrl1',
          masterComponentHosts: [
            {component: 'C1'},
            {component: 'C2'}
          ],
          componentsFromConfigs: ["C1","C2"],
          recommendationsHostGroups: {
            blueprint: {host_groups: [{name: 'host-group-1', components: [{name: 'C1'}, {name: 'C2'}]}]},
            blueprint_cluster_binding: {host_groups: [{name: 'host-group-1', hosts: [{fqdn: 'localhost'}]}]}
          }
        }),
        configWidgetContext: {
          config: Em.Object.create()
        }
      });
      view.set('mastersToCreate', ['C2']);
      view.removeMasterComponent();
      expect(view.get('content.masterComponentHosts')).to.be.eql([{component: 'C1'}]);
      expect(view.get('content.recommendationsHostGroups').blueprint).to.be.eql({host_groups: [{name: 'host-group-1', components: [{name: 'C1'}]}]});
    });

    it("should call clearComponentsToBeAdded when controllerName is null", function() {
      view.setProperties({
        content: Em.Object.create(),
        mastersToCreate: ['C1'],
        configWidgetContext: {
          config: Em.Object.create()
        }
      });
      view.removeMasterComponent();
      expect(view.clearComponentsToBeAdded.calledWith('C1')).to.be.true;
      expect(App.get('componentToBeDeleted')).to.be.eql(Em.Object.create({
        componentName: 'C1',
        hostName: 'host1'
      }));
    });
  });

  describe("#renderHostInfo()", function () {

    beforeEach(function() {
      sinon.stub(view, 'getHosts').returns([]);
    });

    afterEach(function() {
      view.getHosts.restore();
    });

    it("should make general request to get hosts", function() {
      view.reopen({
        content: Em.Object.create({
          controllerName: 'name'
        })
      });
      view.renderHostInfo();
      var args = testHelpers.findAjaxRequest('name', 'hosts.high_availability.wizard');
      expect(args).exists;
    });

    it("should make request for installer to get hosts", function() {
      view.reopen({
        content: Em.Object.create({
          controllerName: 'installerController'
        })
      });
      view.renderHostInfo();
      var args = testHelpers.findAjaxRequest('name', 'hosts.info.install');
      expect(args).exists;
    });
  });

  describe("#loadMasterComponentHosts()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1'
        }),
        Em.Object.create({
          componentName: 'C2'
        })
      ]);
      sinon.stub(App, 'get').returns(['C2']);
    });

    afterEach(function() {
      App.get.restore();
      App.HostComponent.find.restore();
    });

    it("should set master components", function() {
      view.loadMasterComponentHosts();
      expect(view.get('masterComponentHosts').mapProperty('component')).to.be.eql(['C2']);
    });
  });

  describe("#getAllMissingDependentServices()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        stackService: App.StackService.createRecord({
          requiredServices: ['S1', 'S2']
        })
      }));
      sinon.stub(App.Service, 'find').returns([
        App.Service.createRecord({serviceName: 'S1'})
      ]);
      sinon.stub(App.StackService, 'find', function(input) {
        return input
          ? Em.Object.create({displayName: input, serviceName: input})
          : [
              App.StackService.createRecord({serviceName: 'S1', displayName: 'S1'}),
              App.StackService.createRecord({serviceName: 'S2', displayName: 'S2'})
            ]
      });
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it("test", function() {
      view.set('configActionComponent', Em.Object.create({
        componentName: 'C1'
      }));
      expect(view.getAllMissingDependentServices()).to.be.eql(['S2']);
    });
  });

  describe('#getPendingBatchRequests', function() {

    it('App.ajax.send should be called', function() {
      view.getPendingBatchRequests({componentName: 'C1'});
      var args = testHelpers.findAjaxRequest('name', 'request_schedule.get.pending');
      expect(args[0]).to.be.eql({
        name : 'request_schedule.get.pending',
        sender: view,
        error : 'pendingBatchRequestsAjaxError',
        success: 'pendingBatchRequestsAjaxSuccess',
        data: {
          hostComponent: {componentName: 'C1'}
        }
      });
    });
  });

  describe('#pendingBatchRequestsAjaxError', function() {
    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function() {
      App.showAlertPopup.restore();
    });

    it('should call showAlertPopup, invalid JSON', function() {
      view.pendingBatchRequestsAjaxError({responseText: null});
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'),
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'),
        null
      )).to.be.true;
    });

    it('should call showAlertPopup, valid JSON', function() {
      view.pendingBatchRequestsAjaxError({responseText: '{"message":"foo"}'});
      expect(App.showAlertPopup.calledWith(
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'),
        Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error') + 'foo',
        null
      )).to.be.true;
    });
  });

  describe('#pendingBatchRequestsAjaxSuccess', function() {
    var configWidgetContext = Em.Object.create({
      config: Em.Object.create({
        initialValue: 'iv1',
        value: 'v1'
      }),
      controller: {
        forceUpdateBoundaries: false
      },
      setValue: sinon.spy(),
      sendRequestRorDependentConfigs: sinon.spy()
    });
    beforeEach(function() {
      this.mock = sinon.stub(view, 'shouldShowAlertOnBatchRequest');
      sinon.stub(App, 'showAlertPopup', function() {
        arguments[2].apply({hide: Em.K});
      });
      sinon.stub(view, 'showPopup');
      view.set('configWidgetContext', configWidgetContext);
    });
    afterEach(function() {
      this.mock.restore();
      App.showAlertPopup.restore();
      view.showPopup.restore();
    });

    it('showPopup should be called', function() {
      this.mock.returns(false);
      view.pendingBatchRequestsAjaxSuccess({}, {}, {hostComponent: {componentName: 'C1'}});
      expect(view.showPopup.calledWith({componentName: 'C1'})).to.be.true;
    });

    describe('showAlertPopup should be called', function() {
      beforeEach(function() {
        this.mock.returns(true);
        view.pendingBatchRequestsAjaxSuccess({}, {}, {hostComponent: {componentName: 'C1'}});
      });
      it('App.showAlertPopup is called', function () {
        expect(App.showAlertPopup.calledWith(
          Em.I18n.t('services.service.actions.hsi.alertPopup.header'),
          Em.I18n.t('services.service.actions.hsi.alertPopup.body')
        )).to.be.true;
      });
      it('config value is correct', function () {
        expect(configWidgetContext.get('config.value')).to.be.equal('iv1');
      });
      it('forceUpdateBoundaries is true', function () {
        expect(configWidgetContext.get('controller.forceUpdateBoundaries')).to.be.true;
      });
      it('configWidgetContext.setValue is called', function () {
        expect(configWidgetContext.setValue.calledWith('iv1')).to.be.true;
      });
      it('configWidgetContext.sendRequestRorDependentConfigs is called', function () {
        expect(configWidgetContext.sendRequestRorDependentConfigs.calledWith(
          configWidgetContext.get('config')
        )).to.be.true;
      });
    });
  });

  describe('#shouldShowAlertOnBatchRequest', function() {
    var testCases = [
      {
        input: {},
        expected: false
      },
      {
        input: {
          items: []
        },
        expected: false
      },
      {
        input: {
          items: [
            {
              RequestSchedule: {
                batch: {
                  batch_requests: [
                    {
                      request_type: 'ADD',
                      request_uri: ''
                    }
                  ]
                }
              }
            }
          ]
        },
        expected: false
      },
      {
        input: {
          items: [
            {
              RequestSchedule: {
                batch: {
                  batch_requests: [
                    {
                      request_type: 'DELETE',
                      request_uri: 'HIVE_SERVER_INTERACTIVE'
                    }
                  ]
                }
              }
            }
          ]
        },
        expected: true
      }
    ];

    testCases.forEach(function(test) {
      it('should return ' + test.expected + ' when data = ' + JSON.stringify(test.input), function() {
        expect(view.shouldShowAlertOnBatchRequest(test.input)).to.be.equal(test.expected);
      });
    });
  });

  describe('#updateComponent and showAddControl should be false for component', function() {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          stackService: Em.Object.create({
            isInstalled: false
          })
        })
      ]);
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it('showRemoveControl ', function() {
      var component = Em.Object.create({
        component_name: 'C1',
        showAddControl: true,
        showRemoveControl: true
      });
      view.setProperties({
        mastersToCreate: [],
        selectedServicesMasters: [ component, {component_name: 'C2'} ]
      });
      view.updateComponent('C1');
      expect(component.get('showAddControl')).to.be.false;
      expect(component.get('showRemoveControl')).to.be.false;
    });
  });

  describe('#saveRecommendationsHostGroups', function() {
    beforeEach(function() {
      sinon.stub(view, 'getSelectedHostNames').returns(['host1']);
    });
    afterEach(function() {
      view.getSelectedHostNames.restore();
    });

    it('should add component to recommendations', function() {
      var recommendationsHostGroups = {
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: 'g1',
              hosts: [
                {
                  fqdn: 'host1'
                }
              ]
            }
          ]
        },
        blueprint: {
          host_groups: [
            {
              name: 'g1',
              components: []
            }
          ]
        }
      };
      view.reopen({
        mastersToCreate: ['C1'],
        content: Em.Object.create({
          recommendationsHostGroups: recommendationsHostGroups
        })
      });
      view.saveRecommendationsHostGroups();
      expect(view.get('content.recommendationsHostGroups')).to.be.eql(Object.assign(recommendationsHostGroups, {
        blueprint: {
          host_groups: [
            {
              name: 'g1',
              components: [{name: 'C1'}]
            }
          ]
        }
      }));
    });
  });

  describe('#setGlobalComponentToBeAdded', function() {

    it('should set componentToBeAdded', function() {
      view.setGlobalComponentToBeAdded('C1', ['host1']);
      expect(App.get('componentToBeAdded')).to.be.eql(Em.Object.create({
        componentName: 'C1',
        hostNames: ['host1']
      }));
    });
  });

  describe('#clearComponentsToBeDeleted', function() {

    it('should clear componentToBeDeleted', function() {
      App.set('componentToBeDeleted', Em.Object.create({
        componentName: 'C1'
      }));
      view.clearComponentsToBeDeleted('C1');
      expect(App.get('componentToBeDeleted')).to.be.empty;
    });
  });

  describe('#clearComponentsToBeAdded', function() {

    it('should clear componentToBeAdded', function() {
      App.set('componentToBeAdded', Em.Object.create({
        componentName: 'C1'
      }));
      view.clearComponentsToBeAdded('C1');
      expect(App.get('componentToBeAdded')).to.be.empty;
    });
  });

  describe('#showPopup', function() {
    beforeEach(function() {
      this.mock = sinon.stub(view, 'getAllMissingDependentServices');
      sinon.stub(view, 'showInstallServicesPopup');
      sinon.stub(view, 'showAssignComponentPopup');
    });
    afterEach(function() {
      this.mock.restore();
      view.showInstallServicesPopup.restore();
      view.showAssignComponentPopup.restore();
    });

    it('showAssignComponentPopup should be called', function() {
      this.mock.returns([]);
      view.showPopup({componentName: 'C1'});
      expect(view.get('mastersToCreate')).to.be.eql(['C1']);
      expect(view.showAssignComponentPopup.calledOnce).to.be.true;
    });

    it('showInstallServicesPopup should be called', function() {
      this.mock.returns([{}]);
      view.reopen({
        content: Em.Object.create()
      });
      view.showPopup({componentName: 'C1'});
      expect(view.showInstallServicesPopup.calledWith([{}])).to.be.true;
    });
  });

  describe('#submit', function() {
    var configWidgetContext = Em.Object.create({
      controller: {
        forceUpdateBoundaries: false,
        stepConfigs: [
          Em.Object.create({
            serviceName: 'S1',
            configs: []
          }),
          Em.Object.create({
            serviceName: 'MISC',
            configs: []
          })
        ],
        selectedService: {
          serviceName: 'S1'
        },
        loadConfigRecommendations: function () {

        }
      },
      config: Em.Object.create({
        configAction: {
          dependencies: []
        },
        fileName: 'random',
        name: 'random'
      })
    });
    beforeEach(function() {
      sinon.stub(view, 'saveMasterComponentHosts');
      sinon.stub(view, 'saveRecommendationsHostGroups');
      sinon.stub(view, 'setGlobalComponentToBeAdded');
      sinon.stub(view, 'clearComponentsToBeDeleted');
      sinon.stub(App, 'get').returns({
        getKDCSessionState: Em.clb
      });
      sinon.stub(view, 'getSelectedHostNames').returns(['host1']);
      view.setProperties({
        configWidgetContext: configWidgetContext,
        configActionComponent: { componentName: 'C1'},
        popup: {
          hide: sinon.spy()
        }
      });
    });
    afterEach(function() {
      App.get.restore();
      view.clearComponentsToBeDeleted.restore();
      view.setGlobalComponentToBeAdded.restore();
      view.saveRecommendationsHostGroups.restore();
      view.saveMasterComponentHosts.restore();
      view.getSelectedHostNames.restore();
    });

    it('saveMasterComponentHosts should be called when controllerName defined', function() {
      view.reopen({
        content: {
          controllerName: 'ctrl1'
        }
      });
      view.submit();
      expect(view.saveMasterComponentHosts.calledOnce).to.be.true;
    });
    it('saveRecommendationsHostGroups should be called when controllerName defined', function() {
      view.reopen({
        content: {
          controllerName: 'ctrl1'
        }
      });
      view.submit();
      expect(view.saveRecommendationsHostGroups.calledOnce).to.be.true;
    });
    it('setGlobalComponentToBeAdded should be called when controllerName undefined', function() {
      view.reopen({
        content: {
          controllerName: undefined
        }
      });
      view.submit();
      expect(view.setGlobalComponentToBeAdded.calledWith('C1', ['host1'])).to.be.true;
    });
    it('clearComponentsToBeDeleted should be called when controllerName undefined', function() {
      view.reopen({
        content: {
          controllerName: undefined
        }
      });
      view.submit();
      expect(view.clearComponentsToBeDeleted.calledWith('C1')).to.be.true;
    });
    it('hide should be called', function() {
      view.submit();
      expect(view.get('popup').hide.calledOnce).to.be.true;
    });
    it('configActionComponent should be set', function() {
      view.submit();
      expect(view.get('configWidgetContext.config.configActionComponent')).to.be.eql({
        componentName: 'C1',
        hostNames: ['host1']
      });
    });
  });

  describe('#saveMasterComponentHosts', function() {
    var mockCtrl = {
      saveMasterComponentHosts: sinon.spy(),
      loadMasterComponentHosts: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mockCtrl);
      view.reopen({
        content: Em.Object.create({
          componentsFromConfigs: []
        })
      });
      view.set('mastersToCreate', [
        {}
      ]);
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('saveMasterComponentHosts should be called', function() {
      view.saveMasterComponentHosts();
      expect(mockCtrl.saveMasterComponentHosts.calledWith(view, true)).to.be.true;
    });
    it('loadMasterComponentHosts should be called', function() {
      view.saveMasterComponentHosts();
      expect(mockCtrl.loadMasterComponentHosts.calledWith(true)).to.be.true;
    });
    it('componentsFromConfigs should be set', function() {
      view.saveMasterComponentHosts();
      expect(view.get('content.componentsFromConfigs')).to.be.eql([{}]);
    });
  });

  describe('#getSelectedHostNames', function() {

    it('should return host of component', function() {
      view.set('selectedServicesMasters', [
        {
          component_name: 'C1',
          selectedHost: 'host1'
        }
      ]);
      expect(view.getSelectedHostNames('C1')).to.be.eql(['host1']);
    });
  });

  describe('#saveRecommendations', function() {
    var mockCtrl = {
      loadRecommendationsSuccess: sinon.spy(),
      wizardController: {
        name: 'installerController'
      }
    };
    var context = Em.Object.create({
      controller: mockCtrl
    });
    it('loadRecommendationsSuccess should be called', function() {
      view.set('configWidgetContext', {
        config: Em.Object.create({
          fileName: 'foo.xml',
          name: 'bar',
          initialValue: 'iv1'
        })
      });
      view.saveRecommendations(context, {});
      expect(mockCtrl.loadRecommendationsSuccess.getCall(0).args).to.be.eql([
        {
          resources: [
            {
              recommendations: {
                blueprint: {
                  configurations: {}
                }
              }
            }
          ]
        }, null, {
          dataToSend: {
            changed_configurations: [{
              type: 'foo',
              name: 'bar',
              old_value: 'iv1'
            }]
          }
        }
      ]);
    });
  });

  describe('#getDependenciesForeignKeys', function() {
    var dependencies = {
      foreignKeys: [
        {
          fileName: 'foo.xml',
          propertyName: 'c1',
          key: 'k1'
        }
      ]
    };
    var serviceConfigs = [
      Em.Object.create({
        filename: 'foo.xml',
        name: 'c1',
        value: 'val1'
      })
    ];

    it('should return foreignKeys map', function() {
      expect(view.getDependenciesForeignKeys(dependencies, serviceConfigs)).to.be.eql({
        k1: 'val1'
      });
    });
  });

  describe('#getMasterComponents', function() {
    var dependencies = {
      initializer: {
        componentNames: ['C1']
      }
    };
    var context = Em.Object.create({
      controller: {
        content: {
          masterComponentHosts: [
            {
              component: 'C1',
              hostName: 'host2'
            }
          ]
        }
      }
    });
    beforeEach(function() {
      sinon.stub(blueprintUtils, 'getComponentForHosts').returns({
        host1: ['C1']
      });
    });
    afterEach(function() {
      blueprintUtils.getComponentForHosts.restore();
    });

    it('should return master components when controllerName undefined', function() {
      view.set('content.controllerName', undefined);
      expect(view.getMasterComponents(dependencies, context)).to.be.eql([
        {
          component: 'C1',
          hostName: 'host1',
          isInstalled: true
        }
      ]);
    });

    it('should return master components when controllerName valid', function() {
      view.set('content.controllerName', 'ctrl1');
      expect(view.getMasterComponents(dependencies, context)).to.be.eql([
        {
          component: 'C1',
          hostName: 'host2',
          isInstalled: true
        }
      ]);
    });
  });
});
