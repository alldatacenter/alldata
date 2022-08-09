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
    "jquery",
    "underscore",
    "backbone",
    "App",
    "utils/Globals",
    "utils/Utils",
    "utils/UrlLinks",
    'utils/Enums',
    "collection/VGlossaryList"
], function($, _, Backbone, App, Globals, Utils, UrlLinks, Enums, VGlossaryList) {
    var AppRouter = Backbone.Router.extend({
        routes: {
            // Define some URL routes
            "": "defaultAction",
            // Search
            "!/search": "renderDefaultSearchLayoutView",
            "!/search/searchResult": function() {
                this.renderDefaultSearchLayoutView({ fromSearchResultView: true });
            },
            // Tag
            "!/tag": "renderTagLayoutView",
            "!/tag/tagAttribute/(*name)": "renderTagLayoutView",
            // Glossary
            "!/glossary": "renderGlossaryLayoutView",
            "!/glossary/:id": "renderGlossaryLayoutView",
            // Details
            "!/detailPage/:id": "detailPage",
            //Audit table
            '!/administrator': 'administrator',
            '!/administrator/businessMetadata/:id': 'businessMetadataDetailPage',
            //Debug Metrics
            '!/debugMetrics': 'debugMetrics',
            // Default
            "*actions": "defaultAction"
        },
        initialize: function(options) {
            _.extend(
                this,
                _.pick(options, "entityDefCollection", "typeHeaders", "enumDefCollection", "classificationDefCollection", "metricCollection", "classificationAndMetricEvent", "businessMetadataDefCollection")
            );
            this.showRegions();
            this.bindCommonEvents();
            this.listenTo(this, "route", this.postRouteExecute, this);
            this.searchVent = new Backbone.Wreqr.EventAggregator();
            this.categoryEvent = new Backbone.Wreqr.EventAggregator();
            this.glossaryCollection = new VGlossaryList([], {
                comparator: function(item) {
                    return item.get("name");
                }
            });
            this.preFetchedCollectionLists = {
                entityDefCollection: this.entityDefCollection,
                typeHeaders: this.typeHeaders,
                enumDefCollection: this.enumDefCollection,
                classificationDefCollection: this.classificationDefCollection,
                glossaryCollection: this.glossaryCollection,
                metricCollection: this.metricCollection,
                classificationAndMetricEvent: this.classificationAndMetricEvent,
                businessMetadataDefCollection: this.businessMetadataDefCollection
            };
            this.ventObj = {
                searchVent: this.searchVent,
                categoryEvent: this.categoryEvent
            }
            this.sharedObj = {
                searchTableColumns: {},
                glossary: {
                    selectedItem: {}
                },
                searchTableFilters: {
                    tagFilters: {},
                    entityFilters: {}
                }
            };
        },
        bindCommonEvents: function() {
            var that = this;
            $("body").on("click", "a.show-stat", function() {
                require(["views/site/Statistics"], function(Statistics) {
                    new Statistics(_.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj));
                });
            });
            $("body").on("click", "li.aboutAtlas", function() {
                require(["views/site/AboutAtlas"], function(AboutAtlas) {
                    new AboutAtlas();
                });
            });

            $("body").on("click", "a.show-classification", function() {
                Utils.setUrl({
                    url: "!/tag",
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
                //console.log('No route:', actions);
            });

            $("body").on("click", "a.show-glossary", function() {
                // that.renderGlossaryLayoutView();
                Utils.setUrl({
                    url: "!/glossary",
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            });
        },
        showRegions: function() {},
        renderViewIfNotExists: function(options) {
            var view = options.view,
                render = options.render,
                viewName = options.viewName,
                manualRender = options.manualRender;
            if (!view.currentView) {
                if (render) view.show(options.render(options));
            } else if (manualRender && viewName) {
                if (viewName === view.currentView._viewName) {
                    options.manualRender(options);
                } else {
                    if (render) view.show(options.render(options));
                }
            } else {
                if (manualRender) options.manualRender(options);
            }
        },

        /**
         * @override
         * Execute a route handler with the provided parameters. This is an
         * excellent place to do pre-route setup or post-route cleanup.
         * @param  {Function} callback - route handler
         * @param  {Array}   args - route params
         */
        execute: function(callback, args) {
            this.preRouteExecute();
            if (callback) callback.apply(this, args);
            this.postRouteExecute();
        },
        preRouteExecute: function() {
            $("body").removeClass("global-search-active");
            $(".tooltip").tooltip("hide");
            // console.log("Pre-Route Change Operations can be performed here !!");
        },
        postRouteExecute: function(name, args) {
            // console.log("Post-Route Change Operations can be performed here !!");
            // console.log("Route changed: ", name);
        },
        getHeaderOptions: function(Header, options) {
            var that = this;
            return {
                view: App.rHeader,
                manualRender: function() {
                    this.view.currentView.manualRender(options);
                },
                render: function() {
                    return new Header(_.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj, options));
                }
            };
        },
        detailPage: function(id) {
            var that = this;
            if (id) {
                require(["views/site/Header", "views/detail_page/DetailPageLayoutView", "views/site/SideNavLayoutView"], function(Header, DetailPageLayoutView, SideNavLayoutView) {
                    var paramObj = Utils.getUrlState.getQueryParams(),
                        options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                    that.renderViewIfNotExists(that.getHeaderOptions(Header));
                    that.renderViewIfNotExists({
                        view: App.rSideNav,
                        manualRender: function() {
                            this.view.currentView.manualRender(options);
                        },
                        render: function() {
                            return new SideNavLayoutView(options);
                        }
                    });

                    var dOptions = _.extend({ id: id, value: paramObj }, options);
                    that.renderViewIfNotExists({
                        view: App.rContent,
                        viewName: "DetailPageLayoutView",
                        manualRender: function() {
                            this.view.currentView.manualRender(dOptions);
                        },
                        render: function() {
                            return new DetailPageLayoutView(dOptions);
                        }
                    });
                });
            }
        },
        renderTagLayoutView: function(tagName) {
            var that = this;
            require(["views/site/Header", "views/tag/TagContainerLayoutView", "views/site/SideNavLayoutView"], function(Header, TagContainerLayoutView, SideNavLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams();
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                var options = _.extend({
                        tag: tagName,
                        value: paramObj
                    },
                    that.preFetchedCollectionLists,
                    that.sharedObj,
                    that.ventObj
                )
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.manualRender(options);
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rContent.show(new TagContainerLayoutView(options));
            });
        },
        renderGlossaryLayoutView: function(id) {
            var that = this;
            require(["views/site/Header", "views/glossary/GlossaryContainerLayoutView", "views/search/SearchDefaultLayoutView", "views/site/SideNavLayoutView"], function(Header, GlossaryContainerLayoutView, SearchDefaultLayoutView, SideNavLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams();
                //Below if condition is added  to handle "when Glossary tab does not have any glossary and selected in Old UI and switched to New UI is show continous loading
                if (paramObj === undefined) {
                    that.defaultAction();
                    return;
                }
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                var options = _.extend({
                        guid: id,
                        value: paramObj
                    },
                    that.preFetchedCollectionLists,
                    that.sharedObj,
                    that.ventObj
                );

                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.manualRender(options);
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                //Below condition is added for showing Detail Page for only Term and Category,
                //because we are showing default search Page on Glossary Selection.
                if (paramObj.gType !== "glossary") {
                    that.renderViewIfNotExists({
                        view: App.rContent,
                        viewName: "GlossaryContainerLayoutView",
                        manualRender: function() {
                            this.view.currentView.manualRender(options);
                        },
                        render: function() {
                            return new GlossaryContainerLayoutView(options)
                        }
                    });
                } else {
                    options["value"] = "";
                    options["initialView"] = true;
                    that.renderViewIfNotExists(
                        that.getHeaderOptions(Header, {
                            fromDefaultSearch: true
                        })
                    );
                    App.rContent.show(new SearchDefaultLayoutView(options));
                }
            });
        },
        renderDefaultSearchLayoutView: function(opt) {
            var that = this;
            require(["views/site/Header", "views/search/SearchDefaultLayoutView", "views/site/SideNavLayoutView", "collection/VTagList"], function(Header, SearchDefaultLayoutView, SideNavLayoutView, VTagList) {
                var paramObj = Utils.getUrlState.getQueryParams();
                tag = new VTagList();
                if (paramObj && (paramObj.type || paramObj.tag || paramObj.term || paramObj.query || paramObj.udKeys || paramObj.udLabels) === undefined) {
                    Utils.setUrl({
                        url: "#!/search",
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
                if (Utils.getUrlState.getQueryUrl().lastValue !== "search" && Utils.getUrlState.isAdministratorTab() === false) {
                    paramObj = _.omit(paramObj, ["tabActive", "ns", "nsa"]);
                    Utils.setUrl({
                        url: "#!/search/searchResult",
                        urlParams: paramObj,
                        mergeBrowserUrl: false,
                        trigger: false,
                        updateTabState: true
                    });
                }
                if (paramObj) {
                    if (!paramObj.type) {
                        if (paramObj.entityFilters) {
                            paramObj.entityFilters = null;
                        }
                    }
                    if (!paramObj.tag) {
                        if (paramObj.tagFilters) {
                            paramObj.tagFilters = null;
                        }
                        renderSearchView();
                    } else {
                        var tagValidate = paramObj.tag,
                            isTagPresent = false;
                        if ((tagValidate.indexOf('*') == -1)) {
                            classificationDefCollection.fullCollection.each(function(model) {
                                var name = Utils.getName(model.toJSON(), 'name');
                                if (model.get('category') == 'CLASSIFICATION') {
                                    if (tagValidate) {
                                        if (name === tagValidate) {
                                            isTagPresent = true;
                                        }
                                    }
                                }
                            });
                            _.each(Enums.addOnClassification, function(classificationName) {
                                if (classificationName === tagValidate) {
                                    isTagPresent = true;
                                }
                            });
                            if (!isTagPresent) {
                                tag.url = UrlLinks.classificationDefApiUrl(tagValidate);
                                tag.fetch({
                                    success: function(tagCollection) {
                                        isTagPresent = true;
                                    },
                                    cust_error: function(model, response) {
                                        paramObj.tag = null;
                                    },
                                    complete: function() {
                                        renderSearchView.call();
                                    }
                                });
                            } else {
                                renderSearchView();
                            }
                        } else {
                            renderSearchView();
                        }
                    }
                } else {
                    renderSearchView();
                }

                function renderSearchView() {
                    var isinitialView = true,
                        isTypeTagNotExists = false,
                        tempParam = _.extend({}, paramObj);
                    if (paramObj) {
                        isinitialView =
                            (
                                paramObj.type ||
                                (paramObj.dslChecked == "true" ? "" : paramObj.tag || paramObj.term) ||
                                (paramObj.query ? paramObj.query.trim() : "")
                            ).length === 0;
                    }
                    var options = _.extend({
                            value: paramObj,
                            initialView: isinitialView,
                            fromDefaultSearch: opt ? (opt && !opt.fromSearchResultView) : true,
                            fromSearchResultView: (opt && opt.fromSearchResultView) || false,
                            fromCustomFilterView: (opt && opt.fromCustomFilterView) || false,
                            isTypeTagNotExists: paramObj && (paramObj.type != tempParam.type || tempParam.tag != paramObj.tag)
                        },
                        that.preFetchedCollectionLists,
                        that.sharedObj,
                        that.ventObj
                    );
                    that.renderViewIfNotExists(
                        that.getHeaderOptions(Header, {
                            fromDefaultSearch: options.fromDefaultSearch
                        })
                    );
                    that.renderViewIfNotExists({
                        view: App.rSideNav,
                        manualRender: function() {
                            this.view.currentView.manualRender(options);
                        },
                        render: function() {
                            return new SideNavLayoutView(options);
                        }
                    });
                    that.renderViewIfNotExists({
                        view: App.rContent,
                        viewName: "SearchDefaultlLayoutView",
                        manualRender: function() {
                            this.view.currentView.manualRender(options);
                        },
                        render: function() {
                            return new SearchDefaultLayoutView(options);
                        }
                    });
                }
            });
        },
        administrator: function() {
            var that = this;
            require(["views/site/Header", "views/site/SideNavLayoutView", 'views/administrator/AdministratorLayoutView'], function(Header, SideNavLayoutView, AdministratorLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.manualRender(options);
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rContent.show(new AdministratorLayoutView(_.extend({ value: paramObj, guid: null }, options)));
            });
        },
        debugMetrics: function() {
            var that = this;
            require(["views/site/Header", "views/site/SideNavLayoutView", 'views/dev_debug/DebugMetricsLayoutView'], function(Header, SideNavLayoutView, DebugMetricsLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.manualRender(options);
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rContent.show(new DebugMetricsLayoutView(options));
            });
        },
        businessMetadataDetailPage: function(guid) {
            var that = this;
            require(["views/site/Header", "views/site/SideNavLayoutView", "views/business_metadata/BusinessMetadataContainerLayoutView", ], function(Header, SideNavLayoutView, BusinessMetadataContainerLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({ guid: guid, value: paramObj }, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.manualRender(options);
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rContent.show(new BusinessMetadataContainerLayoutView(options));
            });
        },
        defaultAction: function(actions) {
            // We have no matching route, lets just log what the URL was
            Utils.setUrl({
                url: "#!/search",
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });

            console.log("No route:", actions);
        }
    });
    return AppRouter;
});