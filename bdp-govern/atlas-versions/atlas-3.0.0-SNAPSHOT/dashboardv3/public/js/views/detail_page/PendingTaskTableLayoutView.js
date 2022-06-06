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
    'hbs!tmpl/detail_page/PendingTaskTableLayoutView_tmpl',
    'collection/VEntityList',
    'utils/Utils',
    'utils/Enums',
    'utils/UrlLinks',
    'utils/CommonViewFunction'
], function(require, Backbone, PendingTaskTableLayoutView_tmpl, VEntityList, Utils, Enums, UrlLinks, CommonViewFunction) {
    'use strict';

    var PendingTaskTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends PendingTaskTableLayoutView */
        {
            _viewName: 'PendingTaskTableLayoutView',

            template: PendingTaskTableLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RPendingTaskTableLayoutView: "#r_pendingTaskTableLayoutView",
            },

            /** ui selector cache */
            ui: {
                refreshPendingTask: "[data-id='refreshPendingTask']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.refreshPendingTask] = function(e) {
                    this.fetchPendingTaskCollection();
                };
                return events;
            },
            /**
             * intialize a new PendingTaskTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'entity', 'entityName', 'attributeDefs'));
                this.pendingTaskCollection = new VEntityList();
                this.limit = 25;
                this.offset = 0;
                this.pendingTaskCollection.url = UrlLinks.pendingTaskApiUrl();
                this.entityModel = new this.pendingTaskCollection.model();
                this.pervOld = [];
                this.commonTableOptions = {
                    collection: this.pendingTaskCollection,
                    includeFilter: false,
                    includePagination: false,
                    includeAtlasPagination: true,
                    includeAtlasPageSize: true,
                    includeTableLoader: true,
                    includeAtlasTableSorting: false,
                    showDefaultTableSorted: false,
                    columnSorting: false,
                    includeFooterRecords: false,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    isApiSorting: false,
                    atlasPaginationOpts: this.getPaginationOptions(),
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.currPage = 1;
                this.fromSort = false;
            },
            onRender: function() {
                this.fetchPendingTaskCollection();
            },
            fetchPendingTaskCollection: function() {
                this.commonTableOptions['atlasPaginationOpts'] = this.getPaginationOptions();
                this.fetchCollection();
                this.pendingTaskCollection.comparator = function(model) {
                    return -model.get('createdBy');
                }
            },
            bindEvents: function() {},
            getPaginationOptions: function() {
                return {
                    count: this.getPageCount(),
                    offset: this.pendingTaskCollection.queryParams.offset || this.offset,
                    fetchCollection: this.fetchCollection.bind(this)
                };
            },
            getPageCount: function() {
                return (this.pendingTaskCollection.queryParams.limit || this.pendingTaskCollection.queryParams.count) || this.limit;
            },
            fetchCollection: function(options) {
                var that = this;

                this.pendingTaskCollection.fetch({
                    success: function(dataOrCollection, response) {
                        that.pendingTaskCollection.state.pageSize = that.getPageCount();
                        that.pendingTaskCollection.fullCollection.reset(response);
                    },
                    complete: function() {
                        that.$('.fontLoader').hide();
                        that.$('.tableOverlay').hide();
                        that.$('.auditTable').show();
                        that.renderTableLayoutView();
                    },
                    silent: true
                });

            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getAuditTableColumns());
                    that.RPendingTaskTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));

                });
            },
            getAuditTableColumns: function() {
                var that = this;
                return this.pendingTaskCollection.constructor.getTableCols({
                    tool: {
                        label: "",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        fixWidth: "20",
                        cell: Backgrid.ExpandableCell,
                        accordion: false,
                        expand: function(el, model) {
                            el.attr('colspan', '8');
                            var count = model.get('attemptCount'),
                                parameters = _.omit(_.extend(model.get('parameters'), { 'attemptCount': model.get('attemptCount'), 'createdBy': model.get('createdBy') }),"entityGuid"),
                                memoryTable = CommonViewFunction.propertyTable({
                                    scope: this,
                                    formatStringVal: false,
                                    valueObject: parameters
                                }),
                                tableData = ' <div class="col-sm-12"> <div class="card-container panel  panel-default custom-panel">' +
                                '<div class="panel-heading">Parameters</div> <div class="panel-body">' +
                                '<table class="table stat-table task-details">' +
                                '<tbody data-id="memory-card">' +
                                memoryTable +
                                '</tbody>' +
                                '</table> </div> </div> </div>';
                            $(el).append($('<div>').html(tableData));
                        }
                    },
                    type: {
                        label: "Type",
                        cell: "html",
                        sortable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Enums.auditAction[model.get('type')] || rawValue;
                            }
                        })
                    },
                    guid: {
                        label: "Guid",
                        cell: "html",
                        sortable: false,
                        editable: false
                    },
                    status: {
                        label: "Status",
                        cell: "html",
                        sortable: false,
                        editable: false
                    },
                    createdTime: {
                        label: "Created Time",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: rawValue });
                            }
                        })
                    },
                    updatedTime: {
                        label: "Updated Time",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: rawValue });
                            }
                        })
                    }
                }, this.pendingTaskCollection);

            }
        });
    return PendingTaskTableLayoutView;
});