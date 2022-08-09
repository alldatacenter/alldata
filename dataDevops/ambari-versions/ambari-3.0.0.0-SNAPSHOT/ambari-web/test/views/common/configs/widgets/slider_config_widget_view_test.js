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
var validator = require('utils/validator');
var viewInt, viewFloat, viewPercent;

describe('App.SliderConfigWidgetView', function () {

  beforeEach(function () {
    viewInt = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      movePopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: App.ServiceConfigProperty.create({
        name: 'a.b.c',
        description: 'A B C',
        value: '486',
        savedValue: '486',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'int',
            minimum: '0',
            maximum: '2096',
            unit: 'MB',
            group1: {
              maximum: '3072'
            }
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'MB'}]
          })
        })
      })
    });
    viewInt.willInsertElement();
    viewInt.didInsertElement();

    viewFloat = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: App.ServiceConfigProperty.create({
        name: 'a.b.c2',
        description: 'A B C 2',
        value: '72.2',
        savedValue: '72.2',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'float',
            minimum: '0',
            maximum: '100'
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'float'}]
          })
        })
      })
    });
    viewFloat.willInsertElement();
    viewFloat.didInsertElement();

    viewPercent = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: App.ServiceConfigProperty.create({
        name: 'a.b.c3',
        description: 'A B C 3',
        value: '0.22',
        savedValue: '0.22',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'float',
            minimum: '0',
            maximum: '0.8'
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'percent'}]
          })
        })
      })
    });
    viewPercent.willInsertElement();
    viewPercent.didInsertElement();

    sinon.stub(viewInt, 'changeBoundaries', Em.K);
    sinon.stub(viewFloat, 'changeBoundaries', Em.K);
    sinon.stub(viewPercent, 'changeBoundaries', Em.K);
  });

  afterEach(function() {
    viewInt.changeBoundaries.restore();
    viewFloat.changeBoundaries.restore();
    viewPercent.changeBoundaries.restore();
  });

  describe('#mirrorValue', function () {
    it('should be equal to config.value after init', function () {
      expect(viewInt.get('mirrorValue').toString()).to.be.equal(viewInt.get('config.value'));
      expect(viewFloat.get('mirrorValue').toString()).to.be.equal(viewFloat.get('config.value'));
    });

    it('should be converted according to widget format', function() {
      expect(viewPercent.get('mirrorValue')).to.equal(22);
    });
  });

  describe('#mirrorValueObsOnce', function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.tryInvoke);
    });

    afterEach(function () {
      Em.run.once.restore();
    });

    describe('check int', function () {

      describe('valid value', function () {

        beforeEach(function () {
          viewInt.set('mirrorValue', 1000);
        });

        it('isMirrorValueValid is true', function () {
          expect(viewInt.get('isMirrorValueValid')).to.be.true;
        });
        it('config value is 1000', function () {
          expect(viewInt.get('config.value')).to.equal('1000');
        });
        it('errorMessage is empty', function () {
          expect(viewInt.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is empty', function () {
          expect(viewInt.get('config.warnMessage')).to.equal('');
        });
        it('warn is false', function () {
          expect(viewInt.get('config.warn')).to.be.false;
        });

      });

      describe('invalid value', function () {

        beforeEach(function () {
          viewInt.set('mirrorValue', 100500);
        });

        it('isMirrorValueValid is false', function () {
          expect(viewInt.get('isMirrorValueValid')).to.be.false;
        });
        it('config value is 486', function () {
          expect(viewInt.get('config.value')).to.equal('486');
        });
        it('errorMessage is empty', function () {
          expect(viewInt.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is not empty', function () {
          expect(viewInt.get('config.warnMessage')).to.have.property('length').that.is.least(1);
        });
        it('warn is true', function () {
          expect(viewInt.get('config.warn')).to.be.true;
        });

      });

    });

    describe('check float', function () {

      describe('valid value', function () {

        beforeEach(function () {
          viewFloat.set('mirrorValue', 55.5);
        });

        it('isMirrorValueValid is true', function () {
          expect(viewFloat.get('isMirrorValueValid')).to.be.true;
        });
        it('config value is 1000', function () {
          expect(viewFloat.get('config.value')).to.equal('55.5');
        });
        it('errorMessage is empty', function () {
          expect(viewFloat.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is empty', function () {
          expect(viewFloat.get('config.warnMessage')).to.equal('');
        });
        it('warn is false', function () {
          expect(viewFloat.get('config.warn')).to.be.false;
        });

      });

      describe('invalid value', function () {

        beforeEach(function () {
          viewFloat.set('mirrorValue', 100500.5);
        });

        it('isMirrorValueValid is false', function () {
          expect(viewFloat.get('isMirrorValueValid')).to.be.false;
        });
        it('config value is 1000', function () {
          expect(viewFloat.get('config.value')).to.equal('72.2');
        });
        it('errorMessage is empty', function () {
          expect(viewFloat.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is not empty', function () {
          expect(viewFloat.get('config.warnMessage')).to.have.property('length').that.is.least(1);
        });
        it('warn is true', function () {
          expect(viewFloat.get('config.warn')).to.be.true;
        });

      });

    });

    describe('check percent', function () {

      describe('valid value', function () {

        beforeEach(function () {
          viewPercent.set('mirrorValue', 32);
        });

        it('isMirrorValueValid is true', function () {
          expect(viewPercent.get('isMirrorValueValid')).to.be.true;
        });
        it('config value is 1000', function () {
          expect(viewPercent.get('config.value')).to.equal('0.32');
        });
        it('errorMessage is empty', function () {
          expect(viewPercent.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is empty', function () {
          expect(viewPercent.get('config.warnMessage')).to.equal('');
        });
        it('warn is false', function () {
          expect(viewPercent.get('config.warn')).to.be.false;
        });

      });

      describe('invalid value', function () {

        beforeEach(function () {
          viewPercent.set('mirrorValue', 100500.5);
        });

        it('isMirrorValueValid is false', function () {
          expect(viewPercent.get('isMirrorValueValid')).to.be.false;
        });
        it('config value is 1000', function () {
          expect(viewPercent.get('config.value')).to.equal('0.22');
        });
        it('errorMessage is empty', function () {
          expect(viewPercent.get('config.errorMessage')).to.equal('');
        });
        it('warnMessage is not empty', function () {
          expect(viewPercent.get('config.warnMessage')).to.have.property('length').that.is.least(1);
        });
        it('warn is true', function () {
          expect(viewPercent.get('config.warn')).to.be.true;
        });

      });

    });

  });

  describe('#getValueAttributeByGroup', function() {
    it('returns default max value', function() {
      viewInt.set('config.group', null);
      expect(viewInt.getValueAttributeByGroup('maximum')).to.equal('2096');
    });

    it('returns max value for group1', function() {
      viewInt.set('config.group', {name: 'group1'});
      expect(viewInt.getValueAttributeByGroup('maximum')).to.equal('3072');
    });

    it('minimum is missing', function () {
      viewInt.set('config.stackConfigProperty.valueAttributes.minimum', undefined);
      expect(viewInt.getValueAttributeByGroup('minimum')).to.equal('486');
    });

    it('minimum is missing, value is invalid', function () {
      viewInt.get('config').setProperties({
        'value': 3072,
        'stackConfigProperty.valueAttributes.minimum': undefined
      });
      expect(viewInt.getValueAttributeByGroup('minimum')).to.equal('2096');
    });
  });

  describe('#initSlider', function() {
    beforeEach(function() {
      this.view = App.SliderConfigWidgetView.create();
      sinon.stub(this.view, '$')
        .withArgs('input.slider-input').returns([])
        .withArgs('.ui-slider-wrapper:eq(0) .slider-tick').returns({
          eq: Em.K,
          addClass: Em.K,
          on: Em.K,
          append: Em.K,
          find: Em.K,
          css: Em.K,
          width: function() {},
          last: Em.K,
          hide: Em.K
        });
    });

    afterEach(function() {
      this.view.$.restore();
      this.view.destroy();
      this.view = null;
    });

    var tests = [
      {
        viewSetup: {
          minMirrorValue: 20,
          maxMirrorValue: 100,
          widgetRecommendedValue: 30,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'float' },
              widget: { units: [ { 'unit-name': "percent"}]}
            })
          })
        },
        e: {
          ticks: [20,30,40,60,80,90,100],
          ticksLabels: ['20 %', '', '', '60 %', '', '', '100 %']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 5,
          maxMirrorValue: 50,
          widgetRecommendedValue: 35,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int' },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [5, 16, 22, 28, 35, 39, 50],
          ticksLabels: ['5','', '', '28', '', '', '50']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 2,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2],
          ticksLabels: ['1', '2']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 3,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2,3],
          ticksLabels: ['1', '2', '3']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 3,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,1,2,3],
          ticksLabels: ['0', '1', '2', '3']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 5,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2,3,4,5],
          ticksLabels: ['1', '', '3', '', '5']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 5,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,2,3,5],
          ticksLabels: ['0', '2', '3', '5']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 23,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,2,6,12,17,20,23],
          ticksLabels: ['0', '', '', '12', '', '', '23']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 30,
          widgetRecommendedValue: 1,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { unit: "B", type: "int", minimum: "1048576", maximum: "31457280", increment_step: "262144" },
              widget: { units: [ { 'unit-name': "MB"}]}
            })
          })
        },
        e: {
          ticks: [1, 8.25, 15.5, 22.75, 30],
          ticksLabels: ["1 MB", "", "15.5 MB", "", "30 MB"]
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 100,
          widgetRecommendedValue: 10,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: {unit: "B", type: "int", minimum: "1073741824", maximum: "107374182400", increment_step: "1073741824"},
              widget: { units: [ { 'unit-name': "GB"}]}
            })
          })
        },
        e: {
          ticks: [1, 10, 26, 51, 75, 87.5, 100],
          ticksLabels: ["1 GB", "", "", "51 GB", "", "", "100 GB"]
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 100,
          isCompareMode: true,
          widgetRecommendedValue: 10,
          config: Em.Object.create({
            isOriginalSCP: false,
            stackConfigProperty: Em.Object.create({
              valueAttributes: {unit: "B", type: "int", minimum: "1073741824", maximum: "107374182400", increment_step: "1073741824"},
              widget: { units: [ { 'unit-name': "GB"}]}
            })
          })
        },
        e: {
          ticks: [1, 26, 51, 75, 100],
          ticksLabels: ["1 GB", "", "51 GB", "", "100 GB"]
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0.166,
          maxMirrorValue: 0.5,
          isCompareMode: false,
          widgetRecommendedValue: 0.166,
          config: Em.Object.create({
            isOriginalSCP: true,
            stackConfigProperty: Em.Object.create({
              valueAttributes: {unit: "MB", type: "int", minimum: "170", maximum: "512", increment_step: "256"},
              widget: {"units":[{"unit-name":"GB"}]}
            })
          })
        },
        e: {
          ticks: [0.166, 0.416, 0.5],
          ticksLabels: ["0.166 GB", "0.416 GB", "0.5 GB"]
        }
      }
    ];

    tests.forEach(function(test) {
      describe('should generate ticks: {0} - tick labels: {1}'.format(test.e.ticks, test.e.ticksLabels), function() {
        var ticks, ticksLabels;
        beforeEach(function () {
          this.view.reopen(test.viewSetup);
          this.view.set('controller', {
            isCompareMode: test.viewSetup.isCompareMode
          });
          var sliderCopy = window.Slider.prototype;
          window.Slider = function(a, b) {
            ticks = b.ticks;
            ticksLabels = b.ticks_labels;
            return {
              on: function() {
                return this;
              }
            };
          };
          this.view.willInsertElement();
          this.view.initSlider();
          window.Slider.prototype = sliderCopy;
        });

        it('ticks are ' + test.e.ticks, function () {
          expect(ticks.toArray()).to.be.eql(test.e.ticks);
        });

        it('ticksLabels are ' + test.e.ticksLabels, function () {
          expect(ticksLabels.toArray()).to.be.eql(test.e.ticksLabels);
        });
      });
    });
  });

  describe('#isValueCompatibleWithWidget', function() {
    var stackConfigProperty = null;

    beforeEach(function() {
      viewInt.set('config', App.ServiceConfigProperty.create({}));
      stackConfigProperty = {name: 'p1', widget: { units: [ { 'unit-name': "int"}]}, valueAttributes: {minimum: 1, maximum: 10, increment_step: 4, type: 'int'}};
      viewInt.set('config.stackConfigProperty', stackConfigProperty);
      viewInt.set('config.isValid', true);
    });

    it ('fail by config validation', function() {
      viewInt.set('config.isValid', false);
      expect(viewInt.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail by view validation', function() {
      viewInt.set('config.value', 'a');
      expect(viewInt.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail by view validation int', function() {
      viewInt.set('config.value', '2.2');
      expect(viewInt.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail by view validation float', function() {
      viewFloat.set('config.value', '2.2.2');
      viewFloat.set('validateFunction', validator.isValidFloat);
      expect(viewFloat.isValueCompatibleWithWidget()).to.be.false;
    });

    it ('fail: to large', function() {
      viewInt.set('config.value', 12);
      expect(viewInt.isValueCompatibleWithWidget()).to.be.false;
      expect(viewInt.get('warnMessage')).to.have.property('length').that.is.least(1);
      expect(viewInt.get('issueMessage')).to.have.property('length').that.is.least(1);
    });

    it ('fail: to small', function() {
      viewInt.set('config.value', 0);
      expect(viewInt.isValueCompatibleWithWidget()).to.be.false;
      expect(viewInt.get('warnMessage')).to.have.property('length').that.is.least(1);
      expect(viewInt.get('issueMessage')).to.have.property('length').that.is.least(1);
    });

    it ('fail: for wrong step', function() {
      viewInt.set('config.stackConfigProperty', stackConfigProperty);
      viewInt.set('config.value', '3');
      expect(viewInt.isValueCompatibleWithWidget()).to.be.true;
    });

    it ('ok', function() {
      viewInt.set('config.value', 4);
      expect(viewInt.isValueCompatibleWithWidget()).to.be.true;
      expect(viewInt.get('warnMessage')).to.equal('');
      expect(viewInt.get('issueMessage')).to.equal('');
    });

  });

  describe('#formatTickLabel', function () {

    var bytesView,
      cases = [
        {
          unitLabel: 'B',
          tick: 1024,
          result: '1024B',
          title: 'no conversion'
        },
        {
          unitLabel: 'KB',
          tick: 10240,
          result: '10MB',
          title: 'one exponent up conversion'
        },
        {
          unitLabel: 'MB',
          tick: 10000,
          result: '9.766GB',
          title: 'rounding to three decimals'
        },
        {
          unitLabel: 'GB',
          tick: 10752,
          separator: ' ',
          result: '10.5 TB',
          title: 'rounding to less than three decimals, custom separator'
        },
        {
          unitLabel: 'B',
          tick: 20971520,
          result: '20MB',
          title: 'several exponents up conversion'
        },
        {
          unitLabel: 'TB',
          tick: 10000,
          result: '10000TB',
          title: 'no conversions for the highest exponent unit'
        }
      ];

    beforeEach(function () {
      bytesView = App.SliderConfigWidgetView.create();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        bytesView.set('unitLabel', item.unitLabel);
        expect(bytesView.formatTickLabel(item.tick, item.separator)).to.equal(item.result);
      });
    });

  });

});
