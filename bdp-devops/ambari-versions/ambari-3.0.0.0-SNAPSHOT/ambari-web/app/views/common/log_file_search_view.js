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
var filters = require('views/common/filter_view');

/**
* @augments App.InfiniteScrollMixin
* @type {Em.View}
*/
App.LogFileSearchView = Em.View.extend(App.InfiniteScrollMixin, {
  classNames: ['log-file-search'],
  templateName: require('templates/common/log_file_search'),
  logLevels: ['fatal', 'critical', 'error', 'warning', 'info', 'debug'],

  /**
  * @typedef {Em.Object} FilterKeyword
  * @property {Boolean} isIncluded determines include/exclude status of keyword
  * @property {String} id unique identifier
  * @property {String} value keyword value
  */

  /**
  * Stores all selected keywords.
  *
  * @type {FilterKeyword[]}
  */
  selectedKeywords: [],

  selectedKeywordsDidChange: function() {
    this.fetchContent();
  }.observes('selectedKeywords.length'),

  levelsContext: function() {
    var self = this;
    var levels = this.get('logLevels');

    return Em.A(levels.map(function(level) {
      return Em.Object.create({name: level.toUpperCase(), counter: 0, displayName: level.capitalize(), checked: false});
    }));
  }.property(),

  levelsContextDidChange: function(e) {
    this.fetchContent();
  }.observes('levelsContext.@each.checked'),

  /** mock data **/
  content: function() {
    var data = [{
      message: 'java.lang.NullPointerException',
      date: '05.12.2016, 10:10:20',
      level: 'INFO'
    },
    {
      message: 'java.lang.NullPointerException',
      date: '05.12.2016, 10:10:20',
      level: 'ERROR'
    }];

    var initialSize = 20;
    var ret = [];

    for (var i = 0; i < 20; i++) {
      ret.push(Em.Object.create(data[Math.ceil(Math.random()*2) - 1]));
    }
    return ret;
  }.property(),

  contentDidChange: function() {
    this.refreshLevelCounters();
  }.observes('content.length'),

  dateFromValue: null,
  dateToValue: null,

  keywordsFilterView: filters.createTextView({
    layout: Em.Handlebars.compile('{{yield}}')
  }),

  keywordsFilterValue: null,

  didInsertElement: function() {
    this._super();
    this.infiniteScrollInit(this.$().find('.log-file-search-content'), {
      callback: this.loadMore.bind(this)
    });
    this.$().find('.log-file-search-content').contextmenu({
      target: '#log-file-search-item-context-menu'
    });
    this.refreshLevelCounters();
  },

  /** mock data **/
  loadMore: function() {
    var dfd = $.Deferred();
    var self = this;
    setTimeout(function() {
      var data = self.get('content');
      self.get('content').pushObjects(data.slice(0, 10));
      dfd.resolve();
    }, Math.ceil(Math.random()*4000));
    return dfd.promise();
  },

  refreshLevelCounters: function() {
    var self = this;
    this.get('logLevels').forEach(function(level) {
      var levelContext = self.get('levelsContext').findProperty('name', level.toUpperCase());
      levelContext.set('counter', self.get('content').filterProperty('level', level.toUpperCase()).length);
    });
  },

  /**
  * Make request and get content with applied filters.
  */
  fetchContent: function() {
    console.debug('Make Request with params:', this.serializeFilters());
  },

  submitKeywordsValue: function() {
    this.fetchContent();
  },

  serializeFilters: function() {
    var levels = this.serializeLevelFilters();
    var keywords = this.serializeKeywordsFilter();
    var date = this.serializeDateFilter();
    var includedExcludedKeywords = this.serializeIncludedExcludedKeywordFilter();

    return [levels, keywords, date, includedExcludedKeywords].compact().join('&');
  },

  serializeKeywordsFilter: function() {
    return !!this.get('keywordsFilterValue') ? 'keywords=' + this.get('keywordsFilterValue'): null;
  },

  serializeDateFilter: function() {
    var dateFrom = !!this.get('dateFromValue') ? 'dateFrom=' + this.get('dateFromValue') : null;
    var dateTo = !!this.get('dateToValue') ? 'dateTo=' + this.get('dateFromValue') : null;
    var ret = [dateTo, dateFrom].compact();
    return ret.length ? ret.join('&') : null;
  },

  serializeLevelFilters: function() {
    var selectedLevels = this.get('levelsContext').filterProperty('checked').mapProperty('name');
    return selectedLevels.length ? 'levels=' + selectedLevels.join(',') : null;
  },

  serializeIncludedExcludedKeywordFilter: function() {
    var self = this;
    var getValues = function(included) {
      return self.get('selectedKeywords').filterProperty('isIncluded', included).mapProperty('value');
    };
    var included = getValues(true).join(',');
    var excluded = getValues(false).join(',');
    var ret = [];
    if (included.length) ret.push('include=' + included);
    if (excluded.length) ret.push('exclude=' + excluded);
    return ret.length ? ret.join('&') : null;
  },

  /** include/exclude keywords methods **/

  keywordToId: function(keyword) {
    return keyword.toLowerCase().split(' ').join('_');
  },

  /**
  * Create keyword object
  * @param  {string} keyword keyword value
  * @param  {object} [opts]
  * @return {Em.Object}
  */
  createSelectedKeyword: function(keyword, opts) {
    var defaultOpts = {
      isIncluded: false,
      id: this.keywordToId(keyword),
      value: keyword
    };
    return Em.Object.create($.extend({}, defaultOpts, opts));
  },

  /**
  * Adds keyword if not added.
  * @param  {FilterKeyword} keywordObject
  */
  addKeywordToList: function(keywordObject) {
    if (!this.get('selectedKeywords').someProperty('id', keywordObject.get('id'))) {
      this.get('selectedKeywords').pushObject(keywordObject);
    }
  },

  /**
  * @param  {FilterKeyword} keyword
  */
  includeSelectedKeyword: function(keyword) {
    this.addKeywordToList(this.createSelectedKeyword(keyword, { isIncluded: true }));
  },

  /**
  * @param  {FilterKeyword} keyword
  */
  excludeSelectedKeyword: function(keyword) {
    this.addKeywordToList(this.createSelectedKeyword(keyword, { isIncluded: false }));
  },

  /** view actions **/

  /** toolbar context menu actions **/
  moveTableTop: function(e) {
    var $el = $('.log-file-search-content');
    $el.scrollTop(0);
    $el = null;
  },

  moveTableBottom: function(e) {
    var $el = $('.log-file-search-content');
    $el.scrollTop($el.get(0).scrollHeight);
    $el = null;
  },

  navigateToLogUI: function(e) {
    console.error('navigate to Log UI');
  },

  removeKeyword: function(e) {
    this.get('selectedKeywords').removeObject(e.context);
  },

  /** toolbar reset filter actions **/
  resetKeywordsDateFilter: function(e) {
    this.setProperties({
      keywordsFilterValue: '',
      dateFromValue: '',
      dateToValue: ''
    });
  },

  resetLevelsFilter: function(e) {
    this.get('levelsContext').invoke('set', 'checked', false);
  },

  resetKeywordsFilter: function(e) {
    this.get('selectedKeywords').clear();
  },

  /** log search item context menu actions **/
  includeSelected: function() {
    var selection = window.getSelection().toString();
    if (!!selection) this.includeSelectedKeyword(selection);
  },

  excludeSelected: function() {
    var selection = window.getSelection().toString();
    if (!!selection) this.excludeSelectedKeyword(selection);
  }
});
