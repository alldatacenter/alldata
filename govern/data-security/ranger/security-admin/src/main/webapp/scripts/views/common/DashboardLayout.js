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
	var XALinks 			= require('modules/XALinks');
	var DashboardlayoutTmpl = require('hbs!tmpl/dashboard/DashboardLayout_tmpl');

	var DashboardLayout = Backbone.Marionette.Layout.extend(
	/** @lends DashboardLayout */
	{
		_viewName : name,
		
    	template: DashboardlayoutTmpl,
    	breadCrumbs : [XALinks.get('Dashboard')],
        
		/** Layout sub regions */
    	regions: {},

    	/** ui selector cache */
    	ui: {},

		/** ui events hash */
		events: {},

    	/**
		* intialize a new DashboardLayout Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a DashboardLayout Layout");

			_.extend(this, _.pick(options, ''));
			
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
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		/** on close */
		onClose: function(){
		}

	});
	
	return DashboardLayout;
});
