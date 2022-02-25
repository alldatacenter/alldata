/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
var date = require('utils/date/date');

describe('MainChartHeatmapMetric', function () {
  var mainChartHeatmapMetric = App.MainChartHeatmapMetric.create({});

  beforeEach(function () {
    mainChartHeatmapMetric = App.MainChartHeatmapMetric.create({});
  });

  describe('#slotDefinitions', function () {
    beforeEach(function () {
      sinon.stub(mainChartHeatmapMetric, 'generateSlot', Em.K);
      mainChartHeatmapMetric.set('maximumValue', 100);
      mainChartHeatmapMetric.set('minimumValue', 0);
    });
    afterEach(function () {
      mainChartHeatmapMetric.generateSlot.restore();
    });

    describe('one slot', function () {

      beforeEach(function () {
        mainChartHeatmapMetric.set('numberOfSlots', 1);
        mainChartHeatmapMetric.propertyDidChange('slotDefinitions');
        this.slotDefinitions = mainChartHeatmapMetric.get('slotDefinitions');
      });

      it('4 slotDefinitions', function () {
        expect(this.slotDefinitions.length).to.equal(4);
      });
      it('generateSlot is called 1 time', function () {
        expect(mainChartHeatmapMetric.generateSlot.callCount).to.be.equal(1);
      });
      it('generateSlot is called with correct arguments', function () {
        expect(mainChartHeatmapMetric.generateSlot.getCall(0).args).to.eql([0, 100, '', '#1EB475']);
      });

    });

    describe('two slots', function () {

      beforeEach(function () {
        mainChartHeatmapMetric.set('numberOfSlots', 2);
        mainChartHeatmapMetric.propertyDidChange('slotDefinitions');
        this.slotDefinitions = mainChartHeatmapMetric.get('slotDefinitions');
      });

      it('4 slotDefinitions', function () {
        expect(this.slotDefinitions.length).to.equal(5);
      });
      it('generateSlot is called 2 times', function () {
        expect(mainChartHeatmapMetric.generateSlot.callCount).to.be.equal(2);
      });
      it('generateSlot 1st call has valid arguments', function () {
        expect(mainChartHeatmapMetric.generateSlot.getCall(0).args).to.eql([0, 50, '', '#1EB475']);
      });
      it('generateSlot 2nd call has valid arguments', function () {
        expect(mainChartHeatmapMetric.generateSlot.getCall(1).args).to.eql([50, 100, '', '#1FB418']);
      });

    });
  });

  describe('#generateSlot()', function () {

    beforeEach(function () {
      sinon.stub(mainChartHeatmapMetric, 'formatLegendLabel').returns('label');
      sinon.stub(mainChartHeatmapMetric, 'convertNumber').returns('number');
    });

    afterEach(function () {
      mainChartHeatmapMetric.formatLegendLabel.restore();
      mainChartHeatmapMetric.convertNumber.restore();
    });

    it('generateSlot result is valid', function () {
      expect(mainChartHeatmapMetric.generateSlot(0, 1, '', '#1FB418')).to.eql(Em.Object.create({
        hasBoundaries: true,
        "from": "number",
        "to": "number",
        "label": "label - label",
        "cssStyle": "background-color:#1FB418"
      }));
    });
  });

  describe('#getHatchStyle()', function () {
    var testCases = [
      {
        title: 'unknown browser',
        data: {},
        result: 'background-color:#666'
      },
      {
        title: 'webkit browser',
        data: {
          webkit: true
        },
        result: 'background-image:-webkit-repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)'
      },
      {
        title: 'mozilla browser',
        data: {
          mozilla: true
        },
        result: 'background-image:repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)'
      },
      {
        title: 'IE version 9',
        data: {
          msie: true,
          version: '9.0'
        },
        result: 'background-color:#666'
      },
      {
        title: 'IE version 10',
        data: {
          msie: true,
          version: '10.0'
        },
        result: 'background-image:repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)'
      }
    ];

    testCases.forEach(function(test){
      it(test.title, function () {
        jQuery.browser = test.data;
        expect(mainChartHeatmapMetric.getHatchStyle()).to.equal(test.result);
      });
    });
  });

  describe('#hostToSlotMap', function () {

    beforeEach(function () {
      this.stub = sinon.stub(mainChartHeatmapMetric, 'calculateSlot');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it('hostToValueMap is null', function () {
      mainChartHeatmapMetric.set('hostToValueMap', null);
      mainChartHeatmapMetric.set('hostNames', []);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
    });
    it('hostNames is null', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', null);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
    });
    it('slot greater than -1', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', ['host1']);
      this.stub.returns(0);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.eql({'host1': 0});
      expect(mainChartHeatmapMetric.calculateSlot.calledWith({}, 'host1')).to.be.true;
    });
    it('slot equal to -1', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', ['host1']);
      this.stub.returns('-1');
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
      expect(mainChartHeatmapMetric.calculateSlot.calledWith({}, 'host1')).to.be.true;
    });
  });

  describe('#calculateSlot()', function () {

    beforeEach(function() {
      sinon.stub(mainChartHeatmapMetric, 'get').withArgs('slotDefinitions').returns([
        Em.Object.create({
          from: 0,
          to: 2,
          hasBoundaries: true
        }),
        Em.Object.create({
          from: 2,
          to: 10,
          hasBoundaries: true
        }),
        Em.Object.create({
          invalidData: true,
          index: 5
        }),
        Em.Object.create({
          notAvailable: true,
          index: 6
        }),
        Em.Object.create({
          notApplicable: true,
          index: 7
        })
      ]);
    });

    afterEach(function() {
      mainChartHeatmapMetric.get.restore();
    });

    it('not applicable metric', function () {
      expect(mainChartHeatmapMetric.calculateSlot({}, 'host1')).to.be.equal(7);
    });

    it('not available data metric', function () {
      expect(mainChartHeatmapMetric.calculateSlot({'host1': undefined}, 'host1')).to.be.equal(6);
    });

    it('invalid data metric', function () {
      expect(mainChartHeatmapMetric.calculateSlot({'host1': 'NaN'}, 'host1')).to.be.equal(5);
    });

    it('metric should be in a minimal slot', function () {
      expect(mainChartHeatmapMetric.calculateSlot({'host1': '1'}, 'host1')).to.be.equal(0);
    });

    it('metric should be in a maximal slot', function () {
      expect(mainChartHeatmapMetric.calculateSlot({'host1': '11'}, 'host1')).to.be.equal(1);
    });
  });

  describe('#formatLegendLabel', function() {
    beforeEach(function () {
      sinon.stub(date, 'timingFormat').returns('30 secs');
    });

    afterEach(function () {
      date.timingFormat.restore();
    });


    var testCases = [
      {
        num: 100.1,
        units: '',
        expected: '100'
      },
      {
        num: 98.94,
        units: 'MB',
        expected: '98.9MB'
      },
      {
        num: 8.123,
        units: '%',
        expected: '8.12%'
      },
      {
        num: 30000,
        units: 'ms',
        expected: '30 secs'
      }
    ];

    testCases.forEach(function(test) {
      it('should return label = ' + test.expected, function() {
        expect(mainChartHeatmapMetric.formatLegendLabel(test.num, test.units)).to.be.equal(test.expected);
      });
    });
  });

  describe('#convertNumber', function() {

    it('MB units', function() {
      expect(mainChartHeatmapMetric.convertNumber(1, 'MB')).to.be.equal(1024 * 1024);
    });

    it('no units', function() {
      expect(mainChartHeatmapMetric.convertNumber(1, '')).to.be.equal(1);
    });
  });

});
