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

require('models/alerts/alert_definition');

var model;

function getModel() {
  return App.AlertDefinition.createRecord();
}

describe('App.AlertDefinition', function () {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedAnd(getModel(), 'isHostAlertDefinition', ['isAmbariService', 'isAmbariAgentComponent']);

  describe('#isCriticalOrWarning', function () {

    Em.A([
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {CRITICAL: {count: 0, maintenanceCount: 1}}, e: false},
      {summary: {CRITICAL: {count: 1, maintenanceCount: 1}}, e: true},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isCriticalOrWarning')).to.equal(test.e);
      });
    });

  });

  describe('#isCritical', function () {

    Em.A([
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isCritical')).to.equal(test.e);
      });
    });

  });

  describe('#isWarning', function () {

    Em.A([
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isWarning')).to.equal(test.e);
      });
    });

  });

  describe('#isOK', function () {

    Em.A([
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isOK')).to.equal(test.e);
      });
    });

  });

  describe('#isUnknown', function () {

    Em.A([
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isUnknown')).to.equal(test.e);
      });
    });

  });

  describe('#lastTriggeredAgoFormatted', function () {

    it('should be empty', function () {
      model.set('lastTriggeredRaw', 0);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('');
    });

    it('should not be empty', function () {
      model.set('lastTriggeredRaw', new Date().getTime() - 61000);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('about a minute ago');
    });

  });

  describe('#serviceDisplayName', function () {

    it('should get name for non-existing service', function () {
      model.set('serviceName', 'FOOBAR');
      expect(model.get('serviceDisplayName')).to.equal('Foobar');
    });

  });

  describe('#componentNameFormatted', function () {

    beforeEach(function () {
      sinon.stub(App.format, 'role', function (a) {
        return 'role ' + a;
      });
    });

    it('should wrap component name by App.format.role method', function () {
      model.set('componentName', 'test');
      var result = model.get('componentNameFormatted');
      expect(result).to.equal('role test');
    });

    afterEach(function () {
      App.format.role.restore();
    });


  });

  App.TestAliases.testAsComputedGetByKey(getModel(), 'typeIconClass', 'typeIcons', 'type', {map: {
    METRIC: 'glyphicon glyphicon-flash',
    SCRIPT: 'glyphicon glyphicon-file',
    WEB: 'glyphicon glyphicon-globe',
    PORT: 'glyphicon glyphicon-log-in',
    AGGREGATE: 'glyphicon glyphicon-plus',
    SERVER: 'glyphicon glyphicon-oil',
    RECOVERY: 'glyphicon glyphicon-oil'
  }});

  describe('#lastTriggeredFormatted', function () {
    it('should be empty', function () {
      model.set('lastTriggered', 0);
      expect(model.get('lastTriggeredFormatted')).to.be.equal('');
    });
  });

  describe('REOPEN', function () {

    describe('#getSortDefinitionsByStatus', function () {

      Em.A([
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {WARNING: {count: 1, maintenanceCount: 0}}}),
            order: true,
            e: -1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 2, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            order: true,
            e: -1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {WARNING: {count: 1, maintenanceCount: 0}}}),
            order: false,
            e: 1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 2, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            order: false,
            e: 1
          }
        ]).forEach(function(test, i) {
          it('test #' + (i + 1), function () {
            var func = App.AlertDefinition.getSortDefinitionsByStatus(test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
        });

    });

  });

});
