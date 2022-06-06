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

describe('App.EditableList', function () {

  var view;

  beforeEach(function () {
    view = App.EditableList.create();
  });

  describe('#init', function () {
    it('should call updateItemsOriginal and set correct initial values', function () {
      sinon.stub(view, 'updateItemsOriginal');
      view.init();
      expect(view.updateItemsOriginal.calledOnce).to.be.true;
      expect(view.get('input')).to.be.equal('');
      expect(view.get('editMode')).to.be.equal(false);
      view.updateItemsOriginal.restore();
    });
  });

  describe('#updateItemsOriginal', function () {
    it('should update original items whe intems are changed', function () {
      var items = ['item1', 'item2'];
      view.set('items', items);
      expect(view.get('itemsOriginal')).to.be.eql(items);
    });
  });

  describe('#onPrimary', function () {
    it('should set value to empty string, edit mode to false and call event stop propagation', function () {
      var e = {stopPropagation: function(){}};
      sinon.stub(e, 'stopPropagation');
      view.onPrimary(e);
      expect(view.get('input')).to.be.equal('');
      expect(view.get('editMode')).to.be.equal(false);
      expect(e.stopPropagation.calledOnce).to.be.true;
      e.stopPropagation.restore();
    });
  });

  describe('#onSecondary', function () {
    it('should set vinitial values', function () {
      view.set('items', ['test1']);
      view.set('itemsOriginal', ['test1', 'test2']);
      view.onSecondary();
      expect(view.get('input')).to.be.equal('');
      expect(view.get('editMode')).to.be.equal(false);
      expect(view.get('items')).to.be.eql(['test1', 'test2']);
    });
  });

  describe('#enableEditMode', function () {
    it('sholud set empty string to value and set edit mode to true', function () {
      view.enableEditMode();
      expect(view.get('editMode')).to.be.equal(true);
      expect(view.get('input')).to.be.equal('');
    });
  });

  describe('#removeFromItems', function () {
    it('should remove item and set input to empty string', function () {
      view.set('items', ['test1', 'test2']);
      view.removeFromItems({context: 'test1'});
      expect(view.get('items')[0]).to.be.equal('test2');
      expect(view.get('input')).to.be.equal('');
    });
  });

  describe('#availableItemsToAdd', function () {
    it('should return copy of all resources which are not in current list', function () {
      view.set('resources', [{name:'test1'}, {name:'test2'}, {name:'test3'}]);
      view.set('items', [{name:'test1'}]);
      expect(view.get('availableItemsToAdd').length).to.be.equal(2);
    });

    it('should return copy of all resources which are not in current list and in current input', function () {
      view.set('input', '3');
      view.set('resources', [{name:'test1'}, {name:'test2'}, {name:'test3'}]);
      view.set('items', [{name:'test1'}]);
      expect(view.get('availableItemsToAdd').length).to.be.equal(1);
    });
  });

  describe('#addItem', function () {
    it('should add event context ot the list', function () {
      view.set('items', ['test1']);
      view.addItem({context: 'test2'});
      expect(view.get('items').length).to.be.equal(2);
    });
  });
});