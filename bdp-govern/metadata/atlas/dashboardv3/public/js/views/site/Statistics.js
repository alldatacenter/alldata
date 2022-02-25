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
    'hbs!tmpl/site/Statistics_tmpl',
    'hbs!tmpl/site/Statistics_Notification_table_tmpl',
    'hbs!tmpl/site/Statistics_Topic_Offset_table_tmpl',
    'hbs!tmpl/site/entity_tmpl',
    'modules/Modal',
    'models/VCommon',
    'utils/UrlLinks',
    'collection/VTagList',
    'utils/CommonViewFunction',
    'utils/Enums',
    'moment',
    'utils/Utils',
    'utils/Globals',
    'moment-timezone'
], function(require, Backbone, StatTmpl, StatsNotiTable, TopicOffsetTable, EntityTable, Modal, VCommon, UrlLinks, VTagList, CommonViewFunction, Enums, moment, Utils, Globals) {
    'use strict';

    var StatisticsView = Backbone.Marionette.LayoutView.extend(
        /** @lends AboutAtlasView */
        {
            template: StatTmpl,

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
                memoryPoolUsage: "[data-id='memory-pool-usage-card']"
            },
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new AboutAtlasView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, options);
                var that = this;
                this.DATA_MAX_LENGTH = 25;
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
                            that.fetchMetricData({ update: true });
                        }
                    }]
                });
                modal.on('closeModal', function() {
                    modal.trigger('cancel');
                });
                this.modal = modal;
                modal.open();
            },
            bindEvents: function() {
                var that = this;
                if (this.modal) {
                    this.$el.on('click', '.linkClicked', function() {
                        that.modal.close();
                    })
                }
            },
            fetchMetricData: function(options) {
                var that = this;
                this.metricCollection.fetch({
                    success: function(data) {
                        var data = _.first(data.toJSON());
                        that.renderStats({ valueObject: data.general.stats, dataObject: data.general });
                        that.renderEntities({ data: data });
                        that.renderSystemDeatils({ data: data });
                        that.renderClassifications({ data: data });
                        that.$('.statsContainer,.statsNotificationContainer').removeClass('hide');
                        that.$('.statsLoader,.statsNotificationLoader').removeClass('show');
                        if (options && options.update) {
                            that.modal.$el.find('.header-button .fa-refresh').prop('disabled', false).removeClass('fa-spin');
                            Utils.notifySuccess({
                                content: "Metric data is refreshed"
                            })
                        }
                    }
                });
            },
            onRender: function() {
                this.bindEvents();
                this.fetchMetricData();
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
                if (data.Notification) {
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
                    _.each(systemRuntimeData, function(val, key) {
                        var space
                    })
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
            }
        });
    return StatisticsView;
});