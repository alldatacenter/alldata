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
	
	var KmsKeyCreateTmpl= require('hbs!tmpl/kms/KmsKeyCreate_tmpl');
	var RKmsKeyForm 	= require('views/kms/KmsKeyForm');

	var KmsKeyCreate = Backbone.Marionette.Layout.extend(
	/** @lends KmsKeyCreate */
	{
		_viewName : 'KmsKeyCreate',
		
    	template : KmsKeyCreateTmpl,
    	templateHelpers : function(){
    		return {
    			editPolicy : this.editPolicy
    		};
    	},
    	breadCrumbs :function(){
    		var opts = { 'kmsService' : this.kmsService, 'kmsServiceDefModel' : this.kmsServiceDefModel }
    		if(this.model.isNew()){
    			return [XALinks.get('KmsManage',opts), XALinks.get('KmsServiceForKey', opts), XALinks.get('KmsKeyCreate')];
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
		* intialize a new KmsKeyCreate Layout 
		* @constructs
		*/
		initialize: function(options) {
			var that = this;
			console.log("initialized a KmsKeyCreate Layout");

			_.extend(this, _.pick(options,'kmsServiceName'));
			this.getKmsInfoFromServiceName();
			that.form = new RKmsKeyForm({
				template : require('hbs!tmpl/kms/KmsKeyForm_tmpl'),
				model : this.model,
			});

			this.editPolicy = this.model.has('id') ? true : false;
			this.bindEvents();
			this.params = {};
		},
		
		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			this.rForm.show(this.form);
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.rForm.$el);
		},
		onSave: function(){
			var that =this ;
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors))	return;
			var options = {
				url : this.model.urlRoot+"?provider="+ this.kmsServiceName,
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.allowNavigation();
					var msg = that.editGroup ? 'Key updated successfully' :'Key created successfully';
					XAUtil.notifySuccess('Success', msg);
					App.appRouter.navigate("#!/kms/keys/edit/manage/"+that.kmsServiceName,{trigger: true});
				},
				error : function (model, resp, options) {
					XAUtil.blockUI('unblock');
					var errorMsg = 'Error creating Key!';
					if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) 
							&& !_.isUndefined(resp.responseJSON.msgDesc)){
						errorMsg = resp.responseJSON.msgDesc;
					}
					XAUtil.notifyError('Error', errorMsg);
				}
			}
			
			//to check model is new or not
			options.type = (this.model.has('versions')) ? 'PUT'  : 'POST';
			this.form.beforeSave();
			XAUtil.blockUI();
			this.model.save({},options);
		},
		onCancel : function(){
			XAUtil.allowNavigation();
			App.appRouter.navigate("#!/kms/keys/edit/manage/"+this.kmsServiceName,{trigger: true});
		},
		onDelete :function(){
			var that = this;
			var url = this.model.urlRoot+"?provider="+ this.kmsServiceName;
			XAUtil.confirmPopup({
				msg :'Are you sure want to delete ?',
				callback : function(){
					XAUtil.blockUI();
					that.model.destroy({
						url : url,
						success: function(model, response) {
							XAUtil.blockUI('unblock');
							XAUtil.allowNavigation();
							XAUtil.notifySuccess('Success', localization.tt('msg.keyDeleteMsg'));
							App.appRouter.navigate("#!/kms/keys/edit/manage/"+that.kmsServiceName,{trigger: true});
						},
						error: function (model, response, options) {
							XAUtil.blockUI('unblock');
							XAUtil.notifyError('Error', 'Error deleting key!');
						}
					});
				}
			});
		},
		getKmsInfoFromServiceName : function() {
			var KmsServiceDef = require('models/RangerServiceDef');
			var KmsService = require('models/RangerService');
			this.kmsService = new KmsService();
			this.kmsService.url = XAUtil.getRangerServiceByName(this.kmsServiceName);
			this.kmsService.fetch({ cache : false, async : false });
 			this.kmsServiceDefModel	= new KmsServiceDef();
			this.kmsServiceDefModel.url = XAUtil.getRangerServiceDef(this.kmsService.get('type'));
			this.kmsServiceDefModel.fetch({ cache : false, async : false });
		},
		/** on close */
		onClose: function(){
			XAUtil.allowNavigation();
		}

	});

	return KmsKeyCreate;
});
