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

App.SearchBoxView = Em.View.extend({
  templateName: require('templates/main/host/combo_search_box'),
  errMsg: '',
  classNames: ['col-sm-12'],
  keyFilterMap: [],

  didInsertElement: function () {
    this.initVS();
    this.restoreComboFilterQuery();
    this.showHideClearButton();
    this.initOpenVSButton();
  },

  showErrMsg: function(category) {
    this.set('errMsg', category.attributes.value + " " + Em.I18n.t('hosts.combo.search.invalidCategory'));
  },

  clearErrMsg: function() {
    this.set('errMsg', '')
  },

  initOpenVSButton: function() {
    $('.VS-open-box button').click(function() {
      $('.VS-open-box .popup-arrow-up, .search-box-row').toggleClass('hide');
    });
  },

  initVS: function() {
    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      delay: 500,
      placeholder: Em.I18n.t('common.search'),
      unquotable: [
        'text'
      ],
      callbacks: {
        search: this.search.bind(this),
        facetMatches: this.facetMatches.bind(this),
        valueMatches: this.valueMatches.bind(this)
      }
    });
  },

  /**
   * 'search' callback for visualsearch.js
   * @param query
   * @param searchCollection
   */
  search: function (query, searchCollection) {
    this.clearErrMsg();
    this.showHideClearButton();
    var invalidFacet = this.findInvalidFacet(searchCollection);
    if (invalidFacet) {
      this.showErrMsg(invalidFacet);
    }
    var tableView = this.get('parentView');
    App.db.setComboSearchQuery(tableView.get('controller.name'), query);
    var filterConditions = this.createFilterConditions(searchCollection);
    tableView.updateComboFilter(filterConditions);
  },

  /**
   * 'facetMatches' callback for visualsearch.js
   * @param callback
   */
  facetMatches: function (callback) {
    callback(this.get('keyFilterMap').mapProperty('label'), {preserveOrder: true});
  },

  valueMatches: Em.K,

  /**
   *
   * @param {Array} values
   * @param {string} facetValue
   */
  rejectUsedValues: function(values, facetValue) {
    return values.reject(function (item) {
      return visualSearch.searchQuery.values(facetValue).indexOf(item) >= 0;
    })
  },

  /**
   *
   * @param {object} searchCollection
   * @returns {!object}
   */
  findInvalidFacet: function(searchCollection) {
    const map = this.get('keyFilterMap');
    return searchCollection.models.find((facet) => {
      return !map.someProperty('label', facet.attributes.category);
    });
  },

  showHideClearButton: function () {
    if (visualSearch.searchQuery.length > 0) {
      $('.VS-cancel-search-box').removeClass('hide');
    } else {
      $('.VS-cancel-search-box').addClass('hide');
    }
  },

  restoreComboFilterQuery: function() {
    const query = App.db.getComboSearchQuery(this.get('parentView.controller.name'));
    if (query) {
      visualSearch.searchBox.setQuery(query);
    }
  },

  /**
   *
   * @param {object} searchCollection
   * @returns {Array}
   */
  createFilterConditions: function (searchCollection) {
    const filterConditions = [];
    const map = this.get('keyFilterMap');

    searchCollection.models.forEach((model) => {
      const filter = model.attributes;
      const category = map.findProperty('label', filter.category);
      if (category) {
        filterConditions.push({
          skipFilter: false,
          iColumn: category.column,
          value: this.mapLabelToValue(category.key, filter.value),
          type: category.type
        });
      }
    });
    return filterConditions;
  },

  /**
   *
   * @param {string} category
   * @param {string} label
   * @returns {string}
   */
  mapLabelToValue: function(category, label) {
    return label;
  }
});
