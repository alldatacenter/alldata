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
    'hbs!tmpl/site/SideNavLayoutView_tmpl',
    'utils/Utils',
    'utils/Globals',
    'utils/UrlLinks'
], function(require, tmpl, Utils, Globals, UrlLinks) {
    'use strict';

    var SideNavLayoutView = Marionette.LayoutView.extend({
        template: tmpl,

        regions: {
            RTagLayoutView: "#r_tagLayoutView",
            RSearchLayoutView: "#r_searchLayoutView",
            RGlossaryLayoutView: "#r_glossaryLayoutView"
        },
        ui: {
            tabs: '.tabs li a',
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl
            };
        },
        events: function() {
            var events = {},
                that = this;
            events["click " + this.ui.tabs] = function(e) {
                var urlString = "",
                    elementName = $(e.currentTarget).data(),
                    tabStateUrls = Globals.saveApplicationState.tabState,
                    urlStateObj = Utils.getUrlState,
                    hashUrl = Utils.getUrlState.getQueryUrl().hash;

                if (urlStateObj.isTagTab()) {
                    if (hashUrl != tabStateUrls.tagUrl) {
                        Globals.saveApplicationState.tabState.tagUrl = hashUrl;
                    }
                } else if (urlStateObj.isSearchTab()) {
                    if (hashUrl != tabStateUrls.searchUrl) {
                        Globals.saveApplicationState.tabState.searchUrl = hashUrl;
                    }
                }

                if (elementName.name == "tab-classification") {
                    urlString = tabStateUrls.tagUrl; //'#!/tag';
                } else if (elementName.name == "tab-search") {
                    urlString = tabStateUrls.searchUrl; // '#!/search';
                } else if (elementName.name == "tab-glossary") {
                    urlString = tabStateUrls.glossaryUrl; // '#!/glossary';
                }

                Utils.setUrl({
                    url: urlString,
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            };
            return events;
        },
        initialize: function(options) {
            this.options = options;
        },
        onRender: function() {
            this.renderTagLayoutView();
            this.renderSearchLayoutView();
            this.renderGlossaryLayoutView();
            this.selectTab();

        },
        renderTagLayoutView: function() {
            var that = this;
            require(['views/tag/TagLayoutView'], function(TagLayoutView) {
                that.RTagLayoutView.show(new TagLayoutView(
                    _.extend(that.options, {
                        "collection": that.options.classificationDefCollection
                    })
                ));
            });
        },
        renderSearchLayoutView: function() {
            var that = this;
            require(['views/search/SearchLayoutView'], function(SearchLayoutView) {
                that.RSearchLayoutView.show(new SearchLayoutView(that.options));
            });
        },
        renderGlossaryLayoutView: function() {
            var that = this;
            require(['views/glossary/GlossaryLayoutView'], function(GlossaryLayoutView) {
                that.RGlossaryLayoutView.show(new GlossaryLayoutView(that.options));
            });
        },
        selectTab: function() {
            var that = this;
            var activeTab = function(options) {
                var view = options.view;
                that.$('.tabs').find('li a[aria-controls="tab-' + view + '"]').parents('li').addClass('active').siblings().removeClass('active');
                that.$('.tab-content').find('div#tab-' + view).addClass('active').siblings().removeClass('active');
            };
            if (Utils.getUrlState.isSearchTab() || Utils.getUrlState.isInitial()) {
                activeTab({ "view": "search" });
            } else if (Utils.getUrlState.isTagTab()) {
                activeTab({ "view": "classification" });
            } else if (Utils.getUrlState.isGlossaryTab()) {
                activeTab({ "view": "glossary" });
            } else if (Utils.getUrlState.isDetailPage()) {
                var queryParams = Utils.getUrlState.getQueryParams(),
                    view = "search";
                if (queryParams && queryParams.from) {
                    if (queryParams.from == "classification") {
                        view = "tag";
                    } else if (queryParams.from == "glossary") {
                        view = "glossary";
                    }
                }
                activeTab({ "view": view });
            }
        },
    });
    return SideNavLayoutView;
});