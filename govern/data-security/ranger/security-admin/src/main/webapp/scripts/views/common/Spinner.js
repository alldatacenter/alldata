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
	var Handlebars		= require('handlebars');
	var Communicator	= require('communicator');

	
	var Spinner = Backbone.Marionette.Layout.extend(
	/** @lends Spinner */
	{
		_viewName : 'Spinner',
		
		template  : Handlebars.compile("<span></span>"),

		tagName	  : 'span',
        
    	/** ui selector cache */
    	ui: {},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			return events;
		},

    	/**
		* intialize a new Spinner Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a Spinner ItemView");

			_.extend(this, _.pick(options, 'collection'));

			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
            this.listenTo(this.collection, 'request', this.displaySpinner);
            this.listenTo(this.collection, 'sync error', this.removeSpinner);
		},

		/** on render callback */
		onRender: function() {
			this.initializePlugins();
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		displaySpinner: function () {
            this.$el.empty().append('<i class="fa-fw fa fa-spinner fa-fw fa fa-spin fa-fw fa fa-3x white spin-position"></i>');
        },

        removeSpinner: function () {
            this.$el.empty();
        },
		
		/** on close */
		onClose: function(){
		}

	});

	return Spinner;
});
