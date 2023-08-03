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

	var VXPortalUserBase	= require('model_bases/VXPortalUserBase');
	var XAEnums			= require('utils/XAEnums');
	var localization		= require('utils/XALangSupport');
	
	var VXPortalUser = VXPortalUserBase.extend(
	/** @lends VXPortalUser.prototype */
	{
		/**
		 * @function schema
		 * This method is meant to be used by UI,
		 * by default we will remove the unrequired attributes from serverSchema
		 */

		schema : function(){
			var attrs = _.omit(this.serverSchema, 'id', 'createDate', 'updateDate', "permList", "publicScreenName");

			_.each(attrs, function(o){
				o.type = 'Hidden';
			});

			// Overwrite your schema definition here
			return _.extend(attrs,{
				firstName : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.firstName")+' *',
                                        validators  : ['required',{type:'regexp',regexp:/^([a-zA-Z0-9_]|[\u00C0-\u017F])([a-zA-Z0-9\s_. -@]|[\u00C0-\u017F])+$/i,message :'Invalid first name.'}],
					editorAttrs : { 'placeholder' : localization.tt("lbl.firstName")},
					errorMsg    :localization.tt('validationMessages.firstNameValidationMsg'),
					
				},
				lastName : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.lastName"),
                                        validators  : ['required',{type:'regexp',regexp:/^([a-zA-Z0-9_]|[\u00C0-\u017F])([a-zA-Z0-9\s_. -@]|[\u00C0-\u017F])+$/i,message :'Invalid last name.'}],
					editorAttrs : { 'placeholder' : localization.tt("lbl.lastName")},
					errorMsg    :localization.tt('validationMessages.lastNameValidationMsg'),
				},
				emailAddress : {
					type		: 'Text',
					title		: localization.tt("lbl.emailAddress"),
					validators  : ['email'],
					editorAttrs : { 'placeholder' : localization.tt("lbl.emailAddress")}//'disabled' : true}
					
				},
				oldPassword : {
					type		: 'PasswordFiled',
					title		: localization.tt("lbl.oldPassword")+' *',
				//	validators  : ['required'],
					fieldAttrs : {style : 'display:none;'},
					editorAttrs : { 'placeholder' : localization.tt("lbl.oldPassword"),'oncopy':'return false;','autocomplete':'off'},
					errorMsg    : localization.tt('validationMessages.passwordError')
					
				},
				newPassword : {
					type		: 'PasswordFiled',
					title		: localization.tt("lbl.newPassword")+' *',
				//	validators  : ['required'],
					fieldAttrs : {style : 'display:none;'},
					editorAttrs : { 'placeholder' : localization.tt("lbl.newPassword"),'oncopy':'return false;','autocomplete':'off'},
					errorMsg    : localization.tt('validationMessages.passwordError')
					
				},
				reEnterPassword : {
					type		: 'PasswordFiled',
					title		: localization.tt("lbl.reEnterPassword")+' *',
				//	validators  : ['required'],
					fieldAttrs : {style : 'display:none;'},
					editorAttrs : { 'placeholder' : localization.tt("lbl.reEnterPassword"),'oncopy':'return false;','autocomplete':'off'},
					errorMsg    : localization.tt('validationMessages.passwordError')
					
				},
				userRoleList : {
					type : 'Select',
					options : function(callback, editor){
						var XAUtils = require('utils/XAUtils');
						var userTypes = _.filter(XAEnums.UserRoles,function(m){return m.label != 'Unknown'});
						var nvPairs = XAUtils.enumToSelectPairs(userTypes);
						callback(nvPairs);
					},
					title : localization.tt('lbl.selectRole')+' *',
					editorAttrs : {disabled:'disabled'},
				}
			});
		},
		/** This models toString() */
		toString : function(){
			return /*this.get('name')*/;
		}

	}, {
		// static class members
	});

    return VXPortalUser;
	
});


