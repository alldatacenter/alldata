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
    "backbone",
    "hbs!tmpl/tag/TagLayoutView_tmpl",
    "utils/Utils",
    "utils/Messages",
    "utils/Globals",
    "utils/UrlLinks",
    "models/VTag"
], function(require, Backbone, TagLayoutViewTmpl, Utils, Messages, Globals, UrlLinks, VTag) {
    "use strict";

    var TagLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends TagLayoutView */
        {
            _viewName: "TagLayoutView",

            template: TagLayoutViewTmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                tagsParent: "[data-id='tagsParent']",
                tagsList: "[data-id='tagsList']",
                createTag: "[data-id='createTag']",
                tags: "[data-id='tags']",
                offLineSearchTag: "[data-id='offlineSearchTag']",
                treeLov: "[data-id='treeLov']",
                refreshTag: '[data-id="refreshTag"]',
                tagView: 'input[name="tagView"]',
                expandArrow: '[data-id="expandArrow"]'
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.createTag] = "onClickCreateTag";
                events["click " + this.ui.tags] = "onTagList";
                events["keyup " + this.ui.offLineSearchTag] = "offlineSearchTag";
                events["change " + this.ui.treeLov] = "onTreeSelect";
                events["click " + this.ui.refreshTag] = "fetchCollections";
                events["change " + this.ui.tagView] = "tagViewToggle";
                events["click " + this.ui.expandArrow] = "toggleChild";
                return events;
            },
            /**
             * intialize a new TagLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, "tag", "collection", "typeHeaders", "value", "enumDefCollection"));
                this.viewType = "flat";
                this.query = {
                    flat: {
                        tagName: null
                    },
                    tree: {
                        tagName: null
                    }
                };
                if (Utils.getUrlState.isTagTab() && this.value && this.value.viewType) {
                    this.viewType = this.value.viewType;
                }
                this.query[this.viewType].tagName = this.tag;
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(
                    this.collection.fullCollection,
                    "reset add remove",
                    function() {
                        this.tagsGenerator();
                    },
                    this
                );
                this.ui.tagsList.on("click", "li.parent-node a", function() {
                    that.setUrl(this.getAttribute("href"));
                });
                $("body").on("click", ".tagPopoverOptions li", function(e) {
                    that.$(".tagPopover").popover("hide");
                    that[
                        $(this)
                        .find("a")
                        .data("fn")
                    ](e);
                });
            },
            onRender: function() {
                var that = this;
                this.bindEvents();
                this.tagsGenerator();
            },
            fetchCollections: function() {
                this.collection.fetch({ reset: true });
                this.ui.offLineSearchTag.val("");
            },
            manualRender: function(options) {
                this.tag = options && options.tagName;
                _.extend(this.value, options);
                this.query[this.viewType].tagName = this.tag;
                if (options && options.viewType) {
                    this.viewType = options.viewType;
                }
                if (!this.createTag) {
                    this.setValues(true);
                }
            },
            renderTreeList: function() {
                var that = this;
                this.ui.treeLov.empty();
                var treeStr = "<option></option>";
                this.collection.fullCollection.each(function(model) {
                    var name = Utils.getName(model.toJSON(), "name");
                    treeStr += "<option>" + name + "</option>";
                });
                that.ui.treeLov.html(treeStr);
                that.ui.treeLov.select2({
                    placeholder: "Search Classification",
                    allowClear: true
                });
            },
            onTreeSelect: function() {
                var name = this.ui.treeLov.val();
                Utils.setUrl({
                    url: name ? "#!/tag/tagAttribute/" + name : "#!/tag",
                    urlParams: {
                        viewType: "tree"
                    },
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            setValues: function(manual) {
                var el = this.ui.tagsList;
                if (this.viewType == "tree") {
                    el = this.ui.tagsParent;
                    if (!this.ui.tagView.prop("checked")) {
                        this.ui.tagView.prop("checked", true).trigger("change");
                    }
                } else {
                    if (this.ui.tagView.prop("checked")) {
                        this.ui.tagView.prop("checked", false).trigger("change");
                    }
                }
                var $firstEl = el.find("li a") ? el.find("li a").first() : null;
                if (Utils.getUrlState.isTagTab()) {
                    if (!this.tag) {
                        this.selectFirst = false;
                        el.find("li")
                            .first()
                            .addClass("active");
                        if ($firstEl && $firstEl.length) {
                            url: $firstEl.attr("href"),
                            Utils.setUrl({
                                url: $firstEl.attr("href"),
                                mergeBrowserUrl: false,
                                updateTabState: true
                            });
                        }
                    } else {
                        var presentTag = this.collection.fullCollection.findWhere({ name: this.tag }),
                            url = Utils.getUrlState.getQueryUrl().queyParams[0],
                            tag = this.tag,
                            query = Utils.getUrlState.getQueryParams() || null;
                        if (!presentTag) {
                            tag = $firstEl.data("name");
                            url = $firstEl && $firstEl.length ? $firstEl.attr("href") : "#!/tag";
                            if ($firstEl && $firstEl.length) {
                                _.extend(query, { dlttag: true });
                            }
                        }
                        Utils.setUrl({
                            url: url,
                            urlParams: query,
                            updateTabState: true
                        });
                        if (!presentTag) {
                            return false;
                        }
                        el.find("li").removeClass("active"); // remove selected
                        el.find("li.parent-node").each(function() {
                            // based on browser url select tag.
                            var target = $(this);
                            if (
                                target
                                .children("div")
                                .find("a")
                                .text() === tag
                            ) {
                                target.addClass("active");
                                target
                                    .parents("ul")
                                    .addClass("show")
                                    .removeClass("hide"); // Don't use toggle
                                if (this.createTag || !manual) {
                                    if (target.offset()) {
                                        $("#sidebar-wrapper").animate({
                                                scrollTop: target.offset().top - 100
                                            },
                                            500
                                        );
                                    }
                                }
                                return false;
                            }
                        });
                    }
                }
            },
            tagsGenerator: function(searchString) {
                var tagParents = "",
                    tagLists = "";

                if (this.collection && this.collection.fullCollection.length >= 0) {
                    var sortedCollection = this.collection.fullCollection;
                    this.tagTreeList = this.getTagTreeList({ collection: sortedCollection });
                    if (searchString) {
                        if (this.viewType == "flat") {
                            this.ui.tagsList.empty().html(this.generateTree({ data: sortedCollection, searchString: searchString }));
                        } else {
                            this.ui.tagsParent.empty().html(this.generateTree({ data: this.tagTreeList, isTree: true, searchString: searchString }));
                        }
                    } else {
                        this.ui.tagsParent.empty().html(this.generateTree({ data: this.tagTreeList, isTree: true, searchString: searchString }));
                        this.ui.tagsList.empty().html(this.generateTree({ data: sortedCollection, searchString: searchString }));
                    }
                    this.createTagAction();
                    this.setValues();
                    this.renderTreeList();
                    if (this.createTag) {
                        this.createTag = false;
                    }
                }
            },
            getTagTreeList: function(options) {
                var that = this,
                    collection = options.collection,
                    listOfParents = {},
                    getChildren = function(options) {
                        var children = options.children,
                            data = [];
                        if (children && children.length) {
                            _.each(children, function(name) {
                                var child = collection.find({ name: name });
                                if (child) {
                                    var modelJSON = child.toJSON();
                                    data.push({
                                        name: name,
                                        children: getChildren({ children: modelJSON.subTypes })
                                    });
                                }
                            });
                        }
                        return data;
                    };
                collection.each(function(model) {
                    var modelJSON = model.toJSON();
                    if (modelJSON.superTypes.length == 0) {
                        var name = modelJSON.name;
                        listOfParents[name] = {
                            name: name,
                            children: getChildren({ children: modelJSON.subTypes })
                        };
                    }
                });
                return listOfParents;
            },
            generateTree: function(options) {
                var data = options.data,
                    isTree = options.isTree,
                    searchString = options.searchString,
                    that = this,
                    element = "",
                    getElString = function(options) {
                        var name = options.name,
                            hasChild = isTree && options.children && options.children.length;
                        return (
                            '<li class="parent-node" data-id="tags">' +
                            '<div><div class="tools"><i class="fa fa-ellipsis-h tagPopover"></i></div>' +
                            (hasChild ? '<i class="fa toggleArrow fa-angle-right" data-id="expandArrow" data-name="' + name + '"></i>' : "") +
                            '<a href="#!/tag/tagAttribute/' +
                            name +
                            "?viewType=" +
                            (isTree ? "tree" : "flat") +
                            '"  data-name="' +
                            name +
                            '">' +
                            name +
                            "</a></div>" +
                            (isTree && hasChild ?
                                '<ul class="child hide">' + that.generateTree({ data: options.children, isTree: isTree }) + "</ul>" :
                                "") +
                            "</li>"
                        );
                    };
                if (isTree) {
                    _.each(data, function(obj) {
                        element += getElString({
                            name: obj.name,
                            children: obj.children
                        });
                    });
                } else {
                    data.each(function(obj) {
                        var name = obj.get("name");
                        if (searchString) {
                            if (name.search(new RegExp(searchString, "i")) != -1) {
                                element += getElString({
                                    name: obj.get("name"),
                                    children: null
                                });
                            } else {
                                return;
                            }
                        } else {
                            element += getElString({
                                name: obj.get("name"),
                                children: null
                            });
                        }
                    });
                }
                return element;
            },
            toggleChild: function(e) {
                var el = $(e.currentTarget);
                if (el) {
                    el.parent()
                        .siblings("ul.child")
                        .toggleClass("hide show");
                }
            },
            tagViewToggle: function(e) {
                if (e.currentTarget.checked) {
                    this.$(".tree-view").show();
                    this.$(".list-view").hide();
                    this.viewType = "tree";
                } else {
                    this.viewType = "flat";
                    this.$(".tree-view").hide();
                    this.$(".list-view").show();
                }
                if (Utils.getUrlState.isTagTab()) {
                    var name = this.query[this.viewType].tagName;
                    Utils.setUrl({
                        url: name ? "#!/tag/tagAttribute/" + name : "#!/tag",
                        urlParams: {
                            viewType: this.viewType
                        },
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
            },
            onClickCreateTag: function(e) {
                var that = this,
                    nodeName = e.currentTarget.nodeName;
                $(e.currentTarget).attr("disabled", "true");
                require(["views/tag/CreateTagLayoutView", "modules/Modal"], function(CreateTagLayoutView, Modal) {
                    var name = !(nodeName == "BUTTON") ? that.query[that.viewType].tagName : null;
                    var view = new CreateTagLayoutView({ tagCollection: that.collection, selectedTag: name, enumDefCollection: enumDefCollection }),
                        modal = new Modal({
                            title: "Create a new classification",
                            content: view,
                            cancelText: "Cancel",
                            okCloses: false,
                            okText: "Create",
                            allowCancel: true
                        }).open();
                    modal.$el.find("button.ok").attr("disabled", "true");
                    view.ui.tagName.on("keyup", function(e) {
                        modal.$el.find("button.ok").removeAttr("disabled");
                    });
                    view.ui.tagName.on("keyup", function(e) {
                        if ((e.keyCode == 8 || e.keyCode == 32 || e.keyCode == 46) && e.currentTarget.value.trim() == "") {
                            modal.$el.find("button.ok").attr("disabled", "true");
                        }
                    });
                    modal.on("shownModal", function() {
                        view.ui.parentTag.select2({
                            multiple: true,
                            placeholder: "Search Classification",
                            allowClear: true
                        });
                    });
                    modal.on("ok", function() {
                        modal.$el.find("button.ok").attr("disabled", "true");
                        that.onCreateButton(view, modal);
                    });
                    modal.on("closeModal", function() {
                        modal.trigger("cancel");
                        that.ui.createTag.removeAttr("disabled");
                    });
                });
            },
            onCreateButton: function(ref, modal) {
                var that = this;
                var validate = true;
                if (modal.$el.find(".attributeInput").length > 0) {
                    modal.$el.find(".attributeInput").each(function() {
                        if ($(this).val() === "") {
                            $(this).css("borderColor", "red");
                            validate = false;
                        }
                    });
                }
                modal.$el.find(".attributeInput").keyup(function() {
                    $(this).css("borderColor", "#e8e9ee");
                    modal.$el.find("button.ok").removeAttr("disabled");
                });
                if (!validate) {
                    Utils.notifyInfo({
                        content: "Please fill the attributes or delete the input box"
                    });
                    return;
                }

                this.name = ref.ui.tagName.val();
                this.description = ref.ui.description.val();
                var superTypes = [];
                if (ref.ui.parentTag.val() && ref.ui.parentTag.val()) {
                    superTypes = ref.ui.parentTag.val();
                }
                var attributeObj = ref.collection.toJSON();
                if (ref.collection.length === 1 && ref.collection.first().get("name") === "") {
                    attributeObj = [];
                }

                if (attributeObj.length) {
                    var superTypesAttributes = [];
                    _.each(superTypes, function(name) {
                        var parentTags = that.collection.fullCollection.findWhere({ name: name });
                        superTypesAttributes = superTypesAttributes.concat(parentTags.get("attributeDefs"));
                    });

                    var duplicateAttributeList = [];
                    _.each(attributeObj, function(obj) {
                        var duplicateCheck = _.find(superTypesAttributes, function(activeTagObj) {
                            return activeTagObj.name.toLowerCase() === obj.name.toLowerCase();
                        });
                        if (duplicateCheck) {
                            duplicateAttributeList.push(obj.name);
                        }
                    });
                    var notifyObj = {
                        modal: true,
                        confirm: {
                            confirm: true,
                            buttons: [{
                                    text: "Ok",
                                    addClass: "btn-atlas btn-md",
                                    click: function(notice) {
                                        notice.remove();
                                    }
                                },
                                null
                            ]
                        }
                    };
                    if (duplicateAttributeList.length) {
                        if (duplicateAttributeList.length < 2) {
                            var text = "Attribute <b>" + duplicateAttributeList.join(",") + "</b> is duplicate !";
                        } else {
                            if (attributeObj.length > duplicateAttributeList.length) {
                                var text = "Attributes: <b>" + duplicateAttributeList.join(",") + "</b> are duplicate !";
                            } else {
                                var text = "All attributes are duplicate !";
                            }
                        }
                        notifyObj["text"] = text;
                        Utils.notifyConfirm(notifyObj);
                        return false;
                    }
                }
                this.json = {
                    classificationDefs: [{
                        name: this.name.trim(),
                        description: this.description.trim(),
                        superTypes: superTypes.length ? superTypes : [],
                        attributeDefs: attributeObj
                    }],
                    entityDefs: [],
                    enumDefs: [],
                    structDefs: []
                };
                new this.collection.model().set(this.json).save(null, {
                    success: function(model, response) {
                        var classificationDefs = model.get("classificationDefs");
                        that.ui.createTag.removeAttr("disabled");
                        that.createTag = true;
                        if (classificationDefs[0]) {
                            _.each(classificationDefs[0].superTypes, function(superType) {
                                var superTypeModel = that.collection.fullCollection.find({ name: superType }),
                                    subTypes = [];
                                if (superTypeModel) {
                                    subTypes = superTypeModel.get("subTypes");
                                    subTypes.push(classificationDefs[0].name);
                                    superTypeModel.set({ subTypes: _.uniq(subTypes) });
                                }
                            });
                        }
                        that.collection.fullCollection.add(classificationDefs);
                        that.setUrl("#!/tag/tagAttribute/" + ref.ui.tagName.val(), true);
                        Utils.notifySuccess({
                            content: "Classification " + that.name + Messages.getAbbreviationMsg(false, 'addSuccessMessage')
                        });
                        modal.trigger("cancel");
                        that.typeHeaders.fetch({ reset: true });
                    }
                });
            },
            setUrl: function(url, create) {
                Utils.setUrl({
                    url: url,
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            onTagList: function(e, toggle) {
                var that = this;
                e.stopPropagation();
                if (e.target.nodeName === "A") {
                    that.$(".tagPopover").popover("hide");
                    $(e.currentTarget)
                        .parents("ul.tag-tree")
                        .find("li.active")
                        .removeClass("active");
                    $(e.currentTarget).addClass("active");
                }
            },
            offlineSearchTag: function(e) {
                var type = $(e.currentTarget).data("type");
                this.tagsGenerator($(e.currentTarget).val());
            },
            createTagAction: function() {
                var that = this;
                Utils.generatePopover({
                    el: this.$(".tagPopover"),
                    contentClass: "tagPopoverOptions",
                    popoverOptions: {
                        content: function() {
                            return (
                                "<ul>" +
                                "<li class='listTerm' ><i class='fa fa-search'></i> <a href='javascript:void(0)' data-fn='onSearchTag'>Search Classification</a></li>" +
                                "<li class='listTerm' ><i class='fa fa-plus'></i> <a href='javascript:void(0)' data-fn='onClickCreateTag'>Create Sub-classification</a></li>" +
                                "<li class='listTerm' ><i class='fa fa-trash-o'></i> <a href='javascript:void(0)' data-fn='onDeleteTag'>Delete Classification</a></li>" +
                                "</ul>"
                            );
                        }
                    }
                });
            },
            onSearchTag: function() {
                var el = this.ui.tagsList;
                if (this.viewType == "tree") {
                    el = this.ui.tagsParent;
                }
                Utils.setUrl({
                    url: "#!/search/searchResult",
                    urlParams: {
                        tag: el
                            .find("li.active")
                            .find("a[data-name]")
                            .data("name"),
                        searchType: "basic",
                        dslChecked: false
                    },
                    mergeBrowserUrl: false,
                    trigger: true,
                    updateTabState: true
                });
            },
            onDeleteTag: function() {
                var that = this,
                    notifyObj = {
                        modal: true,
                        ok: function(argument) {
                            that.onNotifyOk();
                        },
                        cancel: function(argument) {}
                    };
                var text = "Are you sure you want to delete the classification";
                notifyObj["text"] = text;
                Utils.notifyConfirm(notifyObj);
            },
            onNotifyOk: function(data) {
                var that = this,
                    deleteTagData = this.collection.fullCollection.findWhere({ name: this.tag });
                deleteTagData.deleteTag({
                    typeName: that.tag,
                    success: function() {
                        Utils.notifySuccess({
                            content: "Classification " + that.tag + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                        });
                        // if deleted tag is prviously searched then remove that tag url from save state of tab.
                        var searchUrl = Globals.saveApplicationState.tabState.searchUrl;
                        var urlObj = Utils.getUrlState.getQueryParams(searchUrl);
                        if (urlObj && urlObj.tag && urlObj.tag === that.tag) {
                            Globals.saveApplicationState.tabState.searchUrl = "#!/search";
                        }
                        that.collection.fullCollection.remove(deleteTagData);
                        // to update tag list of search tab fetch typeHeaders.
                        that.typeHeaders.fetch({ reset: true });
                    }
                });
            }
        }
    );
    return TagLayoutView;
});