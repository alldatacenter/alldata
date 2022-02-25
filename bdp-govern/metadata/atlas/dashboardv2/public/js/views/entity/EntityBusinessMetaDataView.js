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
    "hbs!tmpl/entity/EntityBusinessMetaDataView_tmpl",
    "views/entity/EntityBusinessMetaDataItemView",
    "models/VEntity",
    "utils/Utils",
    "utils/Messages",
    "utils/Enums",
    "utils/CommonViewFunction",
    "moment",
    "utils/Globals"
], function(require, Backbone, EntityBusinessMetaDataView_tmpl, EntityBusinessMetaDataItemView, VEntity, Utils, Messages, Enums, CommonViewFunction, moment, Globals) {
    "use strict";

    return Backbone.Marionette.CompositeView.extend({
        _viewName: "EntityBusinessMetaDataView",
        template: EntityBusinessMetaDataView_tmpl,
        childView: EntityBusinessMetaDataItemView,
        childViewContainer: "[data-id='itemView']",
        childViewOptions: function() {
            return {
                editMode: this.editMode,
                entity: this.entity,
                businessMetadataCollection: this.businessMetadataCollection,
                enumDefCollection: this.enumDefCollection
            };
        },
        templateHelpers: function() {
            return {
                readOnlyEntity: this.readOnlyEntity
            }
        },
        /** ui selector cache */
        ui: {
            addItem: "[data-id='addItem']",
            addBusinessMetadata: "[data-id='addBusinessMetadata']",
            saveBusinessMetadata: "[data-id='saveBusinessMetadata']",
            businessMetadataTree: "[data-id='businessMetadataTree']",
            cancel: "[data-id='cancel']",
            businessMetadataHeader: ".businessMetaDataPanel .panel-heading.main-parent"
        },
        events: function() {
            var events = {};
            events["click " + this.ui.addItem] = 'createNameElement';
            events["click " + this.ui.addBusinessMetadata] = "onAddBusinessMetadata";
            events["click " + this.ui.saveBusinessMetadata] = "onSaveBusinessMetadata";
            events["click " + this.ui.cancel] = "onCancel";
            events["click " + this.ui.businessMetadataHeader] = "onHeaderClick";
            return events;
        },
        initialize: function(options) {
            var that = this;
            _.extend(this, _.pick(options, "entity", "businessMetadataCollection", "enumDefCollection", "guid", "fetchCollection"));
            this.editMode = false;
            this.readOnlyEntity = Enums.entityStateReadOnly[this.entity.status];
            this.$("editBox").hide();
            this.actualCollection = new Backbone.Collection(
                _.map(this.entity.businessAttributes, function(val, key) {
                    var foundBusinessMetadata = that.businessMetadataCollection[key];
                    if (foundBusinessMetadata) {
                        _.each(val, function(aVal, aKey) {
                            var foundAttr = _.find(foundBusinessMetadata, function(o) {
                                return o.name === aKey
                            });
                            if (foundAttr) {
                                val[aKey] = { value: aVal, typeName: foundAttr.typeName };
                            }
                        })
                    }
                    return _.extend({}, val, { __internal_UI_businessMetadataName: key });
                }));
            this.collection = new Backbone.Collection();
            this.entityModel = new VEntity();
        },
        onHeaderClick: function() {
            var that = this;
            $('.businessMetaDataPanel').on("hidden.bs.collapse", function(e) {
                that.ui.cancel.hide();
                that.ui.saveBusinessMetadata.hide();
                that.ui.addBusinessMetadata.show();
                that.editMode = false;
                that.ui.businessMetadataTree.show();
                that.$(".editBox").hide();
                that.updateToActualData();
                if (that.collection && that.collection.length === 0) {
                    that.ui.addBusinessMetadata.text("Add");
                } else {
                    that.ui.addBusinessMetadata.text("Edit");
                }
            });
        },
        updateToActualData: function(options) {
            var silent = options && options.silent || false;
            this.collection.reset($.extend(true, [], this.actualCollection.toJSON()), { silent: silent });
        },
        onAddBusinessMetadata: function() {
            this.ui.addBusinessMetadata.hide();
            this.ui.saveBusinessMetadata.show();
            this.ui.cancel.show();
            this.editMode = true;
            this.ui.businessMetadataTree.hide();
            this.$(".editBox").show();
            this.updateToActualData({ silent: true });
            if (this.collection.length === 0) {
                this.createNameElement();
            } else {
                this.collection.trigger("reset");
            }
            this.panelOpenClose();
        },
        onCancel: function() {
            this.ui.cancel.hide();
            this.ui.saveBusinessMetadata.hide();
            this.ui.addBusinessMetadata.show();
            this.editMode = false;
            this.ui.businessMetadataTree.show();
            this.$(".editBox").hide();
            this.updateToActualData();
            this.panelOpenClose();
        },
        panelOpenClose: function() {
            var collection = this.editMode ? this.collection : this.actualCollection,
                headingEl = this.$el.find(".panel-heading.main-parent");
            if (collection && collection.length === 0) {
                this.ui.addBusinessMetadata.text("Add");
            } else {
                this.ui.addBusinessMetadata.text("Edit");
                if (headingEl.hasClass("collapsed")) {
                    headingEl.click();
                }
            }
        },
        validate: function() {
            var validation = true;
            this.$el.find('.custom-col-1[data-id="value"] [data-key]').each(function(el) {
                var val = $(this).val(),
                    elIsSelect2 = $(this).hasClass("select2-hidden-accessible");
                if (_.isString(val)) {
                    val = val.trim();
                }
                if (_.isEmpty(val)) {
                    if (validation) {
                        validation = false;
                    }
                    if (elIsSelect2) {
                        $(this).siblings(".select2").find(".select2-selection").attr("style", "border-color : red !important");
                    } else {
                        $(this).css("borderColor", "red");
                    }
                } else {
                    if (elIsSelect2) {
                        $(this).siblings(".select2").find(".select2-selection").attr("style", "");
                    } else {
                        $(this).css("borderColor", "");
                    }
                }
            });
            return validation;
        },
        onSaveBusinessMetadata: function() {
            var that = this;
            if (!this.validate()) {
                return;
            }
            var nData = this.generateData();
            if (this.actualCollection.length === 0 && _.isEmpty(nData)) {
                this.onCancel();
                return;
            }
            this.entityModel.saveBusinessMetadataEntity(this.guid, {
                data: JSON.stringify(nData),
                type: "POST",
                success: function(data) {
                    Utils.notifySuccess({
                        content: "One or more Business Metadata attribute" + Messages.getAbbreviationMsg(true, 'editSuccessMessage')
                    });
                    that.entity.businessAttributes = data;
                    that.ui.businessMetadataTree.html("");
                    that.editMode = false;
                    that.fetchCollection();
                    that.onCancel();

                },
                complete: function(model, response) {
                    //that.hideLoader();
                }
            });
        },
        generateData: function() {
            var finalObj = {};
            this.collection.forEach(function(model) {
                if (!model.has("addAttrButton")) {
                    var businessMetadataName = model.get("__internal_UI_businessMetadataName"),
                        modelObj = model.toJSON();
                    _.each(modelObj, function(o, k) {
                        if (k === "isNew" || k === "__internal_UI_businessMetadataName") {
                            delete modelObj[k];
                            return;
                        }
                        if (_.isObject(o) && o.value !== undefined) {
                            modelObj[k] = o.value;
                        }
                    })
                    if (businessMetadataName !== undefined) {
                        if (finalObj[businessMetadataName]) {
                            finalObj[businessMetadataName] = _.extend(finalObj[businessMetadataName], modelObj);
                        } else {
                            finalObj[businessMetadataName] = modelObj;
                        }
                    }
                }
            });
            if (_.isEmpty(finalObj)) {
                this.actualCollection.forEach(function(model) {
                    var businessMetadataName = model.get("__internal_UI_businessMetadataName");
                    if (businessMetadataName) {
                        finalObj[businessMetadataName] = {};
                    }
                })
            }
            return finalObj;
        },
        createNameElement: function(options) {
            var modelObj = { isNew: true };
            this.collection.unshift(modelObj);
        },
        renderBusinessMetadata: function() {
            var that = this,
                li = ""
            this.actualCollection.forEach(function(obj) {
                var attrLi = "";
                _.each(obj.attributes, function(val, key) {
                    if (key !== "__internal_UI_businessMetadataName") {
                        var newVal = val;
                        if (_.isObject(val) && !_.isUndefinedNull(val.value)) {
                            newVal = val.value;
                            if (newVal.length > 0 && val.typeName.indexOf("date") > -1) {
                                newVal = _.map(newVal, function(dates) {
                                    return Utils.formatDate({ date: dates, zone: false, dateFormat: Globals.dateFormat });
                                });
                            }
                            if (val.typeName === "date") {
                                newVal = Utils.formatDate({ date: newVal, zone: false, dateFormat: Globals.dateFormat });
                            }
                        }
                        attrLi += "<tr><td class='business-metadata-detail-attr-key'>" + _.escape(key) + " (" + _.escape(val.typeName) + ")</td><td>" + _.escape(newVal) + "</td></tr>";
                    }
                });
                li += that.associateAttributePanel(obj, attrLi);
            });
            var html = li;
            if (html === "" && this.readOnlyEntity === false) {
                html = '<div class="col-md-12"> No business metadata have been created yet. To add a business metadata, click <a href="javascript:void(0)" data-id="addBusinessMetadata">here</a></div>';
            }
            this.ui.businessMetadataTree.html(html);
        },
        associateAttributePanel: function(obj, tableBody) {
            return '<div class="panel panel-default custom-panel expand_collapse_panel-icon no-border business-metadata-detail-attr">' +
                '<div class="panel-heading" data-toggle="collapse" href="#' + _.escape(obj.get("__internal_UI_businessMetadataName")) + '" aria-expanded="true" style="width: 90%;">' +
                '<h4 class="panel-title"> <a>' + _.escape(obj.get("__internal_UI_businessMetadataName")) + '</a></h4>' +
                '<div class="btn-group pull-left"> <button type="button" title="Collapse"><i class="ec-icon fa"></i></button></div>' +
                '</div>' +
                '<div id="' + _.escape(obj.get("__internal_UI_businessMetadataName")) + '" class="panel-collapse collapse in">' +
                '<div class="panel-body"><table class="table bold-key">' + tableBody + '</table></div>' +
                '</div></div>';
        },
        onRender: function() {
            if (this.actualCollection && this.actualCollection.length) {
                this.$el.find(".panel-heading.main-parent").removeClass("collapsed").attr("aria-expanded", "true");
                this.$el.find("#businessMetadataCollapse").addClass("in").removeAttr("style");
                this.ui.addBusinessMetadata.text("Edit");
            }
            this.renderBusinessMetadata();
        }
    });
});