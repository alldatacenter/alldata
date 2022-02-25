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

/**
 * Set of typical properties for Select View with list of "rows-per-page" options
 * @type {Ember.View}
 */
App.PaginationSelectMixin = Em.Mixin.create({

  content: ['10', '25', '50', '100'],

  attributeBindings: ['disabled'],

  disabled: false,

  change: function () {
    this.get('parentView.dataView').saveDisplayLength();
  }

});

App.PaginationView = Em.View.extend({

  templateName: require('templates/common/pagination'),

  classNames: ['pagination-block', 'pull-right'],

  /**
   * Link to view corresponding to paginated data
   */
  dataViewBinding: 'parentView',

  /**
   * Determines whether links to the first and last pages should be displayed
   * @type {Boolean}
   */
  hasFirstAndLastPageLinks: false,

  /**
   * Determines whether paginated data is loaded
   * Should be bound to property from dataView if necessary
   * @type {Boolean}
   */
  isDataLoaded: true,

  /**
   * Determines whether link to previous page should be disabled
   * @type {Boolean}
   */
  isPreviousDisabled: Em.computed.or('dataView.isCurrentPageFirst', '!isDataLoaded'),

  /**
   * Determines whether link to next page should be disabled
   * @type {Boolean}
   */
  isNextDisabled: Em.computed.or('dataView.isCurrentPageLast', '!isDataLoaded'),

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Em.Select.extend(App.PaginationSelectMixin)

});