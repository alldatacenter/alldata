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
    'table-dragger',
    'hbs!tmpl/search/SearchResultLayoutView_tmpl',
    'modules/Modal',
    'models/VEntity',
    'utils/Utils',
    'utils/Globals',
    'collection/VSearchList',
    'models/VCommon',
    'utils/CommonViewFunction',
    'utils/Messages',
    'utils/Enums',
    'utils/UrlLinks',
    'platform'
], function(require, Backbone, tableDragger, SearchResultLayoutViewTmpl, Modal, VEntity, Utils, Globals, VSearchList, VCommon, CommonViewFunction, Messages, Enums, UrlLinks, platform) {
    'use strict';

    var SearchResultLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchResultLayoutView */
        {
            _viewName: 'SearchResultLayoutView',

            template: SearchResultLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RTagLayoutView: "#r_tagLayoutView",
                RSearchLayoutView: "#r_searchLayoutView",
                REntityTableLayoutView: "#r_searchResultTableLayoutView",
                RSearchQuery: '#r_searchQuery'
            },

            /** ui selector cache */
            ui: {
                tagClick: '[data-id="tagClick"]',
                termClick: '[data-id="termClick"]',
                addTag: '[data-id="addTag"]',
                addTerm: '[data-id="addTerm"]',
                paginationDiv: '[data-id="paginationDiv"]',
                previousData: "[data-id='previousData']",
                nextData: "[data-id='nextData']",
                pageRecordText: "[data-id='pageRecordText']",
                addAssignTag: "[data-id='addAssignTag']",
                addAssignTerm: "[data-id='addAssignTerm']",
                createEntity: "[data-id='createEntity']",
                checkDeletedEntity: "[data-id='checkDeletedEntity']",
                checkSubClassification: "[data-id='checkSubClassification']",
                checkSubType: "[data-id='checkSubType']",
                colManager: "[data-id='colManager']",
                containerCheckBox: "[data-id='containerCheckBox']",
                columnEmptyInfo: "[data-id='columnEmptyInfo']",
                showPage: "[data-id='showPage']",
                gotoPage: "[data-id='gotoPage']",
                gotoPagebtn: "[data-id='gotoPagebtn']",
                activePage: "[data-id='activePage']"
            },
            templateHelpers: function() {
                return {
                    entityCreate: Globals.entityCreate,
                    searchType: this.searchType,
                    fromView: this.fromView,
                    isGlossaryView: this.fromView == "glossary",
                    isSearchTab: Utils.getUrlState.isSearchTab()
                };
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.tagClick] = function(e) {
                    var scope = $(e.currentTarget);
                    if (e.target.nodeName.toLocaleLowerCase() == "i") {
                        this.onClickTagCross(e);
                    } else {
                        this.triggerUrl({
                            url: '#!/tag/tagAttribute/' + scope.text(),
                            urlParams: null,
                            mergeBrowserUrl: false,
                            trigger: true,
                            updateTabState: null
                        });
                    }
                };
                events["click " + this.ui.termClick] = function(e) {
                    var scope = $(e.currentTarget);
                    if (e.target.nodeName.toLocaleLowerCase() == "i") {
                        this.onClickTermCross(e);
                    } else {
                        this.triggerUrl({
                            url: '#!/glossary/' + scope.find('i').data('termguid'),
                            urlParams: { gType: "term", viewType: "term", fromView: "entity" },
                            mergeBrowserUrl: false,
                            trigger: true,
                            updateTabState: null
                        });
                    }
                };
                events["keyup " + this.ui.gotoPage] = function(e) {
                    var code = e.which,
                        goToPage = parseInt(e.currentTarget.value);
                    if (e.currentTarget.value) {
                        that.ui.gotoPagebtn.attr('disabled', false);
                    } else {
                        that.ui.gotoPagebtn.attr('disabled', true);
                    }
                    if (code == 13) {
                        if (e.currentTarget.value) {
                            that.gotoPagebtn();
                        }
                    }
                };
                events["change " + this.ui.showPage] = 'changePageLimit';
                events["click " + this.ui.gotoPagebtn] = 'gotoPagebtn';
                events["click " + this.ui.addTag] = 'onClickAddTag';
                events["click " + this.ui.addTerm] = 'onClickAddTermBtn';
                events["click " + this.ui.addAssignTag] = 'onClickAddTag';
                events["click " + this.ui.addAssignTerm] = 'onClickAddTermBtn';
                events["click " + this.ui.nextData] = "onClicknextData";
                events["click " + this.ui.previousData] = "onClickpreviousData";
                events["click " + this.ui.createEntity] = 'onClickCreateEntity';
                events["click " + this.ui.checkDeletedEntity] = 'onCheckExcludeIncludeResult';
                events["click " + this.ui.checkSubClassification] = 'onCheckExcludeIncludeResult';
                events["click " + this.ui.checkSubType] = 'onCheckExcludeIncludeResult';
                return events;
            },
            /**
             * intialize a new SearchResultLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'guid', 'initialView', 'isTypeTagNotExists', 'classificationDefCollection', 'entityDefCollection', 'typeHeaders', 'searchVent', 'enumDefCollection', 'tagCollection', 'searchTableColumns', 'isTableDropDisable', 'fromView', 'glossaryCollection', 'termName', 'businessMetadataDefCollection', 'profileDBView'));
                this.entityModel = new VEntity();
                this.searchCollection = new VSearchList();
                this.limit = 25;
                this.asyncFetchCounter = 0;
                this.offset = 0;
                this.bindEvents();
                this.multiSelectEntity = [];
                this.activeEntityCountSelected = 0;
                this.searchType = 'Basic Search';
                this.columnOrder = null;
                this.defaultColumns = ["selected", "name", "description", "typeName", "owner", "tag", "term"];
                if (this.value) {
                    if (this.value.searchType && this.value.searchType == 'dsl') {
                        this.searchType = 'Advanced Search';
                    }
                    if (this.value.pageLimit) {
                        var pageLimit = parseInt(this.value.pageLimit, 10);
                        if (_.isNaN(pageLimit) || pageLimit == 0 || pageLimit <= -1) {
                            this.value.pageLimit = this.limit;
                            this.triggerUrl();
                        } else {
                            this.limit = pageLimit;
                        }
                    }
                    if (this.value.pageOffset) {
                        var pageOffset = parseInt(this.value.pageOffset, 10);
                        if (_.isNaN(pageOffset) || pageLimit <= -1) {
                            this.value.pageOffset = this.offset;
                            this.triggerUrl();
                        } else {
                            this.offset = pageOffset;
                        }
                    }
                };
                if (platform.name === "IE") {
                    this.isTableDropDisable = true;
                }
            },
            bindEvents: function() {
                var that = this;
                this.onClickLoadMore();
                this.listenTo(this.searchCollection, 'backgrid:selected', function(model, checked) {
                    this.multiSelectEntity = [];
                    that.activeEntityCountSelected = 0;
                    if (checked === true) {
                        model.set("isEnable", true);
                    } else {
                        model.set("isEnable", false);
                    }
                    this.searchCollection.find(function(item) {
                        if (item.get('isEnable')) {
                            var obj = item.toJSON();
                            if (item.get('status') === 'ACTIVE') {
                                that.activeEntityCountSelected++;
                            }
                            that.multiSelectEntity.push({
                                id: obj.guid,
                                model: obj
                            });
                        }
                    });
                    this.updateMultiSelect();
                });
                this.listenTo(this.searchCollection, "error", function(model, response) {
                    this.hideLoader({ type: 'error' });
                    var responseJSON = response && response.responseJSON ? response.responseJSON : null,
                        errorText = (responseJSON && (responseJSON.errorMessage || responseJSON.message || responseJSON.error)) || 'Invalid Expression';
                    if (errorText) {
                        Utils.notifyError({
                            content: errorText
                        });
                        this.$('.searchTable > .well').html('<center>' + _.escape(errorText) + '</center>')
                    }
                }, this);
                this.listenTo(this.searchCollection, "state-changed", function(state) {
                    if (Utils.getUrlState.isSearchTab()) {
                        this.updateColumnList(state);
                        var excludeDefaultColumn = [];
                        if (this.value && this.value.type) {
                            excludeDefaultColumn = _.difference(this.searchTableColumns[this.value.type], this.defaultColumns);
                            if (this.searchTableColumns[this.value.type] === null) {
                                this.ui.columnEmptyInfo.show();
                            } else {
                                this.ui.columnEmptyInfo.hide();
                            }
                        }
                        this.columnOrder = this.getColumnOrder(this.REntityTableLayoutView.$el.find('.colSort th.renderable'));
                        this.triggerUrl();
                        var attributes = this.searchCollection.filterObj.attributes;
                        if ((excludeDefaultColumn && attributes) && (excludeDefaultColumn.length > attributes.length || _.difference(excludeDefaultColumn, attributes).length)) {
                            this.fetchCollection(this.value);
                        }
                    }
                }, this);
                this.listenTo(this.searchVent, "search:refresh", function(model, response) {
                    this.fetchCollection();
                }, this);
                this.listenTo(this.searchCollection, "backgrid:sorted", function(model, response) {
                    this.checkTableFetch();
                }, this)
            },
            onRender: function() {
                var that = this;
                this.checkEntityImage = {};
                this.commonTableOptions = {
                    collection: this.searchCollection,
                    includePagination: false,
                    includeFooterRecords: false,
                    includeColumnManager: (Utils.getUrlState.isSearchTab() && this.value && this.value.searchType === "basic" && !this.profileDBView ? true : false),
                    includeOrderAbleColumns: false,
                    includeSizeAbleColumns: false,
                    includeTableLoader: false,
                    includeAtlasTableSorting: true,
                    showDefaultTableSorted: true,
                    updateFullCollectionManually: true,
                    columnOpts: {
                        opts: {
                            initialColumnsVisible: null,
                            saveState: false
                        },
                        visibilityControlOpts: {
                            buttonTemplate: _.template("<button class='btn btn-action btn-sm pull-right'>Columns&nbsp<i class='fa fa-caret-down'></i></button>")
                        },
                        el: this.ui.colManager
                    },
                    gridOpts: {
                        emptyText: 'No Records found!',
                        className: 'table table-hover backgrid table-quickMenu colSort'
                    },
                    sortOpts: {
                        sortColumn: "name",
                        sortDirection: "ascending"
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                if (!this.initialView) {
                    var value = {},
                        that = this;
                    if (this.value) {
                        value = this.value;
                        if (value && value.includeDE) {
                            this.ui.checkDeletedEntity.prop('checked', true);
                        }
                        if (value && value.excludeSC) {
                            this.ui.checkSubClassification.prop('checked', true);
                        }
                        if (value && value.excludeST) {
                            this.ui.checkSubType.prop('checked', true);
                        }

                    } else {
                        value = {
                            'query': null,
                            'searchType': 'basic'
                        };
                    }
                    this.updateColumnList();
                    if (this.value && this.searchTableColumns && this.searchTableColumns[this.value.type] === null) {
                        this.ui.columnEmptyInfo.show();
                    } else {
                        this.ui.columnEmptyInfo.hide();
                    }
                    this.fetchCollection(value, _.extend({ 'fromUrl': true }, (this.value && this.value.pageOffset ? { 'next': true } : null)));
                    this.ui.showPage.select2({
                        data: _.sortBy(_.union([25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500], [this.limit])),
                        tags: true,
                        dropdownCssClass: "number-input",
                        multiple: false
                    });
                    if (this.value && this.value.pageLimit) {
                        this.ui.showPage.val(this.limit).trigger('change', { "skipViewChange": true });
                    }
                } else {
                    if (Globals.entityTypeConfList) {
                        this.$(".entityLink").show();
                    }
                    if (this.isTypeTagNotExists) {
                        Utils.notifyError({
                            content: Messages.search.notExists
                        });
                    }
                }
            },
            getColumnOrderWithPosition: function() {
                var that = this;
                return _.map(that.columnOrder, function(value, key) {
                    return key + "::" + value;
                }).join(",");
            },
            triggerUrl: function(options) {
                Utils.setUrl(_.extend({
                    url: Utils.getUrlState.getQueryUrl().queyParams[0],
                    urlParams: this.columnOrder ? _.extend(this.value, { 'uiParameters': this.getColumnOrderWithPosition() }) : this.value,
                    mergeBrowserUrl: false,
                    trigger: false,
                    updateTabState: true
                }, options));
            },
            updateMultiSelect: function() {
                var addTermTagButton = this.$('.multiSelectTag,.multiSelectTerm');
                if (this.activeEntityCountSelected > 0) {
                    addTermTagButton.show();
                } else {
                    addTermTagButton.hide();
                }
            },
            updateColumnList: function(updatedList) {
                if (updatedList) {
                    var listOfColumns = [];
                    _.map(updatedList, function(obj) {
                        var key = obj.name;
                        if (obj.renderable) {
                            listOfColumns.push(obj.name);
                        }
                    });
                    listOfColumns = _.sortBy(listOfColumns);
                    this.value.attributes = listOfColumns.length ? listOfColumns.join(",") : null;
                    if (this.value && this.value.type && this.searchTableColumns) {
                        this.searchTableColumns[this.value.type] = listOfColumns.length ? listOfColumns : null;
                    }
                } else if (this.value && this.value.type && this.searchTableColumns && this.value.attributes) {
                    this.searchTableColumns[this.value.type] = this.value.attributes.split(",");
                }
            },
            fetchCollection: function(value, options) {
                var that = this,
                    isPostMethod = (this.value && this.value.searchType === "basic"),
                    isSearchTab = Utils.getUrlState.isSearchTab(),
                    tagFilters = null,
                    entityFilters = null;
                that.activeEntityCountSelected = 0;
                that.updateMultiSelect();
                if (isSearchTab) {
                    tagFilters = CommonViewFunction.attributeFilter.generateAPIObj(this.value.tagFilters);
                    entityFilters = CommonViewFunction.attributeFilter.generateAPIObj(this.value.entityFilters);
                }

                if (isPostMethod && isSearchTab) {
                    var excludeDefaultColumn = this.value.type && this.searchTableColumns ? _.difference(this.searchTableColumns[this.value.type], this.defaultColumns) : null,
                        filterObj = {
                            'entityFilters': entityFilters,
                            'tagFilters': tagFilters,
                            'attributes': excludeDefaultColumn ? excludeDefaultColumn : null
                        };
                }

                this.showLoader();
                if (Globals.searchApiCallRef && Globals.searchApiCallRef.readyState === 1) {
                    Globals.searchApiCallRef.abort();
                }
                var apiObj = {
                    skipDefaultError: true,
                    sort: false,
                    success: function(dataOrCollection, response) {
                        if (that.isDestroyed) {
                            return;
                        }
                        Globals.searchApiCallRef = undefined;
                        var isFirstPage = that.offset === 0,
                            dataLength = 0,
                            goToPage = that.ui.gotoPage.val();
                        that.ui.gotoPage.val('');
                        that.ui.gotoPage.parent().removeClass('has-error');
                        that.ui.gotoPagebtn.prop("disabled", true);
                        if (!(that.ui.pageRecordText instanceof jQuery)) {
                            return;
                        }
                        if (isPostMethod && dataOrCollection && dataOrCollection.entities) {
                            dataLength = dataOrCollection.entities.length;
                        } else {
                            dataLength = dataOrCollection.length;
                        }

                        if (!dataLength && that.offset >= that.limit && ((options && options.next) || goToPage) && (options && !options.fromUrl)) {
                            /* User clicks on next button and server returns
                            empty response then disabled the next button without rendering table*/

                            that.hideLoader();
                            var pageNumber = that.activePage + 1;
                            if (goToPage) {
                                pageNumber = goToPage;
                                that.offset = (that.activePage - 1) * that.limit;
                            } else {
                                that.finalPage = that.activePage;
                                that.ui.nextData.attr('disabled', true);
                                that.offset = that.offset - that.limit;
                            }
                            if (that.value) {
                                that.value.pageOffset = that.offset;
                                that.triggerUrl();
                            }
                            Utils.notifyInfo({
                                html: true,
                                content: Messages.search.noRecordForPage + '<b>' + Utils.getNumberSuffix({ number: pageNumber, sup: true }) + '</b> page'
                            });
                            return;
                        }
                        if (isPostMethod) {
                            Utils.findAndMergeRefEntity({
                                attributeObject: dataOrCollection.entities,
                                referredEntities: dataOrCollection.referredEntities
                            });
                            that.searchCollection.reset(dataOrCollection.entities, { silent: true });
                            that.searchCollection.fullCollection.reset(dataOrCollection.entities, { silent: true });
                        }


                        /*Next button check.
                        It's outside of Previous button else condition
                        because when user comes from 2 page to 1 page than we need to check next button.*/
                        if (dataLength < that.limit) {
                            that.ui.nextData.attr('disabled', true);
                        } else {
                            that.ui.nextData.attr('disabled', false);
                        }

                        if (isFirstPage && (!dataLength || dataLength < that.limit)) {
                            that.ui.paginationDiv.hide();
                        } else {
                            that.ui.paginationDiv.show();
                        }

                        // Previous button check.
                        if (isFirstPage) {
                            that.ui.previousData.attr('disabled', true);
                            that.pageFrom = 1;
                            that.pageTo = that.limit;
                        } else {
                            that.ui.previousData.attr('disabled', false);
                        }

                        if (options && options.next) {
                            //on next click, adding "1" for showing the another records.
                            that.pageTo = that.offset + that.limit;
                            that.pageFrom = that.offset + 1;
                        } else if (!isFirstPage && options && options.previous) {
                            that.pageTo = that.pageTo - that.limit;
                            that.pageFrom = (that.pageTo - that.limit) + 1;
                        }
                        that.ui.pageRecordText.html("Showing  <u>" + that.searchCollection.models.length + " records</u> From " + that.pageFrom + " - " + that.pageTo);
                        that.activePage = Math.round(that.pageTo / that.limit);
                        that.ui.activePage.attr('title', "Page " + that.activePage);
                        that.ui.activePage.text(that.activePage);
                        that.renderTableLayoutView();
                        that.multiSelectEntity = [];

                        if (dataLength > 0) {
                            that.$('.searchTable').removeClass('noData')
                        }

                        if (Utils.getUrlState.isSearchTab() && value && !that.profileDBView) {
                            var searchString = 'Results for: <span class="filterQuery">' + CommonViewFunction.generateQueryOfFilter(that.value) + "</span>";
                            if (Globals.entityCreate && Globals.entityTypeConfList && Utils.getUrlState.isSearchTab()) {
                                searchString += "<p>If you do not find the entity in search result below then you can" + '<a href="javascript:void(0)" data-id="createEntity"> create new entity</a></p>';
                            }
                            that.$('.searchResult').html(searchString);
                        }
                    },
                    silent: true,
                    reset: true
                }
                if (this.value) {
                    var checkBoxValue = {
                        'excludeDeletedEntities': (this.value.includeDE ? false : true),
                        'includeSubClassifications': (this.value.excludeSC ? false : true),
                        'includeSubTypes': (this.value.excludeST ? false : true),
                        'includeClassificationAttributes': true // server will return classication details with guid
                    }
                }
                if (value) {
                    if (value.searchType) {
                        this.searchCollection.url = UrlLinks.searchApiUrl(value.searchType);
                    }
                    _.extend(this.searchCollection.queryParams, { 'limit': this.limit, 'offset': this.offset, 'query': _.trim(value.query), 'typeName': value.type || null, 'classification': value.tag || null, 'termName': value.term || null });
                    if (this.profileDBView && value.typeName && value.guid) {
                        var profileParam = {};
                        profileParam['guid'] = value.guid;
                        profileParam['relation'] = value.typeName === 'hive_db' ? '__hive_table.db' : '__hbase_table.namespace';
                        profileParam['sortBy'] = 'name';
                        profileParam['sortOrder'] = 'ASCENDING';
                        _.extend(this.searchCollection.queryParams, profileParam);
                    }
                    if (isPostMethod) {
                        this.searchCollection.filterObj = _.extend({}, filterObj);
                        apiObj['data'] = _.extend(checkBoxValue, filterObj, _.pick(this.searchCollection.queryParams, 'query', 'excludeDeletedEntities', 'limit', 'offset', 'typeName', 'classification', 'termName'));
                        Globals.searchApiCallRef = this.searchCollection.getBasicRearchResult(apiObj);
                    } else {
                        apiObj.data = null;
                        this.searchCollection.filterObj = null;
                        if (this.profileDBView) {
                            _.extend(this.searchCollection.queryParams, checkBoxValue);
                        }
                        Globals.searchApiCallRef = this.searchCollection.fetch(apiObj);
                    }
                } else {
                    _.extend(this.searchCollection.queryParams, { 'limit': this.limit, 'offset': this.offset });
                    if (isPostMethod) {
                        apiObj['data'] = _.extend(checkBoxValue, filterObj, _.pick(this.searchCollection.queryParams, 'query', 'excludeDeletedEntities', 'limit', 'offset', 'typeName', 'classification', 'termName'));
                        Globals.searchApiCallRef = this.searchCollection.getBasicRearchResult(apiObj);
                    } else {
                        apiObj.data = null;
                        if (this.profileDBView) {
                            _.extend(this.searchCollection.queryParams, checkBoxValue);
                        }
                        Globals.searchApiCallRef = this.searchCollection.fetch(apiObj);
                    }
                }
            },
            tableRender: function(options) {
                var that = this,
                    savedColumnOrder = options.order,
                    TableLayout = options.table,
                    columnCollection = Backgrid.Columns.extend({
                        sortKey: "displayOrder",
                        className: "my-awesome-css-animated-grid",
                        comparator: function(item) {
                            return item.get(this.sortKey) || 999;
                        },
                        setPositions: function() {
                            _.each(this.models, function(model, index) {
                                model.set("displayOrder", (savedColumnOrder == null ? index : parseInt(savedColumnOrder[model.get('label')])) + 1, { silent: true });
                            });
                            return this;
                        }
                    });
                var columns = new columnCollection((that.searchCollection.dynamicTable ? that.getDaynamicColumns(that.searchCollection.toJSON()) : that.getFixedDslColumn()));
                columns.setPositions().sort();
                if (this.searchType == "Advanced Search" && columns.length && that.searchCollection.length) {
                    //for dsl search with Select clause, default column to be use as sorting
                    var tableColumnNames = Object.keys(that.searchCollection.toJSON()[0]);
                    that.commonTableOptions['sortOpts'] = {
                        sortColumn: tableColumnNames[0],
                        sortDirection: "ascending"
                    };
                }
                var table = new TableLayout(_.extend({}, that.commonTableOptions, {
                    columns: columns
                }));
                if (table.collection.length === 0) {
                    that.$(".searchTable").addClass('noData');
                }
                if (!that.REntityTableLayoutView) {
                    return;
                }
                if (!that.value) {
                    that.value = that.options.value;
                }
                that.REntityTableLayoutView.show(table);
                if (that.value.searchType !== "dsl") {
                    that.ui.containerCheckBox.show();
                } else {
                    that.ui.containerCheckBox.hide();
                }
                that.$(".ellipsis-with-margin .inputAssignTag").hide();
                table.trigger("grid:refresh"); /*Event fire when table rendered*/
                // that.REntityTableLayoutView.$el.find('.colSort thead tr th:not(:first)').addClass('dragHandler');
                if (that.isTableDropDisable !== true) {
                    var tableDropFunction = function(from, to, el) {
                        tableDragger(document.querySelector(".colSort")).destroy();
                        that.columnOrder = that.getColumnOrder(el.querySelectorAll('th.renderable'));
                        that.triggerUrl();
                        that.tableRender({ "order": that.columnOrder, "table": TableLayout });
                        that.checkTableFetch();
                    }
                    that.REntityTableLayoutView.$el.find('.colSort thead tr th:not(.select-all-header-cell)').addClass('dragHandler');
                    tableDragger(document.querySelector(".colSort"), { dragHandler: ".dragHandler" }).on('drop', tableDropFunction);
                }
            },
            renderTableLayoutView: function(col) {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    // displayOrder added for column manager
                    if (that.value.uiParameters) {
                        var savedColumnOrder = _.object(that.value.uiParameters.split(',').map(function(a) {
                            return a.split('::');
                        })); // get Column position from string to object
                    }

                    that.tableRender({ "order": savedColumnOrder, "table": TableLayout });
                    that.checkTableFetch();
                });
            },
            getColumnOrder: function(arr) {
                var obj = {};
                for (var i = 0; i < arr.length; ++i) {
                    var innerText = arr[i].innerText.trim();
                    obj[(innerText == "" ? 'Select' : innerText)] = i;
                }
                return obj;
            },
            checkTableFetch: function() {
                if (this.asyncFetchCounter <= 0) {
                    this.hideLoader();
                    Utils.generatePopover({
                        el: this.$('[data-id="showMoreLess"]'),
                        contentClass: 'popover-tag-term',
                        viewFixedPopover: true,
                        popoverOptions: {
                            container: null,
                            content: function() {
                                return $(this).find('.popup-tag-term').children().clone();
                            }
                        }
                    });
                }
            },
            getFixedDslColumn: function() {
                var that = this,
                    nameCheck = 0,
                    columnToShow = null,
                    col = {};
                this.value = this.fromView === "glossary" ? this.value : Utils.getUrlState.getQueryParams() || this.value;
                if (this.value && this.value.searchType === "basic" && this.searchTableColumns && (this.searchTableColumns[this.value.type] !== undefined)) {
                    columnToShow = this.searchTableColumns[this.value.type] == null ? [] : this.searchTableColumns[this.value.type];
                }
                col['Check'] = {
                    name: "selected",
                    label: "Select",
                    cell: "select-row",
                    resizeable: false,
                    orderable: false,
                    renderable: (columnToShow ? _.contains(columnToShow, 'selected') : true),
                    headerCell: "select-all"
                };


                col['name'] = {
                    label: this.value && this.profileDBView ? "Table Name" : "Name",
                    cell: "html",
                    editable: false,
                    resizeable: true,
                    orderable: false,
                    renderable: true,
                    className: "searchTableName",
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function(rawValue, model) {
                            var obj = model.toJSON(),
                                nameHtml = "",
                                name = Utils.getName(obj);
                            if (!obj.attributes || obj.attributes.serviceType === undefined) {
                                if (Globals.serviceTypeMap[obj.typeName] === undefined && that.entityDefCollection) {
                                    var defObj = that.entityDefCollection.fullCollection.find({ name: obj.typeName });
                                    if (defObj) {
                                        Globals.serviceTypeMap[obj.typeName] = defObj.get('serviceType');
                                    }
                                }
                            } else if (Globals.serviceTypeMap[obj.typeName] === undefined) {
                                Globals.serviceTypeMap[obj.typeName] = obj.attributes ? obj.attributes.serviceType : null;
                            }
                            obj.serviceType = Globals.serviceTypeMap[obj.typeName];
                            if (obj.guid) {
                                if (obj.guid == "-1") {
                                    nameHtml = '<span title="' + name + '">' + name + '</span>';
                                } else {
                                    nameHtml = '<a title="' + name + '" href="#!/detailPage/' + obj.guid + (that.fromView ? "?from=" + that.fromView : "") + '">' + name + '</a>';
                                }
                            } else {
                                nameHtml = '<span title="' + name + '">' + name + '</span>';
                            }
                            if (obj.status && Enums.entityStateReadOnly[obj.status]) {
                                nameHtml += '<button type="button" title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i></button>';
                                nameHtml = '<div class="readOnly readOnlyLink">' + nameHtml + '</div>';
                            }
                            var getImageData = function(options) {
                                var imagePath = options.imagePath,
                                    returnImgUrl = null;
                                that.checkEntityImage[model.get('guid')] = false;
                                $.ajax({
                                        "url": imagePath,
                                        "method": "get",
                                        "cache": true
                                    })
                                    .always(function(data, status, xhr) {
                                        if (data.status == 404) {
                                            returnImgUrl = getImageData({
                                                "imagePath": Utils.getEntityIconPath({ entityData: obj, errorUrl: imagePath })
                                            });
                                        } else if (data) {
                                            that.checkEntityImage[model.get('guid')] = imagePath;
                                            returnImgUrl = imagePath;
                                            that.$("img[data-imgGuid='" + obj.guid + "']").removeClass("searchTableLogoLoader").attr("src", imagePath);
                                        }
                                    });
                            }
                            var img = "";
                            img = "<div><img data-imgGuid='" + obj.guid + "' class='searchTableLogoLoader'></div>";
                            if (that.checkEntityImage[model.get('guid')] == undefined) {
                                getImageData({ imagePath: Utils.getEntityIconPath({ entityData: obj }) });
                            } else {
                                if (that.checkEntityImage[model.get('guid')] != false) {
                                    img = "<div><img data-imgGuid='" + obj.guid + "' src='" + that.checkEntityImage[model.get('guid')] + "'></div>";
                                }
                            }
                            return (img + nameHtml);
                        }
                    })
                };

                col['owner'] = {
                    label: "Owner",
                    cell: "String",
                    editable: false,
                    resizeable: true,
                    orderable: true,
                    renderable: true,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function(rawValue, model) {
                            var obj = model.toJSON();
                            if (obj && obj.attributes && obj.attributes.owner) {
                                return obj.attributes.owner;
                            }
                        }
                    })
                };
                if (this.value && this.profileDBView) {
                    col['createTime'] = {
                        label: "Date Created",
                        cell: "Html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj && obj.attributes && obj.attributes.createTime) {
                                    return Utils.formatDate({ date: obj.attributes.createTime });
                                } else {
                                    return '-'
                                }
                            }
                        })
                    }
                }
                if (this.value && !this.profileDBView) {
                    col['description'] = {
                        label: "Description",
                        cell: "String",
                        editable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: true,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj && obj.attributes && obj.attributes.description) {
                                    return obj.attributes.description;
                                }
                            }
                        })
                    };


                    col['typeName'] = {
                        label: "Type",
                        cell: "Html",
                        editable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'typeName') : true),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj && obj.typeName) {
                                    return '<a title="Search ' + obj.typeName + '" href="#!/search/searchResult?query=' + obj.typeName + ' &searchType=dsl&dslChecked=true">' + obj.typeName + '</a>';
                                }
                            }
                        })
                    };
                    this.getTagCol({ 'col': col, 'columnToShow': columnToShow });
                    if ((!_.contains(["glossary"], this.fromView))) {
                        this.getTermCol({ 'col': col, 'columnToShow': columnToShow });
                    }

                    if (this.value && this.value.searchType === "basic") {
                        var def = this.entityDefCollection.fullCollection.find({ name: this.value.type }),
                            systemAttr = [],
                            businessMetadataAttr = [],
                            businessAttributes = {};
                        if (this.value.type == "_ALL_ENTITY_TYPES") {
                            this.businessMetadataDefCollection.each(function(model) {
                                var sortedAttributes = model.get('attributeDefs') || null,
                                    name = model.get('name');
                                if (sortedAttributes) {
                                    sortedAttributes = _.sortBy(sortedAttributes, function(obj) {
                                        return obj.name;
                                    });
                                    businessAttributes[name] = $.extend(true, {}, sortedAttributes);
                                }
                            })
                        } else {
                            businessAttributes = def ? ($.extend(true, {}, def.get('businessAttributeDefs')) || null) : null;
                        }
                        if (def || Globals[this.value.type] || (
                                this.value.tag ?
                                Globals[this.value.tag] ?
                                Globals[this.value.tag] :
                                Globals[Enums.addOnClassification[0]] :
                                undefined)) {
                            var attrObj = def ? Utils.getNestedSuperTypeObj({ data: def.toJSON(), collection: this.entityDefCollection, attrMerge: true }) : [];
                            if (this.value.type && (Globals[this.value.type] || Globals[Enums.addOnEntities[0]])) {
                                systemAttr = (Globals[this.value.type] || Globals[Enums.addOnEntities[0]]).attributeDefs;
                            }
                            if (this.value.tag && (Globals[this.value.tag] || Globals[Enums.addOnClassification[0]])) {
                                systemAttr = (Globals[this.value.tag] || Globals[Enums.addOnClassification[0]]).attributeDefs;
                            }
                            attrObj = attrObj.concat(systemAttr);
                            if (businessAttributes) {
                                _.each(businessAttributes, function(businessMetadata, businessMetadataName) {
                                    _.each(businessMetadata, function(attr, index) {
                                        var attribute = attr;
                                        attribute.isBusinessAttributes = true;
                                        attribute.name = businessMetadataName + '.' + attribute.name;
                                        businessMetadataAttr.push(attribute);
                                    })
                                })
                            }
                            attrObj = attrObj.concat(businessMetadataAttr);
                            _.each(attrObj, function(obj, key) {
                                var key = obj.name,
                                    isRenderable = _.contains(columnToShow, key),
                                    isSortable = obj.typeName.search(/(array|map)/i) == -1;
                                if (key == "name" || key == "description" || key == "owner") {
                                    if (columnToShow) {
                                        col[key].renderable = isRenderable;
                                    }
                                    return;
                                }
                                if (key == "__historicalGuids" || key == "__classificationsText" || key == "__classificationNames" || key == "__propagatedClassificationNames") {
                                    return;
                                }
                                col[obj.name] = {
                                    label: Enums.systemAttributes[obj.name] ? Enums.systemAttributes[obj.name] : (_.escape(obj.isBusinessAttributes ? obj.name : obj.name.capitalize())),
                                    cell: "Html",
                                    headerCell: Backgrid.HeaderHTMLDecodeCell,
                                    editable: false,
                                    resizeable: true,
                                    orderable: true,
                                    sortable: isSortable,
                                    renderable: isRenderable,
                                    headerClassName: obj.isBusinessAttributes ? "no-capitalize" : "",
                                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                        fromRaw: function(rawValue, model) {
                                            var modelObj = model.toJSON();
                                            if (modelObj && modelObj.attributes && !_.isUndefined(modelObj.attributes[key])) {
                                                var tempObj = {
                                                    'scope': that,
                                                    'attributeDefs': [obj],
                                                    'valueObject': {},
                                                    'isTable': false
                                                };
                                                if (key == "__labels") {
                                                    var values = modelObj.attributes[key] ? modelObj.attributes[key].split("|") : null,
                                                        valueOfArray = [];
                                                    if (values) {
                                                        if (values[values.length - 1] === "") { values.pop(); }
                                                        if (values[0] === "") { values.shift(); }
                                                        _.each(values, function(names) {
                                                            valueOfArray.push('<span class="json-string"><a class="btn btn-action btn-sm btn-blue btn-icon" ><span>' + _.escape(names) + '</span></a></span>');
                                                        });
                                                        return valueOfArray.join(' ');
                                                    }
                                                }
                                                if (key == "__customAttributes") {
                                                    var customAttributes = modelObj.attributes[key] ? JSON.parse(modelObj.attributes[key]) : null,
                                                        valueOfArray = [];
                                                    if (customAttributes) {
                                                        _.each(Object.keys(customAttributes), function(value, index) {
                                                            valueOfArray.push('<span class="json-string"><a class="btn btn-action btn-sm btn-blue btn-icon" ><span><span>' + _.escape(value) + '</span> : <span>' + _.escape(Object.values(customAttributes)[index]) + '</span></span></a></span>');
                                                        });
                                                        return valueOfArray.join(' ');
                                                    }
                                                }
                                                tempObj.valueObject[key] = modelObj.attributes[key];
                                                var tablecolumn = CommonViewFunction.propertyTable(tempObj);
                                                if (_.isArray(modelObj.attributes[key])) {
                                                    var column = $("<div>" + tablecolumn + "</div>")
                                                    if (tempObj.valueObject[key].length > 2) {
                                                        column.addClass("toggleList semi-collapsed").append("<span><a data-id='load-more-columns'>Show More</a></span>");
                                                    }
                                                    return column;
                                                }
                                                return tablecolumn;
                                            }
                                        }
                                    })
                                };
                            });
                        }
                    }
                }
                return this.searchCollection.constructor.getTableCols(col, this.searchCollection);
            },
            onClickLoadMore: function() {
                var that = this;
                this.$el.on('click', "[data-id='load-more-columns']", function(event) {
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                    var $this = $(this),
                        $toggleList = $(this).parents('.toggleList');
                    if ($toggleList.length) {
                        if ($toggleList.hasClass('semi-collapsed')) {
                            $toggleList.removeClass('semi-collapsed');
                            $this.text("Show Less");
                        } else {
                            $toggleList.addClass('semi-collapsed');
                            $this.text("Show More");
                        }
                    }
                });
            },
            getDaynamicColumns: function(valueObj) {
                var that = this,
                    col = {};
                if (valueObj && valueObj.length) {
                    var firstObj = _.first(valueObj);
                    _.each(_.keys(firstObj), function(key) {
                        col[key] = {
                            label: key.capitalize(),
                            cell: "Html",
                            editable: false,
                            resizeable: true,
                            orderable: true,
                            formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                fromRaw: function(rawValue, model) {
                                    var modelObj = model.toJSON();
                                    if (key == "name") {
                                        var nameHtml = "",
                                            name = _.escape(modelObj[key]);
                                        if (modelObj.guid) {
                                            nameHtml = '<a title="' + name + '" href="#!/detailPage/' + modelObj.guid + (that.fromView ? "?from=" + that.fromView : "") + '">' + name + '</a>';
                                        } else {
                                            nameHtml = '<span title="' + name + '">' + name + '</span>';
                                        }
                                        if (modelObj.status && Enums.entityStateReadOnly[modelObj.status]) {
                                            nameHtml += '<button type="button" title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i></button>';
                                            return '<div class="readOnly readOnlyLink">' + nameHtml + '</div>';
                                        }
                                        return nameHtml;
                                    } else if (modelObj && !_.isUndefined(modelObj[key])) {
                                        var tempObj = {
                                            'scope': that,
                                            // 'attributeDefs':
                                            'valueObject': {},
                                            'isTable': false
                                        };
                                        tempObj.valueObject[key] = modelObj[key];
                                        return CommonViewFunction.propertyTable(tempObj);
                                    }
                                }
                            })
                        };
                    });
                }
                return this.searchCollection.constructor.getTableCols(col, this.searchCollection);
            },
            getTagCol: function(options) {
                var that = this,
                    columnToShow = options.columnToShow,
                    col = options.col;
                if (col) {
                    col['tag'] = {
                        label: "Classifications",
                        cell: "Html",
                        editable: false,
                        sortable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'tag') : true),
                        className: 'searchTag',
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj.guid == "-1") {
                                    return
                                }
                                if (obj.status && Enums.entityStateReadOnly[obj.status]) {
                                    return '<div class="readOnly">' + CommonViewFunction.tagForTable(obj); + '</div>';
                                } else {
                                    return CommonViewFunction.tagForTable(obj);
                                }

                            }
                        })
                    };
                }
            },
            getTermCol: function(options) {
                var that = this,
                    columnToShow = options.columnToShow,
                    col = options.col;
                if (col) {
                    col['term'] = {
                        label: "Term",
                        cell: "Html",
                        editable: false,
                        sortable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'term') : true),
                        className: 'searchTag',
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj.guid == "-1") {
                                    return
                                }
                                if (obj.typeName && !(_.startsWith(obj.typeName, "AtlasGlossary"))) {
                                    if (obj.status && Enums.entityStateReadOnly[obj.status]) {
                                        return '<div class="readOnly">' + CommonViewFunction.termForTable(obj); + '</div>';
                                    } else {
                                        return CommonViewFunction.termForTable(obj);
                                    }
                                }
                            }
                        })
                    };
                }
            },
            addTagModalView: function(guid, multiple, entityCount) {
                var that = this;
                require(['views/tag/AddTagModalView'], function(AddTagModalView) {
                    var view = new AddTagModalView({
                        guid: guid,
                        multiple: multiple,
                        entityCount: entityCount,
                        callback: function() {
                            that.multiSelectEntity = [];
                            that.fetchCollection();
                            if (that.searchVent) {
                                that.searchVent.trigger('entityList:refresh');
                            }
                        },
                        tagList: that.getTagList(guid, multiple),
                        showLoader: that.showLoader.bind(that),
                        hideLoader: that.hideLoader.bind(that),
                        collection: that.classificationDefCollection,
                        enumDefCollection: that.enumDefCollection
                    });
                });
            },
            getTagList: function(guid, multiple) {
                var that = this;
                if (!multiple || multiple.length === 0) {
                    var model = this.searchCollection.find(function(item) {
                        var obj = item.toJSON();
                        if (obj.guid === guid) {
                            return true;
                        }
                    });
                    if (model) {
                        var obj = model.toJSON();
                    } else {
                        return [];
                    }
                    return _.compact(_.map(obj.classifications, function(val) {
                        if (val.entityGuid == guid) {
                            return val.typeName
                        }
                    }));
                } else {
                    return [];
                }
            },
            showLoader: function() {
                this.$('.fontLoader:not(.for-ignore)').addClass('show');
                this.$('.tableOverlay').addClass('show');
            },
            hideLoader: function(options) {
                this.$('.fontLoader:not(.for-ignore)').removeClass('show');
                options && options.type === 'error' ? this.$('.ellipsis-with-margin,.pagination-box').hide() : this.$('.ellipsis-with-margin,.pagination-box').show(); // only show for first time and hide when type is error
                this.$('.tableOverlay').removeClass('show');
            },
            onClickAddTag: function(e) {
                var guid = "",
                    that = this,
                    isTagMultiSelect = $(e.currentTarget).hasClass('multiSelectTag');
                if (isTagMultiSelect && this.multiSelectEntity && this.multiSelectEntity.length) {
                    that.addTagModalView(guid, this.multiSelectEntity, this.activeEntityCountSelected);
                } else {
                    guid = that.$(e.currentTarget).data("guid");
                    that.addTagModalView(guid);
                }
            },
            //This function checks for the lenght of Available terms and modal for adding terms is displayed accordingly.
            assignTermModalView: function(glossaryCollection, obj) {
                var that = this,
                    terms = 0;
                _.each(glossaryCollection.fullCollection.models, function(model) {
                    if (model.get('terms')) {
                        terms += model.get('terms').length;
                    };
                });
                if (terms) {
                    require(['views/glossary/AssignTermLayoutView'], function(AssignTermLayoutView) {
                        var view = new AssignTermLayoutView({
                            guid: obj.guid,
                            multiple: obj.multiple,
                            associatedTerms: obj.associatedTerms,
                            callback: function() {
                                that.multiSelectEntity = [];
                                that.fetchCollection();
                            },
                            glossaryCollection: glossaryCollection,
                        });
                    });
                } else {
                    Utils.notifyInfo({
                        content: "There are no available terms"
                    });
                }
            },
            onClickAddTermBtn: function(e) {
                var that = this,
                    guid = "",
                    entityGuid = $(e.currentTarget).data("guid"),
                    obj = {
                        guid: entityGuid,
                        multiple: undefined,
                        associatedTerms: undefined,
                    },
                    isTermMultiSelect = $(e.currentTarget).hasClass('multiSelectTerm');

                that.assignTermModalView(this.glossaryCollection, obj);
                if (isTermMultiSelect && this.multiSelectEntity && this.multiSelectEntity.length) {
                    obj.multiple = this.multiSelectEntity;
                } else if (entityGuid) {
                    obj.associatedTerms = this.searchCollection.find({ guid: entityGuid }).get('meanings');
                }
            },
            onClickTagCross: function(e) {
                var that = this,
                    tagName = $(e.target).data("name"),
                    guid = $(e.target).data("guid"),
                    entityGuid = $(e.target).data("entityguid"),
                    assetName = $(e.target).data("assetname");
                CommonViewFunction.deleteTag({
                    tagName: tagName,
                    guid: guid,
                    associatedGuid: guid != entityGuid ? entityGuid : null,
                    msg: "<div class='ellipsis-with-margin'>Remove: " + "<b>" + _.escape(tagName) + "</b> assignment from <b>" + _.escape(assetName) + " ?</b></div>",
                    titleMessage: Messages.removeTag,
                    okText: "Remove",
                    showLoader: that.showLoader.bind(that),
                    hideLoader: that.hideLoader.bind(that),
                    callback: function() {
                        that.fetchCollection();
                        if (that.searchVent) {
                            that.searchVent.trigger('entityList:refresh');
                        }
                    }
                });

            },
            onClickTermCross: function(e) {
                var $el = $(e.target),
                    termGuid = $el.data('termguid'),
                    guid = $el.data('guid'),
                    termName = $(e.currentTarget).text(),
                    assetname = $el.data('assetname'),
                    meanings = this.searchCollection.find({ "guid": guid }).get("meanings"),
                    that = this,
                    termObj = _.find(meanings, { termGuid: termGuid });
                CommonViewFunction.removeCategoryTermAssociation({
                    termGuid: termGuid,
                    model: {
                        guid: guid,
                        relationshipGuid: termObj.relationGuid
                    },
                    collection: that.glossaryCollection,
                    msg: "<div class='ellipsis-with-margin'>Remove: " + "<b>" + _.escape(termName) + "</b> assignment from <b>" + _.escape(assetname) + "?</b></div>",
                    titleMessage: Messages.glossary.removeTermfromEntity,
                    isEntityView: true,
                    buttonText: "Remove",
                    callback: function() {
                        that.fetchCollection();
                    }
                });
            },
            onClicknextData: function() {
                this.offset = this.offset + this.limit;
                _.extend(this.searchCollection.queryParams, {
                    offset: this.offset
                });
                if (this.value) {
                    this.value.pageOffset = this.offset;
                    this.triggerUrl();
                }
                this.fetchCollection(null, {
                    next: true
                });
            },
            onClickpreviousData: function() {
                this.offset = this.offset - this.limit;
                if (this.offset <= -1) {
                    this.offset = 0;
                }
                _.extend(this.searchCollection.queryParams, {
                    offset: this.offset
                });
                if (this.value) {
                    this.value.pageOffset = this.offset;
                    this.triggerUrl();
                }
                this.fetchCollection(null, {
                    previous: true
                });
            },
            onClickCreateEntity: function(e) {
                var that = this;
                $(e.currentTarget).blur();
                require([
                    'views/entity/CreateEntityLayoutView'
                ], function(CreateEntityLayoutView) {
                    var view = new CreateEntityLayoutView({
                        entityDefCollection: that.entityDefCollection,
                        typeHeaders: that.typeHeaders,
                        searchVent: that.searchVent,
                        callback: function() {
                            that.fetchCollection();
                        }
                    });
                });
            },
            onCheckExcludeIncludeResult: function(e) {
                var flag = false,
                    val = $(e.currentTarget).attr('data-value');
                if (e.target.checked) {
                    flag = true;
                }
                if (this.value) {
                    this.value[val] = flag;
                    this.triggerUrl();
                }
                _.extend(this.searchCollection.queryParams, { limit: this.limit, offset: this.offset });
                this.fetchCollection();

            },
            changePageLimit: function(e, obj) {
                if (!obj || (obj && !obj.skipViewChange)) {
                    var limit = parseInt(this.ui.showPage.val());
                    if (limit == 0) {
                        this.ui.showPage.data('select2').$container.addClass('has-error');
                        return;
                    } else {
                        this.ui.showPage.data('select2').$container.removeClass('has-error');
                    }
                    this.limit = limit;
                    this.offset = 0;
                    if (this.value) {
                        this.value.pageLimit = this.limit;
                        this.value.pageOffset = this.offset;
                        this.triggerUrl();
                    }
                    _.extend(this.searchCollection.queryParams, { limit: this.limit, offset: this.offset });
                    this.fetchCollection();
                }
            },
            gotoPagebtn: function(e) {
                var that = this;
                var goToPage = parseInt(this.ui.gotoPage.val());
                if (!(_.isNaN(goToPage) || goToPage <= -1)) {
                    if (this.finalPage && this.finalPage < goToPage) {
                        Utils.notifyInfo({
                            html: true,
                            content: Messages.search.noRecordForPage + '<b>' + Utils.getNumberSuffix({ number: goToPage, sup: true }) + '</b> page'
                        });
                        return;
                    }
                    this.offset = (goToPage - 1) * this.limit;
                    if (this.offset <= -1) {
                        this.offset = 0;
                    }
                    _.extend(this.searchCollection.queryParams, { limit: this.limit, offset: this.offset });
                    if (this.offset == (this.pageFrom - 1)) {
                        Utils.notifyInfo({
                            content: Messages.search.onSamePage
                        });
                    } else {
                        if (this.value) {
                            this.value.pageOffset = this.offset;
                            this.triggerUrl();
                        }
                        // this.offset is updated in gotoPagebtn function so use next button calculation.
                        this.fetchCollection(null, { 'next': true });
                    }
                }
            }
        });
    return SearchResultLayoutView;
});