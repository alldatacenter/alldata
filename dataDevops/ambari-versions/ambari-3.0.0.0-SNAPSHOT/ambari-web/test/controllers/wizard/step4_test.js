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

var Ember = require('ember');
var App = require('app');
var modelSetup = require('test/init_model_test');

require('controllers/wizard/step4_controller');
describe('App.WizardStep4Controller', function () {

  var services = [
    'HDFS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SCOOP', 'ZOOKEEPER', 'SMARTSENSE', 'LOGSEARCH',
    'YARN', 'MAPREDUCE2', 'FALCON', 'TEZ', 'STORM', 'AMBARI_METRICS', 'RANGER', 'SPARK', 'SLIDER', 'ATLAS', 'AMBARI_INFRA_SOLR'
  ];
  var controller;

  beforeEach(function() {
    controller = App.WizardStep4Controller.create();
    services.forEach(function(serviceName) {
      controller.pushObject(App.StackService.createRecord({
        'serviceName':serviceName, 'isSelected': true, 'isHiddenOnSelectServicePage': false, 'isInstalled': false, 'isDisabled': 'HDFS' === serviceName, isDFS: 'HDFS' === serviceName
      }));
    });
  });

  var generateSelectedServicesContent = function(selectedServiceNames) {
    var allServices = services.slice(0);
    modelSetup.setupStackServiceComponent();
    if (selectedServiceNames.contains('GLUSTERFS')) allServices.push('GLUSTERFS');
    allServices = allServices.map(function(serviceName) {
      return [App.StackService.createRecord({
        'serviceName': serviceName,
        'isSelected': false,
        'canBeSelected': true,
        'isInstalled': false,
        isPrimaryDFS: serviceName === 'HDFS',
        isDFS: ['HDFS','GLUSTERFS'].contains(serviceName),
        isMonitoringService: ['GANGLIA'].contains(serviceName),
        requiredServices: App.StackService.find(serviceName).get('requiredServices') || [],
        displayNameOnSelectServicePage: App.format.role(serviceName, true),
        coSelectedServices: function() {
          return App.StackService.coSelected[this.get('serviceName')] || [];
        }.property('serviceName')
      })];
    }).reduce(function(current, prev) { return current.concat(prev); });

    selectedServiceNames.forEach(function(serviceName) {
      allServices.findProperty('serviceName', serviceName).set('isSelected', true);
    });

    return allServices;
  };

  describe('#isSubmitDisabled', function () {
    it('should return false if at least one selected service is not installed', function () {
      expect(controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should return true if all selected services are already installed', function () {
      controller.setEach('isInstalled', true);
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.get('isSubmitDisabled')).to.equal(true);
    });
  });

  describe('#isAllChecked', function () {
    it('should return true if all non DFS services are selected', function () {
      controller.setEach('isInstalled', false);
      controller.findProperty('serviceName', 'YARN').set('isSelected', true);
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.get('isAllChecked')).to.equal(true);
    });

    it('should return false if at least one service is not selected', function () {
      controller.findProperty('serviceName', 'YARN').set('isSelected', false);
      expect(controller.get('isAllChecked')).to.equal(false);
    });
  });

  describe('#fileSystems', function () {
    beforeEach(function () {
      controller.clear();
      controller.set('content', generateSelectedServicesContent(['HDFS', 'GLUSTERFS', 'YARN']));
    });

    it('returns only DFS services', function () {
      expect(controller.get('fileSystems')).to.have.length(2);
      expect(controller.get('fileSystems').mapProperty('serviceName')).to.contain('GLUSTERFS');
      expect(controller.get('fileSystems').mapProperty('serviceName')).to.contain('HDFS');
    });

    it('allows selecting only one DFS at a time', function () {
      var fileSystems = controller.get('fileSystems');
      fileSystems[0].set('isSelected', true);
      expect(fileSystems[0].get('isSelected')).to.equal(true);
      expect(fileSystems[1].get('isSelected')).to.equal(false);
      fileSystems[1].set('isSelected', true);
      expect(fileSystems[0].get('isSelected')).to.equal(false);
      expect(fileSystems[1].get('isSelected')).to.equal(true);
    });
  });
  describe('#multipleDFSs()', function () {
    it('should return true if HDFS is selected and GLUSTERFS is selected', function () {
      controller.set('content', generateSelectedServicesContent(['HDFS', 'GLUSTERFS']));
      expect(controller.multipleDFSs()).to.equal(true);
    });
    it('should return false if HDFS is not selected and GLUSTERFS is selected', function () {
      controller.set('content', generateSelectedServicesContent(['GLUSTERFS']));
      expect(controller.multipleDFSs()).to.equal(false);
    });
    it('should return false if HDFS is selected and GLUSTERFS is not selected', function () {
      controller.set('content', generateSelectedServicesContent(['HDFS']));
      expect(controller.multipleDFSs()).to.equal(false);
    });
  });

  describe('#setGroupedServices()', function () {
    var testCases = [
      {
        title: 'should set MapReduce2 isSelected to true when YARN is selected',
        condition: {
          'YARN': true,
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': true
        }
      },
      {
        title: 'should set MapReduce2 isSelected to false when YARN is not selected',
        condition: {
          'YARN': false,
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': false
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to true when YARN is selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'YARN': true,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': true
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to false when YARN is not selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'YARN': false,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': false
        }
      }
    ];

    testCases.forEach(function(testCase){
      describe(testCase.title, function () {

        beforeEach(function () {
          controller.clear();
          Object.keys(testCase.condition).forEach(function (id) {
            controller.pushObject(Em.Object.create({
              serviceName: id,
              isSelected: testCase.condition[id],
              canBeSelected: true,
              isInstalled: false,
              coSelectedServices: function() {
                var coSelected = {
                  'YARN': ['MAPREDUCE2']
                };
                return coSelected[this.get('serviceName')] || [];
              }.property('serviceName')
            }));
          });
          controller.setGroupedServices();
        });

        Object.keys(testCase.result).forEach(function (service) {
          it(service, function () {
            expect(controller.findProperty('serviceName', service).get('isSelected')).to.equal(testCase.result[service]);
          });
        });
      });
    }, this);
  });

  describe('#addValidationError()', function() {
    var tests = [
      {
        errorObjects: [
          {
            id: 'serviceCheck_ZOOKEEPER',
            shouldBeAdded: true
          },
          {
            id: 'serviceCheck_YARN',
            shouldBeAdded: true
          }
        ],
        expectedIds: ['serviceCheck_ZOOKEEPER', 'serviceCheck_YARN']
      },
      {
        errorObjects: [
          {
            id: 'fsCheck',
            shouldBeAdded: true
          },
          {
            id: 'fsCheck',
            shouldBeAdded: false
          }
        ],
        expectedIds: ['fsCheck']
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    tests.forEach(function(test) {
      var message = 'Erorrs {0} thrown. errorStack property should contains ids: {1}'
        .format(test.errorObjects.mapProperty('id').join(', '), test.expectedIds.join(', '));
      describe(message, function() {

        beforeEach(function () {
          this.added = [];
          test.errorObjects.forEach(function(errorObject) {
            this.added.push(controller.addValidationError(errorObject));
          }, this);
        });

        it('shouldBeAdded', function() {
          expect(this.added).to.be.eql(test.errorObjects.mapProperty('shouldBeAdded'));
        });

        it('expectedIds', function() {
          expect(controller.get('errorStack').mapProperty('id')).to.eql(test.expectedIds);
        });
      });
    })
  });

  describe('#validate()', function() {
    var tests = [
        {
          services: ['HDFS', 'ZOOKEEPER'],
          errorsExpected: ['ambariMetricsCheck', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['ZOOKEEPER'],
          errorsExpected: ['ambariMetricsCheck', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS'],
          errorsExpected: ['serviceCheck_ZOOKEEPER', 'ambariMetricsCheck', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'TEZ', 'ZOOKEEPER'],
          errorsExpected: ['serviceCheck_YARN', 'ambariMetricsCheck' , 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'FALCON'],
          errorsExpected: ['serviceCheck_OOZIE', 'ambariMetricsCheck' , 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'GANGLIA', 'HIVE'],
          errorsExpected: ['serviceCheck_YARN', 'ambariMetricsCheck' , 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
          errorsExpected: ['serviceCheck_YARN', 'multipleDFS', 'ambariMetricsCheck', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'GANGLIA'],
          errorsExpected: ['ambariMetricsCheck', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: ['smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: ['smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_ZOOKEEPER', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'TEZ', 'ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'FALCON', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_OOZIE', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'GANGLIA', 'HIVE', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN', 'multipleDFS', 'smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'GANGLIA', 'AMBARI_METRICS'],
          errorsExpected: ['smartSenseCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['RANGER'],
          errorsExpected: ['ambariMetricsCheck', 'smartSenseCheck', 'atlasCheck', 'ambariRangerInfraCheck']
        },
        {
          services: ['SMARTSENSE'],
          errorsExpected: ['ambariMetricsCheck', 'rangerCheck', 'atlasCheck']
        },
        {
          services: ['ATLAS', 'AMBARI_METRICS', 'SMARTSENSE'],
          errorsExpected: ['rangerCheck', 'ambariAtlasInfraCheck', 'ambariAtlasHbaseCheck']
        },
        {
          services: ['LOGSEARCH', 'AMBARI_METRICS', 'SMARTSENSE'],
          errorsExpected: ['rangerCheck', 'atlasCheck', 'ambariLogsearchCheck']
        },
        {
          services: ['ATLAS', 'AMBARI_METRICS', 'SMARTSENSE', 'RANGER'],
          errorsExpected: ['ambariRangerInfraCheck', 'ambariAtlasInfraCheck', 'ambariAtlasHbaseCheck']
        },
      ],
      controllerNames = ['installerController', 'addServiceController'],
      wizardNames = {
        installerController: 'Install Wizard',
        addServiceController: 'Add Service Wizard'
      },
      sparkCases = [
        {
          currentStackName: 'HDP',
          currentStackVersionNumber: '2.2',
          sparkWarningExpected: true,
          title: 'HDP 2.2'
        },
        {
          currentStackName: 'HDP',
          currentStackVersionNumber: '2.3',
          sparkWarningExpected: false,
          title: 'HDP 2.3'
        },
        {
          currentStackName: 'BIGTOP',
          currentStackVersionNumber: '0.8',
          sparkWarningExpected: false,
          title: 'Non-HDP stack'
        }
      ];

    beforeEach(function () {
      controller.clear();
    });

    controllerNames.forEach(function (name) {
      tests.forEach(function(test) {
        var errorsExpected = test.errorsExpected;
        if (name !== 'installerController') {
          errorsExpected = test.errorsExpected.without('ambariMetricsCheck').without('smartSenseCheck').without('rangerCheck').without('atlasCheck');
        }
        var message = '{0}, {1} selected validation should be {2}, errors: {3}'
          .format(wizardNames[name], test.services.join(','), errorsExpected.length ? 'passed' : 'failed',
            errorsExpected.length ? errorsExpected.join(',') : 'absent');
        it(message, function() {
          controller.setProperties({
            content: generateSelectedServicesContent(test.services),
            errorStack: [],
            wizardController: Em.Object.create({
              name: name
            })
          });
          controller.validate();
          expect(controller.get('errorStack').mapProperty('id')).to.eql(errorsExpected.toArray());
        });
      })
    });

    sparkCases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(App, 'get').withArgs('currentStackName').returns(item.currentStackName).
            withArgs('currentStackVersionNumber').returns(item.currentStackVersionNumber);
          controller.set('errorStack', []);
          controller.set('content', generateSelectedServicesContent(['SPARK']));
          controller.validate();
        });

        afterEach(function () {
          App.get.restore();
        });

        it('sparkWarning ' + (item.sparkWarningExpected ? 'exists' : 'not exists'), function () {
          expect(controller.get('errorStack').someProperty('id', 'sparkWarning')).to.equal(item.sparkWarningExpected);
        });
      });
    });

  });

  describe('#onPrimaryPopupCallback()', function() {
    var c;
    var tests = [
      {
        services: ['HDFS','ZOOKEEPER'],
        confirmPopupCount: 0,
        errorsExpected: []
      },
      {
        services: ['ZOOKEEPER'],
        confirmPopupCount: 0,
        errorsExpected: []
      },
      {
        services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
        confirmPopupCount: 2,
        errorsExpected: ['serviceCheck_YARN', 'serviceCheck_TEZ', 'multipleDFS']
      },
      {
        services: ['HDFS','ZOOKEEPER', 'GANGLIA'],
        confirmPopupCount: 0,
        errorsExpected: []
      }
    ];

    beforeEach(function() {
      c = App.WizardStep4Controller.create({});
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(c, 'submit', Em.K);
      sinon.spy(c, 'onPrimaryPopupCallback');
    });

    afterEach(function() {
      App.router.send.restore();
      c.submit.restore();
      c.onPrimaryPopupCallback.restore();
    });


    tests.forEach(function(test) {
      var message = 'Selected services: {0}. {1} errors should be confirmed'
        .format(test.services.join(', '), test.confirmPopupCount);

      describe(message, function() {
        var runValidations = function() {
          c.serviceDependencyValidation();
          c.fileSystemServiceValidation();
        };

        beforeEach(function () {
          c.set('content', generateSelectedServicesContent(test.services));
          runValidations();
        });

        it('errors count validation', function () {
          expect(c.get('errorStack.length')).to.equal(test.confirmPopupCount);
        });

        if (test.errorsExpected) {
          describe('if errors detected than it should be shown', function () {
            var currentErrorObject;
            beforeEach(function () {
              currentErrorObject = c.get('errorStack').findProperty('isShown', false);
            });
            test.errorsExpected.forEach(function(error) {
              it(error, function () {
                // validate current error
                if (currentErrorObject) {
                  expect(test.errorsExpected).to.contain(currentErrorObject.id);
                  // show current error
                  var popup = c.showError(currentErrorObject);
                  // submit popup
                  popup.onPrimary();
                  // onPrimaryPopupCallback should be called
                  expect(c.onPrimaryPopupCallback.called).to.equal(true);
                  // submit called
                  expect(c.submit.called).to.equal(true);
                  if (c.get('errorStack').length) {
                    // current error isShown flag changed to true
                    expect(currentErrorObject.isShown).to.equal(true);
                  }
                  runValidations();
                }
              });
            });
          });
        }
      });
    });

  });

  describe('#needToAddServicePopup', function() {

    beforeEach(function () {
      sinon.stub(controller, 'submit', Em.K);
    });

    afterEach(function () {
      controller.submit.restore();
    });

    Em.A([
        {
          m: 'one service',
          services: {selected: true, serviceName: 's1'},
          content: [Em.Object.create({serviceName: 's1', isSelected: false})],
          e: [true]
        },
        {
          m: 'many services',
          services: [{selected: true, serviceName: 's1'}, {selected: false, serviceName: 's2'}],
          content: [Em.Object.create({serviceName: 's1', isSelected: false}),
            Em.Object.create({serviceName: 's2', isSelected: true})],
          e: [true, false]
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          controller.set('content', test.content);
          controller.needToAddServicePopup(test.services, '').onPrimary();
          expect(controller.submit.calledOnce).to.equal(true);
          expect(controller.mapProperty('isSelected')).to.eql(test.e);
        });
      });
  });

  describe('#submit', function() {
    var c;
    var tests = [
      {
        isSubmitDisabled: true,
        validate: false,
        userCanProceed: false
      },
      {
        isSubmitDisabled: false,
        validate: false,
        userCanProceed: false
      },
      {
        isSubmitDisabled: false,
        validate: true,
        userCanProceed: true
      }
    ];

    beforeEach(function() {
      c = App.WizardStep4Controller.create();
      sinon.stub(App.router, 'send', Em.K);
    });

    afterEach(function() {
      App.router.send.restore();
    });

    tests.forEach(function(test) {
      var messageFormat = [
        test.isSubmitDisabled ? 'disabled' : 'enabled',
        test.validate ? 'success' : 'failed',
        test.userCanProceed ? '' : 'not'
      ];
      var message = String.prototype.format.apply('Submit btn: {0}. Validation: {1}. Can{2} move to the next step.', messageFormat);

      it(message, function() {
        c.reopen({
          isSubmitDisabled: test.isSubmitDisabled,
          validate: function() { return test.validate; }
        });
        c.clear();
        c.submit();

        expect(App.router.send.calledOnce).to.equal(test.userCanProceed);
      });

    })
  });

  describe('#dependencies', function() {
    var tests = [
      {
        services: ['HDFS'],
        dependencies: ['ZOOKEEPER']
      },
      {
        services: ['STORM'],
        dependencies: ['ZOOKEEPER']
      }
    ];
    tests.forEach(function(test) {
      var message = '{0} dependency should be {1}'.format(test.services.join(','), test.dependencies.join(','));
      it(message, function() {

        controller.clear();
        controller.set('content', generateSelectedServicesContent(test.services));

        var dependentServicesTest = [];

        test.services.forEach(function(serviceName) {
          var service = controller.filterProperty('serviceName', serviceName);
          service.forEach(function(item) {
            var dependencies = item.get('requiredServices');
            if(!!dependencies) {
              dependentServicesTest = dependentServicesTest.concat(dependencies);
            }
          });
        });

        expect(dependentServicesTest).to.be.eql(test.dependencies);
      });
    })
  });

  describe('#serviceDependencyValidation', function () {

    var cases = [
      {
        services: ['HBASE'],
        dependentServices: ['HDFS', 'ZOOKEEPER'],
        title: 'HBASE selected and HDFS not selected initially'
      },
      {
        services: ['TEZ', 'HDFS'],
        dependentServices: ['ZOOKEEPER', 'YARN'],
        title: 'TEZ selected and ZOOKEEPER not selected initially'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          controller.set('content', generateSelectedServicesContent(item.services));
          controller.serviceDependencyValidation();
        });

        it('check errors in the stack', function () {
          var ids = controller.get('errorStack').mapProperty('id');
          expect(ids.contains("serviceCheck_" + item.dependentServices[0])).to.be.true;
          expect(ids.contains("serviceCheck_" + item.dependentServices[1])).to.be.true;
        });

        it('simulate situation where user clicks cancel on error for first dependent service and then selects it in which case', function () {
          controller.findProperty('serviceName', item.dependentServices[0]).set('isSelected', true);
          //serviceDependencyValidation() will be called again
          controller.serviceDependencyValidation();
          //error for first dependent service must be removed from errorStack array
          var ids = controller.get('errorStack').mapProperty('id');
          expect(ids.contains("serviceCheck_" + item.dependentServices[0])).to.be.false;
          expect(ids.contains("serviceCheck_" + item.dependentServices[1])).to.be.true;
        });

      });
    });
  });

  describe('#serviceValidation', function () {

    var cases = [
      {
        services: ['HDFS'],
        isAmbariMetricsWarning: false,
        title: 'Ambari Metrics not available'
      },
      {
        services: ['AMBARI_METRICS'],
        isAmbariMetricsSelected: false,
        isAmbariMetricsWarning: true,
        title: 'Ambari Metrics not selected'
      },
      {
        services: ['AMBARI_METRICS'],
        isAmbariMetricsSelected: true,
        isAmbariMetricsWarning: false,
        title: 'Ambari Metrics selected'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('content', generateSelectedServicesContent(item.services));
        var ams = controller.findProperty('serviceName', 'AMBARI_METRICS');
        if (item.services.contains('AMBARI_METRICS')) {
          ams.set('isSelected', item.isAmbariMetricsSelected);
        } else {
          controller.removeObject(ams);
        }
        controller.serviceValidation(Em.K, 'AMBARI_METRICS', 'ambariMetricsCheck');
        expect(controller.get('errorStack').mapProperty('id').contains('ambariMetricsCheck')).to.equal(item.isAmbariMetricsWarning);
      });
    });

  });

  describe('#sparkValidation', function () {

    var cases = [
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark not available'
      },
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark not available'
      },
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark not available'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: true,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark installed'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark installed'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark installed'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.get.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {
        beforeEach(function () {
          this.stub.withArgs('currentStackName').returns(item.currentStackName).
            withArgs('currentStackVersionNumber').returns(item.currentStackVersionNumber);
          controller.set('content', generateSelectedServicesContent(item.services));
          var spark = controller.findProperty('serviceName', 'SPARK');
          if (item.services.contains('SPARK')) {
            spark.setProperties({
              isSelected: item.isSparkSelected,
              isInstalled: item.isSparkInstalled
            });
          } else {
            controller.removeObject(spark);
          }
          controller.sparkValidation();
        });

        it('sparkWarning is ' + item.sparkWarning, function () {
          expect(controller.get('errorStack').mapProperty('id').contains('sparkWarning')).to.equal(item.isSparkWarning);
        });
      });
    });

  });

  describe('#clearErrors', function () {

    var cases = [
      {
        errorStack: [{
          isAccepted: false
        }],
        resultingErrorStack: [{
          isAccepted: false
        }],
        title: 'error stack shouldn\'t be cleared during validation'
      },
      {
        errorStack: [{
          isAccepted: true
        }],
        resultingErrorStack: [],
        title: 'error stack should be cleared'
      }
    ];

    beforeEach(function () {
      controller.set('errorStack', [{}]);
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('errorStack', item.errorStack);
        controller.propertyDidChange('@each.isSelected');
        expect(controller.get('errorStack')).to.eql(item.resultingErrorStack);
      });
    });

  });

  describe('Service warnings popup', function () {

    var target = {
      clb: Em.K
    };
    var id = 1;

    beforeEach(function () {
      sinon.spy(target, 'clb');
      sinon.stub(controller, 'onPrimaryPopupCallback', Em.K);
    });

    afterEach(function () {
      target.clb.restore();
      controller.onPrimaryPopupCallback.restore();
    });

    Em.A([
      'serviceCheckPopup',
      'sparkWarningPopup'
    ]).forEach(function (methodName) {

      describe('#' + methodName, function () {

        beforeEach(function () {
          this.popup = controller[methodName](target.clb, id);
        });

        it('#onPrimary', function () {
          this.popup.onPrimary();
          expect(controller.onPrimaryPopupCallback.calledWith(target.clb)).to.be.true;
        });

        it('#onSecondary', function () {
          this.popup.onSecondary();
          expect(target.clb.calledWith(id)).to.be.true;
        });

        it('#onClose', function () {
          this.popup.onClose();
          expect(target.clb.calledWith(id)).to.be.true;
        });
      });
    });
  });

  describe('#dependentServiceValidation', function() {

    beforeEach(function() {
      sinon.stub(controller, 'serviceValidation');
    });

    afterEach(function() {
      controller.serviceValidation.restore();
      controller.clear();
    });

    it('serviceValidation should not be called when selected service does not exist', function() {
      controller.dependentServiceValidation('S1', 'S2', 'check', Em.K);
      expect(controller.serviceValidation.called).to.be.false;
    });

    it('serviceValidation should not be called when service not selected', function() {
      controller.pushObject(App.StackService.createRecord({
        serviceName: 'S1',
        isSelected: false
      }));
      controller.dependentServiceValidation('S1', 'S2', 'check', Em.K);
      expect(controller.serviceValidation.called).to.be.false;
    });

    it('serviceValidation should not be called when dependent service does not exist', function() {
      controller.pushObjects([
        App.StackService.createRecord({
          serviceName: 'S1',
          isSelected: true
        })
      ]);
      controller.dependentServiceValidation('S1', 'S2', 'check', Em.K);
      expect(controller.serviceValidation.called).to.be.false;
    });

    it('serviceValidation should not be called when dependent service is selected', function() {
      controller.pushObjects([
        App.StackService.createRecord({
          serviceName: 'S1',
          isSelected: true
        }),
        Em.Object.create({
          serviceName: 'S2',
          isSelected: true
        })
      ]);
      controller.dependentServiceValidation('S1', 'S2', 'check', Em.K);
      expect(controller.serviceValidation.called).to.be.false;
    });

    it('serviceValidation should be called when dependent service is not selected', function() {
      controller.pushObjects([
        App.StackService.createRecord({
          serviceName: 'S1',
          isSelected: true
        }),
        Em.Object.create({
          serviceName: 'S2',
          isSelected: false
        })
      ]);
      controller.dependentServiceValidation('S1', 'S2', 'check', Em.K);
      expect(controller.serviceValidation.calledOnce).to.be.true;
    });
  });

});
