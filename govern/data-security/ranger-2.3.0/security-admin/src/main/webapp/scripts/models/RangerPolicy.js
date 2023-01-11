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

	var RangerPolicyBase	= require('model_bases/RangerPolicyBase');
	var XAUtils		= require('utils/XAUtils');
	var XAEnums		= require('utils/XAEnums');
	var localization= require('utils/XALangSupport');

	var RangerPolicy = RangerPolicyBase.extend(
	/** @lends RangerPolicy.prototype */
	{
		/**
		 * RangerPolicy initialize method
		 * @augments RangerPolicyBase
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'RangerPolicy';
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
			return _.extend(attrs,{
				name : {
                    type		: 'TextFieldWithIcon',
					title		: 'Policy Name *',
					validators	: ['required'],
					editorAttrs 	:{ maxlength: 255},
                                        errorMsg    : localization.tt("validationMessages.policyNameValidationMsg")
				},
				description : {
					type		: 'TextArea',
					title		: 'Description',
					validators	: []
				},
                policyLabels : {
                    type	   : 'Select2Remote',
                    title	   : 'Policy Label',
                    pluginAttr : this.getPluginAttr(),
                    options    : function(callback, editor){
                        callback();
                    },
                },
				isEnabled : {
					type		: 'Switch',
					title		: '',//localization.tt("lbl.policyStatus"),
					editorClass : 'policy-enable-disable',
					onText		: 'Enabled',
					offText		: 'Disabled',
					width		: '80',
					switchOn	: true
				},
                policyPriority : {
                    type            : 'Switch',
                    title           : '',//localization.tt("lbl.policyStatus"),
                    editorClass     : 'policy-priority',
                    onText          : 'Override',
                    offText         : 'Normal',
                    width           : '80',
                    switchOn        : false
                },
				isAuditEnabled : {
					type		: 'Switch',
					title		: localization.tt("lbl.auditLogging"),
					onText 		: 'Yes',
					offText		: 'No',
					switchOn	: true
				},
				//recursive(ON/OFF) toggle
				recursive : {
					type : 'Switch',
					title : 'Recursive',
					onText : 'On',
					offText : 'Off',
					switchOn : true
				}
			});
		},

		/** need to pass eventTime in queryParams(opt.data) */
		fetchByEventTime : function(opt){
			var queryParams = opt.data;
			queryParams.policyId = this.get('id');
			queryParams.versionNo = this.get('version');
			if(_.isUndefined(queryParams.eventTime)){
				throw('eventTime can not be undefined');
			}

			opt.url = 'service/plugins/policies/eventTime';
			return this.fetch(opt);
		},

		fetchByVersion : function(versionNo, opt){
			if(_.isUndefined(versionNo)){
				throw('versionNo can not be undefined');
			}
			opt.url = 'service/plugins/policy/'+this.get('id')+'/version/'+versionNo;
			return this.fetch(opt);
		},

		fetchVersions : function(){
			var versionList;
			var url = 'service/plugins/policy/'+this.get('id')+'/versionList';
			$.ajax({
				url : url,
				async : false,
				dataType : 'JSON',
				success : function(data){
                                        versionList = (data.value.split(',').map(Number)).sort(function(a, b) { return a - b });
				},
			});
			return versionList;
		},

		/** This models toString() */
		toString : function(){
			return this.get('name');
                },

        getPluginAttr : function(){
            var tags = [];
            _.each(this.get('policyLabels') , function(name){
                tags.push( { 'id' : _.escape( name ), 'text' : _.escape( name ) } );
            });
            return{
                multiple: true,
                closeOnSelect : true,
                placeholder : 'Policy Label',
                allowClear: true,
                tokenSeparators: ["," , " "],
                tags : true,
                initSelection : function (element, callback) {
                    callback(tags);
                },
                createSearchChoice: function(term, data) {
                    term = _.escape(term);
                    if ($(data).filter(function() {
                        return this.text.localeCompare(term) === 0;
                    }).length === 0) {
                        if($.inArray(term, this.val()) >= 0){
                            return null;
                        }else{
                            return {
                                id : term,
                                text: term
                            };
                        }
                    }
                },
                ajax: {
                    url: 'service/plugins/policyLabels',
                    dataType: 'json',
                    data: function (term, page) {
                        return {policyLabel : term};
                    },
                    results: function (data, page) {
                        var results = [] , selectedVals = [];
                        if(data.resultSize != "0"){
                            results = data.map(function(m){	return {id : _.escape(m), text: _.escape(m) };	});
                        }
                        return {results : results};
                    },
                },
                formatResult : function(result){
                    return result.text;
                },
                formatSelection : function(result){
                    return result.text;
                },
            }
        },
    }, {
		// static class members
	});

    return RangerPolicy;
	
});


