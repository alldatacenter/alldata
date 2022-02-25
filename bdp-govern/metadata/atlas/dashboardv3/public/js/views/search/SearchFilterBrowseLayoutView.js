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
define([
    "require",
    "hbs!tmpl/search/SearchFilterBrowseLayoutView_tmpl",
    "utils/Utils",
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    "collection/VSearchList",
    'modules/Modal',
    "jstree"
], function(require, SearchFilterBrowseLayoutViewTmpl, Utils, Globals, UrlLinks, CommonViewFunction, VSearchList, Modal) {
    "use strict";

    var SearchFilterBrowseLayoutViewNew = Marionette.LayoutView.extend({
        template: SearchFilterBrowseLayoutViewTmpl,

        regions: {
            // RSaveSearchBasic: '[data-id="r_saveSearchBasic"]',
            RGlossaryTreeRender: '[data-id="r_glossaryTreeRender"]',
            RClassificationTreeRender: '[data-id="r_classificationTreeRender"]',
            REntityTreeRender: '[data-id="r_entityTreeRender"]',
            RCustomFilterTreeRender: '[data-id="r_customFilterTreeRender"]',
            RBusinessMetadataTreeRender: '[data-id="r_businessMetadataTreeRender"]'
        },
        ui: {
            searchNode: '[data-id="searchNode"]',
            sliderBar: '[data-id="sliderBar"]',
            menuItems: ".menu-items"
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl
            };
        },
        events: function() {
            var events = {},
                that = this;

            events["click " + this.ui.sliderBar] = function(e) {
                e.stopPropagation();
                $("#sidebar-wrapper,#page-wrapper").addClass("animate-me");
                $(".container-fluid.view-container").toggleClass("slide-in");
                $("#page-wrapper>div").css({ width: "auto" });
                $("#sidebar-wrapper,.search-browse-box,#page-wrapper").removeAttr("style");
                setTimeout(function() {
                    $("#sidebar-wrapper,#page-wrapper").removeClass("animate-me");
                }, 301);
            };

            events["keyup " + this.ui.searchNode] = function(e) {
                var searchString = _.escape(e.target.value);
                if (searchString.trim() === "") {
                    this.$(".panel").removeClass("hide");
                }
                this.entitySearchTree = this.$('[data-id="entitySearchTree"]');
                this.classificationSearchTree = this.$('[data-id="classificationSearchTree"]');
                this.termSearchTree = this.$('[data-id="termSearchTree"]');
                this.customFilterSearchTree = this.$('[data-id="customFilterSearchTree"]');
                this.businessMetadataSearchTree = this.$('[data-id="businessMetadataSearchTree"]');
                this.entitySearchTree.jstree(true).show_all();
                this.entitySearchTree.jstree("search", searchString);
                this.classificationSearchTree.jstree(true).show_all();
                this.classificationSearchTree.jstree("search", searchString);
                this.termSearchTree.jstree(true).show_all();
                this.termSearchTree.jstree("search", searchString);
                this.customFilterSearchTree.jstree(true).show_all();
                this.customFilterSearchTree.jstree("search", searchString);
                this.businessMetadataSearchTree.jstree(true).show_all();
                this.businessMetadataSearchTree.jstree("search", searchString);
                this.$(".panel-heading.dash-button-icon").removeClass("collapsed").attr("aria-expanded", true);
                this.$(".panel-collapse.collapse").addClass("in").attr("aria-expanded", true).css({ height: "auto" });

            };

            events["click " + this.ui.menuItems] = function(e) {
                e.stopPropagation();
            };
            return events;
        },
        bindEvents: function() {},
        initialize: function(options) {
            this.options = options;
            _.extend(
                this,
                _.pick(
                    options,
                    "typeHeaders",
                    "searchVent",
                    "entityDefCollection",
                    "enumDefCollection",
                    "classificationDefCollection",
                    "searchTableColumns",
                    "searchTableFilters",
                    "metricCollection",
                    "glossaryCollection"
                )
            );
            this.bindEvents();
        },
        onRender: function() {
            var opt = Utils.getUrlState.getQueryParams();
            this.renderEntityTree(opt);
            this.renderClassificationTree(opt);
            this.renderGlossaryTree(opt);
            this.renderCustomFilterTree();
            this.renderBusinessMetadataTree();
            this.showHideGlobalFilter();
            this.showDefaultPage();
        },
        showDefaultPage: function() {
            if (Utils.getUrlState.isSearchTab() && this.options.value) {
                if (
                    !this.options.value.type &&
                    !this.options.value.tag &&
                    !this.options.value.term &&
                    !this.options.value.gType
                ) {
                    Utils.setUrl({
                        url: "!/search",
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
            }
        },
        onShow: function() {
            var that = this;
            this.$(".search-browse-box").resizable({
                handles: { "e": ".slider-bar" },
                minWidth: 30,
                minHeight: window.screen.height,
                resize: function(event, ui) {
                    var width = ui.size.width,
                        calcWidth = "calc(100% - " + width + "px)";
                    $("#sidebar-wrapper").width(width);
                    $("#page-wrapper").css({ width: calcWidth, marginLeft: width + "px" });
                    var selectedEl = $("#page-wrapper>div");
                    if (width > 700) {
                        $("#page-wrapper").css({ overflowX: "auto" });
                        selectedEl.css({ width: window.screen.width - 360 });
                    } else {
                        $("#page-wrapper").css({ overflow: "none" });
                        selectedEl.css({ width: "100%" });
                    }
                },
                start: function() {
                    $(".searchLayoutView").removeClass("open");
                    this.expanding = $(".container-fluid.view-container").hasClass("slide-in");
                    $(".container-fluid.view-container").removeClass("slide-in");
                    if (this.expanding) {
                        $("#sidebar-wrapper,#page-wrapper").addClass("animate-me");
                    }
                },
                stop: function(event, ui) {
                    if (!this.expanding && ui.size.width <= 30) {
                        $("#sidebar-wrapper,#page-wrapper").addClass("animate-me");
                        $("#sidebar-wrapper,#page-wrapper,.search-browse-box").removeAttr("style");
                        $(".container-fluid.view-container").addClass("slide-in");
                    }
                    setTimeout(function() {
                        $("#sidebar-wrapper,#page-wrapper").removeClass("animate-me");
                    }, 301);
                }
            });
        },
        showHideGlobalFilter: function() {
            if (this.options.fromDefaultSearch) {
                this.$(".mainContainer").removeClass("global-filter-browser");
            } else {
                this.$(".mainContainer").addClass("global-filter-browser");
            }
        },
        manualRender: function(options) {
            var that = this;
            if (options) {
                _.extend(this.options, options);
                this.showHideGlobalFilter();
                if (!this.options.value) {
                    this.ui.searchNode.val('').trigger('keyup');
                }
                if (this.RBusinessMetadataTreeRender.currentView) {
                    this.RBusinessMetadataTreeRender.currentView.manualRender(this.options);
                }
                if (this.RCustomFilterTreeRender.currentView) {
                    this.RCustomFilterTreeRender.currentView.manualRender(this.options);
                }
                if (this.RGlossaryTreeRender.currentView) {
                    this.RGlossaryTreeRender.currentView.manualRender(this.options);
                }
                if (this.RClassificationTreeRender.currentView) {
                    this.RClassificationTreeRender.currentView.manualRender(this.options);
                }
                if (this.REntityTreeRender.currentView) {
                    this.REntityTreeRender.currentView.manualRender(this.options);
                }
            }
        },
        renderEntityTree: function(opt) {
            var that = this;
            require(["views/search/tree/EntityTreeLayoutView"], function(ClassificationTreeLayoutView) {
                that.REntityTreeRender.show(new ClassificationTreeLayoutView(_.extend({ query: that.query }, that.options, { value: opt })));
            });
        },
        renderClassificationTree: function(opt) {
            var that = this;
            require(["views/search/tree/ClassificationTreeLayoutView"], function(ClassificationTreeLayoutView) {
                that.RClassificationTreeRender.show(new ClassificationTreeLayoutView(_.extend({ query: that.query }, that.options, { value: opt })));
            });
        },
        renderGlossaryTree: function(opt) {
            var that = this;
            require(["views/search/tree/GlossaryTreeLayoutView"], function(GlossaryTreeLayoutView) {
                that.RGlossaryTreeRender.show(new GlossaryTreeLayoutView(_.extend({ query: that.query }, that.options, { value: opt })));
            });
        },
        renderCustomFilterTree: function() {
            var that = this;
            require(["views/search/tree/CustomFilterTreeLayoutView"], function(CustomFilterTreeLayoutView) {
                that.RCustomFilterTreeRender.show(new CustomFilterTreeLayoutView(_.extend({ query: that.query }, that.options)));
            });
        },
        renderBusinessMetadataTree: function() {
            var that = this;
            require(["views/search/tree/BusinessMetadataTreeLayoutView"], function(BusinessMetadataTreeLayoutView) {
                that.RBusinessMetadataTreeRender.show(new BusinessMetadataTreeLayoutView(_.extend({ query: that.query }, that.options)));
            });
        }
    });
    return SearchFilterBrowseLayoutViewNew;
});