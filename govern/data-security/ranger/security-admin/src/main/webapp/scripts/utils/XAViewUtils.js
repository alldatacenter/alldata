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
	
    var XAEnums = require('utils/XAEnums');
    var localization = require('utils/XALangSupport');
    var XAUtils = require('utils/XAUtils');
    var XAViewUtil = {};
    require('Backbone.BootstrapModal');

    XAViewUtil.resourceTypeFormatter = function(rawValue, model){
        var resourcePath = _.isUndefined(model.get('resourcePath')) ? undefined : model.get('resourcePath');
        var resourceType = _.isUndefined(model.get('resourceType')) ? undefined : model.get('resourceType');
        if((model.get('serviceType') === XAEnums.ServiceType.Service_HIVE.label || model.get('serviceType') === XAEnums.ServiceType.Service_HBASE.label || model.get('serviceType') === XAEnums.ServiceType.Service_SOLR.label || model.get('serviceType') === XAEnums.ServiceType.Service_HDFS.label)
            && model.get('aclEnforcer') === "ranger-acl"
            && model.get('requestData')){
            if(resourcePath && !_.isEmpty(model.get('requestData'))) {
                return '<div class="clearfix">\
                            <div class="pull-left resourceText" title="'+ _.escape(resourcePath)+'">'+_.escape(resourcePath)+'</div>\
                            <div class="pull-right">\
                                <div class="queryInfo btn btn-sm link-tag query-icon" title="Query Info" data-name = "queryInfo" data-id ="'+model.get('id')+'">\
                                    <i class="fa-fw fa fa-table" ></i>\
                                </div>\
                            </div>\
                        </div>\
                        <div title="'+_.escape(resourceType)+'" class="border-top-1">'+_.escape(resourceType)+'</div>';
            }else{
                return '<div class="clearfix">\
                            <div class="pull-left">--</div>\
                            <div class="pull-right">\
                                <div class="queryInfo btn btn-sm link-tag query-icon" title="Query Info" data-id ="'+model.get('id')+'"data-name = "queryInfo"">\
                                    <i class="fa-fw fa fa-table"></i>\
                                </div>\
                            </div>\
                        </div>';
            }
        }else{
            if(resourcePath){
                return '<div class ="resourceText" title="'+_.escape(resourcePath)+'">'+_.escape(resourcePath)+'</div>\
                        <div title="'+_.escape(resourceType)+'" class="border-top-1">'+_.escape(resourceType)+'</div>';
            }else{
                return '--';
            }
        }
    };

    XAViewUtil.showQueryPopup = function(model, that){
        if((model.get('serviceType') === XAEnums.ServiceType.Service_HIVE.label || model.get('serviceType') === XAEnums.ServiceType.Service_HBASE.label || model.get('serviceType') === XAEnums.ServiceType.Service_SOLR.label || model.get('serviceType') === XAEnums.ServiceType.Service_HDFS.label)
            && model.get('aclEnforcer') === "ranger-acl"
            && model.get('requestData') && !_.isEmpty(model.get('requestData'))){
            var titleMap = {};
            titleMap[XAEnums.ServiceType.Service_HIVE.label] = 'Hive Query';
            titleMap[XAEnums.ServiceType.Service_HBASE.label] = 'HBase Audit Data';
            titleMap[XAEnums.ServiceType.Service_SOLR.label] = 'Solr Query';
            titleMap[XAEnums.ServiceType.Service_HDFS.label] = 'HDFS Operation Name';
            var msg = '<div class="pull-right link-tag query-icon copyQuery btn btn-sm" title="Copy Query"><i class="fa-fw fa fa-copy"></i></div><div class="query-content">'+model.get('requestData')+'</div>';
            var $elements = that.$el.find('table [data-name = "queryInfo"][data-id = "'+model.id+'"]');
            $elements.popover({
                html: true,
                title: '<b class="custom-title">'+ (titleMap[model.get('serviceType')] || 'Request Data') +'</b><a href="javascript:void(0)" class="close popoverClose">&times;</a>',
                content: msg,
                selector : true,
                container:'body',
                placement: 'top'
            }).on("click", function(e){
                e.stopPropagation();
                $('.popoverClose').on("click", function(){
                    $(this).parents(".popover").popover('hide');
                });
                if($(e.target).data('toggle') !== 'popover' && $(e.target).parents('.popover.in').length === 0){
                    $('.queryInfo').not(this).popover('hide');
                    $('.copyQuery').on("click", function(e){
                        XAUtils.copyToClipboard(e , model.get('requestData'));
                    })
                }
            });
        }
    };

    XAViewUtil.syncSourceDetail = function(e , that){
        if($(e.target).is('.fa-fw fa fa-edit,.fa-fw fa fa-trash,a,code'))
            return;
        var SyncSourceView = Backbone.Marionette.ItemView.extend({
            template : require('hbs!tmpl/reports/UserSyncInfo_tmpl'),
            templateHelpers:function(){
                var syncSourceInfo = _.filter(that.userSyncAuditList.models , function(m){
                    return m.id == e.currentTarget.getAttribute('id');
                });
                syncSourceInfo = _.map(syncSourceInfo[0].get('syncSourceInfo'), function(value, key){
                    if(key == 'lastModified' || key == 'syncTime' ){
                        return {'label': 'lbl.'+key, 'value': Globalize.format(new Date(value),  "MM/dd/yyyy hh:mm:ss tt") }
                    }else{
                        return {'label': 'lbl.'+key, 'value': value };
                    }
                });
                return {'syncSourceInfo' : syncSourceInfo };
            },
            initialize: function(){
            },
            onRender: function(){}
        });
        var modal = new Backbone.BootstrapModal({
            animate : true,
            content     : new SyncSourceView({model : this.model}),
            title: localization.tt("h.syncDetails"),
            okText :localization.tt("lbl.ok"),
            allowCancel : true,
            escape : true,
            focusOk : false
        }).open();
        modal.$el.find('.cancel').hide();
    };

    XAViewUtil.syncUsersGroupsDetails = function(self){
        var syncData = [];
        var syncDetails = JSON.parse(self.model.get('otherAttributes'));
        _.mapObject(syncDetails, function(value, key) {
                syncData.push({'label': key, 'value': value});
        })
        return syncData;
    }
	
	return XAViewUtil;

});
