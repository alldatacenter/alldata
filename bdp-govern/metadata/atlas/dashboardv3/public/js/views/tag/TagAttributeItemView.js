/*
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
    'hbs!tmpl/tag/TagAttributeItemView_tmpl'

], function(require, Backbone, TagAttributeItemViewTmpl) {
    'use strict';

    return Backbone.Marionette.ItemView.extend(
        /** @lends GlobalExclusionListView */
        {

            template: TagAttributeItemViewTmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                attributeInput: "[data-id='attributeInput']",
                close: "[data-id='close']",
                dataTypeSelector: "[data-id='dataTypeSelector']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["keyup " + this.ui.attributeInput] = function(e) {
                    this.model.set({ "name": e.target.value.trim() });
                };
                events["change " + this.ui.dataTypeSelector] = function(e) {
                    this.model.set({ "typeName": e.target.value.trim() });
                };
                events["click " + this.ui.close] = 'onCloseButton';
                return events;
            },

            /**
             * intialize a new GlobalExclusionComponentView Layout
             * @constructs
             */
            initialize: function(options) {
                this.parentView = options.parentView;

            },
            onRender: function() {
                var that = this;
                this.parentView.enumDefCollection.fullCollection.each(function(model) {
                    that.ui.dataTypeSelector.append("<option>" + model.get('name') + "</option>");
                });
            },
            onCloseButton: function() {
                var tagName = this.parentView.$el.find('[data-id="tagName"]').val();
                if (this.parentView.collection.models.length > 0) {
                    this.model.destroy();
                }
                if (this.parentView.$el.find('input').length === 1) {
                    $(this.ui.close).hide();
                }
            }
        });
});