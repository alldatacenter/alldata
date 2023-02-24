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
    'hbs!tmpl/business_metadata/BusinessMetadataDetailLayoutView_tmpl',
    'utils/Utils',
], function(require, Backbone, BusinessMetadataDetailLayoutViewTmpl, Utils) {
    'use strict';

    var BusinessMetadataDetailLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends BusinessMetadataDetailLayoutView */
        {
            template: BusinessMetadataDetailLayoutViewTmpl,
            /** Layout sub regions */
            regions: {},
            /** ui selector cache */
            ui: {
                title: '[data-id="title"]',
                description: '[data-id="description"]',
                textType: '[name="textType"]'
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["change " + this.ui.textType] = function(e) {
                    this.isTextTypeChecked = !this.isTextTypeChecked;
                    this.renderDetail();
                };
                return events;
            },
            /**
             * intialize a new BusinessMetadataDetailLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'model'));
                $('body').addClass("detail-page");
                this.isTextTypeChecked = false;
            },
            onRender: function() {
                this.renderDetail();
            },
            renderDetail: function() {
                this.ui.title.html('<span>' + this.model.get('name') + '</span>');
                if (this.model.get('description')) {
                    this.isTextTypeChecked ? this.ui.description.text(this.model.get('description')) : this.ui.description.html(this.model.get('description'));
                }
            },
            onDestroy: function() {
                if (!Utils.getUrlState.isBSDetail()) {
                    $('body').removeClass("detail-page");
                }
            }
        });
    return BusinessMetadataDetailLayoutView;
});