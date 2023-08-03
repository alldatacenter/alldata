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
    var ZoneAdministrationTmpl = require('hbs!tmpl/security_zone/ZoneAdministration_tmpl');
    require('Backbone.BootstrapModal');


   var ZoneAdministration = Backbone.Marionette.Layout.extend(
        /** @lends ZoneCreate */
        {
            _viewName: 'ZoneAdministration',

            template: ZoneAdministrationTmpl,

            templateHelpers: function() {
                var tagServices = this.zoneModel && this.zoneModel.get('tagServices') ? this.zoneModel.get('tagServices') : '';
                return {
                    zoneModel: this.zoneModel,
                    tagServices : tagServices,
                    description : (this.zoneModel && this.zoneModel.get('description') && !_.isEmpty(this.zoneModel.get('description')))
                        ? this.zoneModel.get('description') : false,
                }
            },

            regions: {
                'rTableList': 'div[data-id="zoneTableLayout"]'
            },

            /** ui selector cache */
            ui: {
                'btnServiceName': '[data-id="servicesName"]',
                'panelClick' : '[data-id="panel"]'

            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.btnSave] = 'onSave';
                events['click ' + this.ui.panelClick] = 'onPanelToggle';
                return events;
            },

            /**
             * intialize a new ZoneAdministration Layout
             * @constructs
             */
            initialize: function(options) {
                console.log("initialized a ZoneAdministration Layout");
                var that = this;
                _.extend(this, _.pick(options, 'zoneModel'));
               
            },

            /** on render callback */
            onRender: function() {
               
            },

            onPanelToggle : function(e){
                $(e.currentTarget).children().toggleClass('fa-chevron-down');
                $(e.currentTarget).next().slideToggle();
            }
        });

    return ZoneAdministration;
});