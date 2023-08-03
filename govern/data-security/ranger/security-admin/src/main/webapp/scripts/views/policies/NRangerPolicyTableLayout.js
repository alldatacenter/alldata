/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


define(function(require) {
    'use strict';

    var App = require('App');
    var Backbone = require('backbone');
    var XAEnums = require('utils/XAEnums');
    var XALinks = require('modules/XALinks');
    var XAGlobals = require('utils/XAGlobals');
    var SessionMgr = require('mgrs/SessionMgr');
    var XAUtil = require('utils/XAUtils');

    var XABackgrid = require('views/common/XABackgrid');
    var XATableLayout = require('views/common/XATableLayout');
    var localization = require('utils/XALangSupport');
    var RangerService = require('models/RangerService');
    var RangerServiceDef = require('models/RangerServiceDef');
    var RangerPolicy = require('models/RangerPolicy');
    var RangerPolicyTableLayoutTmpl = require('hbs!tmpl/policies/RangerPolicyTableLayout_tmpl');
    var RangerPolicyRO = require('views/policies/RangerPolicyRO');
    var RangerServiceViewDetail = require('views/service/RangerServiceViewDetail');
    var RangerServiceList = require('collections/RangerServiceList');

    require('backgrid-filter');
    require('backgrid-paginator');

    var NRangerPolicyTableLayout = Backbone.Marionette.Layout.extend(
        /** @lends NRangerPolicyTableLayout */
        {
            _viewName: 'NRangerPolicyTableLayout',

            template: RangerPolicyTableLayoutTmpl,

            templateHelpers: function() {
                var infoMsg ="", displayClass = "d-none";
                if(this.rangerService && this.rangerService.get('type')){
                    if(this.rangerService.get('type') == XAEnums.ServiceType.Service_HDFS.label || this.rangerService.get('type') == XAEnums.ServiceType.Service_YARN.label) {
                        infoMsg = XAUtil.pluginConfigInfo(this.rangerService.get('type').toUpperCase())
                        displayClass = "show"
                    }
                }
                return {
                    rangerService: this.rangerService,
                    rangerServiceDef: this.rangerServiceDefModel,
                    rangerPolicyType: this.collection.queryParams['policyType'],
                    isRenderAccessTab: XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef')) ? true :
                        XAUtil.isRenderRowFilter(this.rangerServiceDefModel.get('rowFilterDef')) ? true : false,
                    isAddNewPolicyButtonShow: !(XAUtil.isAuditorOrKMSAuditor(SessionMgr)) && this.rangerService.get('isEnabled'),
                    setNewUi : localStorage.getItem('setOldUI') == "true" ? false : true,
                    isNotAuditorOrKMSAuditor : !XAUtil.isAuditorOrKMSAuditor(SessionMgr),
                    isNotUser : ! SessionMgr.isUser(),
                    displayClass : displayClass,
                    infoMsg : infoMsg,
                };
            },

            breadCrumbs: function() {
                if (this.rangerService.get('type') == XAEnums.ServiceType.SERVICE_TAG.label) {
                    if (App.vZone && App.vZone.vZoneName) {
                        return [XALinks.get('TagBasedServiceManager', App.vZone.vZoneName), XALinks.get('ManagePolicies', {
                            model: this.rangerService
                        })];
                    } else {
                        return [XALinks.get('TagBasedServiceManager'), XALinks.get('ManagePolicies', {
                            model: this.rangerService
                        })];
                    }
                } else {
                    if (App.vZone && App.vZone.vZoneName) {
                        return [XALinks.get('ServiceManager', App.vZone.vZoneName),
                            XALinks.get('ManagePolicies', {
                                model: this.rangerService
                            })
                        ];
                    } else {
                        return [XALinks.get('ServiceManager'), XALinks.get('ManagePolicies', {
                            model: this.rangerService
                        })];
                    }
                }
            },

            /** Layout sub regions */
            regions: {
                'rTableList': 'div[data-id="r_table"]',
            },

            // /** ui selector cache */
            ui: {
                'btnDeletePolicy': '[data-name="deletePolicy"]',
                'btnShowMore': '[data-id="showMore"]',
                'btnShowLess': '[data-id="showLess"]',
                'visualSearch': '.visual_search',
                'policyTypeTab': 'div[data-id="policyTypeTab"]',
                'addNewPolicy': '[data-js="addNewPolicy"]',
                'iconSearchInfo': '[data-id="searchInfo"]',
                'btnViewPolicy': '[data-name ="viewPolicy"]',
                'deleteService': '[data-name="deleteService"]',
                'serviceDetails': '[data-name="serviceDetails"]',
                'serviceEdit': '[data-name="serviceEdit"]'
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.btnDeletePolicy] = 'onDelete';
                events['click ' + this.ui.btnShowMore] = 'onShowMore';
                events['click ' + this.ui.btnShowLess] = 'onShowLess';
                events['click ' + this.ui.policyTypeTab + ' ul li a'] = 'onTabChange';
                events['click ' + this.ui.btnViewPolicy] = 'onView';
                events['click ' + this.ui.deleteService] = 'OnDeleteService';
                events['click ' + this.ui.serviceDetails] = 'viewServices';
                events['click ' + this.ui.serviceEdit] = 'serviceEdit';
                return events;
            },

            /**
             * intialize a new RangerPolicyTableLayout Layout 
             * @constructs
             */
            initialize: function(options) {
                console.log("initialized a NRangerPolicyTableLayout Layout");
                _.extend(this, _.pick(options, 'rangerService'));
                this.urlQueryParams = XAUtil.urlQueryParams();
                this.bindEvents();
                this.initializeServiceDef();
                this.initializeServices();
                if (_.isUndefined(App.vZone)) {
                    App.vZone = {};
                }
                if (App.vZone && App.vZone.vZoneName && !_.isEmpty(App.vZone.vZoneName)) {
                    XAUtil.changeParamToUrlFragment({
                        "securityZone": App.vZone.vZoneName
                    }, this.collection.modelName);
                }
                if (!_.isUndefined(this.urlQueryParams)) {
                    var searchFregment = XAUtil.changeUrlToSearchQuery(decodeURIComponent(this.urlQueryParams));
                    if (_.has(searchFregment, 'securityZone')) {
                        App.vZone.vZoneName = searchFregment['securityZone'];
                        App.rSideBar.currentView.render();
                        searchFregment = _.omit(searchFregment, 'securityZone');
                        if (_.isEmpty(searchFregment)) {
                            this.urlQueryParams = '';
                        } else {
                            this.urlQueryParams = $.param(searchFregment);
                        }
                    } else {
                        App.vZone.vZoneName = "";
                    }
                }
            },

            /** all events binding here */
            bindEvents: function() {
                //this.listenTo(this.collection, "sync", this.render, this);
            },
            initializeServiceDef: function() {
                this.rangerServiceDefModel = new RangerServiceDef();
                this.rangerServiceDefModel.url = "service/plugins/definitions/name/" + this.rangerService.get('type');
                this.rangerServiceDefModel.fetch({
                    cache: false,
                    async: false
                });
            },

            initializeServices: function() {
                this.services = new RangerServiceList();
                this.services.setPageSize(200);
                this.services.fetch({
                    cache: false,
                    async: false
                });

            },

            initializePolicies: function(policyType) {
                this.collection.url = XAUtil.getServicePoliciesURL(this.rangerService.id);
                if (!_.isUndefined(policyType)) {
                    this.collection.queryParams['policyType'] = policyType;
                }
                if (!_.isUndefined(App.vZone) && App.vZone.vZoneName) {
                    this.collection.queryParams['zoneName'] = App.vZone.vZoneName;
                }
            },
            /** on render callback */
            onRender: function() {
                this.setTabForPolicyListing();
                this.renderTable();
                this.initializePolicies();
                this.addVisualSearch();
                if (_.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)) {
                    this.collection.fetch({
                        cache: false,
                    })
                }
                XAUtil.searchInfoPopover(this.searchInfoArray, this.ui.iconSearchInfo, 'bottom');

            },
            /** all post render plugin initialization */
            initializePlugins: function() {},
            setTabForPolicyListing: function() {
                var policyType = this.collection.queryParams['policyType']
                if (XAUtil.isMaskingPolicy(policyType)) {
                    this.ui.policyTypeTab.find('ul li').removeClass('active');
                    this.$el.find('li[data-tab="masking"]').addClass('active');
                } else if (XAUtil.isRowFilterPolicy(policyType)) {
                    this.ui.policyTypeTab.find('ul li').removeClass('active');
                    this.$el.find('li[data-tab="rowLevelFilter"]').addClass('active');
                }
                this.showRequiredTabs()
            },
            showRequiredTabs: function() {
                if (XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef'))) {
                    this.$el.find('li[data-tab="masking"]').show();
                }
                if (XAUtil.isEmptyObjectResourceVal(this.rangerServiceDefModel.get('rowFilterDef'))) {
                    this.$el.find('li[data-tab="rowLevelFilter"]').hide();
                }
            },
            renderTable: function() {
                var that = this;
                this.rTableList.show(new XATableLayout({
                    columns: this.getColumns(),
                    collection: this.collection,
                    includeFilter: false,
                    gridOpts: {
                        row: Backgrid.Row.extend({}),
                        header: XABackgrid,
                        emptyText: 'No Policies found!' + (this.rangerService.get('isEnabled') ? '' : ' The service is disabled!')
                    },
                }));
            },

            onView: function(e) {
                var that = this;
                XAUtil.blockUI();
                var policyId = $(e.currentTarget).data('id');
                var rangerPolicy = new RangerPolicy({
                    id: policyId
                });
                rangerPolicy.fetch({
                    cache: false,
                }).done(function() {
                    XAUtil.blockUI('unblock');
                    var policyVersionList = rangerPolicy.fetchVersions();
                    var view = new RangerPolicyRO({
                        model: rangerPolicy,
                        policyVersionList: policyVersionList,
                        rangerService: that.rangerServiceDefModel
                    });
                    var modal = new Backbone.BootstrapModal({
                        animate: true,
                        content: view,
                        title: localization.tt("h.policyDetails"),
                        okText: localization.tt("lbl.ok"),
                        allowCancel: true,
                        escape: true,
                        focusOk : false
                    }).open();
                    var policyVerEl = modal.$el.find('.modal-footer').prepend('<div class="policyVer pull-left"></div>').find('.policyVer');
                    policyVerEl.append('<i id="preVer" class="fa-fw fa fa-chevron-left ' + ((rangerPolicy.get('version') > 1) ? 'active' : '') + '"></i><text>Version ' + rangerPolicy.get('version') + '</text>').find('#preVer').click(function(e) {
                        view.previousVer(e);
                    });
                    var policyVerIndexAt = policyVersionList.indexOf(rangerPolicy.get('version'));
                    policyVerEl.append('<i id="nextVer" class="fa-fw fa fa-chevron-right ' + (!_.isUndefined(policyVersionList[++policyVerIndexAt]) ? 'active' : '') + '"></i>').find('#nextVer').click(function(e) {
                        view.nextVer(e);
                    });
                    policyVerEl.after('<a id="revert" href="#" class="btn btn-primary" style="display:none;">Revert</a>').next('#revert').click(function(e) {
                        view.revert(e, that.collection, modal);
                    });
                    modal.$el.find('.cancel').hide();
                });
            },

            getColumns: function() {
                var that = this;
                var cols = {
                    policyId : {
                        cell: 'html',
                        label: localization.tt("lbl.policyId"),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                if (XAUtil.isAuditorOrKMSAuditor(SessionMgr)) {
                                    if (!_.isEmpty(model.get('validitySchedules')) && XAUtil.isPolicyExpierd(model)) {
                                        return '<div class="expiredIconPosition">\
                                                <i class="fa-fw fa fa-exclamation-circle backgrigModelId" title="Policy expired"></i>\
                                                ' + model.id + '\
                                             </div>';
                                    } else {
                                        return '<div class="expiredIconPosition">\
                                                ' + model.id + '\
                                            </div>';
                                    }
                                } else {
                                    if (!_.isEmpty(model.get('validitySchedules')) && XAUtil.isPolicyExpierd(model)) {
                                        return '<div class="expiredIconPosition">\
                                                <i class="fa-fw fa fa-exclamation-circle backgrigModelId" title="Policy expired"></i>\
                                                <a class="" href="#!/service/' + that.rangerService.id + '/policies/' + model.id + '/edit">' + model.id + '</a>\
                                             </div>';
                                    } else {
                                        return '<div class="expiredIconPosition">\
                                                <a class="" href="#!/service/' + that.rangerService.id + '/policies/' + model.id + '/edit">' + model.id + '</a>\
                                            </div>';
                                    }
                                }
                            }
                        }),
                        editable: false,
                        sortable: true,
                        direction: "ascending",
                    },
                    policyName : {
                        cell : 'string',
                        label : localization.tt("lbl.policyName"),
                        editable: false,
                        sortable:true,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function (rawValue, model) {
                                if(model) {
                                    return model.get('name');
                                } else {
                                    return '--';
                                }
                            }
                        })
                    },
                    policyLabels: {
                        cell: Backgrid.HtmlCell.extend({
                            className: 'cellWidth-1'
                        }),
                        label: localization.tt("lbl.policyLabels"),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                if (!_.isUndefined(rawValue) && rawValue.length != 0) {
                                    return XAUtil.showMoreAndLessButton(rawValue, model)
                                } else {
                                    return '--';
                                }
                            }
                        }),
                        editable: false,
                        sortable: false
                    },
                    isEnabled: {
                        label: localization.tt('lbl.status'),
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue) {
                                return rawValue ? '<label class="badge badge-success">Enabled</label>' : '<label class="badge badge-danger">Disabled</label>';
                            }
                        }),
                        click: false,
                        drag: false,
                        sortable: false
                    },
                    isAuditEnabled: {
                        label: localization.tt('lbl.auditLogging'),
                        cell: "html",
                        editable: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue) {
                                return rawValue ? '<label class="badge badge-success">Enabled</label>' : '<label class="badge badge-danger">Disabled</label>';
                            }
                        }),
                        click: false,
                        drag: false,
                        sortable: false
                    },
                    roles: {
                        reName: 'roleName',
                        cell: Backgrid.HtmlCell.extend({
                            className: 'cellWidth-1'
                        }),
                        label: localization.tt("lbl.roles"),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return XAUtil.showGroupsOrUsersForPolicy(model.get('policyItems'), model, 'roles', that.rangerServiceDefModel);
                            }
                        }),
                        editable: false,
                        sortable: false
                    },
                    policyItems: {
                        reName: 'groupName',
                        cell: Backgrid.HtmlCell.extend({
                            className: 'cellWidth-1'
                        }),
                        label: localization.tt("lbl.group"),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                if (!_.isUndefined(rawValue)) {
                                    return XAUtil.showGroupsOrUsersForPolicy(rawValue, model, 'groups', that.rangerServiceDefModel);
                                }
                                return '--';
                            }
                        }),
                        editable: false,
                        sortable: false
                    },
                    //Hack for backgrid plugin doesn't allow to have same column name 
                    users: {
                        reName: 'userName',
                        cell: Backgrid.HtmlCell.extend({
                            className: 'cellWidth-1'
                        }),
                        label: localization.tt("lbl.users"),
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return XAUtil.showGroupsOrUsersForPolicy(model.get('policyItems'), model, 'users', that.rangerServiceDefModel);
                            }
                        }),
                        editable: false,
                        sortable: false
                    },
                };
                cols['permissions'] = {
                    cell: "html",
                    label: localization.tt("lbl.action"),
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function(rawValue, model) {
                            if (XAUtil.isAuditorOrKMSAuditor(SessionMgr)) {
                                return '<a href="javascript:void(0);" data-name ="viewPolicy" data-id="' + model.id + '" class="btn btn-mini" title="View"><i class="fa-fw fa fa-eye fa-fw fa fa-large"></i></a>';
                            } else {
                                return '<a href="javascript:void(0);" data-name ="viewPolicy" data-id="' + model.id + '" class="btn btn-mini" title="View"><i class="fa-fw fa fa-eye fa-fw fa fa-large"></i></a>\
                                    <a href="#!/service/' + that.rangerService.id + '/policies/' + model.id + '/edit" class="btn btn-mini" title="Edit"><i class="fa-fw fa fa-edit fa-fw fa fa-large"></i></a>\
                                    <a href="javascript:void(0);" data-name ="deletePolicy" data-id="' + model.id + '"  class="btn btn-mini btn-danger" title="Delete"><i class="fa-fw fa fa-trash fa-fw fa fa-large"></i></a>';
                                //You can use rawValue to custom your html, you can change this value using the name parameter.
                            }
                        }
                    }),
                    editable: false,
                    sortable: false

                };
                return this.collection.constructor.getTableCols(cols, this.collection);
            },
            onDelete: function(e) {
                var that = this;

                var obj = this.collection.get($(e.currentTarget).data('id'));
                var model = new RangerPolicy(obj.attributes);
                model.collection = this.collection;
                XAUtil.confirmPopup({
                    //msg :localize.tt('msg.confirmDelete'),
                    msg: 'Are you sure want to delete ?',
                    callback: function() {
                        XAUtil.blockUI();
                        model.destroy({
                            success: function(model, response) {
                                XAUtil.blockUI('unblock');
                                that.collection.remove(model.get('id'));
                                XAUtil.notifySuccess('Success', localization.tt('msg.policyDeleteMsg'));
                                that.renderTable();
                                that.collection.fetch();
                            },
                            error: function(model, response, options) {
                                XAUtil.blockUI('unblock');
                                if (response && response.responseJSON && response.responseJSON.msgDesc) {
                                    XAUtil.notifyError('Error', response.responseJSON.msgDesc);
                                } else
                                    XAUtil.notifyError('Error', 'Error deleting Policy!');
                                console.log("error");
                            }
                        });
                    }
                });
            },
            onShowMore: function(e) {
                var attrName = this.attributName(e);
                var id = $(e.currentTarget).attr(attrName[0]);
                var $td = $(e.currentTarget).parents('td');
                $td.find('[' + attrName + '="' + id + '"]').show();
                $td.find('[data-id="showLess"][' + attrName + '="' + id + '"]').show();
                $td.find('[data-id="showMore"][' + attrName + '="' + id + '"]').hide();
                $td.find('[data-id="showMore"][' + attrName + '="' + id + '"]').parents('div[data-id="groupsDiv"]').addClass('set-height-groups');
            },
            onShowLess: function(e) {
                var attrName = this.attributName(e)
                var $td = $(e.currentTarget).parents('td');
                var id = $(e.currentTarget).attr(attrName[0]);
                $td.find('[' + attrName + '="' + id + '"]').slice(4).hide();
                $td.find('[data-id="showLess"][' + attrName + '="' + id + '"]').hide();
                $td.find('[data-id="showMore"][' + attrName + '="' + id + '"]').show();
                $td.find('[data-id="showMore"][' + attrName + '="' + id + '"]').parents('div[data-id="groupsDiv"]').removeClass('set-height-groups');
            },
            attributName: function(e) {
                var attrName = ['policy-groups-id', 'policy-users-id', 'policy-label-id', 'policy-roles-id'],
                    attributeName = "";
                attributeName = _.filter(attrName, function(name) {
                    if ($(e.currentTarget).attr(name)) {
                        return name;
                    }
                });
                return attributeName;
            },

            addVisualSearch: function() {

                var that = this,
                    resources = this.rangerServiceDefModel.get('resources'),
                    query = '';
                var policyType = this.collection.queryParams['policyType'];
                if (XAUtil.isMaskingPolicy(policyType)) {
                    if (!_.isEmpty(this.rangerServiceDefModel.get('dataMaskDef').resources)) {
                        resources = this.rangerServiceDefModel.get('dataMaskDef')['resources'];
                    } else {
                        resources = this.rangerServiceDefModel.get('resources');
                    }
                } else if (XAUtil.isRowFilterPolicy(policyType)) {
                    resources = this.rangerServiceDefModel.get('rowFilterDef')['resources'];
                }
                var resourceSearchOpt = _.map(resources, function(resource) {
                    return {
                        'name': resource.name,
                        'label': resource.label,
                        'description':resource.description
                    };
                });
                var PolicyStatusValue = _.map(XAEnums.ActiveStatus, function(status) {
                    return {
                        'label': status.label,
                        'value': Boolean(status.value)
                    };
                });

                var searchOpt = ['Policy Name', 'Group Name', 'User Name', 'Status', 'Policy Label', 'Role Name']; //,'Start Date','End Date','Today'];
                searchOpt = _.union(searchOpt, _.map(resourceSearchOpt, function(opt) {
                    return opt.label
                }))
                var serverAttrName = [{
                    text: "Group Name",
                    label: "group",
                    info: localization.tt('h.groupNameMsg'),
                    urlLabel: 'groupName'
                }, {
                    text: "Policy Name",
                    label: "policyNamePartial",
                    info: localization.tt('msg.policyNameMsg'),
                    urlLabel: 'policyName'
                }, {
                    text: "Status",
                    info: localization.tt('msg.statusMsg'),
                    label: "isEnabled",
                    'multiple': true,
                    'optionsArr': PolicyStatusValue,
                    urlLabel: 'status'
                }, {
                    text: "User Name",
                    label: "user",
                    info: localization.tt('h.userMsg'),
                    urlLabel: 'userName'
                }, {
                    text: "Policy Label",
                    label: "policyLabelsPartial",
                    info: localization.tt('h.policyLabelsinfo'),
                    urlLabel: 'policyLabel'
                }, {
                    text : "Role Name",
                    label :"role" ,
                    info :localization.tt('h.roleMsg'),
                    urlLabel : 'roleName'
                }];
                var info = {
                    collection: localization.tt('h.collection'),
                    column: localization.tt('lbl.columnName'),
                    'column-family': localization.tt('msg.columnfamily'),
                    database: localization.tt('h.database'),
                    entity: localization.tt('h.entity'),
                    keyname: localization.tt('lbl.keyName'),
                    path: localization.tt('h.path'),
                    queue: localization.tt('h.queue'),
                    service: localization.tt('h.serviceNameMsg'),
                    table: localization.tt('lbl.tableName'),
                    tag: localization.tt('h.tagsMsg'),
                    topic: localization.tt('h.topic'),
                    topology: localization.tt('lbl.topologyName'),
                    type: localization.tt('h.type'),
                    udf: localization.tt('h.udf'),
                    url: localization.tt('h.url'),
                    'type-category': localization.tt('h.typeCategory'),
                    'entity-type': localization.tt('h.entityType'),
                    'entity-classification': localization.tt('h.entityClassification'),
                    'atlas-service': localization.tt('h.atlasService'),
                    connector: localization.tt('h.connector'),
                    link: localization.tt('h.link'),
                    job: localization.tt('h.job'),
                    project: localization.tt('h.project'),
                    'nifi-resource': localization.tt('h.nifiResource')
                };
                var serverRsrcAttrName = _.map(resourceSearchOpt, function(opt) {
                    return {
                        'text': opt.label,
                        'label': 'resource:' + opt.name,
                        'info': !_.isUndefined(info[opt.name]) ? info[opt.name] : opt.description,
                        'urlLabel': XAUtil.stringToCamelCase(opt.label.toLowerCase()),
                    };
                });
                serverAttrName = _.union(serverAttrName, serverRsrcAttrName)
                this.searchInfoArray = serverAttrName;
                if (!_.isUndefined(this.urlQueryParams)) {
                    var urlQueryParams = XAUtil.changeUrlToSearchQuery(this.urlQueryParams);
                    _.map(urlQueryParams, function(val, key) {
                        if (_.some(serverAttrName, function(m) {
                                return m.urlLabel == key
                            })) {
                            query += '"' + XAUtil.filterKeyForVSQuery(serverAttrName, key) + '":"' + val + '"';
                        }
                    });
                }
                var pluginAttr = {
                    placeholder: localization.tt('h.searchForPolicy'),
                    container: this.ui.visualSearch,
                    query: query,
                    callbacks: {
                        valueMatches: function(facet, searchTerm, callback) {
                            switch (facet) {
                                case 'Status':
                                    callback(that.getActiveStatusNVList());
                                    break;
                                case 'Policy Type':
                                    callback(that.getNameOfPolicyTypeNVList());
                                    break;
                            }

                        }
                    }
                };
                window.vs = XAUtil.addVisualSearch(searchOpt, serverAttrName, this.collection, pluginAttr);
            },

            getActiveStatusNVList: function() {
                var activeStatusList = _.filter(XAEnums.ActiveStatus, function(obj) {
                    if (obj.label != XAEnums.ActiveStatus.STATUS_DELETED.label)
                        return obj;
                });
                return _.map(activeStatusList, function(status) {
                    return {
                        'label': status.label,
                        'value': status.label
                    };
                })
            },
            getNameOfPolicyTypeNVList: function() {
                return _.map(XAEnums.PolicyType, function(type) {
                    return {
                        'label': type.label,
                        'value': type.label
                    };
                });
            },
            onTabChange: function(e) {
                var that = this,
                    tab = $(e.currentTarget).attr('href');
                var href = this.ui.addNewPolicy.attr('href')
                switch (tab) {
                    case "#access":
                        var val = XAEnums.RangerPolicyType.RANGER_ACCESS_POLICY_TYPE.value;
                        App.appRouter.navigate("#!/service/" + this.rangerService.id + "/policies/" + val, {
                            trigger: true
                        });
                        break;
                    case "#masking":
                        var val = XAEnums.RangerPolicyType.RANGER_MASKING_POLICY_TYPE.value;
                        App.appRouter.navigate("#!/service/" + this.rangerService.id + "/policies/" + val, {
                            trigger: true
                        });
                        break;
                    case "#rowLevelFilter":
                        var val = XAEnums.RangerPolicyType.RANGER_ROW_FILTER_POLICY_TYPE.value;
                        App.appRouter.navigate("#!/service/" + this.rangerService.id + "/policies/" + val, {
                            trigger: true
                        });
                        break;
                }
            },

            OnDeleteService: function() {
                var that = this;
                var model = this.rangerService;
                if (model) {
                    XAUtil.confirmPopup({
                        msg: 'Are you sure want to delete ?',
                        callback: function() {
                            XAUtil.blockUI();
                            model.destroy({
                                success: function(model, response) {
                                    XAUtil.blockUI('unblock');
                                    XAUtil.notifySuccess('Success', 'Service deleted successfully');
                                    App.rSideBar.currentView.render();
                                    that.gotoResourceOrTagTab()
                                },
                                error: function(model, response) {
                                    XAUtil.blockUI('unblock');
                                    if (!_.isUndefined(response) && !_.isUndefined(response.responseJSON) && !_.isUndefined(response.responseJSON.msgDesc && response.status != '419')) {
                                        XAUtil.notifyError('Error', response.responseJSON.msgDesc);
                                    }
                                }
                            });
                        }
                    });
                }
            },

            gotoResourceOrTagTab: function() {
                if(XAEnums.ServiceType.SERVICE_TAG.label == this.rangerServiceDefModel.get('name')){
                    App.appRouter.navigate("#!/policymanager/tag",{trigger: true});
                    return;
                }
                App.appRouter.navigate("#!/policymanager/resource", {
                    trigger: true
                });
            },

            viewServices: function(e) {
                var that = this;
                var serviceId = this.rangerService.id;
                var rangerService = this.rangerService;
                var serviceDef = this.rangerServiceDefModel;
                var view = new RangerServiceViewDetail({
                    serviceDef: serviceDef,
                    rangerService: rangerService,
                    rangerSeviceList: that.services,

                });
                var modal = new Backbone.BootstrapModal({
                    animate: true,
                    content: view,
                    title: localization.tt("h.serviceDetails"),
                    okText: localization.tt("lbl.ok"),
                    allowCancel: true,
                    escape: true,
                    focusOk : false
                }).open();
                modal.$el.find('.cancel').hide();
            },

            serviceEdit: function() {
                var serviceUrl = '#!/service/' + this.rangerServiceDefModel.id + '/edit/' + this.rangerService.id
                App.appRouter.navigate(serviceUrl, {
                    trigger: true
                });
            },

            /** on close */
            onClose: function() {
                XAUtil.removeUnwantedDomElement();
            }

        });

    return NRangerPolicyTableLayout;
});