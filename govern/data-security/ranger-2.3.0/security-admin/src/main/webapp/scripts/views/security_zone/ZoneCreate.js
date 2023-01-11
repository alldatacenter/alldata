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


/*
 * Repository/Service create view
 */
define(function(require) {
    'use strict';

    var Backbone = require('backbone');
    var App = require('App');
    var XAUtil = require('utils/XAUtils');
    var XAEnums = require('utils/XAEnums');
    var localization = require('utils/XALangSupport');
    var XALinks = require('modules/XALinks');
    var ZoneCreateTmpl = require('hbs!tmpl/security_zone/ZoneCreate_tmpl');
    var XABackgrid = require('views/common/XABackgrid');
    var XATableLayout = require('views/common/XATableLayout');
    var vzoneResource = require('views/security_zone/zoneResource');
    var RangerPolicyResource = require('models/RangerPolicyResource');
    var App = require('App');
    var ZoneCreateForm = require('views/security_zone/ZoneCreateForm');
    var ZoneCreate = Backbone.Marionette.Layout.extend(
        /** @lends ZoneCreate */
        {
            _viewName: 'ZoneCreate',

            template: ZoneCreateTmpl,

            templateHelpers: function() {
                var that = this;
                return {
                    editService: that.editZone,
                }
            },

            breadCrumbs: function() {
                if (this.model.isNew()) {
                    return [XALinks.get('SecurityZone'), XALinks.get('ZoneCreate', {
                        model: this.rangerService
                    })];
                } else {
                    return [XALinks.get('SecurityZone'), XALinks.get('ZoneEdit', {
                        model: this.rangerService
                    })];
                }
            },

            regions: {
                // 'rTableList': 'div[data-id="zoneTableLayout"]'
                'rZoneForm' :'div[data-id="r_zoneForm"]'
            },

            /** ui selector cache */
            ui: {
                'btnSave': '[data-id="save"]',
                'btnCancel': '[data-id="cancel"]',
                'btnDelete': '[data-id="delete"]',
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.btnSave] = 'onSave';
                events['click ' + this.ui.btnCancel] = 'onCancel';
                events['click ' + this.ui.btnDelete] = 'onDelete';
                return events;
            },

            /**
             * intialize a new ZoneCreate Layout
             * @constructs
             */
            initialize: function(options) {
                console.log("initialized a ZoneCreate Layout");
                var that = this;
                this.editZone = this.model.has('id') ? true : false;
                _.extend(this, _.pick(options, 'model', 'rangerService', 'zoneSerivesColl'));
                // this.resourceColl = new Backbone.Collection();
                that.form = new ZoneCreateForm({
                    template : require('hbs!tmpl/security_zone/ZoneCreateForm_tmpl'),
                    model : this.model,
                    zoneSerivesColl : this.zoneSerivesColl,
                    rangerService : this.rangerService,
                });
            },

            /** all events binding here */
            bindEvents: function() {
                /*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
                /*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
            },

            /** on render callback */
            onRender: function() {
                this.rZoneForm.show(this.form);
                this.rZoneForm.$el.dirtyFields();
                // XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.rZoneForm.$el);
                // XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavZoneForm'),this.$el);
            },

            /** all post render plugin initialization */
            initializePlugins: function() {},

            onSave: function(e) {
                e.stopPropagation();
                var that = this , resourceValueEmpty = false;
                if(_.isEmpty(this.$el.find('[data-id="servicesName"]').val()) || _.isUndefined(this.$el.find('[data-id="servicesName"]').val())){
                    this.$el.find('[data-id="selectServicesName"]').addClass('error');
                    this.$el.find('[data-id="serviceNameHelp"]').show();
                }else{
                    this.$el.find('[data-id="selectServicesName"]').removeClass('error');
                    this.$el.find('[data-id="serviceNameHelp"]').hide();
                }
                var errors = this.form.commit({validate : false});
                if(! _.isEmpty(errors)){
                    return;
                }

                if(_.isEmpty(this.$el.find('[data-id="servicesName"]').val()) || _.isUndefined(this.$el.find('[data-id="servicesName"]').val())){
                    this.$el.find('[data-id="selectServicesName"]').addClass('error');
                    return
                }
                var services = {};
                var allServiceResource = this.zoneSerivesColl.each(function(model, i) {
                    var serviceResourceData = model.get('resourceColl');
                    if(_.isEmpty(serviceResourceData.models)){
                        XAUtil.alertPopup({ msg : 'Please add at least one resource for '+ model.get('name') +' service'});
                        resourceValueEmpty = true;
                        return
                    }
                    serviceResourceData.each(function(data, i) {
                        var tmpRsr = [], serviceResource = data.get('resources');
                        if (_.isUndefined(services[model.get('name')])) {
                            services[model.get('name')] = {
                                'resources': [serviceResource]
                            };
                        } else {
                            services[model.get('name')]['resources'].push(serviceResource);
                        }
                    });
                });
                if(resourceValueEmpty){
                    return
                }
                this.model.set('services', services);
                this.model.set('adminUsers', this.model.get('adminUsers').split(','));
                this.model.set('adminUserGroups', this.model.get('adminUserGroups').split(','));
                this.model.set('auditUsers', this.model.get('auditUsers').split(','));
                this.model.set('auditUserGroups', this.model.get('auditUserGroups').split(','));
                this.model.set('tagServices', this.model.get('tagServices').split(','));
                this.model.unset('policyType');
                XAUtil.blockUI();
                this.model.save({}, {
                    wait: true,
                    success: function(model) {
                        XAUtil.blockUI('unblock');
                        XAUtil.allowNavigation();
                        if(that.editZone && App.vZone && !_.isNull(App.vZone.vZoneId) && App.vZone.vZoneId == model.id &&
                            App.vZone.vZoneName !== model.get('name')){
                            App.vZone.vZoneName = model.get('name');
                        }
                        var msg = that.editZone ? 'Service zone updated successfully' : 'Service zone created successfully';
                        XAUtil.notifySuccess('Success', msg);
                        if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
                            App.rSideBar.currentView.render();
                        }
                        App.appRouter.navigate("#!/zones/zone/"+model.id, {
                            trigger: true
                        });
                    },
                    error: function(model, response, options) {
                        XAUtil.blockUI('unblock');
                        var msg = that.editZone ? 'Error updating Ranger zone.' : 'Error creating Ranger zone.';
                        if (response && response.responseJSON && response.responseJSON.msgDesc) {
                            XAUtil.showErrorMsg(response.responseJSON.msgDesc);
                        } else {
                            XAUtil.notifyError('Error', msg);
                        }
                    }
                });
            },
            onCancel: function() {
                XAUtil.allowNavigation();
                App.appRouter.navigate("#!/zones/zone/list", {trigger: true});
            },
    });

    return ZoneCreate;
});