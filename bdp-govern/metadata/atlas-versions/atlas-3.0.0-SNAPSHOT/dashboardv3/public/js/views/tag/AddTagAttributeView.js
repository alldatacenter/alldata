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
    'hbs!tmpl/tag/AddTagAttributeView_tmpl',
    'views/tag/TagAttributeItemView',
    'utils/UrlLinks',
    'collection/VTagList'

], function(require, Backbone, AddTagAttributeView_tmpl, TagAttributeItemView, UrlLinks, VTagList) {
    'use strict';

    return Backbone.Marionette.CompositeView.extend(
        /** @lends GlobalExclusionListView */
        {

            template: AddTagAttributeView_tmpl,
            templateHelpers: function() {
                return {
                    create: this.create,
                    description: this.description
                };
            },

            childView: TagAttributeItemView,

            childViewContainer: "[data-id='addAttributeDiv']",

            childViewOptions: function() {
                return {
                    // saveButton: this.ui.saveButton,
                    parentView: this
                };
            },
            /** ui selector cache */
            ui: {
                close: "[data-id='close']",
                attributeId: "[data-id='attributeId']",
                attributeData: "[data-id='attributeData']",
                addAttributeDiv: "[data-id='addAttributeDiv']"
            },
            events: function() {
                var events = {};
                events["click " + this.ui.attributeData] = "onClickAddAttriBtn";
                return events;
            },
            initialize: function(options) {
                _.extend(this, _.pick(options, 'enumDefCollection'));
                this.collection = new Backbone.Collection();
                this.collectionAttribute();
            },
            onRender: function() {
                var that = this;
                this.ui.addAttributeDiv.find('.closeInput').hide();
                if (!('placeholder' in HTMLInputElement.prototype)) {
                    this.ui.addAttributeDiv.find('input,textarea').placeholder();
                }
                that.$('.hide').removeClass('hide');
            },
            bindEvents: function() {},
            collectionAttribute: function() {
                this.collection.add(new Backbone.Model({
                    "name": "",
                    "typeName": "string",
                    "isOptional": true,
                    "cardinality": "SINGLE",
                    "valuesMinCount": 0,
                    "valuesMaxCount": 1,
                    "isUnique": false,
                    "isIndexable": true
                }));

            },
            onClickAddAttriBtn: function() {
                if (this.ui.addAttributeDiv.find("input").length > 0) {
                    this.ui.addAttributeDiv.find('.closeInput').show();
                };
                this.collectionAttribute();
                if (!('placeholder' in HTMLInputElement.prototype)) {
                    this.ui.addAttributeDiv.find('input,textarea').placeholder();
                }
            }
        });
});