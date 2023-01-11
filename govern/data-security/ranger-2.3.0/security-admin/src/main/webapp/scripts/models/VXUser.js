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

	var VXUserBase	= require('model_bases/VXUserBase');
	var XAEnums     = require('utils/XAEnums');

	var VXUser = VXUserBase.extend(
	/** @lends VXUser.prototype */
	{
		/**
		 * VXUserBase initialize method
		 * @augments FSBaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXUser';
			var selectable = new Backbone.Picky.Selectable(this);
			_.extend(this, selectable);
			this.bindErrorEvents();
			this.toView();
			this.toViewStatus();
		},

		toView : function(){
			if(!_.isUndefined(this.get('isVisible'))){
				var visible = (this.get('isVisible') == XAEnums.VisibilityStatus.STATUS_VISIBLE.value);
				this.set('isVisible', visible);
			}
		},

		toServer : function(){
			var visible = this.get('isVisible') ? XAEnums.VisibilityStatus.STATUS_VISIBLE.value : XAEnums.VisibilityStatus.STATUS_HIDDEN.value;
			this.set('isVisible', visible);
		},
		
		toViewStatus : function(){
			if(!_.isUndefined(this.get('status'))){
				var status = (this.get('status') == XAEnums.ActiveStatus.STATUS_ENABLED.value);
				this.set('status', status);
			}
		},

		toServerStatus : function(){
			var status = this.get('status') ? XAEnums.ActiveStatus.STATUS_ENABLED.value : XAEnums.ActiveStatus.STATUS_DISABLED.value;
			this.set('status', status);
		},

		/** This models toString() */
		toString : function(){
			return this.get('name');
		}

	}, {
		// static class members
	});

    return VXUser;
	
});


