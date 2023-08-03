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
    "hbs!tmpl/search/tree/RelationshipLayoutView_tmpl",
    "utils/Utils",
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    'utils/Enums',
    "jstree"
], function(require, RelationshipLayoutViewTmpl, Utils, Globals, UrlLinks, CommonViewFunction, Enums) {
    "use strict";

    var RelationshipSearchTreeLayoutView = Marionette.LayoutView.extend({
        template: RelationshipLayoutViewTmpl,

        regions: {},
        ui: {
            refreshTree: '[data-id="refreshTree"]',
            relationshipSearchTree: '[data-id="relationshipSearchTree"]',
            relationshipTreeLoader: '[data-id="relationshipTreeLoader"]'
        },
        templateHelpers: function() {
            return {};
        },
        events: function() {
            var events = {},
                that = this;
            events["click " + this.ui.refreshTree] = function(e) {
                that.changeLoaderState(true);
                that.ui.refreshTree.attr("disabled", true).tooltip("hide");
                e.stopPropagation();
                that.refresh();
            };
            return events;
        },
        bindEvents: function() {
            var that = this;
            this.listenTo(this.relationshipDefCollection, 'reset', function() {
                that.relationshipTreeRefresh();
                that.changeLoaderState(false);
                that.ui.refreshTree.attr("disabled", false);
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
                    "relationshipDefCollection",
                    "searchTableColumns",
                    "searchTableFilters",
                    "metricCollection"
                )
            );
            this.relationshipId = null;
            this.bindEvents();
        },
        onRender: function() {
            this.changeLoaderState(true);
            this.renderRelationshipTree();
            this.changeLoaderState(false);
        },
        changeLoaderState: function(showLoader) {
            if (showLoader) {
                this.ui.relationshipSearchTree.hide();
                this.ui.relationshipTreeLoader.show();
            } else {
                this.ui.relationshipSearchTree.show();
                this.ui.relationshipTreeLoader.hide();
            }
        },
        fetchCollection: function(e) {
            this.relationshipDefCollection.fetch({ reset: true });
        },
        relationshipTreeRefresh: function() {
            if (this.ui.relationshipSearchTree.jstree(true)) {
                this.ui.relationshipSearchTree.jstree(true).refresh();
            } else {
                this.renderRelationshipTree();
            }
        },
        renderRelationshipTree: function() {
            var that = this;
            this.generateSearchTree({
                $el: that.ui.relationshipSearchTree
            });
        },
        manualRender: function(options) {
            var that = this;
            _.extend(this.options, options);
            if (this.options.value === undefined) {
                this.options.value = {};
            }
            if (!this.options.value.relationshipName) {
                this.ui.relationshipSearchTree.jstree(true).deselect_all();
                this.relationshipId = null;
            } else {
                if (that.options.value.relationshipName) {
                    this.fromManualRender = true;
                    if (this.relationshipId) {
                        this.ui.relationshipSearchTree.jstree(true).deselect_node(this.relationshipId);
                    }
                    //this.relationshipId = Globals[that.options.value.relationshipName].guid;
                    this.ui.relationshipSearchTree.jstree(true).select_node(this.relationshipId);
                } else if (that.options.value.relationshipName !== this.relationshipId) {
                    var dataFound = this.relationshipDefCollection.fullCollection.find(function(obj) {
                        return obj.get("name") === that.options.value.relationshipName;
                    });
                    if (dataFound) {
                        if ((this.relationshipId && this.relationshipId !== dataFound.get("guid")) || this.relationshipId === null) {
                            if (this.relationshipId) {
                                this.ui.relationshipSearchTree.jstree(true).deselect_node(this.relationshipId);
                            }
                            this.fromManualRender = true;
                            this.relationshipId = dataFound.get("guid");
                            this.ui.relationshipSearchTree.jstree(true).select_node(dataFound.get("guid"));
                        }
                    }
                }
            }
        },
        refresh: function() {
            this.relationshipId = null;
            this.fetchCollection({ reset: true });
        },
        onNodeSelect: function(options) {
            var that = this,
                type,
                name = options.node.original.name,
                selectedNodeId = options.node.id,
                getUrl = Utils.getUrlState.isRelationTab(),
                params = {
                    searchType: "basic",
                    dslChecked: false
                },
                values = this.options.value;
                Globals.fromRelationshipSearch = true;
            if (!getUrl) { that.relationshipId = null; }
            if (that.relationshipId != selectedNodeId) {
                that.relationshipId = selectedNodeId;
                params["relationshipName"] = name;
            } else {
                that.relationshipId = params["relationshipName"] = null;
                that.ui.relationshipSearchTree.jstree(true).deselect_all(true);
                if (!that.options.value.relationshipName) {
                    that.showDefaultPage();
                    return;
                }
            }
            if(values && (values.type || values.tag)){
                values.attributes = null;
                values.uiParameters = null;
            }
            var searchParam = _.extend({}, values, params);
            this.triggerSearch(searchParam);
        },
        triggerSearch: function(params, url) {
            var searchUrl = url ? url : '#!/relationship/relationshipSearchResult';
            Utils.setUrl({
                url: searchUrl,
                urlParams: params,
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        getRelationshipTree: function(options) {
            var that = this,
                collection = (options && options.collection) || this.relationshipDefCollection.fullCollection,
                listofNodes = [],
                relationshipName = that.options && that.options.value ? that.options.value.relationshipName : "",
                generateNode = function(nodeOptions, options) {
                    var nodeStructure = {
                        text: _.escape(nodeOptions.name),
                        name: _.escape(nodeOptions.name),
                        id: nodeOptions.model.get("guid"),
                        icon: "fa fa-link",
                        gType: "Relationship",
                        state: {
                            disabled: false,
                            selected: (nodeOptions.name === relationshipName) ? true : false,
                            opened: true
                        }
                    }
                    return nodeStructure;
                }
            collection.each(function(model) {
                var nodeDetails = {
                        name: _.escape(model.get('name')),
                        model: model
                    },
                    getParentNodeDetails = generateNode(nodeDetails, model);
                listofNodes.push(getParentNodeDetails);
            });
            return listofNodes;
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
                            if (type === "RELATIONSHIP") {
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
                                    cb(that.getRelationshipTree());
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
                if (that.fromManualRender !== true) {
                    that.onNodeSelect(data);
                } else {
                    that.fromManualRender = false;
                }
            }).on("search.jstree", function(nodes, str, res) {
                if (str.nodes.length === 0) {
                    $el.jstree(true).hide_all();
                    $el.parents(".panel").addClass("hide");
                } else {
                    $el.parents(".panel").removeClass("hide");
                }
            }).on("hover_node.jstree", function(nodes, str, res) {
                var aTag = that.$("#" + str.node.a_attr.id),
                    tagOffset = aTag.find(">.jstree-icon").offset();
                that.$(".tree-tooltip").removeClass("show");
                setTimeout(function() {
                    if (aTag.hasClass("jstree-hovered") && tagOffset.top && tagOffset.left) {
                        aTag.find(">span.tree-tooltip").css({
                            top: "calc(" + tagOffset.top + "px - 45px)",
                            left: "24px"
                        }).addClass("show");
                    }
                }, 1200);
            }).on("dehover_node.jstree", function(nodes, str, res) {
                that.$(".tree-tooltip").removeClass("show");
            });
        }
    });
    return RelationshipSearchTreeLayoutView;
});