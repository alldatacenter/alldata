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

	var RangerServiceDefBase	= require('model_bases/RangerServiceDefBase');
	var XAUtils		= require('utils/XAUtils');
	var XAEnums		= require('utils/XAEnums');
	var localization= require('utils/XALangSupport');
        var RangerService	= require('models/RangerService');

	var RangerServiceDef = RangerServiceDefBase.extend(
	/** @lends RangerServiceDef.prototype */
	{
		/**
		 * RangerServiceDef initialize method
		 * @augments RangerServiceDefBase
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'RangerServiceDef';
			this.bindErrorEvents();
		},
		/**
		 * @function schema
		 * This method is meant to be used by UI,
		 * by default we will remove the unrequired attributes from serverSchema
		 */

		schemaBase : function(){
			var attrs = _.omit(this.serverSchema, 'id', 'createDate', 'updateDate', "version",
					"displayOption", "permList", "status", "updatedBy");

			_.each(attrs, function(o){
				o.type = 'Hidden';
			});

			// Overwrite your schema definition here
			return _.extend(attrs,{

				name : {
					type		: 'Text',
					title		: 'Service Name *',
                    validators	: ['required',{type:'regexp', regexp:/^[a-zA-Z0-9_-][a-zA-Z0-9\s_-]{0,254}$/,message : localization.tt("validationMessages.nameValidationMsg")}],
                },
                displayName : {
                    type : 'Text',
                    title : 'Display Name',
                    validators  : [{type:'regexp', regexp:/^[a-zA-Z0-9_-][a-zA-Z0-9\s_-]{0,254}$/, message : localization.tt("validationMessages.nameValidationMsg")}]
				},
				description : {
					type		: 'TextArea',
					title		: 'Description',
					validators	: []
				},
				isEnabled : {
				    type    :  'Radio',
					title   : 'Active Status',
					options : function(callback, editor){
						var activeStatus = _.filter(XAEnums.ActiveStatus,function(m){return m.label != 'Deleted'});
						var nvPairs = XAUtils.enumToSelectPairs(activeStatus);
						callback(_.sortBy(nvPairs, function(n){ return !n.val; }));
					}
				},
				tagService : {
					type : 'Select2Remote',
					title : 'Select Tag Service',
					pluginAttr : this.getPluginAttr(),
					options    : function(callback, editor){
	                    callback();
	                },
	                onFocusOpen : false
				}
			});
		},
		getPluginAttr : function(){
			return { 
				closeOnSelect : true,
				placeholder : 'Select Tag Service',
				allowClear: true,
				initSelection : function (element, callback) {
                                        var rangerService = new RangerService()
                                        rangerService.url = XAUtils.getServiceByName(element.val());
                                        rangerService.fetch( {
                                                cache : false,
                                                async : false,
                                        }).done(function(m) {
                                                callback( { id:_.escape(m.get('name')), text:_.escape(m.get('displayName')) })
                                        })
				},
				ajax: { 
					url: "service/plugins/services",
					dataType: 'json',
					data: function (term, page) {
						return { serviceNamePartial : term, serviceType : 'tag' };
					},
					results: function (data, page) { 
						var results = [];
						if(data.resultSize != "0"){
                                                        results = data.services.map(function(m, i){	return {id : _.escape(m.name), text: _.escape(m.displayName) }});
                                                }
                                                if($.find('[name="tagService"]') && !_.isEmpty($.find('[name="tagService"]')[0].value)) {
                                                        results = _.reject(results, function(m) {return m.id == $.find('[name="tagService"]')[0].value});
						}
						return {results : results};
					},
					transport : function (options) {
                                                $.ajax(options).fail(function(respones) {
							XAUtils.defaultErrorHandler('error',respones);
							this.success({
								resultSize : 0
							});
						});
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No tag service found.';
				}
			};
		},

		/** This models toString() */
		toString : function(){
			return this.get('name');
		}

	}, {
		// static class members
	});

    return RangerServiceDef;
	
});


