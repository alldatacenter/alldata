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
var controller;

function getController() {
  return App.MainAlertDefinitionConfigsController.create({
    allServices: ['service1', 'service2', 'service3'],
    allComponents: ['component1', 'component2', 'component3'],
    aggregateAlertNames: ['alertDefinitionName', 'alertDefinitionName2', 'alertDefinitionName3']
  });
}

function getEmptyArray() {
  return [];
}

describe('App.MainAlertDefinitionConfigsController', function () {

  beforeEach(function () {
    controller = getController();
  });

  App.TestAliases.testAsComputedOr(getController(), 'hasErrors', ['someConfigIsInvalid', 'hasThresholdsError']);

  describe('#renderConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({}));
      sinon.stub(controller, 'renderPortConfigs', getEmptyArray);
      sinon.stub(controller, 'renderMetricConfigs', getEmptyArray);
      sinon.stub(controller, 'renderWebConfigs', getEmptyArray);
      sinon.stub(controller, 'renderScriptConfigs', getEmptyArray);
      sinon.stub(controller, 'renderAggregateConfigs', getEmptyArray);
    });

    afterEach(function () {
      controller.renderPortConfigs.restore();
      controller.renderMetricConfigs.restore();
      controller.renderWebConfigs.restore();
      controller.renderScriptConfigs.restore();
      controller.renderAggregateConfigs.restore();
    });

    it('should call renderPortConfigs method', function () {
      controller.set('alertDefinitionType', 'PORT');
      controller.renderConfigs();
      expect(controller.renderPortConfigs.calledOnce).to.be.true;
    });

    it('should call renderMetricConfigs method', function () {
      controller.set('alertDefinitionType', 'METRIC');
      controller.renderConfigs();
      expect(controller.renderMetricConfigs.calledOnce).to.be.true;
    });

    it('should call renderWebConfigs method', function () {
      controller.set('alertDefinitionType', 'WEB');
      controller.renderConfigs();
      expect(controller.renderWebConfigs.calledOnce).to.be.true;
    });

    it('should call renderScriptConfigs method', function () {
      controller.set('alertDefinitionType', 'SCRIPT');
      controller.renderConfigs();
      expect(controller.renderScriptConfigs.calledOnce).to.be.true;
    });

    it('should call renderAggregateConfigs method', function () {
      controller.set('alertDefinitionType', 'AGGREGATE');
      controller.renderConfigs();
      expect(controller.renderAggregateConfigs.calledOnce).to.be.true;
    });

  });

  describe('#renderPortConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: 'alertDefinitionUri',
        defaultPort: '777'
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderPortConfigs();
      expect(result.length).to.equal(7);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderPortConfigs();
      expect(result.length).to.equal(5);
    });

  });

  describe('#renderMetricConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: {
          "http": "{{mapred-site/mapreduce.jobhistory.webapp.address}}",
          "https": "{{mapred-site/mapreduce.jobhistory.webapp.https.address}}",
          "https_property": "{{mapred-site/mapreduce.jobhistory.http.policy}}",
          "https_property_value": "HTTPS_ONLY",
          "default_port": 0.0,
          "connection_timeout": 123
        },
        jmx: {
          propertyList: ['property1', 'property2'],
          value: 'jmxValue'
        },
        ganglia: {
          propertyList: null,
          value: null
        }
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderMetricConfigs();
      expect(result.length).to.equal(10);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderMetricConfigs();
      expect(result.length).to.equal(6);
    });

  });

  describe('#renderWebConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        uri: {
          "http": "{{mapred-site/mapreduce.jobhistory.webapp.address}}",
          "https": "{{mapred-site/mapreduce.jobhistory.webapp.https.address}}",
          "https_property": "{{mapred-site/mapreduce.jobhistory.http.policy}}",
          "https_property_value": "HTTPS_ONLY",
          "default_port": 0.0,
          "connection_timeout": 123
        }
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderWebConfigs();
      expect(result.length).to.equal(10);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderWebConfigs();
      expect(result.length).to.equal(6);
    });

  });

  describe('#renderScriptConfigs()', function () {

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        service: {displayName: 'alertDefinitionService'},
        componentName: 'component1',
        scope: 'HOST',
        description: 'alertDefinitionDescription',
        interval: 60,
        parameters: [
          Em.Object.create({}),
          Em.Object.create({}),
        ],
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ],
        location: 'path to script'
      }));
    });

    it('isWizard = true', function () {
      controller.set('isWizard', true);
      var result = controller.renderScriptConfigs();
      expect(result.length).to.equal(8);
    });

    it('isWizard = false', function () {
      controller.set('isWizard', false);
      var result = controller.renderScriptConfigs();
      expect(result.length).to.equal(4);
    });

  });

  describe('#renderAggregateConfigs()', function () {

    it('should render array of configs with correct values', function () {

      controller.set('content', Em.Object.create({
        name: 'alertDefinitionName',
        description: 'alertDefinitionDescription',
        interval: 60,
        reporting: [
          Em.Object.create({
            type: 'warning',
            value: 10
          }),
          Em.Object.create({
            type: 'critical',
            value: 20
          }),
          Em.Object.create({
            type: 'ok',
            value: 30
          })
        ]
      }));

      var result = controller.renderAggregateConfigs();

      expect(result.length).to.equal(5);
    });

  });

  describe('#editConfigs()', function () {

    beforeEach(function () {
      controller.set('configs', [
        Em.Object.create({value: 'value1', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value2', previousValue: '', isDisabled: true}),
        Em.Object.create({value: 'value3', previousValue: '', isDisabled: true})
      ]);
      controller.set('canEdit', false);
      controller.editConfigs();
    });

    it('should set previousValue', function () {
      expect(controller.get('configs').mapProperty('previousValue')).to.eql(['value1', 'value2', 'value3']);
    });
    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', true)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.true;
    });

  });

  describe('#cancelEditConfigs()', function () {

    beforeEach(function () {
      controller.set('configs', [
        Em.Object.create({value: '', previousValue: 'value1', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value2', isDisabled: false}),
        Em.Object.create({value: '', previousValue: 'value3', isDisabled: false})
      ]);
      controller.set('canEdit', true);
      controller.cancelEditConfigs();
    });

    it('should set previousValue', function () {
      expect(controller.get('configs').mapProperty('value')).to.eql(['value1', 'value2', 'value3']);
    });
    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.false;
    });

  });

  describe('#saveConfigs()', function () {

    beforeEach(function () {
      controller.set('configs', [
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true}),
        Em.Object.create({isDisabled: true})
      ]);
      controller.set('canEdit', true);
      controller.saveConfigs();
    });

    it('should set isDisabled for each config', function () {
      expect(controller.get('configs').someProperty('isDisabled', false)).to.be.false;
    });
    it('should change canEdit flag', function () {
      expect(controller.get('canEdit')).to.be.false;
    });
    it('should sent 1 request', function () {
      var args = testHelpers.findAjaxRequest('name', 'alerts.update_alert_definition');
      expect(args[0]).to.exists;
    });

  });

  describe('#getPropertiesToUpdate()', function () {

    beforeEach(function () {
      controller.set('content', {
        rawSourceData: {
          path1: 'value',
          path2: {
            path3: 'value'
          }
        }
      });
    });

    var testCases = [
      {
        m: 'should ignore configs with wasChanged false',
        configs: [
          Em.Object.create({
            wasChanged: false,
            apiProperty: 'name1',
            apiFormattedValue: 'test1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'name2',
            apiFormattedValue: 'test2'
          }),
          Em.Object.create({
            wasChanged: false,
            apiProperty: 'name3',
            apiFormattedValue: 'test3'
          })
        ],
        result: {
          'AlertDefinition/name2': 'test2'
        }
      },
      {
        m: 'should correctly map deep source properties',
        configs: [
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'name1',
            apiFormattedValue: 'test1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'source.path1',
            apiFormattedValue: 'value1'
          }),
          Em.Object.create({
            wasChanged: true,
            apiProperty: 'source.path2.path3',
            apiFormattedValue: 'value2'
          })
        ],
        result: {
          'AlertDefinition/name1': 'test1',
          'AlertDefinition/source': {
            path1: 'value1',
            path2: {
              path3: 'value2'
            }
          }
        }
      },
      {
        m: 'should correctly multiple apiProperties',
        configs: [
          Em.Object.create({
            wasChanged: true,
            apiProperty: ['name1', 'name2'],
            apiFormattedValue: ['value1', 'value2']
          })
        ],
        result: {
          'AlertDefinition/name1': 'value1',
          'AlertDefinition/name2': 'value2'
        }
      }
    ];

    testCases.forEach(function (testCase) {

      it(testCase.m, function () {

        controller.set('configs', testCase.configs);
        var result = controller.getPropertiesToUpdate(true);

        expect(result).to.eql(testCase.result);
      });
    });

    describe('Some fields should be removed', function () {

      beforeEach(function () {
        controller.set('content', Em.Object.create({
          rawSourceData: {
            uri: {
              id: 123
            }
          }
        }));
        controller.set('configs', [
          Em.Object.create({
            apiProperty: 'source.uri.connection_timeout',
            apiFormattedValue: 123,
            wasChanged: true
          })
        ]);
        this.result = controller.getPropertiesToUpdate();
      });

      it('`AlertDefinition/source.uri.id`', function () {
        expect(this.result).to.not.have.deep.property('AlertDefinition/source.uri.id');
      });

    });

    describe('`source/parameters` for SCRIPT configs', function () {

      beforeEach(function () {
        controller.set('content', Em.Object.create({
          parameters: [
            Em.Object.create({name: 'p1', value: 'v1'}),
            Em.Object.create({name: 'p2', value: 'v2'}),
            Em.Object.create({name: 'p3', value: 'v3'}),
            Em.Object.create({name: 'p4', value: 'v4'})
          ],
          rawSourceData: {
            parameters: [
              {name: 'p1', value: 'v1'},
              {name: 'p2', value: 'v2'},
              {name: 'p3', value: 'v3'},
              {name: 'p4', value: 'v4'}
            ]
          }
        }));
        controller.set('configs', [
          Em.Object.create({apiProperty:'p1', apiFormattedValue: 'v11', wasChanged: true, name: 'parameter'}),
          Em.Object.create({apiProperty:'p2', apiFormattedValue: 'v21', wasChanged: true, name: 'parameter'}),
          Em.Object.create({apiProperty:'p3', apiFormattedValue: 'v31', wasChanged: true, name: 'parameter'}),
          Em.Object.create({apiProperty:'p4', apiFormattedValue: 'v41', wasChanged: true, name: 'parameter'})
        ]);
        this.result = controller.getPropertiesToUpdate();
      });

      it('should update parameters', function () {
        expect(this.result['AlertDefinition/source'].parameters).to.have.property('length').equal(4);
        expect(this.result['AlertDefinition/source'].parameters.mapProperty('value')).to.be.eql(['v11', 'v21', 'v31', 'v41']);
      });

    });

  });

  describe('#renderCommonWizardConfigs()', function () {

    it('should return correct number of configs', function () {

      var result = controller.renderCommonWizardConfigs();

      expect(result.length).to.equal(4);

    });

  });

  describe('#getConfigsValues()', function () {

    it('should create key-value map from configs', function () {

      controller.set('configs', [
        Em.Object.create({name: 'name1', value: 'value1'}),
        Em.Object.create({name: 'name2', value: 'value2'}),
        Em.Object.create({name: 'name3', value: 'value3'})
      ]);

      var result = controller.getConfigsValues();

      expect(result).to.eql([
        {name: 'name1', value: 'value1'},
        {name: 'name2', value: 'value2'},
        {name: 'name3', value: 'value3'}
      ]);

    });

  });

});
