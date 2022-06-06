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
    'hbs!tmpl/business_metadata/BusinessMetadataAttrTableLayoutView_tmpl',
    'collection/VEntityList'
], function(require, Backbone, BusinessMetadataAttrTableLayoutView_tmpl, VEntityList) {
    'use strict';

    var BusinessMetadataAttrTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends BusinessMetadataAttrTableLayoutView */
        {
            _viewName: 'BusinessMetadataAttrTableLayoutView',

            template: BusinessMetadataAttrTableLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RBusinessMetadataAttrTableLayoutView: "#r_businessMetadataAttrTableLayoutView",
                RModal: "#r_modal"
            },

            /** ui selector cache */
            ui: {
                attributeEdit: "[data-id='attributeEdit']",
                addAttribute: '[data-id="addAttribute"]',
                businessMetadataAttrPage: "[data-id='businessMetadataAttrPage']",
                businessMetadataAttrPageTitle: "[data-id='businessMetadataAttrPageTitle']",
                businessMetadataDetailPage: "[data-id='businessMetadataDetailPage']",
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.attributeEdit] = "onEditAttr";
                events["click " + this.ui.addAttribute] = "onEditAttr";
                return events;
            },
            /**
             * intialize a new BusinessMetadataAttrTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'model', 'typeHeaders', 'businessMetadataDefCollection', 'entityDefCollection'));
                this.businessMetadataAttr = new VEntityList(this.model.get("attributeDefs") || []);
                this.commonTableOptions = {
                    collection: this.businessMetadataAttr,
                    includeFilter: false,
                    includePagination: false,
                    includePageSize: false,
                    includeAtlasTableSorting: true,
                    includeFooterRecords: false,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.showDetails = true;
            },
            onRender: function() {
                this.renderTableLayoutView();
                this.toggleBusinessMetadataDetailsAttrView();
            },
            bindEvents: function() {},
            toggleBusinessMetadataDetailsAttrView: function() {
                var that = this;
                if (that.showDetails) {
                    that.ui.businessMetadataAttrPage.hide();
                    that.ui.businessMetadataDetailPage.show();
                } else {
                    that.ui.businessMetadataAttrPage.show();
                    that.ui.businessMetadataDetailPage.hide();
                }
            },
            onEditAttr: function(e) {
                var that = this,
                    isAttrEdit = false,
                    selectedBusinessMetadata = that.model,
                    attrributes = selectedBusinessMetadata ? selectedBusinessMetadata.get('attributeDefs') : null,
                    attrName = e.target.dataset.name ? e.target.dataset.name : null,
                    attrDetails = { name: attrName };
                if (e.target.dataset.action == 'attributeEdit') {
                    isAttrEdit = true
                }
                if (selectedBusinessMetadata) {
                    that.newAttr = isAttrEdit ? false : true;
                    _.each(attrributes, function(attrObj) {
                        if (attrObj.name === attrName) {
                            attrDetails = $.extend(true, {}, attrObj);
                            if (attrObj.typeName.includes('array')) {
                                attrDetails.typeName = attrObj.typeName.replace("array<", "").replace(">", "");
                                attrDetails.multiValued = true;
                            }
                        }
                    });
                    this.showDetails = false;
                    that.toggleBusinessMetadataDetailsAttrView();
                    require(["views/business_metadata/CreateBusinessMetadataLayoutView"], function(CreateBusinessMetadataLayoutView) {
                        that.view = new CreateBusinessMetadataLayoutView({
                            onEditCallback: function() {
                                enumDefCollection.fetch({ reset: true });
                                that.businessMetadataAttr.reset(that.model.get("attributeDefs"));
                            },
                            onUpdateBusinessMetadata: function(fetch) {
                                that.showDetails = true;
                                that.toggleBusinessMetadataDetailsAttrView();
                                if (fetch) {
                                    that.entityDefCollection.fetch({ silent: true });
                                }
                            },
                            parent: that.$el,
                            businessMetadataDefCollection: that.businessMetadataDefCollection,
                            enumDefCollection: enumDefCollection,
                            isAttrEdit: isAttrEdit,
                            attrDetails: attrDetails,
                            typeHeaders: typeHeaders,
                            selectedBusinessMetadata: that.model,
                            guid: that.guid,
                            isNewAttr: that.newAttr
                        });
                        if (isAttrEdit) {
                            that.ui.businessMetadataAttrPageTitle.text("Update Attribute of: " + selectedBusinessMetadata.get('name'));
                        } else {
                            that.ui.businessMetadataAttrPageTitle.text("Add Business Metadata Attribute for: " + selectedBusinessMetadata.get('name'));
                        }
                        that.RModal.show(that.view);
                    });
                }

            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getBusinessMetadataTableColumns());
                    that.RBusinessMetadataAttrTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));
                });
            },
            getBusinessMetadataTableColumns: function() {
                var that = this;
                return this.businessMetadataAttr.constructor.getTableCols({
                    name: {
                        label: "Attribute Name",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return _.escape(model.get('name'));
                            }
                        })
                    },
                    typeName: {
                        label: "Type Name",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return _.escape(model.get('typeName'));
                            }
                        })
                    },
                    searchWeight: {
                        label: "Search Weight",
                        cell: "String",
                        editable: false
                    },
                    enableMultipleValue: {
                        label: "Enable Multivalues",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var enableMultipleValue = '';
                                if (model.get('typeName').indexOf('array<') > -1) {
                                    enableMultipleValue = 'checked';
                                }
                                return '<input type="checkbox" class="form-check-input multi-value-select" data-id="multiValueSelectStatus" ' + enableMultipleValue + ' disabled="disabled">';
                            }
                        })
                    },
                    maxStrLength: {
                        label: "Max Length",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var maxString = "NA";
                                if (model.get('typeName').indexOf('string') > -1) {
                                    maxString = model.get('options').maxStrLength || maxString;
                                }
                                return maxString;
                            }
                        })
                    },
                    applicableEntityTypes: {
                        label: "Entity Type(s)",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var options = model.get('options')
                                if (options && options.applicableEntityTypes) {
                                    var applicableEntityTypes = '',
                                        attrEntityTypes = JSON.parse(options.applicableEntityTypes);
                                    _.each(attrEntityTypes, function(values) {
                                        applicableEntityTypes += '<label class="btn btn-action btn-xs btn-blue no-pointer">' + values + '</label>';
                                    });
                                    return applicableEntityTypes;
                                }
                            }
                        })
                    },
                    tool: {
                        label: "Action",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return '<div class="btn btn-action btn-sm" data-id="attributeEdit" data-action="attributeEdit" data-name="' + model.get('name') + '">Edit</div>';
                            }
                        })
                    }
                }, this.businessMetadataAttr);
            }
        });
    return BusinessMetadataAttrTableLayoutView;
});