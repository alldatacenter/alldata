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

define(["require", "backbone", "utils/Globals", "hbs!tmpl/search/SearchDefaultLayoutView_tmpl", 'utils/Utils', 'utils/CommonViewFunction', 'utils/Enums'], function(require, Backbone, Globals, SearchDefaultLayoutViewTmpl, Utils, CommonViewFunction, Enums) {
    "use strict";

    var SearchDefaultlLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchDefaultlLayoutView */
        {
            _viewName: "SearchDefaultlLayoutView",

            template: SearchDefaultLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RGlobalSearchLayoutView: "#r_globalSearchLayoutView",
                RSearchResultLayoutView: "#r_searchResultLayoutView",
                RQueryBuilderEntity: "#r_attributeQueryBuilderEntity",
                RQueryBuilderClassification: "#r_attributeQueryBuilderClassification"
            },

            /** ui selector cache */
            ui: {
                resizable: '[data-id="resizable"]',
                attrFilter: "[data-id='attrFilter']",
                attrApply: "[data-id='attrApply']",
                attrClose: "[data-id='attrClose']",
                entityRegion: "[data-id='entityRegion']",
                classificationRegion: "[data-id='classificationRegion']",
                checkDeletedEntity: "[data-id='checkDeletedEntity']",
                checkSubClassification: "[data-id='checkSubClassification']",
                checkSubType: "[data-id='checkSubType']",
                entityName: ".entityName",
                classificationName: ".classificationName",
                createNewEntity: '[data-id="createNewEntity"]',
                clearQuerySearch: "[data-id='clearQuerySearch']",
                refreshSearchQuery: "[data-id='refreshSearchResult']"
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.attrFilter] = function(e) {
                    if (this.$('.attribute-filter-container').hasClass("hide")) {
                        this.onClickAttrFilter();
                        this.$('.attributeResultContainer').addClass("overlay");
                    } else {
                        this.$('.attributeResultContainer').removeClass("overlay");
                    }
                    this.$('.fa-angle-right').toggleClass('fa-angle-down');
                    this.$('.attribute-filter-container, .attr-filter-overlay').toggleClass('hide');
                };

                events["click " + this.ui.refreshSearchQuery] = function(e) {
                    this.options.searchVent.trigger('search:refresh');
                };

                events["click " + this.ui.attrApply] = function(e) {
                    that.okAttrFilterButton(e);
                };

                events["click " + this.ui.attrClose] = function(e) {
                    // this.$('.attribute-filter-container').hide();
                    // this.$('.fa-chevron-right').toggleClass('fa-chevron-down');
                    this.$('.attributeResultContainer').removeClass("overlay");
                    this.$('.fa-angle-right').toggleClass('fa-angle-down');
                    this.$('.attribute-filter-container, .attr-filter-overlay').toggleClass('hide');
                };
                events["click " + ".clear-attr"] = function(e) {
                    var type = $(e.currentTarget).data("type"),
                        that = this;

                    switch (type) {
                        case "entityFilters":
                            that.filtersQueryUpdate(e, false);
                            break;
                        case "tagFilters":
                            that.filtersQueryUpdate(e, true);
                            break;
                        default:
                            this.options.value[type] = null;
                    }
                    this.searchAttrFilter();
                };
                events["click " + this.ui.clearQuerySearch] = function(e) {
                    var notifyObj = {
                        modal: true,
                        ok: function(argument) {
                            if (Utils.getUrlState.getQueryParams().searchType === "dsl") {
                                Globals.advanceSearchData = {};
                            }
                            Utils.setUrl({
                                url: '#!/search',
                                mergeBrowserUrl: false,
                                trigger: true,
                                updateTabState: true
                            });
                        },
                        cancel: function(argument) {}
                    };
                    notifyObj['text'] = "Search parameters will be reset and you will return to the default search page. Continue?";
                    Utils.notifyConfirm(notifyObj);
                };
                events["click " + this.ui.createNewEntity] = 'onCreateNewEntity';
                events["click " + this.ui.checkDeletedEntity] = 'onCheckExcludeIncludeResult';
                events["click " + this.ui.checkSubClassification] = 'onCheckExcludeIncludeResult';
                events["click " + this.ui.checkSubType] = 'onCheckExcludeIncludeResult';
                return events;
            },
            templateHelpers: function() {
                return {
                    entityCreate: Globals.entityCreate
                };
            },
            /**
             * intialize a new SearchDefaultlLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this.options, options);
                this.hidenFilter = false;
                this.tagAttributeLength = 0;
                this.entityAttributeLength = 0;
                this.tagEntityCheck = false;
                this.typeEntityCheck = false;
            },
            bindEvents: function() {},
            onRender: function() {
                this.toggleLayoutClass = this.$(".col-sm-9.f-right, .col-sm-12.f-right");
                this.renderGlobalSearch();
                this.renderSearchResult();
                this.updateView();
                this.ui.entityRegion.hide();
                this.ui.classificationRegion.hide();
            },
            filtersQueryUpdate: function(e, isTag) {
                var filters = CommonViewFunction.attributeFilter.extractUrl({ "value": isTag ? this.options.value.tagFilters : this.options.value.entityFilters, "formatDate": true }),
                    rules = filters.rules,
                    filtertype = isTag ? "tagFilters" : "entityFilters",
                    that = this;

                filters.rules = _.filter(rules, function(obj, key) {
                    return (obj.id + key) != $(e.currentTarget).data("id");
                });
                if (filters) {
                    that.updateFilterOptions(filters, filtertype, isTag);
                }
                if (filters.rules.length == 0) {
                    if (isTag) {
                        this.options.searchTableFilters["tagFilters"][that.options.value.tag] = "";
                    } else {
                        this.options.searchTableFilters["entityFilters"][that.options.value.type] = "";
                    }
                }
            },
            onCheckExcludeIncludeResult: function(e) {

                var flag = false,
                    val = $(e.currentTarget).attr('data-value');
                if (e.target.checked) {
                    flag = true;
                }
                if (this.options.value) {
                    this.options.value[val] = flag;
                    //  this.triggerUrl();
                }
                //_.extend(this.searchCollection.queryParams, { limit: this.limit, offset: this.offset });
                // this.fetchCollection();

            },
            onCreateNewEntity: function(e) {
                var that = this;
                require([
                    'views/entity/CreateEntityLayoutView'
                ], function(CreateEntityLayoutView) {
                    var view = new CreateEntityLayoutView({
                        entityDefCollection: that.options.entityDefCollection,
                        typeHeaders: that.options.typeHeaders,
                        searchVent: that.options.searchVent,
                        callback: function() {
                            // that.renderFliterBrowser();
                        }
                    });
                });
            },
            updateView: function() {
                if (this.options.fromSearchResultView) {
                    this.$('.search-container,[data-id="createEntity"]').hide();
                    $("body").removeClass("ui-autocomplete-small-height");
                    this.$(".searchResultContainer,.attributeResultContainer").show();

                } else {
                    this.$('.search-container,[data-id="createEntity"]').show();
                    $("body").addClass("ui-autocomplete-small-height");
                    this.$(".searchResultContainer,.attributeResultContainer").hide();
                }
            },
            manualRender: function(options) {
                _.extend(this.options, options);
                this.updateView();
                this.onClickAttrFilter();
                this.renderSearchResult();
            },
            renderGlobalSearch: function() {
                var that = this;
                require(["views/search/GlobalSearchLayoutView"], function(GlobalSearchLayoutView) {

                    that.RGlobalSearchLayoutView.show(new GlobalSearchLayoutView(_.extend({ closeOnSubmit: true }, _.omit(that.options, "value"))));
                });
            },
            renderSearchResult: function() {
                var that = this;
                require(["views/search/SearchResultLayoutView"], function(SearchResultLayoutView) {
                    that.RSearchResultLayoutView.show(new SearchResultLayoutView(that.options));
                });
            },
            checkEntityFilter: function(options) {
                if (options && options.value) {
                    if (options.value.type && options.value.entityFilters) {
                        options.searchTableFilters.entityFilters[options.value.type] = options.value.entityFilters;
                    }
                    if (options.value.tag && options.value.tagFilters) {
                        options.searchTableFilters.tagFilters[options.value.tag] = options.value.tagFilters;
                    }
                }
                return options.searchTableFilters;
            },
            onClickAttrFilter: function() {
                var that = this,
                    obj = {
                        value: that.options.value,
                        searchVent: that.options.searchVent,
                        entityDefCollection: that.options.entityDefCollection,
                        enumDefCollection: that.options.enumDefCollection,
                        typeHeaders: that.options.typeHeaders,
                        classificationDefCollection: that.options.classificationDefCollection,
                        businessMetadataDefCollection: that.options.businessMetadataDefCollection,
                        searchTableFilters: that.checkEntityFilter(that.options)
                    };
                this.tagEntityCheck = false;
                this.typeEntityCheck = false;
                if (that.options.value) {
                    this.ui.checkDeletedEntity.prop('checked', this.options.value.includeDE ? this.options.value.includeDE : false);
                    this.ui.checkSubClassification.prop('checked', this.options.value.excludeSC ? this.options.value.excludeSC : false);
                    this.ui.checkSubType.prop('checked', this.options.value.excludeST ? this.options.value.excludeST : false);

                    if (that.options.value.tag && that.options.value.type) {
                        this.$('.attribute-filter-container').removeClass('no-attr');
                        this.ui.classificationRegion.show();
                        this.ui.entityRegion.show();
                    } else {
                        if (!that.options.value.tag && !that.options.value.type) {
                            this.$('.attribute-filter-container').addClass('no-attr');
                        }
                        this.ui.entityRegion.hide();
                        this.ui.classificationRegion.hide();
                    }
                    if (that.options.value.tag) {
                        this.ui.classificationRegion.show();
                        // this.ui.entityRegion.hide();
                        var attrTagObj = that.options.classificationDefCollection.fullCollection.find({ name: that.options.value.tag });
                        if (attrTagObj) {
                            attrTagObj = Utils.getNestedSuperTypeObj({
                                data: attrTagObj.toJSON(),
                                collection: that.options.classificationDefCollection,
                                attrMerge: true,
                            });
                            this.tagAttributeLength = attrTagObj.length;
                        }
                        if (Globals[that.options.value.tag] || Globals[Enums.addOnClassification[0]]) {
                            obj.systemAttrArr = (Globals[that.options.value.tag] || Globals[Enums.addOnClassification[0]]).attributeDefs;
                            this.tagAttributeLength = obj.systemAttrArr.length;
                        }
                        this.renderQueryBuilder(_.extend({}, obj, {
                            tag: true,
                            type: false,
                            attrObj: attrTagObj
                        }), this.RQueryBuilderClassification);
                        this.ui.classificationName.html(that.options.value.tag);
                    }
                    if (that.options.value.type) {
                        this.ui.entityRegion.show();
                        var attrTypeObj = that.options.entityDefCollection.fullCollection.find({ name: that.options.value.type });
                        if (attrTypeObj) {
                            attrTypeObj = Utils.getNestedSuperTypeObj({
                                data: attrTypeObj.toJSON(),
                                collection: that.options.entityDefCollection,
                                attrMerge: true
                            });
                            this.entityAttributeLength = attrTypeObj.length;
                        }
                        if (Globals[that.options.value.type] || Globals[Enums.addOnEntities[0]]) {
                            obj.systemAttrArr = (Globals[that.options.value.type] || Globals[Enums.addOnEntities[0]]).attributeDefs;
                            this.entityAttributeLength = obj.systemAttrArr.length;
                        }
                        this.renderQueryBuilder(_.extend({}, obj, {
                            tag: false,
                            type: true,
                            attrObj: attrTypeObj
                        }), this.RQueryBuilderEntity);

                        this.ui.entityName.html(_.escape(that.options.value.type));
                    }
                }

            },
            okAttrFilterButton: function(e) {
                var isTag,
                    filtertype,
                    queryBuilderRef,
                    isFilterValidate = true,
                    that = this;

                if (this.options.value.tag) {
                    isTag = true;
                    filtertype = isTag ? "tagFilters" : "entityFilters";
                    if (this.tagAttributeLength !== 0) {
                        queryBuilderRef = this.RQueryBuilderClassification.currentView.ui.builder;
                        searchAttribute();
                    }
                }
                if (this.options.value.type) {
                    isTag = false;
                    filtertype = isTag ? "tagFilters" : "entityFilters";
                    if (this.entityAttributeLength !== 0) {
                        queryBuilderRef = this.RQueryBuilderEntity.currentView.ui.builder;
                        searchAttribute();
                    }
                }
                filterPopupStatus();

                function searchAttribute() {
                    var queryBuilderObj = queryBuilderRef.data("queryBuilder");
                    if (queryBuilderObj) {
                        var ruleWithInvalid = queryBuilderObj.getRules({ allow_invalid: true }),
                            rule = queryBuilderObj.getRules();
                        rule ? that.updateFilterOptions(rule, filtertype, isTag) : isFilterValidate = false;
                        if (ruleWithInvalid && ruleWithInvalid.rules.length === 1 && ruleWithInvalid.rules[0].id === null) {
                            isFilterValidate = true;
                            queryBuilderObj.clearErrors();
                        }
                        if (rule && rule.rules) {
                            if (!that.tagEntityCheck) {
                                var state = _.find(rule.rules, { id: "__state" });
                                if (state) {
                                    that.typeEntityCheck = (state.value === "ACTIVE" && state.operator === "=") || (state.value === "DELETED" && state.operator === "!=") ? false : true;
                                    that.options.value.includeDE = that.typeEntityCheck;
                                }
                            }
                            if (!that.typeEntityCheck) {
                                var entityStatus = _.find(rule.rules, { id: "__entityStatus" });
                                if (entityStatus) {
                                    that.tagEntityCheck = (entityStatus.value === "ACTIVE" && entityStatus.operator === "=") || (entityStatus.value === "DELETED" && entityStatus.operator === "!=") ? false : true;
                                    that.options.value.includeDE = that.tagEntityCheck
                                }
                            }
                        }
                    }
                }

                function filterPopupStatus() {
                    if (isFilterValidate) {
                        if ($(e.currentTarget).hasClass("search")) {
                            that.$('.fa-angle-right').toggleClass('fa-angle-down');
                            that.$('.attribute-filter-container, .attr-filter-overlay').toggleClass('hide');
                            that.searchAttrFilter();
                            that.$('.attributeResultContainer').removeClass("overlay");
                        }
                    }
                }
            },
            getIdFromRuleObj: function(rule) {
                var that = this,
                    col = new Set();
                _.map(rule.rules, function(obj, key) {
                    if (_.has(obj, "condition")) {
                        return that.getIdFromRuleObj(obj);
                    } else {
                        return col.add(obj.id);
                    }
                });
                return Array.from(col);
            },
            updateFilterOptions: function(rule, filtertype, isTag) {
                var that = this,
                    ruleUrl = CommonViewFunction.attributeFilter.generateUrl({ value: rule, formatedDateToLong: true });
                this.options.searchTableFilters[filtertype][isTag ? this.options.value.tag : this.options.value.type] = ruleUrl;
                if (!isTag && this.options.value && this.options.value.type && this.options.searchTableColumns) {
                    if (!this.options.searchTableColumns[this.options.value.type]) {
                        this.options.searchTableColumns[this.options.value.type] = ["selected", "name", "description", "typeName", "owner", "tag", "term"];
                    }
                    this.options.searchTableColumns[this.options.value.type] = _.sortBy(_.union(_.without(this.options.searchTableColumns[this.options.value.type]), this.getIdFromRuleObj(rule)));
                }
            },
            renderQueryBuilder: function(obj, rQueryBuilder) {
                var that = this;
                require(['views/search/QueryBuilderView'], function(QueryBuilderView) {
                    rQueryBuilder.show(new QueryBuilderView(obj));
                });
            },
            searchAttrFilter: function() {
                var selectedNodeId,
                    name = this.options.value.type || this.options.value.tag,
                    typeValue,
                    tagValue,
                    termValue,
                    entityFilterObj = this.options.searchTableFilters["entityFilters"],
                    tagFilterObj = this.options.searchTableFilters["tagFilters"],
                    params = {
                        searchType: "basic",
                        dslChecked: false,
                        tagFilters: null,
                        entityFilters: null,
                        query: null,
                        type: null,
                        tag: null,
                        term: null,
                        attributes: null,
                        pageOffset: null,
                        pageLimit: null,
                        includeDE: null
                    },
                    updatedUrl;

                if (this.options.value) {
                    if (this.options.value.tag) {
                        params["tag"] = this.options.value.tag;
                    }
                    if (this.options.value.type) {
                        params["type"] = this.options.value.type;
                    }
                    if (this.options.value.term) {
                        params["term"] = this.options.value.term;
                    }
                    if (this.options.value.query) {
                        params["query"] = this.options.value.query;
                    }
                    var columnList = this.options.value && this.options.value.type && this.options.searchTableColumns ? this.options.searchTableColumns[this.options.value.type] : null;
                    if (columnList) {
                        params["attributes"] = columnList.join(",");
                    }
                    params['includeDE'] = _.isUndefinedNull(this.options.value.includeDE) ? false : this.options.value.includeDE;
                    params["excludeST"] = _.isUndefinedNull(this.options.value.excludeST) ? false : this.options.value.excludeST;
                    params["excludeSC"] = _.isUndefinedNull(this.options.value.excludeSC) ? false : this.options.value.excludeSC;
                }
                if (entityFilterObj) {
                    params['entityFilters'] = entityFilterObj[this.options.value.type];
                }
                if (tagFilterObj) {
                    params['tagFilters'] = tagFilterObj[this.options.value.tag];
                }
                params["pageOffset"] = 0;

                if (!this.options.value.tag && !this.options.value.type && !this.options.value.term && !this.options.value.query) {
                    params["tag"] = null;
                    params["type"] = null;
                    params["term"] = null;
                    params["query"] = null;
                    params["attributes"] = null;
                    params["includeDE"] = null;
                    params["excludeST"] = null;
                    params["excludeSC"] = null;
                    updatedUrl = '#!/search';
                } else {
                    updatedUrl = '#!/search/searchResult';
                }
                Utils.setUrl({
                    url: updatedUrl,
                    urlParams: _.extend({}, params),
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });

                var paramObj = Utils.getUrlState.getQueryParams();
                this.options.value = paramObj;
            }
        }
    );
    return SearchDefaultlLayoutView;
});