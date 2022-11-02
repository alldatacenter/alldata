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
    'backbone',
    'hbs!tmpl/glossary/GlossaryLayoutView_tmpl',
    'utils/Utils',
    'utils/Messages',
    'utils/Globals',
    'utils/CommonViewFunction',
    'jstree'
], function(require, Backbone, GlossaryLayoutViewTmpl, Utils, Messages, Globals, CommonViewFunction) {
    'use strict';

    var GlossaryLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends GlossaryLayoutView */
        {
            _viewName: 'GlossaryLayoutView',

            template: GlossaryLayoutViewTmpl,

            /** Layout sub regions */
            regions: {},
            templateHelpers: function() {
                return {
                    isAssignView: this.isAssignView,
                    isAssignAttributeRelationView: this.isAssignAttributeRelationView
                };
            },

            /** ui selector cache */
            ui: {
                createGlossary: "[data-id='createGlossary']",
                refreshGlossary: "[data-id='refreshGlossary']",
                searchTerm: "[data-id='searchTerm']",
                searchCategory: "[data-id='searchCategory']",
                glossaryView: 'input[name="glossaryView"]',
                termTree: "[data-id='termTree']",
                categoryTree: "[data-id='categoryTree']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["change " + this.ui.glossaryView] = 'glossaryViewToggle';
                events["click " + this.ui.createGlossary] = function(e) {
                    var that = this;
                    if (e) {
                        $(e.currentTarget).attr("disabled", "true");
                    }
                    CommonViewFunction.createEditGlossaryCategoryTerm({
                        isGlossaryView: true,
                        collection: this.glossaryCollection,
                        callback: function() {
                            that.ui.createGlossary.removeAttr("disabled");
                            that.getGlossary();
                        },
                        onModalClose: function() {
                            that.ui.createGlossary.removeAttr("disabled");
                        }
                    })
                };
                events["click " + this.ui.refreshGlossary] = 'getGlossary';
                events["keyup " + this.ui.searchTerm] = function() {
                    this.ui.termTree.jstree("search", this.ui.searchTerm.val());
                };
                events["keyup " + this.ui.searchCategory] = function() {
                    this.ui.categoryTree.jstree("search", this.ui.searchCategory.val());
                };
                return events;
            },
            /**
             * intialize a new GlossaryLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'associatedTerms', 'guid', 'value', 'glossaryCollection', 'glossary', 'isAssignTermView', 'isAssignCategoryView', 'isAssignEntityView', 'isAssignAttributeRelationView'));
                this.viewType = "term";
                this.isAssignView = this.isAssignTermView || this.isAssignCategoryView || this.isAssignEntityView || this.isAssignAttributeRelationView;
                this.bindEvents();
                this.query = {
                    term: {},
                    category: {}
                };
                if (Utils.getUrlState.isGlossaryTab() && this.value && this.value.viewType) {
                    this.viewType = this.value.viewType;
                    this.query[this.viewType] = _.extend({}, this.value, { "guid": this.guid });
                }
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.glossaryCollection.fullCollection, "reset add change", function(skip) {
                    this.generateTree();
                    this.setValues();
                }, this);
                this.listenTo(this.glossaryCollection, "update:details", function(options) {
                    var isGlossaryUpdate = options.isGlossaryUpdate;
                    if (isGlossaryUpdate) {
                        if (this.ui.termTree.jstree(true).refresh) {
                            this.ui.termTree.jstree(true).refresh();
                        }
                        if (this.ui.categoryTree.jstree(true).refresh) {
                            this.ui.categoryTree.jstree(true).refresh();
                        }
                    } else {
                        var $tree = this.ui[this.viewType == "term" ? "termTree" : "categoryTree"];
                        if ($tree.jstree(true).refresh) {
                            $tree.jstree(true).refresh();
                            this.setValues({ trigger: false });
                        }
                    }
                }, this);
                if (!this.isAssignView) {
                    $('body').on('click', '.termPopoverOptions li, .categoryPopoverOptions li', function(e) {
                        that.$('.termPopover,.categoryPopover').popover('hide');
                        that[$(this).find('a').data('fn')](e)
                    });
                }
            },
            onRender: function() {
                if (this.isAssignCategoryView) {
                    this.$('.category-view').show();
                    this.$('.term-view').hide();
                }
                if (this.isAssignView && this.glossaryCollection.fullCollection.length) {
                    this.generateTree();
                    this.disableNodesList = this.getDisableNodes();
                } else {
                    this.getGlossary();
                }
            },
            setValues: function(options) {
                if (this.viewType == "category") {
                    if (!this.ui.glossaryView.prop("checked")) {
                        this.ui.glossaryView.prop("checked", true).trigger("change", options);
                    }
                } else {
                    if (this.ui.glossaryView.prop("checked")) {
                        this.ui.glossaryView.prop("checked", false).trigger("change", options);
                    }
                }
            },
            glossaryViewToggle: function(e, options) {
                var that = this;
                if (e.currentTarget.checked) {
                    this.$('.category-view').show();
                    this.$('.term-view').hide();
                    this.viewType = "category";
                } else {
                    this.$('.term-view').show();
                    this.$('.category-view').hide();
                    this.viewType = "term";
                }
                var setDefaultSelector = function() {
                    if (!that.value) {
                        return;
                    }
                    var model = null;
                    if (that.value.gId) {
                        model = that.glossaryCollection.fullCollection.get(that.value.gId);
                    } else {
                        model = that.glossaryCollection.fullCollection.first();
                    }
                    model = model.toJSON ? model.toJSON() : model;
                    that.glossary.selectedItem = {
                        type: "Glossary",
                        guid: model.guid,
                        id: model.guid,
                        model: model,
                        text: model.name,
                        gType: "glossary"
                    }
                }
                if (Utils.getUrlState.isGlossaryTab()) {
                    var obj = this.query[this.viewType],
                        $tree = this.ui[(this.viewType == "term" ? "termTree" : "categoryTree")]
                    if (obj.guid) {
                        var node = $tree.jstree(true).get_node(obj.guid);
                        if (node) {
                            this.glossary.selectedItem = node.original;
                            $tree.jstree('activate_node', obj.guid);
                        }
                    } else {
                        setDefaultSelector();
                        $tree.jstree('activate_node', that.glossary.selectedItem.guid);
                    }
                    this.query[this.viewType] = _.extend(obj, _.pick(this.glossary.selectedItem, 'model', 'guid', 'gType', 'type'), { "viewType": this.viewType, "isNodeNotFoundAtLoad": this.query[this.viewType].isNodeNotFoundAtLoad });
                    var url = _.isEmpty(this.glossary.selectedItem) ? '#!/glossary' : '#!/glossary/' + this.glossary.selectedItem.guid;
                    Utils.setUrl({
                        "url": url,
                        "urlParams": _.extend({}, _.omit(obj, 'guid', 'model', 'type', 'isNodeNotFoundAtLoad')),
                        "mergeBrowserUrl": false,
                        "trigger": (options && !_.isUndefined(options.trigger) ? options.trigger : true),
                        "updateTabState": true
                    });
                }
            },
            getGlossary: function() {
                this.glossaryCollection.fetch({ reset: true });
            },
            generateCategoryData: function(options) {
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
            generateData: function(opt) {
                var that = this,
                    selectedGuid = that.guid,
                    associatedTerms = that.associatedTerms,
                    type = opt.type;

                if (opt.type == this.viewType) {
                    this.query[opt.type].isNodeNotFoundAtLoad = true;
                }
                var getSelectedState = function(options) {
                    var objGuid = options.objGuid,
                        node = options.node,
                        index = options.index;
                    if (that.isAssignView) {
                        return {
                            'opened': true
                        }
                    } else if (!that.guid) {
                        that.query[that.viewType].isNodeNotFoundAtLoad = false;
                        var selectedItem = {
                            "type": "Glossary",
                            "gType": "glossary",
                            "model": that.glossaryCollection.fullCollection.first().toJSON()
                        };
                        selectedItem.text = selectedItem.model.name;
                        selectedItem.guid = selectedItem.model.guid;
                        if (index == 0 && selectedItem.guid == objGuid) {
                            that.glossary.selectedItem = selectedItem;
                            that.query[that.viewType].model = selectedItem.model;
                            that.query[that.viewType].type = selectedItem.type;
                            return {
                                'opened': true,
                                'selected': true
                            }
                        }
                    } else {
                        if (that.guid == objGuid) {
                            that.query[that.viewType].isNodeNotFoundAtLoad = false;
                            that.glossary.selectedItem = node
                            that.query[that.viewType].model = node.model;
                            that.query[that.viewType].type = node.type;
                            return {
                                'opened': true,
                                'selected': true
                            }
                        }
                    }
                }
                return this.glossaryCollection.fullCollection.map(function(model, i) {
                    var obj = model.toJSON(),
                        parent = {
                            "text": _.escape(obj.name),
                            "icon": "fa fa-folder-o",
                            "guid": obj.guid,
                            "id": obj.guid,
                            "model": obj,
                            "type": obj.typeName ? obj.typeName : "Glossary",
                            "gType": "glossary",
                            "children": []
                        }
                    parent.state = getSelectedState({
                        index: i,
                        node: parent,
                        objGuid: obj.guid
                    });

                    if (type == "category" && obj.categories) {
                        _.each(obj.categories, function(category) {
                            if (category.parentCategoryGuid) {
                                return;
                            }
                            var typeName = category.typeName || "GlossaryCategory",
                                guid = category.categoryGuid,
                                categoryObj = {
                                    "text": _.escape(category.displayText),
                                    "type": typeName,
                                    "gType": "category",
                                    "guid": guid,
                                    "id": guid,
                                    "parent": obj,
                                    "glossaryId": obj.guid,
                                    "glossaryName": obj.name,
                                    "model": category,
                                    "children": true,
                                    "icon": "fa fa-files-o",
                                };
                            categoryObj.state = getSelectedState({
                                index: i,
                                node: categoryObj,
                                objGuid: guid
                            })
                            parent.children.push(categoryObj)
                        });
                    }
                    if (type == "term" && obj.terms) {
                        _.each(obj.terms, function(term) {
                            if (associatedTerms) {
                                var associatedTermFound = _.find(associatedTerms, function(obj, index) {
                                    if ((obj.termGuid ? obj.termGuid : obj.guid) == term.termGuid) {
                                        return obj;
                                    }
                                });
                                if (associatedTermFound) {
                                    return;
                                }
                            }

                            var typeName = term.typeName || "GlossaryTerm",
                                guid = term.termGuid,
                                termObj = {
                                    "text": _.escape(term.displayText),
                                    "type": typeName,
                                    "gType": "term",
                                    "guid": guid,
                                    "id": guid,
                                    "parent": obj,
                                    "glossaryName": obj.name,
                                    "glossaryId": obj.guid,
                                    "model": term,
                                    "icon": "fa fa-file-o"
                                }
                            termObj.state = getSelectedState({
                                index: i,
                                node: termObj,
                                objGuid: guid
                            })
                            parent.children.push(termObj);

                        });
                    }
                    return parent;
                });
            },
            manualRender: function(options) {
                _.extend(this, _.omit(options, 'isTrigger'));
                if (this.value && this.value.viewType) {
                    this.viewType = this.value.viewType;
                }
                if (this.guid && this.value && ((this.value.fromView && this.value.fromView) || (this.value.updateView))) {
                    var $tree = this.ui[this.viewType == "term" ? "termTree" : "categoryTree"],
                        node = $tree.jstree(true).get_node(this.guid);
                    if (node) {
                        $tree.jstree('activate_node', this.guid, { skipTrigger: true });
                        delete this.value.fromView;
                        delete this.value.updateView;
                        this.glossary.selectedItem = node.original;
                        this.query[this.viewType] = _.extend({}, _.pick(this.glossary.selectedItem, 'model', 'guid', 'gType', 'type'), { "viewType": this.viewType });
                        Utils.setUrl({
                            url: '#!/glossary/' + this.guid,
                            urlParams: this.value,
                            mergeBrowserUrl: false,
                            trigger: false,
                            updateTabState: true
                        });
                        this.glossaryCollection.trigger("update:details", { isGlossaryUpdate: this.value.gType == "glossary" });
                    }
                } else {
                    this.setValues();
                }
                if (options.isTrigger) {
                    this.triggerUrl();
                }
            },
            getDisableNodes: function() {
                var disableNodesSelection = [];
                if (this.options && this.options.isAssignAttributeRelationView) {
                    var disableTerms = (this.options.termData && this.options.selectedTermAttribute) ? this.options.termData[this.options.selectedTermAttribute] : null;
                    disableNodesSelection = _.map(disableTerms, function(obj) {
                        return obj.termGuid;
                    });
                    disableNodesSelection.push(this.options.termData.guid);
                }
                return disableNodesSelection;
            },
            generateTree: function() {
                var $termTree = this.ui.termTree,
                    $categoryTree = this.ui.categoryTree,
                    that = this,
                    this_guid = that.guid,
                    getTreeConfig = function(options) {
                        return {
                            "plugins": ["search", "themes", "core", "wholerow", "sort", "conditionalselect"],
                            "conditionalselect": function(node) {
                                var obj = node && node.original && node.original.type;
                                if (!obj) {
                                    return;
                                }
                                if (that.isAssignView) {
                                    var isDisableNode = false;
                                    if (that.disableNodesList) {
                                        isDisableNode = (that.disableNodesList.indexOf(node.original.guid) > -1) ? true : false;
                                    }
                                    return (obj != "Glossary" && !isDisableNode) ? true : false;
                                } else {
                                    return obj != "NoAction" ? true : false;
                                }
                            },
                            "search": {
                                "show_only_matches": true
                            },
                            "core": {
                                "data": function(node, cb) {
                                    if (node.id === "#") {
                                        cb(that.generateData(options));
                                    } else {
                                        that.getCategory({ "node": node.original, "callback": cb });
                                    }
                                },
                                "themes": {
                                    "name": that.isAssignView ? "default" : "default-dark",
                                    "dots": true
                                },
                            }
                        }
                    },
                    treeLoaded = function(options) {
                        if (that.query[options.type].isNodeNotFoundAtLoad == true) {
                            var id = that.glossary.selectedItem.guid;
                            options.$el.jstree('activate_node', id);
                        }
                    },
                    createAction = function(options) {
                        var $el = options.el,
                            type = options.type,
                            popoverClassName = type == "term" ? "termPopover" : "categoryPopover";
                        if (!that.isAssignView) {
                            var wholerowEl = $el.find("li[role='treeitem'] > .jstree-wholerow:not(:has(>div.tools))")
                            wholerowEl.append('<div class="tools"><i class="fa fa-ellipsis-h ' + popoverClassName + '"></i></div>');

                            if (type == "term") {
                                that.createTermAction();
                            } else if (type == "category") {
                                that.createCategoryAction();
                            }
                        }
                    },
                    initializeTree = function(options) {
                        var $el = options.el,
                            type = options.type;

                        $el.jstree(getTreeConfig({
                                type: type
                            })).on("load_node.jstree", function(e, data) {
                                createAction(_.extend({}, options, data));
                            }).on("open_node.jstree", function(e, data) {
                                createAction(_.extend({}, options, data));
                            })
                            .on("select_node.jstree", function(e, data) {
                                if (that.isAssignView) {
                                    that.glossary.selectedItem = data.node.original;
                                    that.glossaryCollection.trigger("node_selected");
                                } else {
                                    var popoverClassName = (type == "term" ? '.termPopover' : '.categoryPopover'),
                                        currentClickedPopoverEl = "";
                                    if (data.event) {
                                        if ($(data.event.currentTarget).parent().hasClass('jstree-leaf')) {
                                            currentClickedPopoverEl = $(data.event.currentTarget).parent().find(popoverClassName);
                                        } else {
                                            currentClickedPopoverEl = $(data.event.currentTarget).parent().find(">div " + popoverClassName);
                                        }
                                        $(popoverClassName).not(currentClickedPopoverEl).popover('hide');
                                    }
                                    if (that.query[type].isNodeNotFoundAtLoad == true) {
                                        that.query[type].isNodeNotFoundAtLoad = false;
                                    } else if (type == that.viewType) {
                                        if (data && data.event && data.event.skipTrigger) {
                                            return;
                                        } else if (that.glossary.selectedItem.guid !== data.node.original.guid) {
                                            that.glossary.selectedItem = data.node.original;
                                            that.triggerUrl();
                                        }
                                    }
                                }
                            }).on("search.jstree", function(e, data) {
                                createAction(_.extend({}, options, data));
                            }).on("clear_search.jstree", function(e, data) {
                                createAction(_.extend({}, options, data));
                            }).bind('loaded.jstree', function(e, data) {
                                if (that.query[type].isNodeNotFoundAtLoad == true) {
                                    treeLoaded({ "$el": $el, "type": type });
                                }
                            });
                    },
                    initializeTermTree = function() {
                        if ($termTree.data('jstree')) {
                            $('.termPopover').popover('destroy');
                            $termTree.jstree(true).refresh();
                        } else {
                            initializeTree({
                                el: $termTree,
                                type: "term"
                            });
                        }
                    },
                    initializeCategoryTree = function() {
                        if ($categoryTree.data('jstree')) {
                            $('.categoryPopover').popover('destroy');
                            $categoryTree.jstree(true).refresh();
                        } else {
                            initializeTree({
                                el: $categoryTree,
                                type: "category"
                            })
                        }
                    }
                if (this.isAssignView) {
                    if (this.isAssignTermView || this.isAssignEntityView || this.isAssignAttributeRelationView) {
                        initializeTermTree();
                    } else if (this.isAssignCategoryView) {
                        initializeCategoryTree();
                    }
                } else {
                    initializeTermTree();
                    initializeCategoryTree();
                }


                if (Utils.getUrlState.isGlossaryTab()) {
                    this.triggerUrl();
                }
                this.glossaryCollection.trigger("render:done");
            },
            createTermAction: function() {
                var that = this;
                Utils.generatePopover({
                    el: this.$('.termPopover'),
                    contentClass: 'termPopoverOptions',
                    popoverOptions: {
                        content: function() {
                            var node = that.query[that.viewType],
                                liString = "";
                            if (node.type == "Glossary") {
                                liString = "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='createSubNode'>Create Term</a></li>" +
                                    "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Glossary</a></li>"
                            } else {
                                liString = "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Term</a></li>"
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
                            var node = that.query[that.viewType],
                                liString = "";
                            if (node.type == "Glossary") {
                                liString = "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='createSubNode'>Create Category</a></li>" +
                                    "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Glossary</a></li>"
                            } else {
                                liString = "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='createSubNode'>Create Sub-Category</a></li>" +
                                    "<li data-type=" + node.type + " class='listTerm'><i class='fa fa-trash-o'></i><a href='javascript:void(0)' data-fn='deleteNode'>Delete Category</a></li>"
                            }
                            return "<ul>" + liString + "</ul>";
                        }
                    }
                });
            },
            createSubNode: function(opt) {
                var that = this,
                    type = this.glossary.selectedItem.type;
                if ((type == "Glossary" || type == "GlossaryCategory") && this.viewType == "category") {
                    CommonViewFunction.createEditGlossaryCategoryTerm({
                        "isCategoryView": true,
                        "collection": that.glossaryCollection,
                        "callback": function() {
                            if (that.value.gType == "glossary") {
                                that.getGlossary();
                            } else {
                                that.ui.categoryTree.jstree(true).refresh();
                            }
                        },
                        "node": this.glossary.selectedItem
                    })
                } else {
                    CommonViewFunction.createEditGlossaryCategoryTerm({
                        "isTermView": true,
                        "callback": function() {
                            that.getGlossary();
                        },
                        "collection": that.glossaryCollection,
                        "node": this.glossary.selectedItem
                    })
                }
            },
            deleteNode: function(opt) {
                var that = this,
                    messageType = "",
                    type = this.glossary.selectedItem.type,
                    guid = this.glossary.selectedItem.guid,
                    gId = this.glossary.selectedItem.glossaryId,
                    options = {
                        success: function(rModel, response) {
                            if (!gId) {
                                gId = guid;
                            }
                            var glossary = that.glossaryCollection.fullCollection.get(gId);
                            if (that.value) {
                                if (that.value.gType == "term") {
                                    glossary.set('terms', _.reject(glossary.get('terms'), function(obj) {
                                        return obj.termGuid == guid;
                                    }), { silent: true });
                                } else if (that.value.gType == "category") {
                                    glossary.set('categories', _.reject(glossary.get('categories'), function(obj) {
                                        return obj.categoryGuid == guid;
                                    }), { silent: true });
                                } else {
                                    glossary = that.glossaryCollection.fullCollection.first();
                                    if (glossary) {
                                        gId = glossary.get('guid');
                                    } else {
                                        gId = null
                                    }
                                }
                            }
                            Utils.notifySuccess({
                                content: messageType + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                            });
                            var url = gId ? '#!/glossary/' + gId : '#!/glossary';
                            if (gId == null) {
                                that.glossary.selectedItem = {};
                                that.value = null;
                                that.query = {
                                    term: {},
                                    category: {}
                                };
                                that.ui.categoryTree.jstree(true).refresh();
                                that.ui.termTree.jstree(true).refresh();
                            }
                            Utils.setUrl({
                                url: url,
                                mergeBrowserUrl: false,
                                trigger: true,
                                urlParams: gId ? _.extend({}, that.value, {
                                    gType: 'glossary',
                                    updateView: true,
                                    gId: null
                                }) : null,
                                updateTabState: true
                            });
                        }
                    },
                    notifyObj = {
                        modal: true,
                        ok: function(argument) {
                            if (type == "Glossary") {
                                that.glossaryCollection.fullCollection.get(guid).destroy(options, { silent: true, reset: false });
                            } else if (type == "GlossaryCategory") {
                                new that.glossaryCollection.model().deleteCategory(guid, options);
                            } else if (type == "GlossaryTerm") {
                                new that.glossaryCollection.model().deleteTerm(guid, options);
                            }
                        },
                        cancel: function(argument) {}
                    };
                if (type == "Glossary") {
                    messageType = "Glossary";
                } else if (type == "GlossaryCategory") {
                    messageType = "Category"
                } else if (type == "GlossaryTerm") {
                    messageType = "Term";
                }
                notifyObj['text'] = "Are you sure you want to delete the " + messageType;;
                Utils.notifyConfirm(notifyObj);
            },
            triggerUrl: function(options) {
                if (this.isAssignView) {
                    return;
                }
                var selectedItem = this.glossary.selectedItem;
                if (this.glossaryCollection.length && (_.isEmpty(selectedItem) || this.query[this.viewType].isNodeNotFoundAtLoad)) {
                    var model = selectedItem.model
                    if (model && !_.isUndefined(model.parentCategory || model.parentCategoryGuid)) {
                        selectedItem = { "model": this.glossaryCollection.first().toJSON() };
                        selectedItem.guid = selectedItem.model.guid;
                        selectedItem.type = "Glossary";
                        selectedItem.gType = "glossary";
                        selectedItem.text = model.name;
                        this.glossary.selectedItem = selectedItem;
                        this.query[this.viewType].model = selectedItem.model;
                        this.query[this.viewType].gType = "glossary";
                        this.query[this.viewType].type = "Glossary";
                        delete this.query[this.viewType].gId;
                    }
                }
                if (_.isEmpty(selectedItem)) {
                    return;
                }
                if (Utils.getUrlState.isGlossaryTab() || Utils.getUrlState.isDetailPage()) {
                    var obj = {};
                    if (selectedItem.glossaryId) {
                        obj["gId"] = selectedItem.glossaryId;
                    } else if (selectedItem.type == "Glossary") {
                        obj["gId"] = selectedItem.guid;
                    }
                    this.query[this.viewType] = _.extend(obj, _.omit(this.value, 'gId'), _.pick(this.glossary.selectedItem, 'model', 'type', 'gType', 'guid'), { "viewType": this.viewType, "isNodeNotFoundAtLoad": this.query[this.viewType].isNodeNotFoundAtLoad });
                    Utils.setUrl({
                        url: '#!/glossary/' + obj.guid,
                        mergeBrowserUrl: false,
                        trigger: true,
                        urlParams: _.omit(obj, 'model', 'guid', 'type', 'isNodeNotFoundAtLoad'),
                        updateTabState: true
                    });
                }
            }
        });
    return GlossaryLayoutView;
});