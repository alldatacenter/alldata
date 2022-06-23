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
define(["require", "backbone", "hbs!tmpl/business_metadata/EnumCreateUpdateItemView_tmpl", "utils/Utils", "utils/UrlLinks"], function(
    require,
    Backbone,
    EnumCreateUpdateItemViewTmpl,
    Utils,
    UrlLinks
) {
    "use strict";

    return Backbone.Marionette.ItemView.extend(
        /** @lends GlobalExclusionListView */
        {
            template: EnumCreateUpdateItemViewTmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                enumTypeSelectorContainer: "[data-id='enumTypeSelectorContainer']",
                enumSelector: "[data-id='enumSelector']",
                enumValueSelectorContainer: "[data-id='enumValueSelectorContainer']",
                valueSelector: "[data-id='valueSelector']",
                enumCancleBtn: "[data-id='enumCancleBtn']",
                enumOkBtn: "[data-id='enumOkBtn']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["change " + this.ui.enumSelector] = function(e) {
                    this.model.set({ enumValues: e.target.value.trim() });
                };
                events["change " + this.ui.enumSelector] = function(e) {
                    var emumValue = this.ui.enumSelector.select2("data")[0] ?
                        this.ui.enumSelector.select2("data")[0].text :
                        this.ui.enumSelector.val();
                    if (emumValue == "" || emumValue == null) {
                        this.ui.enumValueSelectorContainer.hide();
                    } else {
                        this.ui.enumValueSelectorContainer.show();
                        this.showEnumValues(emumValue);
                    }
                };
                events["change " + this.ui.valueSelector] = function(e) {};
                events["click " + this.ui.enumCancleBtn] = function(e) {
                    if (this.options.closeModal) {
                        this.options.closeModal();
                        return;
                    }
                    this.ui.enumValueSelectorContainer.hide();
                    this.ui.enumSelector.val("").trigger("change");
                    this.ui.enumCancleBtn.attr("disabled", "true");
                };
                events["click " + this.ui.enumOkBtn] = function(e) {
                    this.ui.enumCancleBtn.attr("disabled", "true");
                    this.onUpdateEnum();
                };
                return events;
            },

            /**
             * intialize a new GlobalExclusionComponentView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, "businessMetadataDefCollection", "enumDefCollection"));
            },
            onRender: function() {
                this.ui.enumValueSelectorContainer.hide();
                this.bindEvents();
                this.emumTypeSelectDisplay();
                if (!this.options.closeModal) {
                    this.ui.enumCancleBtn.attr("disabled", "true");
                    this.ui.enumCancleBtn.text("Clear");
                }
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.enumDefCollection, 'reset', function() {
                    that.emumTypeSelectDisplay();
                })
            },

            showEnumValues: function(enumName) {
                var enumValues = "",
                    selectedValues = [],
                    selectedEnum = this.enumDefCollection.fullCollection.findWhere({ name: enumName }),
                    selectedEnumValues = selectedEnum ? selectedEnum.get("elementDefs") : null;
                _.each(selectedEnumValues, function(enumVal, index) {
                    selectedValues.push(enumVal.value);
                    enumValues += "<option>" + _.escape(enumVal.value) + "</option>";
                });

                this.ui.enumCancleBtn.removeAttr("disabled");
                this.ui.valueSelector.empty();
                this.ui.valueSelector.append(enumValues);
                this.ui.valueSelector.val(selectedValues);
                this.ui.valueSelector.select2({
                    placeholder: "Select Enum value",
                    allowClear: false,
                    tags: true,
                    multiple: true
                });
            },
            emumTypeSelectDisplay: function() {
                var enumTypes = "";
                this.enumDefCollection.fullCollection.each(function(model, index) {
                    enumTypes += "<option>" + _.escape(model.get("name")) + "</option>";
                });
                this.ui.enumSelector.empty();
                this.ui.enumSelector.append(enumTypes);
                this.ui.enumSelector.val("");
                this.ui.enumSelector.select2({
                    placeholder: "Select Enum name",
                    tags: true,
                    allowClear: true,
                    multiple: false,
                    templateResult: this.formatSearchResult
                });
            },
            formatSearchResult: function(state) {
                if (!state.id) {
                    return state.text;
                }
                if (!state.element) {
                    return $("<span>Create new enum : <strong> " + _.escape(state.text) + "</strong></span>");
                } else {
                    return $("<span>" + _.escape(state.text) + "</span>");
                }
            },
            validationEnum: function() {
                var selectedEnumName = this.ui.enumSelector.val(),
                    selectedEnumValues = this.ui.valueSelector.val();

                if (selectedEnumName == "" || selectedEnumName == null) {
                    this.ui.enumOkBtn.hideButtonLoader();
                    Utils.notifyInfo({
                        content: "Please enter the Enumeration Name"
                    });
                    return true;
                }
                if (selectedEnumValues == "" || selectedEnumValues == null) {
                    this.ui.enumOkBtn.hideButtonLoader();
                    Utils.notifyInfo({
                        content: "Please  enter the Enum values"
                    });
                    return true;
                }
            },
            onUpdateEnum: function(view, modal) {
                var that = this,
                    selectedEnumName = this.ui.enumSelector.val(),
                    selectedEnumValues = this.ui.valueSelector.val(),
                    enumName = this.enumDefCollection.fullCollection.findWhere({ name: selectedEnumName }),
                    isPutCall = false,
                    isPostCallEnum = false,
                    enumDefs = [];
                if (this.validationEnum()) {
                    return;
                }
                this.ui.enumOkBtn.showButtonLoader();
                this.ui.enumSelector.attr("disabled", "true");
                this.ui.valueSelector.attr("disabled", "true");
                this.ui.enumCancleBtn.attr("disabled", "true");
                if (enumName) {
                    var enumDef = enumName.get("elementDefs");
                    if (enumDef.length === selectedEnumValues.length) {
                        _.each(enumDef, function(enumVal, index) {
                            if (selectedEnumValues.indexOf(enumVal.value) === -1) {
                                isPutCall = true;
                            }
                        });
                    } else {
                        isPutCall = true;
                    }
                } else {
                    isPostCallEnum = true;
                }
                var elementValues = [];
                _.each(selectedEnumValues, function(inputEnumVal, index) {
                    elementValues.push({
                        ordinal: index + 1,
                        value: inputEnumVal
                    });
                });

                enumDefs.push({
                    name: selectedEnumName,
                    elementDefs: elementValues
                });

                this.json = {
                    enumDefs: enumDefs
                };
                var apiObj = {
                    sort: false,
                    success: function(model, response) {
                        that.ui.enumValueSelectorContainer.hide();
                        if (isPostCallEnum) {
                            that.enumDefCollection.add(model.enumDefs[0]);
                            Utils.notifySuccess({
                                content: "Enumeration " + selectedEnumName + " added successfully"
                            });
                        } else {
                            var foundEnum = that.enumDefCollection.fullCollection.find({ guid: model.enumDefs[0].guid })
                            if (foundEnum) {
                                foundEnum.set(model.enumDefs[0]);
                            }
                            Utils.notifySuccess({
                                content: "Enumeration " + selectedEnumName + " updated successfully"
                            });
                        }
                        that.enumDefCollection.fetch({ reset: true });
                        if (that.options.onUpdateEnum) { //callback from BusinessMetadataAttributeItemView
                            that.options.onUpdateEnum();
                        }
                        that.ui.enumCancleBtn.attr("disabled", "true");
                    },
                    silent: true,
                    reset: true,
                    complete: function(model, status) {
                        that.emumTypeSelectDisplay();
                        that.ui.enumOkBtn.hideButtonLoader();
                        that.ui.enumSelector.removeAttr("disabled");
                        that.ui.valueSelector.removeAttr("disabled");
                        if (that.options.closeModal) {
                            that.options.closeModal();
                        }
                    }
                };
                $.extend(apiObj, { contentType: "application/json", dataType: "json", data: JSON.stringify(this.json) });
                if (isPostCallEnum) {
                    this.businessMetadataDefCollection.constructor.nonCrudOperation.call(this, UrlLinks.typedefsUrl().defs, "POST", apiObj);
                } else if (isPutCall) {
                    this.businessMetadataDefCollection.constructor.nonCrudOperation.call(this, UrlLinks.typedefsUrl().defs, "PUT", apiObj);
                } else {
                    Utils.notifySuccess({
                        content: "No updated values"
                    });
                    that.ui.enumOkBtn.hideButtonLoader();
                    that.ui.enumSelector.removeAttr("disabled");
                    that.ui.valueSelector.removeAttr("disabled");
                    if (that.options.closeModal) {
                        that.options.closeModal();
                    }
                }
            }
        }
    );
});