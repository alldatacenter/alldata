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
    'hbs!tmpl/search/RelationSearchDetailLayoutView_tmpl',
], function(require, Backbone, RelationSearchDetailLayoutViewTmpl) {
    'use strict';

    var RelationSearchDetailLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends RelationSearchDetailLayoutView */
        {
            _viewName: 'RelationSearchDetailLayoutView',

            template: RelationSearchDetailLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RRelationSearchResultLayoutView: "#r_relationSearchResultLayoutView"
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new RelationSearchDetailLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, options);
            },
            bindEvents: function() {},
            onRender: function() {
                this.renderRelationSearchResultLayoutView();
            },
            renderRelationSearchResultLayoutView: function() {
                var that = this;
                require(['views/search/RelationSearchResultLayoutView'], function(RelationSearchResultLayoutView) {
                    if (that.RRelationSearchResultLayoutView) {
                        that.RRelationSearchResultLayoutView.show(new RelationSearchResultLayoutView(that.options));
                    }
                });
            }
        });
    return RelationSearchDetailLayoutView;
});