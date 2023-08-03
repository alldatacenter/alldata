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
    'hbs!tmpl/profile/ProfileLayoutView_tmpl',
    'collection/VProfileList',
    'utils/Utils',
    'utils/Messages',
    'utils/Globals'
], function(require, Backbone, ProfileLayoutViewTmpl, VProfileList, Utils, Messages, Globals) {
    'use strict';

    var ProfileLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends ProfileLayoutView */
        {
            _viewName: 'ProfileLayoutView',

            template: ProfileLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RProfileTableOrColumnLayoutView: "#r_profileTableOrColumnLayoutView"
            },
            /** ui selector cache */
            ui: {},
            templateHelpers: function() {
                return {
                    profileData: this.profileData ? this.profileData.attributes : this.profileData,
                    typeName: this.typeName
                };
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.addTag] = 'checkedValue';
                return events;
            },
            /**
             * intialize a new ProfileLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'profileData', 'guid', 'value', 'typeName', 'entityDetail', 'typeHeaders', 'entityDefCollection', 'enumDefCollection', 'classificationDefCollection', 'glossaryCollection'));
                if (this.typeName === "hive_db" || this.typeName === "hbase_namespace") {
                    this.profileData = { attributes: true };
                }
            },
            bindEvents: function() {},
            onRender: function() {
                if (this.profileData) {
                    if (this.typeName === "hive_table") {
                        this.renderProfileTableLayoutView();
                    } else if (this.typeName === "hive_db" || this.typeName === "hbase_namespace") {
                        this.renderSearchResultLayoutView();
                    } else {
                        this.renderProfileColumnLayoutView();
                    }
                }
            },
            renderSearchResultLayoutView: function() {
                var that = this;
                require(['views/search/SearchResultLayoutView'], function(SearchResultLayoutView) {
                    var value = _.extend({}, that.value, {
                        'guid': that.guid,
                        'searchType': 'relationship',
                        'typeName': that.typeName
                    });
                    that.RProfileTableOrColumnLayoutView.show(new SearchResultLayoutView({
                        'value': value,
                        'profileDBView': true,
                        'typeHeaders': that.typeHeaders,
                        'entityDefCollection': that.entityDefCollection,
                        'enumDefCollection': that.enumDefCollection,
                        'isTableDropDisable': true,
                        'glossaryCollection': that.glossaryCollection,
                        'classificationDefCollection': that.classificationDefCollection
                    }));
                });
            },
            renderProfileTableLayoutView: function(tagGuid) {
                var that = this;
                require(['views/profile/ProfileTableLayoutView'], function(ProfileTableLayoutView) {
                    that.RProfileTableOrColumnLayoutView.show(new ProfileTableLayoutView(that.options));
                });
            },
            renderProfileColumnLayoutView: function(tagGuid) {
                var that = this;
                require(['views/profile/ProfileColumnLayoutView'], function(ProfileColumnLayoutView) {
                    that.RProfileTableOrColumnLayoutView.show(new ProfileColumnLayoutView(that.options));
                });
            },
        });
    return ProfileLayoutView;
});