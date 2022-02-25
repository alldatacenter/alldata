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

describe('App.MainAdminServiceAutoStartView', function () {

  beforeEach(function () {
    view = App.MainAdminServiceAutoStartView.create({
      controller: Em.Object.create({
        load: Em.K,
        componentsConfigsGrouped: []
      })
    });
  });


  describe('#didInsertElement()', function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'load');
    });
    afterEach(function() {
      view.get('controller').load.restore();
    });

    it('load should be called', function() {
      view.didInsertElement();
      expect(view.get('controller').load.calledOnce).to.be.true;
    });
  });

  describe('#onValueChange()', function() {
    var mock = {
      bootstrapSwitch: Em.K
    };

    beforeEach(function() {
      sinon.spy(mock, 'bootstrapSwitch');
    });

    afterEach(function() {
      mock.bootstrapSwitch.restore();
    });

    it('bootstrapSwitch should not be called', function() {
      view.set('switcher', null);
      view.onValueChange();
      expect(mock.bootstrapSwitch).to.not.be.called;
    });

    it('bootstrapSwitch should be called', function() {
      view.set('switcher', mock);
      view.onValueChange();
      expect(mock.bootstrapSwitch).to.be.calledOnce;
    });
  });

  describe('#observeAllComponentsChecked', function() {

    it('should skip checking child checkboxes', function() {
      view.set('skipCyclicCall', true);
      view.observeAllComponentsChecked();
      expect(view.get('skipCyclicCall')).to.be.false;
    });

    it('should check all child checkboxes', function() {
      view.get('controller').set('componentsConfigsGrouped', [
        Em.Object.create({
          recoveryEnabled: false
        })
      ]);
      view.set('skipCyclicCall', false);
      view.set('allComponentsChecked', true);
      view.observeAllComponentsChecked();
      expect(view.get('controller.componentsConfigsGrouped').mapProperty('recoveryEnabled')).to.be.eql(
        [true]
      );
    });
  });

  describe('#observesEachComponentChecked', function() {

    it('should be false when at least one checkbox unchecked', function() {
      view.set('allComponentsChecked', true);
      view.get('controller').set('componentsConfigsGrouped', [
        Em.Object.create({
          recoveryEnabled: false
        })
      ]);
      view.observesEachComponentChecked();
      expect(view.get('allComponentsChecked')).to.be.false;
    });

    it('should be true when all checked', function() {
      view.set('allComponentsChecked', false);
      view.get('controller').set('componentsConfigsGrouped', [
        Em.Object.create({
          recoveryEnabled: true
        })
      ]);
      view.observesEachComponentChecked();
      expect(view.get('allComponentsChecked')).to.be.true;
    });
  });
});