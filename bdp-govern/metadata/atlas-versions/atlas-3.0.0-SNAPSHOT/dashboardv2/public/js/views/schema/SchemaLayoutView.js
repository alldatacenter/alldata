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
    'hbs!tmpl/schema/SchemaTableLayoutView_tmpl',
    'collection/VSchemaList',
    'utils/Utils',
    'utils/CommonViewFunction',
    'utils/Messages',
    'utils/Globals',
    'utils/Enums',
    'utils/UrlLinks'
], function(require, Backbone, SchemaTableLayoutViewTmpl, VSchemaList, Utils, CommonViewFunction, Messages, Globals, Enums, UrlLinks) {
    'use strict';

    var SchemaTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends SchemaTableLayoutView */
        {
            _viewName: 'SchemaTableLayoutView',

            template: SchemaTableLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                RSchemaTableLayoutView: "#r_schemaTableLayoutView",
            },
            /** ui selector cache */
            ui: {
                tagClick: '[data-id="tagClick"]',
                addTag: "[data-id='addTag']",
                addAssignTag: "[data-id='addAssignTag']",
                checkDeletedEntity: "[data-id='checkDeletedEntity']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.addTag] = 'checkedValue';
                events["click " + this.ui.addAssignTag] = 'checkedValue';
                events["click " + this.ui.tagClick] = function(e) {
                    if (e.target.nodeName.toLocaleLowerCase() == "i") {
                        this.onClickTagCross(e);
                    } else {
                        var value = e.currentTarget.text;
                        Utils.setUrl({
                            url: '#!/tag/tagAttribute/' + value,
                            mergeBrowserUrl: false,
                            trigger: true
                        });
                    }
                };
                events["click " + this.ui.checkDeletedEntity] = 'onCheckDeletedEntity';

                return events;
            },
            /**
             * intialize a new SchemaTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'classificationDefCollection', 'entityDefCollection', 'attribute', 'fetchCollection', 'enumDefCollection'));
                this.schemaCollection = new VSchemaList([], {});
                this.commonTableOptions = {
                    collection: this.schemaCollection,
                    includeFilter: false,
                    includePagination: true,
                    includePageSize: true,
                    includeGotoPage: true,
                    includeFooterRecords: true,
                    includeOrderAbleColumns: false,
                    includeAtlasTableSorting: true,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.bindEvents();
                this.bradCrumbList = [];
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.schemaCollection, 'backgrid:selected', function(model, checked) {
                    this.arr = [];
                    if (checked === true) {
                        model.set("isEnable", true);
                    } else {
                        model.set("isEnable", false);
                    }
                    this.schemaCollection.find(function(item) {
                        var obj = item.toJSON();
                        if (item.get('isEnable')) {
                            that.arr.push({
                                id: obj.guid,
                                model: obj
                            });
                        }
                    });
                    if (this.arr.length > 0) {
                        this.$('.multiSelectTag').show();
                    } else {
                        this.$('.multiSelectTag').hide();
                    }
                });
            },
            onRender: function() {
                this.generateTableData();
            },
            generateTableData: function(checkedDelete) {
                var that = this,
                    newModel;
                this.activeObj = [];
                this.deleteObj = [];
                this.schemaTableAttribute = null;
                if (this.attribute && this.attribute[0]) {
                    var firstColumn = this.attribute[0],
                        defObj = that.entityDefCollection.fullCollection.find({ name: firstColumn.typeName });
                    if (defObj && defObj.get('options') && defObj.get('options').schemaAttributes) {
                        if (firstColumn) {
                            try {
                                var mapObj = JSON.parse(defObj.get('options').schemaAttributes);
                                that.schemaTableAttribute = _.pick(firstColumn.attributes, mapObj);
                            } catch (e) {}
                        }
                    }
                }
                _.each(this.attribute, function(obj) {
                    if (!Enums.entityStateReadOnly[obj.status]) {
                        that.activeObj.push(obj);
                        that.schemaCollection.push(obj);
                    } else if (Enums.entityStateReadOnly[obj.status]) {
                        that.deleteObj.push(obj);
                    }
                });
                if (this.schemaCollection.length === 0 && this.deleteObj.length) {
                    this.ui.checkDeletedEntity.find("input").prop('checked', true);
                    this.schemaCollection.fullCollection.reset(this.deleteObj);
                }
                if (this.activeObj.length === 0 && this.deleteObj.length === 0) {
                    this.ui.checkDeletedEntity.hide();
                }
                this.renderTableLayoutView();
            },
            showLoader: function() {
                this.$('.fontLoader').show();
                this.$('.tableOverlay').show()
            },
            hideLoader: function(argument) {
                this.$('.fontLoader').hide();
                this.$('.tableOverlay').hide();
            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var columnCollection = Backgrid.Columns.extend({}),
                        columns = new columnCollection(that.getSchemaTableColumns());
                    that.RSchemaTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: columns
                    })));
                    that.$('.multiSelectTag').hide();
                    Utils.generatePopover({
                        el: that.$('[data-id="showMoreLess"]'),
                        contentClass: 'popover-tag-term',
                        viewFixedPopover: true,
                        popoverOptions: {
                            container: null,
                            content: function() {
                                return $(this).find('.popup-tag-term').children().clone();
                            }
                        }
                    });
                });
            },
            getSchemaTableColumns: function() {
                var that = this,
                    col = {
                        Check: {
                            name: "selected",
                            label: "",
                            cell: "select-row",
                            headerCell: "select-all"
                        }
                    }
                if (this.schemaTableAttribute) {
                    _.each(_.keys(this.schemaTableAttribute), function(key) {
                        if (key !== "position") {
                            col[key] = {
                                label: key.capitalize(),
                                cell: "html",
                                editable: false,
                                className: "searchTableName",
                                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                    fromRaw: function(rawValue, model) {
                                        var value = _.escape(model.get('attributes')[key]);
                                        if (key === "name" && model.get('guid')) {
                                            var nameHtml = '<a href="#!/detailPage/' + model.get('guid') + '">' + value + '</a>';
                                            if (model.get('status') && Enums.entityStateReadOnly[model.get('status')]) {
                                                nameHtml += '<button type="button" title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i></button>';
                                                return '<div class="readOnly readOnlyLink">' + nameHtml + '</div>';
                                            } else {
                                                return nameHtml;
                                            }
                                        } else {
                                            return value
                                        }
                                    }
                                })
                            };
                        }
                    });
                    col['tag'] = {
                        label: "Classifications",
                        cell: "Html",
                        editable: false,
                        sortable: false,
                        className: 'searchTag',
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var obj = model.toJSON();
                                if (obj.status && Enums.entityStateReadOnly[obj.status]) {
                                    return '<div class="readOnly">' + CommonViewFunction.tagForTable(obj); + '</div>';
                                } else {
                                    return CommonViewFunction.tagForTable(obj);
                                }
                            }
                        })
                    };
                    return this.schemaCollection.constructor.getTableCols(col, this.schemaCollection);
                }

            },
            checkedValue: function(e) {
                if (e) {
                    e.stopPropagation();
                }
                var guid = "",
                    that = this,
                    isTagMultiSelect = $(e.currentTarget).hasClass('multiSelectTag');
                if (isTagMultiSelect && this.arr && this.arr.length) {
                    that.addTagModalView(guid, this.arr);
                } else {
                    guid = that.$(e.currentTarget).data("guid");
                    that.addTagModalView(guid);
                }
            },
            addTagModalView: function(guid, multiple) {
                var that = this;
                var tagList = that.schemaCollection.find({ 'guid': guid });
                require(['views/tag/AddTagModalView'], function(AddTagModalView) {
                    var view = new AddTagModalView({
                        guid: guid,
                        multiple: multiple,
                        tagList: _.map((tagList ? tagList.get('classifications') : []), function(obj) {
                            return obj.typeName;
                        }),
                        callback: function() {
                            that.fetchCollection();
                            that.arr = [];
                        },
                        hideLoader: that.hideLoader.bind(that),
                        showLoader: that.showLoader.bind(that),
                        collection: that.classificationDefCollection,
                        enumDefCollection: that.enumDefCollection
                    });
                });
            },
            onClickTagCross: function(e) {
                var that = this,
                    tagName = $(e.target).data("name"),
                    guid = $(e.target).data("guid"),
                    assetName = $(e.target).data("assetname");
                CommonViewFunction.deleteTag({
                    tagName: tagName,
                    guid: guid,
                    msg: "<div class='ellipsis-with-margin'>Remove: " + "<b>" + _.escape(tagName) + "</b> assignment from <b>" + _.escape(assetName) + " ?</b></div>",
                    titleMessage: Messages.removeTag,
                    okText: "Remove",
                    showLoader: that.showLoader.bind(that),
                    hideLoader: that.hideLoader.bind(that),
                    callback: function() {
                        that.fetchCollection();
                    }
                });
            },
            onCheckDeletedEntity: function(e) {
                if (e.target.checked) {
                    if (this.deleteObj.length) {
                        this.schemaCollection.fullCollection.reset(this.activeObj.concat(this.deleteObj));
                    }
                } else {
                    this.schemaCollection.fullCollection.reset(this.activeObj)
                }
            }
        });
    return SchemaTableLayoutView;
});