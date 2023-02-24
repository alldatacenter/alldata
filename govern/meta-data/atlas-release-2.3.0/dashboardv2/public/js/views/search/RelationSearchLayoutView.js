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
    'hbs!tmpl/search/RelationshipSearch_tmpl',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/Globals',
    'utils/Enums',
    'collection/VSearchList',
    'utils/CommonViewFunction',
    'modules/Modal'
], function(require, Backbone, RelationshipSearchViewTmpl, Utils, UrlLinks, Globals, Enums, VSearchList, CommonViewFunction, Modal) {
    'use strict';

    var RelationshipSearchLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchLayoutView */
        {
            _viewName: 'RelationshipSearchLayoutView',

            template: RelationshipSearchViewTmpl,

            /** Layout sub regions */
            regions: {
                RSaveSearchRelationship: "[data-id='r_saveSearchRelationship']"
            },

            /** ui selector cache */
            ui: {
                searchBtn: '[data-id="relationshipSearchBtn"]',
                clearSearch: '[data-id="clearRelationSearch"]',
                relationshipLov: '[data-id="relationshipLOV"]',
                relationshipAttrFilter: '[data-id="relationAttrFilter"]',
                refreshBtn: '[data-id="refreshRelationBtn"]'
            },

            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.searchBtn] = 'findSearchResult';
                events["change " + this.ui.relationshipLov] = 'checkForButtonVisiblity';
                events["click " + this.ui.clearSearch] = 'clearSearchData';
                events["click " + this.ui.refreshBtn] = 'onRefreshButton';
                events["click " + this.ui.relationshipAttrFilter] = 'openAttrFilter';
                return events;
            },
            /**
             * intialize a new RelationshipSearchLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'searchVent', 'typeHeaders', 'searchTableColumns', 'searchTableFilters', 'entityDefCollection', 'enumDefCollection', 'classificationDefCollection', 'businessMetadataDefCollection', 'relationshipDefCollection', 'metricCollection', 'relationshipEventAgg'));
                if (!this.value) {
                    this.value = {};
                }
                this.bindEvents();
            },
            bindEvents: function() {
                this.listenTo(this.relationshipDefCollection, 'reset', function() {
                    this.renderRelationshipList();
                    this.setValues();
                });
                this.relationshipEventAgg.on("Relationship:Update", function(options) {
                    that.value = Utils.getUrlState.getQueryParams() || {};
                    that.initializeValues();
                });
            },
            onRender: function() {
                this.initializeValues();
            },
            initializeValues: function() {
                this.renderRelationshipList();
                this.checkForButtonVisiblity();
                this.setValues();
                this.renderSaveSearch();
            },
            renderSaveSearch: function() {
                var that = this;
                require(['views/search/save/SaveSearchView'], function(SaveSearchView) {
                    var saveSearchRelationshipCollection = new VSearchList(),
                        saveSearchCollection = new VSearchList();
                    saveSearchCollection.url = UrlLinks.saveSearchApiUrl();
                    saveSearchRelationshipCollection.fullCollection.comparator = function(model) {
                        return model.get('name').toLowerCase();
                    };
                    var obj = {
                        value: that.value,
                        searchVent: that.searchVent,
                        typeHeaders: that.typeHeaders,
                        fetchCollection: fetchSaveSearchCollection,
                        relationshipDefCollection: that.relationshipDefCollection,
                        getValue: function() {
                            var queryObj = that.value,
                                relationshipObj = that.searchTableFilters['relationshipFilters'],
                                urlObj = Utils.getUrlState.getQueryParams(),
                                finalObj = _.extend({}, queryObj, urlObj, {
                                'relationshipFilters': relationshipObj ? relationshipObj[queryObj.relationshipName] : null,
                                'relationshipName': queryObj.relationshipName,
                                'query': queryObj.query,
                            });
                            return finalObj;
                        },
                        applyValue: function(model, searchType) {
                            that.manualRender(_.extend(searchType, CommonViewFunction.generateUrlFromSaveSearchObject({
                                value: { "searchParameters": model.get('searchParameters'), 'uiParameters': model.get('uiParameters') },
                                relationshipDefCollection: that.relationshipDefCollection
                            })));
                        }
                    }
                    that.RSaveSearchRelationship.show(new SaveSearchView(_.extend(obj, {
                        isRelationship: true,
                        collection: saveSearchRelationshipCollection.fullCollection
                    })));

                    function fetchSaveSearchCollection() {
                        saveSearchCollection.fetch({
                            success: function(collection, data) {
                                saveSearchRelationshipCollection.fullCollection.reset(_.where(data, { "searchType": "BASIC_RELATIONSHIP" }));
                            },
                            silent: true
                        });
                    }
                    fetchSaveSearchCollection();
                });
            },
            makeFilterButtonActive: function(filtertypeParam) {
                var filtertype = "relationshipFilters";
                var filterObj = this.searchTableFilters['relationshipFilters'][this.value.relationshipName];
                if (this.value.relationshipName) {
                    if (filterObj && filterObj.length) {
                        this.ui.relationshipAttrFilter.addClass('active');
                    } else {
                        this.ui.relationshipAttrFilter.removeClass('active');
                    }
                    this.ui.relationshipAttrFilter.prop('disabled', false);
                } else {
                    this.ui.relationshipAttrFilter.removeClass('active');
                    this.ui.relationshipAttrFilter.prop('disabled', true);
                }
            },
            setValues: function(paramObj) {
                if (paramObj && paramObj.from !== "relationshipSearch") {
                    this.value = paramObj;
                }
                if (this.value) {
                    this.ui.relationshipLov.val(this.value.relationshipName);
                    if (this.ui.relationshipLov.data('select2')) {
                        if (this.ui.relationshipLov.val() !== this.value.relationshipName) {
                            this.value.relationshipName = null;
                            this.ui.relationshipLov.val("").trigger("change", { 'manual': true });
                        } else {
                            this.ui.relationshipLov.trigger("change", { 'manual': true });
                        }
                    }
                }
            },
            renderRelationshipList: function() {
                var relationStr = '<option></option>';
                this.ui.relationshipLov.empty();
                this.relationshipDefCollection.fullCollection.each(function(model) {
                    var name = Utils.getName(model.toJSON(), 'name');
                    relationStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + '</option>';
                });
                this.ui.relationshipLov.html(relationStr);
                var relationshipLovSelect2 = this.ui.relationshipLov.select2({
                    placeholder: "Select Relationship",
                    allowClear: true,
                });
            },
            checkForButtonVisiblity: function(e, options) {
                var isBasicSearch = true;
                this.ui.relationshipAttrFilter.prop('disabled', true);
                if (e && e.currentTarget) {
                    var $el = $(e.currentTarget),
                        isRelationshipEl = $el.data('id') == "relationshipLOV",
                        select2Data = $el.select2('data');
                    if (e.type === "change" && select2Data) {
                        var value = _.first($(this.ui.relationshipLov).select2('data')).id,
                            key = "relationshipName",
                            filterType = "relationshipFilters";
                        value = value && value.length ? value : null;
                        if (this.value) {
                            //On Change handle
                            if (this.value[key] !== value || (!value && !this.value[key])) {
                                var temp = {};
                                temp[key] = value;
                                _.extend(this.value, temp);
                                if (_.isUndefined(options)) {
                                    this.value.pageOffset = 0;
                                }
                            } else if (isBasicSearch) {
                                // Initial loading handle.
                                if (filterType) {
                                    var filterObj = this.searchTableFilters[filterType];
                                    if (filterObj && this.value[key]) {
                                        this.searchTableFilters[filterType][this.value[key]] = this.value[filterType] ? this.value[filterType] : null;
                                    }
                                }
                                if (this.value.relationshipName) {
                                    if (this.value.attributes) {
                                        var attributes = _.sortBy(this.value.attributes.split(',')),
                                            tableColumn = this.searchTableColumns[this.value.type];
                                        if (_.isEmpty(this.searchTableColumns) || !tableColumn) {
                                            this.searchTableColumns[this.value.relationshipName] = attributes
                                        } else if (tableColumn.join(",") !== attributes.join(",")) {
                                            this.searchTableColumns[this.value.relationshipName] = attributes;
                                        }
                                    } else if (this.searchTableColumns[this.value.relationshipName]) {
                                        this.searchTableColumns[this.value.relationshipName] = undefined;
                                    }
                                }
                            }
                            if (isBasicSearch && filterType) {
                                this.makeFilterButtonActive();
                            }
                        }
                    }
                }
                var value = this.ui.relationshipLov.val();
                if (value && value.length) {
                    this.ui.searchBtn.removeAttr("disabled");
                } else {
                    this.ui.searchBtn.attr("disabled", "true");
                }
            },
            manualRender: function(paramObj) {
                if (paramObj) {
                    this.value = paramObj;
                }
                this.renderRelationshipList();
                this.setValues(paramObj);
            },
            okAttrFilterButton: function(e) {
                var isTag = this.attrModal.tag ? true : false,
                    filtertype = 'relationshipFilters',
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
                    this.searchTableFilters[filtertype][(this.value.relationshipName)] = ruleUrl;
                    this.makeFilterButtonActive();
                    if (this.value && this.value.relationshipName && this.searchTableColumns) {
                        if (!this.searchTableColumns[this.value.relationshipName]) {
                            this.searchTableColumns[this.value.relationshipName] = ["name", "typeName", "end1", "end2", "label"];
                        }
                        this.searchTableColumns[this.value.relationshipName] = _.sortBy(_.union(this.searchTableColumns[this.value.relationshipName], getIdFromRuleObject(rule)));
                    }
                    this.attrModal.modal.close();
                    if ($(e.currentTarget).hasClass('search')) {
                        this.findSearchResult();
                    }
                }
            },
            findSearchResult: function() {
                this.triggerSearch(this.ui.relationshipLov.val());
                Globals.fromRelationshipSearch = true;
            },
            triggerSearch: function(value) {
                var param = {
                    relationshipName: value,
                    relationshipFilters: null,
                    searchType: 'basic'
                };
                if (!this.dsl) {
                    var relationFilterObj = this.searchTableFilters['relationshipFilters'];
                    if (this.value) {
                        if (this.value.relationshipName) {
                            param['relationshipFilters'] = relationFilterObj[this.value.relationshipName]
                        }
                    }
                    var columnList = this.value.relationshipName && this.searchTableColumns ? this.searchTableColumns[this.value.relationshipName] : null;
                    if (columnList) {
                        param['attributes'] = columnList.join(',');
                    }
                }
                _.extend(this.value, param);
                Utils.setUrl({
                    url: '#!/relationship/relationshipSearchResult',
                    urlParams: _.extend({}, param),
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            fetchCollection: function(e) {
                this.relationshipDefCollection.fetch({ reset: true });
            },
            onRefreshButton: function() {
                this.disableRefreshButton();
                var that = this,
                    apiCount = 2,
                    updateSearchList = function() {
                        if (apiCount === 0) {
                            that.initializeValues();
                            var checkURLValue = Utils.getUrlState.getQueryParams(that.url);
                            if (that.searchVent && (_.has(checkURLValue, "relationshipName"))) {
                                that.searchVent.trigger('relationSearch:refresh');
                            }
                        }
                    };
                this.relationshipDefCollection.fetch({
                    silent: true,
                    complete: function() {
                        --apiCount;
                        updateSearchList();
                    }
                });
            },
            disableRefreshButton: function() {
                var that = this;
                this.ui.refreshBtn.attr('disabled', true);
                setTimeout(function() {
                    $(that.ui.refreshBtn).attr('disabled', false);
                }, 1000);
            },
            openAttrFilter: function() {
                var that = this;
                require(['views/search/SearchQueryView'], function(SearchQueryView) {
                    that.attrModal = new SearchQueryView({
                        value: that.value,
                        relationship: true,
                        searchVent: that.searchVent,
                        typeHeaders: that.typeHeaders,
                        entityDefCollection: that.entityDefCollection,
                        enumDefCollection: that.enumDefCollection,
                        classificationDefCollection: that.classificationDefCollection,
                        businessMetadataDefCollection: that.businessMetadataDefCollection,
                        relationshipDefCollection: that.relationshipDefCollection,
                        searchTableFilters: that.searchTableFilters,
                    });
                    that.attrModal.on('ok', function(scope, e) {
                        that.okAttrFilterButton(e);
                    });
                });
            },
            clearSearchData: function() {
                this.ui.relationshipLov.val("").trigger("change");
                this.checkForButtonVisiblity();
                this.searchTableFilters.relationshipFilters = {};
                this.makeFilterButtonActive();
                var type = "relationshipSaveSearch"
                Utils.setUrl({
                    url: '#!/relationship',
                    urlParams: {
                        searchType: this.type
                    },
                    mergeBrowserUrl: false,
                    trigger: true
                });
                this.$('.' + type + ' .saveSearchList').find('li.active').removeClass('active');
                this.$('.' + type + ' [data-id="saveBtn"]').attr('disabled', true);
            }

        });
    return RelationshipSearchLayoutView;
});