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
    'hbs!tmpl/business_metadata/BusinessMetadataAttributeItemView_tmpl'
], function(require, Backbone, BusinessMetadataAttributeItemViewTmpl) {
    'use strict';

    return Backbone.Marionette.ItemView.extend(
        /** @lends GlobalExclusionListView */
        {

            template: BusinessMetadataAttributeItemViewTmpl,
            templateHelpers: function() {
                return {
                    modalID: this.viewId
                };
            },

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                attributeInput: "[data-id='attributeInput']",
                close: "[data-id='close']",
                dataTypeSelector: "[data-id='dataTypeSelector']",
                searchWeightSelector: "[data-id='searchWeightSelector']",
                entityTypeSelector: "[data-id='entityTypeSelector']",
                enumTypeSelectorContainer: "[data-id='enumTypeSelectorContainer']",
                enumTypeSelector: "[data-id='enumTypeSelector']",
                enumValueSelectorContainer: "[data-id='enumValueSelectorContainer']",
                enumValueSelector: "[data-id='enumValueSelector']",
                multiValueSelect: "[data-id='multiValueSelect']",
                multiValueSelectStatus: "[data-id='multiValueSelectStatus']",
                stringLengthContainer: "[data-id='stringLengthContainer']",
                stringLengthValue: "[data-id='stringLength']",
                createNewEnum: "[data-id='createNewEnum']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["keyup " + this.ui.attributeInput] = function(e) {
                    this.model.set({ "name": e.target.value.trim() });
                };
                events["change " + this.ui.searchWeightSelector] = function(e) {
                    this.model.set({ "searchWeight": e.target.value.trim() });
                };
                events["change " + this.ui.dataTypeSelector] = function(e) {
                    var obj = { options: this.model.get('options') || {} };
                    delete obj.enumValues;
                    delete obj.options.maxStrLength;
                    if (e.target.value.trim().toLowerCase() === 'enumeration') {
                        this.ui.enumTypeSelectorContainer.show();
                        this.ui.enumTypeSelector.show();
                        this.emumTypeSelectDisplay();
                        this.ui.stringLengthContainer.hide();
                        this.ui.stringLengthValue.hide();
                    } else {
                        obj.typeName = e.target.value.trim();
                        if (e.target.value.trim().toLowerCase() === 'string') {
                            this.ui.stringLengthContainer.show();
                            this.ui.stringLengthValue.show();
                            this.ui.enumTypeSelectorContainer.hide();
                            this.ui.enumTypeSelector.hide();
                            this.ui.enumValueSelectorContainer.hide();
                            obj.options["maxStrLength"] = this.ui.stringLengthValue.val()
                        } else {
                            this.ui.enumTypeSelectorContainer.hide();
                            this.ui.enumTypeSelector.hide();
                            this.ui.enumValueSelectorContainer.hide();
                            this.ui.stringLengthContainer.hide();
                            this.ui.stringLengthValue.hide();
                        }
                    }
                    this.model.set(obj);
                    if (e.target.value.trim() != 'enumeration') {
                        this.ui.multiValueSelectStatus.trigger('change');
                    }
                };
                events["change " + this.ui.enumTypeSelector] = function(e) {
                    this.model.set({ "enumValues": e.target.value.trim() });
                };
                events["change " + this.ui.stringLengthContainer] = function(e) {
                    var options = this.model.get('options') || {};
                    if (this.ui.dataTypeSelector.val() == 'string') {
                        options["maxStrLength"] = e.target.value.trim();
                    }
                    this.model.set({ "options": options });

                };
                events["change " + this.ui.enumTypeSelector] = function(e) {
                    var emumValue = this.ui.enumTypeSelector.select2('data')[0] ? this.ui.enumTypeSelector.select2('data')[0].text : this.ui.enumTypeSelector.val();

                    this.model.set({ "typeName": emumValue });
                    if (this.model.get("multiValueSelect")) {
                        this.model.set({ "typeName": "array<" + emumValue + ">" });
                    }
                    if (emumValue == '' || emumValue == null) {
                        this.ui.enumValueSelectorContainer.hide();
                    } else {
                        this.ui.enumValueSelectorContainer.show();
                        this.showEnumValues(_.escape(emumValue));
                    }
                };
                events["change " + this.ui.enumValueSelector] = function(e) {
                    this.model.set({ "enumValues": this.ui.enumValueSelector.val() });
                };
                events["change " + this.ui.multiValueSelectStatus] = function(e) {
                    this.model.set({ "multiValueSelect": e.target.checked });
                    var typename = this.model.get('typeName');
                    if (e.target.checked) {
                        typename = "array<" + typename + ">";
                    } else {
                        typename = typename.replace('array<', '').replace('>', '');
                    }
                    this.model.set({ "typeName": typename });
                };
                events["change " + this.ui.entityTypeSelector] = function(e) {
                    var options = this.model.get('options') || {};
                    options.applicableEntityTypes = JSON.stringify(this.ui.entityTypeSelector.val());
                    this.model.set({ "options": options });
                }
                events["click " + this.ui.close] = 'onCloseButton';
                events["click " + this.ui.createNewEnum] = 'onCreateUpdateEnum';
                return events;
            },

            /**
             * intialize a new GlobalExclusionComponentView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, "typeHeaders", "businessMetadataDefCollection", "enumDefCollection", "isAttrEdit", "viewId", "collection"));
                this.viewId = options.model ? options.model.cid : this.viewId;
            },
            onRender: function() {
                var that = this,
                    entitytypes = '',
                    enumTypes = [],
                    searchWeightValue = '5',
                    stringLengthValue = '50',
                    applicableEntityType = '';
                this.typeHeaders.fullCollection.each(function(model) {
                    if (model.toJSON().category == "ENTITY") {
                        that.ui.entityTypeSelector.append("<option>" + model.get('name') + "</option>");
                        entitytypes += '<option  value="' + (model.get('name')) + '" data-name="' + (model.get('name')) + '">' + model.get('name') + '</option>';
                    }
                });
                this.ui.entityTypeSelector.select2({
                    placeholder: "Select Entity type",
                    allowClear: true,
                    multiple: true,
                    selectionAdapter: $.fn.select2.amd.require("TagHideDeleteButtonAdapter")
                });
                this.ui.entityTypeSelector.html(entitytypes);
                this.ui.entityTypeSelector.on('select2:open', function(e) { // to make selected option disable in dropdown added remove-from-list class
                    $('.select2-dropdown--below').addClass('remove-from-list');
                });
                if (this.model.get("searchWeight")) {
                    searchWeightValue = this.model.get("searchWeight") === -1 ? 0 : this.model.get("searchWeight");
                }
                if (this.model.get("options")) {
                    stringLengthValue = this.model.get("options").maxStrLength || '50';
                    applicableEntityType = this.model.get("options").applicableEntityTypes ? JSON.parse(this.model.get("options").applicableEntityTypes) : null;
                }
                this.ui.stringLengthValue.val(stringLengthValue).trigger('change'); //default length for string is 50
                this.ui.searchWeightSelector.val(searchWeightValue).trigger('change');
                this.ui.enumValueSelector.attr("disabled", "false"); // cannot edit the values
                this.emumTypeSelectDisplay();
                this.ui.enumTypeSelectorContainer.hide();
                this.ui.enumTypeSelector.hide();
                this.ui.enumValueSelectorContainer.hide();
                if (this.isAttrEdit) {
                    var typeName = this.model.get("typeName");
                    this.ui.close.hide();
                    this.ui.createNewEnum.hide(); // cannot add new businessMetadata on edit view
                    this.ui.attributeInput.val(this.model.get("name"));
                    this.ui.attributeInput.attr("disabled", "false");
                    this.ui.dataTypeSelector.attr("disabled", "false");
                    this.ui.dataTypeSelector.attr("disabled", "false");
                    this.ui.multiValueSelect.hide();
                    this.ui.dataTypeSelector.val(typeName);
                    if (typeName == "string") {
                        this.ui.stringLengthContainer.show();
                        this.ui.stringLengthValue.show();
                    } else {
                        this.ui.stringLengthContainer.hide();
                        this.ui.stringLengthValue.hide();
                    }
                    if (applicableEntityType) {
                        _.each(applicableEntityType, function(valName) {
                            that.ui.entityTypeSelector.find('option').each(function(o) {
                                var $el = $(this)
                                if ($el.data("name") === valName) {
                                    $el.attr("data-allowremove", "false");
                                }
                            })
                        });
                    }
                    this.ui.entityTypeSelector.val(applicableEntityType).trigger('change');
                    if (typeName != "string" && typeName != "boolean" && typeName != "byte" && typeName != "short" && typeName != "int" && typeName != "float" && typeName != "double" && typeName != "long" && typeName != "date") {
                        this.ui.enumTypeSelector.attr("disabled", "false");
                        this.ui.dataTypeSelector.val("enumeration").trigger('change');
                        this.ui.enumTypeSelector.val(typeName).trigger('change');
                    }
                    if (this.model.get("multiValued")) {
                        this.ui.multiValueSelect.show();
                        $(this.ui.multiValueSelectStatus).prop('checked', true).trigger('change');
                        this.ui.multiValueSelectStatus.attr("disabled", "false");
                    }
                }
            },
            showEnumValues: function(enumName) {
                var enumValues = '',
                    selectedValues = [],
                    selectedEnum = this.enumDefCollection.fullCollection.findWhere({ name: enumName }),
                    selectedEnumValues = selectedEnum ? selectedEnum.get('elementDefs') : null,
                    savedValues = [];
                _.each(selectedEnumValues, function(enumVal, index) {
                    selectedValues.push(enumVal.value);
                    enumValues += "<option>" + _.escape(enumVal.value) + "</option>";
                });
                this.ui.enumValueSelector.empty();
                this.ui.enumValueSelector.append(enumValues);
                this.ui.enumValueSelector.val(selectedValues);
                this.ui.enumValueSelector.select2({
                    placeholder: "Select Enum value",
                    allowClear: false,
                    tags: false,
                    multiple: true
                });
                this.model.set({ "enumValues": this.ui.enumValueSelector.val() });
            },
            emumTypeSelectDisplay: function() {
                var enumTypes = '';
                this.enumDefCollection.fullCollection.each(function(model, index) {
                    enumTypes += "<option>" + _.escape(model.get('name')) + "</option>";
                });
                this.ui.enumTypeSelector.empty();
                this.ui.enumTypeSelector.append(enumTypes);
                this.ui.enumTypeSelector.val('');
                this.ui.enumTypeSelector.select2({
                    placeholder: "Select Enum name",
                    tags: false,
                    allowClear: true,
                    multiple: false
                });
            },
            onCreateUpdateEnum: function(e) {
                var that = this;
                require(["views/business_metadata/EnumCreateUpdateItemView", "modules/Modal"], function(EnumCreateUpdateItemView, Modal) {
                    var view = new EnumCreateUpdateItemView({
                            onUpdateEnum: function() {
                                that.ui.enumValueSelectorContainer.hide();
                                that.emumTypeSelectDisplay();
                                that.ui.enumValueSelector.empty();
                            },
                            closeModal: function() {
                                modal.trigger("cancel");
                                that.enumDefCollection.fetch({
                                    success: function() {
                                        that.ui.enumTypeSelector.val(that.model.get('typeName')).trigger('change');
                                    }
                                });
                            },
                            enumDefCollection: that.enumDefCollection,
                            businessMetadataDefCollection: that.businessMetadataDefCollection
                        }),
                        modal = new Modal({
                            title: "Create/ Update Enum",
                            content: view,
                            cancelText: "Cancel",
                            okCloses: false,
                            okText: "Update",
                            allowCancel: true,
                            showFooter: false
                        }).open();
                    modal.on('closeModal', function() {
                        modal.trigger('cancel');
                    });
                });
            },
            onCloseButton: function() {
                var tagName = this.$el.find('[data-id="tagName"]').val();
                if (this.collection.models.length > 0) {
                    this.model.destroy();
                }
                if (this.collection.models.length == 0 && tagName != "") {
                    this.$el.parent().next().find('button.ok').removeAttr("disabled");
                }
            }
        });
});