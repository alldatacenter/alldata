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

/**
 * @file This is the common View file for displaying Table/Grid to be used overall in the application.
 */
define(['require',
    'backbone',
    'hbs!tmpl/common/TableLayout_tmpl',
    'utils/Messages',
    'utils/Utils',
    'utils/Globals',
    'utils/CommonViewFunction',
    'backgrid-filter',
    'backgrid-paginator',
    'backgrid-sizeable',
    'backgrid-orderable',
    'backgrid-select-all',
    'backgrid-columnmanager'
], function(require, Backbone, FSTablelayoutTmpl, Messages, Utils, Globals, CommonViewFunction) {
    'use strict';

    var FSTableLayout = Backbone.Marionette.LayoutView.extend(
        /** @lends FSTableLayout */
        {
            _viewName: 'FSTableLayout',

            template: FSTablelayoutTmpl,
            templateHelpers: function() {
                return this.options;
            },

            /** Layout sub regions */
            regions: {
                'rTableList': 'div[data-id="r_tableList"]',
                'rTableSpinner': 'div[data-id="r_tableSpinner"]',
                'rPagination': 'div[data-id="r_pagination"]',
                'rFooterRecords': 'div[data-id="r_footerRecords"]'
            },

            // /** ui selector cache */
            ui: {
                selectPageSize: 'select[data-id="pageSize"]',
                paginationDiv: '[data-id="paginationDiv"]',
                previousData: "[data-id='previousData']",
                nextData: "[data-id='nextData']",
                pageRecordText: "[data-id='pageRecordText']",
                showPage: "[data-id='showPage']",
                gotoPage: "[data-id='gotoPage']",
                gotoPagebtn: "[data-id='gotoPagebtn']",
                activePage: "[data-id='activePage']"
            },

            gridOpts: {
                className: 'table table-bordered table-hover table-condensed backgrid',
                emptyText: 'No Records found!'
            },

            /**
             * Backgrid.Filter default options
             */
            filterOpts: {
                placeholder: 'plcHldr.searchByResourcePath',
                wait: 150
            },

            /**
             * Paginator default options
             */
            paginatorOpts: {
                // If you anticipate a large number of pages, you can adjust
                // the number of page handles to show. The sliding window
                // will automatically show the next set of page handles when
                // you click next at the end of a window.
                windowSize: 5, // Default is 10

                // Used to multiple windowSize to yield a number of pages to slide,
                // in the case the number is 5
                slideScale: 0.5, // Default is 0.5

                // Whether sorting should go back to the first page
                goBackFirstOnSort: false // Default is true
            },

            /**
               page handlers for pagination
            */
            controlOpts: {
                rewind: null,
                back: {
                    label: "<i class='fa fa-angle-left'></i>",
                    title: "Previous"
                },
                forward: {
                    label: "<i class='fa fa-angle-right'></i>",
                    title: "Next"
                },
                fastForward: null
            },
            columnOpts: {
                opts: {
                    initialColumnsVisible: 4,
                    // State settings
                    saveState: false,
                    loadStateOnInit: true
                },
                visibilityControlOpts: {},
                el: null
            },

            includePagination: true,

            includeAtlasPagination: false,

            includeAtlasPageSize: false,

            includeFilter: false,

            includeHeaderSearch: false,

            includePageSize: false,

            includeGotoPage: false,

            includeFooterRecords: true,

            includeColumnManager: false,

            includeSizeAbleColumns: false,

            includeOrderAbleColumns: false,

            includeTableLoader: true,

            includeAtlasTableSorting: false,

            showDefaultTableSorted: false,

            /**
             * [updateFullCollectionManually  If collection was updated using silent true
             * then need to update FullCollection Manually for correct sorting experience]
             * @type {Boolean}
             */
            updateFullCollectionManually: false,

            sortOpts: {
                sortColumn: "name",
                sortDirection: "ascending"
            },


            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events['change ' + this.ui.selectPageSize] = 'onPageSizeChange';
                events['change ' + this.ui.showPage] = 'changePageLimit';
                events["click " + this.ui.nextData] = "onClicknextData";
                events["click " + this.ui.previousData] = "onClickpreviousData";
                events["click " + this.ui.gotoPagebtn] = 'gotoPagebtn';
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
                return events;
            },

            /**
             * intialize a new HDFSTableLayout Layout
             * @constructs
             */
            initialize: function(options) {
                this.limit = 25;
                this.offset = 0;
                _.extend(this, _.omit(options, 'gridOpts', 'sortOpts', 'atlasPaginationOpts'));
                _.extend(this, options.atlasPaginationOpts);
                _.extend(this.gridOpts, options.gridOpts, { collection: this.collection, columns: this.columns });
                _.extend(this.sortOpts, options.sortOpts);
                if (this.isApiSorting) {
                    //after audit sorting pagination values
                    if (this.offset === 0) {
                        this.limit = this.count || this.limit;
                    }
                }
                if (this.includeAtlasTableSorting) {
                    var oldSortingRef = this.collection.setSorting;
                    this.collection.setSorting = function() {
                        this.state.pageSize = this.length
                        var val = oldSortingRef.apply(this, arguments);
                        val.fullCollection.sort();
                        this.comparator = function(next, previous, data) {
                            var getValue = function(options) {
                                    var next = options.next,
                                        previous = options.previous,
                                        order = options.order;
                                    if (next === previous) {
                                        return null;
                                    } else {
                                        if (order === -1) {
                                            return next < previous ? -1 : 1;
                                        } else {
                                            return next < previous ? 1 : -1;
                                        }
                                    }
                                },
                                getKeyVal = function(model, key) {
                                    //for nested obj
                                    var value = null;
                                    if (model && key) {
                                        value = model[key];
                                        if (!value) {
                                            _.each(model, function(modalValue) {
                                                if (typeof(modalValue) == "object") {
                                                    if (!value) {
                                                        value = getKeyVal(modalValue, key);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                    return Number(value) || value;
                                };
                            if (val.state && (!_.isNull(val.state.sortKey))) {
                                var nextValue,
                                    previousValue;
                                if ((next && next.get("attributes") && next.get("attributes")[val.state.sortKey]) || (previous && previous.get("attributes") && previous.get("attributes")[val.state.sortKey])) {
                                    nextValue = next.get("attributes")[val.state.sortKey];
                                    previousValue = previous.get("attributes")[val.state.sortKey];
                                } else {
                                    nextValue = getKeyVal(next.attributes, val.state.sortKey);
                                    previousValue = getKeyVal(previous.attributes, val.state.sortKey);
                                }
                                nextValue = (typeof nextValue === 'string') ? nextValue.toLowerCase() : nextValue;
                                previousValue = (typeof previousValue === 'string') ? previousValue.toLowerCase() : previousValue;
                                return getValue({
                                    "next": nextValue || '',
                                    "previous": previousValue || '',
                                    "order": val.state.order
                                });
                            }
                        }
                        return val;
                    };
                }
                this.bindEvents();
            },
            /** all events binding here */
            bindEvents: function() {
                this.listenTo(this.collection, 'request', function() {
                    this.$('div[data-id="r_tableSpinner"]').addClass('show');
                }, this);
                this.listenTo(this.collection, 'sync error', function() {
                    this.$('div[data-id="r_tableSpinner"]').removeClass('show');
                }, this);

                this.listenTo(this.collection, 'reset', function(collection, options) {
                    this.$('div[data-id="r_tableSpinner"]').removeClass('show');
                    this.ui.gotoPage.val('');
                    this.ui.gotoPage.parent().removeClass('has-error');
                    this.ui.gotoPagebtn.prop("disabled", true);
                    if (this.includePagination) {
                        this.renderPagination();
                    }
                    if (this.includeFooterRecords) {
                        this.renderFooterRecords(this.collection.state);
                    }
                    if (this.includeAtlasPagination) {
                        this.renderAtlasPagination(options);
                    }
                }, this);

                /*This "sort" trigger event is fired when clicked on
                'sortable' header cell (backgrid).
                Collection.trigger event was fired because backgrid has
                removeCellDirection function (backgrid.js - line no: 2088)
                which is invoked when "sort" event is triggered
                on collection (backgrid.js - line no: 2081).
                removeCellDirection function - removes "ascending" and "descending"
                which in turn removes chevrons from every 'sortable' header-cells*/
                this.listenTo(this.collection, "backgrid:sorted", function(column, direction, collection) {
                    // backgrid:sorted fullCollection trigger required for icon chage
                    if (this.isApiSorting) {
                        column.set("direction", direction);
                    } else {
                        this.collection.fullCollection.trigger("backgrid:sorted", column, direction, collection)
                        if (this.includeAtlasTableSorting && this.updateFullCollectionManually) {
                            this.collection.fullCollection.reset(collection.toJSON(), { silent: true });
                        }
                    }
                }, this);
                this.listenTo(this, "grid:refresh", function() {
                    if (this.grid) {
                        this.grid.trigger("backgrid:refresh");
                    }
                });
                this.listenTo(this, "grid:refresh:update", function() {
                    if (this.grid) {
                        this.grid.trigger("backgrid:refresh");
                        if (this.grid.collection) { this.grid.collection.trigger("backgrid:colgroup:updated"); }
                    }
                });

                // It will show tool tip when td has ellipsis  Property
                this.listenTo(this.collection, "backgrid:refresh", function() {
                    /*this.$('.table td').bind('mouseenter', function() {
                        var $this = $(this);
                        if (this.offsetWidth < this.scrollWidth && !$this.attr('title')) {
                            $this.attr('title', $this.text());
                        }
                    });*/
                }, this);

            },

            /** on render callback */
            onRender: function() {
                this.renderTable();
                if (this.includePagination) {
                    this.renderPagination();
                }
                if (this.includeAtlasPagination) {
                    this.renderAtlasPagination();
                }
                if (this.includeFilter) {
                    this.renderFilter();
                }
                if (this.includeFooterRecords) {
                    this.renderFooterRecords(this.collection.state);
                }
                if (this.includeColumnManager) {
                    this.renderColumnManager();
                }
                var pageSizeEl = null;
                if (this.includePageSize) {
                    pageSizeEl = this.ui.selectPageSize;
                } else if (this.includeAtlasPageSize) {
                    pageSizeEl = this.ui.showPage;
                }
                if (pageSizeEl) {
                    pageSizeEl.select2({
                        data: _.sortBy(_.union([25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500], [this.collection.state.pageSize])),
                        tags: true,
                        dropdownCssClass: "number-input",
                        multiple: false
                    });
                    var val = this.collection.state.pageSize;
                    if (this.value && this.value.pageLimit) {
                        val = this.limit;
                    }
                    pageSizeEl.val(val).trigger('change', { "skipViewChange": true });
                }

            },
            /**
             * show table
             */
            renderTable: function() {
                var that = this;
                this.grid = new Backgrid.Grid(this.gridOpts).on('backgrid:rendered', function() {
                    that.trigger('backgrid:manual:rendered', this)
                });
                if (this.showDefaultTableSorted) {
                    this.grid.render();
                    if (this.collection.fullCollection.length > 1 || this.isApiSorting) {
                        this.grid.sort(this.sortOpts.sortColumn, this.sortOpts.sortDirection);
                    }
                    this.rTableList.show(this.grid);
                } else {
                    this.rTableList.show(this.grid);
                }
            },
            onShow: function() {
                if (this.includeSizeAbleColumns) {
                    this.renderSizeAbleColumns();
                }
                if (this.includeOrderAbleColumns) {
                    this.renderOrderAbleColumns();
                }
            },
            /**
             * show pagination buttons(first, last, next, prev and numbers)
             */
            renderPagination: function() {
                var options = _.extend({
                    collection: this.collection,
                    controls: this.controlOpts
                }, this.paginatorOpts);

                // TODO - Debug this part
                if (this.rPagination) {
                    this.rPagination.show(new Backgrid.Extension.Paginator(options));
                } else if (this.regions.rPagination) {
                    this.$('div[data-id="r_pagination"]').show(new Backgrid.Extension.Paginator(options));
                    this.showHideGoToPage();
                }
            },

            renderAtlasPagination: function(options) {
                var isFirstPage = this.offset === 0,
                    dataLength = this.collection.length,
                    goToPage = this.ui.gotoPage.val();

                /*Next button check.
                It's outside of Previous button else condition
                because when user comes from 2 page to 1 page than we need to check next button.*/
                if (dataLength < this.limit) {
                    this.ui.nextData.attr('disabled', true);
                } else {
                    this.ui.nextData.attr('disabled', false);
                }

                if (isFirstPage && (!dataLength || dataLength < this.limit)) {
                    this.ui.paginationDiv.hide();
                } else {
                    this.ui.paginationDiv.show();
                }

                // Previous button check.s
                if (isFirstPage) {
                    this.ui.previousData.attr('disabled', true);
                    this.pageFrom = 1;
                    this.pageTo = this.limit;
                } else {
                    this.ui.previousData.attr('disabled', false);
                }

                if (options && options.next) {
                    //on next click, adding "1" for showing the another records.
                    this.pageTo = this.offset + this.limit;
                    this.pageFrom = this.offset + 1;
                } else if (!isFirstPage && options && options.previous) {
                    this.pageTo = this.pageTo - this.limit;
                    this.pageFrom = (this.pageTo - this.limit) + 1;
                }
                if (this.isApiSorting && !this.pageTo && !this.pageFrom) {
                    this.limit = this.count;
                    this.pageTo = (this.offset + this.limit);
                    this.pageFrom = this.offset + 1;
                }
                this.ui.pageRecordText.html("Showing  <u>" + this.collection.length + " records</u> From " + this.pageFrom + " - " + this.pageTo);
                this.activePage = Math.round(this.pageTo / this.limit);
                this.ui.activePage.attr('title', "Page " + this.activePage);
                this.ui.activePage.text(this.activePage);
                this.ui.showPage.val(this.limit).trigger('change', { "skipViewChange": true });

                if (!dataLength && this.offset >= this.limit && ((options && options.next) || goToPage) && (options && !options.fromUrl)) {
                    /* User clicks on next button and server returns
                    empty response then disabled the next button without rendering table*/

                    var pageNumber = this.activePage;
                    if (goToPage) {
                        pageNumber = goToPage;
                        this.offset = (this.activePage - 1) * this.limit;
                    } else {
                        this.ui.nextData.attr('disabled', true);
                    }
                    if (this.value) {
                        this.value.pageOffset = this.offset;
                        if (this.triggerUrl) {
                            this.triggerUrl();
                        }
                    }
                    Utils.notifyInfo({
                        html: true,
                        content: Messages.search.noRecordForPage + '<b>' + Utils.getNumberSuffix({ number: pageNumber, sup: true }) + '</b> page'
                    });
                    return;
                }
            },

            /**
             * show/hide pagination buttons of the grid
             */
            showHidePager: function() {

                if (!this.includePagination) {
                    return;
                }

                if (this.collection.state && this.collection.state.totalRecords > this.collection.state.pageSize) {
                    this.$('div[data-id="r_pagination"]').show();
                } else {
                    this.$('div[data-id="r_pagination"]').hide();
                }
            },

            showHideGoToPage: function() {
                if (this.collection.state.pageSize > this.collection.fullCollection.length) {
                    this.ui.paginationDiv.hide();
                } else {
                    this.ui.paginationDiv.show();
                }
            },

            /**
             * show/hide filter of the grid
             */
            renderFilter: function() {
                this.rFilter.show(new Backgrid.Extension.ServerSideFilter({
                    collection: this.collection,
                    name: ['name'],
                    placeholder: 'plcHldr.searchByResourcePath',
                    wait: 150
                }));

                setTimeout(function() {
                    that.$('table').colResizable({ liveDrag: true });
                }, 0);
            },

            /**
             * show/hide footer details of the list/collection shown in the grid
             */
            renderFooterRecords: function(collectionState) {
                var collState = collectionState;
                var totalRecords = collState.totalRecords || 0;
                var pageStartIndex = totalRecords ? (collState.currentPage * collState.pageSize) : 0;
                var pageEndIndex = pageStartIndex + this.collection.length;
                this.$('[data-id="r_footerRecords"]').html('<h5>Showing ' + (totalRecords ? pageStartIndex + 1 : (this.collection.length === 0) ? 0 : 1) + ' - ' + pageEndIndex + '</h5>');
                return this;
            },
            /**
             * ColumnManager for the table
             */
            renderColumnManager: function() {
                if (!this.columns) {
                    return;
                }
                var that = this,
                    $el = this.columnOpts.el || this.$("[data-id='control']"),
                    colManager = new Backgrid.Extension.ColumnManager(this.columns, this.columnOpts.opts),
                    // Add control
                    colVisibilityControl = new Backgrid.Extension.ColumnManagerVisibilityControl(_.extend({
                        columnManager: colManager,
                    }, this.columnOpts.visibilityControlOpts));
                if (!$el.jquery) {
                    $el = $($el)
                }
                if (this.columnOpts.el) {
                    $el.html(colVisibilityControl.render().el);
                } else {
                    $el.append(colVisibilityControl.render().el);
                }

                colManager.on("state-changed", function(state) {
                    that.collection.trigger("state-changed", state);
                });
                colManager.on("state-saved", function() {
                    that.collection.trigger("state-changed");
                });
            },
            renderSizeAbleColumns: function() {
                // Add sizeable columns
                var that = this,
                    sizeAbleCol = new Backgrid.Extension.SizeAbleColumns({
                        collection: this.collection,
                        columns: this.columns,
                        grid: this.getGridObj()
                    });
                this.$('thead').before(sizeAbleCol.render().el);

                // Add resize handlers
                var sizeHandler = new Backgrid.Extension.SizeAbleColumnsHandlers({
                    sizeAbleColumns: sizeAbleCol,
                    // grid: this.getGridObj(),
                    saveModelWidth: true
                });
                this.$('thead').before(sizeHandler.render().el);

                // Listen to resize events
                this.columns.on('resize', function(columnModel, newWidth, oldWidth) {
                    console.log('Resize event on column; name, model, new and old width: ', columnModel.get("name"), columnModel, newWidth, oldWidth);
                });
            },

            renderOrderAbleColumns: function() {
                // Add orderable columns
                var sizeAbleCol = new Backgrid.Extension.SizeAbleColumns({
                    collection: this.collection,
                    grid: this.getGridObj(),
                    columns: this.columns
                });
                this.$('thead').before(sizeAbleCol.render().el);
                var orderHandler = new Backgrid.Extension.OrderableColumns({
                    grid: this.getGridObj(),
                    sizeAbleColumns: sizeAbleCol
                });
                this.$('thead').before(orderHandler.render().el);
            },

            /** on close */
            onClose: function() {},

            /**
             * get the Backgrid object
             * @return {null}
             */
            getGridObj: function() {
                if (this.rTableList.currentView) {
                    return this.rTableList.currentView;
                }
                return null;
            },

            /**
             * handle change event on page size select box
             * @param  {Object} e event
             */
            onPageSizeChange: function(e, options) {
                if (!options || !options.skipViewChange) {
                    var pagesize = $(e.currentTarget).val();
                    if (pagesize == 0) {
                        this.ui.selectPageSize.data('select2').$container.addClass('has-error');
                        return;
                    } else {
                        this.ui.selectPageSize.data('select2').$container.removeClass('has-error');
                    }
                    this.collection.state.pageSize = parseInt(pagesize, 10);
                    this.collection.state.currentPage = this.collection.state.firstPage;
                    this.showHideGoToPage();
                    if (this.collection.mode == "client") {
                        this.collection.fullCollection.reset(this.collection.fullCollection.toJSON());
                    } else {
                        this.collection.fetch({
                            sort: false,
                            reset: true,
                            cache: false
                        });
                    }
                }
            },
            /**
                atlasNextBtn
            **/
            onClicknextData: function() {
                this.offset = this.offset + this.limit;
                _.extend(this.collection.queryParams, {
                    offset: this.offset
                });
                if (this.value) {
                    this.value.pageOffset = this.offset;
                    if (this.triggerUrl) {
                        this.triggerUrl();
                    }
                }
                if (this.fetchCollection) {
                    this.fetchCollection({
                        next: true
                    });
                }
            },
            /**
                atlasPrevBtn
            **/
            onClickpreviousData: function() {
                this.offset = this.offset - this.limit;
                if (this.offset <= -1) {
                    this.offset = 0;
                }
                _.extend(this.collection.queryParams, {
                    offset: this.offset
                });
                if (this.value) {
                    this.value.pageOffset = this.offset;
                    if (this.triggerUrl) {
                        this.triggerUrl();
                    }
                }
                if (this.fetchCollection) {
                    this.fetchCollection({
                        previous: true
                    });
                }
            },
            /**
                atlasPageLimit
            **/
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
                        if (this.triggerUrl) {
                            this.triggerUrl();
                        }
                    }
                    _.extend(this.collection.queryParams, { limit: this.limit, offset: this.offset });
                    this.fetchCollection();
                }
            },
            /**
                atlasGotoBtn & Local Goto Btn
            **/
            gotoPagebtn: function(e) {
                var that = this;
                var goToPage = parseInt(this.ui.gotoPage.val());
                if (!_.isNaN(goToPage) && ((goToPage == 0) || (this.collection.state.totalPages < goToPage))) {
                    Utils.notifyInfo({
                        content: Messages.search.noRecordForPage + "page " + goToPage
                    });
                    this.ui.gotoPage.val('')
                    that.ui.gotoPagebtn.attr('disabled', true);
                    return;
                }
                if (!(_.isNaN(goToPage) || goToPage <= -1)) {
                    if (this.collection.mode == "client") {
                        return this.collection.getPage((goToPage - 1), {
                            reset: true
                        });
                    } else {
                        this.offset = (goToPage - 1) * this.limit;
                        if (this.offset <= -1) {
                            this.offset = 0;
                        }
                        _.extend(this.collection.queryParams, { limit: this.limit, offset: this.offset });
                        if (this.offset == (this.pageFrom - 1)) {
                            Utils.notifyInfo({
                                content: Messages.search.onSamePage
                            });
                        } else {
                            if (this.value) {
                                this.value.pageOffset = this.offset;
                                if (this.triggerUrl) {
                                    this.triggerUrl();
                                }
                            }
                            // this.offset is updated in gotoPagebtn function so use next button calculation.
                            if (this.fetchCollection) {
                                this.fetchCollection({ 'next': true });
                            }
                        }
                    }
                }
            }
        });

    return FSTableLayout;
});