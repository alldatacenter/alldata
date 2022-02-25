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
require('controllers/wizard/step5_controller');
var modelSetup = require('test/init_model_test');
require('utils/ajax/ajax');
var c;
describe('App.WizardStep5Controller', function () {

  beforeEach(function () {
    c = App.WizardStep5Controller.create();
    sinon.stub(App.router, 'send', Em.K);
    App.set('router.nextBtnClickInProgress', false);
  });

  afterEach(function () {
    App.router.send.restore();
    App.set('router.nextBtnClickInProgress', false);
  });

  var controller = App.WizardStep5Controller.create();
  controller.set('content', {});

  controller.set('content', {});

  describe('#sortHosts', function () {

    var tests = Em.A([
      {
        hosts: [
          Em.Object.create({memory: 4, cpu: 1, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 3, cpu: 1, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 2, cpu: 1, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 4})
        ],
        m: 'memory',
        e: [1, 2, 3, 4]
      },
      {
        hosts: [
          Em.Object.create({memory: 1, cpu: 4, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 1, cpu: 3, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 1, cpu: 2, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 4})
        ],
        m: 'cpu',
        e: [1, 2, 3, 4]
      },
      {
        hosts: [
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 1}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host1', id: 4})
        ],
        m: 'host_name',
        e: [4, 2, 3, 1]
      },
      {
        hosts: [
          Em.Object.create({memory: 2, cpu: 1, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 1, cpu: 2, host_name: 'host3', id: 2}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host2', id: 4})
        ],
        m: 'mix',
        e: [1, 2, 4, 3]
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        var hosts = Em.copy(test.hosts);
        controller.sortHosts(hosts);
        expect(Em.A(hosts).mapProperty('id')).to.eql(test.e);
      });
    });

  });

  describe('#loadWizardHostsSuccessCallback', function () {

    var tests = Em.A([
      {
        dbHosts: {
          host1: {bootStatus: 'INIT'},
          host2: {bootStatus: 'INIT'},
          host3: {bootStatus: 'INIT'},
          host4: {bootStatus: 'INIT'}
        },
        hosts: [
          {Hosts: {total_mem: 4, cpu_count: 1, host_name: 'host1', bootStatus: 'INIT'}},
          {Hosts: {total_mem: 3, cpu_count: 1, host_name: 'host2', bootStatus: 'INIT'}},
          {Hosts: {total_mem: 2, cpu_count: 1, host_name: 'host3', bootStatus: 'INIT'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host4', bootStatus: 'INIT'}}
        ],
        m: 'no one host is REGISTERED',
        e: []
      },
      {
        dbHosts: {
          host1: {bootStatus: 'REGISTERED'},
          host2: {bootStatus: 'REGISTERED'},
          host3: {bootStatus: 'REGISTERED'},
          host4: {bootStatus: 'REGISTERED'}
        },
        hosts: [
          {Hosts: {total_mem: 4, cpu_count: 1, host_name: 'host1', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 3, cpu_count: 1, host_name: 'host2', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 2, cpu_count: 1, host_name: 'host3', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host4', bootStatus: 'REGISTERED'}}
        ],
        m: 'all hosts are REGISTERED, memory',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        dbHosts: {
          host1: {bootStatus: 'REGISTERED'},
          host2: {bootStatus: 'REGISTERED'},
          host3: {bootStatus: 'REGISTERED'},
          host4: {bootStatus: 'REGISTERED'}
        },
        hosts: [
          {Hosts: {total_mem: 1, cpu_count: 4, host_name: 'host1', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 3, host_name: 'host2', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 2, host_name: 'host3', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host4', bootStatus: 'REGISTERED'}}
        ],
        m: 'all hosts are REGISTERED, cpu',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        dbHosts: {
          host1: {bootStatus: 'REGISTERED'},
          host2: {bootStatus: 'REGISTERED'},
          host3: {bootStatus: 'REGISTERED'},
          host4: {bootStatus: 'REGISTERED'}
        },
        hosts: [
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host4', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host2', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host3', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host1', bootStatus: 'REGISTERED'}}
        ],
        m: 'all hosts are REGISTERED, host_name',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        dbHosts: {
          host1: {bootStatus: 'REGISTERED'},
          host2: {bootStatus: 'INIT'},
          host3: {bootStatus: 'INIT'},
          host4: {bootStatus: 'REGISTERED'}
        },
        hosts: [
          {Hosts: {total_mem: 2, cpu_count: 1, host_name: 'host1', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 2, host_name: 'host3', bootStatus: 'INIT'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host4', bootStatus: 'REGISTERED'}},
          {Hosts: {total_mem: 1, cpu_count: 1, host_name: 'host2', bootStatus: 'INIT'}}
        ],
        m: 'mix',
        e: ['host1', 'host4']
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content', {hosts: test.dbHosts});
        controller.loadWizardHostsSuccessCallback({items: test.hosts});
        var r = controller.get('hosts');
        expect(Em.A(r).mapProperty('host_name')).to.eql(test.e);
      });
    });

  });

  describe('#last', function () {

    var tests = Em.A([
      {
        selectedServicesMasters: Em.A([
          {component_name: 'c1', indx: 1},
          {component_name: 'c2', indx: 2},
          {component_name: 'c1', indx: 2}
        ]),
        m: 'Components exists',
        c: 'c1',
        e: 2
      },
      {
        selectedServicesMasters: Em.A([
          {component_name: 'c1', indx: 1},
          {component_name: 'c2', indx: 2},
          {component_name: 'c1', indx: 2}
        ]),
        m: 'Components don\'t exists',
        c: 'c3',
        e: null
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('selectedServicesMasters', test.selectedServicesMasters);
        if (!Em.isNone(test.e)) {
          expect(controller.last(test.c).indx).to.equal(test.e);
        }
        else {
          expect(Em.isNone(controller.last(test.c))).to.equal(true);
        }
      })
    });

  });

  describe('#remainingHosts', function () {
    it('should show count of hosts without masters', function () {
      c.reopen({masterHostMapping: [
        {}
      ]});
      c.set('hosts', [
        {},
        {},
        {}
      ]);
      expect(c.get('remainingHosts')).to.equal(2);
    });
  });

  describe('#clearStep', function () {
    var tests = Em.A([
      {p: 'hosts'},
      {p: 'selectedServicesMasters'},
      {p: 'servicesMasters'}
    ]);
    tests.forEach(function (test) {
      it('should cleanup ' + test.p, function () {
        c.set(test.p, [Em.Object.create({}), Em.Object.create({})]);
        c.clearStep();
        expect(c.get(test.p).length).to.equal(0);
      });
    });
  });

  describe('#updateComponent', function () {
    var tests = Em.A([
      {
        componentName: 'HBASE_SERVER',
        serviceComponents: [
          Em.Object.create({
            componentName: 'HBASE_SERVER',
            stackService: Em.Object.create({isInstalled: true, serviceName: 'HBASE'})
          })
        ],
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: false, showRemoveControl: true, component_name: 'HBASE_SERVER'}),
          Em.Object.create({showAddControl: true, showRemoveControl: false, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({})
        ]),
        controllerName: 'addServiceController',
        m: 'service is installed',
        e: {
          showAddControl: true,
          showRemoveControl: false
        }
      },
      {
        componentName: 'HBASE_SERVER',
        serviceComponents: [
          Em.Object.create({
            componentName: 'HBASE_SERVER',
            stackService: Em.Object.create({isInstalled: false, serviceName: 'HBASE'})
          })
        ],
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: true, showRemoveControl: false, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({})
        ]),
        controllerName: 'addServiceController',
        m: 'service not installed, but all host already have provided component',
        e: {
          showAddControl: true,
          showRemoveControl: false
        }
      },
      {
        componentName: 'HBASE_SERVER',
        serviceComponents: [
          Em.Object.create({
            componentName: 'HBASE_SERVER',
            stackService:  Em.Object.create({isInstalled: false, serviceName: 'HBASE'})
          })
        ],
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: false, showRemoveControl: true, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({}),
          Em.Object.create({})
        ]),
        controllerName: 'reassignMasterController',
        m: 'service not installed, not all host already have provided component, but is reassignMasterController',
        e: {
          showAddControl: false,
          showRemoveControl: false
        }
      }
    ]);

    tests.forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          sinon.stub(App.StackServiceComponent, 'find', function () {
            return test.serviceComponents;
          });
          c.reopen({
            content: Em.Object.create({
              controllerName: test.controllerName
            }),
            selectedServicesMasters: test.selectedServicesMasters,
            hosts: test.hosts
          });
          c.updateComponent(test.componentName);
        });

        afterEach(function () {
          App.StackServiceComponent.find.restore();
        });

        Em.keys(test.e).forEach(function (k) {
          it(k, function () {
            expect(c.last(test.componentName).get(k)).to.equal(test.e[k]);
          });
        });

      });
    });
  });

  describe('#renderComponents', function () {
    var tests = Em.A([
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'}
        ]),
        services: Em.A([
          Em.Object.create({serviceName: 'ZOOKEEPER', isInstalled: false, isSelected: true})
        ]),
        controllerName: 'reassignMasterController',
        m: 'One component',
        isHaEnabled: false,
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER'],
          showRemoveControl: [false],
          isInstalled: [false],
          serviceComponentId: [1]
        }
      },
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'}
        ]),
        services: Em.A([
          Em.Object.create({serviceName: 'ZOOKEEPER', isInstalled: false, isSelected: true})
        ]),
        controllerName: 'addServiceController',
        m: 'One component, service is not installed',
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER'],
          showRemoveControl: [false],
          serviceComponentId: [1]
        }
      },
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'},
          {component_name: 'ZOOKEEPER_SERVER'}
        ]),
        services: Em.A([
          Em.Object.create({serviceName: 'ZOOKEEPER', isInstalled: true})
        ]),
        controllerName: 'addServiceController',
        m: 'Two components, but service is installed',
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER'],
          showRemoveControl: [false, false],
          serviceComponentId: [1, 2]
        }
      }
    ]);
    tests.forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          sinon.stub(App, 'get').withArgs('isHaEnabled').returns(test.isHaEnabled);
          sinon.stub(App.StackService, 'find', function () {
            return test.services;
          });
          modelSetup.setupStackServiceComponent();
          c.reopen({
            content: Em.Object.create({
              services: test.services,
              controllerName: test.controllerName,
              reassign: {component_name: test.component_name}
            })
          });
          c.renderComponents(test.masterComponents);
        });

        afterEach(function () {
          App.get.restore();
          App.StackService.find.restore();
          modelSetup.cleanStackServiceComponent();
        });

        it('all selectedServicesMasters.component_name are valid', function () {
          expect(c.get('selectedServicesMasters').mapProperty('component_name')).to.eql(test.e.selectedServicesMasters);
        });

        it('all servicesMasters.component_name are valid', function () {
          expect(c.get('servicesMasters').mapProperty('component_name')).to.eql(test.e.servicesMasters);
        });

        it('all showRemoveControl are valid', function () {
          expect(c.get('selectedServicesMasters').mapProperty('showRemoveControl')).to.eql(test.e.showRemoveControl);
        });

        it('all serviceComponentId are valid', function () {
          expect(c.get('selectedServicesMasters').mapProperty('serviceComponentId')).to.eql(test.e.serviceComponentId);
        });

        it('servicesMasters.@each.isInstalled is valid', function () {
          if (c.get('isReasignController')) {
            expect(c.get('servicesMasters').mapProperty('isInstalled')).to.eql(test.e.isInstalled);
          }
        });

      });
    });
  });

  describe('#assignHostToMaster', function () {
    var tests = Em.A([
        {
          componentName: 'c1',
          selectedHost: 'h2',
          serviceComponentId: '1',
          e: {
            indx: 0
          }
        },
        {
          componentName: 'c2',
          selectedHost: 'h3',
          serviceComponentId: '2',
          e: {
            indx: 3
          }
        },
        {
          componentName: 'c3',
          selectedHost: 'h1',
          e: {
            indx: 2
          }
        },
        {
          componentName: 'c2',
          selectedHost: 'h4',
          e: {
            indx: 1
          }
        }
      ]),
      selectedServicesMasters = Em.A([
        Em.Object.create({component_name: 'c1', serviceComponentId: '1', selectedHost: 'h1'}),
        Em.Object.create({component_name: 'c2', serviceComponentId: '1', selectedHost: 'h1'}),
        Em.Object.create({component_name: 'c3', serviceComponentId: '1', selectedHost: 'h3'}),
        Em.Object.create({component_name: 'c2', serviceComponentId: '2', selectedHost: 'h2'})
      ]);

    tests.forEach(function (test) {
      it(test.componentName + ' ' + test.selectedHost + ' ' + test.serviceComponentId, function () {
        c.set('selectedServicesMasters', selectedServicesMasters);
        c.assignHostToMaster(test.componentName, test.selectedHost, test.serviceComponentId);
        expect(c.get('selectedServicesMasters').objectAt(test.e.indx).get('selectedHost')).to.equal(test.selectedHost);
      })
    });
  });

  describe('#removeComponent', function () {

    beforeEach(function () {
      sinon.stub(c, 'getMaxNumberOfMasters', function () {
        return Infinity;
      });
    });

    afterEach(function(){
      c.getMaxNumberOfMasters.restore();
    });

    var tests = Em.A([
      {
        componentName: 'c1',
        serviceComponentId: 1,
        selectedServicesMasters: Em.A([]),
        hosts: [],
        m: 'empty selectedServicesMasters',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        serviceComponentId: 1,
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'HBASE_SERVER'})
        ]),
        hosts: [],
        m: 'no such components',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        serviceComponentId: 1,
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'ZOOKEPEER_SERVER'})
        ]),
        hosts: [],
        m: 'component is only 1',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        serviceComponentId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({serviceComponentId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [
          {},
          {}
        ],
        m: 'two components, add allowed, remove not allowed',
        e: true,
        showAddControl: true,
        showRemoveControl: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        serviceComponentId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({serviceComponentId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({serviceComponentId: 3, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: true})
        ]),
        hosts: [
          {},
          {},
          {}
        ],
        m: 'three components, add allowed, remove allowed',
        e: true,
        showAddControl: true,
        showRemoveControl: true
      }
    ]);
    tests.forEach(function (test) {
      describe(test.m, function () {

        beforeEach(function () {
          c.set('selectedServicesMasters', JSON.parse(JSON.stringify(test.selectedServicesMasters)));
          c.set('hosts', test.hosts);
          this.result = c.removeComponent(test.componentName, test.serviceComponentId);
        });

        it('removeComponent returns ' + test.e, function () {
          expect(this.result).to.equal(test.e);
        });

        if (test.e) {
          it('showRemoveControl is correct', function () {
            expect(c.get('selectedServicesMasters.lastObject.showRemoveControl')).to.equal(test.showRemoveControl);
          });

          it('showAddControl is correct', function () {
            expect(c.get('selectedServicesMasters.lastObject.showAddControl')).to.equal(test.showAddControl);
          });

          it('componentToRebalance is correct', function () {
            expect(c.get('componentToRebalance')).to.equal(test.componentName);
          });

          it('lastChangedComponent is correct', function () {
            expect(c.get('lastChangedComponent')).to.equal(test.componentName);
          });
        }
      })
    });
  });

  describe('#addComponent', function () {

    beforeEach(function () {
      sinon.stub(c, 'getMaxNumberOfMasters', function () {
        return Infinity;
      });
    });

    afterEach(function(){
      c.getMaxNumberOfMasters.restore();
    });

    var tests = Em.A([
      {
        componentName: 'c1',
        selectedServicesMasters: Em.A([]),
        hosts: [],
        m: 'empty selectedServicesMasters',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'HBASE_SERVER'})
        ]),
        hosts: [],
        m: 'no such components',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({serviceComponentId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({serviceComponentId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [Em.Object.create({}), Em.Object.create({}), Em.Object.create({})],
        m: 'two components, 3 hosts',
        e: true
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.set('selectedServicesMasters', test.selectedServicesMasters);
        c.set('hosts', test.hosts);
        expect(c.addComponent(test.componentName)).to.equal(test.e);
        if (test.e) {
          expect(c.get('componentToRebalance')).to.equal(test.componentName);
          expect(c.get('lastChangedComponent')).to.equal(test.componentName);
        }
      });
    });
  });

  describe('#masterHostMapping', function () {
    Em.A([
        {
          selectedServicesMasters: [
            Em.Object.create({selectedHost: 'h1'}),
            Em.Object.create({selectedHost: 'h2'}),
            Em.Object.create({selectedHost: 'h1'})
          ],
          hosts: [
            Em.Object.create({host_name: 'h1', host_info: {}}),
            Em.Object.create({host_name: 'h2', host_info: {}})
          ],
          m: 'Two hosts',
          e: [
            {host_name: 'h1', hostInfo: {}, masterServices: [
              {},
              {}
            ]},
            {host_name: 'h2', hostInfo: {}, masterServices: [
              {}
            ]}
          ]
        },
        {
          selectedServicesMasters: [],
          hosts: [],
          m: 'No hosts',
          e: []
        },
        {
          selectedServicesMasters: [
            Em.Object.create({selectedHost: 'h1'}),
            Em.Object.create({selectedHost: 'h1'})
          ],
          hosts: [
            Em.Object.create({host_name: 'h1', host_info: {}})
          ],
          m: 'One host',
          e: [
            {host_name: 'h1', hostInfo: {}, masterServices: [
              {},
              {}
            ]}
          ]
        }
      ]).forEach(function (test) {
        describe(test.m, function () {
          var result;
          beforeEach(function () {
            c.reopen({
              selectedServicesMasters: test.selectedServicesMasters,
              hosts: test.hosts
            });
            result = c.get('masterHostMapping');
          });

          it('all needed hosts are mapped', function () {
            expect(result.length).to.equal(test.e.length);
          });

          it('all needed hosts have valid data', function () {
            expect(result.mapProperty('host_name')).to.be.eql(test.e.mapProperty('host_name'));
            expect(result.mapProperty('masterServices.length')).to.be.eql(test.e.mapProperty('masterServices.length'));
          });
        });
      });
  });

  describe('#anyError', function () {

    Em.A([
        {
          servicesMasters: [
            Em.Object.create({errorMessage: 'some message'}),
            Em.Object.create({errorMessage: ''})
          ],
          generalErrorMessages: [],
          e: true
        },
        {
          servicesMasters: [
            Em.Object.create({errorMessage: ''}),
            Em.Object.create({errorMessage: ''})
          ],
          generalErrorMessages: [],
          e: false
        },
        {
          servicesMasters: [
            Em.Object.create({errorMessage: 'some message'}),
            Em.Object.create({errorMessage: 'some message 2'})
          ],
          generalErrorMessages: ['some message'],
          e: true
        },
        {
          servicesMasters: [
            Em.Object.create({errorMessage: ''}),
            Em.Object.create({errorMessage: ''})
          ],
          generalErrorMessages: ['some message'],
          e: true
        }
      ]).forEach(function (test, i) {
        it('test #' + i.toString(), function () {
          c.setProperties({
            servicesMasters: test.servicesMasters,
            generalErrorMessages: test.generalErrorMessages
          });
          expect(c.get('anyError')).to.equal(test.e);
        });
      });

  });

  describe('#anyWarning', function () {

    Em.A([
        {
          servicesMasters: [
            Em.Object.create({warnMessage: 'some message'}),
            Em.Object.create({warnMessage: ''})
          ],
          generalWarningMessages: [],
          e: true
        },
        {
          servicesMasters: [
            Em.Object.create({warnMessage: ''}),
            Em.Object.create({warnMessage: ''})
          ],
          generalWarningMessages: [],
          e: false
        },
        {
          servicesMasters: [
            Em.Object.create({warnMessage: 'some message'}),
            Em.Object.create({warnMessage: 'some message 2'})
          ],
          generalWarningMessages: ['some message'],
          e: true
        },
        {
          servicesMasters: [
            Em.Object.create({warnMessage: ''}),
            Em.Object.create({warnMessage: ''})
          ],
          generalWarningMessages: ['some message'],
          e: true
        }
      ]).forEach(function (test, i) {
        it('test #' + i.toString(), function () {
          c.setProperties({
            servicesMasters: test.servicesMasters,
            generalWarningMessages: test.generalWarningMessages
          });
          expect(c.get('anyWarning')).to.equal(test.e);
        });
      });

  });

  describe('#clearRecommendations', function () {

    it('should clear content.recommendations', function () {

      c.set('content', {recommendations: {'s': {}}});
      c.clearRecommendations();
      expect(c.get('content.recommendations')).to.be.null;

    });

  });

  describe('#updateIsSubmitDisabled', function () {

    var clearCases = [
      {
        isHostNameValid: true,
        isInitialLayout: true,
        isInitialLayoutResulting: false,
        clearRecommendationsCallCount: 0,
        recommendAndValidateCallCount: 1,
        title: 'initial masters-hosts layout'
      },
      {
        isHostNameValid: true,
        isInitialLayout: false,
        isInitialLayoutResulting: false,
        clearRecommendationsCallCount: 1,
        recommendAndValidateCallCount: 1,
        title: 'master-hosts layout changed'
      },
      {
        isHostNameValid: false,
        isInitialLayout: false,
        isInitialLayoutResulting: false,
        clearRecommendationsCallCount: 0,
        recommendAndValidateCallCount: 0,
        title: 'invalid host name specified'
      }
    ];

    beforeEach(function () {
      c.set('selectedServicesMasters', [
        {isInstalled: false}
      ]);
      sinon.stub(c, 'clearRecommendations', Em.K);
      sinon.stub(c, 'recommendAndValidate', Em.K);
    });

    afterEach(function () {
      c.clearRecommendations.restore();
      c.recommendAndValidate.restore();
    });

    it('shouldn\'t change submitDisabled if thereIsNoMasters returns false', function () {

      c.set('selectedServicesMasters', [
        {isInstalled: true}
      ]);
      c.set('submitDisabled', false);
      c.updateIsSubmitDisabled();
      expect(c.get('submitDisabled')).to.equal(false);

    });

    it('should check servicesMasters.@each.isHostNameValid if useServerValidation is false', function () {

      c.set('useServerValidation', false);
      c.set('servicesMasters', [
        {isHostNameValid: false},
        {isHostNameValid: true}
      ]);
      c.updateIsSubmitDisabled();
      expect(c.get('submitDisabled')).to.equal(true);

      c.set('servicesMasters', [
        {isHostNameValid: true},
        {isHostNameValid: true}
      ]);
      c.updateIsSubmitDisabled();
      expect(c.get('submitDisabled')).to.equal(false);

    });

    clearCases.forEach(function (item) {
      it(item.title, function () {
        c.setProperties({
          isInitialLayout: item.isInitialLayout,
          servicesMasters: [{
            isHostNameValid: item.isHostNameValid
          }]
        });
        expect(c.get('isInitialLayout')).to.equal(item.isInitialLayoutResulting);
        expect(c.clearRecommendations.callCount).to.equal(item.clearRecommendationsCallCount);
        expect(c.recommendAndValidate.callCount).to.equal(item.recommendAndValidateCallCount);
      });
    });

  });

  describe('#isHostNameValid', function () {

    beforeEach(function () {
      c.setProperties({
        hosts: [
          {host_name: 'h1', isInstalled: false},
          {host_name: 'h2', isInstalled: true, maintenance_state: 'ON'},
          {host_name: 'h3', isInstalled: true, maintenance_state: 'OFF'}
        ],
        selectedServicesMasters: [
          {component_name: 'c1', selectedHost: 'h1'},
          {component_name: 'c2', selectedHost: 'h2'},
          {component_name: 'c3', selectedHost: 'h3'},
          {component_name: 'c3', selectedHost: 'h1'}
        ]
      });
    });

    Em.A([
      {
        componentName: 'c1',
        selectedHost: '   ',
        m: 'empty hostName is invalid',
        e: false
      },
      {
        componentName: 'c1',
        selectedHost: 'h4',
        m: 'hostName not exists',
        e: false
      },
      {
        componentName: 'c1',
        selectedHost: 'h1',
        m: 'not installed host',
        e: true
      },
      {
        componentName: 'c1',
        selectedHost: 'h2',
        m: 'installed host with maintenance ON',
        e: false
      },
      {
        componentName: 'c1',
        selectedHost: 'h3',
        m: 'installed host with maintenance OFF',
        e: true
      },
      {
        componentName: 'c4',
        selectedHost: 'h3',
        m: 'component not exists on host',
        e: true
      }
    ]).forEach(function (test) {
      it(test.m, function () {
        expect(c.isHostNameValid(test.componentName, test.selectedHost)).to.equal(test.e);
      });
    });

  });

  describe('#createComponentInstallationObject', function () {

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    Em.A([
        {
          fullComponent: Em.Object.create({
            componentName: 'c1',
            serviceName: 's1'
          }),
          hostName: 'h1',
          mastersToMove: ['c1'],
          savedComponent: {
            hostName: 'h2',
            isInstalled: true
          },
          stackServiceComponents: [Em.Object.create({componentName: 'c1', isCoHostedComponent: true})],
          e: {
            component_name: 'c1',
            display_name: 'C1',
            serviceId: 's1',
            selectedHost: 'h2',
            isInstalled: true,
            isServiceCoHost: false
          }
        },
        {
          fullComponent: Em.Object.create({
            componentName: 'c1',
            serviceName: 's1'
          }),
          hostName: 'h1',
          mastersToMove: [],
          stackServiceComponents: [Em.Object.create({componentName: 'c1', isCoHostedComponent: false})],
          e: {
            component_name: 'c1',
            display_name: 'C1',
            serviceId: 's1',
            selectedHost: 'h1',
            isInstalled: false,
            isServiceCoHost: false
          }
        },
        {
          fullComponent: Em.Object.create({
            componentName: 'c1',
            serviceName: 's1'
          }),
          hostName: 'h1',
          mastersToMove: [],
          stackServiceComponents: [Em.Object.create({componentName: 'c1', isCoHostedComponent: true})],
          e: {
            component_name: 'c1',
            display_name: 'C1',
            serviceId: 's1',
            selectedHost: 'h1',
            isInstalled: false,
            isServiceCoHost: true
          }
        }
      ]).forEach(function (test, i) {
        describe('test #' + i, function () {

          beforeEach(function () {
            sinon.stub(App.StackServiceComponent, 'find', function () {
              return test.stackServiceComponents;
            });
            c.set('mastersToMove', test.mastersToMove);
            c.set('content', {controllerName: test.controllerName});
          });

          it('component-object is valid', function () {
            expect(c.createComponentInstallationObject(test.fullComponent, test.hostName, test.savedComponent)).to.eql(test.e);
          });

        });
      });

  });

  describe('#createComponentInstallationObjects', function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return [
          Em.Object.create({isShownOnAddServiceAssignMasterPage: true, componentName: 'c1', serviceName: 's1'}),
          Em.Object.create({isShownOnAddServiceAssignMasterPage: true, componentName: 'c2', serviceName: 's2'}),
          Em.Object.create({isShownOnAddServiceAssignMasterPage: true, componentName: 'c4', serviceName: 's2'}),
          Em.Object.create({isShownOnInstallerAssignMasterPage: true, componentName: 'c1', serviceName: 's1'}),
          Em.Object.create({isShownOnInstallerAssignMasterPage: true, componentName: 'c2', serviceName: 's2'}),
          Em.Object.create({isShownOnInstallerAssignMasterPage: true, componentName: 'c4', serviceName: 's2'})
        ];
      });

      var recommendations = {
        "blueprint": {
          "host_groups": [
            {
              "name": "host-group-1",
              "components": [ {"name": "c1"}, {"name": "c2"} ]
            },
            {
              "name": "host-group-2",
              "components": [ {"name": "c1"}, {"name": "c2"} ]
            },
            {
              "name": "host-group-3",
              "components": [ {"name": "c1"} ]
            }
          ]
        },
        "blueprint_cluster_binding": {
          "host_groups": [
            {
              "name": "host-group-1",
              "hosts": [ {"fqdn": "h1"} ]
            },
            {
              "name": "host-group-2",
              "hosts": [ {"fqdn": "h2"} ]
            },
            {
              "name": "host-group-3",
              "hosts": [ {"fqdn": "h3"} ]
            }
          ]
        }
      };

      c.set('recommendations', recommendations);
      c.set('content', {
        masterComponentHosts: [],
        services: [
          {serviceName: 's1', isSelected: true, isInstalled: false},
          {serviceName: 's2', isSelected: true, isInstalled: false}
        ],
        recommendations: recommendations
      });

    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it('simple map without nothing stored/saved etc', function() {
      var r = c.createComponentInstallationObjects();
      expect(r.mapProperty('component_name')).to.eql(['c1', 'c2', 'c1', 'c2', 'c1']);
      expect(r.mapProperty('serviceId')).to.eql(['s1', 's2', 's1', 's2', 's1']);
      expect(r.mapProperty('selectedHost')).to.eql(['h1', 'h1', 'h2', 'h2', 'h3']);
    });

    describe('some saved components exist', function() {

      beforeEach(function () {
        c.set('content.controllerName', 'addServiceController');
        c.get('multipleComponents').push('c4');
        c.set('content.masterComponentHosts', [
          {hostName: 'h3', component: 'c4'}
        ]);
        c.get('content.recommendations.blueprint.host_groups')[2].components.push({name: 'c4'});
      });

      it('data contains save components', function () {
        var r = c.createComponentInstallationObjects();
        expect(r.mapProperty('component_name')).to.eql(['c1', 'c2', 'c1', 'c2', 'c1', 'c4']);
        expect(r.mapProperty('serviceId')).to.eql(['s1', 's2', 's1', 's2', 's1', 's2']);
        expect(r.mapProperty('selectedHost')).to.eql(['h1', 'h1', 'h2', 'h2', 'h3', 'h3']);
      });

    });

  });

  describe('#updateValidationsSuccessCallback', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find', function() {
        return [];
      });
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    describe('should map messages to generalErrorMessages, generalWarningMessages', function() {

      var data = [
          {
            type: 'host-component',
            'component-name': 'c1',
            host: 'h1',
            level: 'ERROR',
            message: 'm1'
          },
          {
            type: 'host-component',
            'component-name': 'c2',
            host: 'h2',
            level: 'WARN',
            message: 'm2'
          },
          {
            type: 'host-component',
            'component-name': 'c3',
            host: 'h3',
            level: 'ERROR',
            message: 'm3'
          },
          {
            type: 'host-component',
            'component-name': 'c4',
            host: 'h4',
            level: 'WARN',
            message: 'm4'
          }
        ],
        servicesMasters = [
          Em.Object.create({selectedHost: 'h1', component_name: 'c1'}),
          Em.Object.create({selectedHost: 'h2', component_name: 'c2'})
        ];

      beforeEach(function () {
        c.set('servicesMasters', servicesMasters);
        c.updateValidationsSuccessCallback({resources: [{items: data}]});
      });

      it('submitDisabled is false', function () {
        expect(c.get('submitDisabled')).to.equal(false);
      });

      it('errorMessage for c1 is `m1`', function () {
        expect(c.get('servicesMasters').findProperty('component_name', 'c1').get('errorMessage')).to.equal('m1');
      });

      it('errorMessage for c2 is `m2`', function () {
        expect(c.get('servicesMasters').findProperty('component_name', 'c2').get('warnMessage')).to.equal('m2');
      });

      it('no general errors', function () {
        expect(c.get('generalErrorMessages')).to.be.empty;
      });

      it('no general warnings', function () {
        expect(c.get('generalWarningMessages')).to.be.empty;
      });

    });

  });

});
