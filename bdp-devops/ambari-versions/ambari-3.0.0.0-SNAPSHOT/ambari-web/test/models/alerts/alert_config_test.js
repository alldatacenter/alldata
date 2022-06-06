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

require('models/alerts/alert_config');

var model;

describe('App.AlertConfigProperty', function () {

  function getConfigProperty() {
    return App.AlertConfigProperty.create();
  }

  App.TestAliases.testAsComputedNotExistsIn(getConfigProperty(), 'isPreLabeled', 'displayType', ['radioButton']);

  App.TestAliases.testAsComputedAlias(getConfigProperty(), 'apiFormattedValue', 'value');

});

describe('App.AlertConfigProperties', function () {

  describe('Parameter', function () {

    function getModel() {
      return App.AlertConfigProperties.Parameter.create();
    }

    App.TestAliases.testAsComputedAlias(getModel(), 'badge', 'threshold');

  });

  describe('App.AlertConfigProperties.Parameters', function () {

    describe('StringMixin', function () {

      var obj;

      beforeEach(function () {
        obj = App.AlertConfigProperties.Parameter.create(App.AlertConfigProperties.Parameters.StringMixin, {});
      });

      describe('#isValid', function () {
        Em.A([
          {value: '', expected: false},
          {value: '\t', expected: false},
          {value: '    ', expected: false},
          {value: '\n', expected: false},
          {value: '\r', expected: false},
          {value: 'some not empty string', expected: true}
        ]).forEach(function (test) {
          it('value: ' + JSON.stringify(test.value) + ' ;result - ' + test.expected, function () {
            obj.set('value', test.value);
            expect(obj.get('isValid')).to.be.equal(test.expected);
          });
        });
      });

    });

    describe('NumericMixin', function () {

      var obj;

      beforeEach(function () {
        obj = App.AlertConfigProperties.Parameter.create(App.AlertConfigProperties.Parameters.NumericMixin, {});
      });

      describe('#isValid', function () {
        Em.A([
          {value: '', expected: false},
          {value: 'abc', expected: false},
          {value: 'g1', expected: false},
          {value: '1g', expected: false},
          {value: '123', expected: true},
          {value: '123.8', expected: true},
          {value: 123, expected: true},
          {value: 123.8, expected: true},
        ]).forEach(function (test) {
          it('value: ' + JSON.stringify(test.value) + ' ;result - ' + test.expected, function () {
            obj.set('value', test.value);
            expect(obj.get('isValid')).to.be.equal(test.expected);
          });
        });
      });

    });

    describe('PercentageMixin', function () {

      var obj;

      beforeEach(function () {
        obj = App.AlertConfigProperties.Parameter.create(App.AlertConfigProperties.Parameters.PercentageMixin, {});
      });

      describe('#isValid', function () {
        Em.A([
          {value: '', expected: false},
          {value: 'abc', expected: false},
          {value: 'g1', expected: false},
          {value: '1g', expected: false},
          {value: '123', expected: true},
          {value: '23', expected: true},
          {value: '123.8', expected: true},
          {value: '5.8', expected: true},
          {value: 123, expected: true},
          {value: 23, expected: true},
          {value: 123.8, expected: true},
          {value: 5.8, expected: true}
        ]).forEach(function (test) {
          it('value: ' + JSON.stringify(test.value) + ' ;result - ' + test.expected, function () {
            obj.set('value', test.value);
            expect(obj.get('isValid')).to.be.equal(test.expected);
          });
        });
      });

    });

  });

  describe('Threshold', function () {

    beforeEach(function () {
      model = App.AlertConfigProperties.Threshold.create({});
    });

    describe('#apiFormattedValue', function () {

      it('should be based on showInputForValue and showInputForText', function () {

        model.setProperties({
          value: 'value',
          text: 'text',
          showInputForValue: false,
          showInputForText: false
        });
        expect(model.get('apiFormattedValue')).to.eql([]);

        model.set('showInputForValue', true);
        expect(model.get('apiFormattedValue')).to.eql(['value']);

        model.set('showInputForText', true);
        expect(model.get('apiFormattedValue')).to.eql(['value', 'text']);

      });

    });

    describe('#badgeCssClass', function () {

      it ('should be based on badge', function () {

        model.set('badge', 'OK');
        expect(model.get('badgeCssClass')).to.equal('alert-state-OK');

      });

    });

    describe('#wasChanged', function () {

      Em.A([
          {
            p: {
              previousValue: null,
              previousText: null,
              value: '',
              text: ''
            },
            e: false
          },
          {
            p: {
              previousValue: 'not null',
              previousText: null,
              value: '',
              text: ''
            },
            e: true
          },
          {
            p: {
              previousValue: null,
              previousText: 'not null',
              value: '',
              text: ''
            },
            e: true
          },
          {
            p: {
              previousValue: 'not null',
              previousText: 'not null',
              value: '',
              text: ''
            },
            e: true
          }
        ]).forEach(function (test, i) {
        it('test #' + (i + 1), function () {
          model.setProperties(test.p);
          expect(model.get('wasChanged')).to.equal(test.e);
        });
      });

    });

    describe('#isValid', function () {

      it('should be true if showInputForValue is false', function () {
        model.set('showInputForValue', false);
        expect(model.get('isValid')).to.be.true;
      });

      it('should be false if displayValue is null', function () {
        model.set('displayValue', null);
        expect(model.get('isValid')).to.be.false;

        model.set('displayValue', undefined);
        expect(model.get('isValid')).to.be.false;
      });

      it('should be false if METRIC displayValue is not valid float', function () {
        model.set('displayValue', '$1234.444');
        expect(model.get('isValid')).to.be.false;

        model.set('displayValue', 'hello-world!');
        expect(model.get('isValid')).to.be.false;
      });

      it('should be true if METRIC displayValue is valid float with at most one decimal', function () {
        model.set('displayValue', '123.4');
        expect(model.get('isValid')).to.be.true;

        model.set('displayValue', '123.0');
        expect(model.get('isValid')).to.be.true;

        model.set('displayValue', '666');
        expect(model.get('isValid')).to.be.true;
      });

      it('should be false if METRIC displayValue is valid float with more than one decimal', function () {
        model.set('displayValue', '123.48');
        expect(model.get('isValid')).to.be.false;
      });

      it('should be true for AGGREGATE percentage with precision of 1', function () {
        model = Em.Object.create(App.AlertConfigProperties.Thresholds.PercentageMixin, {
          displayValue: '1',
          showInputForValue: true
        });

        expect(model.get('isValid')).to.be.true;

        model.set('displayValue', '88');
        expect(model.get('isValid')).to.be.true;
      });

      it('should be false for AGGREGATE percentage values with precision smaller than 1', function () {
        model = Em.Object.create(App.AlertConfigProperties.Thresholds.PercentageMixin, {
          displayValue: '70.01',
          showInputForValue: true
        });

        expect(model.get('isValid')).to.be.false;

        model.set('displayValue', '70.0');
        expect(model.get('isValid')).to.be.false;

        model.set('displayValue', '80.000');
        expect(model.get('isValid')).to.be.false;
      });

      it('should be true for PORT percentage values with precision of 1/10th', function () {
        model = App.AlertConfigProperties.Threshold.create({
          value: '0.4',
          showInputForValue: true
        })

        expect(model.get('isValid')).to.be.true;

        model.set('value', '3');
        expect(model.get('isValid')).to.be.true;

        model.set('value', '33.0');
        expect(model.get('isValid')).to.be.true;
      });

      it('should be false for PORT percentage values with precision greater than 1/10th', function() {
        model = App.AlertConfigProperties.Threshold.create({
          value: '4.234',
          showInputForValue: true
        });

        expect(model.get('isValid')).to.be.false;

        model.set('value', '44.001');
        expect(model.get('isValid')).to.be.false;

        model.set('value', '112.01');
        expect(model.get('isValid')).to.be.false;
      });

    });

  });

  describe('App.AlertConfigProperties.Thresholds', function () {

    describe('OkThreshold', function () {

      beforeEach(function () {
        model = App.AlertConfigProperties.Thresholds.OkThreshold.create();
      });

      describe('#apiProperty', function () {

        it('should be based on showInputForValue and showInputForText', function () {

          model.setProperties({
            showInputForValue: false,
            showInputForText: false
          });
          expect(model.get('apiProperty')).to.eql([]);

          model.set('showInputForValue', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.ok.value']);

          model.set('showInputForText', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.ok.value', 'source.reporting.ok.text']);

        });

      });

    });

    describe('WarningThreshold', function () {

      beforeEach(function () {
        model = App.AlertConfigProperties.Thresholds.WarningThreshold.create();
      });

      describe('#apiProperty', function () {

        it('should be based on showInputForValue and showInputForText', function () {

          model.setProperties({
            showInputForValue: false,
            showInputForText: false
          });
          expect(model.get('apiProperty')).to.eql([]);

          model.set('showInputForValue', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.warning.value']);

          model.set('showInputForText', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.warning.value', 'source.reporting.warning.text']);

        });

      });

    });

    describe('CriticalThreshold', function () {

      beforeEach(function () {
        model = App.AlertConfigProperties.Thresholds.CriticalThreshold.create();
      });

      describe('#apiProperty', function () {

        it('should be based on showInputForValue and showInputForText', function () {

          model.setProperties({
            showInputForValue: false,
            showInputForText: false
          });
          expect(model.get('apiProperty')).to.eql([]);

          model.set('showInputForValue', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.critical.value']);

          model.set('showInputForText', true);
          expect(model.get('apiProperty')).to.eql(['source.reporting.critical.value', 'source.reporting.critical.text']);

        });

      });

    });

  });

  describe('App.AlertConfigProperties.FormatString', function () {
    function getFormatStringConfig() {
      return App.AlertConfigProperties.FormatString.create();
    }

    App.TestAliases.testAsComputedIfThenElse(getFormatStringConfig(), 'apiProperty', 'isJMXMetric', 'source.jmx.value', 'source.ganglia.value');

  });

});
