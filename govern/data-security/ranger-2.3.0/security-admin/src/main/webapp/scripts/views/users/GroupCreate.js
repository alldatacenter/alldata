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
	
	var UserTableLayout	= require('views/users/UserTableLayout');
	var VXGroupList		= require('collections/VXGroupList');
	var GroupForm		= require('views/users/GroupForm');
	var GroupcreateTmpl = require('hbs!tmpl/users/GroupCreate_tmpl');
        var SessionMgr      = require('mgrs/SessionMgr');

	var GroupCreate = Backbone.Marionette.Layout.extend(
	/** @lends GroupCreate */
	{
		_viewName : 'GroupCreate',
		
    	template: GroupcreateTmpl,
    	breadCrumbs :function(){
    		return this.model.isNew() ? [XALinks.get('Groups'),XALinks.get('GroupCreate')]
    							: [XALinks.get('Groups'),XALinks.get('GroupEdit')];
    	},
        
		/** Layout sub regions */
    	regions: {
    		'rForm' :'div[data-id="r_form"]'
    	},

    	/** ui selector cache */
    	ui: {
    		'btnSave'	: '[data-id="save"]',
    		'btnCancel' : '[data-id="cancel"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click ' + this.ui.btnSave]		= 'onSave';
			events['click ' + this.ui.btnCancel]	= 'onCancel';
			return events;
		},

    	/**
		* intialize a new GroupCreate Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a GroupCreate Layout");

			_.extend(this, _.pick(options, ''));
			this.editGroup = this.model.has('id') ? true : false;
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			var that = this
			this.initializePlugins();
			this.form = new GroupForm({
				template : require('hbs!tmpl/users/GroupForm_tmpl'),
				model : this.model
			});
			this.rForm.show(this.form);
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavGroupForm'),this.rForm.$el);
                        if((!_.isUndefined(this.model.get('groupSource')) && this.model.get('groupSource') == XAEnums.GroupSource.XA_GROUP.value)
                                || XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                                this.ui.btnSave.prop( "disabled", true );
			}
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		onSave: function(){
			var that = this ;
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				return;
			}
			XAUtil.blockUI();
			
			this.model.save({},{
				success: function () {
					XAUtil.blockUI('unblock');
					XAUtil.allowNavigation();
					Backbone.fetchCache._cache = {}
					var msg = that.editGroup ? 'Group updated successfully' :'Group created successfully';
					XAUtil.notifySuccess('Success', msg);
					if(that.editGroup){
						var VXResourceList 	= require('collections/VXResourceList');
						var resourceList = new VXResourceList();
						_.each(Backbone.fetchCache._cache, function(url, val){
							var urlStr= resourceList.url;
							if((val.indexOf(urlStr) != -1)) 
								Backbone.fetchCache.clearItem(val);
						});
						App.appRouter.navigate("#!/users/grouptab",{trigger: true});
						return;
					}
					App.usersGroupsListing = {'showLastPage' : true}
					App.appRouter.navigate("#!/users/grouptab",{trigger: true});
				},
				error : function (model, response, options) {
					XAUtil.blockUI('unblock');
					if ( response && response.responseJSON && response.responseJSON.msgDesc){
						if(response.responseJSON.msgDesc == "XGroup already exists"){
							XAUtil.notifyError('Error', "Group name already exists");
						} else {
							XAUtil.notifyError('Error', response.responseJSON.msgDesc);
						}
					}else {
						XAUtil.notifyError('Error', 'Error occurred while creating/updating group!');
					}
				}
			});
		},
		onCancel : function(){
			XAUtil.allowNavigation();
			App.appRouter.navigate("#!/users/grouptab",{trigger: true});
		},

		/** on close */
		onClose: function(){
		}

	});

	return GroupCreate; 
});