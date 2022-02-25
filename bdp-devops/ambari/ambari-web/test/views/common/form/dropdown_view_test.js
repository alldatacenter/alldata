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

describe('#DropdownView', function () {
  var view;
  var prop = {value: 2};
  beforeEach(function () {
    view = App.DropdownView.create({});
  });

  describe('#onValueOrSelectionUpdate', function () {
    var cases = [
      {changedProperty: 'value', findPropertyReturn: null, propertyValue: 1, expectedProperty: 'selection', expectedValue: 1, path: ''},
      {changedProperty: 'value', findPropertyReturn: prop, propertyValue: 1, expectedProperty: 'selection', expectedValue: prop, path: 'value'},
      {changedProperty: 'selection', findPropertyReturn: prop, propertyValue: prop, expectedProperty: 'value', expectedValue: 2, path: 'value'},
      {changedProperty: 'selection', findPropertyReturn: prop, propertyValue: prop, expectedProperty: 'value', expectedValue: prop, path: 'Value'}
    ];
    cases.forEach(function (c) {
      it('should set ' + c.changedProperty + ' to '+ c.expectedProperty + ' if ' + c.changedProperty + 'was changed', function () {
        view.set('optionValuePath', c.path);
        view.set('content', {findProperty: function () {return c.findPropertyReturn}});
        view.set(c.changedProperty, c.propertyValue);
        expect(view.get(c.expectedProperty)).to.eql(c.expectedValue);
      });
    });
  });

  describe('#selectOption', function () {
    it('should change selection property using context prop', function () {
      view.selectOption({context: prop});
      expect(view.get('selection')).to.eql(prop);
    });

    it('should call change method', function () {
      sinon.stub(view, 'change');
      view.selectOption({context: prop});
      expect(view.change.calledOnce).to.be.true;
      view.change.restore();
    });
  });

  describe('#observeEmptySelection', function () {
    it('should set first property of content if selection is falsy', function () {
      view.set('content', [prop, {value: 3}]);
      expect(view.get('selection')).to.eql(prop);
    });

    it('should not set first property of content if selection is truthly', function () {
      var prop2 = {value: 3};
      view.set('selection', prop2);
      view.set('content', [prop, prop2]);
      expect(view.get('selection')).to.eql(prop2);
    });
  });
});