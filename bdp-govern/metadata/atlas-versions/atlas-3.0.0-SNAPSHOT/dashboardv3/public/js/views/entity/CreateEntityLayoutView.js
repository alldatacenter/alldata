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
    'hbs!tmpl/entity/CreateEntityLayoutView_tmpl',
    'utils/Utils',
    'collection/VTagList',
    'collection/VEntityList',
    'models/VEntity',
    'modules/Modal',
    'utils/Messages',
    'moment',
    'utils/UrlLinks',
    'collection/VSearchList',
    'utils/Enums',
    'utils/Globals',
    'daterangepicker'
], function(require, Backbone, CreateEntityLayoutViewTmpl, Utils, VTagList, VEntityList, VEntity, Modal, Messages, moment, UrlLinks, VSearchList, Enums, Globals) {

    var CreateEntityLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends CreateEntityLayoutView */
        {
            _viewName: 'CreateEntityLayoutView',

            template: CreateEntityLayoutViewTmpl,

            templateHelpers: function() {
                return {
                    guid: this.guid
                };
            },

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                entityName: "[data-id='entityName']",
                entityList: "[data-id='entityList']",
                entityInputData: "[data-id='entityInputData']",
                toggleRequired: 'input[name="toggleRequired"]',
                assetName: "[data-id='assetName']",
                entityInput: "[data-id='entityInput']",
                entitySelectionBox: "[data-id='entitySelectionBox']",

            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["change " + this.ui.entityList] = "onEntityChange";
                events["change " + this.ui.toggleRequired] = function(e) {
                    this.requiredAllToggle(e.currentTarget.checked)
                };
                return events;
            },
            /**
             * intialize a new CreateEntityLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'callback', 'showLoader', 'entityDefCollection', 'typeHeaders', 'searchVent'));
                var that = this,
                    entityTitle, okLabel;
                this.selectStoreCollection = new Backbone.Collection();
                this.collection = new VEntityList();
                this.entityModel = new VEntity();
                if (this.guid) {
                    this.collection.modelAttrName = "createEntity"
                }
                this.asyncReferEntityCounter = 0;
                this.required = true;
                if (this.guid) {
                    entityTitle = 'Edit entity';
                    okLabel = 'Update';
                } else {
                    entityTitle = 'Create entity';
                    okLabel = 'Create';
                }
                this.modal = new Modal({
                    title: entityTitle,
                    content: this,
                    cancelText: "Cancel",
                    okText: okLabel,
                    allowCancel: true,
                    okCloses: false,
                    width: '50%'
                }).open();
                this.modal.$el.find('button.ok').attr("disabled", true);
                this.modal.on('ok', function(e) {
                    that.modal.$el.find('button.ok').showButtonLoader();
                    that.okButton();
                });
                this.modal.on('closeModal', function() {
                    that.modal.trigger('cancel');
                });
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.collection, "reset", function() {
                    this.entityCollectionList();
                }, this);
                this.listenTo(this.collection, 'error', function() {
                    this.hideLoader();
                }, this);
            },
            onRender: function() {
                this.bindEvents();
                if (!this.guid) {
                    this.bindRequiredField();
                }
                this.showLoader();
                this.fetchCollections();
                this.$('.toggleRequiredSwitch').hide();
            },
            bindRequiredField: function() {
                var that = this;
                this.ui.entityInputData.on("keyup change", "textarea", function(e) {
                    var value = this.value;
                    if (!value.length && $(this).hasClass('false')) {
                        $(this).removeClass('errorClass');
                        that.modal.$el.find('button.ok').prop("disabled", false);

                    } else {
                        try {
                            if (value && value.length) {
                                JSON.parse(value);
                                $(this).removeClass('errorClass');
                                that.modal.$el.find('button.ok').prop("disabled", false);
                            }
                        } catch (err) {
                            $(this).addClass('errorClass');
                            that.modal.$el.find('button.ok').prop("disabled", true);
                        }
                    }
                });

                this.ui.entityInputData.on('keyup change', 'input.true,select.true', function(e) {
                    if (this.value.trim() !== "") {
                        if ($(this).data('select2')) {
                            $(this).data('select2').$container.find('.select2-selection').removeClass("errorClass");
                        } else {
                            $(this).removeClass('errorClass');
                        }
                        if (that.ui.entityInputData.find('.errorClass').length === 0) {
                            that.modal.$el.find('button.ok').prop("disabled", false);
                        }
                    } else {
                        that.modal.$el.find('button.ok').prop("disabled", true);
                        if ($(this).data('select2')) {
                            $(this).data('select2').$container.find('.select2-selection').addClass("errorClass");
                        } else {
                            $(this).addClass('errorClass');
                        }
                    }
                });
            },
            bindNonRequiredField: function() {
                var that = this;
                this.ui.entityInputData.off('keyup change', 'input.false,select.false').on('keyup change', 'input.false,select.false', function(e) {
                    if (that.modal.$el.find('button.ok').prop('disabled') && that.ui.entityInputData.find('.errorClass').length === 0) {
                        that.modal.$el.find('button.ok').prop("disabled", false);
                    }
                });
            },
            decrementCounter: function(counter) {
                if (this[counter] > 0) {
                    --this[counter];
                }
            },
            fetchCollections: function() {
                if (this.guid) {
                    this.collection.url = UrlLinks.entitiesApiUrl({ guid: this.guid });
                    this.collection.fetch({ reset: true });
                } else {
                    this.entityCollectionList();
                }
            },
            entityCollectionList: function() {
                this.ui.entityList.empty();
                var that = this,
                    name = "",
                    value;
                if (this.guid) {
                    this.collection.each(function(val) {
                        name += Utils.getName(val.get("entity"));
                        that.entityData = val;
                    });
                    this.ui.assetName.html(name);
                    var referredEntities = this.entityData.get('referredEntities');
                    var attributes = this.entityData.get('entity').attributes;
                    _.map(_.keys(attributes), function(key) {
                        if (_.isObject(attributes[key])) {
                            var attrObj = attributes[key];
                            if (_.isObject(attrObj) && !_.isArray(attrObj)) {
                                attrObj = [attrObj];
                            }
                            _.each(attrObj, function(obj) {
                                if (obj.guid && !referredEntities[obj.guid]) {
                                    ++that.asyncReferEntityCounter;
                                    that.collection.url = UrlLinks.entitiesApiUrl({ guid: obj.guid });
                                    that.collection.fetch({
                                        success: function(data, response) {
                                            referredEntities[obj.guid] = response.entity;
                                        },
                                        complete: function() {
                                            that.decrementCounter('asyncReferEntityCounter');
                                            if (that.asyncReferEntityCounter === 0) {
                                                that.onEntityChange(null, that.entityData);
                                            }
                                        },
                                        silent: true
                                    });
                                }
                            });
                        }
                    });
                    if (this.asyncReferEntityCounter === 0) {
                        this.onEntityChange(null, this.entityData);
                    }
                } else {
                    var str = '<option disabled="disabled" selected>--Select entity-type--</option>';
                    this.entityDefCollection.fullCollection.each(function(val) {
                        var name = Utils.getName(val.toJSON());
                        if (Globals.entityTypeConfList && name.indexOf("__") !== 0) {
                            if (_.isEmptyArray(Globals.entityTypeConfList)) {
                                str += '<option>' + name + '</option>';
                            } else {
                                if (_.contains(Globals.entityTypeConfList, val.get("name"))) {
                                    str += '<option>' + name + '</option>';
                                }
                            }
                        }
                    });
                    this.ui.entityList.html(str);
                    this.ui.entityList.select2({});
                    this.hideLoader();
                }
            },
            capitalize: function(string) {
                return string.charAt(0).toUpperCase() + string.slice(1);
            },
            requiredAllToggle: function(checked) {
                if (checked) {
                    this.ui.entityInputData.addClass('all').removeClass('required');
                    this.ui.entityInputData.find('div.true').show();
                    this.ui.entityInputData.find('fieldset div.true').show();
                    this.ui.entityInputData.find('fieldset').show();
                    this.required = false;
                } else {
                    this.ui.entityInputData.addClass('required').removeClass('all');
                    this.ui.entityInputData.find('fieldset').each(function() {
                        if (!$(this).find('div').hasClass('false')) {
                            $(this).hide();
                        }
                    });
                    this.ui.entityInputData.find('div.true').hide();
                    this.ui.entityInputData.find('fieldset div.true').hide();
                    this.required = true;
                }

            },
            onEntityChange: function(e, value) {
                var that = this,
                    typeName = value && value.get('entity') ? value.get('entity').typeName : null;
                if (!this.guid) {
                    this.showLoader();
                }
                this.ui.entityInputData.empty();
                if (typeName) {
                    this.collection.url = UrlLinks.entitiesDefApiUrl(typeName);
                } else if (e) {
                    this.collection.url = UrlLinks.entitiesDefApiUrl(e.target.value);
                    this.collection.modelAttrName = 'attributeDefs';
                }
                this.collection.fetch({
                    success: function(model, data) {
                        that.supuertypeFlag = 0;
                        that.subAttributeData(data)
                    },
                    complete: function() {
                        that.modal.$el.find('button.ok').prop("disabled", true);
                        //that.initilizeElements();
                    },
                    silent: true
                });
            },
            renderAttribute: function(object) {
                var that = this,
                    visitedAttr = {},
                    attributeObj = object.attributeDefs,
                    isAllAttributeOptinal = true,
                    isAllRelationshipAttributeOptinal = true,
                    attributeDefList = attributeObj.attributeDefs,
                    relationshipAttributeDefsList = attributeObj.relationshipAttributeDefs,
                    attributeHtml = "",
                    relationShipAttributeHtml = "",
                    fieldElemetHtml = '',
                    commonInput = function(object) {
                        var value = object.value,
                            containerHtml = '';
                        containerHtml += that.getContainer(object);
                        return containerHtml;
                    };
                if (attributeDefList.length || relationshipAttributeDefsList.length) {
                    _.each(attributeDefList, function(value) {
                        if (value.isOptional === false) {
                            isAllAttributeOptinal = false;
                        }
                        attributeHtml += commonInput({
                            "value": value,
                            "duplicateValue": false,
                            "isAttribute": true
                        });
                    });
                    _.each(relationshipAttributeDefsList, function(value) {
                        if (value.isOptional === false) {
                            isAllRelationshipAttributeOptinal = false;
                        }
                        if (visitedAttr[value.name] === null) {
                            // on second visited set it to true;and now onwords ignore if same value come.
                            var duplicateRelationship = _.where(relationshipAttributeDefsList, { name: value.name });
                            var str = '<option value="">--Select a Relationship Type--</option>';
                            _.each(duplicateRelationship, function(val, index, list) {
                                str += '<option>' + _.escape(val.relationshipTypeName) + '</option>';
                            });
                            var isOptional = value.isOptional;
                            visitedAttr[value.name] = '<div class="form-group"><select class="form-control row-margin-bottom entityInputBox ' + (value.isOptional === true ? "false" : "true") + '" data-for-key= "' + value.name + '"> ' + str + '</select></div>';
                        } else {
                            relationShipAttributeHtml += commonInput({
                                "value": value,
                                "duplicateValue": true,
                                "isRelation": true
                            });
                            // once visited set it to null;
                            visitedAttr[value.name] = null;
                        }
                    });
                    if (attributeHtml.length) {
                        fieldElemetHtml += that.getFieldElementContainer({
                            "htmlField": attributeHtml,
                            "attributeType": true,
                            "alloptional": isAllAttributeOptinal
                        });
                    }
                    if (relationShipAttributeHtml.length) {
                        fieldElemetHtml += that.getFieldElementContainer({
                            "htmlField": relationShipAttributeHtml,
                            "relationshipType": true,
                            "alloptional": isAllRelationshipAttributeOptinal
                        });
                    }
                    if (fieldElemetHtml.length) {
                        that.ui.entityInputData.append(fieldElemetHtml);
                        _.each(_.keys(visitedAttr), function(key) {
                            if (visitedAttr[key] === null) {
                                return;
                            }
                            var elFound = that.ui.entityInputData.find('[data-key="' + key + '"]');
                            elFound.prop('disabled', true);
                            elFound.parent().prepend(visitedAttr[key]);
                        });
                    } else {
                        fieldElemetHtml = "<h4 class='text-center'>Defination not found</h4>";
                        that.ui.entityInputData.append(fieldElemetHtml);
                    }

                }
                that.ui.entityInputData.find("select[data-for-key]").select2({}).on('change', function() {
                    var forKey = $(this).data('forKey'),
                        forKeyEl = null;
                    if (forKey && forKey.length) {
                        forKeyEl = that.ui.entityInputData.find('[data-key="' + forKey + '"]');
                        if (forKeyEl) {
                            if (this.value == "") {
                                forKeyEl.val(null).trigger('change');
                                forKeyEl.prop("disabled", true);
                            } else {
                                forKeyEl.prop("disabled", false);
                            }
                        }
                    }
                });
                return false;
            },
            subAttributeData: function(data) {
                var that = this,
                    attributeInput = "",
                    alloptional = false,
                    attributeDefs = Utils.getNestedSuperTypeObj({
                        seperateRelatioshipAttr: true,
                        attrMerge: true,
                        data: data,
                        collection: this.entityDefCollection
                    });
                if (attributeDefs && attributeDefs.relationshipAttributeDefs.length) {
                    attributeDefs.attributeDefs = _.filter(attributeDefs.attributeDefs, function(obj) {
                        if (_.find(attributeDefs.relationshipAttributeDefs, { name: obj.name }) === undefined) {
                            return true;
                        }
                    })
                }
                if (attributeDefs.attributeDefs.length || attributeDefs.relationshipAttributeDefs.length) {
                    this.$('.toggleRequiredSwitch').show();
                    this.$('.entity-list').removeClass('col-sm-12').addClass('col-sm-8');
                } else {
                    this.$('.toggleRequiredSwitch').hide();
                    this.$('.entity-list').removeClass('col-sm-8').addClass('col-sm-12');
                }
                //make a function call.
                this.renderAttribute({
                    attributeDefs: attributeDefs
                });

                if (this.required) {
                    this.ui.entityInputData.find('fieldset div.true').hide()
                    this.ui.entityInputData.find('div.true').hide();
                }
                if (!('placeholder' in HTMLInputElement.prototype)) {
                    this.ui.entityInputData.find("input,select,textarea").placeholder();
                }
                that.initilizeElements();
            },
            initilizeElements: function() {
                var that = this,
                    $createTime = this.modal.$el.find('input[name="createTime"]'),
                    dateObj = {
                        "singleDatePicker": true,
                        "showDropdowns": true,
                        "startDate": new Date(),
                        locale: {
                            format: Globals.dateFormat
                        }
                    };
                this.$('input[data-type="date"]').each(function() {
                    if (!$(this).data('daterangepicker')) {
                        if (that.guid && this.value.length) {
                            dateObj["startDate"] = new Date(Number(this.value));
                        }
                        if ($(this).attr('name') === "modifiedTime") {
                            dateObj["minDate"] = $createTime.val();
                        }
                        $(this).daterangepicker(dateObj);
                    }
                });
                modifiedDateObj = _.extend({}, dateObj);
                $createTime.on('apply.daterangepicker', function(ev, picker) {
                    that.modal.$el.find('input[name="modifiedTime"]').daterangepicker(_.extend(modifiedDateObj, { "minDate": $createTime.val() }));
                });
                this.initializeValidation();
                if (this.ui.entityInputData.find('fieldset').length > 0 && this.ui.entityInputData.find('select.true,input.true').length === 0) {
                    this.requiredAllToggle(this.ui.entityInputData.find('select.true,input.true').length === 0);
                    if (!this.guid) {
                        // For create entity bind keyup for non-required field when all elements are optional
                        this.bindNonRequiredField();
                    }
                }
                this.$('select[data-type="boolean"]').each(function(value, key) {
                    var dataKey = $(key).data('key');
                    if (that.entityData) {
                        var setValue = that.entityData.get("entity").attributes[dataKey];
                        this.value = setValue;
                    }
                });
                this.addJsonSearchData();
            },
            initializeValidation: function() {
                // IE9 allow input type number
                var regex = /^[0-9]*((?=[^.]|$))?$/, // allow only numbers [0-9]
                    removeText = function(e, value) {
                        if (!regex.test(value)) {
                            var txtfld = e.currentTarget;
                            var newtxt = txtfld.value.slice(0, txtfld.value.length - 1);
                            txtfld.value = newtxt;
                        }
                    }
                this.$('input[data-type="int"],input[data-type="long"]').on('keydown', function(e) {
                    // allow only numbers [0-9]
                    if (!regex.test(e.currentTarget.value)) {
                        return false;
                    }
                });
                this.$('input[data-type="int"],input[data-type="long"]').on('paste', function(e) {
                    return false;
                });

                this.$('input[data-type="long"],input[data-type="int"]').on('keyup click', function(e) {
                    removeText(e, e.currentTarget.value);
                });

                this.$('input[data-type="date"]').on('hide.daterangepicker keydown', function(event) {
                    if (event.type) {
                        if (event.type == 'hide') {
                            this.blur();
                        } else if (event.type == 'keydown') {
                            return false;
                        }
                    }
                });
            },
            getContainer: function(object) {
                var value = object.value,
                    entityLabel = this.capitalize(_.escape(value.name));

                return '<div class=" row ' + value.isOptional + '"><span class="col-sm-3">' +
                    '<label><span class="' + (value.isOptional ? 'true' : 'false required') + '">' + entityLabel + '</span><span class="center-block ellipsis-with-margin text-gray" title="Data Type : ' + value.typeName + '">' + '(' + _.escape(value.typeName) + ')' + '</span></label></span>' +
                    '<span class="col-sm-9">' + (this.getElement(object)) +
                    '</input></span></div>';
            },
            getFieldElementContainer: function(object) {
                var htmlField = object.htmlField,
                    attributeType = object.attributeType ? object.attributeType : false,
                    relationshipType = object.relationshipType ? object.relationshipType : false,
                    alloptional = object.alloptional,
                    typeOfDefination = (relationshipType ? "Relationships" : "Attributes");
                return '<div class="attribute-dash-box ' + (alloptional ? "alloptional" : "") + ' "><span class="attribute-type-label">' + (typeOfDefination) + '</span>' + htmlField + '</div>';
            },
            getSelect: function(object) {
                var value = object.value,
                    name = _.escape(value.name),
                    entityValue = _.escape(object.entityValue),
                    isAttribute = object.isAttribute,
                    isRelation = object.isRelation;
                if (value.typeName === "boolean") {
                    return '<select class="form-control row-margin-bottom ' + (value.isOptional === true ? "false" : "true") +
                        '" data-type="' + value.typeName +
                        '" data-attribute="' + isAttribute +
                        '" data-relation="' + isRelation +
                        '" data-key="' + name +
                        '" data-id="entityInput">' +
                        '<option value="">--Select true or false--</option><option value="true">true</option>' +
                        '<option value="false">false</option></select>';
                } else {
                    var splitTypeName = value.typeName.split("<");
                    if (splitTypeName.length > 1) {
                        splitTypeName = splitTypeName[1].split(">")[0];
                    } else {
                        splitTypeName = value.typeName;
                    }
                    return '<select class="form-control row-margin-bottom entityInputBox ' + (value.isOptional === true ? "false" : "true") +
                        '" data-type="' + value.typeName +
                        '" data-attribute="' + isAttribute +
                        '" data-relation="' + isRelation +
                        '" data-key="' + name +
                        '" data-id="entitySelectData" data-queryData="' + splitTypeName + '">' + (this.guid ? entityValue : "") + '</select>';
                }

            },
            getTextArea: function(object) {
                var value = object.value,
                    name = _.escape(value.name),
                    setValue = _.escape(object.entityValue),
                    isAttribute = object.isAttribute,
                    isRelation = object.isRelation,
                    structType = object.structType;
                try {
                    if (structType && entityValue && entityValue.length) {
                        var parseValue = JSON.parse(entityValue);
                        if (_.isObject(parseValue) && !_.isArray(parseValue) && parseValue.attributes) {
                            setValue = JSON.stringify(parseValue.attributes);
                        }
                    }
                } catch (err) {}

                return '<textarea class="form-control entityInputBox ' + (value.isOptional === true ? "false" : "true") + '"' +
                    ' data-type="' + value.typeName + '"' +
                    ' data-key="' + name + '"' +
                    ' data-attribute="' + isAttribute + '"' +
                    ' data-relation="' + isRelation + '"' +
                    ' placeholder="' + name + '"' +
                    ' data-id="entityInput">' + setValue + '</textarea>';

            },
            getInput: function(object) {
                var value = object.value,
                    name = _.escape(value.name),
                    entityValue = _.escape(object.entityValue),
                    isAttribute = object.isAttribute,
                    isRelation = object.isRelation;
                return '<input class="form-control entityInputBox ' + (value.isOptional === true ? "false" : "true") + '"' +
                    ' data-type="' + value.typeName + '"' +
                    ' value="' + entityValue + '"' +
                    ' data-key="' + name + '"' +
                    ' data-attribute="' + isAttribute + '"' +
                    ' data-relation="' + isRelation + '"' +
                    ' placeholder="' + name + '"' +
                    ' name="' + name + '"' +
                    ' data-id="entityInput">';
            },
            getElement: function(object) {
                var value = object.value,
                    isAttribute = object.isAttribute,
                    isRelation = object.isRelation,
                    typeName = value.typeName,
                    entityValue = "";
                if (this.guid) {
                    var dataValue = this.entityData.get("entity").attributes[value.name];
                    if (_.isObject(dataValue)) {
                        entityValue = JSON.stringify(dataValue);
                    } else {
                        if (dataValue) {
                            entityValue = dataValue;
                        }
                        if (value.typeName === "date") {
                            if (dataValue) {
                                entityValue = moment(dataValue);
                            } else {
                                entityValue = Utils.formatDate({ zone: false, dateFormat: Globals.dateFormat });

                            }
                        }
                    }
                }
                if ((typeName && this.entityDefCollection.fullCollection.find({ name: typeName })) || typeName === "boolean" || typeName.indexOf("array") > -1) {
                    return this.getSelect({
                        "value": value,
                        "entityValue": entityValue,
                        "isAttribute": isAttribute,
                        "isRelation": isRelation

                    });
                } else if (typeName.indexOf("map") > -1) {
                    return this.getTextArea({
                        "value": value,
                        "entityValue": entityValue,
                        "isAttribute": isAttribute,
                        "isRelation": isRelation,
                        "structType": false
                    });
                } else {
                    var typeNameCategory = this.typeHeaders.fullCollection.findWhere({ name: typeName });
                    if (typeNameCategory && typeNameCategory.get('category') === 'STRUCT') {
                        return this.getTextArea({
                            "value": value,
                            "entityValue": entityValue,
                            "isAttribute": isAttribute,
                            "isRelation": isRelation,
                            "structType": true
                        });
                    } else {
                        return this.getInput({
                            "value": value,
                            "entityValue": entityValue,
                            "isAttribute": isAttribute,
                            "isRelation": isRelation,
                        });
                    }
                }
            },
            okButton: function() {
                var that = this;
                this.showLoader({
                    editVisiblityOfEntitySelectionBox: true
                });
                this.parentEntity = this.ui.entityList.val();
                var entityAttribute = {},
                    referredEntities = {},
                    relationshipAttribute = {};
                var extractValue = function(value, typeName) {
                    if (!value) {
                        return value;
                    }
                    if (_.isArray(value)) {
                        var parseData = [];
                        _.map(value, function(val) {
                            parseData.push({ 'guid': val, 'typeName': typeName });
                        });
                    } else {
                        var parseData = { 'guid': value, 'typeName': typeName };
                    }
                    return parseData;
                }
                try {
                    this.ui.entityInputData.find("input,select,textarea").each(function() {
                        var value = $(this).val(),
                            el = this;
                        if ($(this).val() && $(this).val().trim) {
                            value = $(this).val().trim();
                        }
                        if (this.nodeName === "TEXTAREA") {
                            try {
                                if (value && value.length) {
                                    JSON.parse(value);
                                    $(this).removeClass('errorClass');
                                }
                            } catch (err) {
                                throw new Error(err.message);
                                $(this).addClass('errorClass');
                            }
                        }
                        // validation
                        if ($(this).hasClass("true")) {
                            if (value == "" || value == undefined) {
                                if ($(this).data('select2')) {
                                    $(this).data('select2').$container.find('.select2-selection').addClass("errorClass")
                                } else {
                                    $(this).addClass('errorClass');
                                }
                                that.hideLoader();
                                that.modal.$el.find('button.ok').hideButtonLoader();
                                throw new Error("Please fill the required fields");
                                return;
                            }
                        }
                        var dataTypeEnitity = $(this).data('type'),
                            datakeyEntity = $(this).data('key'),
                            typeName = $(this).data('querydata'),
                            attribute = $(this).data('attribute') == 'undefined' ? false : true,
                            relation = $(this).data('relation') == 'undefined' ? false : true,
                            typeNameCategory = that.typeHeaders.fullCollection.findWhere({ name: dataTypeEnitity }),
                            val = null;
                        // Extract Data
                        if (dataTypeEnitity && datakeyEntity) {
                            if (that.entityDefCollection.fullCollection.find({ name: dataTypeEnitity })) {
                                val = extractValue(value, typeName);
                            } else if (dataTypeEnitity === 'date' || dataTypeEnitity === 'time') {
                                val = Date.parse(value);
                            } else if (dataTypeEnitity.indexOf("map") > -1 || (typeNameCategory && typeNameCategory.get('category') === 'STRUCT')) {
                                try {
                                    if (value && value.length) {
                                        parseData = JSON.parse(value);
                                        val = parseData;
                                    }
                                } catch (err) {
                                    $(this).addClass('errorClass');
                                    throw new Error(datakeyEntity + " : " + err.message);
                                    return;
                                }
                            } else if (dataTypeEnitity.indexOf("array") > -1 && dataTypeEnitity.indexOf("string") === -1) {
                                val = extractValue(value, typeName);
                            } else {
                                if (_.isString(value)) {
                                    if (value.length) {
                                        val = value;
                                    } else {
                                        val = null;
                                    }
                                } else {
                                    val = value;
                                }
                            }
                            if (attribute) {
                                entityAttribute[datakeyEntity] = val;
                            } else if (relation) {
                                relationshipAttribute[datakeyEntity] = val;
                            }
                        } else {
                            var dataRelEntity = $(this).data('forKey');
                            if (dataRelEntity && relationshipAttribute[dataRelEntity]) {
                                if (_.isArray(relationshipAttribute[dataRelEntity])) {
                                    _.each(relationshipAttribute[dataRelEntity], function(obj) {
                                        if (obj) {
                                            obj["relationshipType"] = $(el).val();
                                        }
                                    });
                                } else {
                                    relationshipAttribute[dataRelEntity]["relationshipType"] = $(this).val();
                                }

                            }
                        }
                    });
                    var entityJson = {
                        "entity": {
                            "typeName": (this.guid ? this.entityData.get("entity").typeName : this.parentEntity),
                            "attributes": entityAttribute,
                            "relationshipAttributes": relationshipAttribute,
                            "guid": (this.guid ? this.guid : -1)
                        },
                        "referredEntities": referredEntities
                    };
                    this.entityModel.createOreditEntity({
                        data: JSON.stringify(entityJson),
                        type: "POST",
                        success: function(model, response) {
                            that.modal.$el.find('button.ok').hideButtonLoader();
                            that.modal.close();
                            var msgType = (model.mutatedEntities && model.mutatedEntities.UPDATE) ? "editSuccessMessage" : "addSuccessMessage";
                            Utils.notifySuccess({
                                content: "Entity" + Messages.getAbbreviationMsg(false, msgType)
                            });
                            if (that.guid && that.callback) {
                                that.callback();
                            } else {
                                if (that.searchVent) {
                                    that.searchVent.trigger("Entity:Count:Update");
                                }
                                if (model.mutatedEntities) {
                                    var mutatedEntities = model.mutatedEntities.CREATE || model.mutatedEntities.UPDATE;
                                    if (mutatedEntities && _.isArray(mutatedEntities) && mutatedEntities[0] && mutatedEntities[0].guid) {
                                        Utils.setUrl({
                                            url: '#!/detailPage/' + (mutatedEntities[0].guid),
                                            mergeBrowserUrl: false,
                                            trigger: true
                                        });
                                    }
                                }
                            }
                        },
                        complete: function() {
                            that.hideLoader({
                                editVisiblityOfEntitySelectionBox: true
                            });
                            that.modal.$el.find('button.ok').hideButtonLoader();
                        }
                    });

                } catch (e) {
                    Utils.notifyError({
                        content: e.message
                    });
                    that.hideLoader({
                        editVisiblityOfEntitySelectionBox: true
                    });
                }
            },
            showLoader: function(options) {
                var editVisiblityOfEntitySelectionBox = options && options.editVisiblityOfEntitySelectionBox;
                this.$('.entityLoader').addClass('show');
                this.$('.entityInputData').hide();
                if (this.guid || editVisiblityOfEntitySelectionBox) {
                    this.ui.entitySelectionBox.hide();
                }
            },
            hideLoader: function(options) {
                var editVisiblityOfEntitySelectionBox = options && options.editVisiblityOfEntitySelectionBox
                this.$('.entityLoader').removeClass('show');
                this.$('.entityInputData').show();
                if (this.guid || editVisiblityOfEntitySelectionBox) {
                    this.ui.entitySelectionBox.show();
                }
                // To enable scroll after selecting value from select2.
                this.ui.entityList.select2('open');
                this.ui.entityList.select2('close');
            },
            addJsonSearchData: function() {
                var that = this;
                this.$('select[data-id="entitySelectData"]').each(function(value, key) {
                    var $this = $(this),
                        keyData = $(this).data("key"),
                        typeData = $(this).data("type"),
                        queryData = $(this).data("querydata"),
                        skip = $(this).data('skip'),
                        placeholderName = "Select a " + typeData + " from the dropdown list";
                    $this.attr("multiple", ($this.data('type').indexOf("array") === -1 ? false : true));

                    // Select Value.
                    if (that.guid) {
                        var dataValue = that.entityData.get("entity").attributes[keyData],
                            entities = that.entityData.get("entity").attributes,
                            relationshipType = that.entityData.get("entity").relationshipAttributes ? that.entityData.get("entity").relationshipAttributes[keyData] : null,
                            referredEntities = that.entityData.get("referredEntities"),
                            selectedValue = [],
                            select2Options = [];
                        if (dataValue) {
                            if (_.isObject(dataValue) && !_.isArray(dataValue)) {
                                dataValue = [dataValue];
                            }
                            _.each(dataValue, function(obj) {
                                if (_.isObject(obj) && obj.guid && referredEntities[obj.guid]) {
                                    var refEntiyFound = referredEntities[obj.guid];
                                    refEntiyFound['id'] = refEntiyFound.guid;
                                    if (!Enums.entityStateReadOnly[refEntiyFound.status]) {
                                        select2Options.push(refEntiyFound);
                                        selectedValue.push(refEntiyFound.guid);
                                    }
                                }
                            });
                            if (!_.isUndefined(relationshipType)) {
                                if (relationshipType && relationshipType.relationshipAttributes && relationshipType.relationshipAttributes.typeName) {
                                    that.$("select[data-for-key=" + keyData + "]").val(relationshipType.relationshipAttributes.typeName).trigger("change");
                                }

                            }
                        }

                        // Array of string.
                        if (selectedValue.length === 0 && dataValue && dataValue.length && ($this.data('querydata') === "string")) {
                            var str = "";
                            _.each(dataValue, function(obj) {
                                if (_.isString(obj)) {
                                    selectedValue.push(obj);
                                    str += '<option>' + _.escape(obj) + '</option>';
                                }
                            });
                            $this.html(str);
                        }

                    } else {
                        $this.val([]);
                    }
                    var select2Option = {
                        placeholder: placeholderName,
                        allowClear: true,
                        tags: ($this.data('querydata') == "string" ? true : false)
                    }
                    var getTypeAheadData = function(data, params) {
                        var dataList = data.entities,
                            foundOptions = [];
                        _.each(dataList, function(obj) {
                            if (obj) {
                                if (obj.guid) {
                                    obj['id'] = obj.guid;
                                }
                                foundOptions.push(obj);
                            }
                        });
                        return foundOptions;
                    }
                    if ($this.data('querydata') !== "string") {
                        _.extend(select2Option, {
                            ajax: {
                                url: UrlLinks.searchApiUrl('attribute'),
                                dataType: 'json',
                                delay: 250,
                                data: function(params) {
                                    return {
                                        attrValuePrefix: params.term, // search term
                                        typeName: queryData,
                                        limit: 10,
                                        offset: 0
                                    };
                                },
                                processResults: function(data, params) {
                                    return {
                                        results: getTypeAheadData(data, params)
                                    };
                                },
                                cache: true
                            },
                            templateResult: function(option) {
                                var name = Utils.getName(option, 'qualifiedName');
                                return name === "-" ? option.text : name;
                            },
                            templateSelection: function(option) {
                                var name = Utils.getName(option, 'qualifiedName');
                                return name === "-" ? option.text : name;
                            },
                            escapeMarkup: function(markup) {
                                return markup;
                            },
                            data: select2Options,
                            minimumInputLength: 1
                        });
                    }
                    $this.select2(select2Option);
                    if (selectedValue) {
                        $this.val(selectedValue).trigger("change");
                    }

                });
                if (this.guid) {
                    this.bindRequiredField();
                    this.bindNonRequiredField();
                }
                this.hideLoader();
            }
        });
    return CreateEntityLayoutView;
});