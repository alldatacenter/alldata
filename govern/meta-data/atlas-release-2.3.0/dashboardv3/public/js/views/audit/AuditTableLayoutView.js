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
    'hbs!tmpl/audit/AuditTableLayoutView_tmpl',
    'collection/VEntityList',
    'utils/Utils',
    'utils/Enums',
    'utils/UrlLinks'
], function(require, Backbone, AuditTableLayoutView_tmpl, VEntityList, Utils, Enums, UrlLinks) {
    'use strict';

    var AuditTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends AuditTableLayoutView */
        {
            _viewName: 'AuditTableLayoutView',

            template: AuditTableLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RAuditTableLayoutView: "#r_auditTableLayoutView",
            },

            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new AuditTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'entity', 'entityName', 'attributeDefs'));
                this.entityCollection = new VEntityList();
                this.limit = 25;
                this.offset = 0;
                this.entityCollection.url = UrlLinks.entityCollectionaudit(this.guid);
                this.entityCollection.modelAttrName = "events";
                this.entityModel = new this.entityCollection.model();
                this.pervOld = [];
                this.commonTableOptions = {
                    collection: this.entityCollection,
                    includeFilter: false,
                    includePagination: false,
                    includeAtlasPagination: true,
                    includeAtlasPageSize: true,
                    includeTableLoader: true,
                    includeAtlasTableSorting: false,
                    showDefaultTableSorted: true,
                    columnSorting: false,
                    includeFooterRecords: false,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    sortOpts: {
                        sortColumn: "timestamp",
                        sortDirection: "descending"
                    },
                    isApiSorting: true,
                    atlasPaginationOpts: this.getPaginationOptions(),
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.currPage = 1;
                this.fromSort = false;
            },
            onRender: function() {
                $.extend(this.entityCollection.queryParams, { offset: this.offset, count: this.limit, sortKey: null, order: "sortOrder", sortBy: "timestamp", sortOrder: "desc" });
                this.fetchAuditCollection();
                this.renderTableLayoutView();
            },
            fetchAuditCollection: function() {
                this.commonTableOptions['atlasPaginationOpts'] = this.getPaginationOptions();
                this.fetchCollection();
            },
            bindEvents: function() {},
            getPaginationOptions: function() {
                return {
                    count: this.getPageCount(),
                    offset: this.entityCollection.queryParams.offset || this.offset,
                    fetchCollection: this.fetchCollection.bind(this)
                };
            },
            getPageCount: function() {
                return (this.entityCollection.queryParams.limit || this.entityCollection.queryParams.count) || this.limit;
            },
            fetchCollection: function(options) {
                var that = this;
                this.$('.fontLoader').show();
                this.$('.tableOverlay').show();
                //api needs only sortOrder,count,offset, sortBy queryparams & removed extra queryparams limit,sort_by, order
                this.entityCollection.queryParams.count = this.getPageCount();
                this.entityCollection.queryParams = _.omit(this.entityCollection.queryParams, 'limit');
                this.entityCollection.fetch({
                    success: function(dataOrCollection, response) {
                        that.entityCollection.state.pageSize = that.getPageCount();
                        that.entityCollection.reset(response, $.extend(options));
                    },
                    complete: function() {
                        that.$('.fontLoader').hide();
                        that.$('.tableOverlay').hide();
                        that.$('.auditTable').show();
                        if (that.fromSort) {
                            that.fromSort = !that.fromSort;
                        }
                    },
                    silent: true
                });
            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getAuditTableColumns());
                    that.RAuditTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));

                });
            },
            backgridHeaderClickHandel: function() {
                var that = this;
                return Backgrid.HeaderCell.extend({
                    onClick: function(e) {
                        e.preventDefault();
                        var column = this.column,
                            direction = "ascending",
                            columnName = column.get("name").toLocaleLowerCase();
                        if (column.get("direction") === "ascending") direction = "descending";
                        column.set("direction", direction);
                        var options = {
                            sortBy: columnName,
                            sortOrder: (direction === "ascending") ? "asc" : "desc",
                            offset: that.entityCollection.queryParams.offset || that.offset,
                            count: that.getPageCount()
                        };
                        that.commonTableOptions['sortOpts'] = {
                            sortColumn: columnName,
                            sortDirection: (direction === "ascending") ? "ascending" : "descending"
                        };
                        $.extend(that.entityCollection.queryParams, options);
                        that.fromSort = true;
                        that.fetchAuditCollection();
                    }
                });
            },
            getAuditTableColumns: function() {
                var that = this;
                return this.entityCollection.constructor.getTableCols({
                    tool: {
                        label: "",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        fixWidth: "20",
                        cell: Backgrid.ExpandableCell,
                        accordion: false,
                        expand: function(el, model) {
                            el.attr('colspan', '4');
                            require([
                                'views/audit/CreateAuditTableLayoutView',
                            ], function(CreateAuditTableLayoutView) {
                                that.action = model.get('action');
                                var eventModel = that.entityCollection.fullCollection.findWhere({ 'eventKey': model.get('eventKey') }).toJSON(),
                                    collectionModel = new that.entityCollection.model(eventModel),
                                    view = new CreateAuditTableLayoutView({ guid: that.guid, entityModel: collectionModel, action: that.action, entity: that.entity, entityName: that.entityName, attributeDefs: that.attributeDefs });
                                view.render();
                                $(el).append($('<div>').html(view.$el));
                            });
                        }
                    },
                    user: {
                        label: "Users",
                        cell: "html",
                        editable: false,
                        headerCell: that.backgridHeaderClickHandel()
                    },
                    timestamp: {
                        label: "Timestamp",
                        cell: "html",
                        editable: false,
                        headerCell: that.backgridHeaderClickHandel(),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: rawValue });
                            }
                        })
                    },
                    action: {
                        label: "Actions",
                        cell: "html",
                        editable: false,
                        headerCell: that.backgridHeaderClickHandel(),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                if (Enums.auditAction[rawValue]) {
                                    return Enums.auditAction[rawValue];
                                } else {
                                    return rawValue;
                                }
                            }
                        })
                    }
                }, this.entityCollection);

            }
        });
    return AuditTableLayoutView;
});