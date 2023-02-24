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
    'hbs!tmpl/glossary/GlossaryContainerLayoutView_tmpl'
], function(require, Backbone, GlossaryContainerLayoutViewTmpl) {
    'use strict';

    var GlossaryContainerLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends GlossaryContainerLayoutView */
        {
            _viewName: 'GlossaryContainerLayoutView',

            template: GlossaryContainerLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RGlossaryLayoutView: "#r_glossaryLayoutView",
                RGlossaryDetailLayoutView: "#r_glossaryDetailLayoutView"
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            bindEvents: function() {
                var that = this;
                if (this.options.categoryEvent) {
                    this.options.categoryEvent.on("Success:Category", function(options) {
                        console.log("categoryEvent:");
                        that.renderGlossaryDetailLayoutView(that.options);
                    })
                }
            },
            /**
             * intialize a new GlossaryContainerLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this.options, options);
                this.bindEvents();
            },
            onRender: function() {
                this.renderGlossaryDetailLayoutView(this.options);
            },
            manualRender: function(options) {
                _.extend(this.options, options);
                this.renderGlossaryDetailLayoutView(this.options);
            },
            onBeforeDestroy: function() {
                this.options.categoryEvent.off("Success:Category")
            },
            loadGlossaryLayoutView: function(isSuccessCategory) {
                if (this.options.value.gType == "category") {
                    return;
                }
                this.renderGlossaryDetailLayoutView(this.options);
            },
            renderGlossaryDetailLayoutView: function(options) {
                var that = this;
                require(["views/glossary/GlossaryDetailLayoutView"], function(GlossaryDetailLayoutView) {
                    if (!that.isDestroyed) {
                        that.RGlossaryDetailLayoutView.show(new GlossaryDetailLayoutView(options));
                    }
                });
            }
        });
    return GlossaryContainerLayoutView;
});