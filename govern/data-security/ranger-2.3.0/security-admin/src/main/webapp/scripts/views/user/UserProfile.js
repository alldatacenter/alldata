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

	var Backbone		= require('backbone');
	var Communicator	= require('communicator');
	var App				= require('App');
	var XAUtil			= require('utils/XAUtils');
	var XAEnums			= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var localization	= require('utils/XALangSupport');
	var SessionMgr		= require('mgrs/SessionMgr');

	var VPasswordChange	= require("models/VXPasswordChange");
	var UserProfileForm = require('views/user/UserProfileForm');
	var UserprofileTmpl = require('hbs!tmpl/user/UserProfile_tmpl');

	var UserProfile = Backbone.Marionette.Layout.extend(
	/** @lends UserProfile */
	{
		_viewName : 'UserProfile',
		
    	template: UserprofileTmpl,

    	templateHelpers : function(){
			return{
				setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			}
		},

        breadCrumbs : [XALinks.get('UserProfile')],
		/** Layout sub regions */
    	regions: {
    		'rForm' :'div[data-id="r_form"]'
    	},

    	/** ui selector cache */
    	ui: {
    		tab 		: '.nav-tabs',
    		saveBtn 	: 'button[data-id="save"]',
    		cancelBtn 	: 'button[data-id="cancel"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click '+this.ui.tab+' li a']  = 'onTabChange';
			events['click ' + this.ui.saveBtn]  = 'onSave';
			events['click ' + this.ui.cancelBtn]  = 'onCancel';
			return events;
		},

    	/**
		* intialize a new UserProfile Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a UserProfile Layout");
			//_.extend(this, _.pick(options, ''));
			this.showBasicFields = true;
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			if(localStorage.getItem('setOldUI') == "false") {
				App.rSideBar.currentView.ui.profileTab.find('[data-tab="edit-basic"]').addClass('selected');
			}
			if(!_.isUndefined(this.model.get('userSource')) && this.model.get('userSource') == XAEnums.UserSource.XA_USER.value){
				this.$('[data-tab="edit-password"]').hide();
				this.$('[data-tab="edit-basic"]').hide();
				this.ui.saveBtn.hide();
			}
			this.initializePlugins();
			this.renderForm();
		},
		renderForm : function(){
			this.form = new UserProfileForm({
				template :require('hbs!tmpl/user/UserProfileForm_tmpl'),
				model : this.model,
				showBasicFields : this.showBasicFields
			});
			this.rForm.show(this.form);
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		onTabChange : function(e){
			this.showBasicFields = $(e.currentTarget).parent().data('tab') == 'edit-basic' ? true : false;
			this.renderForm();
			this.clearPasswordFields();
		},
		onSave : function(){
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				return;
			}
			XAUtil.blockUI();
			this.form.afterCommit();
			if(this.showBasicFields){
				this.saveUserDetail();
			} else {
				this.savePasswordDetail();
			}
		},
		saveUserDetail : function(){
			this.model.saveUserProfile(this.model,{
				wait: true,
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.notifySuccess('Success', "User profile updated successfully !!");
					SessionMgr.updateUserProfile();
					App.appRouter.navigate("#!/policymanager/resource",{trigger: true});
					Communicator.vent.trigger('ProfileBar:rerender');
				},
				error: function (model, response, options) {
					XAUtil.blockUI('unblock');
					if(model.responseJSON != undefined && _.isArray(model.responseJSON.messageList)){
						if(model.responseJSON.messageList[0].name == "INVALID_INPUT_DATA"){
							if (model.responseJSON.msgDesc == "Validation failure"){
								XAUtil.notifyError('Error', "Please try different name.");
								return;
							} else {
								XAUtil.notifyError('Error', model.responseJSON.msgDesc);
								return;
							}
						}
					}
					if ( response && response.responseJSON && response.responseJSON.msgDesc){
						that.form.fields.name.setError(response.responseJSON.msgDesc);
						XAUtil.notifyError('Error', response.responseJSON.msgDesc);
					}else {
                                                if(model.status == 419){
                                                        XAUtil.defaultErrorHandler(response , model);
                                                }else{
                                                        XAUtil.notifyError('Error', 'Error occurred while updating user profile!!');
                                                }
					}
				}
			});
		},
		savePasswordDetail : function(){
			var that = this;
			var vPasswordChange = new VPasswordChange();
			vPasswordChange.set({
				loginId : this.model.get('loginId'),
				emailAddress :this.model.get('emailAddress'), 
				oldPassword : this.model.get('oldPassword'),
				updPassword : this.model.get('newPassword')
			});
			this.model.changePassword(this.model.get('id'),vPasswordChange,{
				wait: true,
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.notifySuccess('Success', "User profile updated successfully !!");
					App.appRouter.navigate("#!/policymanager/resource",{trigger: true});
					that.clearPasswordFields();
				},
				error: function (msResponse, options) {
					XAUtil.blockUI('unblock');
					XAUtil.notifyError('Error', 'Error occured while updating user profile');
					if(localization.tt(msResponse.responseJSON.msgDesc) == "Invalid new password"){
						that.form.fields.newPassword.setError(localization.tt('validationMessages.newPasswordError'));
						that.form.fields.reEnterPassword.setError(localization.tt('validationMessages.newPasswordError'));
					}else if((msResponse.responseJSON.msgDesc) == "serverMsg.userMgrOldPassword"){
						that.form.fields.newPassword.setError(localization.tt('validationMessages.oldPasswordRepeatError'));
                                        }else if(msResponse.status == 419){
                                                XAUtil.defaultErrorHandler(options , msResponse);
					} else {
						that.form.fields.oldPassword.setError(localization.tt('validationMessages.oldPasswordError'));
					}
				}
			});
		}, 
		clearPasswordFields : function(){
			this.form.fields.oldPassword.setValue('');
			this.form.fields.newPassword.setValue('');
			this.form.fields.reEnterPassword.setValue('');
		},
		onCancel : function(){
			window.history.back();
		},
		/** on close */
		onClose: function(){
			if(localStorage.getItem('setOldUI') == "false") {
				App.rSideBar.currentView.ui.profileTab.find('.selected').removeClass('selected');
			}
		}

	});

	return UserProfile; 
});
