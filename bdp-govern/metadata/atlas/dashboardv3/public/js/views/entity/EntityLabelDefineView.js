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
    'hbs!tmpl/entity/EntityLabelDefineView_tmpl',
    'models/VEntity',
    'utils/Utils',
    'utils/Messages',
    'utils/Enums',
    'utils/UrlLinks',
    'utils/CommonViewFunction',
], function(require, Backbone, EntityLabelDefineView_tmpl, VEntity, Utils, Messages, Enums, UrlLinks, CommonViewFunction) {
    'use strict';

    return Backbone.Marionette.LayoutView.extend({
        _viewName: 'REntityLabelDefineView',
        template: EntityLabelDefineView_tmpl,
        templateHelpers: function() {
            return {
                swapItem: this.swapItem,
                labels: this.labels,
                saveLabels: this.saveLabels,
                readOnlyEntity: this.readOnlyEntity,
                div_1: this.dynamicId_1,
                div_2: this.dynamicId_2
            };
        },
        ui: {
            addLabelOptions: "[data-id='addLabelOptions']",
            addLabels: "[data-id='addLabels']",
            saveLabels: "[data-id='saveLabels']",
            cancel: "[data-id='cancel']",
            labelsHeader: ".labelsPanel .panel-heading"
        },
        events: function() {
            var events = {};
            events["change " + this.ui.addLabelOptions] = 'onChangeLabelChange';
            events["click " + this.ui.addLabels] = 'handleBtnClick';
            events["click " + this.ui.saveLabels] = 'saveUserDefinedLabels';
            events["click " + this.ui.cancel] = 'onCancelClick';
            events["click " + this.ui.labelsHeader] = 'onHeaderClick';
            return events;
        },
        initialize: function(options) {
            var self = this;
            _.extend(this, _.pick(options, 'entity', 'customFilter', 'renderAuditTableLayoutView'));
            this.swapItem = false, this.saveLabels = false;
            this.readOnlyEntity = this.customFilter === undefined ? Enums.entityStateReadOnly[this.entity.status] : this.customFilter;
            this.entityModel = new VEntity(this.entity);
            this.labels = this.entity.labels || [];
            this.dynamicId_1 = CommonViewFunction.getRandomIdAndAnchor();
            this.dynamicId_2 = CommonViewFunction.getRandomIdAndAnchor();
        },
        onRender: function() {
            this.populateLabelOptions();
        },
        bindEvents: function() {},
        onHeaderClick: function() {
            var that = this;
            $(".labelsPanel").on("hidden.bs.collapse", function() {
                that.labels = that.entityModel.get("labels") || [];
                that.swapItem = false;
                that.saveLabels = false;
                that.render();
                if (that.labels.length > 0) {
                    $('.labelsPanel').find(that.ui.labelsHeader.attr('href')).removeClass('in');
                    that.ui.labelsHeader.addClass('collapsed').attr('aria-expanded', false);
                }
            });
        },
        populateLabelOptions: function() {
            var that = this,
                str = this.labels.map(function(label) {
                    return "<option selected > " + _.escape(label) + " </option>";
                });
            this.ui.addLabelOptions.html(str);
            var getLabelData = function(data, selectedData) {
                if (data.suggestions.length) {
                    return _.map(data.suggestions, function(name, index) {
                        var findValue = _.find(selectedData, { id: name })
                        if (findValue) {
                            return findValue;
                        } else {
                            return {
                                id: name,
                                text: name
                            }
                        }
                    });
                } else {
                    var findValue = _.find(selectedData, { id: data.prefixString })
                    return findValue ? [findValue] : [];
                }
            };
            this.ui.addLabelOptions.select2({
                placeholder: "Select Label",
                allowClear: false,
                tags: true,
                multiple: true,
                ajax: {
                    url: UrlLinks.searchApiUrl('suggestions'),
                    dataType: 'json',
                    delay: 250,
                    data: function(params) {
                        return {
                            prefixString: params.term, // search term
                            fieldName: '__labels'
                        };
                    },
                    processResults: function(data, params) {
                        return { results: getLabelData(data, this.$element.select2("data")) };
                    },
                    cache: true
                },
                createTag: function(data) {
                    var found = _.find(this.$element.select2("data"), { id: data.term });
                    if (!found) {
                        return { id: data.term, text: data.term };
                    }
                },
                templateResult: this.formatResultSearch
            });
        },
        formatResultSearch: function(state) {
            if (!state.id) {
                return state.text;
            }
            if (!state.element && state.text.trim() !== "") {
                return $("<span>Add<strong> '" + _.escape(state.text) + "'</strong></span>");
            }
        },
        onChangeLabelChange: function() {
            this.labels = this.ui.addLabelOptions.val();
        },
        handleBtnClick: function() {
            this.swapItem = !this.swapItem;
            if (this.customFilter === undefined) {
                this.saveLabels = this.swapItem === true ? true : false;
            } else {
                this.saveLabels = false;
            }
            this.render();
        },
        onCancelClick: function() {
            this.labels = this.entityModel.get("labels") || [];
            this.swapItem = false;
            this.saveLabels = false;
            this.render();
        },
        saveUserDefinedLabels: function() {
            var that = this;
            var entityJson = that.entityModel.toJSON();
            if ((entityJson.labels && entityJson.labels.length !== 0) || this.labels.length !== 0) {
                var payload = this.labels;
                that.entityModel.saveEntityLabels(entityJson.guid, {
                    data: JSON.stringify(payload),
                    type: 'POST',
                    success: function() {
                        var msg = entityJson.labels === undefined ? 'addSuccessMessage' : 'editSuccessMessage',
                            caption = "One or more label";
                        if (payload.length === 0) {
                            msg = 'removeSuccessMessage';
                            caption = "One or more existing label";
                            that.entityModel.unset('labels');
                        } else {
                            that.entityModel.set('labels', payload);
                        }
                        Utils.notifySuccess({
                            content: caption + Messages.getAbbreviationMsg(true, msg)
                        });
                        that.swapItem = false;
                        that.saveLabels = false;
                        that.render();
                        if (that.renderAuditTableLayoutView) {
                            that.renderAuditTableLayoutView();
                        }
                    },
                    error: function(e) {
                        that.ui.saveLabels.attr("disabled", false);
                        Utils.notifySuccess({
                            content: e.message
                        });
                    },
                    complete: function() {
                        that.ui.saveLabels.attr("disabled", false);
                        that.render();
                    }
                });
            }
        }
    });
});