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

        var Backbone = require('backbone');
        var XAEnums = require('utils/XAEnums');
        var XALinks = require('modules/XALinks');
        var XAGlobals = require('utils/XAGlobals');
        var localization = require('utils/XALangSupport');
        var XAUtils = require('utils/XAUtils');
        var RangerPolicy = require('models/RangerPolicy');
        var RangerPolicyRO = require('views/policies/RangerPolicyRO');

        var AuditAccessLogDetailTmpl = require('hbs!tmpl/reports/AuditAccessLogDetail_tmpl');

        var AuditAccessLogDetailView = Backbone.Marionette.Layout.extend({

            _viewName: 'AuditAccessLogDetailView',

            template: AuditAccessLogDetailTmpl,

            breadCrumbs :function(){
                return 'Audit Access Log Detail'
            },

            /** Layout sub regions */
            regions: {
                'policyDetailsView' :'div[data-id="PolicyDetaissInfo"]'
            },

            templateHelpers: function() {
                var that = this, result;
                result = _.filter(XAEnums.AccessResult, function(e){ return e.value === that.auditaccessDetail.accessResult });
                return {
                    auditaccessDetail : this.auditaccessDetail,
                    eventTime : Globalize.format(new Date(this.auditaccessDetail.eventTime),  "MM/dd/yyyy hh:mm:ss tt"),
                    result : result[0].label,
                    hiveQuery : ((this.auditaccessDetail.serviceType === XAEnums.ServiceType.Service_HIVE.label || this.auditaccessDetail.serviceType === XAEnums.ServiceType.Service_HBASE.label) &&
                                this.auditaccessDetail.aclEnforcer === "ranger-acl" && this.auditaccessDetail.requestData) ? true : false,

                    tag : this.tags ? this.tags.join() : undefined,
                    auditAccessView : this.auditAccessView,
                    policyDetailsView : (this.auditAccessView && this.auditaccessDetail.policyId !== -1) ? true : false,
                }
            },

            ui: {
                copyQuery : '[data-name="copyQuery"]',
                policyDetails : '[data-js="policyDetails"]',
            },

            /** ui events hash */
            events : function() {
                var events = {};
                events['click ' + this.ui.copyQuery] = 'copyQuery';
                events['click ' + this.ui.policyDetails] = 'policyDetails';
                return events
            },
            /**
             * Initialize a new AuditAccessLogDetailsView Layout
             * @constructs
             */
            initialize: function(options) {
                console.log("Initialized a Ranger Audit Access Log Details");
                _.extend(this, _.pick(options, 'auditaccessDetail', 'auditAccessView', 'serviceDefList'));
                if(_.isUndefined(this.auditAccessView)) {
                    this.auditAccessView = false
                }
                if (this.auditaccessDetail.tags) {
                    var tag = JSON.parse(this.auditaccessDetail.tags);
                    this.tags = _.map(tag, function(m) {
                        return m.type
                    });
                }
                if(this.auditAccessView && this.auditaccessDetail.policyId !== -1) {
                    this.policyDetails();
                }
            },

            copyQuery: function(e) {
                XAUtils.copyToClipboard(e , this.auditaccessDetail.requestData);
            },

            policyDetails : function() {
                var that = this
                if(this.auditaccessDetail.repoType){
                    var repoType =  this.auditaccessDetail.repoType;
                }
                var policyId = this.auditaccessDetail.policyId;
                if(policyId == -1){
                        return;
                }
                var eventTime = this.auditaccessDetail.eventTime;
                var policyVersion = this.auditaccessDetail.policyVersion;
                var application = this.auditaccessDetail.agentId;
                var policy = new RangerPolicy({
                        id: policyId,
                        version:policyVersion
                });
                var policyVersionList = policy.fetchVersions();
                this.policyDetailsTbl = new RangerPolicyRO({
                    policy: policy,
                    policyVersionList : policyVersionList,
                    serviceDefList: that.serviceDefList,
                    eventTime : eventTime,
                    repoType : repoType
                });
            },

            /** on render callback */
            onRender: function() {
                if(this.auditAccessView && this.auditaccessDetail.policyId !== -1) {
                    this.policyDetailsView.show(this.policyDetailsTbl);
                }
            },
            /** on close */
            onClose: function() {}
    });

    return AuditAccessLogDetailView;
});