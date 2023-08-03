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
 *
 */
define(function(require) {
    'use strict';
    
	var Backbone		= require('backbone');
    var App		        = require('App');
	var XAEnums			= require('utils/XAEnums');
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');
	var VXGroup			= require('models/VXGroup');
	var VXGroupList			= require('collections/VXGroupList');
	var VXUserList			= require('collections/VXUserList');
	require('bootstrap-editable');
	    	
	var ConfigItem = Backbone.Marionette.ItemView.extend({
		_msvName : 'FormInputItem',
		template : require('hbs!tmpl/service/ConfigurationItem_tmpl'),
		tagName : 'tr',
		templateHelpers : function(){
		},
		ui : {
			'name' : '[data-js="name"]',
			'value': '[data-js="value"]'
		},
		events : {
			'click [data-action="delete"]'	: 'evDelete',
			'change [data-js="name"]'		: 'onInputNameChange',
			'change [data-js="value"]'		: 'onInputValueChange'
			
		},

		initialize : function(options) {
			_.extend(this, _.pick(options,''));
		},
 
		onRender : function() {
		},
		onInputNameChange : function(e) {
			this.model.set('name', $(e.currentTarget).val().trim());
			this.ui.name.val($(e.currentTarget).val().trim());
		},
		onInputValueChange : function(e) {
			this.model.set('value', $(e.currentTarget).val().trim());
			this.ui.value.val($(e.currentTarget).val().trim());
		},
		evDelete : function(){
			var that = this;
			this.collection.remove(this.model);
		},
	});

	var ConfigurationList =  Backbone.Marionette.CompositeView.extend({
		_msvName : 'ConfigurationList',
		template : require('hbs!tmpl/service/ConfigurationList_tmpl'),
		templateHelpers :function(){
			return { 'fieldLabel' : this.fieldLabel }; 
		},
		getItemView : function(item){
			if(!item){
				return;
			}
			return ConfigItem;
		},
		itemViewContainer : ".js-formInput",
		itemViewOptions : function() {
			return {
				'collection' : this.collection,
			};
		},
		events : {
			'click [data-action="addGroup"]' : 'addNew'
		},
		initialize : function(options) {
			_.extend(this, _.pick(options, 'fieldLabel'));
			if(this.collection.length == 0){
				this.collection.add(new Backbone.Model());
			}
		},
		onRender : function(){
		},
		addNew : function(){
			var that =this;
			this.$('table').show();
			this.collection.add(new Backbone.Model());
		},
	});
	
	return ConfigurationList;

});
