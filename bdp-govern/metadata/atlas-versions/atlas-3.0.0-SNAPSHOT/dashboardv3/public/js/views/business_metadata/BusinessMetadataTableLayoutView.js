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
    'hbs!tmpl/business_metadata/BusinessMetadataTableLayoutView_tmpl',
    'utils/Utils',
    'utils/Messages'
], function(require, Backbone, BusinessMetadataTableLayoutView_tmpl, Utils, Messages) {
    'use strict';

    var BusinessMetadataTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends BusinessMetadataTableLayoutView */
        {
            _viewName: 'BusinessMetadataTableLayoutView',

            template: BusinessMetadataTableLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RBusinessMetadataTableLayoutView: "#r_businessMetadataTableLayoutView",
                RModal: "#r_modal"
            },

            /** ui selector cache */
            ui: {
                businessMetadataAttrPage: "[data-id='businessMetadataAttrPage']",
                businessMetadataAttrPageTitle: "[data-id='businessMetadataAttrPageTitle']",
                businessMetadataDetailPage: "[data-id='businessMetadataDetailPage']",
                createBusinessMetadata: "[data-id='createBusinessMetadata']",
                attributeEdit: "[data-id='attributeEdit']",
                addAttribute: '[data-id="addAttribute"]',
                businessMetadataAttrPageOk: '[data-id="businessMetadataAttrPageOk"]',
                colManager: "[data-id='colManager']",
                deleteBusinessMetadata: '[data-id="deleteBusinessMetadata"]'
            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.createBusinessMetadata] = "onClickCreateBusinessMetadata";
                events["click " + this.ui.addAttribute] = "onEditAttr";
                events["click " + this.ui.attributeEdit] = "onEditAttr";
                events["click " + this.ui.deleteBusinessMetadata] = function(e) {
                    that.guid = e.target.dataset.guid;
                    that.deleteBusinessMetadataElement();
                };
                return events;
            },
            /**
             * intialize a new BusinessMetadataTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'guid', 'entity', 'entityName', 'attributeDefs', 'typeHeaders', 'businessMetadataDefCollection', 'entityDefCollection', 'businessMetadataAttr', 'selectedBusinessMetadata'));
                this.limit = 10;
                this.newAttr = false;
                this.commonTableOptions = {
                    collection: this.businessMetadataDefCollection,
                    includeFilter: false,
                    includePagination: true,
                    includeFooterRecords: true,
                    includePageSize: true,
                    includeGotoPage: true,
                    includeAtlasTableSorting: true,
                    includeTableLoader: true,
                    includeColumnManager: true,
                    gridOpts: {
                        className: "table table-hover backgrid table-quickMenu",
                        emptyText: 'No records found!'
                    },
                    columnOpts: {
                        opts: {
                            initialColumnsVisible: null,
                            saveState: false
                        },
                        visibilityControlOpts: {
                            buttonTemplate: _.template("<button class='btn btn-action btn-sm pull-right'>Columns&nbsp<i class='fa fa-caret-down'></i></button>")
                        },
                        el: this.ui.colManager
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.guid = null;
                this.showDetails = true; // toggle between sttribute page and detail page
            },
            onRender: function() {
                this.toggleBusinessMetadataDetailsAttrView();
                $.extend(this.businessMetadataDefCollection.queryParams, { count: this.limit });
                this.businessMetadataDefCollection.fullCollection.sort({ silent: true });
                this.renderTableLayoutView();
                this.$('.tableOverlay').hide();
                this.$('.auditTable').show(); // Only for first time table show because we never hide after first render.
                this.businessMetadataDefCollection.comparator = function(model) {
                    return -model.get('timestamp');
                }
            },
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
            bindEvents: function() {},
            loaderStatus: function(isActive) {
                var that = this;
                if (isActive) {
                    that.$('.businessMetadata-attr-tableOverlay').show();
                    that.$('.business-metadata-attr-fontLoader').show();
                } else {
                    that.$('.businessMetadata-attr-tableOverlay').hide();
                    that.$('.business-metadata-attr-fontLoader').hide();
                }
            },
            onEditAttr: function(e) {
                var that = this,
                    isAttrEdit = e.currentTarget.dataset && e.currentTarget.dataset.id === 'attributeEdit' ? true : false,
                    guid = e.currentTarget.dataset && e.currentTarget.dataset.guid ? e.currentTarget.dataset.guid : null,
                    selectedBusinessMetadata = that.businessMetadataDefCollection.fullCollection.findWhere({ guid: guid }),
                    attrributes = selectedBusinessMetadata ? selectedBusinessMetadata.get('attributeDefs') : null,
                    attrName = e.currentTarget.dataset.name ? e.currentTarget.dataset.name : null,
                    attrDetails = { name: attrName };
                if (selectedBusinessMetadata) {
                    that.ui.businessMetadataAttrPageOk.text("Save");
                    that.newAttr = e.currentTarget && e.currentTarget.dataset.action === "createAttr" ? true : false;
                    that.guid = guid;
                    _.each(attrributes, function(attrObj) {
                        if (attrObj.name === attrName) {
                            attrDetails = $.extend(true, {}, attrObj);
                            if (attrObj.typeName.includes('array')) {
                                attrDetails.typeName = attrObj.typeName.replace("array<", "").replace(">", "");
                                attrDetails.multiValued = true;
                            }
                        }
                    });

                    that.showDetails = false;
                    that.toggleBusinessMetadataDetailsAttrView();
                    that.ui.businessMetadataAttrPageOk.attr('data-action', e.currentTarget.dataset.id);
                    require(["views/business_metadata/CreateBusinessMetadataLayoutView"], function(CreateBusinessMetadataLayoutView) {
                        that.view = new CreateBusinessMetadataLayoutView({
                            onEditCallback: function() {
                                that.businessMetadataDefCollection.fullCollection.sort({ silent: true });
                                that.renderTableLayoutView();
                            },
                            onUpdateBusinessMetadata: function(fetch) {
                                that.showDetails = true;
                                that.toggleBusinessMetadataDetailsAttrView();
                                if (fetch) {
                                    enumDefCollection.fetch({ reset: true });
                                    that.entityDefCollection.fetch({ silent: true });
                                }
                            },
                            parent: that.$el,
                            businessMetadataDefCollection: that.businessMetadataDefCollection,
                            enumDefCollection: enumDefCollection,
                            isAttrEdit: isAttrEdit,
                            typeHeaders: typeHeaders,
                            attrDetails: attrDetails,
                            selectedBusinessMetadata: selectedBusinessMetadata,
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
            onClickCreateBusinessMetadata: function(e) {
                var that = this,
                    isNewBusinessMetadata = true;
                that.showDetails = false;
                that.ui.businessMetadataAttrPageOk.text("Create");
                that.ui.businessMetadataAttrPageOk.attr('data-action', 'createBusinessMetadata');
                that.ui.businessMetadataAttrPageTitle.text("Create Business Metadata");
                that.toggleBusinessMetadataDetailsAttrView();
                require(["views/business_metadata/CreateBusinessMetadataLayoutView"], function(CreateBusinessMetadataLayoutView) {
                    that.view = new CreateBusinessMetadataLayoutView({
                        onUpdateBusinessMetadata: function(fetch) {
                            that.showDetails = true;
                            that.toggleBusinessMetadataDetailsAttrView();
                            if (fetch) {
                                enumDefCollection.fetch({ reset: true });
                                that.entityDefCollection.fetch({ silent: true });
                            }
                        },
                        businessMetadataDefCollection: that.businessMetadataDefCollection,
                        enumDefCollection: enumDefCollection,
                        typeHeaders: typeHeaders,
                        isNewBusinessMetadata: isNewBusinessMetadata
                    });
                    that.RModal.show(that.view);
                });
            },
            renderTableLayoutView: function() {
                var that = this;
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getBusinessMetadataTableColumns());
                    that.RBusinessMetadataTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));
                    if (!(that.businessMetadataDefCollection.models.length < that.limit)) {
                        that.RBusinessMetadataTableLayoutView.$el.find('table tr').last().hide();
                    }

                });
            },
            getBusinessMetadataTableColumns: function() {
                var that = this;
                return this.businessMetadataDefCollection.constructor.getTableCols({
                    attributeDefs: {
                        label: "",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        cell: Backgrid.ExpandableCell,
                        fixWidth: "20",
                        accordion: false,
                        alwaysVisible: true,
                        expand: function(el, model) {
                            el.attr('colspan', '8');
                            var attrValues = '',
                                attrTable = $('table'),
                                attrTableBody = $('tbody'),
                                attrTableHeading = "<thead><td style='display:table-cell'><b>Attribute</b></td><td style='display:table-cell'><b>Type</b></td><td style='display:table-cell'><b>Search Weight</b></td><td style='display:table-cell'><b>Enable Multivalues</b></td><td style='display:table-cell'><b>Max Length</b></td><td style='display:table-cell'><b>Applicable Type(s)</b></td><td style='display:table-cell'><b>Action</b></td></thead>",
                                attrRow = '',
                                attrTableDetails = '';
                            if (model.attributes && model.attributes.attributeDefs.length) {
                                _.each(model.attributes.attributeDefs, function(attrObj) {
                                    var applicableEntityTypes = '',
                                        typeName = attrObj.typeName,
                                        multiSelect = '',
                                        maxString = 'NA';
                                    if (attrObj.options && attrObj.options.applicableEntityTypes) {
                                        var entityTypes = JSON.parse(attrObj.options.applicableEntityTypes);
                                        _.each(entityTypes, function(values) {
                                            applicableEntityTypes += '<label class="btn btn-action btn-xs btn-blue no-pointer">' + values + '</label>';
                                        })
                                    }
                                    if (typeName.includes('array')) {
                                        typeName = _.escape(typeName);
                                        multiSelect = 'checked';
                                    }
                                    if (typeName.includes('string') && attrObj.options && attrObj.options.maxStrLength) {
                                        maxString = attrObj.options.maxStrLength;
                                    }

                                    attrRow += "<tr> <td style='display:table-cell'>" + _.escape(attrObj.name) + "</td><td style='display:table-cell'>" + typeName + "</td><td style='display:table-cell'>" + _.escape(attrObj.searchWeight) + "</td><td style='display:table-cell'><input type='checkbox' class='form-check-input multi-value-select' " + multiSelect + " disabled='disabled'> </td><td style='display:table-cell'>" + maxString + "</td><td style='display:table-cell'>" + applicableEntityTypes + "</td><td style='display:table-cell'> <div class='btn btn-action btn-sm' style='margin-left:0px;' data-id='attributeEdit' data-guid='" + model.get('guid') + "' data-name ='" + _.escape(attrObj.name) + "' data-action='attributeEdit' >Edit</div> </td></tr> ";
                                });
                                var adminText = '<div class="row"><div class="col-sm-12 attr-details"><table style="padding: 50px;">' + attrTableHeading + attrRow + '</table></div></div>';
                                $(el).append($('<div>').html(adminText));
                            } else {
                                var adminText = '<div class="row"><div class="col-sm-12 attr-details"><h5 class="text-center"> No attributes to show.</h5></div></div>';
                                $(el).append($('<div>').html(adminText));
                            }
                        }
                    },
                    name: {
                        label: "Name",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return '<a title= "' + model.get('name') + '" href ="#!/administrator/businessMetadata/' + model.get('guid') + '?from=bm">' + model.get('name') + '</a>';
                            }
                        })
                    },
                    description: {
                        label: "Description",
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return _.escape(model.get('description'));
                            }
                        })
                    },
                    createdBy: {
                        label: "Created by",
                        cell: "html",
                        renderable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return model.get('updatedBy');
                            }
                        })
                    },
                    createTime: {
                        label: "Created on",
                        cell: "html",
                        renderable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: model.get('createTime') });
                            }
                        })
                    },
                    updatedBy: {
                        label: "Updated by",
                        cell: "html",
                        renderable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return model.get('updatedBy');
                            }
                        })
                    },
                    updateTime: {
                        label: "Updated on",
                        cell: "html",
                        renderable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: model.get('updateTime') });
                            }
                        })
                    },
                    tools: {
                        label: "Action",
                        cell: "html",
                        sortable: false,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return "<button type='button' data-id='addAttribute' data-guid='" + model.get('guid') + "' class='btn btn-action btn-sm' style='margin-bottom: 10px;' data-action='createAttr' data-original-title='Add Business Metadata attribute'><i class='fa fa-plus'></i> Attributes</button>";
                            }
                        })
                    }
                }, this.businessMetadataDefCollection);
            },
            deleteBusinessMetadataElement: function(businessMetadataName) {
                var that = this,
                    notifyObj = {
                        modal: true,
                        ok: function(argument) {
                            that.onNotifyDeleteOk();
                        },
                        cancel: function(argument) {}
                    };
                var text = "Are you sure you want to delete the business metadata";
                notifyObj["text"] = text;
                Utils.notifyConfirm(notifyObj);
            },
            onNotifyDeleteOk: function(data) {
                var that = this,
                    deleteBusinessMetadataData = that.businessMetadataDefCollection.fullCollection.findWhere({ guid: that.guid });
                that.$('.tableOverlay').show();
                if (deleteBusinessMetadataData) {
                    var businessMetadataName = deleteBusinessMetadataData.get("name");
                    deleteBusinessMetadataData.deleteBusinessMetadata({
                        typeName: businessMetadataName,
                        success: function() {
                            Utils.notifySuccess({
                                content: "Business Metadata " + businessMetadataName + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                            });
                            that.businessMetadataDefCollection.fullCollection.remove(deleteBusinessMetadataData);
                            that.businessMetadataDefCollection.fullCollection.sort({ silent: true });
                            that.renderTableLayoutView();
                            that.showDetails = true;
                            that.toggleBusinessMetadataDetailsAttrView();
                            that.loaderStatus(false);
                        },
                        complete: function() {
                            that.$('.tableOverlay').hide();
                            that.$('.position-relative .fontLoader').removeClass('show');
                        }
                    });
                } else {
                    Utils.notifyError({
                        content: Messages.defaultErrorMessage
                    });
                }
            }
        });
    return BusinessMetadataTableLayoutView;
});