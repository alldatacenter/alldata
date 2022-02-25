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
    "hbs!tmpl/search/tree/EntityTreeLayoutView_tmpl",
    "utils/Utils",
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    "collection/VSearchList",
    "collection/VGlossaryList",
    'utils/Enums',
    "jstree"
], function(require, EntityLayoutViewTmpl, Utils, Globals, UrlLinks, CommonViewFunction, VSearchList, VGlossaryList, Enums) {
    "use strict";

    var EntityTreeLayoutview = Marionette.LayoutView.extend({
        template: EntityLayoutViewTmpl,

        regions: {},
        ui: {
            //refresh
            refreshTree: '[data-id="refreshTree"]',
            groupOrFlatTree: '[data-id="groupOrFlatTreeView"]',

            // tree el
            entitySearchTree: '[data-id="entitySearchTree"]',

            // Show/hide empty values in tree
            showEmptyServiceType: '[data-id="showEmptyServiceType"]',
            entityTreeLoader: '[data-id="entityTreeLoader"]',
            importBusinessMetadata: "[data-id='importBusinessMetadata']",
            downloadBusinessMetadata: "[data-id='downloadBusinessMetadata']"
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl,
                importTmplUrl: UrlLinks.businessMetadataImportTempUrl()
            };
        },
        events: function() {
            var events = {},
                that = this;
            events["click " + this.ui.refreshTree] = function(e) {
                that.changeLoaderState(true);
                this.ui.refreshTree.attr("disabled", true).tooltip("hide");
                var type = $(e.currentTarget).data("type");
                e.stopPropagation();
                that.ui[type + "SearchTree"].jstree(true).destroy();
                that.refresh({ type: type });
            };

            // show and hide entities and classifications with 0 numbers
            events["click " + this.ui.showEmptyServiceType] = function(e) {
                e.stopPropagation();
                this.isEmptyServicetype = !this.isEmptyServicetype;
                this.entitySwitchBtnUpdate();
            };
            // refresh individual tree
            events["click " + this.ui.groupOrFlatTree] = function(e) {
                var type = $(e.currentTarget).data("type");
                e.stopPropagation();
                this.isGroupView = !this.isGroupView;
                this.ui.groupOrFlatTree.tooltip('hide');
                this.ui.groupOrFlatTree.find("i").toggleClass("fa-sitemap fa-list-ul");
                this.ui.groupOrFlatTree.find("span").html(this.isGroupView ? "Show flat tree" : "Show group tree");
                that.ui[type + "SearchTree"].jstree(true).destroy();
                that.renderEntityTree();
            };
            events["click " + this.ui.importBusinessMetadata] = function(e) {
                e.stopPropagation();
                that.onClickImportBusinessMetadata();
            };
            events["click " + this.ui.downloadBusinessMetadata] = function(e) {
                e.stopPropagation();
            };

            return events;
        },
        bindEvents: function() {
            var that = this;
            $('body').on('click', '.entityPopoverOptions li', function(e) {
                that.$('.entityPopover').popover('hide');
                that[$(this).find('a').data('fn') + "Entity"](e)
            });
            this.searchVent.on("Entity:Count:Update", function(options) {
                that.changeLoaderState(true);
                var opt = options || {};
                if (opt && !opt.metricData) {
                    that.metricCollection.fetch({
                        complete: function() {
                            that.entityCountObj = _.first(that.metricCollection.toJSON());
                            that.fromManualRender = true;
                            that.ui.entitySearchTree.jstree(true).refresh();
                            that.changeLoaderState(false);
                        }
                    });
                } else {
                    that.entityCountObj = opt.metricData;
                    that.ui.entitySearchTree.jstree(true).refresh();
                    that.changeLoaderState(false);
                }
            });
            this.classificationAndMetricEvent.on("metricCollection:Update", function(options) {
                that.changeLoaderState(true);
                that.ui.refreshTree.attr("disabled", true).tooltip("hide");
                that.ui["entitySearchTree"].jstree(true).destroy();
                that.refresh({ type: "entity", apiCount: 0 });
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
                    "metricCollection",
                    "classificationAndMetricEvent"
                )
            );
            this.bindEvents();
            this.entityCountObj = _.first(this.metricCollection.toJSON()) || { entity: { entityActive: {}, entityDeleted: {} }, tag: { tagEntities: {} } };
            this.isEmptyServicetype = true;
            this.entityTreeData = {};
            this.typeId = null;
            this.isGroupView = true;
        },
        onRender: function() {
            this.changeLoaderState(true);
            this.renderEntityTree();
            this.createEntityAction();
            this.changeLoaderState(false);
        },
        changeLoaderState: function(showLoader) {
            if (showLoader) {
                this.ui.entitySearchTree.hide();
                this.ui.entityTreeLoader.show();
            } else {
                this.ui.entitySearchTree.show();
                this.ui.entityTreeLoader.hide();
            }
        },
        createEntityAction: function() {
            var that = this;
            Utils.generatePopover({
                el: this.$el,
                contentClass: 'entityPopoverOptions',
                popoverOptions: {
                    selector: '.entityPopover',
                    content: function() {
                        var type = $(this).data('detail'),
                            liString = "<li><i class='fa fa-search'></i><a href='javascript:void(0)' data-fn='onSelectedSearch'>Search</a></li>"
                        return "<ul>" + liString + "</ul>";
                    }
                }
            });
        },
        renderEntityTree: function() {
            var that = this;
            this.generateSearchTree({
                $el: that.ui.entitySearchTree
            });
        },
        onSearchEntityNode: function(showEmptyType) {
            // on tree search by text, searches for all entity node, called by searchfilterBrowserLayoutView.js
            this.isEmptyServicetype = showEmptyType;
            this.entitySwitchBtnUpdate();
        },
        entitySwitchBtnUpdate: function() {
            this.ui.showEmptyServiceType.attr("data-original-title", (this.isEmptyServicetype ? "Show" : "Hide") + " empty service types");
            this.ui.showEmptyServiceType.tooltip('hide');
            this.ui.showEmptyServiceType.find("i").toggleClass("fa-toggle-on fa-toggle-off");
            this.ui.entitySearchTree.jstree(true).refresh();
        },
        manualRender: function(options) {
            var that = this;
            _.extend(this.options, options);
            if (this.options.value === undefined) {
                this.options.value = {};
            }
            if (!this.options.value.type) {
                this.ui.entitySearchTree.jstree(true).deselect_all();
                this.typeId = null;
            } else {
                if (that.options.value.type === "_ALL_ENTITY_TYPES" && this.typeId !== "_ALL_ENTITY_TYPES") {
                    this.fromManualRender = true;
                    if (this.typeId) {
                        this.ui.entitySearchTree.jstree(true).deselect_node(this.typeId);
                    }
                    this.typeId = Globals[that.options.value.type].guid;
                    this.ui.entitySearchTree.jstree(true).select_node(this.typeId);
                } else if (this.typeId !== "_ALL_ENTITY_TYPES" && that.options.value.type !== this.typeId) {
                    var dataFound = this.typeHeaders.fullCollection.find(function(obj) {
                        return obj.get("name") === that.options.value.type
                    });
                    if (dataFound) {
                        if ((this.typeId && this.typeId !== dataFound.get("guid")) || this.typeId === null) {
                            if (this.typeId) {
                                this.ui.entitySearchTree.jstree(true).deselect_node(this.typeId);
                            }
                            this.fromManualRender = true;
                            this.typeId = dataFound.get("guid");
                            this.ui.entitySearchTree.jstree(true).select_node(dataFound.get("guid"));
                        }
                    }
                }
            }
        },
        onNodeSelect: function(options) {
            var that = this,
                type,
                name = options.node.original.name,
                selectedNodeId = options.node.id,
                typeValue = null,
                params = {
                    searchType: "basic",
                    dslChecked: false
                };
            if (this.options.value) {
                if (this.options.value.type) {
                    params["type"] = this.options.value.type;
                }
                if (this.options.value.isCF) {
                    this.options.value.isCF = null;
                }
                if (this.options.value.entityFilters) {
                    params["entityFilters"] = null;
                }
            }
            var getUrl = Utils.getUrlState.isSearchTab();
            if (!getUrl) { that.typeId = null; }
            if (that.typeId != selectedNodeId) {
                that.typeId = selectedNodeId;
                typeValue = name;
                params['type'] = typeValue;
            } else {
                that.typeId = params["type"] = null;
                that.ui.entitySearchTree.jstree(true).deselect_all(true);
                if (!that.options.value.type && !that.options.value.tag && !that.options.value.term && !that.options.value.query && !this.options.value.udKeys && !this.options.value.ugLabels) {
                    that.showDefaultPage();
                    return;
                }
            }
            var searchParam = _.extend({}, this.options.value, params);
            this.triggerSearch(searchParam);
        },
        showDefaultPage: function() {
            Utils.setUrl({
                url: '!/search',
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        triggerSearch: function(params, url) {
            var serachUrl = url ? url : '#!/search/searchResult';
            Utils.setUrl({
                url: serachUrl,
                urlParams: params,
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        onSelectedSearchEntity: function() {
            var params = {
                searchType: "basic",
                dslChecked: false,
                type: this.options.value.type
            };
            this.triggerSearch(params);
        },
        getEntityTree: function() {
            var that = this,
                serviceTypeArr = [],
                serviceTypeWithEmptyEntity = [],
                type = "ENTITY",
                entityTreeContainer = this.ui.entitytreeStructure,
                generateTreeData = function(data) {
                    that.typeHeaders.fullCollection.each(function(model) {
                        var totalCount = 0,
                            serviceType = model.toJSON().serviceType,
                            isSelected = false,
                            categoryType = model.toJSON().category,
                            generateServiceTypeArr = function(entityCountArr, serviceType, children, entityCount) {
                                if (that.isGroupView) {
                                    if (entityCountArr[serviceType]) {
                                        entityCountArr[serviceType]["children"].push(children);
                                        entityCountArr[serviceType]["totalCounter"] = +entityCountArr[serviceType]["totalCounter"] + entityCount;
                                    } else {
                                        entityCountArr[serviceType] = [];
                                        entityCountArr[serviceType]["name"] = serviceType;
                                        entityCountArr[serviceType]["children"] = [];
                                        entityCountArr[serviceType]["children"].push(children);
                                        entityCountArr[serviceType]["totalCounter"] = entityCount;
                                    }
                                } else {
                                    entityCountArr.push(children)
                                }
                            };
                        if (!serviceType) {
                            serviceType = "other_types";
                        }
                        if (categoryType == "ENTITY") {
                            var entityCount = that.entityCountObj ?
                                (that.entityCountObj.entity.entityActive[model.get("name")] || 0) +
                                (that.entityCountObj.entity.entityDeleted[model.get("name")] || 0) : 0,
                                modelname = entityCount ? model.get("name") + " (" + _.numberFormatWithComma(entityCount) + ")" : model.get("name");
                            if (that.options.value) {
                                isSelected = that.options.value.type ? that.options.value.type == model.get("name") : false;
                                if (!that.typeId) {
                                    that.typeId = isSelected ? model.get("guid") : null;
                                }
                            }

                            var children = {
                                text: _.escape(modelname),
                                name: model.get("name"),
                                type: model.get("category"),
                                gType: "Entity",
                                guid: model.get("guid"),
                                id: model.get("guid"),
                                model: model,
                                parent: "#",
                                icon: "fa fa-file-o",
                                state: {
                                    disabled: false,
                                    selected: isSelected
                                },
                            };

                            entityCount = _.isNaN(entityCount) ? 0 : entityCount;
                            generateServiceTypeArr(serviceTypeArr, serviceType, children, entityCount);
                            if (entityCount) {
                                generateServiceTypeArr(serviceTypeWithEmptyEntity, serviceType, children, entityCount);
                            }
                        }
                    });

                    var serviceTypeData = that.isEmptyServicetype ? serviceTypeWithEmptyEntity : serviceTypeArr;
                    if (that.isGroupView) {
                        var serviceDataWithRootEntity = pushRootEntityToJstree.call(that, 'group', serviceTypeData);
                        return getParentsData.call(that, serviceDataWithRootEntity);
                    } else {
                        return pushRootEntityToJstree.call(that, null, serviceTypeData);
                    }
                },
                pushRootEntityToJstree = function(type, data) {
                    var rootEntity = Globals[Enums.addOnEntities[0]];
                    var isSelected = this.options.value && this.options.value.type ? this.options.value.type == rootEntity.name : false;
                    var rootEntityNode = {
                        text: _.escape(rootEntity.name),
                        name: rootEntity.name,
                        type: rootEntity.category,
                        gType: "Entity",
                        guid: rootEntity.guid,
                        id: rootEntity.guid,
                        model: rootEntity,
                        parent: "#",
                        icon: "fa fa-file-o",
                        state: {
                            // disabled: entityCount == 0 ? true : false,
                            selected: isSelected
                        },
                    };
                    if (type === 'group') {
                        if (data.other_types === undefined) {
                            data.other_types = { name: "other_types", children: [] };
                        }
                        data.other_types.children.push(rootEntityNode);
                    } else {
                        data.push(rootEntityNode);
                    }
                    return data;
                },
                getParentsData = function(data) {
                    var parents = Object.keys(data),
                        treeData = [],
                        withoutEmptyServiceType = [],
                        treeCoreData = null,
                        openEntityNodesState = function(treeDate) {
                            if (treeDate.length == 1) {
                                _.each(treeDate, function(model) {
                                    model.state = { opened: true }
                                })
                            }
                        },
                        generateNode = function(children) {
                            var nodeStructure = {
                                text: "Service Types",
                                children: children,
                                icon: "fa fa-folder-o",
                                type: "ENTITY",
                                state: { opened: true },
                                parent: "#"
                            }
                            return nodeStructure;
                        };
                    for (var i = 0; i < parents.length; i++) {

                        var checkEmptyServiceType = false,
                            getParrent = data[parents[i]],
                            totalCounter = getParrent.totalCounter,
                            textName = getParrent.totalCounter ? parents[i] + " (" + _.numberFormatWithComma(totalCounter) + ")" : parents[i],
                            parent = {
                                icon: "fa fa-folder-o",
                                type: type,
                                gType: "ServiceType",
                                children: getParrent.children,
                                text: _.escape(textName),
                                name: data[parents[i]].name,
                                id: i,
                                state: { opened: true }
                            };
                        if (that.isEmptyServicetype) {
                            if (data[parents[i]].totalCounter == 0) {
                                checkEmptyServiceType = true;
                            }
                        }
                        treeData.push(parent);
                        if (!checkEmptyServiceType) {
                            withoutEmptyServiceType.push(parent);
                        }
                    }
                    that.entityTreeData = {
                        withoutEmptyServiceTypeEntity: generateNode(withoutEmptyServiceType),
                        withEmptyServiceTypeEntity: generateNode(treeData)
                    };

                    treeCoreData = that.isEmptyServicetype ? withoutEmptyServiceType : treeData;

                    openEntityNodesState(treeCoreData);
                    return treeCoreData;
                };
            return generateTreeData();
        },
        generateSearchTree: function(options) {
            var $el = options && options.$el,
                data = options && options.data,
                type = options && options.type,
                that = this,
                getEntityTreeConfig = function(opt) {
                    return {
                        plugins: ["search", "core", "sort", "conditionalselect", "changed", "wholerow", "node_customize"],
                        conditionalselect: function(node) {
                            var type = node.original.type;
                            if (type == "ENTITY" || type == "GLOSSARY") {
                                if (node.children.length || type == "GLOSSARY") {
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
                                var aType = $(el).find(">a.jstree-anchor");
                                aType.append("<span class='tree-tooltip'>" + aType.text() + "</span>");
                                if ($(el).find(".fa-ellipsis-h").length === 0) {
                                    $(el).append('<div class="tools"><i class="fa fa-ellipsis-h entityPopover" rel="popover"></i></div>');
                                }
                            }
                        },
                        core: {
                            multiple: false,
                            data: function(node, cb) {
                                if (node.id === "#") {
                                    cb(
                                        that.getEntityTree()
                                    );
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
                if (!that.fromManualRender) {
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
                var aType = that.$("#" + str.node.a_attr.id),
                    typeOffset = aType.find(">.jstree-icon").offset();
                that.$(".tree-tooltip").removeClass("show");
                setTimeout(function() {
                    if (aType.hasClass("jstree-hovered") && typeOffset.top && typeOffset.left) {
                        aType.find(">span.tree-tooltip").css({
                            top: "calc(" + typeOffset.top + "px - 45px)",
                            left: "24px"
                        }).addClass("show");
                    }
                }, 1200);
            }).on("dehover_node.jstree", function(nodes, str, res) {
                that.$(".tree-tooltip").removeClass("show");
            });
        },
        refresh: function(options) {
            var that = this,
                apiCount = (options && options.apiCount == 0) ? options.apiCount : 3,
                renderTree = function() {
                    if (apiCount === 0) {
                        that.renderEntityTree();
                        that.changeLoaderState(false);
                        that.ui.refreshTree.attr("disabled", false);
                    }
                };
            if (apiCount == 0) {
                that.entityDefCollection.fullCollection.sort({ silent: true });
                that.entityCountObj = _.first(that.metricCollection.toJSON());
                that.typeHeaders.fullCollection.sort({ silent: true });
                renderTree();
            } else {
                this.entityDefCollection.fetch({
                    complete: function() {
                        that.entityDefCollection.fullCollection.comparator = function(model) {
                            return model.get('name').toLowerCase();
                        };
                        that.entityDefCollection.fullCollection.sort({ silent: true });
                        --apiCount;
                        renderTree();
                    }
                });

                this.metricCollection.fetch({
                    complete: function() {
                        --apiCount;
                        that.entityCountObj = _.first(that.metricCollection.toJSON());
                        renderTree();
                    }
                });

                this.typeHeaders.fetch({
                    complete: function() {
                        that.typeHeaders.fullCollection.comparator = function(model) {
                            return model.get('name').toLowerCase();
                        }
                        that.typeHeaders.fullCollection.sort({ silent: true });
                        --apiCount;
                        renderTree();
                    }
                });
            }
        },
        onClickImportBusinessMetadata: function() {
            var that = this;
            require([
                'views/import/ImportLayoutView'
            ], function(ImportLayoutView) {
                var view = new ImportLayoutView({});
            });
        }
    });
    return EntityTreeLayoutview;
});