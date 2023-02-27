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
    'hbs!tmpl/glossary/TermPropertiestLayoutView_tmpl',
    'utils/Utils',
    'utils/Enums'
], function(require, Backbone, TermPropertiestLayoutView_tmpl, Utils, Enums) {
    'use strict';

    var TermPropertiestLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends TermPropertiestLayoutView */
        {
            _viewName: 'TermPropertiestLayoutView',

            template: TermPropertiestLayoutView_tmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                propertiesCard: "[data-id='properties-card']"
            },
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new TermPropertiestLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, options);

            },
            onRender: function() {
                this.renderStats();
            },
            bindEvents: function() {},
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
            renderStats: function() {
                var that = this,
                    generalData = this.dataObject,
                    createTable = function(obj) {
                        var tableBody = '',
                            enums = obj.enums;
                        _.each(obj.data, function(value, key, list) {
                            tableBody += '<tr><td>' + key + '</td><td class="">' + that.getValue({
                                "value": value,
                                "type": enums[key]
                            }) + '</td></tr>';
                        });
                        return tableBody;
                    };
                if (that.options.additionalAttributes) {
                    that.ui.propertiesCard.html(
                        createTable({
                            "enums": _.extend(Enums.stats.Server, Enums.stats.ConnectionStatus, Enums.stats.generalData),
                            "data":that.options.additionalAttributes
                        })
                    );
                }
            }
        });
    return TermPropertiestLayoutView;
});