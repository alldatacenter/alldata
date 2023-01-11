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
	var XAEnums			= require('utils/XAEnums');
	var localization	= require('utils/XALangSupport');
	var KeyValuePairList= require('views/service/ConfigurationList')
	
	require('backbone-forms');
	require('backbone-forms.templates');
	var KmsKeyForm = Backbone.Form.extend(
	/** @lends KmsKeyForm */
	{
		_viewName : 'KmsKeyForm',

    	/**
		* intialize a new KmsKeyForm Form View 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a KmsKeyForm Form View");
			_.extend(this, _.pick(options,''));
			this.attributesColl = new Backbone.Collection();
			this.setupFormForEditMode();
			Backbone.Form.prototype.initialize.call(this, options);
			this.bindEvents();
		},
		/** all events binding here */
		bindEvents : function(){
		},
		schema : function(){
			return {
				name : {
					type		: 'Text',
					title		: localization.tt("lbl.keyName") +' *',
					validators  : ['required'],
				},
				cipher : {
					type		: 'Text',
					title		: localization.tt("lbl.cipher"),
				},
				length : {
					type		: 'Number',
					title		: localization.tt("lbl.length"),
					validators	: [{type : 'regexp', regexp : /^\d+$/, message : 'Please enter valid integer value.'}],
				},
				/*material : {
					type		: 'Text',
					title		: localization.tt("lbl.material"),
				},*/
				description : {
					type		: 'TextArea',
					title		: localization.tt("lbl.description"),
				}
			};
		},	
		/** on render callback */
		render: function(options) {
			Backbone.Form.prototype.render.call(this, options);
			this.initializePlugins();
			this.renderCustomFields();
		},
		/** all custom field rendering */
		renderCustomFields: function(){
			this.$('.attributes').html(new KeyValuePairList({
				collection : this.attributesColl,
				model 	   : this.model,
				fieldLabel : localization.tt("lbl.attributes"),
			}).render().el);
		},
		setupFormForEditMode : function() {
			if(!this.model.isNew() && !_.isUndefined(this.model.get('attributes'))){
				_.map(this.model.get('attributes'), function(value, key) { this.attributesColl.add({'name' : key, 'value' : value}) }, this)
			}
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		beforeSave : function(){
			var attributes = {};
			this.attributesColl.each(function(obj){ attributes[obj.get('name')] = obj.get('value'); })
			this.model.set('attributes',attributes);
		}
		
	});

	return KmsKeyForm;
});
