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

    var XAUtils = require('utils/XAUtils');
    var Backbone = require('backbone');
    var localization = require('utils/XALangSupport');
    var XALinks = require('modules/XALinks');
    var RangerServiceDef = require('models/RangerServiceDef');
    var ZoneResourceForm = require('views/security_zone/ZoneResourceForm');
    var RangerService = require('models/RangerService');
    var RangerPolicyResource = require('models/RangerPolicyResource');
    var ZoneResourcesTmpl = require('hbs!tmpl/security_zone/ZoneResources_tmpl');
    require('Backbone.BootstrapModal');


    var ZoneResourceItem = Backbone.Marionette.ItemView.extend({
        _msvName: 'ZoneResourceItem',

        template: require('hbs!tmpl/security_zone/ZoneResourceItem_tmpl'),

        templateHelpers: function() {
            var resources = _.map(this.model.get('resources'), function(val, key) {
                return {
                    'key': key,
                    'value': val.join(",  ")
                }
            })
            return {
                'resources': resources
            };
        },

        ui: {
            'name': '[data-js="name"]',
            'value': '[data-js="value"]'
        },

        events: {
            'click [data-action="editResource"]': 'editResource',
            'click [data-action="delete"]': 'evDelete',
            'change': 'render'
        },

        initialize: function(options) {
            _.extend(this, _.pick(options, 'getResource'));
            this.model.on('change', this.render, this);
        },

        onRender: function() {
            XAUtils.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.$el);
        },

        editResource: function() {
            if (!this.model.get('id')) {
                this.model.set('id', 'resource' + this.model.collection.length);
            }
            this.model.set('policyType', 0);
            this.getResource(this.model);
        },

        evDelete: function() {
            var that = this;
            this.collection.remove(this.model);
        },
    });

    var ZoneResourceList = Backbone.Marionette.CompositeView.extend({
        _msvName: 'ZoneResourceList',
        
        template: require('hbs!tmpl/security_zone/ZoneResourceList_tmpl'),
        
        templateHelpers: function() {

        },

        getItemView: function(item) {
            if (!item) {
                return;
            }
            return ZoneResourceItem;
        },

        itemViewContainer: ".js-formInput",

        itemViewOptions: function() {
            return {
                'collection': this.collection,
                'getResource': this.getResource.bind(this)
            };
        },

        events: {
            'click [data-action="addResource"]': 'addNew'
        },

        initialize: function(options) {
            _.extend(this, _.pick(options, 'serviceType', 'serviceName', 'serviceId', 'tableView'));
            this.getServiceDef();
        },

        onRender: function() {
            // XAUtils.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.$el);
        },

        addNew: function() {
            var that = this;
            // XAUtils.allowNavigation();
            this.getResource();
        },

        getServiceDef: function() {
            var that = this;
            that.rangerServiceDefModel = new RangerServiceDef();
            that.rangerServiceDefModel.url = XAUtils.getRangerServiceDef(this.serviceType.toLowerCase());
            that.rangerServiceDefModel.fetch({
                cache: false,
                async: false
            }).done(function(serviceDef) {
                var resourcesVal = _.map(serviceDef.get('resources'), function(m) {
                    return m = _.omit(m, 'excludesSupported', 'recursiveSupported');
                });
                that.rangerServiceDefModel.attributes.resources = resourcesVal;
            });
            this.rangerService = new RangerService({
                id: this.serviceId
            });
            this.rangerService.fetch({
                cache: false,
                async: false
            });
        },

        getResource: function(resourceModel) {
            var model = null;
            if (resourceModel) {
                model = $.extend(true, {}, resourceModel);
                // model = _.clone(resourceModel)
                _.each(model.get('resources'), function(val, key, obj) {
                    obj[key] = {
                        'values': val
                    };
                });
            }
            this.form = new ZoneResourceForm({
                template: require('hbs!tmpl/security_zone/ZoneResourcesForm_tmpl'),
                model: model ? model : new RangerPolicyResource({
                    policyType: 0
                }),
                rangerServiceDefModel: this.rangerServiceDefModel,
                rangerService: this.rangerService,
            });
            this.modal = new Backbone.BootstrapModal({
                animate: true,
                content: this.form,
                title: 'Add/Edit Resources',
                okText: 'Save',
                allowCancel: true,
                escape: true
            }).open();
            this.modal.on("ok", this.onSave.bind(this, resourceModel));
        },

        onSave: function(resourceModel) {
            var that = this,
                tmpResource = {};
            var errors = this.form.commit({
                validate: false
            });
            if (!_.isEmpty(errors)) {
                that.modal.preventClose();
                return;
            }
            _.each(this.form.model.attributes, function(val, key) {
                if (key.indexOf("sameLevel") >= 0 && !_.isNull(val)) {
                    this.form.model.set(val.resourceType, val);
                    this.form.model.unset(key);
                }
            }, this);
            _.each(that.rangerServiceDefModel.get('resources'), function(obj) {
                var tmpObj = that.form.model.get(obj.name);
                if (!_.isUndefined(tmpObj) && _.isObject(tmpObj) && !_.isEmpty(tmpObj.resource)) {
                    if (obj.type == "path") {
                        tmpResource[obj.name] = tmpObj.resource;
                    } else {
                        tmpResource[obj.name] = _.map(tmpObj.resource, function(val){return val.text});
                    }
                    that.form.model.unset(obj.name);
                }
            });

            if (resourceModel) {
                resourceModel.set('resources', tmpResource);
            } else {
                this.collection.add(new Backbone.Model({
                    'resources': tmpResource,
                    id: 'resource' + (this.collection.length + 1)
                }));
            }
        },
    });

    return ZoneResourceList;

});