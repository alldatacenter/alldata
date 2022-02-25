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
    'hbs!tmpl/search/SearchLayoutView_tmpl',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/Globals',
    'utils/Enums',
    'collection/VSearchList',
    'utils/CommonViewFunction',
    'jstree'
], function(require, Backbone, SearchLayoutViewTmpl, Utils, UrlLinks, Globals, Enums, VSearchList, CommonViewFunction) {
    'use strict';

    var SearchLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchLayoutView */
        {
            _viewName: 'SearchLayoutView',

            template: SearchLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                // RSaveSearchBasic: "[data-id='r_saveSearchBasic']",
                // RSaveSearchAdvance: "[data-id='r_saveSearchAdvance']"
            },

            /** ui selector cache */
            ui: {
                searchInput: '[data-id="searchInput"]',
                searchType: 'input[name="queryType"]',
                searchBtn: '[data-id="searchBtn"]',
                clearSearch: '[data-id="clearSearch"]',
                typeLov: '[data-id="typeLOV"]',
                tagLov: '[data-id="tagLOV"]',
                termLov: '[data-id="termLOV"]',
                refreshBtn: '[data-id="refreshBtn"]',
                advancedInfoBtn: '[data-id="advancedInfo"]',
                typeAttrFilter: '[data-id="typeAttrFilter"]',
                tagAttrFilter: '[data-id="tagAttrFilter"]'

            },

            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["keyup " + this.ui.searchInput] = function(e) {
                    var code = e.which;
                    this.value.query = e.currentTarget.value;
                    this.query[this.type].query = this.value.query;
                    if (code == 13) {
                        that.findSearchResult();
                    }
                    this.checkForButtonVisiblity();
                };
                events["change " + this.ui.searchType] = 'dslFulltextToggle';
                events["click " + this.ui.searchBtn] = 'findSearchResult';
                events["click " + this.ui.clearSearch] = 'clearSearchData';
                events["change " + this.ui.typeLov] = 'checkForButtonVisiblity';
                events["change " + this.ui.tagLov] = 'checkForButtonVisiblity';
                events["change " + this.ui.termLov] = 'checkForButtonVisiblity';
                events["click " + this.ui.refreshBtn] = 'onRefreshButton';
                events["click " + this.ui.advancedInfoBtn] = 'advancedInfo';
                events["click " + this.ui.typeAttrFilter] = function() {
                    this.openAttrFilter('type');
                };
                events["click " + this.ui.tagAttrFilter] = function() {
                    this.openAttrFilter('tag');
                };
                return events;
            },
            /**
             * intialize a new SearchLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'typeHeaders', 'searchVent', 'entityDefCollection', 'enumDefCollection', 'classificationDefCollection', 'searchTableColumns', 'searchTableFilters', 'metricCollection', 'onSubmit'));
                this.type = "dsl";
                this.entityCountObj = _.first(this.metricCollection.toJSON()) || { entity: { entityActive: {}, entityDeleted: {} }, tag: { tagEntities: {} } };
                this.filterTypeSelected = [];
                var param = Utils.getUrlState.getQueryParams();
                this.query = {
                    dsl: {
                        query: null,
                        type: null,
                        pageOffset: null,
                        pageLimit: null
                    },
                    basic: {
                        query: null,
                        type: null,
                        tag: null,
                        term: null,
                        attributes: null,
                        tagFilters: null,
                        pageOffset: null,
                        pageLimit: null,
                        entityFilters: null,
                        includeDE: null,
                        excludeST: null,
                        excludeSC: null
                    }
                };
                if (!this.value) {
                    this.value = {};
                }
                this.dsl = false;
                if (param && param.searchType) {
                    this.type = param.searchType;
                    this.updateQueryObject(param);
                }
                this.bindEvents();
            },
            bindEvents: function(param) {
                this.listenTo(this.typeHeaders, "reset", function(value) {
                    this.initializeValues();
                }, this);
            },
            onRender: function() {
                // array of tags which is coming from url
                if (this.type === "basic") {
                    this.$(".searchByText").hide();
                }
                this.initializeValues();
                this.dslFulltextToggle(true, false);
            },
            initializeValues: function() {
                this.renderTypeTagList();
                this.renderTermList();
                this.setValues();
                this.checkForButtonVisiblity();
                //this.renderSaveSearch();
            },
            makeFilterButtonActive: function(filtertypeParam) {
                var filtertype = ['entityFilters', 'tagFilters'],
                    that = this;
                if (filtertypeParam) {
                    if (_.isArray(filtertypeParam)) {
                        filtertype = filtertypeParam;
                    } else if (_.isString(filtertypeParam)) {
                        filtertype = [filtertypeParam];
                    }
                }
                var typeCheck = function(filterObj, type) {
                    if (that.value.type) {
                        if (filterObj && filterObj.length) {
                            that.ui.typeAttrFilter.addClass('active');
                        } else {
                            that.ui.typeAttrFilter.removeClass('active');
                        }
                        that.ui.typeAttrFilter.prop('disabled', false);
                    } else {
                        that.ui.typeAttrFilter.removeClass('active');
                        that.ui.typeAttrFilter.prop('disabled', true);
                    }
                }
                var tagCheck = function(filterObj, type) {
                    if (that.value.tag && !_.contains(Enums.addOnClassification, that.value.tag)) {
                        that.ui.tagAttrFilter.prop('disabled', false);
                        if (filterObj && filterObj.length) {
                            that.ui.tagAttrFilter.addClass('active');
                        } else {
                            that.ui.tagAttrFilter.removeClass('active');
                        }
                    } else {
                        that.ui.tagAttrFilter.removeClass('active');
                        that.ui.tagAttrFilter.prop('disabled', true);
                    }
                }
                _.each(filtertype, function(type) {
                    var filterObj = that.searchTableFilters[type];
                    if (type == "entityFilters") {
                        typeCheck(filterObj[that.value.type], type);
                    }
                    if (type == "tagFilters") {
                        tagCheck(filterObj[that.value.tag], type);
                    }
                });
            },
            checkForButtonVisiblity: function(e, options) {
                var that = this,
                    isBasicSearch = (this.type == "basic");
                if (e && e.currentTarget) {
                    var $el = $(e.currentTarget),
                        isTagEl = $el.data('id') == "tagLOV",
                        isTermEl = $el.data('id') == "termLOV",
                        isTypeEl = $el.data('id') == "typeLOV",
                        select2Data = $el.select2('data');
                    if (e.type == "change" && select2Data) {
                        var value = (_.isEmpty(select2Data) ? select2Data : _.first(select2Data).id),
                            key = "tag",
                            filterType = isBasicSearch ? 'tagFilters' : null,
                            value = value && value.length ? value : null;
                        if (!isTagEl) {
                            key = (isTermEl ? "term" : "type");
                            if (isBasicSearch) {
                                filterType = (isTypeEl ? "entityFilters" : null);
                            }
                        }
                        if (this.value) {
                            //On Change handle
                            if (this.value[key] !== value || (!value && !this.value[key])) {
                                var temp = {};
                                temp[key] = value;
                                _.extend(this.value, temp);
                                // on change of type/tag change the offset.
                                if (_.isUndefined(options)) {
                                    this.value.pageOffset = 0;
                                }
                                _.extend(this.query[this.type], temp);
                            } else if (isBasicSearch) {
                                // Initial loading handle.
                                // if (filterType) {
                                //     var filterObj = this.searchTableFilters[filterType];
                                //     if (filterObj && this.value[key]) {
                                //         this.searchTableFilters[filterType][this.value[key]] = this.value[filterType] ? this.value[filterType] : null;
                                //     }
                                // }

                                if (this.value.type) {
                                    if (this.value.attributes) {
                                        var attributes = _.sortBy(this.value.attributes.split(',')),
                                            tableColumn = this.searchTableColumns[this.value.type];
                                        if (_.isEmpty(this.searchTableColumns) || !tableColumn) {
                                            this.searchTableColumns[this.value.type] = attributes
                                        } else if (tableColumn.join(",") !== attributes.join(",")) {
                                            this.searchTableColumns[this.value.type] = attributes;
                                        }
                                    } else if (this.searchTableColumns[this.value.type]) {
                                        this.searchTableColumns[this.value.type] = undefined;
                                    }
                                }
                            }
                            if (isBasicSearch && filterType) {
                                this.makeFilterButtonActive(filterType);
                            }
                        } else if (isBasicSearch) {
                            this.ui.tagAttrFilter.prop('disabled', true);
                            this.ui.typeAttrFilter.prop('disabled', true);
                        }
                    }
                }

                var value = this.ui.searchInput.val() || _.first($(this.ui.typeLov).select2('data')).id;
                if (!this.dsl && _.isEmpty(value)) {
                    var termData = _.first($(this.ui.termLov).select2('data'));
                    value = _.first($(this.ui.tagLov).select2('data')).id || (termData ? termData.id : "");
                }
                if (value && value.length) {
                    this.ui.searchBtn.removeAttr("disabled");
                    setTimeout(function() {
                        !that.isDestroyed && that.ui.searchInput.focus();
                    }, 0);
                } else {
                    this.ui.searchBtn.attr("disabled", "true");
                }
            },
            updateQueryObject: function(param) {
                if (param && param.searchType) {
                    this.type = param.searchType;
                }
                _.extend(this.query[this.type],
                    (this.type == "dsl" ? {
                        query: null,
                        type: null,
                        pageOffset: null,
                        pageLimit: null
                    } : {
                        query: null,
                        type: null,
                        tag: null,
                        term: null,
                        attributes: null,
                        tagFilters: null,
                        pageOffset: null,
                        pageLimit: null,
                        entityFilters: null,
                        includeDE: null
                    }), param);
            },
            fetchCollection: function(value) {
                this.typeHeaders.fetch({ reset: true });
            },
            onRefreshButton: function() {
                this.fetchCollection();
                //to check url query param contain type or not
                var checkURLValue = Utils.getUrlState.getQueryParams(this.url);
                if (this.searchVent && (_.has(checkURLValue, "tag") || _.has(checkURLValue, "type") || _.has(checkURLValue, "query"))) {
                    this.searchVent.trigger('search:refresh');
                }
            },
            advancedInfo: function(e) {
                require([
                    'views/search/AdvancedSearchInfoView',
                    'modules/Modal'
                ], function(AdvancedSearchInfoView, Modal) {
                    var view = new AdvancedSearchInfoView();
                    var modal = new Modal({
                        title: 'Advanced Search Queries',
                        content: view,
                        okCloses: true,
                        showFooter: true,
                        allowCancel: false
                    }).open();
                    view.on('closeModal', function() {
                        modal.trigger('cancel');
                    });
                });
            },
            openAttrFilter: function(filterType) {
                var that = this;
                require(['views/search/SearchQueryView'], function(SearchQueryView) {
                    that.attrModal = new SearchQueryView({
                        value: that.value,
                        tag: (filterType === "tag" ? true : false),
                        type: (filterType === "type" ? true : false),
                        searchVent: that.searchVent,
                        typeHeaders: that.typeHeaders,
                        entityDefCollection: that.entityDefCollection,
                        enumDefCollection: that.enumDefCollection,
                        classificationDefCollection: that.classificationDefCollection,
                        searchTableFilters: that.searchTableFilters
                    });
                    that.attrModal.on('ok', function(scope, e) {
                        that.okAttrFilterButton(e);
                    });
                });
            },
            okAttrFilterButton: function(e) {
                var isTag = this.attrModal.tag ? true : false,
                    filtertype = isTag ? 'tagFilters' : 'entityFilters',
                    queryBuilderRef = this.attrModal.RQueryBuilder.currentView.ui.builder,
                    col = [];

                function getIdFromRuleObject(rule) {
                    _.map(rule.rules, function(obj, key) {
                        if (_.has(obj, 'condition')) {
                            return getIdFromRuleObject(obj);
                        } else {
                            return col.push(obj.id)
                        }
                    });
                    return col;
                }
                if (queryBuilderRef.data('queryBuilder')) {
                    var rule = queryBuilderRef.queryBuilder('getRules');
                }
                if (rule) {
                    var ruleUrl = CommonViewFunction.attributeFilter.generateUrl({ "value": rule, "formatedDateToLong": true });
                    this.searchTableFilters[filtertype][(isTag ? this.value.tag : this.value.type)] = ruleUrl;
                    this.makeFilterButtonActive(filtertype);
                    if (!isTag && this.value && this.value.type && this.searchTableColumns) {
                        if (!this.searchTableColumns[this.value.type]) {
                            this.searchTableColumns[this.value.type] = ["selected", "name", "owner", "description", "tag", "typeName"]
                        }
                        this.searchTableColumns[this.value.type] = _.sortBy(_.union(this.searchTableColumns[this.value.type], getIdFromRuleObject(rule)));
                    }
                    this.attrModal.modal.close();
                    if ($(e.currentTarget).hasClass('search')) {
                        this.findSearchResult();
                    }
                }
            },
            manualRender: function(paramObj) {
                this.updateQueryObject(paramObj);
                this.setValues(paramObj);
            },
            getFilterBox: function() {
                var serviceStr = '',
                    serviceArr = [],
                    that = this;
                this.typeHeaders.fullCollection.each(function(model) {
                    var serviceType = model.toJSON().serviceType;
                    if (serviceType) {
                        serviceArr.push(serviceType);
                    }
                });
                _.each(_.uniq(serviceArr), function(service) {
                    serviceStr += '<li><div class="pretty p-switch p-fill"><input type="checkbox" class="pull-left" data-value="' + (service) + '" value="" ' + (_.contains(that.filterTypeSelected, service) ? "checked" : "") + '/><div class="state p-primary"><label>' + (service.toUpperCase()) + '</label></div></div></li>';
                });
                var templt = serviceStr + '<hr class="hr-filter"/><div class="text-right"><div class="divider"></div><button class="btn btn-action btn-sm filterDone">Done</button></div>';
                return templt;
            },
            setValues: function(paramObj) {
                var arr = [],
                    that = this;
                if (paramObj) {
                    this.value = paramObj;
                }
                if (this.value) {
                    this.ui.searchInput.val(this.value.query || "");
                    if (this.value.dslChecked == "true") {
                        if (!this.ui.searchType.prop("checked")) {
                            this.ui.searchType.prop("checked", true).trigger("change");
                        }
                    } else {
                        if (this.ui.searchType.prop("checked")) {
                            this.ui.searchType.prop("checked", false).trigger("change");
                        }
                    }
                    this.ui.typeLov.val(this.value.type);
                    if (this.ui.typeLov.data('select2')) {
                        if (this.ui.typeLov.val() !== this.value.type) {
                            this.value.type = null;
                            this.ui.typeLov.val("").trigger("change", { 'manual': true });
                        } else {
                            this.ui.typeLov.trigger("change", { 'manual': true });
                        }
                    }

                    if (!this.dsl) {
                        this.ui.tagLov.val(this.value.tag);
                        if (this.ui.tagLov.data('select2')) {
                            // To handle delete scenario.
                            if (this.ui.tagLov.val() !== this.value.tag) {
                                this.value.tag = null;
                                this.ui.tagLov.val("").trigger("change", { 'manual': true });
                            } else {
                                this.ui.tagLov.trigger("change", { 'manual': true });
                            }
                        }
                        if (this.value.term) {
                            this.ui.termLov.append('<option value="' + this.value.term + '" selected="selected">' + this.value.term + '</option>');
                        }
                        if (this.ui.termLov.data('select2')) {
                            if (this.ui.termLov.val() !== this.value.term) {
                                this.value.term = null;
                                this.ui.termLov.val("").trigger("change", { 'manual': true });
                            } else {
                                this.ui.termLov.trigger("change", { 'manual': true });
                            }
                        }
                    }
                    setTimeout(function() {
                        !that.isDestroyed && that.ui.searchInput.focus();
                    }, 0);
                }
            },
            renderTypeTagList: function(options) {
                var that = this;
                var serviceTypeToBefiltered = (options && options.filterList);
                var isTypeOnly = options && options.isTypeOnly;
                this.ui.typeLov.empty();
                var typeStr = '<option></option>',
                    tagStr = typeStr;
                this.typeHeaders.fullCollection.each(function(model) {
                    var name = Utils.getName(model.toJSON(), 'name');
                    if (model.get('category') == 'ENTITY' && (serviceTypeToBefiltered && serviceTypeToBefiltered.length ? _.contains(serviceTypeToBefiltered, model.get('serviceType')) : true)) {
                        var entityCount = (that.entityCountObj.entity.entityActive[name] + (that.entityCountObj.entity.entityDeleted[name] ? that.entityCountObj.entity.entityDeleted[name] : 0));
                        typeStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + ' ' + (entityCount ? "(" + _.numberFormatWithComma(entityCount) + ")" : '') + '</option>';
                    }
                    if (isTypeOnly == undefined && model.get('category') == 'CLASSIFICATION') {
                        var tagEntityCount = that.entityCountObj.tag.tagEntities[name];
                        tagStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + ' ' + (tagEntityCount ? "(" + _.numberFormatWithComma(tagEntityCount) + ")" : '') + '</option>';
                    }
                });
                if (_.isUndefined(isTypeOnly)) {
                    //to insert extra classification list
                    _.each(Enums.addOnClassification, function(classificationName) {
                        tagStr += '<option  value="' + (classificationName) + '" data-name="' + (classificationName) + '">' + classificationName + '</option>';
                    });
                    that.ui.tagLov.html(tagStr);
                    this.ui.tagLov.select2({
                        placeholder: "Select Classification",
                        allowClear: true
                    });
                }
                that.ui.typeLov.html(typeStr);
                var typeLovSelect2 = this.ui.typeLov.select2({
                    placeholder: "Select Type",
                    dropdownAdapter: $.fn.select2.amd.require("ServiceTypeFilterDropdownAdapter"),
                    allowClear: true,
                    dropdownCssClass: "searchLayoutView",
                    getFilterBox: this.getFilterBox.bind(this),
                    onFilterSubmit: function(options) {
                        that.filterTypeSelected = options.filterVal;
                        that.renderTypeTagList({ "filterList": options.filterVal, isTypeOnly: true })
                    }
                });
                typeLovSelect2.on("select2:close", function() {
                    typeLovSelect2.trigger("hideFilter");
                });
                if (typeLovSelect2 && serviceTypeToBefiltered) {
                    typeLovSelect2.select2('open').trigger("change", { 'manual': true });
                }
            },
            renderTermList: function() {
                var getTypeAheadData = function(data, params) {
                    var dataList = data.entities,
                        foundOptions = [];
                    _.each(dataList, function(obj) {
                        if (obj) {
                            if (obj.guid) {
                                obj['id'] = Utils.getName(obj, 'qualifiedName');
                            }
                            foundOptions.push(obj);
                        }
                    });
                    return foundOptions;
                }
                this.ui.termLov.select2({
                    placeholder: "Search Term",
                    allowClear: true,
                    ajax: {
                        url: UrlLinks.searchApiUrl('attribute'),
                        dataType: 'json',
                        delay: 250,
                        data: function(params) {
                            return {
                                attrValuePrefix: params.term, // search term
                                typeName: "AtlasGlossaryTerm",
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
                    minimumInputLength: 1
                });
            },
            renderSaveSearch: function() {
                var that = this;
                require(['views/search/save/SaveSearchView'], function(SaveSearchView) {
                    var saveSearchBaiscCollection = new VSearchList(),
                        saveSearchAdvanceCollection = new VSearchList(),
                        saveSearchCollection = new VSearchList();
                    saveSearchCollection.url = UrlLinks.saveSearchApiUrl();
                    saveSearchBaiscCollection.fullCollection.comparator = function(model) {
                        return getModelName(model);
                    }
                    saveSearchAdvanceCollection.fullCollection.comparator = function(model) {
                        return getModelName(model);
                    }
                    var obj = {
                        value: that.value,
                        searchVent: that.searchVent,
                        typeHeaders: that.typeHeaders,
                        fetchCollection: fetchSaveSearchCollection,
                        classificationDefCollection: that.classificationDefCollection,
                        entityDefCollection: that.entityDefCollection,
                        getValue: function() {
                            var queryObj = that.query[that.type],
                                entityObj = that.searchTableFilters['entityFilters'],
                                tagObj = that.searchTableFilters['tagFilters'],
                                urlObj = Utils.getUrlState.getQueryParams();
                            if (urlObj) {
                                // includeDE value in because we need to send "true","false" to the server.
                                urlObj.includeDE = urlObj.includeDE == "true" ? true : false;
                                urlObj.excludeSC = urlObj.excludeSC == "true" ? true : false;
                                urlObj.excludeST = urlObj.excludeST == "true" ? true : false;
                            }
                            return _.extend({}, queryObj, urlObj, {
                                'entityFilters': entityObj ? entityObj[queryObj.type] : null,
                                'tagFilters': tagObj ? tagObj[queryObj.tag] : null,
                                'type': queryObj.type,
                                'query': queryObj.query,
                                'term': queryObj.term,
                                'tag': queryObj.tag
                            })
                        },
                        applyValue: function(model, searchType) {
                            that.manualRender(_.extend(searchType, CommonViewFunction.generateUrlFromSaveSearchObject({
                                value: { "searchParameters": model.get('searchParameters'), 'uiParameters': model.get('uiParameters') },
                                classificationDefCollection: that.classificationDefCollection,
                                entityDefCollection: that.entityDefCollection
                            })));
                        }
                    }

                    that.RSaveSearchBasic.show(new SaveSearchView(_.extend(obj, {
                        isBasic: true,
                        collection: saveSearchBaiscCollection.fullCollection
                    })));
                    that.RSaveSearchAdvance.show(new SaveSearchView(_.extend(obj, {
                        isBasic: false,
                        collection: saveSearchAdvanceCollection.fullCollection
                    })));

                    function fetchSaveSearchCollection() {
                        saveSearchCollection.fetch({
                            success: function(collection, data) {
                                saveSearchAdvanceCollection.fullCollection.reset(_.where(data, { "searchType": "ADVANCED" }));
                                saveSearchBaiscCollection.fullCollection.reset(_.where(data, { "searchType": "BASIC" }));
                            },
                            silent: true
                        });
                    }

                    function getModelName(model) {
                        if (model.get('name')) {
                            return model.get('name').toLowerCase();
                        }
                    };
                    fetchSaveSearchCollection();
                });
            },
            findSearchResult: function() {
                this.triggerSearch(this.ui.searchInput.val());
            },
            triggerSearch: function(value) {
                var params = {
                        searchType: this.type,
                        dslChecked: this.ui.searchType.is(':checked'),
                        tagFilters: null,
                        entityFilters: null
                    },
                    typeLovValue = this.ui.typeLov.find(':selected').data('name'),
                    tagLovValue = this.ui.tagLov.find(':selected').data('name'),
                    termLovValue = this.ui.termLov.select2('val')
                params['type'] = typeLovValue || null;
                if (!this.dsl) {
                    params['tag'] = tagLovValue || null;
                    params['term'] = termLovValue || null;
                    var entityFilterObj = this.searchTableFilters['entityFilters'],
                        tagFilterObj = this.searchTableFilters['tagFilters'];
                    if (this.value.tag) {
                        params['tagFilters'] = tagFilterObj[this.value.tag]
                    }
                    if (this.value.type) {
                        params['entityFilters'] = entityFilterObj[this.value.type]
                    }
                    var columnList = this.value && this.value.type && this.searchTableColumns ? this.searchTableColumns[this.value.type] : null;
                    if (columnList) {
                        params['attributes'] = columnList.join(',');
                    }
                    params['includeDE'] = _.isUndefinedNull(this.value.includeDE) ? false : this.value.includeDE;
                    params['excludeST'] = _.isUndefinedNull(this.value.excludeST) ? false : this.value.excludeST;
                    params['excludeSC'] = _.isUndefinedNull(this.value.excludeSC) ? false : this.value.excludeSC;
                }
                if (!_.isUndefinedNull(this.value.pageLimit)) {
                    params['pageLimit'] = this.value.pageLimit;
                }
                if (!_.isUndefinedNull(this.value.pageOffset)) {
                    if (!_.isUndefinedNull(this.query[this.type]) && this.query[this.type].query != value) {
                        params['pageOffset'] = 0;
                    } else {
                        params['pageOffset'] = this.value.pageOffset;
                    }
                }
                params['query'] = value || null;
                _.extend(this.query[this.type], params);
                if (this.onSubmit) {
                    this.onSubmit(this.query[this.type]);
                }
                Utils.setUrl({
                    url: '#!/search/searchResult',
                    urlParams: _.extend({}, this.query[this.type]),
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            dslFulltextToggle: function(e, triggerUrl) {
                var paramObj = Utils.getUrlState.getQueryParams();
                if (paramObj && this.type == paramObj.searchType) {
                    this.updateQueryObject(paramObj);
                }
                if (e) {
                    this.type = "dsl";
                    this.dsl = true;
                    this.$('.typeFilterBtn,.tagBox,.termBox,.basicSaveSearch').hide();
                    //this.$('.typeFilter').addClass('col-sm-12');
                    //this.$('.typeFilter').removeClass('col-sm-10');
                    this.$('.advanceSaveSearch,.searchByText').show();
                    this.$('.searchText').text('Search By Query');
                    this.ui.searchInput.attr("placeholder", 'Search By Query eg. where name="sales_fact"');
                } else {
                    //this.$('.typeFilter').addClass('col-sm-10');
                    //this.$('.typeFilter').removeClass('col-sm-12');
                    this.$('.typeFilterBtn,.tagBox,.termBox,.basicSaveSearch').show();
                    this.$('.advanceSaveSearch,.searchByText').hide();
                    this.dsl = false;
                    this.type = "basic";
                    this.ui.searchInput.attr("placeholder", "Search By Text");
                }
                if (triggerUrl !== false && Utils.getUrlState.isSearchTab()) {
                    Utils.setUrl({
                        url: '#!/search/searchResult',
                        urlParams: _.extend(this.query[this.type], {
                            searchType: this.type,
                            dslChecked: this.ui.searchType.is(':checked')
                        }),
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
            },
            clearSearchData: function() {
                this.filterTypeSelected = [];
                this.renderTypeTagList();
                this.updateQueryObject();
                this.ui.typeLov.val("").trigger("change");
                this.ui.tagLov.val("").trigger("change");
                this.ui.searchInput.val("");
                var type = "basicSaveSearch";
                if (this.type == "dsl") {
                    type = "advanceSaveSearch";
                }
                this.$('.' + type + ' .saveSearchList').find('li.active').removeClass('active');
                this.$('.' + type + ' [data-id="saveBtn"]').attr('disabled', true);
                if (!this.dsl) {
                    this.searchTableFilters.tagFilters = {};
                    this.searchTableFilters.entityFilters = {};
                }
                this.checkForButtonVisiblity();
                // Utils.setUrl({
                //     url: '#!/search/searchResult',
                //     urlParams: {
                //         searchType: this.type,
                //         dslChecked: this.ui.searchType.is(':checked')
                //     },
                //     mergeBrowserUrl: false,
                //     trigger: true
                // });
            }
        });
    return SearchLayoutView;
});