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
    'utils/Utils',
    'hbs!tmpl/entity/EntityBusinessMetaDataItemView_tmpl',
    'moment',
    'utils/Globals',
    'daterangepicker'
], function(require, Backbone, Utils, EntityBusinessMetaDataItemViewTmpl, moment, Globals) {
    'use strict';

    return Backbone.Marionette.ItemView.extend({
        _viewName: 'EntityBusinessMetaDataItemView',

        template: EntityBusinessMetaDataItemViewTmpl,

        templateHelpers: function() {
            return {
                editMode: this.editMode,
                entity: this.entity,
                getValue: this.getValue.bind(this),
                getBusinessMetadataDroupdown: this.getBusinessMetadataDroupdown.bind(this),
                businessMetadataCollection: this.businessMetadataCollection,
                model: this.model.toJSON()
            }
        },
        tagName: 'li',
        className: "business-metadata-tree-child",

        /** Layout sub regions */
        regions: {},

        /** ui selector cache */
        ui: {
            keyEl: "[data-id='key']",
            valueEl: "[data-type='value']",
            addItem: "[data-id='addItem']",
            deleteItem: "[data-id='deleteItem']",
            editMode: "[data-id='editMode']"
        },
        /** ui events hash */
        events: function() {
            var events = {};
            events["click " + this.ui.deleteItem] = 'onDeleteItem';
            events["change " + this.ui.keyEl] = 'onAttrChange';
            return events;
        },

        /**
         * intialize a new EntityBusinessMetaDataItemView Layout
         * @constructs
         */
        initialize: function(options) {
            _.extend(this, options);
        },
        onRender: function() {
            var that = this;
            this.ui.keyEl.val("");
            this.ui.keyEl.select2({ placeholder: "Select Attribute" });

            if (this.editMode && (!this.model.has("isNew"))) {
                this.getEditBusinessMetadataEl();
            }
            this.initializeElement();
            this.bindEvent();
        },
        initializeElement: function() {},
        bindEvent: function() {
            var that = this;
            if (this.editMode) {
                this.listenTo(this.model.collection, 'destroy unset:attr', function() {
                    if (this.model.has("isNew")) {
                        this.render();
                    }
                });
                this.listenTo(this.model.collection, 'selected:attr', function(value, model) {
                    if (model.cid !== this.model.cid && this.model.has("isNew")) {
                        var $select2el = that.$el.find('.custom-col-1:first-child>select[data-id="key"]');
                        $select2el.find('option[value="' + value + '"]').remove();
                        var select2val = $select2el.select2("val");
                        $select2el.select2({ placeholder: "Select Attribute" });
                        if (this.model.keys().length <= 2) {
                            $select2el.val("").trigger("change", true);
                        }
                    }
                });
                this.$el.off("change", ".custom-col-1[data-id='value']>[data-key]").on("change", ".custom-col-1[data-id='value']>[data-key]", function(e) {
                    var key = $(this).data("key"),
                        businessMetadata = $(this).data("businessMetadata"),
                        typeName = $(this).data("typename"),
                        multi = $(this).data("multi"),
                        updateObj = that.model.toJSON();
                    if (_.isUndefinedNull(updateObj[key])) {
                        updateObj[key] = { value: null, typeName: typeName };
                    }
                    updateObj[key].value = e.currentTarget.value;
                    if (multi && typeName.indexOf("date") == -1) {
                        updateObj[key].value = $(this).select2("val");
                    }
                    if (!that.model.has("__internal_UI_businessMetadataName")) {
                        updateObj["__internal_UI_businessMetadataName"] = businessMetadata;
                    }
                    if (typeName === "date" || typeName === "array<date>") {
                        if (multi && updateObj[key].value) {
                            var dateValues = updateObj[key].value.split(','),
                                dateStr = [];
                            if (dateValues.length) {
                                _.each(dateValues, function(selectedDate) {
                                    dateStr.push(new Date(selectedDate.trim()).getTime());
                                });
                                updateObj[key].value = dateStr;
                            }
                        } else {
                            updateObj[key].value = new Date(updateObj[key].value).getTime()
                        }
                    }
                    that.model.set(updateObj);
                });
                this.$el.on('keypress', '.select2_only_number .select2-search__field', function() {
                    var typename = $(this).parents(".select2_only_number").find("select[data-typename]").data("typename")
                    if ((typename === "float" || typename === "array<float>") && event.which == 46) {
                        return;
                    }
                    if ((event.which < 48 || event.which > 57) && event.key !== "-") {
                        event.preventDefault();
                    }
                });
            }
        },
        getAttrElement: function(opt) {
            var that = this,
                returnEL = "N/A",
                options = $.extend(true, {}, opt);
            if (options) {
                var key = options.key,
                    typeName = options.val.typeName || "",
                    val = options.val.value,
                    isMultiValued = typeName && typeName.indexOf("array<") === 0,
                    businessMetadata = options.businessMetadata,
                    allowOnlyNum = false,
                    isEnum = false;
                var elType = isMultiValued ? "select" : "input";
                if (!isMultiValued && !_.isEmpty(val)) {
                    val = _.escape(val);
                }
                if (!_.isUndefinedNull(val) && (typeName === "boolean" || typeName === "array<boolean>")) {
                    val = String(val);
                }
                if (typeName === "date" || typeName === "array<date>") {
                    if (isMultiValued && val && val.length) {
                        var dateStr = [];
                        _.each(val, function(selectedDate) {
                            selectedDate = parseInt(selectedDate);
                            dateStr.push(Utils.formatDate({ date: selectedDate, zone: false, dateFormat: Globals.dateFormat }));
                        });
                        val = dateStr.join(',');
                    } else if (!isMultiValued && val) {
                        val = parseInt(val);
                        val = Utils.formatDate({ date: val, zone: false, dateFormat: Globals.dateFormat });

                    }
                }
                if (typeName === "string" || typeName === "array<string>") {
                    returnEL = '<' + elType + ' type="text" data-key="' + key + '" data-businessMetadata="' + businessMetadata + '" data-typename="' + typeName + '" data-multi="' + isMultiValued + '" data-tags="true"  placeholder="Enter String" class="form-control" ' + (isMultiValued === false && !_.isUndefinedNull(val) ? 'value="' + val + '"' : "") + '></' + elType + '>';
                } else if (typeName === "boolean" || typeName === "array<boolean>") {
                    returnEL = '<select data-key="' + key + '" data-businessMetadata="' + businessMetadata + '" data-typename="' + typeName + '" data-multi="' + isMultiValued + '" class="form-control">' + (isMultiValued ? "" : '<option value="">--Select Value--</option>') + '<option value="true" ' + (!_.isUndefinedNull(val) && val == "true" ? "selected" : "") + '>true</option><option value="false" ' + (!_.isUndefinedNull(val) && val == "false" ? "selected" : "") + '>false</option></select>';
                } else if (typeName === "date" || typeName === "array<date>") {
                    returnEL = '<' + (isMultiValued ? "textarea" : "input") + ' type="text" data-key="' + key + '" data-businessMetadata="' + businessMetadata + '" data-typename="' + typeName + '"data-multi="' + isMultiValued + '" data-type="date" class="form-control" ' + (isMultiValued === false && !_.isUndefinedNull(val) ? 'value="' + val + '"' : "") + '>' + (isMultiValued === true && !_.isUndefinedNull(val) ? val : "") + (isMultiValued ? "</textarea>" : "");
                    setTimeout(function() {
                        var dateObj = { singleDatePicker: true, showDropdowns: true, autoUpdateInput: isMultiValued ? false : true, locale: { format: Globals.dateFormat } },
                            dateEl = that.$el.find('[data-type="date"][data-key="' + key + '"]').daterangepicker(dateObj);
                        if (isMultiValued) {
                            dateEl.on("apply.daterangepicker", function(ev, picker) {
                                var val = picker.element.val();
                                if (val !== "") {
                                    val += ", ";
                                }
                                picker.element.val(val += Utils.formatDate({ date: picker.startDate, zone: false, dateFormat: Globals.dateFormat }));
                                that.$el.find(".custom-col-1[data-id='value']>[data-key]").trigger('change');
                            });
                        }
                    }, 0);
                } else if (typeName === "byte" || typeName === "array<byte>" || typeName === "short" || typeName === "array<short>" || typeName === "int" || typeName === "array<int>" || typeName === "float" || typeName === "array<float>" || typeName === "double" || typeName === "array<double>" || typeName === "long" || typeName === "array<long>") {
                    allowOnlyNum = true;
                    returnEL = '<' + elType + ' data-key="' + key + '" data-businessMetadata="' + businessMetadata + '" data-typename="' + typeName + '" type="number" data-multi="' + isMultiValued + '" data-tags="true" placeholder="Enter Number" class="form-control" ' + (!_.isUndefinedNull(val) ? 'value="' + val + '"' : "") + '></' + elType + '>';
                } else if (typeName) {
                    isEnum = true;
                    var modTypeName = typeName;
                    if (isMultiValued) {
                        var multipleType = typeName.match("array<(.*)>");
                        if (multipleType && multipleType[1]) {
                            modTypeName = multipleType[1];
                        }
                    }
                    var foundEnumType = this.enumDefCollection.fullCollection.find({ name: modTypeName });
                    if (foundEnumType) {
                        var enumOptions = "";
                        _.forEach(foundEnumType.get("elementDefs"), function(obj) {
                            enumOptions += '<option value="' + _.escape(obj.value) + '">' + _.escape(obj.value) + '</option>'
                        });
                        returnEL = '<select data-key="' + key + '" data-businessMetadata="' + businessMetadata + '" data-typename="' + typeName + '" data-multi="' + isMultiValued + '" data-enum="true">' + enumOptions + '</select>';
                    }
                }
                if (isEnum || elType === "select") {
                    setTimeout(function() {
                        var selectEl = that.$el.find('.custom-col-1[data-id="value"] select[data-key="' + key + '"]');
                        var data = [];
                        if (selectEl.data("multi")) {
                            data = val && val.length && (_.isArray(val) ? val : val.split(",")) || [];
                        } else {
                            data = _.unescape(val);
                        }
                        if (allowOnlyNum) {
                            selectEl.parent().addClass("select2_only_number");
                        }
                        var opt = {
                            tags: selectEl.data("tags") ? true : false,
                            multiple: selectEl.data("multi"),
                            createTag: function(params) {
                                var option = params.term;
                                if ($.isNumeric(option) || (typeName === "array<string>" && _.isString(option))) {
                                    return {
                                        id: option,
                                        text: option
                                    }
                                }
                            }
                        }
                        if (!selectEl.data("enum")) {
                            opt.data = data;
                        }
                        selectEl.select2(opt);
                        selectEl.val(data).trigger("change");
                    }, 0);
                }
            }
            return returnEL;
        },
        onAttrChange: function(e, manual) {
            var key = e.currentTarget.value.split(":");
            if (key.length && key.length === 3) {
                var valEl = $(e.currentTarget).parent().siblings(".custom-col-1"),
                    hasModalData = this.model.get(key[1]);
                if (!hasModalData) {
                    var tempObj = {
                        "__internal_UI_businessMetadataName": key[0]
                    };
                    if (this.model.has("isNew")) {
                        tempObj["isNew"] = true;
                    }
                    tempObj[key[1]] = null;
                    this.model.clear({ silent: true }).set(tempObj)
                }
                valEl.html(this.getAttrElement({ businessMetadata: key[0], key: key[1], val: hasModalData ? hasModalData : { typeName: key[2] } }));
                if (manual === undefined) {
                    this.model.collection.trigger("selected:attr", e.currentTarget.value, this.model);
                }
            }
        },
        getValue: function(value, key, businessMetadataName) {
            var typeName = value.typeName,
                value = value.value;
            if (typeName === "date") {
                return Utils.formatDate({ date: value, zone: false, dateFormat: Globals.dateFormat });

            } else {
                return value;
            }
        },
        getBusinessMetadataDroupdown: function(businessMetadataCollection) {
            var optgroup = "";
            var that = this;
            var model = that.model.omit(["isNew", "__internal_UI_businessMetadataName"]),
                keys = _.keys(model),
                isSelected = false,
                selectdVal = null;
            if (keys.length === 1) {
                isSelected = true;
            }
            _.each(businessMetadataCollection, function(obj, key) {
                var options = "";
                if (obj.length) {
                    _.each(obj, function(attrObj) {
                        var entityBusinessMetadata = that.model.collection.filter({ __internal_UI_businessMetadataName: key }),
                            hasAttr = false;
                        if (entityBusinessMetadata) {
                            var found = entityBusinessMetadata.find(function(eObj) {
                                return eObj.attributes.hasOwnProperty(attrObj.name);
                            });
                            if (found) {
                                hasAttr = true;
                            }
                        }
                        if ((isSelected && keys[0] === attrObj.name) || !(hasAttr)) {
                            var value = key + ":" + attrObj.name + ":" + attrObj.typeName;
                            if (isSelected && keys[0] === attrObj.name) { selectdVal = value };
                            options += '<option value="' + value + '">' + attrObj.name + ' (' + _.escape(attrObj.typeName) + ')</option>';
                        }
                    });
                    if (options.length) {
                        optgroup += '<optgroup label="' + key + '">' + options + '</optgroup>';
                    }
                }
            });

            setTimeout(function() {
                if (selectdVal) {
                    that.$el.find('.custom-col-1:first-child>select[data-id="key"]').val(selectdVal).trigger("change", true);
                } else {
                    that.$el.find('.custom-col-1:first-child>select[data-id="key"]').val("").trigger("change", true);
                }
            }, 0);
            return '<select data-id="key">' + optgroup + '</select>';
        },
        getEditBusinessMetadataEl: function() {
            var that = this,
                trs = "";
            _.each(this.model.attributes, function(val, key) {
                if (key !== "__internal_UI_businessMetadataName" && key !== "isNew") {
                    var td = '<td class="custom-col-1" data-key=' + key + '>' + key + ' (' + _.escape(val.typeName) + ')</td><td class="custom-col-0">:</td><td class="custom-col-1" data-id="value">' + that.getAttrElement({ businessMetadata: that.model.get("__internal_UI_businessMetadataName"), key: key, val: val }) + '</td>';

                    td += '<td class="custom-col-2 btn-group">' +
                        '<button class="btn btn-default btn-sm" data-key="' + key + '" data-id="deleteItem">' +
                        '<i class="fa fa-times"> </i>' +
                        '</button></td>';
                    trs += "<tr class='custom-tr'>" + td + "</tr>";
                }
            })
            this.$("[data-id='businessMetadataTreeChild']").html("<table class='custom-table'>" + trs + "</table>");
        },
        onDeleteItem: function(e) {
            var key = $(e.currentTarget).data("key");
            if (this.model.has(key)) {
                if (this.model.keys().length === 2) {
                    this.model.destroy();
                } else {
                    this.model.unset(key);
                    if (!this.model.has("isNew")) {
                        this.$el.find("tr>td:first-child[data-key='" + key + "']").parent().remove()
                    }
                    this.model.collection.trigger("unset:attr");
                }
            } else {
                this.model.destroy();
            }
        }
    });
});