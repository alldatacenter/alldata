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

	var ModulePermissionForm	= require('views/permissions/ModulePermissionForm');
	var ModulePermsTableLayout	= require('views/permissions/ModulePermsTableLayout');
	var VXModuleDefList			= require('collections/VXModuleDefList');
	var ModulePermissionCreateTmpl  = require('hbs!tmpl/permissions/ModulePermissionCreate_tmpl');

	var ModulePermissionCreate = Backbone.Marionette.Layout.extend(
	/** @lends ModulePermissionCreate */
	{
		_viewName : 'ModulePermissionCreate',

		template: ModulePermissionCreateTmpl,
		breadCrumbs :function(){
			return this.model.isNew() ? [XALinks.get('ModulePermissions')] 
					: [XALinks.get('ModulePermissions'),XALinks.get('ModulePermissionEdit',this.model)];
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
			events['click ' + this.ui.btnSave]		= 'onSave';
			events['click ' + this.ui.btnCancel]	= 'onCancel';

			return events;
		},

	/**
		* intialize a new ModulePermissionCreate Layout
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a ModulePermissionCreate Layout");

                _.extend(this, _.pick(options, 'urlQueryParams'));
			this.editMode = this.model.has('id') ? true : false;
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
		},

		/** on render callback */
		onRender: function() {
			var that = this;
			this.renderForm();
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavUserForm'),this.rForm.$el);
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		renderForm : function(){
		   this.form = new ModulePermissionForm({
			   template  : require('hbs!tmpl/permissions/ModulePermissionForm_tmpl'),
                           model 	  : this.model,
		   });
		   this.rForm.show(this.form);
		},
		onSave: function(){
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				this.form.beforeSaveModulePermissions();
			}
			this.saveModulePermissions();

		},
		saveModulePermissions : function(){
			var that = this;
			if(!this.form.beforeSaveModulePermissions()){
				return;
			}
			XAUtil.blockUI();
			this.model.save({},{
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.allowNavigation();
					var msg = that.editMode ? 'Module Permissions updated successfully' :'Module Permissions created successfully';
					XAUtil.notifySuccess('Success', msg);
                                        App.appRouter.navigate("#!/permissions/models",{trigger: true});
				},
				error : function(model,resp){
					XAUtil.blockUI('unblock');
					if(!_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
						XAUtil.notifyError('Error',resp.responseJSON.msgDesc);
					} else {
						XAUtil.notifyError('Error', "Error occurred while creating/updating module permissions.");
					}
				}
			});
		},
		onCancel : function(){
			XAUtil.allowNavigation();
                        App.appRouter.navigate("#!/permissions/models",{trigger: true});

		},
		/** on close */
		onClose: function(){
                    XAUtil.removeUnwantedDomElement();
		}

	});

	return ModulePermissionCreate;
});
