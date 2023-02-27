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
    'hbs!tmpl/search/RelationSearchResultLayoutView_tmpl',
    'modules/Modal',
    'models/VEntity',
    'utils/Utils',
    'utils/Globals',
    'collection/VRelationshipSearchResultList',
    'models/VCommon',
    'utils/CommonViewFunction',
    'utils/Messages',
    'utils/Enums',
    'utils/UrlLinks',
    'platform'
], function(require, Backbone, tableDragger, RelationSearchResultLayoutViewTmpl, Modal, VEntity, Utils, Globals, VRelationshipSearchResultList, VCommon, CommonViewFunction, Messages, Enums, UrlLinks, platform) {
    'use strict';

    var RelationSearchResultLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends RelationSearchResultLayoutView */
        {
            _viewName: 'RelationSearchResultLayoutView',

            template: RelationSearchResultLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                REntityTableLayoutView: "#r_relationSearchResultTableLayoutView"
            },

            /** ui selector cache */
            ui: {
                paginationDiv: '[data-id="paginationDiv"]',
                previousData: "[data-id='previousData']",
                nextData: "[data-id='nextData']",
                pageRecordText: "[data-id='pageRecordText']",
                colManager: "[data-id='colManager']",
                columnEmptyInfo: "[data-id='columnEmptyInfo']",
                showPage: "[data-id='showPage']",
                gotoPage: "[data-id='gotoPage']",
                gotoPagebtn: "[data-id='gotoPagebtn']",
                activePage: "[data-id='activePage']"
            },
            templateHelpers: function() {
                return {
                    searchType: this.searchType,
                    fromView: this.fromView,
                };
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
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
                events["click " + this.ui.nextData] = "onClicknextData";
                events["click " + this.ui.previousData] = "onClickpreviousData";
                return events;
            },
            /**
             * intialize a new SearchResultLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'guid', 'initialView', 'searchVent', 'searchTableColumns', 'isTableDropDisable', 'fromView', 'profileDBView', 'relationshipDefCollection'));
                //this.entityModel = new VEntity();
                this.searchCollection = new VRelationshipSearchResultList();
                this.limit = 25;
                this.asyncFetchCounter = 0;
                this.offset = 0;
                this.bindEvents();
                this.searchType = 'Basic Search';
                this.columnOrder = null;
                this.defaultColumns = ["name", "typeName", "end1", "end2", "label"];
                if (this.value) {
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
            },
            bindEvents: function() {
                var that = this;
                this.onClickLoadMore();
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
                    if (Utils.getUrlState.isRelationTab()) {
                        this.updateColumnList(state);
                        var excludeDefaultColumn = [];
                        if (this.value && this.value.relationshipName) {
                            excludeDefaultColumn = _.difference(this.searchTableColumns[this.value.relationshipName], this.defaultColumns);
                            if (this.searchTableColumns[this.value.relationshipName] === null) {
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
                this.listenTo(this.searchVent, "relationSearch:refresh", function(model, response) {
                    this.updateColumnList();
                    this.fetchCollection();
                }, this);
                this.listenTo(this.searchCollection, "backgrid:sorted", function(model, response) {
                    this.checkTableFetch();
                }, this)
            },
            onRender: function() {
                this.commonTableOptions = {
                    collection: this.searchCollection,
                    includePagination: false,
                    includeFooterRecords: false,
                    includeColumnManager: (Utils.getUrlState.isRelationTab() && this.value && this.value.searchType === "basic" && !this.profileDBView ? true : false),
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
                    var value = {
                        'query': null,
                        'searchType': 'basic'
                    };
                    if (this.value) {
                        value = this.value;
                    }
                    this.updateColumnList();
                    if (this.value && this.searchTableColumns && this.searchTableColumns[this.value.relationshipName] === null) {
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
                    this.$(".relationLink").show();
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
                    if (this.value && this.value.relationshipName && this.searchTableColumns) {
                        this.searchTableColumns[this.value.relationshipName] = listOfColumns.length ? listOfColumns : null;
                    }
                } else if (this.value && this.value.relationshipName && this.searchTableColumns && this.value.attributes) {
                    this.searchTableColumns[this.value.relationshipName] = this.value.attributes.split(",");
                }
            },
            fetchCollection: function(value, options) {
                var that = this,
                    isPostMethod = (this.value && this.value.searchType === "basic"),
                    isRelationSearch = Utils.getUrlState.isRelationTab(),
                    relationshipFilters = null;
                if (isRelationSearch) {
                    relationshipFilters = CommonViewFunction.attributeFilter.generateAPIObj(this.value.relationshipFilters);
                }
                if (isPostMethod && isRelationSearch) {
                    var excludeDefaultColumn = this.value.relationshipName && this.searchTableColumns ? _.difference(this.searchTableColumns[this.value.relationshipName], this.defaultColumns) : null,
                        filterObj = {
                            'attributes': excludeDefaultColumn ? excludeDefaultColumn : null,
                            'relationshipFilters': relationshipFilters
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
                        if (isPostMethod && dataOrCollection && dataOrCollection.relations) {
                            dataLength = dataOrCollection.relations.length;
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
                            that.searchCollection.reset(dataOrCollection.relations, { silent: true });
                            that.searchCollection.fullCollection.reset(dataOrCollection.relations, { silent: true });
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

                        if (dataLength > 0) {
                            that.$('.searchTable').removeClass('noData')
                        }

                        if (isRelationSearch && value && !that.profileDBView) {
                            var searchString = 'Results for: <span class="filterQuery">' + CommonViewFunction.generateQueryOfFilter(that.value) + "</span>";
                            that.$('.searchResult').html(searchString);
                        }
                    },
                    silent: true,
                    reset: true
                }
                if (value) {
                    if (value.searchType) {
                        this.searchCollection.url = UrlLinks.relationshipSearchApiUrl(value.searchType);
                    }
                    _.extend(this.searchCollection.queryParams, { 'limit': this.limit, 'offset': this.offset, 'query': _.trim(value.query), 'relationshipName': value.relationshipName || null });
                    if (isPostMethod) {
                        this.searchCollection.filterObj = _.extend({}, filterObj);
                        apiObj['data'] = _.extend(filterObj, _.pick(this.searchCollection.queryParams, 'query', 'limit', 'offset', 'relationshipName'));
                        Globals.searchApiCallRef = this.searchCollection.getBasicRearchResult(apiObj);
                    } else {
                        apiObj.data = null;
                        this.searchCollection.filterObj = null;
                        Globals.searchApiCallRef = this.searchCollection.fetch(apiObj);
                    }
                } else {
                    _.extend(this.searchCollection.queryParams, { 'limit': this.limit, 'offset': this.offset });
                    if (isPostMethod) {
                        apiObj['data'] = _.extend(filterObj, _.pick(this.searchCollection.queryParams, 'query', 'limit', 'offset', 'relationshipName'));
                        Globals.searchApiCallRef = this.searchCollection.getBasicRearchResult(apiObj);
                    } else {
                        apiObj.data = null;
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
                if (this.value && this.value.searchType === "basic" && this.searchTableColumns && (this.searchTableColumns[this.value.relationshipName] !== undefined)) {
                    columnToShow = this.searchTableColumns[this.value.relationshipName] == null ? [] : this.searchTableColumns[this.value.relationshipName];
                }
                col['name'] = {
                    label: "Guid",
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
                                name = Utils.getName(obj),
                                img = "";
                            if (obj.guid) {
                                if (obj.guid == "-1") {
                                    nameHtml = '<span title="' + name + '">' + name + '</span>';
                                } else {
                                    nameHtml = '<a title="' + name + '" href="#!/relationshipDetailPage/' + obj.guid + (that.fromView ? "?from=" + that.fromView : "") + '">' + name + '</a>';
                                }
                            } else {
                                nameHtml = '<span title="' + name + '">' + name + '</span>';
                            }
                            img = "<div><img data-imgGuid='" + obj.guid + "' src='/img/entity-icon/table.png'></div>";
                            if (obj.status && Enums.entityStateReadOnly[obj.status]) {
                                nameHtml += '<button type="button" title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i></button>';
                                nameHtml = '<div class="readOnly readOnlyLink">' + nameHtml + '</div>';
                                img = "<div><img data-imgGuid='" + obj.guid + "' src='/img/entity-icon/disabled/table.png'></div>";
                            }
                            return (img + nameHtml);
                        }
                    })
                };
                if (this.value) {
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
                                    return '<span>' + obj.typeName + '</span>';
                                }
                            }
                        })
                    };
                    col['end1'] = {
                        label: "End1",
                        cell: "Html",
                        editable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'end1') : true),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj && obj.end1) {
                                    var key, uniqueAttributesValue;
                                    for (key in obj.end1.uniqueAttributes) {
                                        uniqueAttributesValue = obj.end1.uniqueAttributes[key];
                                    }
                                    uniqueAttributesValue  = uniqueAttributesValue ? uniqueAttributesValue : obj.end1.guid;
                                    return '<a title="' + uniqueAttributesValue + '" href="#!/detailPage/' + obj.end1.guid  + '?from=relationshipSearch">' + uniqueAttributesValue + '</a>';
                                }
                            }
                        })
                    };
                    col['end2'] = {
                        label: "End2",
                        cell: "Html",
                        editable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'end2') : true),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj && obj.end2) {
                                    var key, uniqueAttributesValue;
                                    for (key in obj.end2.uniqueAttributes) {
                                        uniqueAttributesValue = obj.end2.uniqueAttributes[key];
                                    }
                                    uniqueAttributesValue  = uniqueAttributesValue ? uniqueAttributesValue : obj.end2.guid;
                                    return '<a title="' + uniqueAttributesValue + '" href="#!/detailPage/' + obj.end2.guid  + '?from=relationshipSearch">' + uniqueAttributesValue + '</a>';
                                }
                            }
                        })
                    };
                    col['label'] = {
                        label: "Label",
                        cell: "Html",
                        editable: false,
                        resizeable: true,
                        orderable: true,
                        renderable: (columnToShow ? _.contains(columnToShow, 'end2') : true),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj) {
                                    return "<span>" + obj.label + "</span>"
                                }
                            }
                        })
                    };

                    if (this.value && this.value.searchType === "basic") {
                        var def = this.relationshipDefCollection.fullCollection.find({ name: this.value.relationshipName });
                        if (def) {
                            var attrObj = def ? Utils.getNestedSuperTypeObj({ data: def.toJSON(), collection: this.relationshipDefCollection, attrMerge: true }) : [];
                            _.each(attrObj, function(obj, key) {
                                var key = obj.name,
                                    isRenderable = _.contains(columnToShow, key),
                                    isSortable = obj.typeName.search(/(array|map)/i) == -1;
                                col[obj.name] = {
                                    label: obj.name.capitalize(),
                                    cell: "Html",
                                    headerCell: Backgrid.HeaderHTMLDecodeCell,
                                    editable: false,
                                    resizeable: true,
                                    orderable: true,
                                    sortable: isSortable,
                                    renderable: isRenderable,
                                    headerClassName: "",
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
            showLoader: function() {
                this.$('.fontLoader:not(.for-ignore)').addClass('show');
                this.$('.tableOverlay').addClass('show');
            },
            hideLoader: function(options) {
                this.$('.fontLoader:not(.for-ignore)').removeClass('show');
                options && options.type === 'error' ? this.$('.ellipsis-with-margin,.pagination-box').hide() : this.$('.ellipsis-with-margin,.pagination-box').show(); // only show for first time and hide when type is error
                this.$('.tableOverlay').removeClass('show');
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
    return RelationSearchResultLayoutView;
});