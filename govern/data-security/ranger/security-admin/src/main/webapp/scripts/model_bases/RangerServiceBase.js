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

	var XABaseModel	= require('models/XABaseModel');
	var XAGlobals	= require('utils/XAGlobals');

	var RangerServiceBase = XABaseModel.extend(
	/** @lends RangerServiceBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'plugins/services',
		
		defaults: {},

		serverSchema : {
			"id" : {
				"dataType" : "Long"
			},
			"guid" : {
				"dataType" : "String"
			},
			"version" : {
				"dataType" : "int"
			},
			"createTime" : {
				"dataType" : "Date"
			},
			"updateTime" : {
				"dataType" : "Date"
			},
			"permList" : {
				"dataType" : "list",
				"listType" : "VNameValue"
			},
			"status" : {
				"dataType" : "int"
			},
			"name" : {
				"dataType" : "String"
			},
			"label" : {
				"dataType" : "String"
			},
			"description" : {
				"dataType" : "String"
			}
		},
		
		
		idAttribute: 'id',

		/**
		 * RangerServiceBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'RangerServiceBase';
			//this.bind("error", XAUtils.defaultErrorHandler);
			this.bindErrorEvents();
		},
		testConfig : function(vRangerService, options){
			var url = this.urlRoot  + '/validateConfig';
			
			options = _.extend({
				data : JSON.stringify(vRangerService),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'POST', options);
		},

	}, {
		// static class members
	});

    return RangerServiceBase;
	
});


