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

	var VXModuleDefBase	= require('model_bases/VXModuleDefBase');
	var XAUtils		= require('utils/XAUtils');
	var XAEnums		= require('utils/XAEnums');
	var localization= require('utils/XALangSupport');

	var VXModuleDef = VXModuleDefBase.extend(
	/** @lends VXModuleDefBase.prototype */
	{
		/**
		 * VXModuleDefBase initialize method
		 * @augments VXModuleDefBase
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXModuleDef';
			this.bindErrorEvents();
		},
		/**
		 * @function schema
		 * This method is meant to be used by UI,
		 * by default we will remove the unrequired attributes from serverSchema
		 */

		schemaBase : function(){
			var attrs = _.omit(this.serverSchema, 'id', 'createDate', 'updateDate', "version",
					"permList", "status", "updatedBy", "isSystem");

			_.each(attrs, function(o){
				o.type = 'Hidden';
			});

			// Overwrite your schema definition here
			return _.extend(attrs,{});
		},

		/** This models toString() */
		toString : function(){
			return this.get('name');
		}

	}, {
		// static class members
	});

    return VXModuleDef;

});
