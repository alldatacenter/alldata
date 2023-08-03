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
	var XAViewUtils		= require('utils/XAViewUtils');
	
	require('backbone-forms');
	require('backbone-forms.templates');
	var GroupForm = Backbone.Form.extend(
	/** @lends GroupForm */
	{
		_viewName : 'GroupForm',

				/**Form template**/

		templateData : function(){
			return {syncSourceInfo : this.syncData}
		},

    	/**
		* intialize a new GroupForm Form View 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a GroupForm Form View");
			_.extend(this, _.pick(options,''));
    		Backbone.Form.prototype.initialize.call(this, options);
			if (this.model && !_.isUndefined(this.model.get('otherAttributes'))) {
				this.syncData = XAViewUtils.syncUsersGroupsDetails(this);
			}

			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
		},

		/** fields for the form
		*/
		fields: ['name', 'description'],
		schema :{},
		/** on render callback */
		render: function(options) {
			Backbone.Form.prototype.render.call(this, options);
			this.initializePlugins();
			if(!this.model.isNew()){
				if(!_.isUndefined(this.model.get('groupSource')) && this.model.get('groupSource') == XAEnums.GroupSource.XA_GROUP.value){
					this.fields.name.editor.$el.find('input').attr('disabled',true);
					this.fields.description.editor.$el.attr('disabled',true);
				}
			}
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		}
		
	});

	return GroupForm;
});
