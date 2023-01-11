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
 * Policy create view
 */

define(function(require){
    'use strict';

	var Backbone		= require('backbone');
	var App				= require('App');
	var XAEnums			= require('utils/XAEnums');
	var XAUtil			= require('utils/XAUtils');
	var XALinks 		= require('modules/XALinks');
	var localization	= require('utils/XALangSupport');
	
	var RangerPolicycreateTmpl = require('hbs!tmpl/policies/RangerPolicyCreate_tmpl');
	var RangerPolicyForm = require('views/policies/RangerPolicyForm');
	var RangerServiceDef	= require('models/RangerServiceDef');
	var Vent			 = require('modules/Vent');

	var RangerPolicyCreate = Backbone.Marionette.Layout.extend(
	/** @lends RangerPolicyCreate */
	{
		_viewName : 'RangerPolicyCreate',
		
    	template : RangerPolicycreateTmpl,
    	templateHelpers : function(){
                var infoMsg = '', displayClass = 'd-none', policyTimeStatus = '', expiredClass = 'd-none';
		if(XAUtil.isMaskingPolicy(this.model.get('policyType'))){
			if(XAUtil.isTagBasedDef(this.rangerServiceDefModel)){
				infoMsg = localization.tt('msg.maskingPolicyInfoMsgForTagBased'), displayClass = 'show';	
			}else{
				infoMsg = localization.tt('msg.maskingPolicyInfoMsg'), displayClass = 'show';
			}
		}else if(XAUtil.isRowFilterPolicy(this.model.get('policyType'))){
			infoMsg = localization.tt('msg.rowFilterPolicyInfoMsg'), displayClass = 'show';
		}
        if(this.editPolicy && !_.isEmpty(this.model.get('validitySchedules'))){
            if(XAUtil.isPolicyExpierd(this.model)){
                policyTimeStatus = localization.tt('msg.policyExpired'), expiredClass = 'show';
            }else{
                expiredClass = 'd-none';
            }
                }
    		return {
			editPolicy : this.editPolicy,
			infoMsg : infoMsg,
                        displayClass : displayClass,
                        policyTimeStatus : policyTimeStatus,
                        expiredClass : expiredClass
    		};
    	},
    	breadCrumbs :function(){
    		var name  = this.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label ? 'ServiceManager' : 'TagBasedServiceManager';
    		if(this.model.isNew()){
                if(App.vZone && App.vZone.vZoneName){
                   return [XALinks.get(name, App.vZone.vZoneName), 
                        XALinks.get('ManagePolicies',{model : this.rangerService}), XALinks.get('PolicyCreate')];
                }else{
                    return [XALinks.get(name),XALinks.get('ManagePolicies',{model : this.rangerService}),
                        XALinks.get('PolicyCreate')];
                }
            } else {
                if(App.vZone && App.vZone.vZoneName){
                    return [XALinks.get(name, App.vZone.vZoneName),
                        XALinks.get('ManagePolicies',{model : this.rangerService}), XALinks.get('PolicyEdit')];
                }else{
                    return [XALinks.get(name),XALinks.get('ManagePolicies',{model : this.rangerService}),
                        XALinks.get('PolicyEdit')];
                }
                /*return [XALinks.get(name),XALinks.get('ManagePolicies',{model : this.rangerService}),
                    XALinks.get('PolicyEdit')];*/
    		}
    	} ,        

		/** Layout sub regions */
    	regions: {
			'rForm' :'div[data-id="r_form"]'
		},

    	/** ui selector cache */
    	ui: {
			'btnSave'	: '[data-id="save"]',
			'btnCancel' : '[data-id="cancel"]',
			'btnDelete' : '[data-id="delete"]',
			'policyDisabledAlert' : '[data-id="policyDisabledAlert"]' 
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click ' + this.ui.btnSave]		= 'onSave';
			events['click ' + this.ui.btnCancel]	= 'onCancel';
			events['click ' + this.ui.btnDelete]	= 'onDelete';
			
			return events;
		},

    	/**
		* intialize a new RangerPolicyCreate Layout 
		* @constructs
		*/
		initialize: function(options) {
			var that = this;
			console.log("initialized a RangerPolicyCreate Layout");

			_.extend(this, _.pick(options, 'rangerService'));
			this.initializeServiceDef();
			that.form = new RangerPolicyForm({
				template : require('hbs!tmpl/policies/RangerPolicyForm_tmpl'),
				model : this.model,
				rangerServiceDefModel : this.rangerServiceDefModel,
				rangerService : this.rangerService,
			});

			this.editPolicy = this.model.has('id') ? true : false;
			this.bindEvents();
			this.params = {};
		},
		initializeServiceDef : function(){
			
			this.rangerServiceDefModel	= new RangerServiceDef();
			this.rangerServiceDefModel.url = XAUtil.getRangerServiceDef(this.rangerService.get('type'));
			this.rangerServiceDefModel.fetch({
				cache : false,
				async : false
			});
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			if((localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null)
				&& App.rSideBar.$el.hasClass('expanded')) {
				App.rContent.$el.addClass('expanded-contant');
			} else {
				App.rContent.$el.removeClass('expanded-contant');
			}
			XAUtil.showAlerForDisabledPolicy(this);
			this.rForm.show(this.form);
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.rForm.$el);
		},
		popupCallBack : function(msg,validateObj){
			if(XAUtil.isMaskingPolicy(this.model.get('policyType'))){
				msg = msg.replace('permission','access type')
			}
			XAUtil.alertPopup({
				msg :msg,
			});
		},
		onSave: function(){
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				return;
			}
			
			
			//validate policyItems in the policy
			var validateObj1 = this.form.formValidation(this.form.formInputList);
			if(!this.validatePolicyItem(validateObj1)) return;
			var	validateObj2 = this.form.formValidation(this.form.formInputAllowExceptionList);
			if(!this.validatePolicyItem(validateObj2)) return;
			var	validateObj3 = this.form.formValidation(this.form.formInputDenyList);
			if(!this.validatePolicyItem(validateObj3)) return;
			var	validateObj4 = this.form.formValidation(this.form.formInputDenyExceptionList);
			if(!this.validatePolicyItem(validateObj4)) return;
			
			var userPerm = (validateObj1.userPerm || validateObj2.userPerm
					  || validateObj3.userPerm || validateObj4.userPerm);
			var groupPerm = (validateObj1.groupPermSet || validateObj2.groupPermSet 
                                        || validateObj3.groupPermSet || validateObj4.groupPermSet);
                        var rolePerm = (validateObj1.rolePerm || validateObj2.rolePerm
                                        || validateObj3.rolePerm || validateObj4.rolePerm);
                        var delegatePerm  = (validateObj1.delegateAdmin || validateObj2.delegateAdmin
                                        || validateObj3.delegateAdmin || validateObj4.delegateAdmin);
                        if((!validateObj1.auditLoggin) && !(groupPerm || userPerm || delegatePerm || rolePerm)){
				XAUtil.alertPopup({ msg :localization.tt('msg.yourAuditLogginIsOff') });
				return;
			}
			
			if(!validateObj1.customMaskSet){
				XAUtil.alertPopup({ msg :localization.tt('msg.enterCustomMask') });
				return;
			}
			this.savePolicy();
		},
		validatePolicyItem : function(validateObj){
			var that = this, valid = false;
                        //DelegateAdmin checks
                        if((validateObj.groupSet || validateObj.userSet || validateObj.roleSet) && validateObj.delegateAdmin){
                                return true;
                        }else if(validateObj.delegateAdmin && !(validateObj.groupSet || validateObj.userSet || validateObj.roleSet)) {
                                this.popupCallBack(localization.tt('msg.addUserOrGroupOrRoleForDelegateAdmin'),validateObj);
                                return false;
                        }
                        valid = (validateObj.groupSet && validateObj.permSet) || (validateObj.userSet && validateObj.userPerm)
                        || (validateObj.roleSet && validateObj.rolePerm);
			if(!valid){
                                if((!validateObj.groupSet && !validateObj.userSet && !validateObj.roleSet) && (validateObj.condSet)) {
                                        this.popupCallBack(localization.tt('msg.addUserOrGroupOrRoleForPC'),validateObj);
                                } else if((!validateObj.groupSet && !validateObj.userSet && !validateObj.roleSet) && (validateObj.permSet)) {
                                        this.popupCallBack(localization.tt('msg.addUserOrGroupOrRole'),validateObj);
					
				} else if(validateObj.groupSet && (!validateObj.permSet)){
					this.popupCallBack(localization.tt('msg.addGroupPermission'),validateObj);
				} else if((!validateObj.groupSet) && (validateObj.permSet)) {
					this.popupCallBack(localization.tt('msg.addGroup'),validateObj);
						
				} else if(validateObj.userSet && (!validateObj.userPerm)){
					this.popupCallBack(localization.tt('msg.addUserPermission'),validateObj);
				} else if((!validateObj.userSet) && (validateObj.userPerm)) {
					this.popupCallBack(localization.tt('msg.addUser'),validateObj);
						
                                } else if(validateObj.roleSet && (!validateObj.rolePerm)){
                                        this.popupCallBack(localization.tt('msg.addRolePermission'),validateObj);
                                } else if((!validateObj.roleSet) && (validateObj.rolePerm)) {
                                        this.popupCallBack(localization.tt('msg.addRole'),validateObj);
				} else if((!validateObj.auditLoggin) && (!validateObj.groupPermSet)){
					return true;
				}else{
					return true;
				}
			} else {
				if(validateObj.groupSet && (!validateObj.permSet)){
					this.popupCallBack(localization.tt('msg.addGroupPermission'),validateObj);
				} else if((!validateObj.groupSet) && (validateObj.permSet)) {
					this.popupCallBack(localization.tt('msg.addGroup'),validateObj);
				} else if(validateObj.userSet && (!validateObj.userPerm)){
					this.popupCallBack(localization.tt('msg.addUserPermission'),validateObj);
				} else if((!validateObj.userSet) && (validateObj.userPerm)) {
					this.popupCallBack(localization.tt('msg.addUser'),validateObj);
                                } else if(validateObj.roleSet && (!validateObj.rolePerm)){
                                        this.popupCallBack(localization.tt('msg.addRolePermission'),validateObj);
                                } else if((!validateObj.roleSet) && (validateObj.rolePerm)) {
                                        this.popupCallBack(localization.tt('msg.addRole'),validateObj);
				} else {
					return true;
				}
			}
			return false;
		},
		savePolicy : function(){
			var that = this;
			this.form.beforeSave();
			this.saveMethod();
		},
		saveMethod : function(){
			var that = this;
			XAUtil.blockUI();
			this.model.save({},{
				wait: true,
				success: function () {
					XAUtil.blockUI('unblock');
					var msg = that.editPolicy ? 'Policy updated successfully' :'Policy created successfully';
					XAUtil.notifySuccess('Success', msg);
					XAUtil.allowNavigation();
					App.appRouter.navigate("#!/service/"+that.rangerService.id+"/policies/"+ that.model.get('policyType'),{trigger: true});
				},
				error : function(model, response, options) {
					XAUtil.blockUI('unblock');
					var msg = that.editPolicy ? 'Error updating policy.': 'Error creating policy.';
					if (response && response.responseJSON && response.responseJSON.msgDesc) {
						XAUtil.showErrorMsg(response.responseJSON.msgDesc);
					} else {
						XAUtil.notifyError('Error', msg);
					}
				}
			});
		},
		onCancel : function(){
			XAUtil.allowNavigation();
			App.appRouter.navigate("#!/service/"+this.rangerService.id+"/policies/"+ this.model.get('policyType'),{trigger: true});

		},
		onDelete :function(){
			var that = this;
			XAUtil.confirmPopup({
				msg :'Are you sure want to delete ?',
				callback : function(){
					XAUtil.blockUI();
					that.model.destroy({
						wait: true,
						success: function(model, response) {
							XAUtil.blockUI('unblock');
							XAUtil.allowNavigation();
							XAUtil.notifySuccess('Success', localization.tt('msg.policyDeleteMsg'));
							App.appRouter.navigate("#!/service/"+that.rangerService.id+"/policies/"+ that.model.get('policyType'),{trigger: true});
						},
						error: function (model, response, options) {
							XAUtil.blockUI('unblock');
							if (response && response.responseJSON && response.responseJSON.msgDesc){
								    XAUtil.notifyError('Error', response.responseJSON.msgDesc);
							} else {
							    	XAUtil.notifyError('Error', 'Error deleting Policy!');
							}
						}
					});
				}
			});
		},
		/** on close */
		onClose: function(){
			XAUtil.allowNavigation();
//			clear Vent 
			Vent._events['resourceType:change']=[];
                XAUtil.removeUnwantedDomElement();
		}
	});
	return RangerPolicyCreate;
});
