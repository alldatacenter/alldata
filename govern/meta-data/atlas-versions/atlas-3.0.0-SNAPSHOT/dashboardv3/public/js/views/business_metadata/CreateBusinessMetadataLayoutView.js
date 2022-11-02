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
    'hbs!tmpl/business_metadata/CreateBusinessMetadataLayoutView_tmpl',
    'utils/Utils',
    'utils/Messages',
    'views/business_metadata/BusinessMetadataAttributeItemView',
    'models/VEntity'
], function(require, Backbone, CreateBusinessMetadataLayoutViewTmpl, Utils, Messages, BusinessMetadataAttributeItemView, VEntity) {

    var CreateBusinessMetadataLayoutView = Backbone.Marionette.CompositeView.extend(
        /** @lends CreateBusinessMetadataLayoutView */
        {
            _viewName: 'CreateBusinessMetadataLayoutView',

            template: CreateBusinessMetadataLayoutViewTmpl,

            templateHelpers: function() {
                return {
                    create: this.create,
                    description: this.description,
                    fromTable: this.fromTable,
                    isEditAttr: this.isEditAttr
                };
            },

            /** Layout sub regions */
            regions: {},

            childView: BusinessMetadataAttributeItemView,

            childViewContainer: "[data-id='addAttributeDiv']",

            childViewOptions: function() {
                return {
                    typeHeaders: this.typeHeaders,
                    businessMetadataDefCollection: this.businessMetadataDefCollection,
                    enumDefCollection: this.enumDefCollection,
                    isAttrEdit: this.isAttrEdit,
                    viewId: this.cid,
                    collection: this.collection
                };
            },
            /** ui selector cache */
            ui: {
                name: "[data-id='name']",
                description: "[data-id='description']",
                title: "[data-id='title']",
                attributeData: "[data-id='attributeData']",
                addAttributeDiv: "[data-id='addAttributeDiv']",
                createForm: '[data-id="createForm"]',
                businessMetadataAttrPageCancle: '[data-id="businessMetadataAttrPageCancle"]',
                businessMetadataAttrPageOk: '[data-id="businessMetadataAttrPageOk"]'
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.attributeData] = "onClickAddAttriBtn";
                events["click " + this.ui.businessMetadataAttrPageOk] = function(e) {
                    var that = this,
                        modal = that.$el;
                    if (e.target.dataset.action == "attributeEdit" || e.target.dataset.action == "addAttribute") {
                        that.onUpdateAttr();
                    } else {
                        that.onCreateBusinessMetadata();
                    }

                };
                events["click " + this.ui.businessMetadataAttrPageCancle] = function(e) {
                    this.options.onUpdateBusinessMetadata();
                };
                return events;
            },
            /**
             * intialize a new CreateBusinessMetadataLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'businessMetadataDefCollection', 'selectedBusinessMetadata', 'enumDefCollection', 'model', 'isNewBusinessMetadata', 'isAttrEdit', 'typeHeaders', 'attrDetails'));
                this.fromTable = this.isNewBusinessMetadata ? true : false;
                this.isEditAttr = this.isAttrEdit ? false : true;
                this.businessMetadataModel = new VEntity();
                if (this.model) {
                    this.description = this.model.get('description');
                } else {
                    this.create = true;
                }
                if (!this.isNewBusinessMetadata) {
                    this.collection = this.isAttrEdit ? new Backbone.Collection([
                        this.attrDetails
                    ]) : new Backbone.Collection([{
                        "name": "",
                        "typeName": "string",
                        "isOptional": true,
                        "cardinality": "SINGLE",
                        "valuesMinCount": 0,
                        "valuesMaxCount": 1,
                        "isUnique": false,
                        "isIndexable": true
                    }]);
                } else {
                    this.collection = new Backbone.Collection();
                }

            },
            bindEvents: function() {},
            onRender: function() {
                var that = this;
                this.$('.fontLoader').show();
                if (!('placeholder' in HTMLInputElement.prototype)) {
                    this.ui.createForm.find('input,textarea').placeholder();
                }
                if (this.isNewBusinessMetadata == true) {
                    that.ui.businessMetadataAttrPageOk.text("Create");
                    that.ui.businessMetadataAttrPageOk.attr('data-action', 'newBusinessMetadata');
                } else {
                    that.ui.businessMetadataAttrPageOk.text("Save");
                    that.ui.businessMetadataAttrPageOk.attr('data-action', 'attributeEdit');
                }
                this.hideLoader();
            },
            hideLoader: function() {
                this.$('.fontLoader').hide();
                this.$('.hide').removeClass('hide');
            },
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
                this.collectionAttribute();
                if (!('placeholder' in HTMLInputElement.prototype)) {
                    this.ui.addAttributeDiv.find('input,textarea').placeholder();
                }
            },
            loaderStatus: function(isActive) {
                var that = this;
                if (isActive) {
                    parent.$('.business-metadata-attr-tableOverlay').show();
                    parent.$('.business-metadata-attr-fontLoader').show();
                } else {
                    parent.$('.business-metadata-attr-tableOverlay').hide();
                    parent.$('.business-metadata-attr-fontLoader').hide();
                }
            },
            validateValues: function(attributeDefs) {
                var isValidate = true,
                    isAttrDuplicate = true,
                    validationFileds = this.$el.find('.require'),
                    attrNames = [];
                if (attributeDefs && !this.isAttrEdit) {
                    attrNames = _.map(attributeDefs, function(model) {
                        return model.name.toLowerCase();
                    });
                }
                validationFileds.each(function(elements) {
                    $(this).removeClass('errorValidate');
                    if (validationFileds[elements].value.trim() == '' || validationFileds[elements].value == null) {
                        if (validationFileds[elements].style.display != 'none') {
                            $(validationFileds[elements]).addClass('errorValidate');
                            $(this).addClass('errorValidate');
                            if (isValidate) { isValidate = false; }
                        }
                    }
                });
                if (isValidate) {
                    this.$el.find('.attributeInput').each(function(element) {
                        var attrValue = this.value.toLowerCase();
                        if (attrNames.indexOf(attrValue) > -1) {
                            Utils.notifyInfo({
                                content: "Attribute name already exist"
                            });
                            $(this).addClass('errorValidate');
                            if (isAttrDuplicate) { isAttrDuplicate = false; }
                        } else {
                            if (attrValue.length) {
                                attrNames.push(attrValue);
                            }
                        }
                    });
                }

                if (!isValidate) {
                    Utils.notifyInfo({
                        content: "Please fill the details"
                    });
                    return true;
                }
                if (!isAttrDuplicate) {
                    return true;
                }

            },
            onCreateBusinessMetadata: function() {
                var that = this;
                if (this.validateValues()) {
                    return;
                };
                this.loaderStatus(true);
                var name = this.ui.name.val(),
                    description = this.ui.description.val();
                var attributeObj = this.collection.toJSON();
                if (this.collection.length === 1 && this.collection.first().get("name") === "") {
                    attributeObj = [];
                }
                this.json = {
                    "enumDefs": [],
                    "structDefs": [],
                    "classificationDefs": [],
                    "entityDefs": [],
                    "businessMetadataDefs": [{
                        "category": "BUSINESS_METADATA",
                        "createdBy": "admin",
                        "updatedBy": "admin",
                        "version": 1,
                        "typeVersion": "1.1",
                        "name": name.trim(),
                        "description": description.trim(),
                        "attributeDefs": attributeObj
                    }]
                };
                var apiObj = {
                    sort: false,
                    data: this.json,
                    success: function(model, response) {
                        var nameSpaveDef = model.businessMetadataDefs;
                        if (nameSpaveDef) {
                            that.businessMetadataDefCollection.fullCollection.add(nameSpaveDef);
                            Utils.notifySuccess({
                                content: "Business Metadata " + name + Messages.getAbbreviationMsg(false, 'addSuccessMessage')
                            });
                        }
                        that.options.onUpdateBusinessMetadata(true);
                    },
                    silent: true,
                    reset: true,
                    complete: function(model, status) {
                        that.loaderStatus(false);
                    }
                }
                apiObj.type = "POST";
                that.businessMetadataModel.saveBusinessMetadata(apiObj);
            },
            onUpdateAttr: function() {
                var that = this,
                    selectedBusinessMetadataClone = $.extend(true, {}, that.selectedBusinessMetadata.toJSON()),
                    attributeDefs = selectedBusinessMetadataClone['attributeDefs'],
                    isvalidName = true;
                if (this.validateValues(attributeDefs)) {
                    return;
                };
                if (this.collection.length > 0) {
                    this.loaderStatus(true);
                    if (selectedBusinessMetadataClone.attributeDefs === undefined) {
                        selectedBusinessMetadataClone.attributeDefs = [];
                    }
                    selectedBusinessMetadataClone.attributeDefs = selectedBusinessMetadataClone.attributeDefs.concat(this.collection.toJSON());
                    this.json = {
                        "enumDefs": [],
                        "structDefs": [],
                        "classificationDefs": [],
                        "entityDefs": [],
                        "businessMetadataDefs": [selectedBusinessMetadataClone]
                    };
                    var apiObj = {
                        sort: false,
                        data: this.json,
                        success: function(model, response) {
                            Utils.notifySuccess({
                                content: "One or more Business Metadata attribute" + Messages.getAbbreviationMsg(true, 'editSuccessMessage')
                            });
                            if (model.businessMetadataDefs && model.businessMetadataDefs.length) {
                                that.selectedBusinessMetadata.set(model.businessMetadataDefs[0]);
                            }
                            that.options.onEditCallback();
                            that.options.onUpdateBusinessMetadata(true);
                        },
                        silent: true,
                        reset: true,
                        complete: function(model, status) {
                            that.loaderStatus(false);
                        }
                    }
                    apiObj.type = "PUT";
                    that.businessMetadataModel.saveBusinessMetadata(apiObj);
                } else {
                    Utils.notifySuccess({
                        content: "No attribute updated"
                    });
                    this.loaderStatus(false);
                    that.options.onUpdateBusinessMetadata();
                }
            }
        });
    return CreateBusinessMetadataLayoutView;
});