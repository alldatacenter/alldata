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
    'hbs!tmpl/site/Header',
    'utils/CommonViewFunction',
    'utils/Globals',
    'utils/Utils',
    'utils/UrlLinks',
    'jquery-ui'
], function(require, tmpl, CommonViewFunction, Globals, Utils, UrlLinks) {
    'use strict';

    var Header = Marionette.LayoutView.extend({
        template: tmpl,
        regions: {},
        templateHelpers: function() {
            return {
                glossaryImportTempUrl: UrlLinks.glossaryImportTempUrl(),
                businessMetadataImportTempUrl: UrlLinks.businessMetadataImportTempUrl(),
                apiDocUrl: UrlLinks.apiDocUrl(),
                isDebugMetricsEnabled: Globals.isDebugMetricsEnabled
            };
        },
        ui: {
            backButton: "[data-id='backButton']",
            menuHamburger: "[data-id='menuHamburger']",
            globalSearch: "[data-id='globalSearch']",
            clearGlobalSearch: "[data-id='clearGlobalSearch']",
            signOut: "[data-id='signOut']",
            administrator: "[data-id='administrator']",
            showDebug: "[data-id='showDebug']",
            uiSwitch: "[data-id='uiSwitch']",
            glossaryImport: "[data-id='glossaryImport']",
            businessMetadataImport: "[data-id='businessMetadataImport']"
        },
        events: function() {
            var events = {};
            events['click ' + this.ui.backButton] = function() {
                Utils.backButtonClick();
            };
            events['click ' + this.ui.clearGlobalSearch] = function() {
                this.ui.globalSearch.val("");
                this.ui.globalSearch.autocomplete("search");
                this.ui.clearGlobalSearch.removeClass("in");
            };
            events['click ' + this.ui.menuHamburger] = function() {
                this.setSearchBoxWidth({
                    updateWidth: function(atlasHeaderWidth) {
                        return $('body').hasClass('full-screen') ? atlasHeaderWidth - 350 : atlasHeaderWidth + 350
                    }
                });
                $('body').toggleClass("full-screen");
            };
            events['click ' + this.ui.signOut] = function() {
                Utils.localStorage.setValue("last_ui_load", "v1");
                var path = Utils.getBaseUrl(window.location.pathname);
                window.location = path + "/logout.html";
            };
            events["click " + this.ui.uiSwitch] = function() {
                var path = Utils.getBaseUrl(window.location.pathname) + "/n/index.html";
                if (window.location.hash.length > 2) {
                    path += window.location.hash;
                }
                window.location.href = path;
            };
            events['click ' + this.ui.administrator] = function() {
                Utils.setUrl({
                    url: "#!/administrator",
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            };
            events['click ' + this.ui.glossaryImport] = function() {
                this.onClickImport(true);
            };
            events['click ' + this.ui.businessMetadataImport] = function() {
                this.onClickImport()
            };
            events['click ' + this.ui.showDebug] = function() {
                Utils.setUrl({
                    url: "#!/debugMetrics",
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            };

            return events;
        },
        initialize: function(options) {
            this.bindEvent();
            this.options = options;
        },
        setSearchBoxWidth: function(options) {
            var atlasHeaderWidth = this.$el.find(".atlas-header").width(),
                minusWidth = (Utils.getUrlState.isDetailPage() || Utils.getUrlState.isBSDetail()) ? 360 : 210;
            if (options && options.updateWidth) {
                atlasHeaderWidth = options.updateWidth(atlasHeaderWidth);
            }
            if (atlasHeaderWidth > minusWidth) {
                this.$el.find(".global-search-container").width(atlasHeaderWidth - minusWidth);
            }
        },
        bindEvent: function() {
            var that = this;
            $(window).resize(function() {
                that.setSearchBoxWidth();
            });
        },
        onRender: function() {
            var that = this;
            if (Globals.userLogedIn.status) {
                that.$('.userName').html(Globals.userLogedIn.response.userName);
            }
            this.initializeGlobalSearch();
        },
        onShow: function() {
            this.setSearchBoxWidth();
        },
        onBeforeDestroy: function() {
            this.ui.globalSearch.atlasAutoComplete("destroy");
        },
        manualRender: function() {
            this.setSearchBoxWidth();
        },
        fetchSearchData: function(options) {
            var that = this,
                request = options.request,
                response = options.response,
                inputEl = options.inputEl,
                term = request.term,
                data = {},
                sendResponse = function() {
                    var query = data.query,
                        suggestions = data.suggestions;
                    if (query !== undefined && suggestions !== undefined) {
                        inputEl.siblings('span.fa-refresh').removeClass("fa-refresh fa-spin-custom").addClass("fa-search");
                        response(data);
                    }
                };
            $.ajax({
                url: UrlLinks.searchApiUrl('quick'),
                contentType: 'application/json',
                data: {
                    "query": term,
                    "limit": 5,
                    "offset": 0
                },
                cache: true,
                success: function(response) {
                    var rData = response.searchResults.entities || [];
                    data.query = { category: "entities", data: rData, order: 1 };
                    sendResponse();
                }
            });

            $.ajax({
                url: UrlLinks.searchApiUrl('suggestions'),
                contentType: 'application/json',
                data: {
                    "prefixString": term
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
                return (str.match(/[+\-&|!(){}[\]^"~*?:/]/g) === null ? (str + "*") : str);
            } else {
                return str;
            }
        },
        triggerBuasicSearch: function(query) {
            Utils.setUrl({
                url: '#!/search/searchResult?query=' + encodeURIComponent(query) + '&searchType=basic',
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        initializeGlobalSearch: function() {
            var that = this;
            this.ui.globalSearch.atlasAutoComplete({
                minLength: 1,
                autoFocus: false,
                search: function() {
                    $(this).siblings('span.fa-search').removeClass("fa-search").addClass("fa-refresh fa-spin-custom");
                },
                select: function(event, ui) {
                    var item = ui && ui.item;
                    event.preventDefault();
                    event.stopPropagation();
                    var $el = $(this);
                    if (_.isString(item)) {
                        $el.val(item);
                        $el.data("valSelected", true);
                        that.triggerBuasicSearch(item);
                    } else if (_.isObject(item) && item.guid) {
                        Utils.setUrl({
                            url: '#!/detailPage/' + item.guid,
                            mergeBrowserUrl: false,
                            trigger: true
                        });
                    }
                    $el.blur();
                    return true;
                },
                source: function(request, response) {
                    that.fetchSearchData({
                        request: request,
                        response: response,
                        inputEl: this.element
                    });
                }
            }).focus(function() {
                $(this).atlasAutoComplete("search");
            }).keyup(function(event) {
                if ($(this).val().trim() === "") {
                    that.ui.clearGlobalSearch.removeClass("in");
                } else {
                    that.ui.clearGlobalSearch.addClass("in");
                    if (event.keyCode == 13) {
                        if ($(this).data("valSelected") !== true) {
                            that.triggerBuasicSearch($(this).val());
                        } else {
                            $(this).data("valSelected", false);
                        }
                    }
                }
            }).atlasAutoComplete("instance")._renderItem = function(ul, searchItem) {
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
                        }
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
                                        table = '';
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
                                    var span = $("<span>" + (getHighlightedTerm(item.itemText)) + "</span>")
                                        .prepend(img);
                                    li = $("<li class='with-icon'>")
                                        .append(span);
                                } else {
                                    li = $("<li>")
                                        .append("<span>" + (getHighlightedTerm(_.escape(item))) + "</span>");
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
        },
        onClickImport: function(isGlossary) {
            var that = this;
            require([
                'views/import/ImportLayoutView'
            ], function(ImportLayoutView) {
                var view = new ImportLayoutView({
                    callback: function() {
                        if (that.options.importVent) {
                            if (isGlossary) {
                                that.options.importVent.trigger("Import:Glossary:Update");
                            } else {
                                that.options.importVent.trigger("Import:BM:Update");
                            }
                        }
                    },
                    isGlossary: isGlossary
                });
            });
        }
    });
    return Header;
});
