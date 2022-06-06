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
var view;

describe('App.TimeIntervalSpinnerView', function () {

  beforeEach(function () {
    view = App.TimeIntervalSpinnerView.create({
      movePopover: Em.K,
      controller: Em.Object.create({
        removeCurrentFromDependentList: Em.K
      }),
      initPopover: Em.K
    });
    sinon.stub(Em.run, 'once', Em.K);
  });

  afterEach(function () {
    view.destroy();
    Em.run.once.restore();
  });

  describe('#generateWidgetValue', function () {

    var createProperty = function (widgetUnits, configPropertyUnits, incrementStep) {
      return Em.Object.create({
        stackConfigProperty: Em.Object.create({
          widget: {
            units: [
              { unit: widgetUnits }
            ]
          },
          valueAttributes: {
            unit: configPropertyUnits,
            increment_step: incrementStep
          }
        })
      });
    };

    var tests = [
      {
        input: 60000,
        config: createProperty("days,hours,minutes", "milliseconds", 1000),
        e: [
          { label: 'Days', value: 0, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 1, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "2592000000",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 30, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 0, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "604800000",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 7, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 0, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "804820200",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 9, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 7, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 33, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "70000",
        config: createProperty("minutes", "milliseconds", 1000),
        e: [
          { label: 'Minutes', value: 1, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "140",
        config: createProperty("hours,minutes", "minutes", 1),
        e: [
          { label: 'Hours', value: 2, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 20, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "2",
        config: createProperty("hours", "hours", 1),
        e: [
          { label: 'Hours', value: 2, incrementStep: 1, enabled: true}
        ]
      }
    ];

    tests.forEach(function (test) {
      it('should convert {0} {1} to {2}'.format(test.input, test.config.get('stackConfigProperty.valueAttributes.unit'), JSON.stringify(test.e)), function () {
        view.set('config', test.config);
        var result = view.generateWidgetValue(test.input, test.inputType, test.desiredUnits).map(function (item) {
          // remove unnecessary keys
          return App.permit(item, ['label', 'value', 'enabled', 'incrementStep']);
        });
        expect(result).to.eql(test.e);
      });
    });

  });

  describe('#parseIncrement', function () {

    var createProperty = function (widgetUnits, configPropertyUnits, incrementStep, value, min, max) {
      return Em.Object.create({
        value: value,
        isValid: true,
        stackConfigProperty: Em.Object.create({
          widget: {
            units: [
              { unit: widgetUnits }
            ]
          },
          valueAttributes: {
            unit: configPropertyUnits,
            minimum: min,
            maximum: max,
            increment_step: incrementStep
          }
        })
      });
    };

    Em.A([
        {
          input: "120000",
          config: createProperty("minutes,seconds", "milliseconds", 10000, "120000", 0, 240000),
          e: [
            { label: 'Minutes', value: 2, incrementStep: 1, enabled: true},
            { label: 'Seconds', value: 0, incrementStep: 10, enabled: true}
          ]
        },
        {
          input: "120000",
          config: createProperty("minutes,seconds", "milliseconds", 60000, "120000", "0", "240000"),
          e: [
            { label: 'Minutes', value: 2, incrementStep: 1, enabled: true},
            { label: 'Seconds', value: 0, incrementStep: 60, enabled: false}
          ]
        }
      ]).forEach(function (test) {
        it('should convert {0} {1} to {2}'.format(test.input, test.config.get('stackConfigProperty.valueAttributes.unit'), JSON.stringify(test.e)), function () {
          view.set('config', test.config);
          view.prepareContent();
          var result = view.get('content').map(function (c) {
            return App.permit(c, ['label', 'value', 'incrementStep', 'enabled']);
          });
          expect(result).to.eql(test.e);
        });
      });

  });

  describe('#checkErrors', function () {

    Em.A([
        {
          config: Em.Object.create({
            value: "540",
            isValid: true,
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.less').format("10 Minutes"),
            warn: true
          }
        },
        {
          config: Em.Object.create({
            value: "86460",
            isValid: true,
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format("24 Hours"),
            warn: true
          }
        },
        {
          config: Em.Object.create({
            value: "12000",
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage:'',
            warn: false
          }
        }
      ]).forEach(function (test) {
        it(test.e.warn + ' ' + test.e.warnMessage, function () {
          view.set('config', test.config);
          view.prepareContent();
          view.checkErrors();
          expect(view.get('config.warnMessage')).to.equal(test.e.warnMessage);
          expect(view.get('config.warn')).to.equal(test.e.warn);
        });
      });

  });

  describe('#isValueCompatibleWithWidget', function() {
    var stackConfigProperty = null;

    beforeEach(function() {
      view.set('config', Em.Object.create({}));
      stackConfigProperty = {
        name: 'p1', valueAttributes: {
          minimum: 1, maximum: 10, increment_step: 4, type: 'int', unit: 'seconds'
        },
        widget: {
          units: [
            {
              'unit-name': 'hours,minutes'
            }
          ]
        }
      };
      view.set('config.stackConfigProperty', stackConfigProperty);
      view.set('config.isValid', true);
      view.set('maxValue', [{"value":10,"type":"hours","minValue":0,"maxValue":10,"incrementStep":1,"enabled":true},{"value":0,"type":"minutes","minValue":0,"maxValue":59,"incrementStep":1,"enabled":true}]);
      view.set('minValue', [{"value":0,"type":"hours","minValue":0,"maxValue":23,"incrementStep":1,"enabled":true},{"value":10,"type":"minutes","minValue":0,"maxValue":59,"incrementStep":1,"enabled":true}]);
    });

    it ('fail by config validation', function() {
      view.set('config.isValid', false);
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail by view validation', function() {
      view.set('config.value', 'a');
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail by view validation int', function() {
      view.set('config.value', '2.2');
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail: to large', function() {
      view.set('config.value', 12);
      expect(view.isValueCompatibleWithWidget()).to.be.false;
      expect(view.get('warnMessage')).to.have.property('length').that.is.least(1);
      expect(view.get('issueMessage')).to.have.property('length').that.is.least(1);
    });

    it ('fail: to small', function() {
      view.set('config.value', 0);
      expect(view.isValueCompatibleWithWidget()).to.be.false;
      expect(view.get('warnMessage')).to.have.property('length').that.is.least(1);
      expect(view.get('issueMessage')).to.have.property('length').that.is.least(1);
    });

    it ('fail: wrong step', function() {
      view.set('config.stackConfigProperty', stackConfigProperty);
      view.set('config.value', '3');
      expect(view.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('ok', function() {
      view.set('config.value', 4);
      expect(view.isValueCompatibleWithWidget()).to.be.true;
      expect(view.get('warnMessage')).to.equal('');
      expect(view.get('issueMessage')).to.equal('');
    });
  });

  describe('#showAsTextBox', function() {
    Em.A([
      {
        config: App.ServiceConfigProperty.create({
          value: "600",
          isValid: true,
          stackConfigProperty: Em.Object.create({
            widget: {
              units: [
                { unit: "hours,minutes" }
              ]
            },
            valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
          })
        }),
        m: 'original config with valid value should be shown as widget',
        e: false
      },
      {
        config: App.ServiceConfigProperty.create({
          value: "test",
          isValid: true,
          stackConfigProperty: Em.Object.create({
            widget: {
              units: [
                { unit: "hours,minutes" }
              ]
            },
            valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
          })
        }),
        m: 'original config with invalid value should be shown as textbox',
        e: true
      },
      {
        config: App.ServiceConfigProperty.create({
          value: "600",
          isValid: true,
          stackConfigProperty: Em.Object.create({
            widget: {
              units: [
                { unit: "hours,minutes" }
              ]
            },
            valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
          }),
          parentSCP: Em.Object.create({ value: "600" })
        }),
        m: 'overriden config have same value as original and values of both configs are valid, widget should be shown',
        e: false
      },
      {
        config: App.ServiceConfigProperty.create({
          value: "test",
          isValid: true,
          stackConfigProperty: Em.Object.create({
            widget: {
              units: [
                { unit: "hours,minutes" }
              ]
            },
            valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
          }),
          parentSCP: Em.Object.create({ value: "test" })
        }),
        m: 'overriden config have same value as original and values of both configs are NOT valid, textbox should be shown',
        e: true
      },
      {
        config: App.ServiceConfigProperty.create({
          value: "test",
          isValid: true,
          stackConfigProperty: Em.Object.create({
            widget: {
              units: [
                { unit: "hours,minutes" }
              ]
            },
            valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
          }),
          parentSCP: Em.Object.create({ value: "500" })
        }),
        m: 'overriden config have different value as original and values of override NOT valid, textbox should be shown',
        e: true
      }
    ]).forEach(function (test) {
      it(test.m, function() {
        view.set('config', test.config);
        view.didInsertElement();
        expect(view.get('config.showAsTextBox')).to.eql(test.e);
      });
    });
  });
});
