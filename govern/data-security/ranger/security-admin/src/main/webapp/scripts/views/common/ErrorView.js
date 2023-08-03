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
	var localization	= require('utils/XALangSupport');
	
	var ErrorView_tmpl = require('hbs!tmpl/common/ErrorView_tmpl'); 
	
	var ErrorView = Backbone.Marionette.ItemView.extend(
	/** @lends ErrorView */
	{
		_viewName : ErrorView,
		
		template: ErrorView_tmpl,

		templateHelpers :function(){ },

		/** ui selector cache */
		ui: {
			'goBackBtn' : 'a[data-id="goBack"]',
			'home' 		: 'a[data-id="home"]',
			'msg'		: '[data-id="msg"]',
			'moreInfo'		: '[data-id="moreInfo"]',
		},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click ' + this.ui.goBackBtn]  = 'goBackClick';
			return events;
		},

		/**
		* intialize a new ErrorView ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a ErrorView ItemView");

			_.extend(this, _.pick(options, 'status'));

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
			$('#r_breadcrumbs').hide();
			$('.latestResponse').hide();
			var msg = '', moreInfo = '';
			if(this.status == 401){
				msg = 'Access Denied (401)';
				moreInfo =  localization.tt("msg.accessDenied");
			} else if(this.status == 204){
				msg = 'No Content (204)'
				moreInfo = localization.tt("msg.noContent");
			} else if(this.status == "checkSSOTrue"){
				msg = 'Sign Out Is Not Complete!'
				moreInfo = localization.tt("msg.signOutIsNotComplete");
			} else {
				msg = 'Page not found (404).'
				moreInfo = localization.tt("msg.pageNotFound");
			}
			this.ui.msg.html(msg);
			this.ui.moreInfo.html(moreInfo);
			if(this.status == 204){
				this.ui.goBackBtn.hide();
				this.ui.home.hide();
			}
			if(this.status == "checkSSOTrue") {
				this.ui.home.hide();
			}
		},
		goBackClick : function(){
			history.back();
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		/** on close */
		onClose: function(){
			$('#r_breadcrumbs').show();
			$('.latestResponse').show();
		}

	});

	return ErrorView;
});
