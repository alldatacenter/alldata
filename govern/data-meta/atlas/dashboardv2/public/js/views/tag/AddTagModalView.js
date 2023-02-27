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
    'hbs!tmpl/tag/AddTagModalView_tmpl',
    'views/tag/AddTimezoneItemView',
    'collection/VTagList',
    'collection/VCommonList',
    'modules/Modal',
    'models/VEntity',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/Enums',
    'utils/Messages',
    'moment',
    'utils/Globals',
    'moment-timezone',
    'daterangepicker'
], function(require, AddTagModalViewTmpl, AddTimezoneItemView, VTagList, VCommonList, Modal, VEntity, Utils, UrlLinks, Enums, Messages, moment, Globals) {
    'use strict';

    var AddTagModel = Backbone.Marionette.CompositeView.extend({
        template: AddTagModalViewTmpl,
        templateHelpers: function() {
            return {
                tagModel: this.tagModel
            };
        },
        childView: AddTimezoneItemView,
        childViewOptions: function() {
            return {
                // saveButton: this.ui.saveButton,
                parentView: this,
                tagModel: this.tagModel
            };
        },
        childViewContainer: "[data-id='addTimezoneDiv']",
        regions: {},
        ui: {
            addTagOptions: "[data-id='addTagOptions']",
            tagAttribute: "[data-id='tagAttribute']",
            checkTimeZone: "[data-id='checkTimezoneProperty']",
            timeZoneDiv: "[data-id='timeZoneDiv']",
            checkTagModalPropagate: "[data-id='checkModalTagProperty']",
            addTimezoneParms: "[data-id='addTimezoneParms']",
            validityPeriodBody: "[data-id='validityPeriodBody']",
            removePropagationOnEntityDelete: "[data-id='removePropagationOnEntityDelete']",
            removePropagationOnEntityDeleteBox: "[data-id='removePropagationOnEntityDeleteBox']"
        },
        events: function() {
            var events = {},
                that = this;
            events["change " + this.ui.addTagOptions] = 'onChangeTagDefination';
            events["change " + this.ui.checkTagModalPropagate] = function(e) {
                if (e.target.checked) {
                    that.ui.removePropagationOnEntityDeleteBox.show();
                    that.$('.addtag-propagte-box').removeClass('no-border');
                } else {
                    that.$('.addtag-propagte-box').addClass('no-border');
                    that.ui.removePropagationOnEntityDeleteBox.hide();
                }
                if (that.tagModel) {
                    that.buttonActive({ isButtonActive: true });
                }
            };
            events["change " + this.ui.removePropagationOnEntityDelete] = function() {
                if (that.tagModel) {
                    that.buttonActive({ isButtonActive: true });
                }
            };
            events["change " + this.ui.checkTimeZone] = function(e) {
                if (this.tagModel) {
                    this.buttonActive({ isButtonActive: true });
                }
                if (e.target.checked) {
                    this.ui.timeZoneDiv.show();
                    this.ui.validityPeriodBody.show();
                    if (_.isEmpty(this.collection.models)) {
                        this.collection.add(new Backbone.Model({
                            "startTime": "",
                            "endTime": "",
                            "timeZone": ""
                        }));
                    }
                } else {
                    this.ui.timeZoneDiv.hide();
                    this.ui.validityPeriodBody.hide();
                }
            };
            events["click " + this.ui.addTimezoneParms] = 'addTimezoneBtn'
            return events;
        },
        /**
         * intialize a new AddTagModel Layout
         * @constructs
         */
        initialize: function(options) {
            _.extend(this, _.pick(options, 'modalCollection', 'guid', 'callback', 'multiple', 'entityCount', 'showLoader', 'hideLoader', 'tagList', 'tagModel', 'enumDefCollection'));
            this.commonCollection = new VTagList();
            if (this.tagModel) {
                this.collection = new Backbone.Collection(this.tagModel.validityPeriods);
            } else {
                this.collection = new Backbone.Collection();
            }
            this.tagCollection = options.collection;
            var that = this,
                modalObj = {
                    title: 'Add Classification',
                    content: this,
                    okText: 'Add',
                    cancelText: "Cancel",
                    mainClass: 'modal-lg',
                    allowCancel: true,
                    okCloses: false
                };
            if (this.tagModel) {
                modalObj.title = 'Edit Classification';
                modalObj.okText = 'Update';
            }
            this.modal = new Modal(modalObj)
            this.modal.open();
            this.modal.$el.find('button.ok').attr("disabled", true);
            this.on('ok', function() {
                if (this.ui.checkTimeZone.is(':checked')) {
                    if (this.validateValues()) {
                        if (this.hideLoader) {
                            this.hideLoader();
                        };
                        return;
                    };
                }
                that.modal.$el.find('button.ok').showButtonLoader();
                var tagName = this.tagModel ? this.tagModel.typeName : this.ui.addTagOptions.val(),
                    tagAttributes = {},
                    tagAttributeNames = this.$(".attrName"),
                    obj = {
                        tagName: tagName,
                        tagAttributes: tagAttributes,
                        guid: [],
                        skipEntity: [],
                        deletedEntity: []
                    },
                    isValidateAttrValue = true,
                    validationKey = [];
                tagAttributeNames.each(function(i, item) {
                    var selection = $(item).data("key"),
                        isRequired = $(item).hasClass('required'),
                        datatypeSelection = $(item).data("type"),
                        $valueElement = $(item);
                    $valueElement.removeClass('errorValidate');
                    if (datatypeSelection === "date") {
                        tagAttributes[selection] = Date.parse($valueElement.val()) || null;
                    } else {
                        if (isRequired) {
                            if ($valueElement.val().length) {
                                tagAttributes[selection] = $(item).val() || null;
                                // tagAttributes[selection] = that.getValue($valueElement, datatypeSelection);
                            } else {
                                isValidateAttrValue = false;
                                $valueElement.addClass('errorValidate');
                                validationKey.push($(item).data("key"));
                            }
                        } else {
                            tagAttributes[selection] = $(item).val() || null;
                            // tagAttributes[selection] = that.getValue($valueElement, datatypeSelection);
                        }
                    }
                });

                if (that.multiple) {
                    _.each(that.multiple, function(entity, i) {
                        var name = Utils.getName(entity.model);
                        if (Enums.entityStateReadOnly[entity.model.status]) {
                            obj.deletedEntity.push(name);
                        } else {
                            if (_.indexOf((entity.model.classificationNames || _.pluck(entity.model.classifications, 'typeName')), tagName) === -1) {
                                obj.guid.push(entity.model.guid)
                            } else {
                                obj.skipEntity.push(name);
                            }
                        }
                    });
                    if (obj.deletedEntity.length) {
                        Utils.notifyError({
                            html: true,
                            content: "<b>" + obj.deletedEntity.join(', ') +
                                "</b> " + (obj.deletedEntity.length === 1 ? "entity " : "entities ") +
                                Messages.assignDeletedEntity
                        });
                        that.modal.close();
                    }
                    if (obj.skipEntity.length) {
                        var text = "<b>" + obj.skipEntity.length + " of " + that.multiple.length +
                            "</b> entities selected have already been associated with <b>" + tagName +
                            "</b> tag, Do you want to associate the tag with other entities ?",
                            removeCancelButton = false;
                        if ((obj.skipEntity.length + obj.deletedEntity.length) === that.multiple.length) {
                            text = (obj.skipEntity.length > 1 ? "All selected" : "Selected") + " entities have already been associated with <b>" + tagName + "</b> tag";
                            removeCancelButton = true;
                        }
                        var notifyObj = {
                            text: text,
                            modal: true,
                            ok: function(argument) {
                                if (obj.guid.length) {
                                    that.saveTagData(obj);
                                } else {
                                    that.hideLoader();
                                }
                            },
                            cancel: function(argument) {
                                that.hideLoader();
                                obj = {
                                    tagName: tagName,
                                    tagAttributes: tagAttributes,
                                    guid: [],
                                    skipEntity: [],
                                    deletedEntity: []
                                }
                            }
                        }
                        if (removeCancelButton) {
                            notifyObj['confirm'] = {
                                confirm: true,
                                buttons: [{
                                        text: 'Ok',
                                        addClass: 'btn-atlas btn-md',
                                        click: function(notice) {
                                            notice.remove();
                                            obj = {
                                                tagName: tagName,
                                                tagAttributes: tagAttributes,
                                                guid: [],
                                                skipEntity: [],
                                                deletedEntity: []
                                            }
                                        }
                                    },
                                    null
                                ]
                            }
                        }
                        Utils.notifyConfirm(notifyObj)
                    } else {
                        if (obj.guid.length) {
                            that.saveTagData(obj);
                        } else {
                            that.hideLoader();
                        }
                    }
                } else if (isValidateAttrValue === false) {
                    var validationMsg = "" + _.each(validationKey, function(key) {
                        return key + " ";
                    });
                    that.modal.$el.find('button.ok').hideButtonLoader();
                    Utils.notifyInfo({
                        content: "Value for " + validationMsg + " cannot be empty"
                    });
                    if (this.hideLoader) {
                        this.hideLoader();
                    };
                    return;
                } else {
                    obj.guid.push(that.guid);
                    that.saveTagData(obj);
                }
            });
            this.on('closeModal', function() {
                this.modal.trigger('cancel');
            });
            this.bindEvents();
        },
        getValue: function($item, datatypeSelection) {
            var that = this;
            return (datatypeSelection && datatypeSelection.indexOf("array") >= 0) ? that.getArrayValues($item) : ($item.val() || null);
        },
        getArrayValues: function($item) {
            var that = this,
                arrayValues = $item.val();
            if ($item.hasClass("set-array") || arrayValues.length == 0) {
                return (arrayValues || null);
            } else {
                return _.map(arrayValues, function(value) {
                    var splitBy = that.guid + "_",
                        splitedValue = value.split(splitBy);
                    return (splitedValue.length > 1) ? splitedValue[1] : splitedValue[0];
                })
            }
        },
        validateValues: function(attributeDefs) {
            var isValidate = true,
                applyErrorClass = function(scope) {
                    if (this.value == '' || this.value == null || this.value.indexOf('Select Timezone') > -1) {
                        $(this).addClass('errorValidate');
                        if (isValidate) { isValidate = false; }
                    } else {
                        $(this).removeClass('errorValidate');
                    }
                };

            this.$el.find('.start-time').each(function(element) {
                applyErrorClass.call(this);
            });
            this.$el.find('.end-time').each(function(element) {
                applyErrorClass.call(this);
            });
            this.$el.find('.time-zone').each(function(element) {
                applyErrorClass.call(this);
            });
            if (!isValidate) {
                Utils.notifyInfo({
                    content: "Please fill the details"
                });
                return true;
            }
        },

        onRender: function() {
            var that = this;
            this.propagate,
                this.hideAttributeBox();
            this.tagsCollection();
            if (this.tagModel) {
                this.fetchTagSubData(that.tagModel.typeName);
                // Added === true because if value is null then use false.
                that.ui.checkTagModalPropagate.prop('checked', this.tagModel.propagate === true ? true : false).trigger('change');
                that.ui.checkTimeZone.prop('checked', _.isEmpty(this.tagModel.validityPeriods) ? false : true);
                that.ui.removePropagationOnEntityDelete.prop('checked', this.tagModel.removePropagationsOnEntityDelete == true ? true : false);
                if (_.isEmpty(this.tagModel.validityPeriods)) {
                    that.ui.timeZoneDiv.hide()
                } else {
                    that.ui.timeZoneDiv.show();
                }
                that.checkTimezoneProperty(that.ui.checkTimeZone[0]);
            }
            that.showAttributeBox();
        },
        addTimezoneBtn: function() {
            this.ui.validityPeriodBody.show();
            this.collection.add(new Backbone.Model({
                "startTime": "",
                "endTime": "",
                "timeZone": ""
            }));
        },
        bindEvents: function() {
            var that = this;
            this.enumArr = [];
            this.listenTo(this.tagCollection, 'reset', function() {
                this.tagsCollection();
            }, this);
            this.listenTo(this.commonCollection, 'reset', function() {
                this.subAttributeData();
            }, this);
        },
        tagsCollection: function() {
            var that = this,
                str = '<option selected="selected" disabled="disabled">-- Select a Classification from the dropdown list --</option>';
            this.tagCollection.fullCollection.each(function(obj, key) {
                var name = Utils.getName(obj.toJSON(), 'name');
                // using obj.get('name') insted of name variable because if html is presen in name then escaped name will not found in tagList.
                if (_.indexOf(that.tagList, obj.get('name')) === -1) {
                    str += '<option ' + (that.tagModel && that.tagModel.typeName === name ? 'selected' : '') + '>' + name + '</option>';
                }
            });
            this.ui.addTagOptions.html(str);
            this.ui.addTagOptions.select2({
                placeholder: "Select Tag",
                allowClear: false
            });
        },
        onChangeTagDefination: function() {
            this.ui.addTagOptions.select2("open").select2("close");
            this.ui.tagAttribute.empty();
            var saveBtn = this.modal.$el.find('button.ok');
            saveBtn.prop("disabled", false);
            var tagname = this.ui.addTagOptions.val();
            this.hideAttributeBox();
            this.fetchTagSubData(tagname);
        },
        fetchTagSubData: function(tagname) {
            var attributeDefs = Utils.getNestedSuperTypeObj({
                data: this.tagCollection.fullCollection.find({ name: tagname }).toJSON(),
                collection: this.tagCollection,
                attrMerge: true
            });
            this.subAttributeData(attributeDefs);
        },
        showAttributeBox: function() {
            var that = this,
                isButtonactive;
            this.$('.attrLoader').hide();
            this.$('.form-group.hide').removeClass('hide');
            if (this.ui.tagAttribute.children().length !== 0) {
                this.ui.tagAttribute.parent().show();
            }
            this.ui.tagAttribute.find('input,select').on("keyup change", function(e) {
                if (e.keyCode != 32) {
                    that.buttonActive({ isButtonActive: true });
                }
            });
        },
        buttonActive: function(option) {
            if (option) {
                var isButton = option.isButtonActive;
                this.modal.$el.find('button.ok').attr("disabled", isButton === true ? false : true);
            }
        },
        hideAttributeBox: function() {
            this.ui.tagAttribute.children().empty();
            this.ui.tagAttribute.parent().hide();
            this.$('.attrLoader').show();
        },
        subAttributeData: function(attributeDefs) {
            var that = this;
            if (attributeDefs) {
                _.each(attributeDefs, function(obj) {
                    var name = Utils.getName(obj, 'name');
                    var typeName = Utils.getName(obj, 'typeName'),
                        isOptional = obj.isOptional,
                        inputClassName = "form-control attributeInputVal attrName" + (isOptional ? "" : " required");
                    var typeNameValue = that.enumDefCollection.fullCollection.findWhere({ 'name': typeName });
                    if (typeNameValue) {
                        var str = '<option value=""' + (!that.tagModel ? 'selected' : '') + '>-- Select ' + typeName + " --</option>";
                        var enumValue = typeNameValue.get('elementDefs');
                        _.each(enumValue, function(key, value) {
                            str += '<option ' + ((that.tagModel && key.value === that.tagModel.attributes[name]) ? 'selected' : '') + '>' + _.escape(key.value) + '</option>';
                        })
                        that.ui.tagAttribute.append('<div class="form-group"><label class="' + (isOptional ? "" : " required") + '">' + name + '</label>' + ' (' + typeName + ')' +
                            '<select class="' + inputClassName + '" data-key="' + name + '">' + str + '</select></div>');
                    } else if (typeName.indexOf('array') == 0) {
                        var arraytTypeName = new DOMParser().parseFromString(typeName, "text/html");
                        var addCardinalityClass = inputClassName + " js-states js-example-events form-control" + (obj.cardinality == "SET" ? " set-array" : " list-array");
                        that.ui.tagAttribute.append('<div class="form-group"><label class="' + (isOptional ? "" : " required") + '">' + name + '</label>' + ' (' + typeName + ') ' + obj.cardinality +
                            '<select class="' + addCardinalityClass + '" data-id="addArryString" multiple="multiple" data-key="' + name + '" data-type="' + arraytTypeName.documentElement.textContent + '"></select></div>');
                    } else {
                        var textElement = that.getElement(name, typeName, inputClassName);
                        if (_.isTypePrimitive(typeName)) {
                            that.ui.tagAttribute.append('<div class="form-group"><label class="' + (isOptional ? "" : " required") + '">' + name + '</label>' + ' (' + typeName + ')' + textElement + '</div>');
                        }
                    }
                });
                that.$('input[data-type="date"]').each(function() {
                    if (!$(this).data('daterangepicker')) {
                        var dateObj = {
                            "singleDatePicker": true,
                            "showDropdowns": true,
                            "timePicker": true,
                            startDate: new Date(),
                            locale: {
                                format: Globals.dateTimeFormat
                            }
                        };
                        if (that.tagModel) {
                            if (this.value.length) {
                                var formatDate = Number(this.value);
                                dateObj["startDate"] = new Date(formatDate);
                            }
                        }
                        $(this).daterangepicker(dateObj);
                    }
                });
                that.$('select[data-type="boolean"]').each(function() {
                    var labelName = $(this).data('key');
                    if (that.tagModel) {
                        this.value = that.tagModel.attributes[labelName];
                    }
                });
                that.$('select[data-type="array<string>"]').each(function() {
                    var stringData = $(this).data('key'),
                        self = this;
                    if (that.tagModel) {
                        var stringValues = that.tagModel.attributes[stringData] || [],
                            str = stringValues.map(function(label) {
                                // var id = ($(self).hasClass("list-array")) ? ("" + Utils.generateUUID() + that.guid + "_" + label + "") : ("" + label + "");
                                // return ($(self).hasClass("list-array")) ? "<option selected value=" + id + "> " + _.escape(label) + " </option>" : "<option selected> " + _.escape(label) + " </option>";
                                return "<option selected> " + _.escape(label) + " </option>";
                            });
                        $(this).html(str);
                    }
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
                    $(this).select2({
                            tags: true,
                            multiple: true,
                            createTag: function(data) {
                                // var isSET = this.$element.hasClass("set-array"),
                                //     found = _.find(this.$element.select2("data"), { id: data.term });
                                // if (isSET) {
                                //     if (!found) {
                                //         return { id: data.term, text: data.term };
                                //     }
                                // } else {
                                //     return { id: data.term, text: data.term };
                                // }
                                var found = _.find(this.$element.select2("data"), { id: data.term });
                                if (!found) {
                                    return { id: data.term, text: data.term };
                                }
                            },
                            templateResult: that.formatResultSearch
                        })
                        // .on("select2:select", function(e) {
                        //     if ($(this).hasClass("list-array")) { //allow duplicates value for list
                        //         var options = $(this).find("option"),
                        //             id = "" + Utils.generateUUID() + that.guid + "_" + e.params.data.text + "";
                        //         $(self).append('<option value="' + id + '">' + e.params.data.text + '</option>');
                        //     }
                        // })
                });
                this.showAttributeBox();
            }
        },
        formatResultSearch: function(state) {
            if (state.text.trim() == "") return;
            if (!state.id) return $("<span>Add<strong> '" + _.escape(state.text) + "'</strong></span>");;
            // if (state.element && state.element.selected) return;
            return $("<span>Add<strong> '" + _.escape(state.text) + "'</strong></span>");
        },
        getElement: function(labelName, typeName, inputClassName) {
            var value = this.tagModel && this.tagModel.attributes ? (this.tagModel.attributes[_.unescape(labelName)] || "") : "",
                isTypeNumber = typeName === "int" || typeName === "byte" || typeName === "short" || typeName === "double" || typeName === "float";
            if (isTypeNumber) {
                inputClassName += ((typeName === "int" || typeName === "byte" || typeName === "short") ? " number-input-negative" : " number-input-exponential");
            }
            if (typeName === "boolean") {
                return '<select class="' + inputClassName + '" data-key="' + labelName + '" data-type="' + typeName + '"> ' +
                    '<option value="">--Select true or false--</option>' +
                    '<option value="true">true</option>' +
                    '<option value="false">false</option></select>';
            } else {
                return '<input type="text" value="' + _.escape(value) + '" class="' + inputClassName + '" data-key="' + labelName + '" data-type="' + typeName + '"/>';
            }
        },
        checkTimezoneProperty: function(e) {
            if (e.checked) {
                this.ui.timeZoneDiv.show();
                this.ui.validityPeriodBody.show();
            } else {
                this.ui.timeZoneDiv.hide();
                this.ui.validityPeriodBody.hide();
            }
        },
        saveTagData: function(options) {
            var that = this;
            this.entityModel = new VEntity();
            var tagName = options.tagName,
                tagAttributes = options.tagAttributes,
                validityPeriodVal = that.ui.checkTimeZone.is(':checked') ? that.collection.toJSON() : [],
                classificationData = {
                    "typeName": tagName,
                    "attributes": tagAttributes,
                    "propagate": that.ui.checkTagModalPropagate.is(":checked") === true ? true : false,
                    "removePropagationsOnEntityDelete": that.ui.removePropagationOnEntityDelete.is(":checked") === true ? true : false,
                    "validityPeriods": validityPeriodVal
                },
                json = {
                    "classification": classificationData,
                    "entityGuids": options.guid
                };
            if (this.tagModel) {
                json = [classificationData]
            }
            if (this.showLoader) {
                this.showLoader();
            }
            this.entityModel.saveTraitsEntity(this.tagModel ? options.guid : null, {
                data: JSON.stringify(json),
                type: this.tagModel ? 'PUT' : 'POST',
                defaultErrorMessage: "Tag " + tagName + " could not be added",
                success: function(data) {
                    var addupdatetext = that.tagModel ? 'updated successfully to ' : 'added to ';
                    Utils.notifySuccess({
                        content: "Classification " + tagName + " has been " + addupdatetext + (that.entityCount > 1 ? "entities" : "entity")
                    });
                    if (options.modalCollection) {
                        options.modalCollection.fetch({ reset: true });
                    }
                    if (that.callback) {
                        that.callback();
                    }
                },
                cust_error: function(model, response) {
                    that.modal.$el.find('button.ok').hideButtonLoader();
                    if (that.hideLoader) {
                        that.hideLoader();
                    }
                },
                complete: function() {
                    that.modal.close();
                }
            });
        },
    });
    return AddTagModel;
});