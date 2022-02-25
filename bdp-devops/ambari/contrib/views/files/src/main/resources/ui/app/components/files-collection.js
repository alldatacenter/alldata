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

import Ember from 'ember';
import { EKMixin, keyUp } from 'ember-keyboard';

export default Ember.Component.extend(EKMixin, {
  minHeight: 600,
  currentWidth: 1000,
  currentHeight: 600,
  columnsConfig: [],
  sortOptions: [-1, 0, 1],
  parentPath: '',
  isEmptyParentPath: Ember.computed('parentPath', function() {
    return Ember.isBlank(this.get('parentPath'));
  }),

  resizeView: Ember.on('init', function() {
    $(window).resize(this.windowResized(this));
  }),

  destroyResizeView: Ember.on('willDestroyElement', function() {
    $(window).off("resize");
  }),

  activateKeyboard: Ember.on('init', function() {
    this.set('keyboardActivated', true);
  }),

  resetAllSelection: Ember.on(keyUp('Escape'), function() {
    this.sendAction('resetSelection');
  }),

  selectAll: Ember.on(keyUp('shift+s'), function() {
    this.sendAction('selectAllAction', false);
  }),

  containerStyle: Ember.computed('currentHeight', function() {
    var height = this.get('currentHeight');
    var style = 'position: relative; height: ' + height + 'px';
    return style.htmlSafe();
  }),

  windowResized: function(scope) {
    return function() {
      Ember.run.later(function() {
        var currentWidth = $("#" + scope.get('containerId')).width();
        var windowHeight = $(window).height();
        var relativeHeight = windowHeight - 220;
        if(relativeHeight < scope.get('minHeight')) {
          relativeHeight = scope.get('minHeight');
        }
        scope.set('currentWidth', currentWidth);
        scope.set('currentHeight', relativeHeight);
      });
    };
  },

  didInsertElement: function() {
    var func = this.windowResized(this);
    func();
  },

  actions: {
    rotateSort: function(column) {
      if(!column['sortable'] || this.get('sortEnabled') !== true) {
        return false;
      }
      var sortOptions = this.get('sortOptions');
      // Resetting the current sort order
      this.get('columnsConfig').forEach(function(entry) {
        if(entry['key'] !== column['key']) {
          Ember.set(entry, 'sortOrder', sortOptions[1]);
        }
      });
      var currentSortOrder = column['sortOrder'];
      var currentSortOrderIndex = sortOptions.indexOf(currentSortOrder);
      var nextSortOrderIndex = (currentSortOrderIndex + 1) % sortOptions.length;
      Ember.set(column, 'sortOrder', sortOptions[nextSortOrderIndex]);
      this.sendAction('sortAction', column);
    }
  }
});
