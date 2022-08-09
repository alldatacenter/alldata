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
var validationUtils = require('utils/validator');
require('utils/helper');
require('controllers/wizard/step6_controller');
var controller,
  services = [
    Em.Object.create({
      serviceName: 'YARN',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'HBASE',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'STORM',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'FLUME',
      isSelected: true
    })
  ];

function getController() {
  var c = App.WizardStep6Controller.create({
    content: Em.Object.create({
      hosts: {},
      masterComponentHosts: {},
      services: services,
      controllerName: ''
    })
  });

  var h = {}, m = [];
  Em.A(['host0', 'host1', 'host2', 'host3']).forEach(function (hostName) {
    var obj = Em.Object.create({
      name: hostName,
      hostName: hostName,
      bootStatus: 'REGISTERED'
    });
    h[hostName] = obj;
    m.push(obj);
  });

  c.set('content.hosts', h);
  c.set('content.masterComponentHosts', m);
  c.set('isMasters', false);
  return c;
}

describe('App.WizardStep6Controller', function () {

  beforeEach(function () {
    controller = getController();
  });

  App.TestAliases.testAsComputedEqual(getController(), 'isAddHostWizard', 'content.controllerName', 'addHostController');

  describe('#installedServiceNames', function () {
    it(' should filter content.services by isInstalled property', function () {
      var _services = Em.A([]);
      _services.pushObjects(Em.A([{isInstalled: true, serviceName: "service1"},
                           {isInstalled: false, serviceName: "service2"},
                           {isInstalled: true, serviceName: "service3"},
                           {isInstalled: false, serviceName: "service4"},
                           {isInstalled: true, serviceName: "service5"}]));
      controller.set('content.services', _services);
      expect(controller.get('installedServiceNames')).to.eql(["service1", "service3", "service5"]);
    });
  });

  describe('#showValidationIssuesAcceptBox', function () {
    it('should return true if success callback', function () {
      var deffer = jQuery.Deferred();
      function callback() {
        deffer.resolve(true);
      }
      controller.showValidationIssuesAcceptBox(callback);
      jQuery.when(deffer.promise()).then(function(data) {
        expect(data).to.equal(true);    
      }); 
    });
  });

  describe('#selectAllNodes', function () {

    var hostsObj = Em.A([Em.Object.create({
      hasMaster: false,
      isInstalled: false,
      checkboxes: Em.A([
        Em.Object.create({
          title: 'l1',
          component: 'name',
          isInstalled: false,
          checked: false
        })
      ])
    })]);
    var obj = Em.Object.create({
      context: {
        name: "name"
      }
    });
    var clientComponents = Em.A([{component_name: "name1"}]);

    it('should make checkbox checked', function () {
      controller.set('hosts', hostsObj);
      controller.set('content.clients', clientComponents);
      controller.selectAllNodes(obj);
      expect(controller.get('hosts')).to.eql(Em.A([Em.Object.create({
        hasMaster: false,
        isInstalled: false,
        checkboxes: Em.A([
          Em.Object.create({
            title: 'l1',
            component: 'name',
            isInstalled: false,
            checked: true
          })
        ])
      })]));
    });
  });

  describe('#deselectAllNodes', function () {

    var hostsObj = Em.A([Em.Object.create({
      hasMaster: false,
      isInstalled: false,
      checkboxes: Em.A([
        Em.Object.create({
          title: 'l1',
          component: 'name',
          isInstalled: false,
          checked: true
        })
      ])
    })]);
    var obj = Em.Object.create({
      context: {
        name: "name"
      }
    });
    var clientComponents = Em.A([{component_name: "name1"}]);

    it('should uncheck checkbox', function () {
      controller.set('hosts', hostsObj);
      controller.set('content.clients', clientComponents);
      controller.deselectAllNodes(obj);
      expect(controller.get('hosts')).to.eql(Em.A([Em.Object.create({
        hasMaster: false,
        isInstalled: false,
        checkboxes: Em.A([
          Em.Object.create({
            title: 'l1',
            component: 'name',
            isInstalled: false,
            checked: false
          })
        ])
      })]));
    });
  });

  describe('#renderSlaves()', function () {
    var hostsObj = [{}];

    beforeEach(function() {
      sinon.stub(controller, 'selectRecommendedComponents');
      sinon.stub(controller, 'setInstalledComponents');
      sinon.stub(controller, 'restoreComponentsSelection');
      sinon.stub(controller, 'selectClientHost');
      sinon.stub(controller, 'enableCheckboxesForDependentComponents');
    });

    afterEach(function() {
      controller.selectRecommendedComponents.restore();
      controller.setInstalledComponents.restore();
      controller.restoreComponentsSelection.restore();
      controller.selectClientHost.restore();
      controller.enableCheckboxesForDependentComponents.restore();
    });

    describe("slaveComponents is null", function() {

      beforeEach(function() {
        controller.set('content.slaveComponentHosts', null);
        controller.set('content.controllerName', null);
      });

      it("selectRecommendedComponents should be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.selectRecommendedComponents.calledWith(hostsObj)).to.be.true;
      });
      it("setInstalledComponents should be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.setInstalledComponents.calledWith(hostsObj)).to.be.true;
      });
      it("restoreComponentsSelection should not be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.restoreComponentsSelection.called).to.be.false;
      });
      it("selectClientHost should be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.selectClientHost.calledWith(hostsObj)).to.be.true;
      });
      it("enableCheckboxesForDependentComponents should not be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.enableCheckboxesForDependentComponents.calledOnce).to.be.false;
      });
      it("enableCheckboxesForDependentComponents should be called", function() {
        controller.set('content.controllerName', 'addServiceController');
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.enableCheckboxesForDependentComponents.calledOnce).to.be.true;
      });
    });

    describe("slaveComponents is defined", function() {

      var slaveComponentHosts = [{}];

      beforeEach(function() {
        controller.set('content.slaveComponentHosts', slaveComponentHosts);
      });

      it("selectRecommendedComponents should not be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.selectRecommendedComponents.called).to.be.false;
      });
      it("setInstalledComponents should not be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.setInstalledComponents.called).to.be.false;
      });
      it("restoreComponentsSelection should be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.restoreComponentsSelection.calledWith(hostsObj, slaveComponentHosts)).to.be.true;
      });
      it("selectClientHost should be called", function() {
        expect(controller.renderSlaves(hostsObj)).to.eql(hostsObj);
        expect(controller.selectClientHost.calledWith(hostsObj)).to.be.true;
      });
    });
  });

  describe("#setInstalledComponents()", function() {
    var hostsObj = [{
      hostName: 'host1',
      checkboxes: [
        {
          component: 'C1',
          isInstalled: false,
          checked: false
        },
        {
          component: 'C2',
          isInstalled: false,
          checked: false
        }
      ]
    }];

    it("installedHosts is null", function() {
      controller.set('content.installedHosts', null);
      expect(controller.setInstalledComponents(hostsObj)).to.be.false;
    });

    it("installedHosts is defined", function() {
      controller.set('content.installedHosts', {
        'host1': {
          hostComponents: [
            {
              HostRoles: {
                component_name: 'C1'
              }
            }
          ]
        }
      });
      controller.setInstalledComponents(hostsObj);
      expect(hostsObj[0].checkboxes[0].isInstalled).to.be.true;
      expect(hostsObj[0].checkboxes[1].isInstalled).to.be.false;
      expect(hostsObj[0].checkboxes[0].checked).to.be.true;
      expect(hostsObj[0].checkboxes[1].checked).to.be.false;
    });
  });

  describe("#restoreComponentsSelection()", function() {
    var getHostsObj = function() {
      return [{
        hostName: 'host1',
        checkboxes: [
          {
            component: 'C1',
            title: 'c1',
            isInstalled: false,
            checked: false
          },
          {
            component: 'C2',
            title: 'c1',
            isInstalled: true,
            checked: false
          },
          {
            component: 'C3',
            title: 'c3',
            isInstalled: false,
            checked: false
          }
        ]
      }];
    };

    var slaveComponents = [
      {
        componentName: 'C1',
        hosts: [{hostName: 'host1', isInstalled: true}]
      }
    ];

    beforeEach(function() {
      controller.set('headers', [
        Em.Object.create({
          name: 'C1',
          label: 'c1'
        }),
        Em.Object.create({
          name: 'C2',
          label: 'c2'
        }),
        Em.Object.create({
          name: 'C3',
          label: 'c3'
        })
      ]);
    });

    it("C1 components should be checked and installed", function() {
      var hostsObj = getHostsObj();
      controller.restoreComponentsSelection(hostsObj, slaveComponents);
      expect(hostsObj[0].checkboxes[0].isInstalled).to.be.true;
      expect(hostsObj[0].checkboxes[0].checked).to.be.true;
    });
    it("C2 components should not be checked and should be installed", function() {
      var hostsObj = getHostsObj();
      controller.restoreComponentsSelection(hostsObj, slaveComponents);
      expect(hostsObj[0].checkboxes[1].isInstalled).to.be.true;
      expect(hostsObj[0].checkboxes[1].checked).to.be.false;
    });
    it("C3 components should not be checked and should not be installed", function() {
      var hostsObj = getHostsObj();
      controller.restoreComponentsSelection(hostsObj, slaveComponents);
      expect(hostsObj[0].checkboxes[2].isInstalled).to.be.false;
      expect(hostsObj[0].checkboxes[2].checked).to.be.false;
    });
  });

  describe("#selectRecommendedComponents()", function() {

    var hostsObj = [{
      hostName: 'host1',
      checkboxes: [
        {
          component: 'C1',
          checked: false,
          isDisabled: false
        },
        {
          component: 'C2',
          checked: false,
          isDisabled: true
        },
        {
          component: 'C3',
          checked: false,
          isDisabled: false
        },
        {
          component: 'CLIENT',
          checked: false,
          isDisabled: false
        }
      ]
    }];

    var recommendations = {
      blueprint: {
        host_groups: [
          {
            name: 'g1',
            components: [
              {name: 'C1'},
              {name: 'C2'},
              {name: 'C_CLIENT'}
            ]
          }
        ]
      },
      blueprint_cluster_binding: {
        host_groups: [
          {
            name: 'g1',
            hosts: [{fqdn: 'host1'}]
          }
        ]
      }
    };

    beforeEach(function() {
      sinon.stub(App, 'get').returns(['C_CLIENT']);
    });

    afterEach(function() {
      App.get.restore();
    });

    it("C1 should be checked", function() {
      controller.set('content.recommendations', recommendations);
      controller.selectRecommendedComponents(hostsObj);
      expect(hostsObj[0].checkboxes[0].checked).to.be.true;
    });

    it("C2 should not be checked, as it is disabled", function() {
      controller.set('content.recommendations', recommendations);
      controller.selectRecommendedComponents(hostsObj);
      expect(hostsObj[0].checkboxes[1].checked).to.be.false;
    });

    it("C3 should not be checked", function() {
      controller.set('content.recommendations', recommendations);
      controller.selectRecommendedComponents(hostsObj);
      expect(hostsObj[0].checkboxes[2].checked).to.be.false;
    });

    it("CLIENT should be checked", function() {
      controller.set('content.recommendations', recommendations);
      controller.selectRecommendedComponents(hostsObj);
      expect(hostsObj[0].checkboxes[3].checked).to.be.true;
    });
  });

  App.TestAliases.testAsComputedOr(getController(), 'anyGeneralErrors', ['errorMessage', 'generalErrorMessages.length']);

  describe('#render', function () {
    it('true if loaded', function () {
      var hosts = {
          h1: {bootStatus: 'REGISTERED', name: 'h1'},
          h2: {bootStatus: 'REGISTERED', name: 'h2'},
          h3: {bootStatus: 'REGISTERED', name: 'h3'}
      };
      var headers = Em.A([
        Em.Object.create({name: "c1", label: 'l1', isDisabled: true}),
        Em.Object.create({name: "c2", label: 'l2', isDisabled: false})
      ]);
      var masterComponentHosts = Em.A([
        {hostName: 'h1', component: 'c1'}
      ]);
      var recommendations = {
        blueprint: {
          host_groups: [
            {
              components: [
                {
                  name: 'c6'
                }
              ],
              name: 'host-group-1'
            },
            {
              components: [
                {
                  name: 'c8'
                }
              ],
              name: 'host-group-2'
            }
          ]
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              hosts: [
                {
                  fqdn: 'h0'
                }
              ],
              name: 'host-group-1'
            },
            {
              hosts: [
                {
                  fqdn: 'h1'
                }
              ],
              name: 'host-group-2'
            }]
        }
      };
      controller.set('content.hosts', hosts);
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.set('content.recommendations', recommendations);
      controller.set('headers', headers);
      controller.render();
      expect(controller.get('isLoaded')).to.equal(true);
    });
  });

  describe('#anyGeneralWarnings', function () {
    it('true if generalWarningMessages is non empty array and warningMessage is undefined', function () {
      controller.set('generalWarningMessages', ["warning1", "warning2"]);
      expect(controller.get('anyGeneralWarnings')).to.equal(true);
    });
    it('false if generalWarningMessages is empty array', function () {
      controller.set('generalWarningMessages', []);
      expect(controller.get('anyGeneralWarnings')).to.equal(false);
    });
    it('undefined if generalWarningMessages is undefined', function () {
      controller.set('generalWarningMessages', false);
      expect(controller.get('anyGeneralWarnings')).to.equal(false);
    });
  });

  App.TestAliases.testAsComputedOr(getController(), 'anyGeneralIssues', ['anyGeneralErrors', 'anyGeneralWarnings']);

  App.TestAliases.testAsComputedOr(getController(), 'anyErrors', ['anyGeneralErrors', 'anyHostErrors']);

  App.TestAliases.testAsComputedOr(getController(), 'anyWarnings', ['anyGeneralWarnings', 'anyHostWarnings']);

  describe('#anyWarnings', function () {
    it('true if generalWarningMessages is non empty', function () {
      controller.set('generalWarningMessages', ["error 404", "error"]);
      expect(controller.get('anyWarnings')).to.equal(true);
    });
    it('false if generalWarningMessages is empty', function () {
      controller.set('generalWarningMessages', []);
      expect(controller.get('anyWarnings')).to.equal(false);
    });
  });

  App.TestAliases.testAsComputedEqual(getController(), 'isInstallerWizard', 'content.controllerName', 'installerController');

  App.TestAliases.testAsComputedEqual(getController(), 'isAddServiceWizard', 'content.controllerName', 'addServiceController');

  describe('#selectClientHost', function () {
    it('true if isClientsSet false', function () {
      var hostsObj = Em.A([Em.Object.create({
        hasMaster: false,
        checkboxes: Em.A([
          Em.Object.create({
            component: 'c1',
            isInstalled: false,
            checked: true
          })
        ])
      })]);
      controller.set('isClientsSet', false);
      controller.selectClientHost(hostsObj);
      expect(controller.get('isClientsSet')).to.equal(true);
    });
  });

  describe('#updateValidationsSuccessCallback', function () {

    var hosts = Em.A([Em.Object.create({
      warnMessages: "warn",
      errorMessages: "error",
      anyMessage: true,
      checkboxes: Em.A([Em.Object.create({
        hasWarnMessage: true,
        hasErrorMessage: true
      })])
    })]);

    var validationData = Em.Object.create({
      resources: Em.A([
        Em.Object.create({
          items: Em.A([
            Em.Object.create({
              "component-name": 'HDFS_CLIENT',
              host: "1",
              isMaster: true
            })
          ])
        })
      ])
    });

    beforeEach(function () {
      sinon.stub(validationUtils, 'filterNotInstalledComponents', function () {
        return Em.A([Em.Object.create({
              componentName: 'c0',
              isSlave: true,
              type: 'host-component',
              level: 'ERROR'
            }),
            Em.Object.create({
              componentName: 'c1',
              isSlave: true,
              type: 'host-component',
              level: 'WARN',
              isShownOnInstallerSlaveClientPage: true
          })]);
      });
      sinon.stub(App.StackServiceComponent, 'find', function () {
          return [
            Em.Object.create({
              componentName: 'c0',
              isSlave: true
            }),
            Em.Object.create({
              componentName: 'c1',
              isSlave: true,
              isShownOnInstallerSlaveClientPage: true
            }),
            Em.Object.create({
              componentName: 'c2',
              isSlave: true,
              isShownOnInstallerSlaveClientPage: false
            }),
            Em.Object.create({
              componentName: 'c3',
              isClient: true
            }),
            Em.Object.create({
              componentName: 'c4',
              isClient: true,
              isRequiredOnAllHosts: false
            }),
            Em.Object.create({
              componentName: 'c5',
              isClient: true,
              isRequiredOnAllHosts: true
            }),
            Em.Object.create({
              componentName: 'c6',
              isMaster: true,
              isShownOnInstallerAssignMasterPage: true
            }),
            Em.Object.create({
              componentName: 'c7',
              isMaster: true,
              isShownOnInstallerAssignMasterPage: false
            }),
            Em.Object.create({
              componentName: 'HDFS_CLIENT',
              isMaster: true,
              isShownOnAddServiceAssignMasterPage: true
            }),
            Em.Object.create({
              componentName: 'c9',
              isMaster: true,
              isShownOnAddServiceAssignMasterPage: false
            })
          ];
        });
      controller.set('hosts', hosts);
      controller.updateValidationsSuccessCallback(validationData);
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
      validationUtils.filterNotInstalledComponents.restore();
    });

    it('no generalErrorMessages', function () {
      expect(controller.get('generalErrorMessages').length).to.equal(0);
    });

    it('no generalWarningMessages', function () {
      expect(controller.get('generalWarningMessages').length).to.equal(0);
    });

    it('hosts info is valid', function () {
      var cHosts = JSON.parse(JSON.stringify(controller.get('hosts')));
      var expected = [{
        warnMessages: [null],
        errorMessages: [null],
        anyMessage: true,
        checkboxes: [{
          hasWarnMessage: true,
          hasErrorMessage: true
        }]
      }];
      expect(cHosts).to.eql(expected);
    });
  });

  describe('#clearError', function () {

    var headers = Em.A([
      Em.Object.create({name: "c1", label: 't1'}),
      Em.Object.create({name: "c2", label: 't2'})]);

    beforeEach(function () {
      controller.set('errorMessage', 'error');
      controller.set('headers', headers);
    });

    it('both checkboxes are checked', function () {
      var hosts = Em.A([
        Em.Object.create({
          checkboxes: Em.A([
            Em.Object.create({
              component: 'c1',
              isInstalled: false,
              checked: true
            }),
            Em.Object.create({
              component: 'c2',
              isInstalled: false,
              checked: true
            })])
        })
      ]);

      controller.set('hosts', hosts);
      controller.clearError();
      expect(controller.get('errorMessage')).to.equal('');
    });

    it('true if is one of checkboxes checked false', function () {
      var hosts = Em.A([
        Em.Object.create({
          checkboxes: Em.A([
            Em.Object.create({
              title: "t1",
              component: 'c1',
              isInstalled: false,
              checked: false
            }),
            Em.Object.create({
              title: "t2",
              component: 'c2',
              isInstalled: false,
              checked: true
            })])
        })
      ]);

      controller.set('hosts', hosts);
      controller.set('isAddHostWizard', true);
      controller.clearError();
      expect(controller.get('errorMessage')).to.equal('error');
    });
  });

  describe('#clearStep', function () {
    beforeEach(function () {
      sinon.stub(controller, 'clearError', Em.K);
    });
    afterEach(function () {
      controller.clearError.restore();
    });
    it('should call clearError', function () {
      controller.clearStep();
      expect(controller.clearError.calledOnce).to.equal(true);
    });
    it('should clear hosts', function () {
      controller.set('hosts', [
        {},
        {}
      ]);
      controller.clearStep();
      expect(controller.get('hosts')).to.eql([]);
    });
    it('should clear headers', function () {
      controller.set('headers', [
        {},
        {}
      ]);
      controller.clearStep();
      expect(controller.get('headers')).to.eql([]);
    });
    it('should set isLoaded to false', function () {
      controller.set('isLoaded', true);
      controller.clearStep();
      expect(controller.get('isLoaded')).to.equal(false);
    });
  });

  describe('#checkCallback', function () {
    beforeEach(function () {
      sinon.stub(controller, 'clearError', Em.K);
    });
    afterEach(function () {
      controller.clearError.restore();
    });
    it('should call clearError', function () {
      controller.checkCallback('');
      expect(controller.clearError.calledOnce).to.equal(true);
    });
    Em.A([
        {
          m: 'all checked, isInstalled false',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: false
          }
        },
        {
          m: 'all checked, isInstalled true',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: true
          }
        },
        {
          m: 'no one checked',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: true
          }
        },
        {
          m: 'some checked',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: true
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: false
          }
        },
        {
          m: 'some checked, some isInstalled true',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: true
          }
        },
        {
          m: 'some checked, some isInstalled true (2)',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: true
          }
        }
      ]).forEach(function (test) {
        describe(test.m, function () {

          beforeEach(function () {
            controller.clearStep();
            controller.set('headers', test.headers);
            controller.set('hosts', test.hosts);
            controller.checkCallback(test.component);
            this.header = controller.get('headers').findProperty('name', test.component);
          });

          it('allChecked is ' + test.e.allChecked, function () {
            expect(this.header.get('allChecked')).to.equal(test.e.allChecked);
          });

          it('noChecked is ' + test.e.noChecked, function () {
            expect(this.header.get('noChecked')).to.equal(test.e.noChecked);
          });
        });
      });
  });

  describe('#getAllHosts', function () {
    var tests = Em.A([
      {
        hosts: {
          h1: {bootStatus: 'REGISTERED', name: 'h1'},
          h2: {bootStatus: 'REGISTERED', name: 'h2'},
          h3: {bootStatus: 'REGISTERED', name: 'h3'}
        },
        m: 'All REGISTERED',
        e: ['h1', 'h2', 'h3']
      },
      {
        hosts: {
          h1: {bootStatus: 'REGISTERED', name: 'h1'},
          h2: {bootStatus: 'FAILED', name: 'h2'},
          h3: {bootStatus: 'REGISTERED', name: 'h3'}
        },
        m: 'Some REGISTERED',
        e: ['h1', 'h3']
      },
      {
        hosts: {
          h1: {bootStatus: 'FAILED', name: 'h1'},
          h2: {bootStatus: 'FAILED', name: 'h2'},
          h3: {bootStatus: 'FAILED', name: 'h3'}
        },
        m: 'No one REGISTERED',
        e: []
      },
      {
        hosts: {},
        m: 'Empty hosts',
        e: []
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.hosts', test.hosts);
        var r = controller.getAllHosts();
        expect(r.mapProperty('hostName')).to.eql(test.e);
      });
    });
  });

  describe('#getMasterComponentsForHost', function () {
    var tests = Em.A([
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'}
        ]),
        hostName: 'h1',
        m: 'host exists',
        e: ['c1']
      },
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'}
        ]),
        hostName: 'h2',
        m: 'host donesn\'t exists',
        e: []
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.masterComponentHosts', test.masterComponentHosts);
        var r = controller.getMasterComponentsForHost(test.hostName);
        expect(r).to.eql(test.e);
      });
    });
  });

  describe('#selectMasterComponents', function () {
    var tests = Em.A([
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h1',
            component: 'c1'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: true,
        m: 'host and component exist'
      },
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h1',
            component: 'c2'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: false,
        m: 'host exists'
      },
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h2',
            component: 'c2'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: false,
        m: 'host and component don\'t exist'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.masterComponentHosts', test.masterComponentHosts);
        var r = controller.selectMasterComponents(test.hostsObj);
        expect(r.findProperty('hostName', 'h1').get('checkboxes').findProperty('component', 'c1').get('checked')).to.equal(test.e);
      });
    });
  });

  describe('#getCurrentMastersBlueprint', function () {
    var tests = Em.A([
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'}
        ]),
        hosts: [{hostName: 'h1'}],
        m: 'one host and one component',
        e:{
          blueprint: {
            host_groups: [
              {
                name: 'host-group-1',
                components: [
                  { name: 'c1' }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                name: 'host-group-1',
                hosts: [
                  { fqdn: 'h1' }
                ]
              }
            ]
          }
        }
      },
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'},
          {hostName: 'h2', component: 'c2'},
          {hostName: 'h2', component: 'c3'}
        ]),
        hosts: [{hostName: 'h1'}, {hostName: 'h2'}, {hostName: 'h3'}],
        m: 'multiple hosts and multiple components',
        e: {
          blueprint: {
            host_groups: [
              {
                name: 'host-group-1',
                components: [
                  { name: 'c1' }
                ]
              },
              {
                name: 'host-group-2',
                components: [
                  { name: 'c2' },
                  { name: 'c3' }
                ]
              },
              {
                name: 'host-group-3',
                components: []
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                name: 'host-group-1',
                hosts: [
                  { fqdn: 'h1' }
                ]
              },
              {
                name: 'host-group-2',
                hosts: [
                  { fqdn: 'h2' }
                ]
              },
              {
                name: 'host-group-3',
                hosts: [
                  { fqdn: 'h3' }
                ]
              }
            ]
          }
        }
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.masterComponentHosts', test.masterComponentHosts);
        controller.set('hosts', test.hosts);
        var r = controller.getCurrentMastersBlueprint();
        expect(r).to.eql(test.e);
      });
    });
  });

  describe('#getCurrentBlueprint', function () {
    var tests = Em.A([
      {
        clientComponents: Em.A([{component_name: "name1"}]),
        hosts: Em.A([
          Em.Object.create({
            checkboxes: Em.A([
              Em.Object.create({
                component: 'c1',
                checked: true
              }),
              Em.Object.create({
                component: 'CLIENT',
                checked: true
              })
            ])
          })
        ]),
        m: 'one host and one component',
        e:{
          blueprint: {
            host_groups: [
              {
                name: 'host-group-1',
                components: [
                  { name: 'c1' },
                  { name: 'name1' }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                name: 'host-group-1',
                hosts: [
                  {}
                ]
              }
            ]
          }
        }
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.clients', test.clientComponents);
        controller.set('hosts', test.hosts);
        var r = controller.getCurrentBlueprint();
        expect(JSON.parse(JSON.stringify(r))).to.eql(JSON.parse(JSON.stringify(test.e)));
      });
    });
  });

  describe('#callServerSideValidation', function () {

    var cases = [
        {
          controllerName: 'installerController',
          hosts: [
            {
              hostName: 'h0'
            },
            {
              hostName: 'h1'
            }
          ],
          expected: [
            ['c0', 'c6'],
            ['c1', 'c3', 'c8']
          ]
        },
        {
          controllerName: 'addServiceController',
          hosts: [
            {
              hostName: 'h0'
            },
            {
              hostName: 'h1'
            }
          ],
          expected: [
            ['c0', 'c6'],
            ['c1', 'c3', 'c8']
          ]
        },
        {
          controllerName: 'addHostController',
          hosts: [
            {
              hostName: 'h0'
            }
          ],
          expected: [
            ['c0', 'c2', 'c5', 'c6'],
            ['c1', 'c2', 'c3', 'c5', 'c8']
          ]
        }
      ],
      expectedHostGroups = [
        {
          name: 'host-group-1',
          fqdn: 'h0'
        },
        {
          name: 'host-group-2',
          fqdn: 'h1'
        }
      ];

    beforeEach(function () {
      controller.get('content').setProperties({
        recommendations: {
          blueprint: {
            host_groups: [
              {
                components: [
                  {
                    name: 'c6'
                  }
                ],
                name: 'host-group-1'
              },
              {
                components: [
                  {
                    name: 'c8'
                  }
                ],
                name: 'host-group-2'
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                hosts: [
                  {
                    fqdn: 'h0'
                  }
                ],
                name: 'host-group-1'
              },
              {
                hosts: [
                  {
                    fqdn: 'h1'
                  }
                ],
                name: 'host-group-2'
              }]
          }
        },
        clients: [
          {
            component_name: 'c3'
          }
        ]
      });
      sinon.stub(App.StackService, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 's0',
            isSelected: true
          }),
          Em.Object.create({
            serviceName: 's1',
            isInstalled: true,
            isSelected: true
          })
        ];
      });
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return [
          Em.Object.create({
            componentName: 'c0',
            isSlave: true
          }),
          Em.Object.create({
            componentName: 'c1',
            isSlave: true,
            isShownOnInstallerSlaveClientPage: true
          }),
          Em.Object.create({
            componentName: 'c2',
            isSlave: true,
            isShownOnInstallerSlaveClientPage: false
          }),
          Em.Object.create({
            componentName: 'c3',
            isClient: true
          }),
          Em.Object.create({
            componentName: 'c4',
            isClient: true,
            isRequiredOnAllHosts: false
          }),
          Em.Object.create({
            componentName: 'c5',
            isClient: true,
            isRequiredOnAllHosts: true
          }),
          Em.Object.create({
            componentName: 'c6',
            isMaster: true,
            isShownOnInstallerAssignMasterPage: true
          }),
          Em.Object.create({
            componentName: 'c7',
            isMaster: true,
            isShownOnInstallerAssignMasterPage: false
          }),
          Em.Object.create({
            componentName: 'c8',
            isMaster: true,
            isShownOnAddServiceAssignMasterPage: true
          }),
          Em.Object.create({
            componentName: 'c9',
            isMaster: true,
            isShownOnAddServiceAssignMasterPage: false
          })
        ];
      });
      sinon.stub(controller, 'getCurrentBlueprint', function () {
        return {
          blueprint: {
            host_groups: [
              {
                components: [
                  {
                    name: 'c0'
                  }
                ],
                name: 'host-group-1'
              },
              {
                components: [
                  {
                    name: 'c1'
                  },
                  {
                    name: 'c3'
                  }
                ],
                name: 'host-group-2'
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                hosts: [
                  {
                    fqdn: 'h0'
                  }
                ],
                name: 'host-group-1'
              },
              {
                hosts: [
                  {
                    fqdn: 'h1'
                  }
                ],
                name: 'host-group-2'
              }]
          }
        };
      });
      sinon.stub(controller, 'getCurrentMastersBlueprint', function () {
        return {
          blueprint: {
            host_groups: [
              {
                components: [
                  {
                    name: 'c6'
                  }
                ],
                name: 'host-group-1'
              },
              {
                components: [
                  {
                    name: 'c8'
                  }
                ],
                name: 'host-group-2'
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                hosts: [
                  {
                    fqdn: 'h0'
                  }
                ],
                name: 'host-group-1'
              },
              {
                hosts: [
                  {
                    fqdn: 'h1'
                  }
                ],
                name: 'host-group-2'
              }]
          }
        };
      });
      sinon.stub(App, 'get').withArgs('components.clients').returns(['c3', 'c4']);
      sinon.stub(controller, 'getCurrentMasterSlaveBlueprint', function () {
        return {
          blueprint: {
            host_groups: [
              {
                components: [
                  {
                    name: 'c6'
                  }
                ],
                name: 'host-group-1'
              },
              {
                components: [
                  {
                    name: 'c8'
                  }
                ],
                name: 'host-group-2'
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                hosts: [
                  {
                    fqdn: 'h0'
                  }
                ],
                name: 'host-group-1'
              },
              {
                hosts: [
                  {
                    fqdn: 'h1'
                  }
                ],
                name: 'host-group-2'
              }]
          }
        };
      });
      sinon.stub(App.Host, 'find', function () {
        return [
          {
            hostName: 'h1'
          }
        ];
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.StackServiceComponent.find.restore();
      controller.getCurrentBlueprint.restore();
      controller.getCurrentMastersBlueprint.restore();
      App.get.restore();
      controller.getCurrentMasterSlaveBlueprint.restore();
      App.Host.find.restore();
    });

    cases.forEach(function (item) {
      describe(item.controllerName, function () {

        beforeEach(function () {
          controller.set('hosts', item.hosts);
          controller.set('content.controllerName', item.controllerName);
          controller.callServerSideValidation();
        });

        it('blueprint.host_groups count is correct', function () {
          expect(controller.get('content.recommendationsHostGroups.blueprint.host_groups.length')).to.equal(expectedHostGroups.length);
        });

        it('blueprint_cluster_binding.host_groups count is correct', function () {
          expect(controller.get('content.recommendationsHostGroups.blueprint_cluster_binding.host_groups.length')).to.equal(expectedHostGroups.length);
        });

        item.expected.forEach(function (e, index) {
          it('components are valid for group# ' + (index + 1), function () {
            expect(controller.get('content.recommendationsHostGroups.blueprint.host_groups')[index].components.mapProperty('name').sort()).to.be.eql(e);
          });
        });

        expectedHostGroups.forEach(function (group) {
          it(group.name, function () {
            var bpGroup = controller.get('content.recommendationsHostGroups.blueprint_cluster_binding.host_groups').findProperty('name', group.name);
            expect(bpGroup.hosts).to.have.length(1);
            expect(bpGroup.hosts[0].fqdn).to.equal(group.fqdn);
          });
        });

      });
    });

  });

  describe('#isAllCheckboxesEmpty', function () {

    Em.A([
      {
        m: 'all checkboxes are not empty',
        hosts: [
          {checkboxes: [{checked: true}, {checked: true}]},
          {checkboxes: [{checked: true}, {checked: true}]}
        ],
        e: false
      },
      {
        m: 'some checkboxes are empty',
        hosts: [
          {checkboxes: [{checked: true}, {checked: false}]},
          {checkboxes: [{checked: true}, {checked: false}]}
        ],
        e: false
      },
      {
        m: 'all checkboxes are empty',
        hosts: [
          {checkboxes: [{checked: false}, {checked: false}]},
          {checkboxes: [{checked: false}, {checked: false}]}
        ],
        e: true
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        controller.set('hosts', test.hosts);
        expect(controller.isAllCheckboxesEmpty()).to.be.equal(test.e);
      });

    });

  });

  describe('#loadStep', function () {

    beforeEach(function () {
      sinon.stub(controller, 'render', Em.K);
      sinon.stub(controller, 'callValidation', Em.K);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          isSelected: true,
          serviceName: 's1',
          serviceComponents: [
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's1c1', isRequired: true}),
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's1c2', isRequired: true})
          ]
        }),
        Em.Object.create({
          isSelected: true,
          serviceName: 's2',
          serviceComponents: [
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's2c3', isRequired: false}),
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's2c4', isRequired: false})
          ]
        }),
        Em.Object.create({
          isInstalled: true,
          serviceName: 's3',
          serviceComponents: [
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's3c1', isRequired: true}),
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's3c2', isRequired: true})
          ]
        }),
        Em.Object.create({
          isInstalled: true,
          serviceName: 's4',
          serviceComponents: [
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's4c3', isRequired: false}),
            Em.Object.create({isShownOnInstallerSlaveClientPage: true, componentName: 's4c4', isRequired: false})
          ]
        })
      ]);
    });

    afterEach(function () {
      controller.render.restore();
      controller.callValidation.restore();
      App.StackService.find.restore();
    });

    describe('isInstallerWizard', function () {

      beforeEach(function () {
        controller.set('content', {
          clients: [{}],
          controllerName: 'installerController'
        });
        controller.loadStep();
      });

      it('component names are valid', function () {
        expect(controller.get('headers').mapProperty('name')).to.be.eql(['s1c1', 's1c2', 's2c3', 's2c4', 'CLIENT']);
      });

      it('component labels are valid', function () {
        expect(controller.get('headers').mapProperty('label')).to.be.eql(['S1c1', 'S1c2', 'S2c3', 'S2c4', 'Client']);
      });

      it('everyone allChecked is false', function () {
        expect(controller.get('headers').everyProperty('allChecked', false)).to.be.true;
      });

      it('component required-flags are valid', function () {
        expect(controller.get('headers').mapProperty('isRequired')).to.be.eql([true, true, false, false, undefined]);
      });

      it('everyone noChecked is false', function () {
        expect(controller.get('headers').everyProperty('noChecked', true)).to.be.true;
      });

      it('everyone isDisabled is false', function () {
        expect(controller.get('headers').everyProperty('isDisabled', false)).to.be.true;
      });

      it('component allId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('allId')).to.be.eql(['all-s1c1', 'all-s1c2', 'all-s2c3', 'all-s2c4', 'all-CLIENT']);
      });

      it('component noneId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('noneId')).to.be.eql(['none-s1c1', 'none-s1c2', 'none-s2c3', 'none-s2c4', 'none-CLIENT']);
      });

    });

    describe('isAddHostWizard', function () {

      beforeEach(function () {
        controller.set('content', {
          clients: [{}],
          controllerName: 'addHostController'
        });
        controller.loadStep();
      });

      it('component names are valid', function () {
        expect(controller.get('headers').mapProperty('name')).to.be.eql(['s3c1', 's3c2', 's4c3', 's4c4', 'CLIENT']);
      });

      it('component labels are valid', function () {
        expect(controller.get('headers').mapProperty('label')).to.be.eql(['S3c1', 'S3c2', 'S4c3', 'S4c4', 'Client']);
      });

      it('everyone allChecked is false', function () {
        expect(controller.get('headers').everyProperty('allChecked', false)).to.be.true;
      });

      it('component required-flags are valid', function () {
        expect(controller.get('headers').mapProperty('isRequired')).to.be.eql([true, true, false, false, undefined]);
      });

      it('everyone noChecked is false', function () {
        expect(controller.get('headers').everyProperty('noChecked', true)).to.be.true;
      });

      it('everyone isDisabled is false', function () {
        expect(controller.get('headers').everyProperty('isDisabled', false)).to.be.true;
      });

      it('component allId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('allId')).to.be.eql(['all-s3c1', 'all-s3c2', 'all-s4c3', 'all-s4c4', 'all-CLIENT']);
      });

      it('component noneId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('noneId')).to.be.eql(['none-s3c1', 'none-s3c2', 'none-s4c3', 'none-s4c4', 'none-CLIENT']);
      });

    });

    describe('isAddServiceWizard', function () {

      beforeEach(function () {
        controller.set('content', {
          clients: [{}],
          controllerName: 'addServiceController'
        });
        controller.loadStep();
      });

      it('component names are valid', function () {
        expect(controller.get('headers').mapProperty('name')).to.be.eql(['s3c1', 's3c2', 's4c3', 's4c4', 's1c1', 's1c2', 's2c3', 's2c4', 'CLIENT']);
      });

      it('component labels are valid', function () {
        expect(controller.get('headers').mapProperty('label')).to.be.eql(['S3c1', 'S3c2', 'S4c3', 'S4c4', 'S1c1', 'S1c2', 'S2c3', 'S2c4', 'Client']);
      });

      it('everyone allChecked is false', function () {
        expect(controller.get('headers').everyProperty('allChecked', false)).to.be.true;
      });

      it('component required-flags are valid', function () {
        expect(controller.get('headers').mapProperty('isRequired')).to.be.eql([true, true, false, false, true, true, false, false, undefined]);
      });

      it('everyone noChecked is false', function () {
        expect(controller.get('headers').everyProperty('noChecked', true)).to.be.true;
      });

      it('installed services are disabled', function () {
        expect(controller.get('headers').mapProperty('isDisabled', false)).to.be.eql([true, true, true, true, false, false, false, false, false]);
      });

      it('component allId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('allId')).to.be.eql(['all-s3c1', 'all-s3c2', 'all-s4c3', 'all-s4c4', 'all-s1c1', 'all-s1c2', 'all-s2c3', 'all-s2c4', 'all-CLIENT']);
      });

      it('component noneId-fields are valid', function () {
        expect(controller.get('headers').mapProperty('noneId')).to.be.eql(['none-s3c1', 'none-s3c2', 'none-s4c3', 'none-s4c4', 'none-s1c1', 'none-s1c2', 'none-s2c3', 'none-s2c4', 'none-CLIENT']);
      });

    });

  });
   
  describe('#anyHostErrors', function () {

    var tests = [
    {
       it: "anyHostErrors returns true if errorMessages are defined",
       host: Em.A([Em.Object.create({
          errorMessages: "Error Message"
       })]),
       result: true
     },
     {
       it: "anyHostErrors returns false if errorMessages are not defined",
       host: Em.A([Em.Object.create({
       })]),
       result: false
     }
    ];

    tests.forEach(function(test) {
      it(test.it, function() {
        controller.set('hosts', test.host);
        expect(controller.get('anyHostErrors')).to.equal(test.result);
      })
    });   
  });


   
  describe('#anyHostWarnings', function () {

    var tests = [
    {
       it: "anyHostWarnings returns true if warnMessages are defined",
       host: Em.A([Em.Object.create({
          warnMessages: "Warning Message"
       })]),
       result: true
     },
     {
       it: "anyHostWarnings returns false if warnMessages are not defined",
       host: Em.A([Em.Object.create({
       })]),
       result: false
     }
    ];

    tests.forEach(function(test) {
      it(test.it, function() {
        controller.set('hosts', test.host);
        expect(controller.get('anyHostWarnings')).to.equal(test.result);
      })
    });   
  });

  describe('#enableCheckboxesForDependentComponents', function () {

    beforeEach(function () {
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 's1',
          isInstalled: false,
          isSelected: true,
          serviceComponents: [
            Em.Object.create({
              componentName: 'c1',
              isSlave: true,
              dependencies: [
                {
                  serviceName: 's2',
                  componentName: 'c2'
                }
              ]
            })
          ]
        }),
        Em.Object.create({
          serviceName: 's2',
          isInstalled: true,
          isSelected: false,
          serviceComponents: [
            Em.Object.create({
              componentName: 'c2',
              isSlave: true,
              dependencies: []
            })
          ]
        })
      ]);
      sinon.stub(App.StackServiceComponent, 'find').returns([
          Em.Object.create({
            componentName: 'c2',
            maxToInstall: 2
          })
      ]);
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.StackServiceComponent.find.restore();
    });

    it('it should enable appropriate checkboxes', function() {
      var hostObj = [
        {
          checkboxes: [
            {
              component: 'c1',
              isInstalled: false,
              isDisabled: false
            },
            {
              component: 'c2',
              isInstalled: false,
              isDisabled: true
            }
          ]
        },
        {
          checkboxes: [
            {
              component: 'c1',
              isInstalled: false,
              isDisabled: false
            },
            {
              component: 'c2',
              isInstalled: false,
              isDisabled: true
            }
          ]
        }
      ];
      expect(controller.enableCheckboxesForDependentComponents(hostObj)).to.be.true;
      expect(hostObj[1].checkboxes[1].isDisabled).to.be.false;
    })
  });

});
