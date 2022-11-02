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
    'hbs!tmpl/migration/MigrationView_tmpl'
], function(require, Backbone, MigrationViewTmpl) {
    'use strict';

    var ProfileLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends ProfileLayoutView */
        {
            _viewName: 'MigrationView',

            template: MigrationViewTmpl,

            /** Layout sub regions */
            regions: {
                RStatisticsView: "#r_statisticsView",
            },
            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {},
            /**
             * intialize a new ProfileLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                this.apiBaseUrl = this.getBaseUrl(window.location.pathname);
            },
            bindEvents: function() {},
            getBaseUrl: function(url) {
                var path = url.replace(/\/[\w-]+.(jsp|html)|\/+$/ig, ''),
                    splitPath = path.split("/");
                if (splitPath && splitPath[splitPath.length - 1] === "n") {
                    splitPath.pop();
                    return splitPath.join("/");
                }
                return path;
            },
            onRender: function() {
                var that = this;
                require(["views/site/Statistics", "collection/VTagList", "utils/UrlLinks"], function(Statistics, VTagList, UrlLinks) {
                    that.metricCollection = new VTagList();
                    that.metricCollection.url = UrlLinks.metricsApiUrl();
                    that.metricCollection.modelAttrName = "data";
                    that.RStatisticsView.show(new Statistics({ metricCollection: that.metricCollection, isMigrationView: true }));
                })
            }
        });
    return ProfileLayoutView;
});