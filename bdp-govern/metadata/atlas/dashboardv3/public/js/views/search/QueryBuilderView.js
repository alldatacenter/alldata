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
    'hbs!tmpl/search/QueryBuilder_tmpl',
    'hbs!tmpl/search/UserDefine_tmpl',
    'utils/Utils',
    'utils/CommonViewFunction',
    'utils/Enums',
    'utils/Globals',
    'moment',
    'query-builder',
    'daterangepicker'
], function(require, Backbone, QueryBuilderTmpl, UserDefineTmpl, Utils, CommonViewFunction, Enums, Globals, moment) {

    var QueryBuilderView = Backbone.Marionette.LayoutView.extend(
        /** @lends QueryBuilderView */
        {
            _viewName: 'QueryBuilderView',

            template: QueryBuilderTmpl,



            /** Layout sub regions */
            regions: {},


            /** ui selector cache */
            ui: {
                "builder": "#builder"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new QueryBuilderView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'attrObj', 'value', 'typeHeaders', 'entityDefCollection', 'enumDefCollection', 'classificationDefCollection', 'businessMetadataDefCollection', 'tag', 'type', 'searchTableFilters', 'systemAttrArr', 'adminAttrFilters'));
                this.attrObj = _.sortBy(this.attrObj, 'name');
                this.filterType = this.tag ? 'tagFilters' : 'entityFilters';
                this.defaultRange = "Last 7 Days";
                this.dateRangesMap = {
                    'Today': [moment(), moment()],
                    'Yesterday': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
                    'Last 7 Days': [moment().subtract(6, 'days'), moment()],
                    'Last 30 Days': [moment().subtract(29, 'days'), moment()],
                    'This Month': [moment().startOf('month'), moment().endOf('month')],
                    'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 3 Months': [moment().subtract(3, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 6 Months': [moment().subtract(6, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 12 Months': [moment().subtract(12, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'This Quarter': [moment().startOf('quarter'), moment().endOf('quarter')],
                    'Last Quarter': [moment().subtract(1, 'quarter').startOf('quarter'), moment().subtract(1, 'quarter').endOf('quarter')],
                    'This Year': [moment().startOf('year'), moment().endOf('year')],
                    'Last Year': [moment().subtract(1, 'year').startOf('year'), moment().subtract(1, 'year').endOf('year')]
                }
            },
            bindEvents: function() {},
            getOperator: function(type, skipDefault) {
                var obj = {
                    operators: null
                }
                if (type === "string") {
                    obj.operators = ['=', '!=', 'contains', 'begins_with', 'ends_with'];
                    if (this.adminAttrFilters) {
                        obj.operators = obj.operators.concat(['like', 'in']);
                    }
                }
                if (type === "date") {
                    obj.operators = ['=', '!=', '>', '<', '>=', '<=', 'TIME_RANGE'];
                }
                if (type === "int" || type === "byte" || type === "short" || type === "long" || type === "float" || type === "double") {
                    obj.operators = ['=', '!=', '>', '<', '>=', '<='];
                }
                if (type === "enum" || type === "boolean") {
                    obj.operators = ['=', '!='];
                }
                if (_.isEmpty(skipDefault) && obj.operators) {
                    obj.operators = obj.operators.concat(['is_null', 'not_null']);
                }
                return obj;
            },
            isPrimitive: function(type) {
                if (type === "int" || type === "byte" || type === "short" || type === "long" || type === "float" || type === "double" || type === "string" || type === "boolean" || type === "date") {
                    return true;
                }
                return false;
            },
            getUserDefineInput: function() {
                return UserDefineTmpl();
            },
            getObjDef: function(attrObj, rules, isGroup, groupType, isSystemAttr) {
                var that = this;
                if (attrObj.name === "__classificationsText" || attrObj.name === "__historicalGuids") {
                    return;
                }
                var getLableWithType = function(label, name) {
                    if (name === "__classificationNames" || name === "__customAttributes" || name === "__labels" || name === "__propagatedClassificationNames") {
                        return label;
                    } else {
                        return label + " (" + attrObj.typeName + ")";
                    }

                }
                var label = (Enums.systemAttributes[attrObj.name] ? Enums.systemAttributes[attrObj.name] : _.escape(attrObj.name));
                var obj = {
                    id: attrObj.name,
                    label: getLableWithType(label, attrObj.name),
                    plainLabel: label,
                    type: _.escape(attrObj.typeName),
                    validation: {
                        callback: function(value, rule) {
                            if (rule.operator.nb_inputs === false || !_.isEmpty(value) || !value instanceof Error) {
                                return true;
                            } else {
                                if (value instanceof Error) {
                                    return value.message; // with params
                                } else {
                                    return rule.filter.plainLabel + ' is required'; // with params
                                }
                            }
                        }
                    }
                };
                if (isGroup) {
                    obj.optgroup = groupType;
                }
                /* __isIncomplete / IsIncomplete */
                if (isSystemAttr && attrObj.name === "__isIncomplete" || isSystemAttr && attrObj.name === "IsIncomplete") {
                    obj.type = "boolean";
                    obj.label = (Enums.systemAttributes[attrObj.name] ? Enums.systemAttributes[attrObj.name] : _.escape(attrObj.name.capitalize())) + " (boolean)";
                    obj['input'] = 'select';
                    obj['values'] = [{ 1: 'true' }, { 0: 'false' }];
                    _.extend(obj, this.getOperator("boolean"));
                    return obj;
                }
                /* Status / __state */
                if (isSystemAttr && attrObj.name === "Status" || isSystemAttr && attrObj.name === "__state" || isSystemAttr && attrObj.name === "__entityStatus") {
                    obj.label = (Enums.systemAttributes[attrObj.name] ? Enums.systemAttributes[attrObj.name] : _.escape(attrObj.name.capitalize())) + " (enum)";
                    obj['input'] = 'select';
                    obj['values'] = ['ACTIVE', 'DELETED'];
                    _.extend(obj, this.getOperator("boolean", true));
                    return obj;
                }
                /* __classificationNames / __propagatedClassificationNames */
                if (isSystemAttr && attrObj.name === "__classificationNames" || attrObj.name === "__propagatedClassificationNames") {
                    obj["plugin"] = "select2";
                    obj["input"] = 'select';
                    obj["plugin_config"] = {
                        placeholder: "Select classfication",
                        tags: true,
                        multiple: false,
                        data: this.classificationDefCollection.fullCollection.models.map(function(o) { return { "id": o.get("name"), "text": o.get("name") } })
                    };
                    obj["valueSetter"] = function(rule) {
                        if (rule && !_.isEmpty(rule.value)) {
                            var selectEl = rule.$el.find('.rule-value-container select')
                            var valFound = that.classificationDefCollection.fullCollection.find(function(o) {
                                return o.get("name") === rule.value
                            })
                            if (valFound) {
                                selectEl.val(rule.value).trigger("change");
                            } else {
                                var newOption = new Option(rule.value, rule.value, false, false);
                                selectEl.append(newOption).val(rule.value);
                            }
                        }
                    };
                    _.extend(obj, this.getOperator("string"));
                    return obj;
                }
                /* __customAttributes */
                if (isSystemAttr && attrObj.name === "__customAttributes") {
                    obj["input"] = function(rule) {
                        return (rule && rule.operator && rule.operator.nb_inputs) ? that.getUserDefineInput() : null
                    }
                    obj["valueGetter"] = function(rule) {
                        if (rule && rule.operator && rule.operator.type === "contains") {
                            var $el = rule.$el.find('.rule-value-container'),
                                key = $el.find("[data-type='key']").val(),
                                val = $el.find("[data-type='value']").val();
                            if (!_.isEmpty(key) && !_.isEmpty(val)) {
                                return key + "=" + val;
                            } else {
                                return new Error("Key & Value is Required");
                            }
                        } else {
                            return [] // in CommonviewFunction.js value is set to "" for object;
                        }
                    }
                    obj["valueSetter"] = function(rule) {
                        if (rule && !rule.$el.hasClass("user-define")) {
                            rule.$el.addClass("user-define");
                        }
                        if (rule && rule.value && rule.value.length && !(rule.value instanceof Error)) {
                            var $el = rule.$el.find('.rule-value-container'),
                                value = rule.value.split("=");
                            if (value) {
                                $el.find("[data-type='key']").val(value[0]),
                                    $el.find("[data-type='value']").val(value[1]);
                            }
                        }
                    }
                    obj.operators = ['contains', 'is_null', 'not_null'];
                    return obj;
                }
                /* __typeName */
                if (isSystemAttr && attrObj.name === "__typeName") {
                    var entityType = [];
                    that.typeHeaders.fullCollection.each(function(model) {
                        if ((that.type == true && model.get('category') == 'ENTITY') || (that.tag == true && model.get('category') == "CLASSIFICATION")) {
                            entityType.push({
                                "id": model.get("name"),
                                "text": model.get("name")
                            })
                        }
                    });
                    obj["plugin"] = "select2";
                    obj["input"] = 'select';
                    obj["plugin_config"] = {
                        placeholder: "Select type",
                        tags: true,
                        multiple: false,
                        data: entityType
                    };
                    obj["valueSetter"] = function(rule) {
                        if (rule && !_.isEmpty(rule.value)) {
                            var selectEl = rule.$el.find('.rule-value-container select')
                            var valFound = that.typeHeaders.fullCollection.find(function(o) {
                                return o.get("name") === rule.value
                            })
                            if (valFound) {
                                selectEl.val(rule.value).trigger("change");
                            } else {
                                var newOption = new Option(rule.value, rule.value, false, false);
                                selectEl.append(newOption).val(rule.value);
                            }
                        }
                    };
                    _.extend(obj, this.getOperator("string"));
                    return obj;
                }
                if (obj.type === "date") {
                    obj['plugin'] = 'daterangepicker';
                    obj['plugin_config'] = this.getDateConfig(rules, obj.id);
                    _.extend(obj, this.getOperator(obj.type));
                    return obj;
                }

                if (this.isPrimitive(obj.type)) {
                    if (obj.type === "boolean") {
                        obj['input'] = 'select';
                        obj['values'] = ['true', 'false'];
                    }
                    _.extend(obj, this.getOperator(obj.type, false));
                    if (_.has(Enums.regex.RANGE_CHECK, obj.type)) {
                        obj.validation = {
                            min: Enums.regex.RANGE_CHECK[obj.type].min,
                            max: Enums.regex.RANGE_CHECK[obj.type].max
                        };
                        if (obj.type === "double" || obj.type === "float") {
                            obj.type = "double";
                        } else if (obj.type === "int" || obj.type === "byte" || obj.type === "short" || obj.type === "long") {
                            obj.type = "integer"
                        }
                    }
                    return obj;
                }
                var enumObj = this.enumDefCollection.fullCollection.find({ name: obj.type });
                if (enumObj) {
                    obj.type = "string";
                    obj['input'] = 'select';
                    var value = [];
                    _.each(enumObj.get('elementDefs'), function(o) {
                        value.push(o.value)
                    })
                    obj['values'] = value;
                    _.extend(obj, this.getOperator('enum'));
                    return obj;
                }
            },
            getDateConfig: function(ruleObj, id, operator) {
                var valueObj = ruleObj ? (_.find(ruleObj.rules, { id: id }) || {}) : {},
                    isTimeRange = (valueObj.operator && valueObj.operator === "TIME_RANGE" && operator === "TIME_RANGE") || (operator === "TIME_RANGE"),
                    obj = {
                        opens: "center",
                        autoApply: true,
                        autoUpdateInput: false,
                        timePickerSeconds: true,
                        timePicker: true,
                        locale: {
                            format: Globals.dateTimeFormat
                        }
                    };

                if (isTimeRange) {
                    var defaultRangeDate = this.dateRangesMap[this.defaultRange];
                    obj.startDate = defaultRangeDate[0];
                    obj.endDate = defaultRangeDate[1];
                    obj.singleDatePicker = false;
                    obj.ranges = this.dateRangesMap;
                } else {
                    obj.singleDatePicker = true;
                    obj.startDate = moment();
                    obj.endDate = obj.startDate;
                }

                if (!_.isEmpty(valueObj) && operator === valueObj.operator) {
                    if (isTimeRange) {
                        if (valueObj.value.indexOf("-") > -1) {
                            var dates = valueObj.value.split("-");
                            obj.startDate = dates[0].trim();
                            obj.endDate = dates[1].trim();
                        } else {
                            var dates = this.dateRangesMap[valueObj.value]
                            obj.startDate = dates[0];
                            obj.endDate = dates[1];
                        }
                        obj.singleDatePicker = false;
                    } else {
                        obj.startDate = moment(Date.parse(valueObj.value));
                        obj.endDate = obj.startDate;
                        obj.singleDatePicker = true;
                    }
                }

                return obj;
            },
            setDateValue: function(rule, rules_widgets) {
                if (rule.filter.type === "date" && rule.operator.nb_inputs) {
                    var inputEl = rule.$el.find(".rule-value-container").find("input"),
                        datepickerEl = rule.$el.find(".rule-value-container").find("input").data("daterangepicker")
                    inputEl.attr('readonly', true);
                    if (datepickerEl) {
                        datepickerEl.remove();
                        var configObj = this.getDateConfig(rules_widgets, rule.filter.id, rule.operator.type)
                        inputEl.daterangepicker(configObj);
                        if (rule.operator.type === "TIME_RANGE") {
                            rule.value = this.defaultRange;
                        } else {
                            rule.value = configObj.startDate.format(Globals.dateTimeFormat);
                        }
                        inputEl.on('apply.daterangepicker', function(ev, picker) {
                            picker.setStartDate(picker.startDate);
                            picker.setEndDate(picker.endDate);
                            var valueString = "";
                            if (picker.chosenLabel) {
                                if (picker.chosenLabel === "Custom Range") {
                                    valueString = picker.startDate.format(Globals.dateTimeFormat) + " - " + picker.endDate.format(Globals.dateTimeFormat);
                                } else {
                                    valueString = picker.chosenLabel;
                                }
                            } else {
                                valueString = picker.startDate.format(Globals.dateTimeFormat);
                            }
                            picker.element.val(valueString);
                            rule.value = valueString;
                        });
                    }
                }
            },
            onRender: function() {
                var that = this,
                    filters = [],
                    isGroupView = true,
                    placeHolder = '--Select Attribute--';
                var rules_widgets = null;
                if (this.adminAttrFilters) {
                    var entityDef = this.entityDefCollection.fullCollection.find({ name: "__AtlasAuditEntry" }),
                        auditEntryAttributeDefs = null;
                    if (entityDef) {
                        auditEntryAttributeDefs = $.extend(true, {}, entityDef.get("attributeDefs")) || null;
                    }
                    if (auditEntryAttributeDefs) {
                        _.each(auditEntryAttributeDefs, function(attributes) {
                            var returnObj = that.getObjDef(attributes, rules_widgets);
                            if (returnObj) {
                                filters.push(returnObj);
                            }
                        });
                    }
                    rules_widgets = CommonViewFunction.attributeFilter.extractUrl({ "value": this.searchTableFilters ? this.searchTableFilters["adminAttrFilters"] : null, "formatDate": true });;
                } else {
                    if (this.value) {
                        rules_widgets = CommonViewFunction.attributeFilter.extractUrl({ "value": this.searchTableFilters[this.filterType][(this.tag ? this.value.tag : this.value.type)], "formatDate": true });
                    }
                    _.each(this.attrObj, function(obj) {
                        var type = that.tag ? that.value.tag : that.value.type;
                        var returnObj = that.getObjDef(obj, rules_widgets, isGroupView, (type + ' Attribute'));
                        if (returnObj) {
                            filters.push(returnObj);
                        }
                    });
                    var sortMap = {
                        "__guid": 1,
                        "__typeName": 2,
                        "__timestamp": 3,
                        "__modificationTimestamp": 4,
                        "__createdBy": 5,
                        "__modifiedBy": 6,
                        "__isIncomplete": 7,
                        "__classificationNames": 9,
                        "__propagatedClassificationNames": 10,
                        "__labels": 11,
                        "__customAttributes": 12,
                    }
                    if (that.type) {
                        sortMap["__state"] = 8;
                    } else {
                        sortMap["__entityStatus"] = 8;
                    }
                    this.systemAttrArr = _.sortBy(this.systemAttrArr, function(obj) {
                        return sortMap[obj.name]
                    })
                    _.each(this.systemAttrArr, function(obj) {
                        var returnObj = that.getObjDef(obj, rules_widgets, isGroupView, 'System Attribute', true);
                        if (returnObj) {
                            filters.push(returnObj);
                        }
                    });
                    if (this.type) {
                        var pushBusinessMetadataFilter = function(sortedAttributes, businessMetadataKey) {
                            _.each(sortedAttributes, function(attrDetails) {
                                var returnObj = that.getObjDef(attrDetails, rules_widgets, isGroupView, "Business Attributes: " + businessMetadataKey);
                                if (returnObj) {
                                    returnObj.id = businessMetadataKey + "." + returnObj.id;
                                    returnObj.label = returnObj.label;
                                    returnObj.data = { 'entityType': "businessMetadata" };
                                    filters.push(returnObj);
                                }
                            });
                        };
                        if (this.value.type == "_ALL_ENTITY_TYPES") {
                            this.businessMetadataDefCollection.each(function(model) {
                                var sortedAttributes = model.get('attributeDefs');
                                sortedAttributes = _.sortBy(sortedAttributes, function(obj) {
                                    return obj.name;
                                });
                                pushBusinessMetadataFilter(sortedAttributes, model.get('name'));
                            })

                        } else {
                            var entityDef = this.entityDefCollection.fullCollection.find({ name: this.value.type }),
                                businessMetadataAttributeDefs = null;
                            if (entityDef) {
                                businessMetadataAttributeDefs = entityDef.get("businessAttributeDefs");
                            }
                            if (businessMetadataAttributeDefs) {
                                _.each(businessMetadataAttributeDefs, function(attributes, key) {
                                    var sortedAttributes = _.sortBy(attributes, function(obj) {
                                        return obj.name;
                                    });
                                    pushBusinessMetadataFilter(sortedAttributes, key);
                                });
                            }
                        }
                    }
                }
                filters = _.uniq(filters, 'id');
                if (filters && !_.isEmpty(filters)) {
                    this.ui.builder.off()
                        .on("afterUpdateRuleOperator.queryBuilder", function(e, rule) {
                            that.setDateValue(rule, rules_widgets);
                        })
                        .queryBuilder({
                            plugins: ['bt-tooltip-errors'],
                            filters: filters,
                            select_placeholder: placeHolder,
                            allow_empty: true,
                            conditions: ['AND', 'OR'],
                            allow_groups: true,
                            allow_empty: true,
                            templates: {
                                rule: '<div id="{{= it.rule_id }}" class="rule-container"> \
                                      <div class="values-box"><div class="rule-filter-container"></div> \
                                      <div class="rule-operator-container"></div> \
                                      <div class="rule-value-container"></div></div> \
                                      <div class="action-box"><div class="rule-header"> \
                                        <div class="btn-group rule-actions"> \
                                          <button type="button" class="btn btn-xs btn-danger" data-delete="rule"> \
                                            <i class="{{= it.icons.remove_rule }}"></i> \
                                          </button> \
                                        </div> \
                                      </div> </div>\
                                      {{? it.settings.display_errors }} \
                                        <div class="error-container"><i class="{{= it.icons.error }}"></i>&nbsp;<span></span></div> \
                                      {{?}} \
                                </div>'
                            },
                            operators: [
                                { type: '=', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean', 'enum'] },
                                { type: '!=', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean', 'enum'] },
                                { type: '>', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean'] },
                                { type: '<', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean'] },
                                { type: '>=', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean'] },
                                { type: '<=', nb_inputs: 1, multiple: false, apply_to: ['number', 'string', 'boolean'] },
                                { type: 'contains', nb_inputs: 1, multiple: false, apply_to: ['string'] },
                                { type: 'like', nb_inputs: 1, multiple: false, apply_to: ['string'] },
                                { type: 'in', nb_inputs: 1, multiple: false, apply_to: ['string'] },
                                { type: 'begins_with', nb_inputs: 1, multiple: false, apply_to: ['string'] },
                                { type: 'ends_with', nb_inputs: 1, multiple: false, apply_to: ['string'] },
                                { type: 'is_null', nb_inputs: false, multiple: false, apply_to: ['number', 'string', 'boolean', 'enum'] },
                                { type: 'not_null', nb_inputs: false, multiple: false, apply_to: ['number', 'string', 'boolean', 'enum'] },
                                { type: 'TIME_RANGE', nb_inputs: 1, multiple: false, apply_to: ['date'] }
                            ],
                            lang: {
                                add_rule: 'Add filter',
                                add_group: 'Add filter group',
                                operators: {
                                    not_null: 'is not null',
                                    TIME_RANGE: "Time Range"
                                }
                            },
                            icons: {
                                add_rule: 'fa fa-plus',
                                remove_rule: 'fa fa-times',
                                error: 'fa fa-exclamation-triangle'
                            },
                            rules: rules_widgets
                        })
                        .on("afterCreateRuleInput.queryBuilder", function(e, rule) {
                            rule.error = null;
                            if (rule.operator.nb_inputs && rule.filter.id === "__customAttributes") {
                                rule.$el.addClass("user-define");
                            } else if (rule.$el.hasClass("user-define")) {
                                rule.$el.removeClass("user-define");
                            }
                            if (rule.filter.type === "date") {
                                rule.$el.find('.rule-value-container >input').attr('readonly', true);
                            }
                            that.setDateValue(rule, rules_widgets);
                        })
                        .on('validationError.queryBuilder', function(e, rule, error, value) {
                            // never display error for my custom filter
                            var errorMsg = error[0];
                            if (that.queryBuilderLang && that.queryBuilderLang.errors && that.queryBuilderLang.errors[errorMsg]) {
                                errorMsg = that.queryBuilderLang.errors[errorMsg];
                            }
                            rule.$el.find(".error-container span").html(errorMsg);
                        });
                    var queryBuilderEl = that.ui.builder.data("queryBuilder");
                    if (queryBuilderEl && queryBuilderEl.lang) {
                        this.queryBuilderLang = queryBuilderEl.lang;
                    }
                    this.$('.rules-group-header .btn-group.pull-right.group-actions').toggleClass('pull-left');
                } else {
                    this.ui.builder.html('<h4>No Attributes are available !</h4>')
                }
            }
        });
    return QueryBuilderView;
});