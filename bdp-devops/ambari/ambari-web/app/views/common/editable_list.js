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

App.EditableList = Ember.View.extend({

  templateName: require('templates/common/editable_list'),

  items: [], // items show on list
  resources: [], // original resources including all items
  itemsOriginal: [], //backup of items

  editMode: false,
  input: '',
  typeahead: [],
  selectedTypeahed: 0,

  isCaseSensitive: true,

  init: function () {
    this._super();
    this.updateItemsOriginal();
    this.set('input', '');
    this.set('editMode', false);
  },

  updateItemsOriginal: function () {
    this.set('itemsOriginal', Em.copy(this.get('items')));
  }.observes('items'),

  onPrimary: function (event) {
    this.set('editMode', false);
    this.set('input', '');
    if(event){
      event.stopPropagation();
    }
  },

  onSecondary: function () {
    // restore all items
    this.set('items', this.get('itemsOriginal'));
    this.set('input', '');
    this.set('editMode', false);
  },

  enableEditMode: function() {
    this.set('input', '');
    this.set('editMode', true);
  },

  removeFromItems: function(event) {
    var items = this.get('items');
    items.removeObject(event.context);
    this.set('input', '');
  },

  /**
   * available items to add, will show up typing ahead
   */
  availableItemsToAdd: function () {
    var allItems = Em.copy(this.get('resources'));
    var isCaseSensitive = this.get('isCaseSensitive');
    var input = isCaseSensitive ? this.get('input') : this.get('input').toLowerCase();
    var toRemove = [];
    var existed = this.get('items');
    allItems.forEach(function(item) {
      var nameToCompare = isCaseSensitive ? item.name : item.name.toLowerCase();
      if (nameToCompare.indexOf(input) < 0 || existed.findProperty('name', item.name)) {
        toRemove.push(item);
      }
    });
    toRemove.forEach(function(item) {
      allItems.removeObject(item);
    });
    return allItems;
  }.property('items', 'input'),

  addItem: function(event) {
    var items = this.get('items');
    var toAdd;
    if (event.context) {
      toAdd = event.context;
    } else if (this.get('typeahead.length') > 0){
      toAdd = this.get('typeahead')[this.get('selectedTypeahead')];
    }
    items.pushObject(event.context);
    this.set('input', '');
  },

  updateTypeahead: function() {
    var newValue = this.get('input');
    var self = this;
    if(newValue){
      var newValue = newValue.split(',');
      if( newValue.length > 1){
        // If coma separated string, then just add all items to list
        newValue.forEach( function(item) {
          self.addItem(item);
        });
        self.clearInput();
      } else {
        // Load typeahed items based on current input
        var items = self.get('availableItemsToAdd');
        self.set('typeahead', items);
        self.set('selectedTypeahed', 0);
      }
    } else {
      self.set('typeahead', []);
      self.set('selectedTypeahed', 0);
    }

  }.observes('input')
});
