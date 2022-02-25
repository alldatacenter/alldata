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
    'modules/Modal'
], function(require, Backbone, SearchLayoutViewTmpl, Utils, UrlLinks, Globals, Enums, VSearchList, CommonViewFunction, Modal) {
    'use strict';

    var SearchLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchLayoutView */
        {
            _viewName: 'SearchLayoutView',

            template: SearchLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RSaveSearchBasic: "[data-id='r_saveSearchBasic']",
                RSaveSearchAdvance: "[data-id='r_saveSearchAdvance']"
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
                _.extend(this, _.pick(options, 'value', 'typeHeaders', 'searchVent', 'entityDefCollection', 'enumDefCollection', 'classificationDefCollection', 'businessMetadataDefCollection', 'searchTableColumns', 'searchTableFilters', 'metricCollection', 'classificationAndMetricEvent'));
                this.type = "basic";
                this.entityCountObj = _.first(this.metricCollection.toJSON()) || { entity: { entityActive: {}, entityDeleted: {} }, tag: { tagEntities: {} } };
                this.selectedFilter = {
                    'basic': [],
                    'dsl': []
                };
                this.filterTypeSelected = null;
                var param = Utils.getUrlState.getQueryParams();
                this.query = {
                    dsl: {
                        query: this.value ? this.value.query : null,
                        type: this.value ? this.value.type : null,
                        pageOffset: this.value ? this.value.pageOffset : null,
                        pageLimit: this.value ? this.value.pageLimit : null
                    },
                    basic: {
                        query: this.value ? this.value.query : null,
                        type: this.value ? this.value.type : null,
                        tag: this.value ? this.value.tag : null,
                        term: this.value ? this.value.term : null,
                        attributes: this.value ? this.value.attributes : null,
                        tagFilters: this.value ? this.value.tagFilters : null,
                        pageOffset: this.value ? this.value.pageOffset : null,
                        pageLimit: this.value ? this.value.pageLimit : null,
                        entityFilters: this.value ? this.value.entityFilters : null,
                        includeDE: this.value ? this.value.includeDE : null,
                        excludeST: this.value ? this.value.excludeST : null,
                        excludeSC: this.value ? this.value.excludeSC : null
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
                if ((this.value && this.value.type) || (this.value && this.value.tag && this.value.searchType === "basic")) {
                    this.setInitialEntityVal = false;
                } else {
                    this.setInitialEntityVal = true;
                }
                this.tagEntityCheck = false;
                this.typeEntityCheck = false;
                this.bindEvents();
            },
            renderSaveSearch: function() {
                var that = this;
                require(['views/search/save/SaveSearchView'], function(SaveSearchView) {
                    var saveSearchBaiscCollection = new VSearchList(),
                        saveSearchAdvanceCollection = new VSearchList(),
                        saveSearchCollection = new VSearchList();
                    saveSearchCollection.url = UrlLinks.saveSearchApiUrl();
                    saveSearchBaiscCollection.fullCollection.comparator = function(model) {
                        return model.get('name').toLowerCase();
                    }
                    saveSearchAdvanceCollection.fullCollection.comparator = function(model) {
                        return model.get('name').toLowerCase();
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
                    fetchSaveSearchCollection();
                });
            },
            bindEvents: function(param) {
                var that = this;
                this.listenTo(this.typeHeaders, "reset", function(value) {
                    this.initializeValues();
                }, this);
                this.listenTo(this.searchVent, "entityList:refresh", function(model, response) {
                    this.onRefreshButton();
                }, this);
                this.classificationAndMetricEvent.on("classification:Update:Search", function(options) {
                    that.entityCountObj = _.first(that.metricCollection.toJSON());
                    that.value = Utils.getUrlState.getQueryParams() || {};
                    if (!that.value.type) that.setInitialEntityVal = true;
                    that.initializeValues();
                });
            },
            initializeValues: function() {
                this.renderTypeTagList();
                this.renderTermList();
                this.setValues();
                this.checkForButtonVisiblity();
                this.renderSaveSearch();
                if (this.setInitialEntityVal) {
                    this.setInitialEntityVal = false;
                }
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
                    var filterAddOn = Enums.addOnClassification.filter(function(a) { a !== Enums.addOnClassification[0] });
                    if (that.value.tag && !_.contains(filterAddOn, that.value.tag)) {
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
                                if (filterType) {
                                    var filterObj = this.searchTableFilters[filterType];
                                    if (filterObj && this.value[key]) {
                                        this.searchTableFilters[filterType][this.value[key]] = this.value[filterType] ? this.value[filterType] : null;
                                    }
                                }

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
                        that.ui.searchInput.focus();
                    }, 0);
                } else {
                    this.ui.searchBtn.attr("disabled", "true");
                }
            },
            onRender: function() {
                // array of tags which is coming from url
                this.initializeValues();
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
            onRefreshButton: function() {
                var that = this,
                    apiCount = 2,
                    updateSearchList = function() {
                        if (apiCount === 0) {
                            that.initializeValues();
                            var checkURLValue = Utils.getUrlState.getQueryParams(that.url);
                            if (that.searchVent && (_.has(checkURLValue, "tag") || _.has(checkURLValue, "type") || _.has(checkURLValue, "query"))) {
                                that.searchVent.trigger('search:refresh');
                            }
                        }
                    };
                this.metricCollection.fetch({
                    complete: function() {
                        --apiCount;
                        that.entityCountObj = _.first(that.metricCollection.toJSON());
                        updateSearchList();
                    }
                });

                this.typeHeaders.fetch({
                    silent: true,
                    complete: function() {
                        --apiCount;
                        updateSearchList();
                    }
                });
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
                        businessMetadataDefCollection: that.businessMetadataDefCollection,
                        searchTableFilters: that.searchTableFilters,

                    });
                    that.attrModal.on('ok', function(scope, e) {
                        that.okAttrFilterButton(e);
                    });
                });
            },
            okAttrFilterButton: function(e) {
                var that = this,
                    isTag = this.attrModal.tag ? true : false,
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
                    if (rule.rules) {
                        if (!isTag && !that.tagEntityCheck) {
                            var state = _.find(rule.rules, { id: "__state" });
                            if (state) {
                                that.typeEntityCheck = (state.value === "ACTIVE" && state.operator === "=") || (state.value === "DELETED" && state.operator === "!=") ? false : true;
                                that.value.includeDE = that.typeEntityCheck;
                            } else if (that.typeEntityCheck) {
                                that.typeEntityCheck = false;
                                if (!that.tagEntityCheck) {
                                    that.value.includeDE = false;
                                }
                            }
                        }
                        if (isTag && !that.typeEntityCheck) {
                            var entityStatus = _.find(rule.rules, { id: "__entityStatus" });
                            if (entityStatus) {
                                that.tagEntityCheck = (entityStatus.value === "ACTIVE" && entityStatus.operator === "=") || (entityStatus.value === "DELETED" && entityStatus.operator === "!=") ? false : true;
                                that.value.includeDE = that.tagEntityCheck
                            } else if (that.tagEntityCheck) {
                                that.tagEntityCheck = false;
                                if (!that.typeEntityCheck) {
                                    that.value.includeDE = false;
                                }
                            }
                        }
                    }
                    this.attrModal.modal.close();
                    if ($(e.currentTarget).hasClass('search')) {
                        this.findSearchResult();
                    }
                }
            },
            manualRender: function(paramObj) {
                if (paramObj) {
                    this.value = paramObj;
                }
                this.updateQueryObject(paramObj);
                this.renderTypeTagList();
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
            renderTypeTagList: function(options) {
                var that = this;
                var serviceTypeToBefiltered = (options && options.filterList);
                var isTypeOnly = options && options.isTypeOnly;
                if (this.selectedFilter[this.type].length) {
                    serviceTypeToBefiltered = this.selectedFilter[this.type];
                }
                this.ui.typeLov.empty();
                var typeStr = '<option></option>',
                    tagStr = typeStr,
                    foundNewClassification = false;
                this.typeHeaders.fullCollection.each(function(model) {
                    var name = Utils.getName(model.toJSON(), 'name');
                    if (model.get('category') == 'ENTITY' && (serviceTypeToBefiltered && serviceTypeToBefiltered.length ? _.contains(serviceTypeToBefiltered, model.get('serviceType')) : true)) {
                        var entityCount = (that.entityCountObj.entity.entityActive[name] || 0) + (that.entityCountObj.entity.entityDeleted[name] || 0);
                        typeStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + ' ' + (entityCount ? "(" + _.numberFormatWithComma(entityCount) + ")" : '') + '</option>';
                    }
                    if (isTypeOnly == undefined && model.get('category') == 'CLASSIFICATION') {
                        var tagEntityCount = that.entityCountObj.tag.tagEntities[name];
                        if (that.value && that.value.tag) { // to check if wildcard classification is present in our data
                            if (name === that.value.tag) {
                                foundNewClassification = true;
                            }
                        }
                        tagStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + ' ' + (tagEntityCount ? "(" + _.numberFormatWithComma(tagEntityCount) + ")" : '') + '</option>';
                    }
                });

                if (this.type !== "dsl") {
                    _.each(Enums.addOnEntities, function(entity) {
                        typeStr += '<option  value="' + (entity) + '" data-name="' + (entity) + '">' + entity + '</option>';
                    });
                }
                if (!foundNewClassification && that.value) {
                    if (that.value.tag) {
                        var classificationValue = decodeURIComponent(that.value.tag);
                        tagStr += '<option  value="' + (classificationValue) + '" data-name="' + (classificationValue) + '">' + classificationValue + '</option>';
                    }
                }
                if (_.isUndefined(isTypeOnly)) {
                    //to insert extra classification list
                    if (that.value) {
                        _.each(Enums.addOnClassification, function(classificationName) {
                            if (classificationName !== that.value.tag) {
                                tagStr += '<option  value="' + (classificationName) + '" data-name="' + (classificationName) + '">' + classificationName + '</option>';
                            }
                        });
                    }
                    that.ui.tagLov.html(tagStr);
                    this.ui.tagLov.select2({
                        placeholder: "Select Classification",
                        allowClear: true,
                        tags: true,
                        createTag: function(tag) {
                            if (tag.term.indexOf('*') != -1) {
                                return {
                                    id: tag.term,
                                    text: tag.term,
                                    isNew: true
                                };
                            }
                        }
                    });
                }
                that.ui.typeLov.html(typeStr);
                var typeLovSelect2 = this.ui.typeLov.select2({
                    placeholder: "Select Type",
                    dropdownAdapter: $.fn.select2.amd.require("ServiceTypeFilterDropdownAdapter"),
                    allowClear: true,
                    getFilterBox: this.getFilterBox.bind(this),
                    onFilterSubmit: function(options) {
                        that.selectedFilter[that.type] = options.filterVal;
                        that.filterTypeSelected = that.selectedFilter[that.type];
                        that.renderTypeTagList({ "filterList": options.filterVal, isTypeOnly: true });
                        that.checkForButtonVisiblity();
                    }
                });
                if (this.value.filterTypeSelected === "undefined") {
                    typeLovSelect2.on("select2:close", function() {
                        typeLovSelect2.trigger("hideFilter");
                    });
                    if (typeLovSelect2 && isTypeOnly) {
                        typeLovSelect2.select2('open').trigger("change", { 'manual': true });
                    }
                }
                if (that.setInitialEntityVal) {
                    var defaultEntity = Enums.addOnEntities[0];
                    that.value.type = defaultEntity;
                    that.ui.typeLov.val(defaultEntity, null);
                }
            },
            renderTermList: function() {
                this.glossaryTermArray = null; //This Value is created to store the result of search Term while basic search through term.
                var that = this;
                var getTypeAheadData = function(data, params) {
                    var dataList = data.entities,
                        foundOptions = [];
                    _.each(dataList, function(obj) {
                        if (obj) {
                            if (obj.guid) {
                                obj['id'] = obj.attributes['qualifiedName'];
                            }
                            foundOptions.push(obj);
                        }
                    });
                    return foundOptions;
                };
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
                            that.glossaryTermArray = getTypeAheadData(data, params); //storing the search Results obj
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
            setValues: function(paramObj) {
                var arr = [],
                    that = this;
                if (paramObj) {
                    this.value = paramObj;
                }
                if (this.value.filterTypeSelected) {
                    this.filterTypeSelected = this.value.filterTypeSelected.split(',');
                    this.renderTypeTagList({ "filterList": this.filterTypeSelected, isTypeOnly: true });
                    this.ui.typeLov.val(this.value.type);
                }
                if (this.value) {
                    this.ui.searchInput.val(this.value.query || "");
                    if (this.value.dslChecked == "true" || this.value.searchType === "dsl") {
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
                            this.ui.termLov.append('<option value="' + _.escape(this.value.term) + '" selected="selected">' + _.escape(this.value.term) + '</option>');
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
                        that.ui.searchInput.focus();
                    }, 0);
                }
            },
            findSearchResult: function() {
                this.triggerSearch(this.ui.searchInput.val());
            },
            //This below function returns the searched Term Guid.
            getSearchedTermGuid: function() {
                var searchedTerm = this.ui.termLov.select2('val'),
                    searchedTermGuid = null;
                if (searchedTerm) {
                    this.glossaryTermArray.find(function(obj) {
                        if (searchedTerm === obj.id)
                            searchedTermGuid = obj.guid;
                    });
                }
                return searchedTermGuid;
            },
            triggerSearch: function(value) {
                var params = {
                        searchType: this.type,
                        dslChecked: this.ui.searchType.is(':checked'),
                        tagFilters: null,
                        entityFilters: null,
                        filterTypeSelected: this.filterTypeSelected
                    },
                    typeLovValue = this.ui.typeLov.find(':selected').data('name'), // to get count of selected element used data
                    tagLovValue = this.ui.tagLov.find(':selected').data('name') || this.ui.tagLov.val(),
                    termLovValue = this.ui.termLov.select2('val');
                params['type'] = typeLovValue || null;
                if (!this.dsl) {
                    params['tag'] = tagLovValue || null;
                    params['term'] = termLovValue || null;
                    params['guid'] = this.getSearchedTermGuid(); //Adding Guid in the URL for selection while switching. 
                    var entityFilterObj = this.searchTableFilters['entityFilters'],
                        tagFilterObj = this.searchTableFilters['tagFilters'];
                    params['includeDE'] = false;
                    params['excludeST'] = false;
                    params['excludeSC'] = false;
                    if (this.value) {
                        if (this.value.tag) {
                            params['tagFilters'] = tagFilterObj[this.value.tag]
                        }
                        if (this.value.type) {
                            params['entityFilters'] = entityFilterObj[this.value.type]
                        }
                        var columnList = this.value.type && this.searchTableColumns ? this.searchTableColumns[this.value.type] : null;
                        if (columnList) {
                            params['attributes'] = columnList.join(',');
                        }
                        params['includeDE'] = _.isUndefinedNull(this.value.includeDE) ? false : this.value.includeDE;
                        params['excludeST'] = _.isUndefinedNull(this.value.excludeST) ? false : this.value.excludeST;
                        params['excludeSC'] = _.isUndefinedNull(this.value.excludeSC) ? false : this.value.excludeSC;
                    }
                }
                if (this.value && !_.isUndefinedNull(this.value.pageLimit)) {
                    params['pageLimit'] = this.value.pageLimit;
                }
                if (this.value && !_.isUndefinedNull(this.value.pageOffset)) {
                    if (!_.isUndefinedNull(this.query[this.type]) && this.query[this.type].query != value) {
                        params['pageOffset'] = 0;
                    } else {
                        params['pageOffset'] = this.value.pageOffset;
                    }
                }
                params['query'] = value || null;
                _.extend(this.query[this.type], params);
                Utils.setUrl({
                    url: '#!/search/searchResult',
                    urlParams: _.extend({}, this.query[this.type]),
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            dslFulltextToggle: function(e) {
                var paramObj = Utils.getUrlState.getQueryParams();
                if (paramObj && this.type == paramObj.searchType) {
                    this.updateQueryObject(paramObj);
                }
                if (e.currentTarget.checked) {
                    this.type = "dsl";
                    this.dsl = true;
                    this.$('.typeFilterBtn,.tagBox,.termBox,.basicSaveSearch').hide();
                    this.$('.typeFilter').addClass('col-sm-12');
                    this.$('.typeFilter').removeClass('col-sm-10');
                    this.$('.advanceSaveSearch').show();
                    this.$('.searchText').text('Search By Query');
                    this.ui.searchInput.attr("placeholder", 'Search By Query eg. where name="sales_fact"');
                } else {
                    this.$('.typeFilter').addClass('col-sm-10');
                    this.$('.typeFilter').removeClass('col-sm-12');
                    this.$('.typeFilterBtn,.tagBox,.termBox,.basicSaveSearch').show();
                    this.$('.advanceSaveSearch').hide();
                    this.dsl = false;
                    this.type = "basic";
                    this.$('.searchText').text('Search By Text');
                    this.ui.searchInput.attr("placeholder", "Search By Text");
                }
                if (Utils.getUrlState.isSearchTab()) {
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
                this.selectedFilter[this.type] = [];
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
                Utils.setUrl({
                    url: '#!/search/searchResult',
                    urlParams: {
                        searchType: this.type,
                        dslChecked: this.ui.searchType.is(':checked')
                    },
                    mergeBrowserUrl: false,
                    trigger: true
                });
            }
        });
    return SearchLayoutView;
});