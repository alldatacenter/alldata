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
    "hbs!tmpl/business_metadata/BusinessMetadataContainerLayoutView_tmpl"
], function(require, Backbone, BusinessMetadataContainerLayoutViewTmpl) {
    "use strict";

    var BusinessMetadataContainerLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends BusinessMetadataContainerLayoutView */
        {
            _viewName: "BusinessMetadataContainerLayoutView",

            template: BusinessMetadataContainerLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RBusinessMetadataDetailContainer: "#r_businessMetadataDetailContainer",
                RBusinessMetadataAttrContainer: "#r_businessMetadataAttrContainer"
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
                _.extend(this, options);
            },
            bindEvents: function() {},
            onRender: function() {
                this.updateView();
            },
            updateView: function() {
                this.model = this.businessMetadataDefCollection.fullCollection.findWhere({ guid: this.guid });
                this.renderBusinessMetadataDetailLayoutView();
                this.renderBusinessMetadataAttrLayoutView();
            },
            renderBusinessMetadataDetailLayoutView: function() {
                var that = this;
                require(["views/business_metadata/BusinessMetadataDetailLayoutView"], function(BusinessMetadataDetailLayoutView) {
                    if (that.isDestroyed) {
                        return;
                    }
                    that.RBusinessMetadataDetailContainer.show(new BusinessMetadataDetailLayoutView({
                        businessMetadataDefCollection: that.businessMetadataDefCollection,
                        guid: that.guid,
                        model: that.model,
                        enumDefCollection: that.enumDefCollection,
                        typeHeaders: that.typeHeaders
                    }));
                });
            },
            renderBusinessMetadataAttrLayoutView: function() {
                var that = this;
                require(["views/business_metadata/BusinessMetadataAttrTableLayoutView"], function(BusinessMetadataAttrTableLayoutView) {
                    if (that.isDestroyed) {
                        return;
                    }
                    that.RBusinessMetadataAttrContainer.show(new BusinessMetadataAttrTableLayoutView({
                        businessMetadataDefCollection: that.businessMetadataDefCollection,
                        model: that.model,
                        guid: that.guid,
                        typeHeaders: that.typeHeaders,
                        enumDefCollection: that.enumDefCollection,
                        entityDefCollection: that.entityDefCollection
                    }));
                });
            }
        }
    );
    return BusinessMetadataContainerLayoutView;
});