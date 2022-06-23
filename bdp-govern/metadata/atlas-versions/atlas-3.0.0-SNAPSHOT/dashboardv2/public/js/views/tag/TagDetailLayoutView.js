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
    'hbs!tmpl/tag/TagDetailLayoutView_tmpl',
], function(require, Backbone, TagDetailLayoutView_tmpl) {
    'use strict';

    var TagDetailLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends TagDetailLayoutView */
        {
            _viewName: 'TagDetailLayoutView',

            template: TagDetailLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RSearchResultLayoutView: "#r_searchResultLayoutView",
                RTagAttributeDetailLayoutView: "#r_TagAttributeDetailLayoutView"
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new TagDetailLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'tag', 'value', 'glossaryCollection', 'classificationDefCollection', 'entityDefCollection', 'typeHeaders', 'enumDefCollection', 'searchVent'));
                this.collection = this.classificationDefCollection;
            },
            bindEvents: function() {},
            onRender: function() {
                this.renderSearchResultLayoutView();
                this.renderTagAttributeCompositeView();
            },
            renderSearchResultLayoutView: function() {
                var that = this;
                require(['views/search/SearchResultLayoutView'], function(SearchResultLayoutView) {
                    var value = {
                        'tag': that.tag,
                        'searchType': 'basic'
                    };
                    if (that.RSearchResultLayoutView) {
                        that.RSearchResultLayoutView.show(new SearchResultLayoutView({
                            value: _.extend({}, that.value, value),
                            entityDefCollection: that.entityDefCollection,
                            typeHeaders: that.typeHeaders,
                            tagCollection: that.collection,
                            enumDefCollection: that.enumDefCollection,
                            classificationDefCollection: that.classificationDefCollection,
                            glossaryCollection: that.glossaryCollection,
                            searchVent: that.searchVent,
                            fromView: "classification"
                        }));
                    }
                });
            },
            renderTagAttributeCompositeView: function() {
                var that = this;
                require(['views/tag/TagAttributeDetailLayoutView'], function(TagAttributeDetailLayoutView) {
                    if (that.RTagAttributeDetailLayoutView) {
                        that.RTagAttributeDetailLayoutView.show(new TagAttributeDetailLayoutView({
                            tag: that.tag,
                            collection: that.collection,
                            enumDefCollection: that.enumDefCollection
                        }));
                    }
                });
            }
        });
    return TagDetailLayoutView;
});