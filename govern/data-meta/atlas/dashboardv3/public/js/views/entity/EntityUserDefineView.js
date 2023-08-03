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
    'hbs!tmpl/entity/EntityUserDefineView_tmpl',
    'models/VEntity',
    'utils/Utils',
    'utils/Enums',
    'utils/Messages',
    'utils/CommonViewFunction',
], function(require, Backbone, EntityUserDefineView_tmpl, VEntity, Utils, Enums, Messages, CommonViewFunction) {
    'use strict';

    return Backbone.Marionette.LayoutView.extend({
        _viewName: 'EntityUserDefineView',
        template: EntityUserDefineView_tmpl,
        templateHelpers: function() {
            return {
                customAttibutes: this.customAttibutes,
                readOnlyEntity: this.readOnlyEntity,
                swapItem: this.swapItem,
                saveAttrItems: this.saveAttrItems,
                divId_1: this.dynamicId_1,
                divId_2: this.dynamicId_2
            };
        },
        ui: {
            addAttr: "[data-id='addAttr']",
            saveAttrItems: "[data-id='saveAttrItems']",
            cancel: "[data-id='cancel']",
            addItem: "[data-id='addItem']",
            userDefineHeader: ".userDefinePanel .panel-heading"
        },
        events: function() {
            var events = {};
            events["click " + this.ui.addAttr] = 'onAddAttrClick';
            events["click " + this.ui.addItem] = 'onAddAttrClick';
            events["click " + this.ui.saveAttrItems] = 'onEditAttrClick';
            events["click " + this.ui.cancel] = 'onCancelClick';
            events["click " + this.ui.userDefineHeader] = 'onHeaderClick';
            return events;
        },
        initialize: function(options) {
            _.extend(this, _.pick(options, 'entity', 'customFilter', 'renderAuditTableLayoutView'));
            this.userDefineAttr = this.entity && this.entity.customAttributes || [];
            this.initialCall = false;
            this.swapItem = false, this.saveAttrItems = false;
            this.readOnlyEntity = this.customFilter === undefined ? Enums.entityStateReadOnly[this.entity.status] : this.customFilter;
            this.entityModel = new VEntity(this.entity);
            this.dynamicId_1 = CommonViewFunction.getRandomIdAndAnchor();
            this.dynamicId_2 = CommonViewFunction.getRandomIdAndAnchor();
            this.generateTableFields();
        },
        onRender: function() {},
        renderEntityUserDefinedItems: function() {
            var that = this;
            require(['views/entity/EntityUserDefineItemView'], function(EntityUserDefineItemView) {
                that.itemView = new EntityUserDefineItemView({ items: that.customAttibutes, updateButtonState: that.updateButtonState.bind(that) });
                that.REntityUserDefinedItemView.show(that.itemView);
            });
        },
        bindEvents: {},
        addChildRegion: function() {
            this.addRegions({
                REntityUserDefinedItemView: "#r_entityUserDefinedItemView"
            });
            this.renderEntityUserDefinedItems();
        },
        onHeaderClick: function() {
            var that = this;
            $(".userDefinePanel").on("hidden.bs.collapse", function() {
                that.swapItem = false;
                that.saveAttrItems = false;
                that.initialCall = false;
                that.render();
                if (that.customAttibutes.length > 0) {
                    $('.userDefinePanel').find(that.ui.userDefineHeader.attr('href')).removeClass('in');
                    that.ui.userDefineHeader.addClass('collapsed').attr('aria-expanded', false);
                }
            });
        },
        onAddAttrClick: function() {
            this.swapItem = !this.swapItem;
            if (this.customFilter === undefined) {
                this.saveAttrItems = this.swapItem === true ? true : false;
            } else {
                this.saveAttrItems = false;
            }
            this.initialCall = true;
            this.render();
            if (this.swapItem === true) {
                this.addChildRegion();
            }
        },
        generateTableFields: function() {
            var that = this;
            this.customAttibutes = [];
            _.each(Object.keys(that.userDefineAttr), function(key, i) {
                that.customAttibutes.push({
                    key: key,
                    value: that.userDefineAttr[key]
                });
            });
        },
        onEditAttrClick: function() {
            this.initialCall = this.customAttibutes.length > 0 ? false : true;
            this.setAttributeModal(this.itemView);
        },
        updateButtonState: function() {
            if (this.customAttibutes.length === 0) {
                this.swapItem = false;
                this.saveAttrItems = false;
                this.render();
            } else {
                return false;
            }
        },
        onCancelClick: function() {
            this.initialCall = false;
            this.swapItem = false;
            this.saveAttrItems = false;
            this.render();
        },
        structureAttributes: function(list) {
            var obj = {}
            list.map(function(o) {
                obj[o.key] = o.value;
            });
            return obj;
        },
        saveAttributes: function(list) {
            var that = this;
            var entityJson = that.entityModel.toJSON();
            var properties = that.structureAttributes(list);
            entityJson.customAttributes = properties;
            var payload = { entity: entityJson };
            that.entityModel.createOreditEntity({
                data: JSON.stringify(payload),
                type: 'POST',
                success: function() {
                    var msg = that.initialCall ? 'addSuccessMessage' : 'editSuccessMessage',
                        caption = "One or more user-defined propertie"; // 's' will be added in abbreviation function
                    that.customAttibutes = list;
                    if (list.length === 0) {
                        msg = 'removeSuccessMessage';
                        caption = "One or more existing user-defined propertie";
                    }
                    Utils.notifySuccess({
                        content: caption + Messages.getAbbreviationMsg(true, msg)
                    });
                    that.swapItem = false;
                    that.saveAttrItems = false;
                    that.render();
                    if (that.renderAuditTableLayoutView) {
                        that.renderAuditTableLayoutView();
                    }
                },
                error: function(e) {
                    that.initialCall = false;
                    Utils.notifySuccess({
                        content: e.message
                    });
                    that.ui.saveAttrItems.attr("disabled", false);
                },
                complete: function() {
                    that.ui.saveAttrItems.attr("disabled", false);
                    that.initialCall = false;
                }
            });
        },
        setAttributeModal: function(itemView) {
            var self = this;
            this.ui.saveAttrItems.attr("disabled", true);
            var list = itemView.$el.find("[data-type]"),
                dataList = [];
            Array.prototype.push.apply(dataList, itemView.items);
            var field = CommonViewFunction.CheckDuplicateAndEmptyInput(list, dataList);
            if (field.validation && !field.hasDuplicate) {
                self.saveAttributes(itemView.items);
            } else {
                this.ui.saveAttrItems.attr("disabled", false);
            }
        }
    });
});