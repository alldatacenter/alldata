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
	var XAEnums 		= require('utils/XAEnums');
	var UserInfo_tmpl = require('hbs!tmpl/users/UserInfo_tmpl'); 
	
	var UserInfo = Backbone.Marionette.ItemView.extend(
	/** @lends UserInfo */
	{
		_viewName : 'UserInfo',
		
    	template: UserInfo_tmpl,
    	templateHelpers : function(){
    		return {
    			'groupList' : this.groupList,
    			'userList'	: this.userList,
    			'userModel'	: this.model.modelName == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.modelName ? true : false,
    			'groupModel': this.model.modelName == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.modelName ? true : false
    		};
    	},
        
    	/** ui selector cache */
    	ui: {
    		'showMoreBtn' : '[data-id="showMore"]',
    		'showLessBtn' : '[data-id="showLess"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click ' + this.ui.showMoreBtn]  = 'onShowMoreClick';
			events['click ' + this.ui.showLessBtn]  = 'onShowLessClick';
			return events;
		},

    	/**
		* intialize a new UserInfo ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a UserInfo ItemView");

			_.extend(this, _.pick(options, 'groupList','userList'));
			
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			this.initializePlugins();
			if(this.ui.showMoreBtn.parent('td').find('span').length > 3){
				this.ui.showMoreBtn.show();	
				this.ui.showMoreBtn.parent('td').find('span').slice(3).hide();
			}
		},
		onShowMoreClick : function(){
			this.ui.showMoreBtn.parent('td').find('span').show();
			this.ui.showMoreBtn.hide();
			this.ui.showLessBtn.show();
		},
		onShowLessClick : function(){
			this.ui.showMoreBtn.parent('td').find('span').slice(3).hide();
			this.ui.showLessBtn.hide();
			this.ui.showMoreBtn.show();
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		/** on close */
		onClose: function(){
		}

	});

	return UserInfo;
});
