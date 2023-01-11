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

 
define(function(require) {
	'use strict';

	var Backbone	= require('backbone');
	var XAGlobals	= require('utils/XAGlobals');
	var XAUtils		= require('utils/XAUtils');
	
	require('backbone-pageable');
	var XABaseCollection = Backbone.PageableCollection.extend(
	/** @lends XABaseCollection.prototype */
	{
		/**
		 * XABaseCollection's initialize function
		 * @augments Backbone.PageableCollection
		 * @constructs
		 */

		initialize : function() {
		},
		bindErrorEvents :function(){
			this.bind("error", XAUtils.defaultErrorHandler);
		},
		/**
		 * state required for the PageableCollection 
		 */
		state : {
			firstPage: 0,
                        pageSize : XAGlobals.settings.PAGE_SIZE,
                        order : 1
		},
		mode : 'server',
		/**
		 * queryParams required for the PageableCollection 
		 * Server sends us this :
		 * pageSize: "2"
		 * resultSize: "2"
		 * startIndex: "0"
		 * totalCount: "15"
		 */
		queryParams: {
			pageSize	: 'pageSize',
			sortKey		: 'sortBy',
			order		: 'sortType',
			totalRecords: 'totalCount',
			startIndex : function(){
				return this.state.currentPage * this.state.pageSize;
			}
		},

		/**
		 * override the parseState of PageableCollection for our use
		 */
		parseState: function (resp, queryParams, state, options) {
			if(!this.modelAttrName){
				throw new Error("this.modelAttrName not defined for " + this);
			}
			var serverState = _.omit(resp,this.modelAttrName);
			var newState = _.clone(state);

			_.each(_.pairs(_.omit(queryParams, "directions")), function (kvp) {
				var k = kvp[0], v = kvp[1];
				var serverVal = serverState[v];
				if (!_.isUndefined(serverVal) && !_.isNull(serverVal)){
					if((k == 'pageSize') || (k == 'totalRecords')){
						newState[k] = parseInt(serverState[v],10);
					} else {
						newState[k] = serverState[v];
					}
				}
			});

			if (serverState.sortType) {
				newState.order = _.invert(queryParams.directions)[serverState.sortType] * 1;
			}
			
			var startIndex = parseInt(serverState.startIndex,10);
			var totalCount = parseInt(serverState.totalCount,10);
			var pageSize = parseInt(serverState.pageSize,10);

			newState.pageSize = pageSize ? pageSize : state.pageSize;
			newState.currentPage = startIndex === 0 ? 0 : Math.ceil(startIndex / newState.pageSize);
			newState.startIndex = startIndex;
			//newState.totalPages = totalCount === 0 ? 0 : Math.ceil(totalCount / serverState.pageSize);

			return newState;
		},

		/**
		 * override the parseRecords of PageableCollection for our use
		 */
		parseRecords : function(resp, options){
			if(!this.modelAttrName){
				throw new Error("this.modelAttrName not defined for " + this);
			}
			return resp[this.modelAttrName];
		}

	}, {
		//static functions

		/**
		 * function to get table cols for backgrid, this function assumes that the 
		 * collection has a static tableCols member.
		 */
		getTableCols : function(cols, collection){
			var retCols = _.map(cols, function(v, k, l){
				var defaults = collection.constructor.tableCols[k];
				if(! defaults){
					//console.log("Error!! " + k + " not found in collection: " , collection);
					defaults = {};
				}
				return _.extend({ 'name' : k }, defaults, v );
			});

			return retCols;
		},
		nonCrudOperation : function(url, requestMethod, options){
			return Backbone.sync.call(this, null, this, _.extend({
				url: url,
				type: requestMethod
			}, options));
		}

	});

	return XABaseCollection;
});
