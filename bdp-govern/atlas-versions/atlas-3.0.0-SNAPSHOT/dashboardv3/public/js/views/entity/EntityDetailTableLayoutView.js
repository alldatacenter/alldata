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
    'hbs!tmpl/entity/EntityDetailTableLayoutView_tmpl',
    'utils/CommonViewFunction',
    'models/VEntity',
    'utils/Utils'
], function(require, Backbone, EntityDetailTableLayoutView_tmpl, CommonViewFunction, VEntity, Utils) {
    'use strict';

    var EntityDetailTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends EntityDetailTableLayoutView */
        {
            _viewName: 'EntityDetailTableLayoutView',

            template: EntityDetailTableLayoutView_tmpl,

            templateHelpers: function() {
                return {
                    editEntity: this.editEntity
                };
            },

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                detailValue: "[data-id='detailValue']",
                noValueToggle: "[data-id='noValueToggle']",
                editButton: '[data-id="editButton"]',
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.noValueToggle] = function() {
                    this.showAllProperties = !this.showAllProperties;
                    this.ui.noValueToggle.attr("data-original-title", (this.showAllProperties ? "Hide" : "Show") + " empty values");
                    Utils.togglePropertyRelationshipTableEmptyValues({
                        "inputType": this.ui.noValueToggle,
                        "tableEl": this.ui.detailValue
                    });
                };
                events["click " + this.ui.editButton] = 'onClickEditEntity';
                return events;
            },
            /**
             * intialize a new EntityDetailTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'entity', 'typeHeaders', 'attributeDefs', 'attributes', 'editEntity', 'guid', 'entityDefCollection', 'searchVent', 'fetchCollection'));
                this.entityModel = new VEntity({});
                this.showAllProperties = false;
            },
            bindEvents: function() {},
            onRender: function() {
                this.entityTableGenerate();
            },
            entityTableGenerate: function() {
                var that = this,
                    highlightString = $(".atlas-header .global-search-container input.global-search").val(),
                    table = CommonViewFunction.propertyTable({
                        scope: this,
                        valueObject: _.extend({ "isIncomplete": this.entity.isIncomplete || false }, this.entity.attributes),
                        attributeDefs: this.attributeDefs,
                        highlightString: highlightString
                    });
                this.ui.detailValue.append(table);
                Utils.togglePropertyRelationshipTableEmptyValues({
                    "inputType": this.ui.noValueToggle,
                    "tableEl": this.ui.detailValue
                });
                setTimeout(function() {
                    that.$el.find(".searched-term-highlight").addClass("bold");
                }, 5000)
            },
            onClickEditEntity: function(e) {
                var that = this;
                $(e.currentTarget).blur();
                require([
                    'views/entity/CreateEntityLayoutView'
                ], function(CreateEntityLayoutView) {
                    var view = new CreateEntityLayoutView({
                        guid: that.guid,
                        searchVent: that.searchVent,
                        entityDefCollection: that.entityDefCollection,
                        typeHeaders: that.typeHeaders,
                        callback: function() {
                            that.fetchCollection();
                        }
                    });

                });
            }
        });
    return EntityDetailTableLayoutView;
});