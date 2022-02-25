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

App.Pagination = Ember.Mixin.create({
  actions:{
    selectPage: function(number) {
      this.set('page', number);
    },

    toggleOrder: function() {
      this.toggleProperty('sortAscending');
    }
  },

  page: 1,

  perPage: 10,

  perPageOptions:[10,25,50,100],

  pageWatcher:function () {
    if (this.get('page') > this.get('totalPages')) {
      this.set('page',this.get('totalPages') || 1);
    }
  }.observes('totalPages'),

  totalPages: function() {
    return Math.ceil(this.get('length') / this.get('perPage'));
  }.property('length', 'perPage'),

  pages: function() {
    var collection = Ember.A();

    for(var i = 0; i < this.get('totalPages'); i++) {
      collection.pushObject(Ember.Object.create({
        number: i + 1
      }));
    }

    return collection;
  }.property('totalPages'),

  hasPages: function() {
    return this.get('totalPages') > 1;
  }.property('totalPages'),

  prevPage: function() {
    var page = this.get('page');
    var totalPages = this.get('totalPages');

    if(page > 1 && totalPages > 1) {
      return page - 1;
    } else {
      return null;
    }
  }.property('page', 'totalPages'),

  nextPage: function() {
    var page = this.get('page');
    var totalPages = this.get('totalPages');

    if(page < totalPages && totalPages > 1) {
      return page + 1;
    } else {
      return null;
    }
  }.property('page', 'totalPages'),


  paginatedContent: function() {
    var start = (this.get('page') - 1) * this.get('perPage');
    var end = start + this.get('perPage');

    return this.get('arrangedContent').slice(start, end);
  }.property('page', 'totalPages', 'arrangedContent.[]'),

  paginationInfo: function () {
    var start = (this.get('page') - 1) * this.get('perPage') + 1;
    var end = start + this.get('paginatedContent.length') - 1;
    return start + ' - ' + end + ' of ' + this.get('arrangedContent.length');
  }.property('page', 'arrangedContent.length', 'perPage')
});
