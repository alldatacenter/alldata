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

define([
    "require",
    "backbone",
    "hbs!tmpl/tag/TagContainerLayoutView_tmpl",
    "utils/Utils",
    "utils/Messages",
    "utils/Globals",
    "utils/UrlLinks",
    "models/VTag"
], function(require, Backbone, TagContainerLayoutViewTmpl, Utils, Messages, Globals, UrlLinks, VTag) {
    "use strict";

    var TagContainerLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends TagContainerLayoutView */
        {
            _viewName: "TagContainerLayoutView",

            template: TagContainerLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RTagLayoutView: "#r_tagLayoutView",
                RTagDetailLayoutView: "#r_tagDetailLayoutView"
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new TagLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this.options, options);
            },
            bindEvents: function() {},
            onRender: function() {
                //this.renderTagLayoutView(this.options);
                this.renderTagDetailLayoutView(this.options);
            },
            renderTagLayoutView: function(options) {
                var that = this;
                require(["views/tag/TagLayoutView"], function(TagLayoutView) {
                    that.RTagLayoutView.show(
                        new TagLayoutView(
                            _.extend(options, {
                                collection: that.options.classificationDefCollection
                            })
                        )
                    );
                });
            },
            renderTagDetailLayoutView: function(options) {
                var that = this;
                require(["views/tag/TagDetailLayoutView"], function(TagDetailLayoutView) {
                    if (that.RTagDetailLayoutView) {
                        that.RTagDetailLayoutView.show(new TagDetailLayoutView(options));
                    }
                });
            }
        }
    );
    return TagContainerLayoutView;
});