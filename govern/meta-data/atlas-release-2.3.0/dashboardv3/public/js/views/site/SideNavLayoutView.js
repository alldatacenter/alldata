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
    'utils/Utils'

], function(require, tmpl, Utils) {
    'use strict';

    var SideNavLayoutView = Marionette.LayoutView.extend({
        template: tmpl,

        regions: {
            RSidebarContent: "#r_SidebarContent"
        },
        ui: {},
        templateHelpers: function() {},
        events: function() {},
        initialize: function(options) {
            this.options = options;
        },
        onRender: function() {
            this.renderSideLayoutView();
        },
        renderSideLayoutView: function(options) {
            var that = this;
            if (options) {
                that.options = options;
            }
            require(['views/search/SearchFilterBrowseLayoutView'], function(SearchFilterBrowseLayoutView) {
                that.RSidebarContent.show(new SearchFilterBrowseLayoutView(that.options));
            });
        },
        manualRender: function(options) {
            if (this.RSidebarContent.currentView) {
                this.RSidebarContent.currentView.manualRender(options);
            }
        },
    });
    return SideNavLayoutView;
});