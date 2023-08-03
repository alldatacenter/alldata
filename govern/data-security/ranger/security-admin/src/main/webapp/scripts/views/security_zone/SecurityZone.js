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

    var XAUtil = require('utils/XAUtils');
    var Backbone = require('backbone');
    var localization = require('utils/XALangSupport');
    var XALinks = require('modules/XALinks');
    var RangerService = require('models/RangerService');
    var SecurityZoneTmpl = require('hbs!tmpl/security_zone/SecurityZone_tmpl');
    var XABackgrid = require('views/common/XABackgrid');
    var XATableLayout = require('views/common/XATableLayout');
    var RangerPolicyResourceList = require('collections/RangerPolicyResourceList');
    var RangerPolicyResource = require('models/RangerPolicyResource');
    var App = require('App');
    var RangerZone = require('models/RangerZone');
    var vzoneAdministrationDetail = require('views/security_zone/ZoneAdministration');
    var SessionMgr = require('mgrs/SessionMgr');

    var SecurityZone = Backbone.Marionette.Layout.extend({
        template: SecurityZoneTmpl,

        templateHelpers: function() {
            var that = this;
            var zoneList = this.collection.pluck('name');
            this.zoneModel = this.getZoneModel();
            var zoneModelName = (this.zoneModel && this.zoneModel.get('name')) ? _.escape(this.zoneModel.get('name')) : '';
            var isZoneAdministration = (SessionMgr.isSystemAdmin()) ? true : false;
            return {
                zoneList: zoneList,
                zoneModel: this.zoneModel,
                zoneModelName: zoneModelName,
                isZoneAdministration:isZoneAdministration,
                setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
            };
        },

        breadCrumbs: function() {
            if (this.type == "tag") {
                return [XALinks.get('TagBasedServiceManager')];
            }
            return [XALinks.get('SecurityZone')];
        },

        initialize: function(options) {
            console.log("initialized a Security Zone");
            _.extend(this, _.pick(options, 'rangerService', 'zoneId'));
            var that = this;
            this.zoneResourcesColl = new RangerPolicyResourceList();
        },

        regions: {
            'rTableList': 'div[data-id="zoneTableLayout"]',
            'rzoneAdministrationView': 'div[data-id="zoneAdministrationView"]'
        },

        /** ui selector cache */
        ui: {
            'clickBtn': '.securityZoneBtn',
            'zoneTable': '[data-id="zone-data"]',
            'sideBar': '[data-id="sideBarBtn"]',
            'zoneName': '[data-id="zoneName"]',
            'deleteZone': '[data-id="deleteZone"]',
            'editZone': '[data-id="editZone"]',
            'zoneSearch' : '[data-id="zoneSearch"]',
            'zoneUlList' : '[data-id="zoneUlList"]',
            'toggleForZoneServiceTbl' : '[data-id="zoneServiceTbl"]'
        },

        events: function() {
            var events = {
                'click [data-action="zoneListing"]': 'zoneListing',
            };
            events['click ' + this.ui.clickBtn] = 'onClick';
            events['click ' + this.ui.sideBar] = 'onSidebar';
            events['click ' + this.ui.zoneTable] = 'renderTable';
            events['click ' + this.ui.deleteZone] = 'onDelete';
            events['keyup ' + this.ui.zoneSearch] = 'zoneSearch';
            events['click ' + this.ui.toggleForZoneServiceTbl] = 'toggleForZoneServiceTbl';
            return events;
        },

        /** on render callback */
        onRender: function() {
            if (this.collection.models.length > 0) {
                this.getZoneModel();
                this.setupCollectionForZoneResource(this.zoneModel);
                this.setupZoneList(this.collection.pluck('name'));
            }
        },

        getZoneModel: function() {
           this.zoneModel = this.collection.get(this.zoneId);
            if(_.isUndefined(this.zoneModel)){
                this.zoneModel = _.first(this.collection.models);
            }
            return this.zoneModel
        },

        setupCollectionForZoneResource: function(zoneModel, render) {
            var that = this, resources = [];
            if(zoneModel){
                _.each(zoneModel.get('services'), function(value, key) {
                    var serviceType = that.rangerService.models.find(function(m) {
                        if (m.get('name') == key)
                            return m.get('type')
                    })
                    var zoneTR = {
                        serviceName: key,
                        serviceTypes: serviceType.get('type').toUpperCase(),
                        resource: value.resources
                    };
                    resources.push(new RangerPolicyResource(zoneTR));
                });
                that.zoneResourcesColl.reset(resources);
                that.renderTable();
                that.zoneAdministrationView();
                that.ui.zoneName.html(_.escape(that.zoneModel.get('name'))).attr('title', _.escape(that.zoneModel.get('name')));
                App.appRouter.navigate("#!/zones/zone/"+zoneModel.id);
                that.ui.editZone.attr('href', '#!/zones/edit/' + zoneModel.get('id'));
            }else{
                App.appRouter.navigate("#!/zones/zone/list");
            }
        },

        setupZoneList: function(zoneArray) {
            var that = this;
            this.ui.zoneUlList.empty();
            if(!_.isEmpty(zoneArray)) {
                _.each(zoneArray,
                    function(zone) {
                        if(that.zoneModel.attributes.name == zone) {
                            that.ui.zoneUlList.append('<li class="selected trim-containt" title="'+_.escape(zone)+
                                '" data-action="zoneListing" data-id="' + _.escape(zone) + '">' + _.escape(zone) + '</li>');
                        } else {
                            that.ui.zoneUlList.append('<li class="trim-containt" data-action="zoneListing" title="'
                                +_.escape(zone)+'" data-id="' + _.escape(zone) + '">' + _.escape(zone) + '</li>');
                        }
                    }
                );
            } else {
                this.ui.zoneUlList.append('<li><h4>No Zone Found !</li>');
            }
        },

        zoneSearch: function() {
            var input = this.ui.zoneSearch.val();
            var that = this;
            that.zoneSearchList = [];

            if (!_.isEmpty(input)) {
                _.each(this.collection.pluck('name'),
                    function(zone) {
                        if (zone.toLowerCase().indexOf(input.toLowerCase()) > -1) {
                            that.zoneSearchList.push(_.escape(zone));
                        }
                    }
                );
                this.setupZoneList(that.zoneSearchList);
            } else {
                this.setupZoneList(this.collection.pluck('name'));
            }
        },

        onSidebar: function(e) {
            var that = this;
            e.stopPropagation();
            if (that.collapes) {
                this.$el.find('.security-sidebar').show();
                this.$el.find('.security-details').animate({width:"70%"}, 500 );
                this.$el.find('.security-sidebar').animate({width:"30%"}, 500 );
                this.$el.find('.security-details').removeClass('sidebar-margin');
                this.collapes = false;
            } else {
                this.collapes = true;
                this.$el.find('.security-sidebar').animate({width: '0%'}, 500 );
                this.$el.find('.security-details').animate({width:"100%"}, 500 );
                this.$el.find('.security-details').addClass('sidebar-margin');
                this.$el.find('.security-sidebar').hide();
            }

        },

        zoneListing: function(e) {
            var that = this,
                $el = $(e.currentTarget);
            if (!$el.hasClass('selected')) {
                $el.parent().find('li').removeClass('selected');
                $el.addClass('selected');
            } else {
                return;
            }
            this.zoneModel = that.collection.find(function(m) {
                return m.get("name") === e.currentTarget.textContent
            });
            this.setupCollectionForZoneResource(this.zoneModel);
        },

        renderTable: function() {
            var that = this;
            this.rTableList.show(new XATableLayout({
                columns: this.getColumns(),
                collection: that.zoneResourcesColl,
                includeFilter: false,
                includePagination: false,
                gridOpts: {
                    row: Backgrid.Row.extend({}),
                    header: XABackgrid,
                    emptyText: 'No Zone Data Found!!'
                }
            }));
        },

        getColumns: function() {
            var that = this;
            var col = {
                serviceName: {
                    label: 'Service Name',
                    cell: 'string',
                    editable: false,
                    sortable: false
                },

                serviceTypes: {
                    label: 'Service Type',
                    cell: 'string',
                    editable: false,
                    sortable: false
                },

                resource: {
                    label: 'Resource',
                    cell: 'html',
                    editable: false,
                    sortable: false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function(rawValue, model) {
                            var span = '';
                            rawValue.map(function(value, index) {
                                span+='<div class="resourceGrp">'
                                _.map(value, function(val, key) {
                                    span += '<p><strong>' + key + ' :</strong> ' + _.escape(val.join(", ")) + '</p>'
                                });
                                span+= '</div>';
                            });
                            return $(span);
                        }
                    })
                },
            };
            return that.zoneResourcesColl.constructor.getTableCols(col, that.zoneResourcesColl);
        },

        zoneAdministrationDetail: function() {
            var view = new vzoneAdministrationDetail({
                zoneModel: this.zoneModel
            });
            var modal = new Backbone.BootstrapModal({
                animate: true,
                content: view,
                title: 'Zone Administration',
                allowCancel: true,
                escape: true,
                focusOk : false
            }).open();
        },

        zoneAdministrationView: function() {
            this.rzoneAdministrationView.show(new vzoneAdministrationDetail({
                zoneModel: this.zoneModel
            }));
        },

        onDelete: function(e) {
            var that = this;
            var model = new RangerZone(this.zoneModel.attributes);
            model.collection = this.collection;
            XAUtil.confirmPopup({
                msg: 'Are you sure want to delete ?',
                callback: function() {
                    XAUtil.blockUI();
                    model.destroy({
                        success: function(model, response) {
                            XAUtil.blockUI('unblock');
                            that.collection.remove(model.get('id'));
                            XAUtil.notifySuccess('Success', localization.tt('msg.zoneDeleteMsg'));
                            that.zoneModel = _.first(that.collection.models);
                            if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
                                App.rSideBar.currentView.render();
                                if(_.isUndefined(that.zoneModel)) {
                                    App.appRouter.navigate("#!/zones/zone/list",{trigger: true});
                                } else {
                                    App.appRouter.navigate("#!/zones/zone/"+that.zoneModel.id,{trigger: true});
                                }
                            } else {
                                that.setupCollectionForZoneResource(that.zoneModel);
                                that.render();
                            }
                            if(App.vZone && !_.isEmpty(App.vZone) && model.get('name') === App.vZone.vZoneName){
                                App.vZone.vZoneName = "";
                            }
                        },
                        error: function(model, response, options) {
                            XAUtil.blockUI('unblock');
                            if (response && response.responseJSON && response.responseJSON.msgDesc) {
                                XAUtil.notifyError('Error', response.responseJSON.msgDesc);
                            } else
                                XAUtil.notifyError('Error', 'Error deleting Zone!');
                        }
                    });
                }
            });
        },

        toggleForZoneServiceTbl : function(e) {
           $(e.currentTarget).children().toggleClass('fa-chevron-down');
           $(e.currentTarget).next().slideToggle();
        },

        onClick: function() {

        }
    });
    return SecurityZone;

});