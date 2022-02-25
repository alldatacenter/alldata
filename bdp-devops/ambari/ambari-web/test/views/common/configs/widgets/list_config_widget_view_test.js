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
describe('App.ListConfigWidgetView', function () {

  beforeEach(function () {

    view = App.ListConfigWidgetView.create({
      initPopover: Em.K,
      movePopover: Em.K,
      config: Em.Object.create({
        name: 'a.b.c',
        savedValue: '2,1',
        value: '2,1',
        filename: 'f1',
        isFinal: false,
        supportsFinal: true,
        widgetType: 'list-widget',
        stackConfigProperty: Em.Object.create({
          valueAttributes: {
            entries: [
              {
                value: '1',
                label: 'first label',
                description: '1'
              },
              {
                value: '2',
                label: 'second label',
                description: '2'
              },
              {
                value: '3',
                label: 'third label',
                description: '3'
              },
              {
                value: '4',
                label: '4th label',
                description: '4'
              },
              {
                value: '5',
                label: '4th label',
                description: '5'
              }
            ],
            selection_cardinality: '3'
          }
        })
      }),
      controller: App.MainServiceInfoConfigsController.create({})
    });
    view.config.set('validate', App.ServiceConfigProperty.prototype.validate.bind(view.config));
    view.config.set('validateErrors', App.ServiceConfigProperty.prototype.validateErrors.bind(view.config));
    view.willInsertElement();
    view.didInsertElement();

  });

  describe('#displayVal', function () {

    it('init value', function () {
      expect(view.get('displayVal')).to.equal('second label, first label');
    });

    it('deselect all', function () {
      view.get('options').setEach('isSelected', false);
      expect(view.get('displayVal')).to.equal(Em.I18n.t('services.service.widgets.list-widget.nothingSelected'));
    });

    it('check that value is trimmed', function () {
      view.get('options').setEach('isSelected', true);
      expect(view.get('displayVal').endsWith(' ...')).to.be.true;
    });

  });

  describe('#calculateOptions', function () {

    it('should create options for each entry', function () {
      view.set('options', []);
      view.calculateOptions();
      expect(view.get('options.length')).to.equal(view.get('config.stackConfigProperty.valueAttributes.entries.length'));
    });

    it('should selected options basing on `value`-property', function () {
      expect(view.get('options').mapProperty('isSelected')).to.eql([true, true, false, false, false]);
    });

    it('should set order to the options basing on `value`-property', function () {
      expect(view.get('options').mapProperty('order')).to.eql([2, 1, 0, 0, 0]);
    });

    it('should disable options basing on `valueAttributes.selection_cardinality`-property', function () {
      expect(view.get('options').everyProperty('isDisabled', false)).to.be.true;
    });

  });

  describe('#calculateInitVal', function () {

    it('should take only selected options', function () {
      expect(view.get('val').length).to.equal(2);
    });

    it('should set `val` empty if `value` is empty', function() {
      view.set('val', [{}]);
      view.set('config.value', '');
      view.calculateInitVal();
      expect(view.get('val')).to.eql([]);
    });

  });

  describe('#calculateVal', function () {
    beforeEach(function() {
      sinon.stub(view, 'sendRequestRorDependentConfigs', Em.K)
    });
    afterEach(function() {
      view.sendRequestRorDependentConfigs.restore();
    });
    it('value updates if some option', function () {
      var options = view.get('options');
      view.toggleOption({context: options[2]});
      expect(view.get('config.value')).to.equal('2,1,3');
      view.toggleOption({context: options[1]});
      expect(view.get('config.value')).to.equal('1,3');
      view.toggleOption({context: options[1]});
      expect(view.get('config.value')).to.equal('1,3,2');
    });

  });

  describe('#restoreValue', function () {

    beforeEach(function() {
      sinon.stub(view, 'restoreDependentConfigs', Em.K);
      sinon.stub(view.get('controller'), 'removeCurrentFromDependentList', Em.K);
      sinon.stub(view, 'sendRequestRorDependentConfigs', function() {return {
        done: function() {}
      }});
    });
    afterEach(function() {
      view.restoreDependentConfigs.restore();
      view.get('controller.removeCurrentFromDependentList').restore();
      view.sendRequestRorDependentConfigs.restore();
    });
    it('should restore saved value', function () {
      var options = view.get('options');
      view.toggleOption({context: options[0]});
      view.toggleOption({context: options[1]});
      view.toggleOption({context: options[2]});
      expect(view.get('config.value')).to.equal('3');
      view.restoreValue();
      expect(view.get('config.value')).to.equal('2,1');
      expect(view.get('controller.removeCurrentFromDependentList')).to.be.called;
    });

  });

  describe('#toggleOption', function () {

    beforeEach(function() {
      sinon.stub(view, 'sendRequestRorDependentConfigs', Em.K);
      view.toggleOption({context: view.get('options')[2]});
    });
    afterEach(function() {
      view.sendRequestRorDependentConfigs.restore();
    });

    describe('should doesn\'t do nothing if maximum number of options is selected', function () {

      it('isSelected', function () {
        expect(view.get('options')[2].get('isSelected')).to.be.true;
        expect(view.get('options')[3].get('isSelected')).to.be.false;
        expect(view.get('options')[4].get('isSelected')).to.be.false;
      });

      it('isDisabled', function () {
        expect(view.get('options')[3].get('isDisabled')).to.be.true;
        expect(view.get('options')[4].get('isDisabled')).to.be.true;
      });

    });

    it('should doesn\'t do nothing if maximum number of options is selected (2)', function () {

      view.toggleOption({context: view.get('options')[3]});
      expect(view.get('options')[3].get('isDisabled')).to.be.true;
      expect(view.get('options')[3].get('isSelected')).to.be.false;
    });

  });

  describe('#checkSelectedItemsCount', function () {

    beforeEach(function () {
      view.set('config.stackConfigProperty.valueAttributes.selection_cardinality', '1+');
      view.parseCardinality();
    });

    it('should check minimum count of the selected items', function () {
      view.get('options').setEach('isSelected', false);
      expect(view.get('config.errorMessage')).to.have.property('length').that.is.least(1);
      view.get('options').setEach('isSelected', true);
      expect(view.get('config.errorMessage')).to.equal('');
    });
  });

});
