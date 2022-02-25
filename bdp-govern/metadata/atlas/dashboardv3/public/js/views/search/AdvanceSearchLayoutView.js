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

    var AdvanceSearchLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends AdvanceSearchLayoutView */
        {
            _viewName: 'AdvanceSearchLayoutView',

            template: SearchLayoutViewTmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                searchInput: '[data-id="searchInput"]',
                searchBtn: '[data-id="searchBtn"]',
                clearSearch: '[data-id="clearSearch"]',
                typeLov: '[data-id="typeLOV"]',
                refreshBtn: '[data-id="refreshBtn"]',
                advancedInfoBtn: '[data-id="advancedInfo"]'
            },

            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["keyup " + this.ui.searchInput] = function(e) {
                    var code = e.which;
                    this.query[this.type].query = Globals.advanceSearchData.searchByQuery = e.currentTarget.value;
                    if (code == 13) {
                        that.findSearchResult();
                        this.getAdvanceSearchValues();
                    }
                    this.checkForButtonVisiblity();
                };
                events["click " + this.ui.searchBtn] = 'findSearchResult';
                events["click " + this.ui.clearSearch] = 'clearSearchData';
                events["change " + this.ui.typeLov] = 'checkForButtonVisiblity';
                events["click " + this.ui.refreshBtn] = 'onRefreshButton';
                events["click " + this.ui.advancedInfoBtn] = 'advancedInfo';
                $('.global-search-container').find("span[data-id='detailSearch']").on("click", function(e) {
                    if (that.$el.height() === 0) {
                        that.renderTypeTagList({ "filterList": Globals.advanceSearchData.filterTypeSelected, isTypeOnly: true });
                        that.setAdvanceSearchValues();
                    }
                });
                return events;
            },
            /**
             * intialize a new AdvanceSearchLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                var that = this;
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
                    }
                };
                if (!this.value) {
                    this.value = {};
                }
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
                var that = this;
                // array of tags which is coming from url
                this.initializeValues();
                this.dslFulltextToggle();
                this.getAdvanceSearchValues();
            },
            initializeValues: function() {
                this.renderTypeTagList();
                this.setValues();
                this.checkForButtonVisiblity();
            },
            getAdvanceSearchValues: function() {
                var params = Utils.getUrlState.getQueryParams();
                if (params && params.searchType === "dsl") {
                    Globals.advanceSearchData.searchByType = params.type;
                    Globals.advanceSearchData.searchByQuery = params.query;
                    if (params.filterTypeSelected) {
                        Globals.advanceSearchData.filterTypeSelected = params.filterTypeSelected.split(',');
                    }
                }
            },
            setAdvanceSearchValues: function() {
                this.ui.typeLov.val(Globals.advanceSearchData.searchByType).trigger('change');
                this.ui.searchInput.val(Globals.advanceSearchData.searchByQuery);
            },
            checkForButtonVisiblity: function(e, options) {
                var that = this;
                if (e && e.currentTarget) {
                    var $el = $(e.currentTarget),
                        isTypeEl = $el.data('id') == "typeLOV",
                        select2Data = $el.select2('data');
                    if (e.type == "change" && select2Data) {
                        var value = (_.isEmpty(select2Data) ? select2Data : _.first(select2Data).id),
                            key = "type";
                        Globals.advanceSearchData.searchByType = value;
                        value = value && value.length ? value : null;
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
                            }
                        }
                    }
                }
                var value = this.ui.searchInput.val() || _.first($(this.ui.typeLov).select2('data')).id;
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
                this.getAdvanceSearchValues();
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
                this.filterTypeSelected = Globals.advanceSearchData.filterTypeSelected;
                _.each(_.uniq(serviceArr), function(service) {
                    serviceStr += '<li><div class="pretty p-switch p-fill"><input type="checkbox" class="pull-left" data-value="' + (service) + '" value="" ' + (_.contains(that.filterTypeSelected, service) ? "checked" : "") + '/><div class="state p-primary"><label>' + (service.toUpperCase()) + '</label></div></div></li>';
                });
                var templt = serviceStr + '<hr class="hr-filter"/><div class="text-right"><div class="divider"></div><button class="btn btn-action btn-sm filterDone">Done</button></div>';
                return templt;
            },
            setValues: function(paramObj) {
                var arr = [],
                    that = this;
                this.setAdvanceSearchValues();
                setTimeout(function() {
                    !that.isDestroyed && that.ui.searchInput.focus();
                }, 0);
            },
            renderTypeTagList: function(options) {
                var that = this;
                var serviceTypeToBefiltered = (options && options.filterList);
                var isTypeOnly = options && options.isTypeOnly;
                this.ui.typeLov.empty();
                var typeStr = '<option></option>';
                this.typeHeaders.fullCollection.each(function(model) {
                    var name = Utils.getName(model.toJSON(), 'name');
                    if (model.get('category') == 'ENTITY' && (serviceTypeToBefiltered && serviceTypeToBefiltered.length ? _.contains(serviceTypeToBefiltered, model.get('serviceType')) : true)) {
                        var entityCount = (that.entityCountObj.entity.entityActive[name] + (that.entityCountObj.entity.entityDeleted[name] ? that.entityCountObj.entity.entityDeleted[name] : 0));
                        typeStr += '<option value="' + (name) + '" data-name="' + (name) + '">' + (name) + ' ' + (entityCount ? "(" + _.numberFormatWithComma(entityCount) + ")" : '') + '</option>';
                    }
                });
                that.ui.typeLov.html(typeStr);
                var typeLovSelect2 = this.ui.typeLov.select2({
                    placeholder: "Select Type",
                    dropdownAdapter: $.fn.select2.amd.require("ServiceTypeFilterDropdownAdapter"),
                    allowClear: true,
                    dropdownCssClass: "searchLayoutView",
                    getFilterBox: this.getFilterBox.bind(this),
                    onFilterSubmit: function(options) {
                        that.filterTypeSelected = options.filterVal;
                        Globals.advanceSearchData.filterTypeSelected = that.filterTypeSelected;
                        that.renderTypeTagList({ "filterList": options.filterVal, isTypeOnly: true });
                        that.checkForButtonVisiblity();
                    }
                });
                if (Globals.advanceSearchData.filterTypeSelected === "undefined") {
                    typeLovSelect2.on("select2:close", function() {
                        typeLovSelect2.trigger("hideFilter");
                    });
                    if (typeLovSelect2 && serviceTypeToBefiltered) {
                        typeLovSelect2.select2('open').trigger("change", { 'manual': true });
                    }
                }
            },
            findSearchResult: function() {
                this.triggerSearch(this.ui.searchInput.val());
                this.getAdvanceSearchValues();
            },
            triggerSearch: function(value) {
                var params = {
                        searchType: this.type,
                        filterTypeSelected: this.filterTypeSelected
                    },
                    typeLovValue = this.ui.typeLov.find(':selected').data('name');
                params['type'] = typeLovValue || null;

                //Saving the advance search query in Global variable to sync both the advance search.
                Globals.advanceSearchData.searchByType = typeLovValue;
                Globals.advanceSearchData.searchByQuery = value;

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
            dslFulltextToggle: function() {
                var paramObj = Utils.getUrlState.getQueryParams();
                if (paramObj && this.type == paramObj.searchType) {
                    this.updateQueryObject(paramObj);
                }
                this.type = "dsl";
                this.dsl = true;
                this.$('.typeFilterBtn,.tagBox,.termBox,.basicSaveSearch').hide();
                this.$('.advanceSaveSearch,.searchByText').show();
                this.$('.searchText').text('Search By Query');
                this.ui.searchInput.attr("placeholder", 'Search By Query eg. where name="sales_fact"');
            },
            clearSearchData: function() {
                this.filterTypeSelected = [];
                Globals.advanceSearchData = {};
                this.renderTypeTagList();
                this.updateQueryObject();
                this.ui.typeLov.val("").trigger("change");
                this.ui.searchInput.val("");
                this.checkForButtonVisiblity();
            }
        });
    return AdvanceSearchLayoutView;
});