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
    "hbs!tmpl/search/tree/BusinessMetadataTreeLayoutView_tmpl",
    "utils/Utils",
    "utils/Messages",
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    "collection/VSearchList",
    "collection/VGlossaryList",
    "utils/Enums",
    "jstree"
], function(require, BusinessMetadataTreeLayoutViewTmpl, Utils, Messages, Globals, UrlLinks, CommonViewFunction, VSearchList, VGlossaryList, Enums) {
    "use strict";

    var BusinessMetadataTreeLayoutView = Marionette.LayoutView.extend({
        template: BusinessMetadataTreeLayoutViewTmpl,

        regions: {},
        ui: {
            //refresh
            refreshTree: '[data-id="refreshTree"]',

            // tree el
            businessMetadataSearchTree: '[data-id="businessMetadataSearchTree"]',

            // Create
            createBusinessMetadata: '[data-id="createBusinessMetadata"]',
            businessMetadataTreeLoader: '[data-id="businessMetadataTreeLoader"]'
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl
            };
        },
        events: function() {
            var events = {},
                that = this;
            // refresh individual tree
            events["click " + this.ui.refreshTree] = function(e) {
                that.changeLoaderState(true);
                that.ui.refreshTree.attr("disabled", true).tooltip("hide");
                e.stopPropagation();
                that.refresh();
            };

            events["click " + this.ui.createBusinessMetadata] = function(e) {
                e.stopPropagation();
                that.triggerUrl("#!/administrator?tabActive=bm");
            };

            return events;
        },
        initialize: function(options) {
            this.options = options;
            _.extend(
                this,
                _.pick(
                    options,
                    "typeHeaders",
                    "guid",
                    "searchVent",
                    "entityDefCollection",
                    "enumDefCollection",
                    "businessMetadataDefCollection",
                    "searchTableColumns",
                    "searchTableFilters",
                    "metricCollection"
                )
            );
            this.bindEvents();
        },
        onRender: function() {
            this.changeLoaderState(true);
            this.renderBusinessMetadataTree();
            this.changeLoaderState(false);
            //this.createBusinessMetadataAction();
        },
        bindEvents: function() {
            var that = this;
            this.listenTo(
                this.businessMetadataDefCollection.fullCollection,
                "reset add remove",
                function() {
                    if (this.ui.businessMetadataSearchTree.jstree(true)) {
                        that.ui.businessMetadataSearchTree.jstree(true).refresh();
                    } else {
                        this.renderBusinessMetadataTree();
                    }
                },
                this
            );
            $("body").on("click", ".businessMetadataPopoverOptions li", function(e) {
                that.$(".businessMetadataPopover").popover("hide");
                that[$(this).find("a").data("fn") + "BusinessMetadata"](e);
            });
        },
        changeLoaderState: function(showLoader) {
            if (showLoader) {
                this.ui.businessMetadataSearchTree.hide();
                this.ui.businessMetadataTreeLoader.show();
            } else {
                this.ui.businessMetadataSearchTree.show();
                this.ui.businessMetadataTreeLoader.hide();
            }
        },
        createBusinessMetadataAction: function() {
            var that = this;
            Utils.generatePopover({
                el: this.$el,
                contentClass: "businessMetadataPopoverOptions",
                popoverOptions: {
                    selector: ".businessMetadataPopover",
                    content: function() {
                        var type = $(this).data("detail"),
                            liString =
                            "<li><i class='fa fa-list-alt'></i><a href='javascript:void(0)' data-fn='onViewEdit'>View/Edit</a></li><li><i class='fa fa-search'></i><a href='javascript:void(0)' data-fn='onSelectedSearch'>Search</a></li>";
                        return "<ul>" + liString + "</ul>";
                    }
                }
            });
        },
        renderBusinessMetadataTree: function() {
            this.generateSearchTree({
                $el: this.ui.businessMetadataSearchTree
            });
        },
        manualRender: function(options) {
            var that = this;
            _.extend(this, options);
            if (Utils.getUrlState.isBMDetailPage() && this.guid) {
                this.ui.businessMetadataSearchTree.jstree(true).select_node(this.guid);
            } else {
                this.ui.businessMetadataSearchTree.jstree(true).deselect_all();
                this.guid = null;
            }
        },
        onNodeSelect: function(nodeData) {
            var that = this,
                options = nodeData.node.original,
                url = "#!/administrator/businessMetadata",
                trigger = true,
                queryParams = Utils.getUrlState.getQueryParams();

            if (options.parent === undefined) {
                url += "/" + options.id;
            }

            if (queryParams && queryParams.from === "bm" && Utils.getUrlState.getQueryUrl().queyParams[0] === url) {
                trigger = false;
            }
            if (trigger) {
                this.triggerUrl(url);
            }

        },
        onViewEditBusinessMetadata: function() {
            var selectedNode = this.ui.businessMetadataSearchTree.jstree("get_selected", true);
            if (selectedNode && selectedNode[0]) {
                selectedNode = selectedNode[0];
                var url = "#!/administrator?tabActive=bm";
                if (selectedNode.parent && selectedNode.original && selectedNode.original.name) {
                    url += "&ns=" + selectedNode.parent + "&nsa=" + selectedNode.original.name;
                    this.triggerUrl(url);
                }
            }
        },
        triggerUrl: function(url) {
            Utils.setUrl({
                url: url,
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        refresh: function(options) {
            var that = this;
            this.businessMetadataDefCollection.fetch({
                silent: true,
                complete: function() {
                    that.businessMetadataDefCollection.fullCollection.comparator = function(model) {
                        return model.get("name").toLowerCase();
                    };
                    that.businessMetadataDefCollection.fullCollection.sort({ silent: true });
                    that.ui.businessMetadataSearchTree.jstree(true).refresh();
                    that.changeLoaderState(false);
                    that.ui.refreshTree.attr("disabled", false);
                }
            });
        },
        getBusinessMetadataTree: function(options) {
            var that = this,
                businessMetadataList = [],
                allCustomFilter = [],
                namsSpaceTreeData = that.businessMetadataDefCollection.fullCollection.models,
                openClassificationNodesState = function(treeDate) {
                    if (treeDate.length == 1) {
                        _.each(treeDate, function(model) {
                            model.state["opeaned"] = true;
                        });
                    }
                },
                generateNode = function(nodeOptions, attrNode) {
                    var attributesNode = attrNode ? null : nodeOptions.get("attributeDefs"),
                        nodeStructure = {
                            text: attrNode ? _.escape(nodeOptions.name) : _.escape(nodeOptions.get("name")),
                            name: attrNode ? _.escape(nodeOptions.name) : _.escape(nodeOptions.get("name")),
                            type: "businessMetadata",
                            id: attrNode ? _.escape(nodeOptions.name) : nodeOptions.get("guid"),
                            icon: attrNode ? "fa fa-file-o" : "fa fa-folder-o",
                            children: [],
                            state: { selected: nodeOptions.get("guid") === that.guid },
                            gType: "BusinessMetadata",
                            model: nodeOptions
                        };
                    return nodeStructure;
                };
            _.each(namsSpaceTreeData, function(filterNode) {
                businessMetadataList.push(generateNode(filterNode));
            });

            var treeView = [{
                icon: "fa fa-folder-o",
                gType: "businessMetadata",
                type: "businessMetadataFolder",
                children: businessMetadataList,
                text: "BusinessMetadata",
                name: "BusinessMetadata",
                state: { opened: true }
            }];
            var customFilterList = treeView;
            return businessMetadataList;
        },
        generateSearchTree: function(options) {
            var $el = options && options.$el,
                type = options && options.type,
                that = this,
                getEntityTreeConfig = function(opt) {
                    return {
                        plugins: ["search", "core", "sort", "conditionalselect", "changed", "wholerow", "node_customize"],
                        conditionalselect: function(node) {
                            var type = node.original.type;
                            if (type == "businessMetadataFolder") {
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
                            default: function(el, node) {
                                var aTag = $(el).find(">a.jstree-anchor");
                                aTag.append("<span class='tree-tooltip'>" + aTag.text() + "</span>");
                                if (node.parent === "#") {
                                    $(el).append('<div class="tools"><i class="fa"></i></div>');
                                } else {
                                    $(el).append('<div class="tools"><i class="fa fa-ellipsis-h businessMetadataPopover" rel="popover"></i></div>');
                                }
                            }
                        },
                        core: {
                            multiple: false,
                            data: function(node, cb) {
                                if (node.id === "#") {
                                    cb(that.getBusinessMetadataTree());
                                }
                            }
                        }
                    };
                };
            $el.jstree(
                    getEntityTreeConfig({
                        type: ""
                    })
                )
                .on("open_node.jstree", function(e, data) {
                    that.isTreeOpen = true;
                })
                .on("select_node.jstree", function(e, data) {
                    that.onNodeSelect(data);
                })
                .on("search.jstree", function(nodes, str, res) {
                    if (str.nodes.length === 0) {
                        $el.jstree(true).hide_all();
                        $el.parents(".panel").addClass("hide");
                    } else {
                        $el.parents(".panel").removeClass("hide");
                    }
                })
                .on("hover_node.jstree", function(nodes, str, res) {
                    var aFilter = that.$("#" + str.node.a_attr.id),
                        filterOffset = aFilter.find(">.jstree-icon").offset();
                    that.$(".tree-tooltip").removeClass("show");
                    setTimeout(function() {
                        if (aFilter.hasClass("jstree-hovered") && ($(":hover").last().hasClass("jstree-hovered") || $(":hover").last().parent().hasClass("jstree-hovered")) && filterOffset.top && filterOffset.left) {
                            aFilter
                                .find(">span.tree-tooltip")
                                .css({
                                    top: "calc(" + filterOffset.top + "px - 45px)",
                                    left: "24px"
                                })
                                .addClass("show");
                        }
                    }, 1200);
                })
                .on("dehover_node.jstree", function(nodes, str, res) {
                    that.$(".tree-tooltip").removeClass("show");
                });
        }
    });
    return BusinessMetadataTreeLayoutView;
});