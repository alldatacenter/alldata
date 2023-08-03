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

 
/**
 *
 * Base Model file from which all models will extend/derive.
 */

define(function(require){
	'use strict';

	var Backbone = require('backbone');
	
	var XABaseModel = Backbone.Model.extend(
	/** @lends XABaseModel.prototype */
	{
		/**
		 * XABaseModel's initialize function
		 * @augments Backbone.Model
		 * @constructs
		 */
		initialize : function() {
			
		},
		bindErrorEvents :function(){
			//Moved require inside fuctn expression due to ie issue
			this.bind("error", function(e, error){
				var XAUtils = require('utils/XAUtils');
				XAUtils.defaultErrorHandler(undefined, error, e);
			});
		},
		/**
		 * toString for a model. Every model should implement this function.
		 */
		toString : function() {
			throw new Error('ERROR: toString() not defined for ' + this.modelName);
		},

		/**
		 * Silent'ly set the attributes. ( do not trigger events )
		 */
		silent_set: function(attrs) {
			return this.set(attrs, {
				silent: true
			});
		},
		parse: function(response){
			for(var key in this.modelRel)
				{
					var embeddedClass = this.modelRel[key];
					var embeddedData = response[key];
					response[key] = new embeddedClass(embeddedData);
				}
				return response;
		}

	}, {
		nestCollection : function(model, attributeName, nestedCollection) {
			//setup nested references
			for (var i = 0; i < nestedCollection.length; i++) {
				model.attributes[attributeName][i] = nestedCollection.at(i).attributes;
			}
			//create empty arrays if none

			nestedCollection.bind('add', function (initiative) {
				if (!model.get(attributeName)) {
					model.attributes[attributeName] = [];
				}
				model.get(attributeName).push(initiative.attributes);
			});

			nestedCollection.bind('remove', function (initiative) {
				var updateObj = {};
				updateObj[attributeName] = _.without(model.get(attributeName), initiative.attributes);
				model.set(updateObj);
			});

			model.parse = function(response) {
				if (response && response[attributeName]) {
					model[attributeName].reset(response[attributeName]);
				}
				return Backbone.Model.prototype.parse.call(model, response);
			}
			return nestedCollection;
		},

		nonCrudOperation : function(url, requestMethod, options){
			return Backbone.sync.call(this, null, this, _.extend({
				url: url,
				type: requestMethod
			}, options));
		}
	});

	return XABaseModel;
});
