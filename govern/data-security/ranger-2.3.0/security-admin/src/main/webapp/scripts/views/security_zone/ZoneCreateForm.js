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
    var XABackgrid = require('views/common/XABackgrid');
    var XATableLayout = require('views/common/XATableLayout');
    var vzoneResource = require('views/security_zone/zoneResource');
    var RangerPolicyResource = require('models/RangerPolicyResource');
    var App = require('App');
    var BackboneFormDataType = require('models/BackboneFormDataType');

    require('backbone-forms.list');
    require('backbone-forms.templates');
    require('backbone-forms');
    require('backbone-forms.XAOverrides');
    var ZoneCreateForm = Backbone.Form.extend(
        /** @lends ZoneCreateForm */
        {
            _viewName: 'ZoneCreateForm',

            // template: ZoneCreateFormTmpl,

            templateHelpers: function() {
                return {
                    editService: this.editZone,
                }
            },

            /** ui selector cache */
            ui: {
                'selectServiceName': '[data-id="servicesName"]',
                'adminUsers': '[data-id="adminUsers"]',
                'adminUserGroups': '[data-id="adminUserGroups"]',
                'auditUsers': '[data-id="auditUsers"]',
                'auditUserGroups': '[data-id="auditUserGroups"]',
                'zoneName'          : '[data-id="zoneName"]'
            },

            // fields: ['name', 'description'],

            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },

            /**
             * intialize a new ZoneCreateForm Layout
             * @constructs
             */
            initialize: function(options) {
                console.log("initialized a ZoneCreateForm Layout");
                var that = this;
                this.editZone = this.model.has('id') ? true : false;
                _.extend(this, _.pick(options, 'model', 'rangerService', 'zoneSerivesColl'));
                Backbone.Form.prototype.initialize.call(this, options);
                this.resourceColl = new Backbone.Collection();

            },
            schema: function() {
                return this.model.schemaBase();
            },

            /** all events binding here */
            bindEvents: function() {
                /*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
                /*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
            },

            /** on render callback */
            render: function(options) {
                Backbone.Form.prototype.render.call(this, options);
                this.fields.name.$el = this.$el.find(this.ui.zoneName);
                this.fields.adminUsers.$el = this.$el.find(this.ui.adminUsers);
                this.fields.adminUserGroups.$el = this.$el.find(this.ui.adminUserGroups);
                this.fields.auditUsers.$el = this.$el.find(this.ui.auditUsers);
                this.fields.auditUserGroups.$el = this.$el.find(this.ui.auditUserGroups);


                if (!this.model.isNew()) {
                    this.editZoneForm();
                }
                this.addServiceName();
                this.zoneTable();
                XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavZoneForm'), this.$el);
            },

            /** all post render plugin initialization */
            initializePlugins: function() {},

            editZoneForm: function() {
                var that = this;
                var $selectServiceName = this.$el.find(this.ui.selectServiceName);
                $selectServiceName.val([" "]);
                var resourceData = _.map(this.model.get('services'), function(value, key, obj, index) {
                    var resourceTmp = [];
                    resourceTmp = _.map(value.resources, function(resource, key) {
                        return {
                            'resources': resource,
                        };
                    });
                    return {
                        name: key,
                        serviceType: that.getService(key).get('type'),
                        serviceId: that.getService(key).get('id'),
                        policyType: 0,
                        resource: resourceTmp
                    };
                })
                this.zoneSerivesColl.reset(resourceData);
            },
            getService: function(serviceName) {
                var service = this.rangerService.findWhere({
                    'name': serviceName
                });
                if (service) return service;
                return null;
            },

            addServiceName: function() {
                var that = this,
                    $selectServiceName = this.$el.find(this.ui.selectServiceName),
                    tags = [];
                if (!_.isUndefined(that.rangerService) && !_.isEmpty(that.rangerService.models)) {
                    var coll = that.rangerService.filter(function(m) {
                        return m.get('type') != XAEnums.ServiceType.SERVICE_TAG.label && m.get('type') != XAEnums.ServiceType.Service_KMS.label
                    });
                    var serviceDef = _.groupBy(coll, function(m) {
                        return m.get('type');
                    });

                    var options = _.map(serviceDef, function(key, value) {
                        return {
                            'text': value.toUpperCase(),
                            'children': _.map(key, function(m) {
                                return {
                                    'id': m.get('name'),
                                    'serviceId': m.get('id'),
                                    'text': m.get('name'),
                                    'seviceType': value.toUpperCase()
                                }
                            })
                        };
                    });
                    if (!this.model.isNew()) {
                        tags = _.map(this.model.attributes.services, function(m, key) {
                            return {
                                'id': key,
                                'text': key
                            }
                        })
                    }
                }
                $selectServiceName.select2({
                    multiple: true,
                    closeOnSelect: true,
                    placeholder: 'Select Service Name',
                    allowClear: true,
                    data: options || [],
                    width :'600px',
                    initSelection: function(element, callback) {
                        callback(tags);
                    },

                }).on('change', function(e) {
                    console.log(e);
                    if (e.added) {
                        var zoneTR = {
                            name: e.added.text,
                            serviceType: e.added.seviceType,
                            serviceId: e.added.serviceId,
                            policyType: 0,

                        };
                        that.zoneSerivesColl.add(new RangerPolicyResource(zoneTR));
                    } else {
                        that.zoneSerivesColl.remove(that.zoneSerivesColl.models.find(function(m) {
                            return m.get('name') === e.removed.text
                        }));
                    }
                })
            },

            zoneTable: function() {
                var that = this;
                this.$el.find('[data-id="zoneTableLayout"]').html(new XATableLayout({
                    columns: this.getColumns(),
                    collection: this.zoneSerivesColl,
                    includeFilter: false,
                    includePagination: false,
                    gridOpts: {
                        row: Backgrid.Row.extend({}),
                        header: XABackgrid,
                        emptyText: 'No Zone Data Found!!'
                    }
                }).render().el);
            },

            getColumns: function() {
                var that = this;
                var col = {
                    name: {
                        label: 'Service Name',
                        cell: 'string',
                        editable: false,
                        sortable: false
                    },

                    serviceType: {
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
                                model.set('resourceColl', new Backbone.Collection(rawValue));
                                return new vzoneResource({
                                    collection: model.get('resourceColl'),
                                    serviceType: model.get('serviceType'),
                                    seviceName: model.get('name'),
                                    serviceId: model.get('serviceId'),
                                    tableView: that.rTableList
                                }).render().el;
                            },
                        })
                    },
                };
                return this.zoneSerivesColl.constructor.getTableCols(col, this.zoneSerivesColl);
            },

        });

    return ZoneCreateForm;
});