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

describe('App.ConfigWithOverrideRecommendationParser', function() {
	var mixin;

	beforeEach(function() {
    mixin = Em.Object.create(App.ConfigWithOverrideRecommendationParser, {
      parseRecommendations: Em.K,
      _updateConfigByRecommendation: Em.K,
      _removeConfigByRecommendation: Em.K,
      getRecommendation: Em.K,
      applyRecommendation: Em.K,
      useInitialValue: Em.K,
      _getInitialValue: Em.K
    });
	});

  describe("#updateOverridesByRecommendations()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'parseRecommendations');
    });

    afterEach(function() {
      mixin.parseRecommendations.restore();
    });

    it("parseRecommendations should be called", function() {
      var configGroup = Em.Object.create({
        name: 'g1',
        isDefault: false
      });
      mixin.updateOverridesByRecommendations({}, [{}], [{}], configGroup);
      expect(mixin.parseRecommendations.calledWith({}, [{}], [{}], configGroup)).to.be.true;
    });
  });

  describe("#_updateOverride()", function () {
    var config = Em.Object.create({
      name: 'c1',
      filename: 'f1',
      getOverride: Em.K
    });

    beforeEach(function() {
      this.mockUpdateValue = sinon.stub(mixin, 'allowUpdateProperty');
      this.mockOverride = sinon.stub(config, 'getOverride');
      sinon.stub(mixin, '_addConfigOverrideRecommendation');
      sinon.stub(mixin, '_updateConfigByRecommendation');
    });

    afterEach(function() {
      this.mockUpdateValue.restore();
      this.mockOverride.restore();
      mixin._addConfigOverrideRecommendation.restore();
      mixin._updateConfigByRecommendation.restore();
    });

    it("_updateConfigByRecommendation should be called", function() {
      this.mockOverride.returns({});
      mixin._updateOverride(config, 'val', [], Em.Object.create());
      expect(mixin._updateConfigByRecommendation.calledWith({}, 'val', [])).to.be.true;
      expect(mixin._addConfigOverrideRecommendation.called).to.be.false;
    });

    it("_addConfigOverrideRecommendation should be called", function() {
      this.mockUpdateValue.returns('val1');
      mixin._updateOverride(config, 'val', [], Em.Object.create());
      expect(mixin._addConfigOverrideRecommendation.calledWith(config, 'val', [], Em.Object.create())).to.be.true;
      expect(mixin._updateConfigByRecommendation.called).to.be.false;
    });

    it("no methods should be called", function() {
      this.mockUpdateValue.returns('');
      this.mockOverride.returns(null);
      mixin._updateOverride(config, 'val', [], Em.Object.create());
      expect(mixin._addConfigOverrideRecommendation.called).to.be.false;
      expect(mixin._updateConfigByRecommendation.called).to.be.false;
    });
  });

  describe("#_removeOverride()", function () {
    var property = Em.Object.create({
      overrides: [],
      getOverride: Em.K
    });

    beforeEach(function() {
      sinon.stub(mixin, '_removeConfigByRecommendation');
      this.mock = sinon.stub(property, 'getOverride');
    });

    afterEach(function() {
      mixin._removeConfigByRecommendation.restore();
      this.mock.restore();
    });

    it("no override", function() {
      this.mock.returns(null);
      mixin._removeOverride(property, [], [], Em.Object.create());
      expect(mixin._removeConfigByRecommendation.calledWith(property, [], [])).to.be.true;
    });

    it("has override", function() {
      this.mock.returns({});
      mixin._removeOverride(property, [], [], Em.Object.create());
      expect(mixin._removeConfigByRecommendation.calledWith({}, [], [])).to.be.true;
    });
  });

  describe("#_addConfigOverrideRecommendation()", function () {
    var config = Em.Object.create({
      name: 'c1',
      filename: 'f1',
      serviceName: 'S1'
    });

    beforeEach(function() {
      this.mockRecommendation = sinon.stub(mixin, 'getRecommendation');
      sinon.stub(App.config, 'createOverride');
      sinon.stub(mixin, 'useInitialValue').returns(false);
      sinon.stub(mixin, 'applyRecommendation');
      sinon.stub(mixin, '_getInitialValue').returns('init');
    });

    afterEach(function() {
      this.mockRecommendation.restore();
      App.config.createOverride.restore();
      mixin.useInitialValue.restore();
      mixin.applyRecommendation.restore();
      mixin._getInitialValue.restore();
    });

    it("applyRecommendation should be called", function() {
      mixin._addConfigOverrideRecommendation(config, 'val', [], Em.Object.create({name: 'g1'}));
      expect(mixin.applyRecommendation.calledWith('c1', 'f1', 'g1', 'val', 'init', [])).to.be.true;
    });

    it("App.config.createOverride should be called, initialValue is null", function() {
      this.mockRecommendation.returns(null);
      mixin._addConfigOverrideRecommendation(config, 'val', [], Em.Object.create({name: 'g1'}));
      expect(App.config.createOverride.calledWith(config, {
        "value": 'val',
        "recommendedValue": 'val',
        "initialValue": null,
        "savedValue": null,
        "isEditable": true,
        "errorMessage": '',
        "warnMessage": ''
      }, Em.Object.create({name: 'g1'}))).to.be.true;
    });

    it("App.config.createOverride should be called, initialValue is correct", function() {
      this.mockRecommendation.returns({value: 'val1'});
      mixin._addConfigOverrideRecommendation(config, 'val', [], Em.Object.create({name: 'g1'}));
      expect(App.config.createOverride.calledWith(config, {
        "value": 'val',
        "recommendedValue": 'val',
        "initialValue": 'val1',
        "savedValue": 'val1',
        "isEditable": true,
        "errorMessage": '',
        "warnMessage": ''
      }, Em.Object.create({name: 'g1'}))).to.be.true;
    });
  });

  describe("#_updateOverrideBoundaries()", function () {
    var stackProperty = {
      valueAttributes: {
        g1: {
          'attr1': 'true'
        }
      }
    };

    it("modify attributes on existing group", function() {
      mixin._updateOverrideBoundaries(stackProperty, 'attr1', 'false', 'n1', 'file1', Em.Object.create({name: 'g1'}));
      expect(stackProperty.valueAttributes.g1.attr1).to.be.equal('false');
    });

    it("modify attributes on new group", function() {
      mixin._updateOverrideBoundaries(stackProperty, 'attr1', 'true', 'n2', 'file1', Em.Object.create({name: 'g2'}));
      expect(stackProperty.valueAttributes.g2.attr1).to.be.equal('true');
    });
  });
});

