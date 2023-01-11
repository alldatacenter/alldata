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
	var App				= require('App');
	var XALinks 		= require('modules/XALinks');
	var XAUtil			= require('utils/XAUtils');
	var XAEnums			= require('utils/XAEnums');
	var localization	= require('utils/XALangSupport');
	
	var UserForm		= require('views/users/UserForm');
	var UserTableLayout	= require('views/users/UserTableLayout');
	var VXUserList		= require('collections/VXUserList');
	var UserCreateTmpl  = require('hbs!tmpl/users/UserCreate_tmpl');
        var SessionMgr		= require('mgrs/SessionMgr');

	var UserCreate = Backbone.Marionette.Layout.extend(
	/** @lends UserCreate */
	{
		_viewName : 'UserCreate',
		
    	template: UserCreateTmpl,
    	breadCrumbs :function(){
    		return this.model.isNew() ? [XALinks.get('Users'),XALinks.get('UserCreate')]
    					: [XALinks.get('Users'),XALinks.get('UserEdit')];
    	},
        
		/** Layout sub regions */
    	regions: {
    		'rForm' :'div[data-id="r_form"]'
    	},

    	/** ui selector cache */
    	ui: {
    		'tab' 		: '.nav-tabs',
    		'btnSave'	: '[data-id="save"]',
    		'btnCancel' : '[data-id="cancel"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click '+this.ui.tab+' li a']  = 'onTabChange';
			events['click ' + this.ui.btnSave]		= 'onSave';
			events['click ' + this.ui.btnCancel]	= 'onCancel';
			
			return events;
		},

    	/**
		* intialize a new UserCreate Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a UserCreate Layout");

			_.extend(this, _.pick(options));
			this.showBasicFields = true;
			this.editUser = this.model.has('id') ? true : false;
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			var that = this;
			this.initializePlugins();
			if(this.model.isNew()){
				this.$('[data-tab="edit-password"]').hide();
				this.$('[data-tab="edit-basic"]').hide();
			}
			if(!_.isUndefined(this.model.get('userSource')) && this.model.get('userSource') == XAEnums.UserSource.XA_USER.value){
				this.$('[data-tab="edit-password"]').hide();
				this.$('[data-tab="edit-basic"]').hide();
			}
			this.renderForm();
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavUserForm'),this.rForm.$el);
                if(XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                    this.ui.btnSave.attr("disabled", true);
                }
                },
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		onTabChange : function(e){
			this.showBasicFields = $(e.currentTarget).parent().data('tab') == 'edit-basic' ? true : false;
			this.renderForm();
			this.clearPasswordFields();
		},
		renderForm : function(){
			var that = this;
			this.form = new UserForm({
				template : require('hbs!tmpl/users/UserForm_tmpl'),
				model : this.model,
				showBasicFields : this.showBasicFields
			});
			this.rForm.show(this.form);
			if(!this.showBasicFields){
				this.form.fields.passwordConfirm.editor.$el.on('keypress',function(e){
					if(e.which === 13){
						that.onSave();
					}
				});
			}
		},

		clearPasswordFields : function(){
			this.form.fields.password.setValue('');
			this.form.fields.passwordConfirm.setValue('');
		},
		onSave: function(){
			var self= this;
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				if(this.showBasicFields)
					this.form.beforeSaveUserDetail();
				return;
			}
			this.showBasicFields ? this.saveUserDetail() : this.savePasswordDetail();
			
		},
		saveUserDetail : function(){
			var that = this;
			if(!this.form.beforeSaveUserDetail()){
				return;
			}
			XAUtil.blockUI();
			this.model.save({},{
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.allowNavigation();
					var msg = that.editUser ? localization.tt('msg.userUpdatedSucc') : localization.tt('msg.userCreatedSucc');
					XAUtil.notifySuccess('Success', msg);
					if(that.editUser){
						App.appRouter.navigate("#!/users/usertab",{trigger: true});
						return;
					}
					App.usersGroupsListing = {'showLastPage' : true}
					App.appRouter.navigate("#!/users/usertab",{trigger: true});
				},
				error : function(model,resp){
					XAUtil.blockUI('unblock');
					if(!_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
						if(resp.responseJSON.msgDesc == "XUser already exists"){
							XAUtil.notifyError('Error',"User already exists.");
						} else {
							XAUtil.notifyError('Error',resp.responseJSON.msgDesc);
						}
					}else {
						XAUtil.notifyError('Error', "Error occurred while creating/updating user.");
					}
				}
			});
		},
		savePasswordDetail : function(){
			this.form.beforeSavePasswordDetail();
			XAUtil.blockUI();
			this.model.save({},{
				
				success: function () {
					XAUtil.blockUI('unblock');
					var msg = 'Password updated successfully';// :'Password created successfully';
					XAUtil.notifySuccess('Success', msg);
					App.appRouter.navigate("#!/users/usertab",{trigger: true});
				},
				error : function(model,resp){
					XAUtil.blockUI('unblock');
					if(!_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
						if(resp.responseJSON.msgDesc == "serverMsg.userMgrNewPassword"){
							XAUtil.notifyError('Error','Invalid new password');
						} else {
							XAUtil.notifyError('Error',resp.responseJSON.msgDesc);
						}
					} else {
						XAUtil.notifyError('Error', "Error occurred while creating/updating user.");
					}
				}
			});
		},
		onCancel : function(){
			XAUtil.allowNavigation();
			App.appRouter.navigate("#!/users/usertab",{trigger: true});

		},
		/** on close */
		onClose: function(){
		}
	});
	return UserCreate; 
});
