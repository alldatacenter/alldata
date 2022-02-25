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
    "hbs!tmpl/search/tree/CustomFilterTreeLayoutView_tmpl",
    "utils/Utils",
    "utils/Messages",
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    "collection/VSearchList",
    "collection/VGlossaryList",
    "jstree"
], function(require, CustomFilterTreeLayoutViewTmpl, Utils, Messages, Globals, UrlLinks, CommonViewFunction, VSearchList, VGlossaryList) {
    "use strict";

    var CustomFilterTreeLayoutView = Marionette.LayoutView.extend({

        _viewName: 'CustomFilterTreeLayoutView',

        template: CustomFilterTreeLayoutViewTmpl,

        regions: {

            RSaveSearchBasic: '[data-id="r_saveSearchBasic"]'
        },
        ui: {
            //refresh
            refreshTree: '[data-id="refreshTree"]',
            groupOrFlatTree: '[data-id="groupOrFlatTreeView"]',
            customFilterSearchTree: '[data-id="customFilterSearchTree"]',
            showCustomFilter: '[data-id="showCustomFilter"]',
            customFilterTreeLoader: '[data-id="customFilterTreeLoader"]'
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl
            };
        },
        events: function() {
            var events = {},
                that = this;
            events["click " + this.ui.refreshTree] = function(e) {
                that.changeLoaderState(true);
                that.ui.refreshTree.attr("disabled", true).tooltip("hide");
                var type = $(e.currentTarget).data("type");
                e.stopPropagation();
                that.refreshCustomFilterTree();
            };
            events["click " + this.ui.showCustomFilter] = function(e) {
                that.isBasic = !that.isBasic;
                this.customFilterSwitchBtnUpdate();
            };
            events["click " + this.ui.groupOrFlatTree] = function(e) {
                that.changeLoaderState(true);
                var type = $(e.currentTarget).data("type");
                e.stopPropagation();
                this.isGroupView = !this.isGroupView;
                this.ui.groupOrFlatTree.attr("data-original-title", (this.isGroupView ? "Show all" : "Show type"));
                this.ui.groupOrFlatTree.tooltip('hide');
                this.ui.groupOrFlatTree.find("i").toggleClass("group-tree-deactivate");
                this.ui.groupOrFlatTree.find("span").html(this.isGroupView ? "Show flat tree" : "Show group tree");
                that.ui[type + "SearchTree"].jstree(true).destroy();
                that.fetchCustomFilter();
            };

            return events;
        },
        bindEvents: function() {
            var that = this;
            this.listenTo(
                this.saveSearchBaiscCollection.fullCollection,
                "reset add change remove",
                function() {
                    if (this.ui.customFilterSearchTree.jstree(true)) {
                        this.ui.customFilterSearchTree.jstree(true).refresh();
                    } else {
                        this.renderCustomFilterTree();
                    }
                },
                this
            );
            this.listenTo(
                this.saveSearchAdvanceCollection.fullCollection,
                "reset add change remove",
                function() {
                    if (this.ui.customFilterSearchTree.jstree(true)) {
                        this.ui.customFilterSearchTree.jstree(true).refresh();
                    } else {
                        this.renderCustomFilterTree();
                    }
                },
                this
            );
            this.searchVent.on("Save:Filter", function(data) {
                that.saveAs();
            })
            $('body').on('click', '.customFilterPopoverOptions li', function(e) {
                that.$('.customFilterPopoverOptions').popover('hide');
                that[$(this).find('a').data('fn') + "CustomFilter"](e)
            });
        },
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
                    "metricCollection"
                )
            );
            this.saveSearchBaiscCollection = new VSearchList();
            this.saveSearchCollection = new VSearchList();
            this.saveSearchAdvanceCollection = new VSearchList();
            this.saveSearchCollection.url = UrlLinks.saveSearchApiUrl();
            this.saveSearchBaiscCollection.fullCollection.comparator = function(model) {
                return getModelName(model);
            }
            this.saveSearchAdvanceCollection.fullCollection.comparator = function(model) {
                return getModelName(model);
            }

            function getModelName(model) {
                if (model.get('name')) {
                    return model.get('name').toLowerCase();
                }
            };
            this.bindEvents();
            this.customFilterData = null;
            this.isBasic = true;
            this.customFilterId = null;
            this.isGroupView = true;
        },
        onRender: function() {
            this.changeLoaderState(true);
            this.fetchCustomFilter();
        },
        changeLoaderState: function(showLoader) {
            if (showLoader) {
                this.ui.customFilterSearchTree.hide();
                this.ui.customFilterTreeLoader.show();
            } else {
                this.ui.customFilterSearchTree.show();
                this.ui.customFilterTreeLoader.hide();
            }
        },
        manualRender: function(options) {
            _.extend(this.options, options);

            if (this.options.value === undefined) {
                this.options.value = {};
            }
            if (!this.options.value.isCF) {
                this.ui.customFilterSearchTree.jstree(true).deselect_all();
                this.customFilterId = null;
            }

        },
        renderCustomFilterTree: function() {
            this.generateCustomFilterTree({
                $el: this.ui.customFilterSearchTree
            });
            this.createCustomFilterAction();
        },
        fetchCustomFilter: function() {
            var that = this;
            this.saveSearchCollection.fetch({
                success: function(collection, data) {
                    that.saveSearchBaiscCollection.fullCollection.reset(_.where(data, { searchType: "BASIC" }));
                    that.saveSearchAdvanceCollection.fullCollection.reset(_.where(data, { searchType: "ADVANCED" }));
                    that.changeLoaderState(false);
                    that.ui.refreshTree.attr("disabled", false);
                },
                silent: true
            });
        },
        generateCustomFilterTree: function(options) {
            var $el = options && options.$el,
                that = this,
                getEntityTreeConfig = function(opt) {
                    return {
                        plugins: ["search", "core", "sort", "conditionalselect", "changed", "wholerow", "node_customize"],
                        conditionalselect: function(node) {
                            var type = node.original.type;
                            if (type == "customFilterFolder") {
                                if (node.children.length) {
                                    return false;
                                } else {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        },
                        state: { opened: true },
                        search: {
                            show_only_matches: true,
                            case_sensitive: false
                        },
                        node_customize: {
                            default: function(el) {
                                var aFilter = $(el).find(">a.jstree-anchor");
                                aFilter.append("<span class='tree-tooltip'>" + _.escape(aFilter.text()) + "</span>");
                                if ($(el).find(".fa-ellipsis-h").length === 0) {
                                    $(el).append('<div class="tools"><i class="fa fa-ellipsis-h customFilterPopover" rel="popover"></i></div>');
                                }
                            }
                        },
                        core: {
                            multiple: false,
                            data: function(node, cb) {
                                if (node.id === "#") {
                                    cb(that.getCustomFilterTree());
                                }
                            }
                        }
                    };
                };

            $el.jstree(
                getEntityTreeConfig({
                    type: ""
                })
            ).on("open_node.jstree", function(e, data) {
                that.isTreeOpen = true;
            }).on("select_node.jstree", function(e, data) {
                that.onNodeSelect(data);
            }).on("search.jstree", function(nodes, str, res) {
                if (str.nodes.length === 0) {
                    $el.jstree(true).hide_all();
                    $el.parents(".panel").addClass("hide");
                } else {
                    $el.parents(".panel").removeClass("hide");
                }
            }).on("hover_node.jstree", function(nodes, str, res) {
                var aFilter = that.$("#" + str.node.a_attr.id),
                    filterOffset = aFilter.find(">.jstree-icon").offset();
                that.$(".tree-tooltip").removeClass("show");
                setTimeout(function() {
                    if (aFilter.hasClass("jstree-hovered") && filterOffset.top && filterOffset.left) {
                        aFilter.find(">span.tree-tooltip").css({
                            top: "calc(" + filterOffset.top + "px - 45px)",
                            left: "24px"
                        }).addClass("show");
                    }
                }, 1200);
            }).on("dehover_node.jstree", function(nodes, str, res) {
                that.$(".tree-tooltip").removeClass("show");
            });
        },
        createCustomFilterAction: function() {
            var that = this;
            Utils.generatePopover({
                el: this.$el,
                contentClass: 'customFilterPopoverOptions',
                popoverOptions: {
                    selector: '.customFilterPopover',
                    content: function() {
                        var type = $(this).data('detail'),
                            liString = "";
                        liString = "<li data-type=" + type + " class='listTerm'><i class='fa fa-pencil'></i><a href='javascript:void(0)' data-fn='rename'>Rename</a></li>" +
                            "<li data-type=" + type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='delete'>Delete</a></li>"

                        return "<ul>" + liString + "</ul>";
                    }
                }
            });
        },
        customFilterSwitchBtnUpdate: function() {
            var that = this,
                getTreeData, displayText;
            that.ui.showCustomFilter.attr("data-original-title", (that.isBasic ? "Show Advanced search" : "Show Basic search"));
            that.ui.showCustomFilter.tooltip('hide');
            that.ui.showCustomFilter.find("i").toggleClass("switch-button");
            that.ui.customFilterSearchTree.jstree(true).refresh();
        },
        getCustomFilterTree: function(options) {
            var that = this,
                customFilterBasicList = [],
                customFilterAdvanceList = [],
                allCustomFilter = [],
                customFilterBasicTreeData = that.saveSearchBaiscCollection.fullCollection.models,
                customFilterAdvanceTreeData = that.saveSearchAdvanceCollection.fullCollection.models,
                openClassificationNodesState = function(treeDate) {
                    if (treeDate.length == 1) {
                        _.each(treeDate, function(model) {
                            model.state['opeaned'] = true;
                        })
                    }
                },
                generateNode = function(nodeOptions) {
                    var searchType = nodeOptions.get('searchType');
                    var nodeStructure = {
                        text: _.escape(nodeOptions.get('name')),
                        name: _.escape(nodeOptions.get('name')),
                        type: "customFilter",
                        id: nodeOptions.get('guid'),
                        icon: (searchType === 'BASIC' ? "fa fa-circle-thin basic-tree" : "fa fa-circle-thin advance-tree"),
                        gType: "CustomFilter",
                        model: nodeOptions
                    }
                    return nodeStructure;

                }
            that.customFilterId = null;
            _.each(customFilterBasicTreeData, function(filterNode) {
                customFilterBasicList.push(generateNode(filterNode));
                allCustomFilter.push(generateNode(filterNode));
            });
            _.each(customFilterAdvanceTreeData, function(filterNode) {
                customFilterAdvanceList.push(generateNode(filterNode));
                allCustomFilter.push(generateNode(filterNode));
            });

            var treeView = [{
                icon: "fa fa-folder-o",
                gType: "customFilter",
                type: "customFilterFolder",
                children: customFilterBasicList,
                text: "Basic Search",
                name: "Basic Search",
                state: { opened: true }
            }, {
                icon: "fa fa-folder-o",
                gType: "customFilter",
                type: "customFilterFolder",
                children: customFilterAdvanceList,
                text: "Advanced Search",
                name: "Advanced Search",
                state: { opened: true }
            }];
            var customFilterList = that.isGroupView ? treeView : allCustomFilter;
            return customFilterList;
        },
        onNodeSelect: function(nodeData) {
            var that = this,
                options = nodeData.node.original,
                selectedNodeId = options.id;
            if (that.customFilterId != selectedNodeId) {
                that.customFilterId = selectedNodeId;
                if (options && options.model) {
                    var searchParameters = options.model.get('searchParameters'),
                        searchType = options.model.get('searchType'),
                        params = CommonViewFunction.generateUrlFromSaveSearchObject({
                            value: { "searchParameters": searchParameters },
                            classificationDefCollection: that.classificationDefCollection,
                            entityDefCollection: that.entityDefCollection
                        });
                    searchType === 'ADVANCED' ? that.isBasic = false : that.isBasic = true;
                    if (searchType === 'ADVANCED') {
                        Globals.advanceSearchData.searchByQuery = searchParameters.query;
                        Globals.advanceSearchData.searchByType = searchParameters.typeName;
                    }
                    _.extend({}, this.options.value, params);
                    // Utils.notifyInfo({
                    //     content: "Saved values are selected."
                    // })

                    Utils.setUrl({
                        url: '#!/search/searchResult',
                        urlParams: _.extend({}, { 'searchType': that.isBasic ? 'basic' : 'dsl', 'isCF': true }, params),
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }

            } else {
                that.customFilterId = null;
                that.ui.customFilterSearchTree.jstree(true).deselect_all(true);
                that.showDefaultPage();
            }
        },
        showDefaultPage: function() {
            Utils.setUrl({
                url: '#!/search',
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        getValue: function() {
            return this.options.value;
        },
        callSaveModalLayoutView: function(options) {
            require([
                'views/search/save/SaveModalLayoutView'
            ], function(SaveModalLayoutView) {
                new SaveModalLayoutView(options);
            });
        },
        renameCustomFilter: function(opt) {
            var that = this,
                selectednode = that.ui.customFilterSearchTree.jstree("get_selected", true),
                options = selectednode[0].original;
            if (options && options.model.attributes) {
                var that = this;
                require([
                    'views/search/save/SaveModalLayoutView'
                ], function(SaveModalLayoutView) {
                    new SaveModalLayoutView({ 'rename': true, 'selectedModel': options.model.clone(), 'collection': that.isBasic ? that.saveSearchBaiscCollection.fullCollection : that.saveSearchAdvanceCollection.fullCollection, 'getValue': that.getValue, 'isBasic': that.isBasic });
                });
            }
        },
        deleteCustomFilter: function(opt) {
            var that = this,
                selectednode = that.ui.customFilterSearchTree.jstree("get_selected", true),
                options = selectednode[0].original;
            if (options && options.model) {
                var that = this;
                var notifyObj = {
                    modal: true,
                    html: true,
                    text: Messages.conformation.deleteMessage + "<b>" + _.escape(options.model.get('name')) + "</b>" + " ?",
                    ok: function(obj) {
                        that.notificationModal = obj;
                        obj.showButtonLoader();
                        that.onDeleteNotifyOk(options);
                    },
                    okCloses: false,
                    cancel: function(argument) {}
                }
                Utils.notifyConfirm(notifyObj);
            }
        },
        onDeleteNotifyOk: function(options) {
            var that = this;
            options.model.urlRoot = UrlLinks.saveSearchApiUrl();
            if (options.model) {
                options.model.id = options.model.get("guid");
                options.model.idAttribute = "guid";
                options.model.destroy({
                    wait: true,
                    success: function(model, data) {
                        that.showDefaultPage();
                        Utils.notifySuccess({
                            content: options.model.attributes.name + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                        });
                    },
                    complete: function() {
                        that.notificationModal.hideButtonLoader();
                        that.notificationModal.remove();
                    }
                });
            } else {
                Utils.notifyError({
                    content: Messages.defaultErrorMessage
                });
            }
        },
        saveAs: function(e) {
            var value = this.getValue();
            if (value && (value.type || value.tag || value.query || value.term)) {
                value.searchType == "basic" ? this.isBasic = true : this.isBasic = false;
                var urlObj = Utils.getUrlState.getQueryParams();
                if (urlObj) {
                    // includeDE value in because we need to send "true","false" to the server.
                    urlObj.includeDE = urlObj.includeDE == "true" ? true : false;
                    urlObj.excludeSC = urlObj.excludeSC == "true" ? true : false;
                    urlObj.excludeST = urlObj.excludeST == "true" ? true : false;
                }
                this.customFilterSwitchBtnUpdate();
                this.callSaveModalLayoutView({
                    'collection': this.isBasic ? this.saveSearchBaiscCollection.fullCollection : this.saveSearchAdvanceCollection.fullCollection,
                    getValue: function() {
                        return _.extend({}, value, urlObj);
                    },
                    'isBasic': this.isBasic
                });
            } else {
                Utils.notifyInfo({
                    content: Messages.search.favoriteSearch.notSelectedSearchFilter
                })
            }
        },
        refreshCustomFilterTree: function() {
            this.fetchCustomFilter();
        }

    });
    return CustomFilterTreeLayoutView;
});