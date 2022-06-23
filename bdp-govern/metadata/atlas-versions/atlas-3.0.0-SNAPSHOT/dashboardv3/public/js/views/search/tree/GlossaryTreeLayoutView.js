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
    "hbs!tmpl/search/tree/GlossaryTreeLayoutView_tmpl",
    "utils/Utils",
    'utils/Messages',
    "utils/Globals",
    "utils/UrlLinks",
    "utils/CommonViewFunction",
    "collection/VSearchList",
    "collection/VGlossaryList",
    "jstree"
], function(require, GlossaryTreeLayoutView_tmpl, Utils, Messages, Globals, UrlLinks, CommonViewFunction, VSearchList, VGlossaryList) {
    "use strict";

    var GlossaryTreeLayoutView = Marionette.LayoutView.extend({
        template: GlossaryTreeLayoutView_tmpl,

        regions: {},
        ui: {
            refreshTree: '[data-id="refreshTree"]',
            termSearchTree: '[data-id="termSearchTree"]',
            createGlossary: '[data-id="createGlossary"]',
            showGlossaryType: '[data-id="showGlossaryType"]',
            importGlossary: "[data-id='importGlossary']",
            downloadTemplate: "[data-id='downloadTemplate']",
            glossaryTreeLoader: ".glossary-tree-loader"
        },
        templateHelpers: function() {
            return {
                apiBaseUrl: UrlLinks.apiBaseUrl,
                importTmplUrl: UrlLinks.glossaryImportTempUrl()
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
                that.refresh({ type: type });
            };

            events["click " + this.ui.createGlossary] = function(e) {
                var that = this;
                e.stopPropagation();
                CommonViewFunction.createEditGlossaryCategoryTerm({
                    isGlossaryView: true,
                    collection: that.glossaryCollection,
                    callback: function(rModel) {
                        that.glossaryCollection.fullCollection.add(rModel);
                    },
                    onModalClose: function() {}
                })
            };

            events["click " + this.ui.showGlossaryType] = function(e) {
                var getTreeData, displayText;
                e.stopPropagation();
                this.isTermView = !this.isTermView;
                this.glossarySwitchBtnUpdate();
            };
            events["click " + this.ui.importGlossary] = function(e) {
                e.stopPropagation();
                var $target = $(e.target);
                if ($target.parents(".disable-list-option").length == 0 && $target.hasClass("disable-list-option") == false) {
                    that.onClickImportGlossary();
                }
            };
            events['click ' + this.ui.downloadTemplate] = function(e) {
                e.stopPropagation();
            };
            return events;
        },
        bindEvents: function() {
            var that = this;
            this.listenTo(
                this.glossaryCollection.fullCollection, "reset add change",
                function(skip) {
                    if (this.ui.termSearchTree.jstree(true)) {
                        that.isGlossaryTree = true; //To Keep the selection of Term after any new Glossary is Created.
                        this.ui.termSearchTree.jstree(true).refresh();
                    } else {
                        this.renderGlossaryTree();
                    }
                    that.changeLoaderState();
                    that.ui.refreshTree.attr("disabled", false);
                },
                this
            );
            if (this.options.categoryEvent) {
                this.options.categoryEvent.on("Success:TermRename", function(options) {
                    that.refresh();
                })
            }

            $('body').on('click', '.termPopoverOptions li, .categoryPopoverOptions li', function(e) {
                that.$('.termPopover,.categoryPopover').popover('hide');
                that[$(this).find('a').data('fn')](e)
            });
        },
        glossarySwitchBtnUpdate: function() {
            var tooltipTitle = (this.isTermView ? "Show Category" : "Show Term");
            this.ui.showGlossaryType.attr({ "data-original-title": tooltipTitle, "title": tooltipTitle });
            this.ui.showGlossaryType.tooltip('hide');
            this.ui.showGlossaryType.find("i").toggleClass("switch-button");
            if (this.isTermView) {
                this.ui.importGlossary.removeClass("disable-list-option").find('a').attr("href", "javascript:void(0)");
                this.ui.downloadTemplate.removeClass("disable-list-option").find('a').attr("href", UrlLinks.glossaryImportTempUrl());
            } else {
                this.ui.importGlossary.addClass("disable-list-option").find('a').removeAttr("href");
                this.ui.downloadTemplate.addClass("disable-list-option").find('a').removeAttr("href");
            }
            this.ui.termSearchTree.jstree(true).refresh();
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
                    "query",
                    "categoryEvent"
                )
            );
            this.glossaryTermId = this.glossaryId = null;
            this.glossaryCollection = new VGlossaryList([], {
                comparator: function(item) {
                    return item.get("name");
                }
            });
            this.getViewType();
            this.bindEvents();
            //To stop the trigger Search event, if the node is selected in Old UI and swicthed to New UI.
            this.isGlossaryTree = this.isGlossryTreeview();
        },
        isGlossryTreeview: function() {
            var queryParams = Utils.getUrlState.getQueryParams();
            if (queryParams && (queryParams.gType === "term" || queryParams.gType === "category")) {
                return true;
            }
        },
        onRender: function() {
            this.changeLoaderState(true);
            this.fetchGlossary();
        },
        changeLoaderState: function(showLoader) {
            if (showLoader) {
                this.ui.termSearchTree.hide();
                this.ui.glossaryTreeLoader.show();
            } else {
                this.ui.termSearchTree.show();
                this.ui.glossaryTreeLoader.hide();
            }
        },
        onBeforeDestroy: function() {
            this.options.categoryEvent.off("Success:TermRename")
        },
        getViewType: function() {
            if (Utils.getUrlState.isGlossaryTab()) {
                this.isTermView = this.options.value.viewType ? this.options.value.viewType == "term" ? true : false : true;
            } else {
                this.isTermView = true;
            }
        },
        manualRender: function(options) {
            var that = this;
            _.extend(this.options, options);
            if (this.options.value === undefined) {
                this.options.value = {};
            }
            if (!this.options.value.term && this.options.value.gType != 'category') {
                this.ui.termSearchTree.jstree(true).deselect_all();
                this.glossaryTermId = null;
            } else {
                if (this.options.value.term) {
                    var glossaryName = this.options.value.term.split('@')[1],
                        termName = this.options.value.term.split('@')[0],
                        dataFound = this.glossaryCollection.fullCollection.find(function(obj) {
                            return obj.get("name") === glossaryName;
                        });
                    if (dataFound) {
                        var terms = dataFound.get('terms');
                        var terModel = _.find(terms, function(model) {
                            return model.displayText === termName;
                        });
                        if (terModel) {
                            if ((this.glossaryTermId && this.glossaryTermId !== terModel.termGuid) || this.glossaryTermId === null) {
                                if (this.glossaryTermId) {
                                    this.ui.termSearchTree.jstree(true).deselect_node(this.glossaryTermId);
                                }
                                this.ui.termSearchTree.jstree(true).deselect_all();
                                this.glossaryTermId = terModel.termGuid;
                                this.fromManualRender = true;
                                this.ui.termSearchTree.jstree(true).select_node(terModel.termGuid);
                            }
                        }
                    }
                }
            }
        },
        fetchGlossary: function() {
            this.glossaryCollection.fetch({ reset: true });
        },
        renderGlossaryTree: function() {
            this.generateSearchTree({
                $el: this.ui.termSearchTree

            });
            this.createTermAction();
        },
        onNodeSelect: function(options, showCategory) {
            var nodeType = options.node.original.type;
            if (this.isGlossaryTree && (nodeType === "GlossaryTerm" || nodeType === "GlossaryCategory")) {
                //To stop the trigger Search event,if the node is selected in Old UI and swicthed to New UI.
                this.isGlossaryTree = false;
                return;
            }
            var name, type, selectedNodeId, that = this,
                glossaryType = options.node.original.gType;
            if (glossaryType == "category") {
                selectedNodeId = options.node.id;
                if (that.glossaryTermId != selectedNodeId) {
                    that.glossaryTermId = selectedNodeId;
                    that.onViewEdit();
                } else {
                    that.glossaryTermId = null;
                    that.showDefaultPage();
                }

            } else if (glossaryType == "term") {
                if (options) {
                    name = _.unescape(options.node.original.name);
                    selectedNodeId = options.node.id;
                }
                var termValue = null,
                    params = {
                        searchType: "basic"
                    };
                if (this.options.value) {
                    if (this.options.value.isCF) {
                        this.options.value.isCF = null;
                    }
                }
                if (that.glossaryTermId != selectedNodeId) {
                    that.glossaryTermId = selectedNodeId;
                    termValue = options ? name + '@' + options.node.original.parent.name : this.options.value.term;
                    params['term'] = termValue;
                    params['gtype'] = 'term';
                    params['viewType'] = 'term';
                    params['guid'] = selectedNodeId;

                } else {
                    that.glossaryTermId = params["term"] = null;
                    that.ui.termSearchTree.jstree(true).deselect_all(true);
                    if (!that.options.value.type && !that.options.value.tag && !that.options.value.query) {
                        that.showDefaultPage();
                        return;
                    }
                }
                that.glossaryId = null;
                var searchParam = _.extend({}, that.options.value, params);

                this.triggerSearch(searchParam);
                if (that.searchVent) {
                    that.searchVent.trigger("Success:Category");
                }
            } else if (glossaryType = "glossary") { //This condition is added to setUrl after click on Glossary for highlighting issue from New UI to Old UI.                
                that.glossaryTermId = null;
                if (that.glossaryId != options.node.id) {
                    that.glossaryId = options.node.id;
                    var params = {
                        "gId": that.glossaryId,
                        "gType": options.node.original.gType,
                        "viewType": (this.isTermView) ? "term" : "category"
                    };
                    Utils.setUrl({
                        url: '#!/glossary/' + that.glossaryId,
                        urlParams: params,
                        mergeBrowserUrl: false,
                        trigger: false,
                        updateTabState: true
                    });
                } else {
                    that.glossaryId = null;
                    that.ui.termSearchTree.jstree(true).deselect_all(true);
                    this.showDefaultPage();
                }
            } else {
                that.glossaryTermId = null;
                if (that.glossaryId != options.node.id) {
                    that.glossaryId = options.node.id;
                } else {
                    that.glossaryId = null;
                    that.ui.termSearchTree.jstree(true).deselect_all(true);
                }
            }
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
        showDefaultPage: function() {
            Utils.setUrl({
                url: '!/search',
                mergeBrowserUrl: false,
                trigger: true,
                updateTabState: true
            });
        },
        generateCategoryData: function(options) {
            var that = this,
                isSelected = false;
            return _.map(options.data, function(obj) {
                return {
                    "text": _.escape(obj.displayText),
                    "icon": "fa fa-files-o",
                    "guid": obj.categoryGuid,
                    "id": obj.categoryGuid,
                    "glossaryId": options.node.glossaryId,
                    "glossaryName": options.node.glossaryName,
                    "model": obj,
                    "type": "GlossaryCategory",
                    "gType": "category",
                    "children": true
                }
            });
        },
        getCategory: function(options) {
            var that = this;
            this.glossaryCollection.getCategory({
                "guid": options.node.guid,
                "related": true,
                "ajaxOptions": {
                    success: function(data) {
                        if (data && data.children) {
                            options.callback(that.generateCategoryData(_.extend({}, { "data": data.children }, options)));
                        } else {
                            options.callback([]);
                        }
                    },
                    cust_error: function() {
                        options.callback([]);
                    }
                }
            });
        },
        getGlossaryTree: function(options) {
            var that = this,
                collection = (options && options.collection) || this.glossaryCollection.fullCollection,
                listOfParents = [],
                type = "term",
                queryParams = Utils.getUrlState.getQueryParams(),
                glossaryGuid = queryParams ? queryParams.gId : null,
                gType = queryParams ? queryParams.gType : "term";
            return this.glossaryCollection.fullCollection.map(function(model, i) {
                var obj = model.toJSON(),
                    parent = {
                        text: _.escape(obj.name),
                        name: _.escape(obj.name),
                        icon: "fa fa-folder-o",
                        guid: obj.guid,
                        id: obj.guid,
                        model: obj,
                        type: obj.typeName ? obj.typeName : "GLOSSARY",
                        gType: "glossary",
                        children: [],
                        state: {
                            opened: true,
                            selected: (model.id === glossaryGuid && gType === "glossary") ? true : false
                        }
                    },
                    openGlossaryNodesState = function(treeDate) {
                        if (treeDate.length == 1) {
                            _.each(treeDate, function(model) {
                                model.state['opeaned'] = true;
                            })
                        }
                    },
                    generateNode = function(nodeOptions, model, isTermView, parentNode) {
                        var nodeStructure = {
                            text: _.escape(model.displayText),
                            name: _.escape(model.displayText),
                            type: nodeOptions.type,
                            gType: that.isTermView ? "term" : "category",
                            guid: nodeOptions.guid,
                            id: nodeOptions.guid,
                            parent: parentNode ? parentNode : obj,
                            glossaryName: parentNode ? parentNode.name ? parentNode.name : parentNode.displayText : obj.name,
                            glossaryId: parentNode ? parentNode.guid ? parentNode.guid : parentNode.categoryGuid : obj.guid,
                            model: model,
                            icon: "fa fa-file-o"
                        };
                        return nodeStructure;

                    };

                if (!that.isTermView && obj.categories && !that.isTermView) {
                    var isSelected = false,
                        parentGuid = obj.guid,
                        parentCategoryGuid = null,
                        categoryList = [],
                        catrgoryRelation = [];
                    _.each(obj.categories, function(category) {
                        if (that.options.value) {
                            isSelected = that.options.value.guid ? that.options.value.guid == category.categoryGuid : false;
                        }

                        var typeName = category.typeName || "GlossaryCategory",
                            guid = category.categoryGuid,
                            categoryObj = {
                                id: guid,
                                guid: guid,
                                text: _.escape(category.displayText),
                                type: typeName,
                                gType: "category",
                                glossaryId: obj.guid,
                                glossaryName: obj.name,
                                children: [],
                                model: category,
                                icon: "fa fa-files-o"
                            };
                        if (category.parentCategoryGuid) {
                            catrgoryRelation.push({ parent: category.parentCategoryGuid, child: guid })
                        }
                        categoryList.push(categoryObj);
                    });
                    _.each(categoryList, function(category) {
                        var getRelation = _.find(catrgoryRelation, function(catrgoryObj) {
                            if (catrgoryObj.child == category.guid) return catrgoryObj;
                        })
                        if (getRelation) {
                            _.map(categoryList, function(catrgoryObj) {
                                if (catrgoryObj.guid == getRelation.parent) {
                                    catrgoryObj["children"].push(category);
                                };
                            })
                        } else {
                            parent.children.push(category)
                        }
                    })
                }
                if (that.isTermView && obj.terms) {
                    var isSelected = false;
                    _.each(obj.terms, function(term) {
                        if (that.options.value) {
                            isSelected = that.options.value.term ? that.options.value.term.split('@')[0] == term.displayText : false;
                        }
                        var parentNodeDetails = {
                                type: term.typeName || "GlossaryTerm",
                                guid: term.termGuid
                            },
                            parentNodeProperties = {},
                            getParentNodeDetails = generateNode(parentNodeDetails, term, that.isTermView),
                            termParentNode = (_.extend(parentNodeProperties, getParentNodeDetails));
                        parent.children.push(termParentNode);
                    });
                }
                openGlossaryNodesState(parent);
                return parent;
            });
        },
        createTermAction: function() {
            var that = this;
            Utils.generatePopover({
                el: this.$el,
                contentClass: 'termPopoverOptions',
                popoverOptions: {
                    selector: '.termPopover',
                    content: function() {
                        var type = $(this).data('detail'),
                            liString = "";
                        if (type == "glossary") {
                            liString = "<li data-type=" + type + " class='listTerm'><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='createSubNode'>Create Term</a></li>" +
                                "<li data-type=" + type + " class='listTerm'><i class='fa fa-list-alt'></i><a href='javascript:void(0)' data-fn='onViewEdit'>View/Edit Glossary</a></li>" +
                                "<li data-type=" + type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Glossary</a></li>"
                        } else {
                            liString = "<li data-type=" + type + " class='listTerm'><i class='fa fa-list-alt'></i><a href='javascript:void(0)' data-fn='onViewEdit'>View/Edit Term</a></li>" +
                                "<li data-type=" + type + " class='listTerm'><i class='fa fa-search'></i><a href='javascript:void(0)' data-fn='searchSelectedTerm'>Search</a></li>" +
                                "<li data-type=" + type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Term</a></li>"
                        }
                        return "<ul>" + liString + "</ul>";
                    }
                }
            });
        },
        createCategoryAction: function() {
            var that = this;
            Utils.generatePopover({
                el: this.$('.categoryPopover'),
                contentClass: 'categoryPopoverOptions',
                popoverOptions: {
                    content: function() {
                        var type = $(this).data('detail'),
                            liString = "";
                        if (type == "glossary") {
                            liString = "<li data-type=" + type + " class='listTerm'><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='createSubNode'>Create Category</a></li>";
                        } else {
                            liString = "<li data-type=" + type + " class='listTerm'><i class='fa fa-list-alt'></i><a href='javascript:void(0)' data-fn='createSubNode'>Create Sub-Category</a></li>";
                        }
                        return "<ul>" + liString + "</ul>";
                    },
                    viewFixedPopover: true,
                }
            });
        },
        createSubNode: function(opt) {
            var that = this,
                selectednode = that.ui.termSearchTree.jstree("get_selected", true);
            if ((selectednode[0].original.type == "GLOSSARY" || selectednode[0].original.type == "GlossaryCategory") && !this.isTermView) {
                CommonViewFunction.createEditGlossaryCategoryTerm({
                    "isCategoryView": true,
                    "collection": that.glossaryCollection,
                    "callback": function(updateCollection) {
                        var updatedObj = {
                                categoryGuid: updateCollection.guid,
                                displayText: updateCollection.name,
                                relationGuid: updateCollection.anchor ? updateCollection.anchor.relationGuid : null
                            },
                            glossary = that.glossaryCollection.fullCollection.findWhere({ guid: updateCollection.anchor.glossaryGuid });
                        if (updateCollection.parentCategory) {
                            updatedObj["parentCategoryGuid"] = updateCollection.parentCategory.categoryGuid;
                        }
                        if (glossary) {
                            var glossaryAttributes = glossary.attributes || null;
                            if (glossaryAttributes) {
                                if (glossaryAttributes.categories) {
                                    glossaryAttributes['categories'].push(updatedObj);
                                } else {
                                    glossaryAttributes['categories'] = [updatedObj];
                                }
                            }
                        }
                        that.ui.termSearchTree.jstree(true).refresh();
                    },
                    "node": selectednode[0].original
                })
            } else {
                CommonViewFunction.createEditGlossaryCategoryTerm({
                    "isTermView": true,
                    "callback": function() {
                        that.fetchGlossary();
                        that.options.categoryEvent.trigger("Success:Term", true);
                    },
                    "collection": that.glossaryCollection,
                    "node": selectednode[0].original
                })
            }
        },
        searchSelectedTerm: function() {
            var params = {
                searchType: "basic",
                dslChecked: false,
                term: this.options.value.term
            };
            this.triggerSearch(params);
        },
        deleteNode: function(opt) {
            var that = this,
                messageType = "",
                selectednode = this.ui.termSearchTree.jstree("get_selected", true),
                type = selectednode[0].original.type,
                guid = selectednode[0].original.guid,
                gId = selectednode[0].original.parent && selectednode[0].original.parent.guid,
                options = {
                    success: function(rModel, response) {
                        var searchParam = null;
                        if (!gId) {
                            gId = guid;
                        }
                        var glossary = that.glossaryCollection.fullCollection.get(gId);
                        if (type == "GlossaryTerm") {
                            glossary.set('terms', _.reject(glossary.get('terms'), function(obj) {
                                return obj.termGuid == guid;
                            }), { silent: true });
                        }
                        Utils.notifySuccess({
                            content: messageType + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                        });
                        that.ui.termSearchTree.jstree(true).refresh();
                        var params = {
                            searchType: "basic",
                            term: null
                        };
                        that.glossaryTermId = null;
                        if (that.options.value.gType == "category") {
                            that.showDefaultPage();
                        } else {
                            searchParam = _.extend({}, that.options.value, params);
                            that.triggerSearch(searchParam);
                        }
                    },
                    complete: function() {
                        that.notificationModal.hideButtonLoader();
                        that.notificationModal.remove();
                    }
                },
                notifyObj = {
                    modal: true,
                    ok: function(obj) {
                        that.notificationModal = obj;
                        obj.showButtonLoader();
                        if (type == "Glossary" || type == "GLOSSARY") {
                            that.glossaryCollection.fullCollection.get(guid).destroy(options, { silent: true, reset: false });
                        } else if (type == "GlossaryCategory") {
                            new that.glossaryCollection.model().deleteCategory(guid, options);

                        } else if (type == "GlossaryTerm") {
                            new that.glossaryCollection.model().deleteTerm(guid, options);
                        }
                    },
                    okCloses: false,
                    cancel: function(argument) {}
                };
            if (type == "Glossary" || type == "GLOSSARY") {
                messageType = "Glossary";
            } else if (type == "GlossaryCategory") {
                messageType = "Category"
            } else if (type == "GlossaryTerm") {
                messageType = "Term";
            }
            notifyObj['text'] = "Are you sure you want to delete the " + messageType;;
            Utils.notifyConfirm(notifyObj);
        },
        onViewEdit: function() {
            var that = this,
                selectednode = this.ui.termSearchTree.jstree("get_selected", true),
                type = selectednode[0].original.type,
                guid = selectednode[0].original.guid,
                gId = selectednode[0].original.parent && selectednode[0].original.parent.guid,
                isGlossaryView = (type == 'GlossaryTerm' || type == 'GlossaryCategory') ? false : true,
                model = this.glossaryCollection.fullCollection.get(guid),
                termModel = this.glossaryCollection.fullCollection.get(gId);
            if (isGlossaryView) {
                CommonViewFunction.createEditGlossaryCategoryTerm({
                    "model": model,
                    "isGlossaryView": true,
                    "collection": this.glossaryCollection,
                    "callback": function(sModel) {
                        var data = sModel.toJSON();
                        model.set(data, { silent: true }); // update glossaryCollection
                        that.ui.termSearchTree.jstree(true).refresh();
                    }
                });
            } else {
                var glossaryId = selectednode[0].original.glossaryId,
                    getSelectedParent = null,
                    params = null;
                if (selectednode[0].parents.length > 2) {
                    getSelectedParent = selectednode[0].parents[selectednode[0].parents.length - 3];
                } else {
                    getSelectedParent = selectednode[0].id;
                }

                params = {
                    gId: glossaryId,
                    guid: getSelectedParent,
                    gType: that.isTermView ? 'term' : 'category',
                    viewType: that.isTermView ? 'term' : 'category',
                    searchType: "basic"
                }
                if (type === "GlossaryTerm") {
                    //Below condition is used to keep the selection or Highlight after clicking on the viewEdit option on term.
                    params['term'] = selectednode[0].original.name + '@' + selectednode[0].original.parent.name;
                }
                var serachUrl = '#!/glossary/' + guid;
                this.triggerSearch(params, serachUrl);
                if (!this.isTermView && this.options.categoryEvent) {
                    that.options.categoryEvent.trigger("Success:Category", true);
                }
            }
        },
        generateSearchTree: function(options) {
            var $el = options && options.$el,
                data = options && options.data,
                type = options && options.type,
                that = this,
                createAction = function(options) {
                    that.isTermView ? that.createTermAction() : that.createCategoryAction();
                },
                getEntityTreeConfig = function(opt) {
                    return {
                        plugins: ["search", "core", "sort", "conditionalselect", "changed", "wholerow", "node_customize"],

                        state: { opened: true },
                        search: {
                            show_only_matches: true,
                            case_sensitive: false
                        },
                        node_customize: {
                            default: function(el, node) {
                                var aTerm = $(el).find(">a.jstree-anchor");
                                aTerm.append("<span class='tree-tooltip'>" + _.escape(aTerm.text()) + "</span>");
                                var popoverClass = that.isTermView ? "fa fa-ellipsis-h termPopover " : "fa fa-ellipsis-h categoryPopover";
                                $(el).append('<div class="tools" data-type=' + node.original.gType + '><i class="' + popoverClass + '"rel="popover" data-detail=' + node.original.gType + '></i></div>');
                            }
                        },
                        core: {
                            data: function(node, cb) {
                                if (node.id === "#") {
                                    cb(that.getGlossaryTree());
                                } else {
                                    that.getCategory({ "node": node.original, "callback": cb });
                                }
                            },
                            multiple: false
                        }
                    };
                };

            $el.jstree(
                    getEntityTreeConfig({
                        type: ""
                    })
                ).on("load_node.jstree", function(e, data) {}).on("open_node.jstree", function(e, data) {}).on("select_node.jstree", function(e, data) {
                    if (that.fromManualRender !== true) {
                        that.onNodeSelect(data);
                        if (that.glossaryId === data.node.original.id) {
                            //This condition is to reset the Glossary ID to null after clicking on the Logo.
                            that.glossaryId = null;
                        }
                    } else {
                        that.fromManualRender = false;
                    }
                    createAction(_.extend({}, options, data));
                })
                .on("open_node.jstree", function(e, data) {
                    createAction(_.extend({}, options, data));
                })
                .on("search.jstree", function(nodes, str, res) {
                    if (str.nodes.length === 0) {
                        $el.jstree(true).hide_all();
                        $el.parents(".panel").addClass("hide");
                    } else {
                        $el.parents(".panel").removeClass("hide");
                    }
                }).on('loaded.jstree', function() {
                    if (that.options.value) {
                        if (that.options.value.term) {
                            that.selectDefaultNode();
                        }
                        if (!that.isTermView) {
                            that.selectDefaultNode();
                            that.options.categoryEvent.trigger("Success:Category", true);
                        }
                    }
                    //Below condition is for switching the the Show Term/Show Category toggle button on switching from Old to New UI.
                    if (that.isTermView === false) {
                        that.glossarySwitchBtnUpdate();
                    }

                }).on("hover_node.jstree", function(nodes, str, res) {
                    var aTerm = that.$("#" + str.node.a_attr.id),
                        termOffset = aTerm.find(">.jstree-icon").offset();
                    that.$(".tree-tooltip").removeClass("show");
                    setTimeout(function() {
                        if (aTerm.hasClass("jstree-hovered") && termOffset.top && termOffset.left) {
                            aTerm.find(">span.tree-tooltip").css({
                                top: "calc(" + termOffset.top + "px - 45px)",
                                left: "24px"
                            }).addClass("show");
                        }
                    }, 1200);
                }).on("dehover_node.jstree", function(nodes, str, res) {
                    that.$(".tree-tooltip").removeClass("show");
                });
        },
        selectDefaultNode: function() {
            this.ui.termSearchTree.jstree(true).select_node(this.options.value.guid);
        },
        refresh: function(options) {
            this.glossaryTermId = null;
            this.fetchGlossary();
            this.isGlossaryTree = true;
        },
        onClickImportGlossary: function() {
            var that = this;
            require([
                'views/import/ImportLayoutView'
            ], function(ImportLayoutView) {
                var view = new ImportLayoutView({
                    callback: function() {
                        that.refresh();
                    },
                    isGlossary: true
                });
            });
        }
    });
    return GlossaryTreeLayoutView;
});