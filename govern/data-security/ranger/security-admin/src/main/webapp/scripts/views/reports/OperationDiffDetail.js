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

 
define(function(require){
    'use strict';

	var Backbone						= require('backbone');
	var XAEnums					 		= require('utils/XAEnums');
	var XALinks							= require('modules/XALinks');
	var AssetOperationDiff_tmpl 		= require('hbs!tmpl/reports/AssetOperationDiff_tmpl');
	var AssetUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/AssetUpdateOperationDiff_tmpl');
	var UserOperationDiff_tmpl 			= require('hbs!tmpl/reports/UserOperationDiff_tmpl');
	var UserUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/UserUpdateOperationDiff_tmpl');
	var GroupOperationDiff_tmpl 		= require('hbs!tmpl/reports/GroupOperationDiff_tmpl');
	var GroupUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/GroupUpdateOperationDiff_tmpl');
	var ZoneOperationDiff_tmpl 			= require('hbs!tmpl/reports/ZoneOperationDiff_tmpl');
	var ZoneUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/ZoneUpdateOperationDiff_tmpl');
        var RoleOperationDiff_tmpl 		    = require('hbs!tmpl/reports/RoleOperationDiff_tmpl');
        var RoleUpdateOperationDiff_tmpl 	= require('hbs!tmpl/reports/RoleUpdateOperationDiff_tmpl');
	
	var OperationDiffDetail = Backbone.Marionette.ItemView.extend(
	/** @lends OperationDiffDetail */
	{
		_viewName : 'OperationDiffDetail',
		
        templateHelpers :function(){
        	var obj = {
                                collection : _.sortBy(this.collection.models, 'id'),
        			action	   : this.action,
        			objectName : this.objectName,
        			objectId   : this.objectId,
        			objectCreatedDate : this.objectCreatedDate,
        			userName   : this.userName
        		};
        	if(this.templateType == XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value){
        		obj = $.extend(obj, {
        			newConnConfig 		: this.newConnConfig,
        			previousConnConfig 	: this.previousConnConfig,
        			isNewConnConfig		: _.isEmpty(this.newConnConfig) ? false : true,
   					isPreviousConnConfig: _.isEmpty(this.previousConnConfig) ? false : true
        		});
        	}
        	if(this.templateType == XAEnums.ClassTypes.CLASS_TYPE_XA_RESOURCE.value){
        		obj = $.extend(obj, {newGroupPermList 			: this.newGroupPermList, 
				        			previousGroupPermList 		: this.previousGroupPermList,
        							newUserPermList 			: this.newUserPermList,
        							previousUserPermList 		: this.previousUserPermList,
        							isGroupPerm 				: this.isGroupPerm,
        							isUserPerm 					: this.isUserPerm,
        							groupList					: this.groupList,
        							userList					: this.userList,
        							repositoryType				: this.repositoryType,
        							policyName					: this.policyName
        			  });
        	}
        	if(this.templateType == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value){
        		obj = $.extend(obj, { 
        				newGroupList 		: this.newGroupList,
        				previousGroupList 	: this.previousGroupList,
        				isGroup 			: this.isGroup
        		});
        	}
        	if(this.templateType == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value){
        		obj = $.extend(obj, {
        			newServiceResourceArr 		: this.newServiceResourceArr,
        			isNewServiceResourceArr		: _.isEmpty(this.newServiceResourceArr) ? false : true,
        			oldServiceResourceArr 		: this.oldServiceResourceArr,
        			isOldServiceResourceArr		: _.isEmpty(this.oldServiceResourceArr) ? false : true,
        			isServiceResourcesDiffEmpty : (_.isEmpty(this.oldServiceResourceArr) && _.isEmpty(this.oldServiceResourceArr)) ? false : true
        		});
        	}

                        if(this.templateType == XAEnums.ClassTypes.CLASS_TYPE_RANGER_ROLE.value){
                                obj = $.extend(obj, {
                                        oldRolesUsersGroupsRolesDetails : this.oldRolesUsersGroupsRolesDetails,
                                        newRolesUsersGroupsRolesDetails : this.newRolesUsersGroupsRolesDetails,
                                        updateRolesUsersGroupsRolesDetails : this.updateRolesUsersGroupsRolesDetails
                                });
                        }

        	return obj;
        },
    	/** ui selector cache */
    	ui: {
    		groupPerm : '.groupPerm',
    		userPerm  : '.userPerm',
    		oldValues : '[data-id="oldValues"]',
    		diff 	  : '[data-id="diff"]',
    		policyDiff: '[data-name="policyDiff"]'
    		
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			return events;
		},

    	/**
		* intialize a new OperationDiffDetail ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a OperationDiffDetail ItemView");
			
			_.extend(this, _.pick(options, 'classType','objectName','objectId','objectCreatedDate','action','userName'));
			this.bindEvents();
			this.getTemplateForView();
			
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},
		/** on render callback */
		onRender: function() {
			this.initializePlugins();
			
			//remove last comma from Perms
			_.each(this.ui.diff.find('ol li'),function(m){
				var text = $(m).text().replace(/,(?=[^,]*$)/, '');
				$(m).find('span').last().remove();
			});
			_.each(this.ui.policyDiff.find('ol li'),function(m){
				if(_.isEmpty($(m).text().trim()))
					$(m).removeClass('change-row').text('--');
			});
		},
		getTemplateForView : function(){
			if(this.classType == XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value
					|| this.classType == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SERVICE.value){
				this.templateType=XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value;
				if(this.action == 'create'){
					this.template = AssetOperationDiff_tmpl;
				} else if(this.action == 'update'){
					this.template = AssetUpdateOperationDiff_tmpl;
				} else {
					this.template = AssetOperationDiff_tmpl;
				}
				this.assetDiffOperation();
			}
			if(this.classType == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value
					|| this.classType == XAEnums.ClassTypes.CLASS_TYPE_USER_PROFILE.value
					|| this.classType == XAEnums.ClassTypes.CLASS_TYPE_PASSWORD_CHANGE.value){
				if(this.action == 'create' || this.action == 'delete'){
					this.template = UserOperationDiff_tmpl;	
				} else if(this.action == 'update' || this.action == "password change"){
					this.template = UserUpdateOperationDiff_tmpl;
				} else{
					this.template = UserOperationDiff_tmpl;
				}
				this.userDiffOperation();
				this.templateType = XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value;
			} 
			if(this.classType == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value){
				if(this.action == 'create'){
					this.template = GroupOperationDiff_tmpl;
				} else if(this.action == 'update'){
					this.template = GroupUpdateOperationDiff_tmpl;
				} else{
					this.template = GroupOperationDiff_tmpl;
				}
				this.templateType = XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value;
			} 
			if (this.classType == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value){
				this.zoneDiffOperation();
				this.templateType = XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value;
				if(this.action == 'update')
					this.template = ZoneUpdateOperationDiff_tmpl;
				else
					this.template = ZoneOperationDiff_tmpl;
			}
            if(this.classType == XAEnums.ClassTypes.CLASS_TYPE_RANGER_ROLE.value){
                this.rolesDiffOperation();
                if(this.action == 'update'){
                    this.template = RoleUpdateOperationDiff_tmpl;
                } else{
                    this.template = RoleOperationDiff_tmpl;
                }
                this.templateType = XAEnums.ClassTypes.CLASS_TYPE_RANGER_ROLE.value;
            }
		},
		assetDiffOperation : function(){	
			var that = this, configModel;
			
			this.collection.each(function(m){
				if(m.get('attributeName') == 'Connection Configurations'){
					if(m.get('action') != 'delete')
						that.newConnConfig = $.parseJSON(m.get('newValue'));
					if(m.get('action') == 'update' || m.get('action') == 'delete')
						that.previousConnConfig = $.parseJSON(m.get('previousValue'));
					configModel = m;
				}else if(m.get('attributeName') == "Service Status"){
					 var newVal = m.get('newValue'), oldVal = m.get('previousValue');            
					 if(!_.isUndefined(newVal) && !_.isEmpty(newVal)){
					         m.set('newValue', $.parseJSON(newVal) ? XAEnums.ActiveStatus.STATUS_ENABLED.label 
					                         : XAEnums.ActiveStatus.STATUS_DISABLED.label);
					 }
					 if(!_.isUndefined(oldVal) && !_.isEmpty(oldVal)){
					         m.set('previousValue', $.parseJSON(oldVal) ? XAEnums.ActiveStatus.STATUS_ENABLED.label 
					                         : XAEnums.ActiveStatus.STATUS_DISABLED.label);
					 }
				}
			});
			if(configModel)
				this.collection.remove(configModel);
			if(this.action == 'create' || this.action == 'delete'){
				this.newConnConfig 		= this.removeUnwantedFromObject(this.newConnConfig);
				this.previousConnConfig = this.removeUnwantedFromObject(this.previousConnConfig);
			}else{
				var tmp = this.newConnConfig, tmp1 = {};
				_.each(tmp,function(val, name){ tmp1[name] = ""; });
				_.each(this.previousConnConfig,function(val, name){ tmp1[name]=val; });
				this.previousConnConfig = tmp1;
			}
			
		},

		userDiffOperation : function(){
			var that = this, modelArr = [];
			this.groupList = [], this.newGroupList = [], this.previousGroupList =[],this.isGroup = false;
			
			this.collection.each(function(m){
				if(m.get('attributeName') == 'Group Name'){
					if(m.get('action') == 'create' || m.get('action') == 'update')
						that.newGroupList.push(m.get('parentObjectName'));
					if(m.get('action') == 'delete' || m.get('action') == 'update')
						that.previousGroupList.push(m.get('parentObjectName'));
					modelArr.push(m);
				} else if(m.get('attributeName') == 'User Role'){
					var newRole;
					if(!_.isUndefined(m.get('newValue'))){
						newRole =  m.get('newValue').replace(/[[\]]/g,'');
					}
					var prevRole = m.get('previousValue').replace(/[[\]]/g,'');
					if( newRole == "ROLE_USER")
						m.set('newValue',XAEnums.UserRoles.ROLE_USER.label)
					else if(newRole == "ROLE_SYS_ADMIN")
						m.set('newValue',XAEnums.UserRoles.ROLE_SYS_ADMIN.label)
					else if(newRole == "ROLE_KEY_ADMIN")
						m.set('newValue',XAEnums.UserRoles.ROLE_KEY_ADMIN.label)
                    else if(newRole == "ROLE_KEY_ADMIN_AUDITOR")
                        m.set('newValue',XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.label)
                    else if(newRole == "ROLE_ADMIN_AUDITOR")
                        m.set('newValue',XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.label)
					if(prevRole == "ROLE_USER")
						m.set('previousValue',XAEnums.UserRoles.ROLE_USER.label)
					else if(prevRole == "ROLE_SYS_ADMIN")
						m.set('previousValue',XAEnums.UserRoles.ROLE_SYS_ADMIN.label)
					else if(prevRole == "ROLE_KEY_ADMIN")
						m.set('previousValue',XAEnums.UserRoles.ROLE_KEY_ADMIN.label)
                    else if(prevRole == "ROLE_KEY_ADMIN_AUDITOR")
                        m.set('previousValue',XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.label)
                    else if(prevRole == "ROLE_ADMIN_AUDITOR")
                        m.set('previousValue',XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.label)
				} else {
					if(!m.has('attributeName'))
						modelArr.push(m);
				}
			});
			if(!_.isEmpty(this.newGroupList) || !_.isEmpty(this.previousGroupList)){
				this.isGroup = true;
			}
			this.collection.remove(modelArr);
		},
		groupDiffOperation : function(){
			var modelArr = [];
			this.collection.each(function(m){
				if(m.get('attributeName') == 'Group Name' && m.get('action') == 'create')
					modelArr.push(m);
			});
			this.collection.remove(modelArr);
		},	
		zoneDiffOperation : function(){
			var that = this;
			this.newServiceResourceArr=[], this.oldServiceResourceArr=[];
			var servicesObj = this.collection.findWhere({attributeName : 'Zone Services'});
			if(servicesObj && servicesObj.get('newValue')  != ''){
				var newValJson = $.parseJSON(servicesObj.get('newValue'));
				var serviceNames = Object.keys(newValJson);
				_.each(serviceNames, function(serviceName){
					that.newServiceResourceArr.push({serviceName : serviceName, resources : newValJson[serviceName]['resources'] });
				});
			}
			if(servicesObj && servicesObj.get('previousValue')  != ''){
				var oldValJson = $.parseJSON(servicesObj.get('previousValue'));
				var serviceNames = Object.keys(oldValJson);
				_.each(serviceNames, function(serviceName){
					that.oldServiceResourceArr.push({serviceName : serviceName, resources : oldValJson[serviceName]['resources'] });
				});
			}
			this.collection.remove(this.collection.where({attributeName : 'Zone Services'}));
			this.collection.each(function(m){
				if(m.get('newValue')[0] == '[' && m.get('newValue')[m.get('newValue').length-1] == ']')
					m.set('newValue', m.get('newValue').substr(1).slice(0, -1));

				if(m.get('previousValue')[0] == '[' && m.get('previousValue')[m.get('previousValue').length - 1] == ']')
					m.set('previousValue', m.get('previousValue').substr(1).slice(0, -1));
			});

		},

                rolesDiffOperation : function() {
                        var that = this;
                        this.newRolesUsersGroupsRolesDetails = {};
                        this.oldRolesUsersGroupsRolesDetails = {};
                        this.updateRolesUsersGroupsRolesDetails = {};
                        var roleDetails = ['Users', 'Groups', 'Roles'];
                        if(this.action !== 'update') {
                                _.each(roleDetails, function(m) {
                                        var rolesObj = that.collection.findWhere({attributeName : m});
                                        if(rolesObj && rolesObj.has('newValue') &&!_.isEmpty(rolesObj.get('newValue'))) {
                                                var newValJson = $.parseJSON(rolesObj.get('newValue'));
                                                that.newRolesUsersGroupsRolesDetails[m] = (newValJson);
                                        }
                                })
                                _.each(roleDetails, function(m) {
                                        var rolesObj = that.collection.findWhere({attributeName : m});
                                        if(rolesObj && rolesObj.has('previousValue') &&!_.isEmpty(rolesObj.get('previousValue'))) {
                                                var newValJson = $.parseJSON(rolesObj.get('previousValue'));
                                                that.oldRolesUsersGroupsRolesDetails[m] = (newValJson);
                                        }
                                })
                        } else {
                                _.each(roleDetails, function(m) {
                                        var rolesObj = that.collection.findWhere({attributeName : m});
                                        if(rolesObj) {
                                                that.updateRolesUsersGroupsRolesDetails[m] = {};
                                                var newValJson, oldValJson;
                                                try {
                                                        newValJson = JSON.parse(rolesObj.get('newValue'));
                                                } catch(err) {
                                                        newValJson = "";
                                                } finally {
                                                        that.updateRolesUsersGroupsRolesDetails[m]["newVal"] = (newValJson)
                                                }
                                                try {
                                                        oldValJson = JSON.parse(rolesObj.get('previousValue'));
                                                } catch(err) {
                                                        oldValJson = "";
                                                } finally {
                                                        that.updateRolesUsersGroupsRolesDetails[m]["oldVal"] = (oldValJson)
                                                }
                                        }
                                })
                        }
                        _.each(roleDetails, function(key) {
                                that.collection.remove(that.collection.where({attributeName : key}));
                        })
                },

		removeUnwantedFromObject : function(obj){
			_.each(obj, function(val, key){
					if(_.isEmpty(val))
						delete obj[key];
				});
			return obj;
		},
		objectSize : function(obj) {
		    var size = 0, key;
		    for (key in obj) {
		        if (obj.hasOwnProperty(key)) size++;
		    }
		    return size;
		},
		
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		/** on close */
		onClose: function(){
		}

	});

	return OperationDiffDetail;
});
