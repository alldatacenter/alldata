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

 /**
 * 
 * Common module for Links used.
 * All hrefs should be used from this file.
 *    
*/
define(function(require) {
	'use strict';
	var XALinks     = {};
	var XAEnums		= require('utils/XAEnums');
	var defaults = {
		href : 'javascript:void(0)',
		text : '',
		title : 'Title'
	};
	var links = {
			/* Main Menu links START */
			
			Dashboard : { 
				href : '#', 
				text : 'h.dashboard',
				title: 'h.dashboard'
			},
			PolicyManager :{
				href : '#!/policymanager', 
				text : 'h.policyManager',
				title: 'h.policyManager'
			},
			RepositoryManager :{
				href : '#!/policymanager', 
				text : 'h.repositoryManager',
				title: 'h.repositoryManager'
			},
			ServiceManager : function(options){
				var textVal = 'h.serviceManager';
				return {
					href : '#!/policymanager/resource',
					text :  !_.isEmpty(options) ? 'Service Manager : ' + options + ' zone' : textVal,
					title : !_.isEmpty(options) ? 'Service Manager : ' + options + ' zone' : textVal,
				}
			},
			TagBasedServiceManager : function(options){
				var textVal = 'h.serviceManager';
				return{
					href : '#!/policymanager/tag',
					text : !_.isEmpty(options) ? 'Service Manager : ' + options + ' zone' : textVal,
					title: !_.isEmpty(options) ? 'Service Manager : ' + options + ' zone' : textVal,
				}
			},
			Users : { 
				href : '#!/users/usertab',
                                text : 'h.usersOrGroupsOrRoles',
                                title: 'h.usersOrGroupsOrRoles'
			},
			Groups : { 
				href : '#!/users/grouptab',
                                text : 'h.usersOrGroupsOrRoles',
                                title: 'h.usersOrGroupsOrRoles'
			},
			Kms : { 
				href : '#!/kms/keys/new/manage/service',
				text : 'h.kms',
				title: 'h.kms'
			},
			KmsKeyCreate : { 
				href : 'javascript:void(0);',
				text : 'h.keyCreate',
				title: 'h.keyCreate'
			},
			KmsKeyEdit : { 
				href : 'javascript:void(0);',
				text : 'h.keyEdit',
				title: 'h.keyEdit'
			},
			KmsKeyForService : { 
				href : 'javascript:void(0);',
				text : 'KMS_TEST1',
				title: 'KMS_TEST1'
			},
			ManageTables: { 
				href : '#!/managetables',
				text : 'h.managetables',
				title: 'h.managetables'
			},
			addPolicyForTable: { 
				href : '#!/addpolicyfortable',
				text : 'h.addpolicyfortable',
				title: 'h.addpolicyfortable'
			},
			ManageFolders: { 
				href : '#!/managefolders',
				title: 'h.managefolders',
				text: 'h.managefolders'
			},
			addPolicyForFolder: { 
				href : '#!/addpolicyforfolder',
				title: 'h.addpolicyforfolder',
				text: 'h.addpolicyforfolder'
			},
			HDFSFolders: { 
				href : '#!/hdfs',
				text : 'h.managePolices',
				title: 'h.managePolices'
			},
			PolicyCreate: { 
				href : 'javascript:void(0);',
				text: 'h.createPolicy',
				title: 'h.createPolicy'
			},
			PolicyEdit: { 
				href : 'javascript:void(0);',
				text: 'h.editPolicy',
				title: 'h.editPolicy'
			},
			HivePolicyListing : {
				href : '#!/hive',
				title: 'h.managetables',
				text: 'h.manageTables'
			},
			/*
			 * Asset related
			 */
			/*AssetCreate: { 
				href : '#!/asset/create',
				text: 'lbl.createAsset',
				title: 'lbl.createAsset'
			},*/
			/*
			 * Reports Related
			 */
			UserAccessReport: { 
				href : '#!/reports/userAccess',
				text: 'lbl.userAccessReport',
				title: 'lbl.userAccessReport'
			},
			AuditReport: { 
				href : '#!/reports/audit',
				text: 'lbl.auditReport',
				title: 'lbl.auditReport'
			},
			UserProfile :{
				href : '#!/userprofile', 
				text : 'h.userProfile',
				title: 'h.userProfile'
			},
			
			/*
			 * User OR Groups Related
			 */
			UserCreate : {
					href : '#!/user/create',
					text : 'lbl.userCreate',
					title: 'lbl.userCreate'
			},
			UserEdit : {
				href : 'javascript:void(0);',
				text : 'lbl.userEdit',
				title: 'lbl.userEdit'
			},
			GroupCreate : {
				href : '#!/group/create',
				text : 'lbl.groupCreate',
				title: 'lbl.groupCreate'
			},
			GroupEdit : {
				href : 'javascript:void(0);',
				text : 'lbl.groupEdit'
			},
			SessionDetail : {
				href : '#!/reports/audit/loginSession',
				text : 'lbl.sessionDetail'
			},
			ServiceCreate : {
				href : "javascript:void(0);",
				text : 'lbl.createService',
				title: 'lbl.createService'
			},
			ServiceEdit : function(options){
				var href = "javascript:void(0);";
				if(_.has(options,'model')){
					href =  '#!/service/'+options.model.get('id');
				}
				if(_.has(options,'id')){
					href =  '#!/service/'+options.id;
				}
				return {
					href : href,
					text : 'lbl.editService',
					title: 'lbl.editService'
				};
			},
			ManagePolicies : function(options){
				var href = "javascript:void(0);";
				if(_.has(options,'model')){
					href =  '#!/service/'+options.model.id+"/policies/"+XAEnums.RangerPolicyType.RANGER_ACCESS_POLICY_TYPE.value;
				}
				return {
					href : href,
                    text : options.model.get('displayName') +' Policies',
                    title: options.model.get('displayName') +' Policies'
				};
			},
			ManageHivePolicies : function(options){
				var href = "javascript:void(0);";
				if(_.has(options,'model')){
					href =  '#!/hive/'+options.model.id+"/policies";
				}
				return {
					href : href,
					text : options.model.get('name') +' Policies',
					title: options.model.get('name') +' Policies'
				};
			},
			ManageHbasePolicies : function(options){
				var href = "javascript:void(0);";
				if(_.has(options,'model')){
					href =  '#!/hbase/'+options.model.id+"/policies";
				}
				return {
					href : href,
					text : options.model.get('name') +' Policies',
					title: options.model.get('name') +' Policies'
				};
			},
			ManageKnoxPolicies : function(options){
				var href = "javascript:void(0);";
				if(_.has(options,'model')){
					href =  '#!/knox/'+options.model.id+"/policies";
				}
				return {
					href : href,
					text : options.model.get('name') +' Policies',
					title: options.model.get('name') +' Policies'
				};
            },
            ManageStormPolicies : function(options){
                var href = "javascript:void(0);";
                if(_.has(options,'model')){
                    href =  '#!/storm/'+options.model.id+"/policies";
                }
                return {
                    href : href,
                    text : options.model.get('name') +' Policies',
                    title: options.model.get('name') +' Policies'
                };
			},
			ModulePermissions :{
                                href : '#!/permissions/models',
				text : 'h.permissions',
				title: 'h.permissions'
			},
			ModulePermissionEdit : function(options){
                var href = "javascript:void(0);";
                if(_.has(options,'model')){
                    href =  '#!/permissions/'+options.model.id+"/edit";
                }
                return {
                    href : href,
                    text : options.model.get('module'),
                    title: options.model.get('module')
                };
			},
			KmsServiceForKey : function(options) {
				var href = "javascript:void(0);";
				if(_.has(options,'kmsServiceDefModel') && _.has(options,'kmsService')){
                    href =  '#!/service/'+options.kmsServiceDefModel.id+"/edit/"+options.kmsService.id;
                }
				return {
                    href : href,
                    text : options.kmsService.get('name'),
                    title: options.kmsService.get('name')
                };
			},
			KmsManage : function(options) {
				var href = "javascript:void(0);";
				if(_.has(options,'kmsService')){
                    href =  '#!/kms/keys/edit/manage/'+options.kmsService;
                }
				return {
                    href : href,
                    text : 'h.kms',
                    title: 'h.kms'
                };
            },
            SecurityZone :{
                href : '#!/zones/zone/list',
                text : 'h.securityZone',
                title: 'h.securityZone'
            },
            ZoneCreate : {
                href : '#!/zones/create',
                text : 'h.zoneCreate',
                title: 'h.zoneCreate'
            },
            ZoneEdit : function(options){
                return {
                    href : 'javascript:void(0);',
                    text : 'h.zoneEdit',
                    title: 'h.zoneEdit'
                };
            },
            ZoneName: function(options) {
                return {
                    href: 'javascript:void(0)',
                    text: 'Zone Name : ' + options
                }
            },
            Roles : {
                href : '#!/users/roletab',
                text : 'h.usersOrGroupsOrRoles',
                title: 'h.usersOrGroupsOrRoles',
            },
            RoleCreate : {
                href : '#!/role/create',
                text : 'lbl.roleCreate',
                title: 'lbl.roleCreate',
            },
            RoleEdit : {
                href : 'javascript:void(0);',
                text : 'lbl.roleEdit',
                title: 'lbl.roleEdit',
            },
	};      
       
	
	XALinks.get = function(type, options){
		if(! _.has(links, type)){
			throw new Error("Link " + type + " not found in XALinks Module!");
		}
		var opts = {};
		// in some case we will get model directly as options
		if( options && !_.has(options,'model') &&  options instanceof Backbone.Model){
			opts['model'] = options;
		} else {
			opts = options;
		}

		if(typeof links[type] === 'function'){
			return links[type](opts||{});
		}

		return links[type];
	};
	
	
	
	
	return XALinks;

});

