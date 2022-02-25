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

function getView() {
  return App.EditDashboardWidgetPopup.create({
    widgetView: Em.View.create(),
    sliderHandlersManager: {}
  });
}

describe('App.EditDashboardWidgetPopup', function () {

  App.TestAliases.testAsComputedAlias(getView(), 'disablePrimary', 'sliderHandlersManager.hasErrors');

  App.TestAliases.testAsComputedAlias(getView(), 'sliderMaxValue', 'sliderHandlersManager.maxValue');

  describe('#init', function () {

    it('should throw an Error if no `widgetView` provided', function () {
      expect(function () {
        App.EditDashboardWidgetPopup.create({
          sliderHandlersManager: {}
        });
      }).to.throw(/`widgetView` should be valid view/);
    });

    it('should throw an Error if no `sliderHandlersManager` provided', function () {
      expect(function () {
        App.EditDashboardWidgetPopup.create({
          widgetView: Em.View.create(),
        });
      }).to.throw(/`sliderHandlersManager` should be set/);
    });

  });

});

describe('App.EditDashboardWidgetPopup.SingleHandler', function () {

  var handler;

  function getSingleHandler() {
    return App.EditDashboardWidgetPopup.SingleHandler.create();
  }

  beforeEach(function () {
    handler = getSingleHandler();
  });

  App.TestAliases.testAsComputedAlias(getSingleHandler(), 'hasErrors', 'thresholdMinError');

  describe('#updateThresholds', function () {

    it('should update `thresholdMin`', function () {
      handler.set('thresholdMin', -1);
      handler.updateThresholds([100500]);
      expect(handler.get('thresholdMin')).to.be.equal(100500);
    });

  });

  describe('#thresholdMinErrorMessage', function() {

    var minValue = 0;
    var maxValue = 100;
    var msg = Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);

    beforeEach(function() {
      handler.setProperties({
        minValue: minValue,
        maxValue: maxValue
      });
    });

    [
      {thresholdMin: -1, e: msg},
      {thresholdMin: 101, e: msg},
      {thresholdMin: 'abc', e: msg},
      {thresholdMin: 60, e: ''}
    ].forEach(function(test) {
      it('thresholdMin: ' + JSON.stringify(test.thresholdMin), function () {
        handler.set('thresholdMin', test.thresholdMin);
        expect(handler.get('thresholdMinErrorMessage')).to.be.equal(test.e);
      });
    });

  });

  describe('#preparedThresholds', function () {

    it('mapped to array threshold values', function () {
      handler.setProperties({
        thresholdMin: 1
      });
      expect(handler.get('preparedThresholds')).to.be.eql([1]);
    });

  });

});

describe('App.EditDashboardWidgetPopup.DoubleHandlers', function () {

  var handler;

  function getDoubleHandlers() {
    return App.EditDashboardWidgetPopup.DoubleHandlers.create();
  }

  beforeEach(function () {
    handler = getDoubleHandlers();
  });

  App.TestAliases.testAsComputedOr(getDoubleHandlers(), 'hasErrors', ['thresholdMinError', 'thresholdMaxError']);

  describe('#updateThresholds', function () {

    it('should update `thresholdMin` and `thresholdMax`', function () {
      handler.set('thresholdMin', -1);
      handler.set('thresholdMax', 1);
      handler.updateThresholds([1234, 4321]);
      expect(handler.get('thresholdMin')).to.be.equal(1234);
      expect(handler.get('thresholdMax')).to.be.equal(4321);
    });

  });

  describe('#thresholdMinErrorMessage', function() {

    var minValue = 0;
    var maxValue = 100;
    var msg = Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);
    var msg2 = Em.I18n.t('dashboard.widgets.error.smaller');

    beforeEach(function() {
      handler.setProperties({
        minValue: minValue,
        maxValue: maxValue
      });
    });

    [
      {thresholdMin: -1, e: msg},
      {thresholdMin: 101, e: msg},
      {thresholdMin: 'abc', e: msg},
      {thresholdMin: 60, e: ''},
      {thresholdMin: 99, e: msg2}
    ].forEach(function(test) {
      it('thresholdMin: ' + JSON.stringify(test.thresholdMin), function () {
        handler.set('thresholdMin', test.thresholdMin);
        handler.set('thresholdMax', 98);
        expect(handler.get('thresholdMinErrorMessage')).to.be.equal(test.e);
      });
    });

  });

  describe('#thresholdMaxErrorMessage', function () {
    var minValue = 0;
    var maxValue = 100;
    var msg = Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);

    beforeEach(function() {
      handler.setProperties({
        minValue: minValue,
        maxValue: maxValue
      });
    });

    [
      {thresholdMax: -1, e: msg},
      {thresholdMax: 101, e: msg},
      {thresholdMax: 'abc', e: msg},
      {thresholdMax: 60, e: ''}
    ].forEach(function(test) {
      it('thresholdMax: ' + JSON.stringify(test.thresholdMax), function () {
        handler.set('thresholdMax', test.thresholdMax);
        expect(handler.get('thresholdMaxErrorMessage')).to.be.equal(test.e);
      });
    });
  });

  describe('#preparedThresholds', function () {

    it('mapped to array threshold values', function () {
      handler.setProperties({
        thresholdMin: 1,
        thresholdMax: 2
      });
      expect(handler.get('preparedThresholds')).to.be.eql([1, 2]);
    });

  });

});