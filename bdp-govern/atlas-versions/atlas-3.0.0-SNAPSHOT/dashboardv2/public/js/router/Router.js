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
    'jquery',
    'underscore',
    'backbone',
    'App',
    'utils/Globals',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/Enums',
    'collection/VGlossaryList'
], function($, _, Backbone, App, Globals, Utils, UrlLinks, Enums, VGlossaryList) {
    var AppRouter = Backbone.Router.extend({
        routes: {
            // Define some URL routes
            "": "defaultAction",
            "!/": "tagAttributePageLoad",
            // Search
            "!/search": "commonAction",
            "!/search/searchResult": "searchResult",
            // Tag
            "!/tag": "commonAction",
            "!/tag/tagAttribute/(*name)": "tagAttributePageLoad",
            // Glossary
            "!/glossary": "commonAction",
            "!/glossary/:id": "glossaryDetailPage",
            // Details
            "!/detailPage/:id": "detailPage",
            //Administrator page
            '!/administrator': 'administrator',
            '!/administrator/businessMetadata/:id': 'businessMetadataDetailPage',
            //Debug Metrics
            '!/debugMetrics': 'debugMetrics',
            // Default
            '*actions': 'defaultAction'
        },
        initialize: function(options) {
            _.extend(this, _.pick(options, 'entityDefCollection', 'typeHeaders', 'enumDefCollection', 'classificationDefCollection', 'metricCollection', 'classificationAndMetricEvent', 'businessMetadataDefCollection'));
            this.showRegions();
            this.bindCommonEvents();
            this.listenTo(this, 'route', this.postRouteExecute, this);
            this.searchVent = new Backbone.Wreqr.EventAggregator();
            this.importVent = new Backbone.Wreqr.EventAggregator();
            this.glossaryCollection = new VGlossaryList([], {
                comparator: function(item) {
                    return item.get("name");
                }
            });
            this.preFetchedCollectionLists = {
                'entityDefCollection': this.entityDefCollection,
                'typeHeaders': this.typeHeaders,
                'enumDefCollection': this.enumDefCollection,
                'classificationDefCollection': this.classificationDefCollection,
                'glossaryCollection': this.glossaryCollection,
                'metricCollection': this.metricCollection,
                'classificationAndMetricEvent': this.classificationAndMetricEvent,
                'businessMetadataDefCollection': this.businessMetadataDefCollection
            }
            this.ventObj = {
                searchVent: this.searchVent,
                importVent: this.importVent
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
            }
        },
        bindCommonEvents: function() {
            var that = this;
            $('body').on('click', 'a.show-stat', function() {
                require([
                    'views/site/Statistics',
                ], function(Statistics) {
                    new Statistics(_.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj));
                });
            });
            $('body').on('click', 'li.aboutAtlas', function() {
                require([
                    'views/site/AboutAtlas',
                ], function(AboutAtlas) {
                    new AboutAtlas();
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
                view: App.rNHeader,
                manualRender: function() {
                    this.view.currentView.manualRender();
                },
                render: function() {
                    return new Header(_.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj, options));
                }
            }
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
                            this.view.currentView.selectTab();
                        },
                        render: function() {
                            return new SideNavLayoutView(options);
                        }
                    });

                    var dOptions = _.extend({ id: id, value: paramObj }, options);
                    that.renderViewIfNotExists({
                        view: App.rNContent,
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
        tagAttributePageLoad: function(tagName) {
            var that = this;
            require([
                'views/site/Header',
                'views/site/SideNavLayoutView',
                'views/tag/TagDetailLayoutView',
            ], function(Header, SideNavLayoutView, TagDetailLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    url = Utils.getUrlState.getQueryUrl().queyParams[0],
                    options = _.extend({ 'tag': tagName, 'value': paramObj }, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        if (paramObj && paramObj.dlttag) {
                            Utils.setUrl({
                                url: url,
                                trigger: false,
                                updateTabState: true
                            });
                        }
                        this.view.currentView.RTagLayoutView.currentView.manualRender(_.extend({}, paramObj, { 'tagName': tagName }));
                        this.view.currentView.selectTab();
                    },
                    render: function() {
                        if (paramObj && paramObj.dlttag) {
                            Utils.setUrl({
                                url: url,
                                trigger: false,
                                updateTabState: true
                            });
                        }
                        return new SideNavLayoutView(options);
                    }
                });
                if (tagName) {
                    // updating paramObj to check for new queryparam.
                    paramObj = Utils.getUrlState.getQueryParams();
                    if (paramObj && paramObj.dlttag) {
                        return false;
                    }
                    App.rNContent.show(new TagDetailLayoutView(options));
                }
            });
        },
        glossaryDetailPage: function(id) {
            var that = this;
            if (id) {
                require([
                    'views/site/Header',
                    'views/glossary/GlossaryDetailLayoutView',
                    'views/site/SideNavLayoutView'
                ], function(Header, GlossaryDetailLayoutView, SideNavLayoutView) {
                    var paramObj = Utils.getUrlState.getQueryParams(),
                        options = _.extend({ 'guid': id, 'value': paramObj }, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                    that.renderViewIfNotExists(that.getHeaderOptions(Header));
                    that.renderViewIfNotExists({
                        view: App.rSideNav,
                        manualRender: function() {
                            this.view.currentView.RGlossaryLayoutView.currentView.manualRender(options);
                            this.view.currentView.selectTab();
                        },
                        render: function() {
                            return new SideNavLayoutView(options)
                        }
                    });
                    App.rNContent.show(new GlossaryDetailLayoutView(options));
                });
            }
        },
        searchResult: function() {
            var that = this;
            require([
                'views/site/Header',
                'views/site/SideNavLayoutView',
                'views/search/SearchDetailLayoutView',
                'collection/VTagList'
            ], function(Header, SideNavLayoutView, SearchDetailLayoutView, VTagList) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj),
                    tag = new VTagList();
                if (paramObj.tag) {
                    var tagValidate = paramObj.tag,
                        isTagPresent = false;
                    if ((tagValidate.indexOf('*') == -1)) {
                        isTagPresent = classificationDefCollection.fullCollection.some(function(model) {
                            var name = Utils.getName(model.toJSON(), 'name');
                            if (model.get('category') == 'CLASSIFICATION') {
                                return name === tagValidate;
                            }
                            return false;
                        })
                        if (!isTagPresent) {
                            isTagPresent = Enums.addOnClassification.some(function(classificationName) {
                                return classificationName === tagValidate;
                            })
                        }
                        if (!isTagPresent) {
                            tag.url = UrlLinks.classicationApiUrl(tagValidate);
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
                        }
                    }
                    if (tagValidate.indexOf('*') >= 0 || isTagPresent) {
                        renderSearchView();
                    }
                } else {
                    renderSearchView();
                }

                function renderSearchView() {
                    var isinitialView = true,
                        isTypeTagNotExists = false,
                        tempParam = $.extend(true, {}, paramObj);
                    that.renderViewIfNotExists(that.getHeaderOptions(Header));
                    that.renderViewIfNotExists({
                        view: App.rSideNav,
                        manualRender: function() {
                            this.view.currentView.RSearchLayoutView.currentView.manualRender(tempParam);
                        },
                        render: function() {
                            return new SideNavLayoutView(_.extend({ 'value': tempParam }, options));
                        }
                    });
                    App.rSideNav.currentView.selectTab();
                    if (paramObj) {
                        isinitialView = (paramObj.type || (paramObj.dslChecked == "true" ? "" : (paramObj.tag || paramObj.term)) || (paramObj.query ? paramObj.query.trim() : "")).length === 0;
                    }
                    App.rNContent.show(new SearchDetailLayoutView(
                        _.extend({
                            'value': paramObj,
                            'initialView': isinitialView,
                            'isTypeTagNotExists': ((paramObj.type != tempParam.type) || (tempParam.tag != paramObj.tag))
                        }, options)
                    ));
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
                        this.view.currentView.selectTab();
                        if (Utils.getUrlState.isTagTab()) {
                            this.view.currentView.RTagLayoutView.currentView.manualRender();
                        } else if (Utils.getUrlState.isGlossaryTab()) {
                            this.view.currentView.RGlossaryLayoutView.currentView.manualRender(_.extend({ "isTrigger": true, "value": paramObj }));
                        }
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rNContent.show(new AdministratorLayoutView(_.extend({ value: paramObj, guid: null }, options)));
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
                        this.view.currentView.selectTab();
                        if (Utils.getUrlState.isTagTab()) {
                            this.view.currentView.RTagLayoutView.currentView.manualRender();
                        } else if (Utils.getUrlState.isGlossaryTab()) {
                            this.view.currentView.RGlossaryLayoutView.currentView.manualRender(_.extend({ "isTrigger": true, "value": paramObj }));
                        }
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rNContent.show(new DebugMetricsLayoutView(options));
            });
        },
        businessMetadataDetailPage: function(guid) {
            var that = this;
            require(["views/site/Header", "views/site/SideNavLayoutView", "views/business_metadata/BusinessMetadataContainerLayoutView", ], function(Header, SideNavLayoutView, BusinessMetadataContainerLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.selectTab();
                        if (Utils.getUrlState.isTagTab()) {
                            this.view.currentView.RTagLayoutView.currentView.manualRender();
                        } else if (Utils.getUrlState.isGlossaryTab()) {
                            this.view.currentView.RGlossaryLayoutView.currentView.manualRender(_.extend({ "isTrigger": true, "value": paramObj }));
                        }
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });
                App.rNContent.show(new BusinessMetadataContainerLayoutView(_.extend({ guid: guid, value: paramObj }, options)));
            });
        },
        commonAction: function() {
            var that = this;
            require([
                'views/site/Header',
                'views/site/SideNavLayoutView',
                'views/search/SearchDetailLayoutView',
            ], function(Header, SideNavLayoutView, SearchDetailLayoutView) {
                var paramObj = Utils.getUrlState.getQueryParams(),
                    options = _.extend({}, that.preFetchedCollectionLists, that.sharedObj, that.ventObj);
                that.renderViewIfNotExists(that.getHeaderOptions(Header));
                that.renderViewIfNotExists({
                    view: App.rSideNav,
                    manualRender: function() {
                        this.view.currentView.selectTab();
                        if (Utils.getUrlState.isTagTab()) {
                            this.view.currentView.RTagLayoutView.currentView.manualRender();
                        } else if (Utils.getUrlState.isGlossaryTab()) {
                            this.view.currentView.RGlossaryLayoutView.currentView.manualRender(_.extend({ "isTrigger": true, "value": paramObj }));
                        }
                    },
                    render: function() {
                        return new SideNavLayoutView(options);
                    }
                });

                if (Utils.getUrlState.isSearchTab()) {
                    App.rNContent.show(new SearchDetailLayoutView(_.extend({ 'value': paramObj, 'initialView': true }, options)));
                } else {
                    if (App.rNContent.currentView) {
                        App.rNContent.currentView.destroy();
                    } else {
                        App.rNContent.$el.empty();
                    }
                }
            });
        },
        defaultAction: function(actions) {
            // We have no matching route, lets just log what the URL was
            Utils.setUrl({
                url: '#!/search',
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });

            console.log('No route:', actions);
        }
    });
    return AppRouter;
});