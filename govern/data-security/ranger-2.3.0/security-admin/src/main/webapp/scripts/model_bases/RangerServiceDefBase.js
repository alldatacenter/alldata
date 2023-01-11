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

	var RangerServiceDefBase = XABaseModel.extend(
	/** @lends RangerServiceDefBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'plugins/definitions',
		
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
			},
			"configs" : {
				"dataType" : "string"
			},
			"resources" : {
				"dataType" : "string"
			},
			"accessTypes" : {
				"dataType" : "string"
			},
			"policyConditions" : {
				"dataType" : "string"
			},
			"enums" : {
				"dataType" : "string"
			}
		},
		
		
		idAttribute: 'id',

		/**
		 * RangerServiceDefBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'RangerServiceDefBase';
			//this.bind("error", XAUtils.defaultErrorHandler);
			this.bindErrorEvents();
		}

	}, {
		// static class members
	});

    return RangerServiceDefBase;
	
});


