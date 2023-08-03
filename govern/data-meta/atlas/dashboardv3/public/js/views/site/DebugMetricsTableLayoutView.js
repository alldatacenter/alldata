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
    'hbs!tmpl/site/DebugMetricsTableLayoutView_tmpl',
    'utils/UrlLinks',
    'collection/VEntityList',
    'utils/CommonViewFunction',
    'utils/Utils'
], function(require, Backbone, DebugMetricsTableLayoutViewTmpl, UrlLinks, VEntityList, CommonViewFunction, Utils) {
    'use strict';

    var DebugMetricsTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends DebugMetricsTableLayoutView */
        {
            _viewName: 'DebugMetricsTableLayoutView',
            template: DebugMetricsTableLayoutViewTmpl,
            /** Layout sub regions */
            regions: {
                RDebugMetricsTableLayoutView: "#r_debugMetricsTableLayoutView"
            },
            /** ui selector cache */
            ui: {
                refreshMetricsBtn: '[data-id="refreshMetricsBtn"]',
                metricsInfoBtn: '[data-id="metricsInfo"]'
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.refreshMetricsBtn] = function(e) {
                    this.currPage = 1;
                    this.limit = 26;
                    this.debugMetricsCollection.state.pageSize = 25;
                    this.debugMetricsCollection.state.currentPage = 0;
                    this.fetchMetricData();
                };
                events["click " + this.ui.metricsInfoBtn] = 'metricsInfo';
                return events;
            },
            /**
             * intialize a new DebugMetricsTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, options);
                var that = this;
                this.DATA_MAX_LENGTH = 25;
                this.debugMetricsCollection = new VEntityList();
                this.debugMetricsCollection.url = UrlLinks.debugMetricsApiUrl();
                this.debugMetricsCollection.modelAttrName = "data";
                this.commonTableOptions = {
                    collection: this.debugMetricsCollection,
                    includeFilter: false,
                    includePagination: true,
                    includeFooterRecords: true,
                    includePageSize: true,
                    includeGotoPage: true,
                    includeAtlasTableSorting: true,
                    includeTableLoader: true,
                    includeColumnManager: false,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.currPage = 1;
                this.limit = 26;
            },
            bindEvents: function() {
                var that = this;
            },
            onRender: function() {
                this.bindEvents();
                this.$('.debug-metrics-table').show();
                this.fetchMetricData();
            },
            metricsInfo: function(e) {
                require([
                    'views/site/MetricsUIInfoView',
                    'modules/Modal'
                ], function(MetricsUIInfoView, Modal) {
                    var view = new MetricsUIInfoView();
                    var modal = new Modal({
                        title: 'Debug Metrics',
                        content: view,
                        okCloses: true,
                        showFooter: false,
                        allowCancel: false
                    }).open();
                    view.on('closeModal', function() {
                        modal.trigger('cancel');
                    });
                });
            },
            fetchMetricData: function(options) {
                var that = this;
                this.debugMetricsCollection.fetch({
                    success: function(data) {
                        var data = _.first(data.toJSON()),
                            metricsDataKeys = data ? Object.keys(data) : null;
                        that.debugMetricsCollection.fullCollection.reset();
                        _.each(metricsDataKeys.sort(), function(keyName) {
                            that.debugMetricsCollection.fullCollection.add(data[keyName]);
                        });
                    },
                    complete: function(data) {
                        that.renderTableLayoutView();
                    }
                });
            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getAuditTableColumns());
                    that.RDebugMetricsTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));
                    if (!(that.debugMetricsCollection.models.length < that.limit)) {
                        that.RDebugMetricsTableLayoutView.$el.find('table tr').last().hide();
                    }
                });
            },
            millisecondsToSeconds: function(rawValue) {
                return parseFloat((rawValue % 60000) / 1000).toFixed(3);
            },
            getAuditTableColumns: function() {
                var that = this;
                return this.debugMetricsCollection.constructor.getTableCols({
                    name: {
                        label: "Name",
                        cell: "html",
                        sortable: true,
                        editable: false
                    },
                    numops: {
                        label: "Count",
                        cell: "html",
                        toolTip: "Number of times the API has been hit since Atlas started",
                        sortable: true,
                        editable: false
                    },
                    minTime: {
                        label: "Min Time (secs)",
                        cell: "html",
                        toolTip: "Minimum API execution time since Atlas started",
                        sortable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return that.millisecondsToSeconds(rawValue);
                            }
                        })
                    },
                    maxTime: {
                        label: "Max Time (secs)",
                        cell: "html",
                        toolTip: "Maximum API execution time since Atlas started",
                        sortable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return that.millisecondsToSeconds(rawValue);
                            }
                        })
                    },
                    avgTime: {
                        label: "Average Time (secs)",
                        cell: "html",
                        toolTip: "Average time taken to execute by an API within an interval of time",
                        sortable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return that.millisecondsToSeconds(rawValue);
                            }
                        })
                    }
                }, this.debugMetricsCollection);

            }
        });
    return DebugMetricsTableLayoutView;
});