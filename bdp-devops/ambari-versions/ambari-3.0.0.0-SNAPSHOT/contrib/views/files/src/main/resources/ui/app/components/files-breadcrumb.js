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

export default Ember.Component.extend({
  path: '',
  collapseAt: 4,
  tagName: 'ul',
  classNames: ['breadcrumb'],
  collapsingRequired: false,
  collapsedCrumbs: [],
  expandedCrumbs: [],

  crumbs: Ember.on('init', Ember.observer('path', function() {
    var path = this.get('path');
    var currentPath = path.split('/').filter((entry) => { return !Ember.isBlank(entry) });
    currentPath.unshift("/");
    var that = this;
    var shouldCollapse = function(scope, array, index) {
      return (((array.length - 1) >= scope.get('collapseAt')) && (index < array.length - 2));
    };
    var getCrumb = function(index, allCrumbs) {
      return {name: allCrumbs[index], path: "/" + allCrumbs.slice(1, index + 1).join('/'), last: false};
    };

    var collapsedCrumbs = currentPath.map(function(curr, i, array) {
      if(shouldCollapse(that, array, i)) {
        return getCrumb(i, array);
      } else {
        return {};
      }
    }).filterBy('name');

    var crumbs = currentPath.map(function(curr, i, array) {
      if(!shouldCollapse(that, array, i)) {
        return getCrumb(i, array);
      } else {
        return {};
      }
    }).filterBy('name');

    crumbs.set('lastObject.last', true);

    if (collapsedCrumbs.length > 0) {
      this.set('collapsingRequired', true);
    } else {
      this.set('collapsingRequired', false);
    }
    this.set('collapsedCrumbs', collapsedCrumbs.reverse());
    this.set('expandedCrumbs', crumbs);
  }))

});
