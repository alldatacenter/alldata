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
    'hbs!tmpl/audit/AdminAuditTableLayoutView_tmpl',
    'collection/VEntityList',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/CommonViewFunction',
    'utils/Enums',
    'moment'
], function(require, Backbone, AdminAuditTableLayoutView_tmpl, VEntityList, Utils, UrlLinks, CommonViewFunction, Enums, moment) {
    'use strict';

    var AdminAuditTableLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends AuditTableLayoutView */
        {
            _viewName: 'AdminAuditTableLayoutView',

            template: AdminAuditTableLayoutView_tmpl,

            /** Layout sub regions */
            regions: {
                RAuditTableLayoutView: "#r_adminAuditTableLayoutView",
                RQueryBuilderAdmin: "#r_attributeQueryBuilderAdmin"
            },

            /** ui selector cache */
            ui: {
                adminPurgedEntityClick: "[data-id='adminPurgedEntity']",
                adminAuditEntityDetails: "[data-id='adminAuditEntityDetails']",
                attrFilter: "[data-id='adminAttrFilter']",
                adminRegion: "[data-id='adminRegion']",
                attrApply: "[data-id='attrApply']",
                showDefault: "[data-id='showDefault']",
                attrClose: "[data-id='attrClose']"

            },
            /** ui events hash */
            events: function() {
                var events = {},
                    that = this;
                events["click " + this.ui.adminPurgedEntityClick] = "onClickAdminPurgedEntity";
                events["click " + this.ui.adminAuditEntityDetails] = "showAdminAuditEntity";
                events["click " + this.ui.attrFilter] = function(e) {
                    this.ui.attrFilter.find('.fa-angle-right').toggleClass('fa-angle-down');
                    this.$('.attributeResultContainer').addClass("overlay");
                    this.$('.attribute-filter-container, .attr-filter-overlay').toggleClass('hide');
                    this.onClickAttrFilter();
                };
                events["click " + this.ui.attrClose] = function(e) {
                    that.closeAttributeModel();
                };
                events["click " + this.ui.attrApply] = function(e) {
                    that.okAttrFilterButton(e);
                };
                return events;
            },
            /**
             * intialize a new AdminTableLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'searchTableFilters', 'entityDefCollection', 'enumDefCollection'));
                this.entityCollection = new VEntityList();
                this.limit = 25;
                this.offset = 0;
                this.entityCollection.url = UrlLinks.adminApiUrl();
                this.entityCollection.modelAttrName = "events";
                this.commonTableOptions = {
                    collection: this.entityCollection,
                    includePagination: false,
                    includeAtlasPagination: true,
                    includeFooterRecords: false,
                    includeColumnManager: true,
                    includeOrderAbleColumns: false,
                    includeSizeAbleColumns: false,
                    includeTableLoader: true,
                    includeAtlasPageSize: true,
                    includeAtlasTableSorting: true,
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
                    atlasPaginationOpts: {
                        limit: this.limit,
                        offset: this.offset,
                        fetchCollection: this.getAdminCollection.bind(this),
                    },
                    gridOpts: {
                        emptyText: 'No Record found!',
                        className: 'table table-hover backgrid table-quickMenu colSort'
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
                this.isFilters = null;
                this.adminAuditEntityData = {};
            },
            onRender: function() {
                this.ui.adminRegion.hide();
                this.getAdminCollection();
                this.entityCollection.comparator = function(model) {
                    return -model.get('timestamp');
                }
                this.renderTableLayoutView();
            },
            onShow: function() {
                this.$('.fontLoader').show();
                this.$('.tableOverlay').show();
            },
            bindEvents: function() {},
            closeAttributeModel: function() {
                var that = this;
                that.$('.attributeResultContainer').removeClass("overlay");
                that.ui.attrFilter.find('.fa-angle-right').toggleClass('fa-angle-down');
                that.$('.attribute-filter-container, .attr-filter-overlay').toggleClass('hide');
            },
            onClickAttrFilter: function() {
                var that = this;
                this.ui.adminRegion.show();
                require(['views/search/QueryBuilderView'], function(QueryBuilderView) {
                    that.RQueryBuilderAdmin.show(new QueryBuilderView({ adminAttrFilters: true, searchTableFilters: that.searchTableFilters, entityDefCollection: that.entityDefCollection, enumDefCollection: that.enumDefCollection }));
                });
            },
            okAttrFilterButton: function(options) {
                var that = this,
                    isFilterValidate = true,
                    queryBuilderRef = that.RQueryBuilderAdmin.currentView.ui.builder;
                if (queryBuilderRef.data("queryBuilder")) {
                    var queryBuilder = queryBuilderRef.queryBuilder("getRules");
                    if (queryBuilder) {
                        that.ruleUrl = that.searchTableFilters["adminAttrFilters"] = CommonViewFunction.attributeFilter.generateUrl({ value: queryBuilder, formatedDateToLong: true });
                        that.isFilters = queryBuilder.rules.length ? queryBuilder.rules : null;
                    } else {
                        isFilterValidate = false
                    }
                }
                if (isFilterValidate) {
                    that.closeAttributeModel();
                    that.defaultPagination();
                    that.getAdminCollection();
                }
            },
            getAdminCollection: function(option) {
                var that = this,
                    auditFilters = CommonViewFunction.attributeFilter.generateAPIObj(that.ruleUrl);
                $.extend(that.entityCollection.queryParams, { auditFilters: that.isFilters ? auditFilters : null, limit: that.entityCollection.queryParams.limit || that.limit, offset: that.entityCollection.queryParams.offset || that.offset, sortBy: "startTime", sortOrder: "DESCENDING" });
                var apiObj = {
                    sort: false,
                    data: _.pick(that.entityCollection.queryParams, 'auditFilters', 'limit', 'offset', 'sortBy', 'sortOrder'),
                    success: function(dataOrCollection, response) {
                        that.entityCollection.state.pageSize = that.entityCollection.queryParams.limit || 25;
                        that.entityCollection.fullCollection.reset(dataOrCollection, option);
                    },
                    complete: function() {
                        that.$('.fontLoader').hide();
                        that.$('.tableOverlay').hide();
                        that.$('.auditTable').show();
                    },
                    reset: true
                }
                this.entityCollection.getAdminData(apiObj);
            },
            renderTableLayoutView: function() {
                var that = this;
                this.ui.showDefault.hide();
                require(['utils/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getAuditTableColumns());
                    that.RAuditTableLayoutView.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols
                    })));
                });
            },
            createTableWithValues: function(tableDetails, isAdminAudit) {
                var attrTable = CommonViewFunction.propertyTable({
                    scope: this,
                    getValue: function(val, key) {
                        if (key && key.toLowerCase().indexOf("time") > 0) {
                            return Utils.formatDate({ date: val });
                        } else {
                            return val;
                        }
                    },
                    valueObject: tableDetails,
                    guidHyperLink: !isAdminAudit
                });
                return attrTable;
            },
            getAuditTableColumns: function() {
                var that = this;
                return this.entityCollection.constructor.getTableCols({
                    result: {
                        label: "",
                        cell: "html",
                        editable: false,
                        sortable: false,
                        cell: Backgrid.ExpandableCell,
                        fixWidth: "20",
                        accordion: false,
                        alwaysVisible: true,
                        renderable: true,
                        isExpandVisible: function(el, model) {
                            if (Enums.serverAudits[model.get('operation')]) {
                                return false;
                            } else {
                                return true;
                            }
                        },
                        expand: function(el, model) {
                            var operation = model.get('operation'),
                                results = model.get('result') || null,
                                adminText = 'No records found',
                                adminTypDetails = null,
                                auditData = {
                                    operation: operation,
                                    model: model,
                                    results: results,
                                    adminText: adminText,
                                    adminTypDetails: adminTypDetails
                                };
                            el.attr('colspan', '8');
                            if (results) {
                                var adminValues = null;
                                if (operation == "PURGE") {
                                    adminText = that.displayPurgeAndImportAudits(auditData);
                                } else if (operation == "EXPORT" || operation == "IMPORT") {
                                    adminText = that.displayExportAudits(auditData);
                                } else {
                                    adminText = that.displayCreateUpdateAudits(auditData);
                                }
                            }
                            $(el).append($('<div>').html(adminText));
                        }
                    },
                    userName: {
                        label: "Users",
                        cell: "html",
                        renderable: true,
                        editable: false
                    },
                    operation: {
                        label: "Operation",
                        cell: "String",
                        renderable: true,
                        editable: false
                    },
                    clientId: {
                        label: "Client ID",
                        cell: "String",
                        renderable: true,
                        editable: false
                    },
                    resultCount: {
                        label: "Result Count",
                        cell: "String",
                        renderable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                if (Enums.serverAudits[model.get('operation')]) {
                                    return "N/A"
                                } else {
                                    return rawValue;
                                }
                            }
                        })
                    },
                    startTime: {
                        label: "Start Time",
                        cell: "html",
                        renderable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: rawValue });
                            }
                        })
                    },
                    endTime: {
                        label: "End Time",
                        cell: "html",
                        renderable: true,
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return Utils.formatDate({ date: rawValue });
                            }
                        })
                    },
                    duration: {
                        label: "Duration",
                        cell: "html",
                        renderable: false,
                        editable: false,
                        sortable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var startTime = model.get('startTime') ? parseInt(model.get('startTime')) : null,
                                    endTime = model.get('endTime') ? parseInt(model.get('endTime')) : null;
                                if (_.isNumber(startTime) && _.isNumber(endTime)) {
                                    var duration = moment.duration(moment(endTime).diff(moment(startTime)));
                                    return Utils.millisecondsToTime(duration);
                                } else {
                                    return "N/A";
                                }
                            }
                        })
                    }
                }, this.entityCollection);
            },
            defaultPagination: function() {
                $.extend(this.entityCollection.queryParams, { limit: this.limit, offset: this.offset });
                this.renderTableLayoutView();
            },
            showAdminAuditEntity: function(e) {
                var typeDefObj = this.adminAuditEntityData[e.target.dataset.auditentityid],
                    typeDetails = this.createTableWithValues(typeDefObj, true),
                    view = '<table class="table admin-audit-details bold-key" ><tbody >' + typeDetails + '</tbody></table>',
                    modalData = {
                        title: Enums.category[typeDefObj.category] + " Type Details: " + typeDefObj.name,
                        htmlContent: view,
                        mainClass: "modal-full-screen",
                        okCloses: true,
                        showFooter: false,
                        width: "40%"
                    };
                this.showModal(modalData);
            },
            displayPurgeAndImportAudits: function(obj) {
                var adminValues = '<ul class="col-sm-6">',
                    guids = null,
                    adminTypDetails = Enums.category[obj.operation];
                if (obj.operation == "PURGE") {
                    guids = obj.results ? obj.results.replace('[', '').replace(']', '').split(',') : guids;
                } else {
                    guids = obj.model.get('params') ? obj.model.get('params').split(',') : guids;
                }
                _.each(guids, function(adminGuid, index) {
                    if (index % 5 == 0 && index != 0) {
                        adminValues += '</ul><ul class="col-sm-6">';
                    }
                    adminValues += '<li class="blue-link" data-id="adminPurgedEntity" data-operation=' + obj.operation + '>' + adminGuid.trim() + '</li>';
                })
                adminValues += '</ul>';
                return '<div class="row"><div class="attr-details"><h4 style="word-break: break-word;">' + adminTypDetails + '</h4>' + adminValues + '</div></div>';
            },
            displayExportAudits: function(obj) {
                var adminValues = "",
                    adminTypDetails = (obj.operation === 'IMPORT') ? Enums.category[obj.operation] : Enums.category[obj.operation] + " And Options",
                    resultData = obj.results ? JSON.parse(obj.results) : null,
                    paramsData = (obj.model && obj.model.get('params') && obj.model.get('params').length) ? { params: [obj.model.get('params')] } : null;

                if (resultData) {
                    adminValues += this.showImportExportTable(resultData, obj.operation);
                }
                if (paramsData) {
                    adminValues += this.showImportExportTable(_.extend(paramsData, { "paramsCount": obj.model.get('paramsCount') }));
                }
                adminValues = adminValues ? adminValues : obj.adminText;
                return '<div class="row"><div class="attr-details"><h4 style="word-break: break-word;">' + adminTypDetails + '</h4>' + adminValues + '</div></div>';
            },
            showImportExportTable: function(obj, operations) {
                var that = this,
                    typeDetails = "",
                    view = '<ul class="col-sm-5 import-export"><table class="table admin-audit-details bold-key" ><tbody >';
                if (operations && operations === "IMPORT") {
                    var importKeys = Object.keys(obj);
                    _.each(importKeys, function(key, index) {
                        var newObj = {};
                        newObj[key] = obj[key];
                        if (index % 5 === 0 && index != 0) {
                            view += '</tbody></table></ul><ul class="col-sm-5 import-export"><table class="table admin-audit-details bold-key" ><tbody >';
                        }
                        view += that.createTableWithValues(newObj, true);
                    })
                } else {
                    view += this.createTableWithValues(obj, true);
                }
                return view += '</tbody></table></ul>';;
            },
            displayCreateUpdateAudits: function(obj) {
                var that = this,
                    resultData = JSON.parse(obj.results),
                    typeName = obj.model ? obj.model.get('params').split(',') : null,
                    typeContainer = '';
                _.each(typeName, function(name) {
                    var typeData = resultData[name],
                        adminValues = (typeName.length == 1) ? '<ul class="col-sm-4">' : '<ul>',
                        adminTypDetails = Enums.category[name] + " " + Enums.auditAction[obj.operation];
                    typeContainer += '<div class="attr-type-container"><h4 style="word-break: break-word;">' + adminTypDetails + '</h4>';
                    _.each(typeData, function(typeDefObj, index) {
                        if (index % 5 == 0 && index != 0 && typeName.length == 1) {
                            adminValues += '</ul><ul class="col-sm-4">';
                        }
                        var panelId = typeDefObj.name.split(" ").join("") + obj.model.get('startTime');
                        that.adminAuditEntityData[panelId] = typeDefObj;
                        adminValues += '<li class="blue-link" data-id="adminAuditEntityDetails" data-auditEntityId=' + panelId + '>' + typeDefObj.name + '</li>';
                    });
                    adminValues += '</ul>';
                    typeContainer += adminValues + '</div>';
                })
                var typeClass = (typeName.length == 1) ? null : "admin-audit-details";
                return '<div class="row"><div class="attr-details ' + typeClass + '">' + typeContainer + '</div></div>';
            },
            onClickAdminPurgedEntity: function(e) {
                var that = this;
                require(['views/audit/AuditTableLayoutView'], function(AuditTableLayoutView) {
                    var obj = {
                            guid: $(e.target).text(),
                            titleText: (e.target.dataset.operation == "PURGE") ? "Purged Entity Details: " : "Import Details: "
                        },
                        modalData = {
                            title: obj.titleText + obj.guid,
                            content: new AuditTableLayoutView(obj),
                            mainClass: "modal-full-screen",
                            okCloses: true,
                            showFooter: false,
                        };
                    that.showModal(modalData);
                });
            },
            showModal: function(modalObj, title) {
                var that = this;
                require([
                    'modules/Modal'
                ], function(Modal) {
                    var modal = new Modal(modalObj).open();
                    modal.on('closeModal', function() {
                        $('.modal').css({ 'padding-right': '0px !important' });
                        modal.trigger('cancel');
                    });
                    modal.$el.on('click', 'td a', function() {
                        modal.trigger('cancel');
                    });
                });
            }
        });
    return AdminAuditTableLayoutView;
});