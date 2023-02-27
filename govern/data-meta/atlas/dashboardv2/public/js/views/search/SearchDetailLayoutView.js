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

define(['require',
    'backbone',
    'hbs!tmpl/search/SearchDetailLayoutView_tmpl',
], function(require, Backbone, SearchDetailLayoutViewTmpl) {
    'use strict';

    var SearchDetailLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchDetailLayoutView */
        {
            _viewName: 'SearchDetailLayoutView',

            template: SearchDetailLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RSearchResultLayoutView: "#r_searchResultLayoutView"
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new SearchDetailLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'initialView', 'classificationDefCollection', 'entityDefCollection', 'typeHeaders', 'searchVent', 'enumDefCollection', 'searchTableColumns'));
            },
            bindEvents: function() {},
            onRender: function() {
                this.renderSearchResultLayoutView();
            },
            renderSearchResultLayoutView: function() {
                var that = this;
                require(['views/search/SearchResultLayoutView'], function(SearchResultLayoutView) {
                    if (that.RSearchResultLayoutView) {
                        that.RSearchResultLayoutView.show(new SearchResultLayoutView(that.options));
                    }
                });
            }
        });
    return SearchDetailLayoutView;
});