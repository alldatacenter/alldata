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

	var VXAssetBase = XABaseModel.extend(
	/** @lends VXAssetBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'assets/assets',
		
		defaults: {},

		serverSchema : {
			"id" : {
				"dataType" : "Long"
			},
			"version" : {
				"dataType" : "int"
			},
			"createDate" : {
				"dataType" : "Date"
			},
			"updateDate" : {
				"dataType" : "Date"
			},
			"permList" : {
				"dataType" : "list",
				"listType" : "VNameValue"
			},
			"forUserId" : {
				"dataType" : "Long"
			},
			"status" : {
				"dataType" : "int"
			},
			"priGrpId" : {
				"dataType" : "Long"
			},
			"updatedBy" : {
				"dataType" : "String"
			},
			"isSystem" : {
				"dataType" : "boolean"
			},
			"name" : {
				"dataType" : "String"
			},
			"description" : {
				"dataType" : "String"
			},
			"activeStatus" : {
				"dataType" : "int"
			},
			"assetType" : {
				"dataType" : "int"
			},
			"config" : {
				"dataType" : "String"
			},
			"supportNative" : {
				"dataType" : "boolean"
			}
		},
		
		
		idAttribute: 'id',

		/**
		 * VXAssetBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXAssetBase';
			//this.bind("error", XAUtils.defaultErrorHandler);
			this.bindErrorEvents();
		},
		
		testConfig : function(vAssest, options){
			var url = this.urlRoot  + '/testConfig';
			
			options = _.extend({
				data : JSON.stringify(vAssest),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'POST', options);
		},
		testKnoxCofig : function(vAssest, data, options){
			var url = 'service/assets/knox/resources?dataSourceName='+data.dataSourceName;
			
			options = _.extend({
//				data : JSON.stringify(data),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
		},

	}, {
		// static class members
	});

    return VXAssetBase;
	
});


