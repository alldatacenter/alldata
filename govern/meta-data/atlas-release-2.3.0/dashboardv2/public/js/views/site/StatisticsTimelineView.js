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
    'views/site/d3.v3.min',
    'hbs!tmpl/site/Statistics_Timeline_tmlp',
    'hbs!tmpl/site/Statistics_Notification_table_tmpl',
    'hbs!tmpl/site/Statistics_Topic_Offset_table_tmpl',
    'hbs!tmpl/site/entity_tmpl',
    'modules/Modal',
    'models/VCommon',
    'utils/UrlLinks',
    'collection/VTagList',
    'utils/CommonViewFunction',
    'utils/Enums',
    'utils/MigrationEnums',
    'moment',
    'utils/Utils',
    'utils/Globals',
    'views/site/nv.d3',
    'moment-timezone',
    'daterangepicker'
], function(require, Backbone, d3, StatTimelineTmpl, StatsNotiTable, TopicOffsetTable, EntityTable, Modal, VCommon, UrlLinks, VTagList, CommonViewFunction, Enums, MigrationEnums, moment, Utils, Globals) {
    'use strict';

    var StatisticsTimelineView = Backbone.Marionette.LayoutView.extend(
        /** @lends AboutAtlasView */
        {
            template: StatTimelineTmpl,

            /** Layout sub regions */
            regions: {},
            /** ui selector cache */
            ui: {
                entity: "[data-id='entity']",
                classification: "[data-id='classification']",
                serverCard: "[data-id='server-card']",
                connectionCard: "[data-id='connection-card']",
                notificationCard: "[data-id='notification-card']",
                statsNotificationTable: "[data-id='stats-notification-table']",
                entityCard: "[data-id='entity-card']",
                classificationCard: "[data-id='classification-card']",
                offsetCard: "[data-id='offset-card']",
                osCard: "[data-id='os-card']",
                runtimeCard: "[data-id='runtime-card']",
                memoryCard: "[data-id='memory-card']",
                memoryPoolUsage: "[data-id='memory-pool-usage-card']",
                statisticsRefresh: "[data-id='statisticsRefresh']",
                notificationDetails: "[data-id='notificationDetails']",
                migrationProgressBar: "[data-id='migrationProgressBar']",
                migrationProgressBarValue: "[data-id='migrationProgressBarValue']",
                metricsTimeLine: "[data-id='metricsTimeLine']",
                metricsTimeList: "[data-id='metricsTimeList']",
                chartTitle: "[data-id='chartTitle']",
                chartTimeLineList: "[data-id='chartTimeLineList']",
                chartButtons: ".chart-buttons",
                compareGraph: "[data-id='compareGraph']",
                entityChartCollapseHeading: "[data-id='entityChartCollapseHeading']",
                entityChartCollapse: "[data-id='entityChartCollapse']",
                entityTypeSelection: ".entity-type-chart",
                chartButton: ".chart-button"
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.statisticsRefresh] = function(e) {
                    this.showLoader();
                    this.fetchMetricData();
                    this.fetchStatusData();
                    this.createMetricsChart();
                };
                events["change " + this.ui.metricsTimeList] = function(e) {
                    var that = this,
                        selectedTime = this.ui.metricsTimeList.val(),
                        options = { isMetricsCollectionTime: true };
                    if (selectedTime.length) {
                        that.metricTime.url = UrlLinks.metricsCollectionTimeApiUrl() + selectedTime;
                        that.showLoader();
                        (selectedTime === "Current") ? that.fetchMetricData(): that.fetchMetricData(options);
                        that.metricsChartDateRange = "7d";
                        that.createMetricsChart();
                        that.selectDefaultValueTimer();

                        //that.ui.chartTimeLineList.val(that.metricsChartDateRange).trigger("change");
                    }
                };

                // events["change " + this.ui.chartTimeLineList] = function(e) {
                //     var that = this;
                //     that.metricsChartDateRange = this.ui.chartTimeLineList.val();
                //     that.createMetricsChart();
                // };

                events["click " + this.ui.chartButton] = function(e) {
                    e.stopPropagation();
                    that.$el.find('.chart-button').removeClass('active');
                    $(e.currentTarget).addClass("active");
                    that.metricsChartDateRange = $(e.currentTarget).children().text();
                    that.createMetricsChart();

                };
                return events;
            },
            createMetricsChart: function() {
                var that = this,
                    selectedDateRange = that.dateRangesMap[that.metricsChartDateRange],
                    startTime = Date.parse(that.getValue({ value: selectedDateRange[0], type: "day" })),
                    endTime = Date.parse(that.getValue({ value: selectedDateRange[1], type: "day" }));
                $(that.ui.chartTitle).text(that.entityType + " chart for " + that.dateRangeText[that.metricsChartDateRange]);
                var options = {
                    "startTime": startTime,
                    "endTime": endTime,
                    "typeName": that.entityType
                };
                that.showChartLoader();
                that.createChartFromCollection(options);
            },
            /**
             * intialize a new AboutAtlasView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, options);
                var that = this;
                this.DATA_MAX_LENGTH = 25;
                this.loaderCount = 0;
                this.chartLoaderCount = 0;
                this.metricCollectctionTime = new VTagList();
                this.metricTime = new VTagList();
                this.metricCollectctionTime.url = UrlLinks.metricsAllCollectionTimeApiUrl();
                this.entityTypeChartCollection = new VTagList();
                this.entityTypeChartCollection.url = UrlLinks.metricsGraphUrl();
                this.entityType = "hive_table"; //default entity type for metrics chart
                this.metricsChartDateRange = "7d"; //default range for metrics  chart 
                this.freshLoad = true;
                this.dateRangesMap = {
                    'Today': moment(),
                    '1d': [moment().subtract(1, 'days'), moment()],
                    '7d': [moment().subtract(6, 'days'), moment()],
                    '14d': [moment().subtract(13, 'days'), moment()],
                    '30d': [moment().subtract(29, 'days'), moment()],
                    'This Month': [moment().startOf('month'), moment().endOf('month')],
                    'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 3 Months': [moment().subtract(3, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 6 Months': [moment().subtract(6, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'Last 12 Months': [moment().subtract(12, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                    'This Quarter': [moment().startOf('quarter'), moment().endOf('quarter')],
                    'Last Quarter': [moment().subtract(1, 'quarter').startOf('quarter'), moment().subtract(1, 'quarter').endOf('quarter')],
                    'This Year': [moment().startOf('year'), moment().endOf('year')],
                    'Last Year': [moment().subtract(1, 'year').startOf('year'), moment().subtract(1, 'year').endOf('year')]
                };
                this.dateRangeText = {
                    '1d': "last 1 day",
                    '7d': "last 7 days",
                    '14d': "last 14 days",
                    '30d': "last 30 days",
                };
                if (this.isMigrationView) {
                    this.migrationImportStatus = new VTagList();
                    this.migrationImportStatus.url = UrlLinks.migrationStatusApiUrl();
                } else {
                    var modal = new Modal({
                        title: 'Statistics',
                        content: this,
                        okCloses: true,
                        okText: "Close",
                        showFooter: true,
                        allowCancel: false,
                        width: "60%",
                        headerButtons: [{
                            title: "Refresh Data",
                            btnClass: "fa fa-refresh",
                            onClick: function() {
                                modal.$el.find('.header-button .fa-refresh').tooltip('hide').prop('disabled', true).addClass('fa-spin');
                                // that.fetchMetricData({ update: true });
                                that.freshChart = true;
                                d3.selectAll("#compareGraph > svg > *").remove();
                                that.fetchAllMetricsData({ update: true });
                            }
                        }]
                    });
                    modal.on('closeModal', function() {
                        modal.trigger('cancel');
                    });
                    this.modal = modal;
                    modal.open();
                }
            },
            bindEvents: function() {
                var that = this;
                if (this.modal) {
                    this.$el.on('click', '.linkClicked', function() {
                        that.modal.close();
                    })
                }
                this.$el.find("table").on('click', function(e) {
                    var clickElement = $(e.target);
                    if ($(this).hasClass("entityTable") && clickElement.parents().hasClass("entity-type-chart")) {
                        $(this).find("tr").removeClass("table-row-selected");
                        var parentRow = clickElement.parents("tr");
                        parentRow.addClass("table-row-selected");
                        if (that.entityType === parentRow.attr('id')) {
                            that.ui.entityChartCollapse.toggleClass('in');
                        } else {
                            that.entityType = parentRow.attr('id');
                            that.freshLoad = true;
                            that.createMetricsChart();
                            if (!that.ui.entityChartCollapse.hasClass('in')) {
                                that.ui.entityChartCollapse.toggleClass('in');
                            }
                        }
                    }
                });
            },
            onRender: function() {
                this.bindEvents();
                this.createChartTimeLineDropdown(); //create dropdown for chart with days options
                this.fetchAllMetricsData();
                this.createTimeLineDropdown();
            },
            fetchStatusData: function() {
                var that = this;
                ++this.loaderCount;
                that.migrationImportStatus.fetch({
                    success: function(data) {
                        var data = _.first(data.toJSON()),
                            migrationStatus = data.MigrationStatus || null,
                            operationStatus = migrationStatus.operationStatus,
                            showProgress = true,
                            totalProgress = 0,
                            progressMessage = "";
                        if (migrationStatus) {
                            if (MigrationEnums.migrationStatus[operationStatus] === "DONE") {
                                showProgress = false;
                            } else if (MigrationEnums.migrationStatus[operationStatus] === "IN_PROGRESS" || MigrationEnums.migrationStatus[operationStatus] === "STARTED") {
                                var currentIndex = migrationStatus.currentIndex || 0,
                                    totalCount = migrationStatus.totalCount || 0;
                                totalProgress = Math.ceil((migrationStatus.currentIndex / migrationStatus.totalCount) * 100)
                                progressMessage = totalProgress + "%";
                                that.ui.migrationProgressBar.removeClass("progress-bar-danger");
                                that.ui.migrationProgressBar.addClass("progress-bar-success");
                            } else if (MigrationEnums.migrationStatus[operationStatus] === "FAIL") {
                                totalProgress = "100";
                                progressMessage = "Failed";
                                that.ui.migrationProgressBar.addClass("progress-bar-danger");
                                that.ui.migrationProgressBar.removeClass("progress-bar-success");
                            }
                            if (showProgress) {
                                that.$el.find(".statistics-header>.progress").removeClass("hide");
                                that.$el.find(".statistics-header>.successStatus").addClass("hide");
                                that.ui.migrationProgressBar.css({ width: totalProgress + '%' });
                                that.ui.migrationProgressBarValue.text(progressMessage);
                            } else {
                                that.$el.find(".statistics-header>.progress").addClass("hide");
                                that.$el.find(".statistics-header>.successStatus").removeClass("hide");
                            }
                        }

                    },
                    complete: function() {
                        --that.loaderCount;
                        that.hideLoader();
                    }
                });
            },
            fetchAllMetricsData: function(options) {
                this.fetchMetricCollectionTime(); // create dropdown as per collectionTime
                this.showLoader();
                if (this.isMigrationView) {
                    this.fetchStatusData();
                }
                this.fetchMetricData(options); // display all the required metrics data
                this.createMetricsChart(); // display Metrics in chart
                this.selectDefaultValueTimer();
            },
            selectDefaultValueTimer: function(options) {
                var that = this;
                this.defaultValues = setTimeout(function() {
                    that.$el.find('#' + that.entityType).addClass('table-row-selected');
                    that.resetChartButtonSelection();
                    that.selectDefaultValueTimerStop();
                }, 1200);
            },
            selectDefaultValueTimerStop: function() {
                clearTimeout(this.defaultValues);
            },
            resetChartButtonSelection: function() {
                var that = this;
                this.$el.find('.chart-button').removeClass('active');
                _.each(this.$el.find('.chart-button'), function(chartButton) {
                    if (chartButton.innerText === that.metricsChartDateRange) {
                        $(chartButton).addClass("active")
                    }
                });
            },
            fetchMetricCollectionTime: function() {
                var that = this;
                ++this.loaderCount;
                this.showLoader();
                this.metricCollectctionTime.fetch({
                    success: function(data) {
                        var data = data ? data.toJSON() : null;
                        data.length ? that.createTimeLineDropdown(data) : that.createTimeLineDropdown();
                    },
                    complete: function() {
                        --that.loaderCount;
                        that.hideLoader()
                    }
                });
            },
            fetchMetricData: function(options) {
                var that = this,
                    collectionTofetch = (options && options.isMetricsCollectionTime) ? that.metricTime : that.metricCollection;
                ++this.loaderCount;

                collectionTofetch.fetch({
                    success: function(data) {
                        var data = _.first(data.toJSON());
                        // data = (options && options.isMetricsCollectionTime) ? data.data : data;
                        if (options && options.isMetricsCollectionTime) {
                            data = (data.metrics && data.metrics.data) ? data.metrics.data : data;
                        }
                        that.renderStats({ valueObject: data.general.stats, dataObject: data.general });
                        that.renderEntities({ data: data });
                        that.renderSystemDeatils({ data: data });
                        that.renderClassifications({ data: data });
                        if (options && options.update) {
                            if (that.modal) {
                                that.modal.$el.find('.header-button .fa-refresh').prop('disabled', false).removeClass('fa-spin');
                            }
                            Utils.notifySuccess({
                                content: "Metric data is refreshed"
                            })
                        }
                    },
                    complete: function() {
                        --that.loaderCount;
                        that.hideLoader();
                    }
                });
            },
            createChartFromCollection: function(params) {
                var that = this,
                    chartData = {};
                ++that.chartLoaderCount;
                console.log("startTime: " + new Date(params.startTime) + ", endTime: " + new Date(params.endTime) + " ,typeName: " + params.typeName);
                that.entityTypeChartCollection.queryParams = params;
                that.entityTypeChartCollection.fetch({
                    success: function(data) {
                        chartData = _.first(data.toJSON());
                        that.renderStackAreaGraps(chartData[that.entityType]);
                        if (that.freshLoad) {
                            that.freshLoad = false;
                            that.selectDefaultChartTimer(chartData[that.entityType]);
                        }
                    },
                    complete: function() {
                        --that.chartLoaderCount;
                        that.hideChartLoader();
                    }
                });
            },
            selectDefaultChartTimer: function(chartData) {
                var that = this;
                this.chartRenderTimer = setTimeout(function() {
                    that.renderStackAreaGraps(chartData);
                    that.selectDefaultChartTimerStop();
                }, 1500);
            },
            selectDefaultChartTimerStop: function() {
                clearTimeout(this.chartRenderTimer);
            },
            createTimeLineDropdown: function(data) {
                var that = this,
                    options = '<option value="Current" data-name="Current" selected>Current</option>';
                that.ui.metricsTimeList.empty();
                _.each(data, function(data) {
                    var collectionTime = that.getValue({ type: "day", value: data.collectionTime });
                    options += '<option value="' + (data.collectionTime) + '" data-name="' + (data.collectionTime) + '">' + collectionTime + '</option>';
                });
                that.ui.metricsTimeList.html(options);
            },
            createChartTimeLineDropdown: function() {
                var that = this,
                    options = '<ul class="pull-right">';
                options += '<li class="chart-button"><a href="javascript:void(0);"  title="Last 1 day"  data-day="1d">1d</a></li>';
                options += '<li class="chart-button active"><a href="javascript:void(0);"  title="Last 7 days"  data-day="7d">7d</a></li>';
                options += '<li class="chart-button"><a href="javascript:void(0);"  title="Last 14 days"  data-day="14d">14d</a></li>';
                options += '<li class="chart-button"><a href="javascript:void(0);"  title="Last 30 days"  data-day="30d">30d</a></li>';
                options += '</ul>'
                that.ui.chartTimeLineList.empty();
                that.ui.chartTimeLineList.html(options);
            },
            createTimeLineDropdownGraph: function() {
                this.defaultRange = "7d";
                this.dateValue = null;
                var that = this,
                    isTimeRange = true,
                    obj = {
                        opens: "center",
                        autoApply: true,
                        autoUpdateInput: false,
                        timePickerSeconds: true,
                        timePicker: true,
                        locale: {
                            format: Globals.dateTimeFormat
                        }
                    };
                if (isTimeRange) {
                    var defaultRangeDate = this.dateRangesMap[this.defaultRange];
                    obj.startDate = defaultRangeDate[0];
                    obj.endDate = defaultRangeDate[1];
                    obj.singleDatePicker = false;
                    obj.ranges = this.dateRangesMap;
                } else {
                    obj.singleDatePicker = true;
                    obj.startDate = moment();
                    obj.endDate = obj.startDate;
                }
                var inputEl = this.ui.metricsTimeLine;

                inputEl.attr('readonly', true);
                inputEl.daterangepicker(obj);
                inputEl.on('apply.daterangepicker', function(ev, picker) {
                    picker.setStartDate(picker.startDate);
                    picker.setEndDate(picker.endDate);
                    var valueString = "";
                    if (picker.chosenLabel) {
                        if (picker.chosenLabel === "Custom Range") {
                            valueString = picker.startDate.format(Globals.dateTimeFormat) + " - " + picker.endDate.format(Globals.dateTimeFormat);
                        } else {
                            valueString = picker.chosenLabel;
                        }
                    } else {
                        valueString = picker.startDate.format(Globals.dateTimeFormat);
                    }
                    picker.element.val(valueString);
                    that.dateValue = valueString;
                });
            },
            hideLoader: function() {
                if (this.loaderCount === 0) {
                    var className = ".statsContainer";
                    if (this.isMigrationView) {
                        className += ",.statistics-header";
                    }
                    this.$(className).removeClass('hide');
                    this.$('.statsLoader').removeClass('show');
                }
            },
            showLoader: function() {
                var className = ".statsContainer";
                if (this.isMigrationView) {
                    className += ",.statistics-header";
                }
                this.$(className).addClass('hide');
                this.$('.statsLoader').addClass('show');
            },
            hideChartLoader: function() {
                if (this.chartLoaderCount === 0) {
                    // var className = ".chart-container";
                    // this.$(className).removeClass('hide');
                    this.$('.statsLoader').removeClass('show');
                    this.$('.modalOverlay').addClass('hide');
                }
            },
            showChartLoader: function() {
                // var className = ".chart-container";
                // this.$(className).addClass('hide');
                this.$('.statsLoader').addClass('show');
                this.$('.modalOverlay').removeClass('hide');
            },
            closePanel: function(options) {
                var el = options.el;
                el.find(">.panel-heading").attr("aria-expanded", "false");
                el.find(">.panel-collapse.collapse").removeClass("in");
            },
            genrateStatusData: function(stateObject) {
                var that = this,
                    stats = {};
                _.each(stateObject, function(val, key) {
                    var keys = key.split(":"),
                        key = keys[0],
                        subKey = keys[1];
                    if (stats[key]) {
                        stats[key][subKey] = val;
                    } else {
                        stats[key] = {};
                        stats[key][subKey] = val;
                    }
                });
                return stats;
            },
            createTable: function(obj) {
                var that = this,
                    tableBody = '',
                    type = obj.type,
                    data = obj.data;
                _.each(data, function(value, key, list) {
                    var newValue = that.getValue({
                        "value": value
                    });
                    if (type === "classification") {
                        newValue = '<a title="Search for entities associated with \'' + key + '\'" class="linkClicked" href="#!/search/searchResult?searchType=basic&tag=' + key + '">' + newValue + '<a>';
                    }
                    tableBody += '<tr><td>' + key + '</td><td class="">' + newValue + '</td></tr>';
                });
                return tableBody;
            },
            renderClassifications: function(options) {
                var that = this,
                    data = options.data,
                    classificationData = data.tag || {},
                    tagEntitiesData = classificationData ? classificationData.tagEntities || {} : {},
                    tagsCount = 0,
                    newTagEntitiesData = {},
                    tagEntitiesKeys = _.keys(tagEntitiesData);
                _.each(_.sortBy(tagEntitiesKeys, function(o) {
                    return o.toLocaleLowerCase();
                }), function(key) {
                    var val = tagEntitiesData[key];
                    newTagEntitiesData[key] = val;
                    tagsCount += val;
                });
                tagEntitiesData = newTagEntitiesData;

                if (!_.isEmpty(tagEntitiesData)) {
                    this.ui.classificationCard.html(
                        that.createTable({
                            "data": tagEntitiesData,
                            "type": "classification"
                        })
                    );
                    this.ui.classification.find(".count").html("&nbsp;(" + _.numberFormatWithComma(tagsCount) + ")");
                    if (tagEntitiesKeys.length > this.DATA_MAX_LENGTH) {
                        this.closePanel({
                            el: this.ui.classification
                        })
                    }
                }
            },
            renderEntities: function(options) {
                var that = this,
                    data = options.data,
                    entityData = data.entity,
                    activeEntities = entityData.entityActive || {},
                    deletedEntities = entityData.entityDeleted || {},
                    shellEntities = entityData.entityShell || {},
                    stats = {},
                    activeEntityCount = 0,
                    deletedEntityCount = 0,
                    shellEntityCount = 0,
                    createEntityData = function(opt) {
                        var entityData = opt.entityData,
                            type = opt.type;
                        _.each(entityData, function(val, key) {
                            var intVal = _.isUndefined(val) ? 0 : val;
                            if (type == "active") {
                                activeEntityCount += intVal;
                            }
                            if (type == "deleted") {
                                deletedEntityCount += intVal;
                            }
                            if (type == "shell") {
                                shellEntityCount += intVal
                            }
                            intVal = _.numberFormatWithComma(intVal)
                            if (stats[key]) {
                                stats[key][type] = intVal;
                            } else {
                                stats[key] = {};
                                stats[key][type] = intVal;
                            }
                        })
                    };

                createEntityData({
                    "entityData": activeEntities,
                    "type": "active"
                })
                createEntityData({
                    "entityData": deletedEntities,
                    "type": "deleted"
                });
                createEntityData({
                    "entityData": shellEntities,
                    "type": "shell"
                });
                if (!_.isEmpty(stats)) {
                    var statsKeys = _.keys(stats);
                    this.ui.entityCard.html(
                        EntityTable({
                            "data": _.pick(stats, _.sortBy(statsKeys, function(o) {
                                return o.toLocaleLowerCase();
                            })),
                        })
                    );
                    this.$('[data-id="activeEntity"]').html("&nbsp;(" + _.numberFormatWithComma(activeEntityCount) + ")");
                    this.$('[data-id="deletedEntity"]').html("&nbsp;(" + _.numberFormatWithComma(deletedEntityCount) + ")");
                    this.$('[data-id="shellEntity"]').html("&nbsp;(" + _.numberFormatWithComma(shellEntityCount) + ")");
                    this.ui.entity.find(".count").html("&nbsp;(" + _.numberFormatWithComma(data.general.entityCount) + ")");
                    if (statsKeys.length > this.DATA_MAX_LENGTH) {
                        this.closePanel({
                            el: this.ui.entity
                        })
                    }
                }
            },
            renderStats: function(options) {
                var that = this,
                    data = this.genrateStatusData(options.valueObject),
                    generalData = options.dataObject,
                    createTable = function(obj) {
                        var tableBody = '',
                            enums = obj.enums,
                            data = obj.data;
                        _.each(data, function(value, key, list) {
                            tableBody += '<tr><td>' + key + '</td><td class="">' + that.getValue({
                                "value": value,
                                "type": enums[key]
                            }) + '</td></tr>';
                        });
                        return tableBody;
                    };
                if (!that.isMigrationView && data.Notification) {
                    var tableCol = [{
                                label: "Total <br> (from " + (that.getValue({
                                    "value": data.Server["startTimeStamp"],
                                    "type": Enums.stats.Server["startTimeStamp"],
                                })) + ")",
                                key: "total"
                            },
                            {
                                label: "Current Hour <br> (from " + (that.getValue({
                                    "value": data.Notification["currentHourStartTime"],
                                    "type": Enums.stats.Notification["currentHourStartTime"],
                                })) + ")",
                                key: "currentHour"
                            },
                            { label: "Previous Hour", key: "previousHour" },
                            {
                                label: "Current Day <br> (from " + (that.getValue({
                                    "value": data.Notification["currentDayStartTime"],
                                    "type": Enums.stats.Notification["currentDayStartTime"],
                                })) + ")",
                                key: "currentDay"
                            },
                            { label: "Previous Day", key: "previousDay" }
                        ],
                        tableHeader = ["count", "AvgTime", "EntityCreates", "EntityUpdates", "EntityDeletes", "Failed"];
                    that.ui.notificationCard.html(
                        StatsNotiTable({
                            "enums": Enums.stats.Notification,
                            "data": data.Notification,
                            "tableHeader": tableHeader,
                            "tableCol": tableCol,
                            "getTmplValue": function(argument, args) {
                                var pickValueFrom = argument.key.concat(args);
                                if (argument.key == "total" && args == "EntityCreates") {
                                    pickValueFrom = "totalCreates";
                                } else if (argument.key == "total" && args == "EntityUpdates") {
                                    pickValueFrom = "totalUpdates";
                                } else if (argument.key == "total" && args == "EntityDeletes") {
                                    pickValueFrom = "totalDeletes";
                                } else if (args == "count") {
                                    pickValueFrom = argument.key;
                                }
                                var returnVal = data.Notification[pickValueFrom];
                                return returnVal ? _.numberFormatWithComma(returnVal) : 0;
                            }
                        })
                    );

                    var offsetTableColumn = function(obj) {
                        var returnObj = []
                        _.each(obj, function(value, key) {
                            returnObj.push({ "label": key, "dataValue": value });
                        });
                        return returnObj
                    }

                    that.ui.offsetCard.html(
                        TopicOffsetTable({
                            data: data.Notification.topicDetails,
                            tableHeader: ["offsetStart", "offsetCurrent", "processedMessageCount", "failedMessageCount", "lastMessageProcessedTime"],
                            tableCol: offsetTableColumn(data.Notification.topicDetails),
                            getTmplValue: function(argument, args) {
                                var returnVal = data.Notification.topicDetails[argument.label][args];
                                return returnVal ? that.getValue({ value: returnVal, type: Enums.stats.Notification[args] }) : 0;
                            }
                        })
                    )
                    that.ui.notificationDetails.removeClass('hide');
                }

                if (data.Server) {
                    that.ui.serverCard.html(
                        createTable({
                            "enums": _.extend(Enums.stats.Server, Enums.stats.ConnectionStatus, Enums.stats.generalData),
                            "data": _.extend(
                                _.pick(data.Server, 'startTimeStamp', 'activeTimeStamp', 'upTime', 'statusBackendStore', 'statusIndexStore'),
                                _.pick(generalData, 'collectionTime'))
                        })
                    );
                }
            },
            renderSystemDeatils: function(options) {
                var that = this,
                    data = options.data,
                    systemData = data.system,
                    systemOS = systemData.os || {},
                    systemRuntimeData = systemData.runtime || {},
                    systemMemoryData = systemData.memory || {};
                if (!_.isEmpty(systemOS)) {
                    that.ui.osCard.html(
                        that.createTable({
                            "data": systemOS
                        })
                    );
                }
                if (!_.isEmpty(systemRuntimeData)) {
                    that.ui.runtimeCard.html(
                        that.createTable({
                            "data": systemRuntimeData
                        })
                    );
                }
                if (!_.isEmpty(systemMemoryData)) {
                    var memoryTable = CommonViewFunction.propertyTable({
                        scope: this,
                        formatStringVal: true,
                        valueObject: systemMemoryData,
                        numberFormat: _.numberFormatWithBytes
                    });
                    that.ui.memoryCard.html(
                        memoryTable);
                }
            },
            getValue: function(options) {
                var value = options.value,
                    type = options.type;
                if (type == 'time') {
                    return Utils.millisecondsToTime(value);
                } else if (type == 'day') {
                    return Utils.formatDate({ date: value })
                } else if (type == 'number') {
                    return _.numberFormatWithComma(value);
                } else if (type == 'millisecond') {
                    return _.numberFormatWithComma(value) + " millisecond/s";
                } else if (type == "status-html") {
                    return '<span class="connection-status ' + value + '"></span>';
                } else {
                    return value;
                }
            },
            renderStackAreaGraps: function(data) {
                var that = this,
                    data = data;

                nv.addGraph(function() {
                    var chart = nv.models.stackedAreaChart()
                        .margin({ right: 60 })
                        .x(function(d) { return d[0] }) //We can modify the data accessor functions...
                        .y(function(d) { return d[1] }) //...in case your data is formatted differently.
                        .useInteractiveGuideline(true) //Tooltips which show all data points. Very nice!
                        .rightAlignYAxis(false) //Let's move the y-axis to the right side.
                        //.transitionDuration(500)
                        .showControls(true) //Allow user to choose 'Stacked', 'Stream', 'Expanded' mode.
                        .clipEdge(true);

                    //Format x-axis labels with custom function.
                    chart.xAxis
                        .tickFormat(function(d) {
                            return d3.time.format('%x')(new Date(d));
                        });

                    chart.yAxis
                        .tickFormat(d3.format(',.2f'));

                    var graphTimer = setTimeout(function() {
                        d3.select(that.ui.compareGraph[0])
                            .datum(data).transition().duration(500)
                            .call(chart);
                        clearTimeout(graphTimer);
                    }, 300);

                    d3.select(window).on('mouseout', function() {
                        d3.selectAll('.nvtooltip').style('opacity', '0');
                    });

                    nv.utils.windowResize(chart.update);

                    return chart;
                });
            }
        });
    return StatisticsTimelineView;
});