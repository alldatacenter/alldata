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

define(["require",
    "backbone",
    "hbs!tmpl/search/GlobalSearchLayoutView_tmpl",
    "utils/Utils",
    "utils/UrlLinks",
    'utils/Globals',
    "jquery-ui"
], function(require, Backbone, GlobalSearchLayoutViewTmpl, Utils, UrlLinks, Globals) {
    "use strict";

    var GlobalSearchLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends GlobalSearchLayoutView */
        {
            _viewName: "GlobalSearchLayoutView",

            template: GlobalSearchLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RSearchLayoutView: "#r_searchLayout"
            },

            /** ui selector cache */
            ui: {
                globalSearch: "[data-id='globalSearch']",
                clearGlobalSearch: "[data-id='clearGlobalSearch']",
                detailSearch: "[data-id='detailSearch']",
                searchLayoutView: ".searchLayoutView"
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.clearGlobalSearch] = function() {
                    this.ui.globalSearch.val("");
                    this.ui.globalSearch.atlasAutoComplete("search");
                    this.ui.clearGlobalSearch.removeClass("in");
                };
                events["click " + this.ui.detailSearch] = function() {
                    this.ui.searchLayoutView.toggleClass("open");
                    if (this.fromDefaultSearch !== true) {
                        $("body").toggleClass("global-search-active");
                    }
                };
                return events;
            },
            /**
             * intialize a new GlobalSearchLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(
                    this,
                    _.pick(
                        options,
                        "value",
                        "closeOnSubmit",
                        "fromDefaultSearch",
                        "initialView",
                        "classificationDefCollection",
                        "entityDefCollection",
                        "typeHeaders",
                        "searchVent",
                        "enumDefCollection",
                        "searchTableColumns"
                    )
                );
                this.bindEvents();
            },
            bindEvents: function() {
                var that = this;
                $("body").on("click", function(e) {
                    if (!that.isDestroyed && that.$(e.target).data("id") !== "detailSearch") {
                        if ($(e.target).parents(".searchLayoutView").length === 0 && that.ui.searchLayoutView.hasClass("open")) {
                            that.ui.searchLayoutView.removeClass("open");
                        }
                    }
                })
            },
            onRender: function() {
                this.initializeGlobalSearch();
                //this.initializeSearchValue();
                this.renderSearchLayoutView();
            },
            onBeforeDestroy: function() {
                this.ui.searchLayoutView.removeClass("open");
                this.ui.globalSearch.atlasAutoComplete("destroy");
            },
            fetchSearchData: function(options) {
                var that = this,
                    request = options.request,
                    response = options.response,
                    term = request.term,
                    data = {},
                    sendResponse = function() {
                        var query = data.query,
                            suggestions = data.suggestions;
                        if (query !== undefined && suggestions !== undefined) {
                            response(data);
                        }
                    };
                $.ajax({
                    url: UrlLinks.searchApiUrl("quick"),
                    contentType: "application/json",
                    data: {
                        query: term,
                        limit: 5,
                        offset: 0
                    },
                    cache: true,
                    success: function(response) {
                        var rData = response.searchResults.entities || [];
                        data.query = { category: "entities", data: rData, order: 1 };
                        sendResponse();
                    }
                });

                $.ajax({
                    url: UrlLinks.searchApiUrl("suggestions"),
                    contentType: "application/json",
                    data: {
                        prefixString: term
                    },
                    cache: true,
                    success: function(response) {
                        var rData = response.suggestions || [];
                        data.suggestions = { category: "suggestions", data: rData, order: 2 };
                        sendResponse();
                    }
                });
            },
            getSearchString: function(str) {
                if (str && str.length) {
                    return str.match(/[+\-&|!(){}[\]^"~*?:/]/g) === null ? str + "*" : str;
                } else {
                    return str;
                }
            },
            triggerBasicSearch: function(query) {
                Utils.setUrl({
                    url: "#!/search/searchResult?query=" + encodeURIComponent(query) + "&searchType=basic",
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            renderSearchLayoutView: function() {
                var that = this;
                require(["views/search/AdvanceSearchLayoutView"], function(AdvanceSearchLayoutView) {
                    that.RSearchLayoutView.show(
                        new AdvanceSearchLayoutView(
                            _.extend({
                                    isHeaderSearch: true,
                                    onSubmit: function() {
                                        that.ui.searchLayoutView.removeClass("open");
                                    }
                                },
                                that.options
                            )
                        )
                    );
                });
            },
            initializeSearchValue: function() {
                if (this.options.value) {
                    var searchValue = CommonViewFunction.generateQueryOfFilter(this.options.value);
                }
            },
            closeSearch: function() {
                this.ui.globalSearch.atlasAutoComplete("close");
            },
            initializeGlobalSearch: function() {
                var that = this;
                this.ui.globalSearch
                    .atlasAutoComplete({
                        minLength: 1,
                        autoFocus: false,
                        search: function() {
                            if (!that.ui.searchLayoutView.hasClass("open")) {
                                $(this)
                                    .siblings("span.fa-search")
                                    .removeClass("fa-search")
                                    .addClass("fa-refresh fa-spin-custom");
                            }
                        },
                        focus: function(event, ui) {
                            return false;
                        },
                        open: function() {
                            $(this)
                                .siblings("span.fa-refresh")
                                .removeClass("fa-refresh fa-spin-custom")
                                .addClass("fa-search");
                        },
                        select: function(event, ui) {
                            var item = ui && ui.item;
                            event.preventDefault();
                            event.stopPropagation();
                            var $el = $(this);
                            if (_.isString(item)) {
                                $el.val(item);
                                $el.data("valSelected", true);
                                that.triggerBasicSearch(item);
                            } else if (_.isObject(item) && item.guid) {
                                Utils.setUrl({
                                    url: "#!/detailPage/" + item.guid,
                                    mergeBrowserUrl: false,
                                    trigger: true
                                });
                            }
                            $el.blur();
                            return true;
                        },
                        source: function(request, response) {
                            if (!that.ui.searchLayoutView.hasClass("open")) {
                                that.fetchSearchData({
                                    request: request,
                                    response: response
                                });
                            }
                        }
                    })
                    .focus(function() {
                        if (!that.ui.searchLayoutView.hasClass("open")) {
                            $(this).atlasAutoComplete("search");
                        }
                    })
                    .keyup(function(event) {
                        if (
                            $(this)
                            .val()
                            .trim() === ""
                        ) {
                            if (event.keyCode == 13) {
                                this.value = "*";
                                that.triggerBasicSearch("*");
                            } else {
                                that.ui.clearGlobalSearch.removeClass("in");
                            }
                        } else {
                            that.ui.clearGlobalSearch.addClass("in");
                            if (event.keyCode == 13) {
                                if ($(this).data("valSelected") !== true) {
                                    that.closeSearch();
                                    that.triggerBasicSearch($(this).val());

                                } else {
                                    $(this).data("valSelected", false);
                                }
                            }
                        }
                    })
                    .atlasAutoComplete("instance")._renderItem = function(ul, searchItem) {
                        if (searchItem) {
                            var data = searchItem.data,
                                searchTerm = this.term,
                                getHighlightedTerm = function(resultStr) {
                                    try {
                                        return resultStr.replace(new RegExp(searchTerm, "gi"), function(foundStr) {
                                            return "<span class='searched-term'>" + foundStr + "</span>";
                                        });
                                    } catch (error) {
                                        return resultStr;
                                    }
                                };
                            if (data) {
                                if (data.length == 0) {
                                    return $("<li class='empty'></li>")
                                        .append("<span class='empty-message'>No " + searchItem.category + " found</span>")
                                        .appendTo(ul);
                                } else {
                                    var items = [];
                                    _.each(data, function(item) {
                                        var li = null;
                                        if (_.isObject(item)) {
                                            item.itemText = Utils.getName(item) + " (" + item.typeName + ")";
                                            var options = {},
                                                table = "";
                                            if (item.serviceType === undefined) {
                                                if (Globals.serviceTypeMap[item.typeName] === undefined && that.entityDefCollection) {
                                                    var defObj = that.entityDefCollection.fullCollection.find({ name: item.typeName });
                                                    if (defObj) {
                                                        Globals.serviceTypeMap[item.typeName] = defObj.get("serviceType");
                                                    }
                                                }
                                            } else if (Globals.serviceTypeMap[item.typeName] === undefined) {
                                                Globals.serviceTypeMap[item.typeName] = item.serviceType;
                                            }
                                            item.serviceType = Globals.serviceTypeMap[item.typeName];
                                            options.entityData = item;
                                            var img = $('<img src="' + Utils.getEntityIconPath(options) + '">').on("error", function(error, s) {
                                                this.src = Utils.getEntityIconPath(_.extend(options, { errorUrl: this.src }));
                                            });

                                            var span = $("<span>" + getHighlightedTerm(item.itemText) + "</span>").prepend(img);
                                            li = $("<li class='with-icon'>").append(span);
                                        } else {
                                            li = $("<li>").append("<span>" + getHighlightedTerm(_.escape(item)) + "</span>");
                                        }
                                        li.data("ui-autocomplete-item", item);
                                        if (searchItem.category) {
                                            items.push(li.attr("aria-label", searchItem.category + " : " + (_.isObject(item) ? item.itemText : item)));
                                        }
                                    });
                                    return ul.append(items);
                                }
                            }
                        }
                    };
            }
        }
    );
    return GlobalSearchLayoutView;
});