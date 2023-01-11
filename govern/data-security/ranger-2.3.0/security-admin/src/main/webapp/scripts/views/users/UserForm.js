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

	var Backbone		= require('backbone');
	var SessionMgr		= require('mgrs/SessionMgr');

	var VXGroupList		= require('collections/VXGroupList');
	var localization	= require('utils/XALangSupport');
	var XAEnums			= require('utils/XAEnums');
	var XAUtils			= require('utils/XAUtils');
	var AddGroup 		= require('views/common/AddGroup');
	var XAViewUtils		= require('utils/XAViewUtils');

	require('backbone-forms');
	require('backbone-forms.templates');
	var UserForm = Backbone.Form.extend(
	/** @lends UserForm */
	{
		_viewName : 'UserForm',


		/** all events binding here */
		bindEvents : function(){
			this.on('userRoleList:change', function(form, fieldEditor){
				//this.userRoleListChange(form, fieldEditor);
				if(this.model.get('userSource') === XAEnums.UserTypes.USER_EXTERNAL.value){
					var externalUserRoleProperty = "<b> Warning !!</b> :  Please make sure that <i>"+ this.model.get('name') + "</i> user's role change performed here is consistent with <i>ranger.usersync.group.based.role.assignment.rules</i> property in ranger usersync configuration.";
					XAUtils.alertPopup({msg : externalUserRoleProperty});
				}
    		});
		},

		/**Form template**/

		templateData : function(){
			return {syncSourceInfo : this.syncData}
		},
		/**
		* intialize a new UserForm Form View
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a UserForm Form View");
			_.extend(this, _.pick(options,'groupList','showBasicFields'));
			Backbone.Form.prototype.initialize.call(this, options);
			if (this.model && !_.isUndefined(this.model.get('otherAttributes'))) {
				this.syncData = XAViewUtils.syncUsersGroupsDetails(this);
			}

			this.bindEvents();
		},

	    /** fields for the form
		*/
		fields: ['name', 'description'],
		schema :function(){
			return{
				name : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.userName") +' *',
					validators  : ['required',{type:'regexp',regexp:/^([A-Za-z0-9_]|[\u00C0-\u017F])([a-z0-9,._\-+/@= ]|[\u00C0-\u017F])+$/i,
						            message :' Invalid user name'}],
					editorAttrs : {'maxlength': 255},
			        errorMsg    :localization.tt('validationMessages.userNameValidationMsg')
				},
				password : {
					type		: 'PasswordFiled',
					title		: localization.tt("lbl.newPassword") +' *',
					validators  : ['required',
					               {type:'regexp',regexp:/^.*(?=.{8,256})(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$/,message :' Invalid password'}],
					editorAttrs : {'oncopy':'return false;','autocomplete':'off'},
					errorMsg    : localization.tt('validationMessages.passwordError')
				},
				passwordConfirm : {
					type		: 'PasswordFiled',
					title		: localization.tt("lbl.passwordConfirm") +' *',
					validators  : ['required',{type: 'match', field: 'password', message: 'Passwords must match !'},
					               {type:'regexp',regexp:/^.*(?=.{8,256})(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$/,message :' Invalid password'}],
					editorAttrs : {'oncopy':'return false;','autocomplete':'off'},
					errorMsg    : localization.tt('validationMessages.passwordError')
				},
				firstName : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.firstName")+' *',
                                        validators  : ['required',{type:'regexp',regexp:/^([A-Za-z0-9_]|[\u00C0-\u017F])([a-zA-Z0-9\s_. -@]|[\u00C0-\u017F])+$/i,
						            message :' Invalid first name'}],
					errorMsg    :localization.tt('validationMessages.firstNameValidationMsg'),
				},
				lastName : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.lastName"),
                                        validators  : [{type:'regexp',regexp:/^([A-Za-z0-9_]|[\u00C0-\u017F])([a-zA-Z0-9\s_. -@]|[\u00C0-\u017F])+$/i,
						            message :' Invalid last name'}],
					errorMsg    :localization.tt('validationMessages.lastNameValidationMsg'),
				},
				emailAddress : {
					type		: 'TextFieldWithIcon',
					title		: localization.tt("lbl.emailAddress"),
					validators  : [{type:'regexp',regexp:/^[\w]([\-\.\w])+[\w]+@[\w]+[\w\-]+[\w]*\.([\w]+[\w\-]+[\w]*(\.[a-z][a-z|0-9]*)?)$/,
						message :'Invalid email address'}],
					errorMsg    :localization.tt('validationMessages.emailAddressValidationMsg'),
				},
				userRoleList : {
					type : 'Select',
					options : function(callback, editor){

                        var userTypes = _.filter(XAEnums.UserRoles,function(m){
                            if(!SessionMgr.isKeyAdmin()){
                                return m.label != 'Unknown'	&& m.label != 'KeyAdmin' && m.label != 'KMSAuditor';
                            } else {
                                return m.label != 'Unknown' && m.label != 'Admin' && m.label != 'Auditor';
                            }
                        });
						var nvPairs = XAUtils.enumToSelectPairs(userTypes);
						callback(nvPairs);
						editor.$el.val("0");
					},
					title : localization.tt('lbl.selectRole')+' *'
				}
			};
		},
		/** on render callback */
		render: function(options) {
			var that = this;
			 Backbone.Form.prototype.render.call(this, options);
			this.renderCustomFields();
			this.initializePlugins();
			this.showCustomFields();
			if(!that.model.isNew()){
				this.fields.name.editor.$el.find('input').attr('disabled',true);
				if(this.model.has('userRoleList')){
					var roleList = this.model.get('userRoleList');
					if(!_.isUndefined(roleList) && roleList.length > 0){
						if(XAEnums.UserRoles[roleList[0]].value == XAEnums.UserRoles.ROLE_USER.value){
							this.fields.userRoleList.setValue(XAEnums.UserRoles.ROLE_USER.value);
						} else if(XAEnums.UserRoles[roleList[0]].value == XAEnums.UserRoles.ROLE_KEY_ADMIN.value){
							this.fields.userRoleList.setValue(XAEnums.UserRoles.ROLE_KEY_ADMIN.value);
                        } else if(XAEnums.UserRoles[roleList[0]].value == XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value){
                            this.fields.userRoleList.setValue(XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value);
                        } else if(XAEnums.UserRoles[roleList[0]].value == XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value){
                            this.fields.userRoleList.setValue(XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value);
						} else {
							this.fields.userRoleList.setValue(XAEnums.UserRoles.ROLE_SYS_ADMIN.value);
						}
					}
				}
				if(!_.isUndefined(this.model.get('userSource')) && this.model.get('userSource') == XAEnums.UserSource.XA_USER.value){
					this.fields.password.editor.$el.find('input').attr('disabled',true);
					this.fields.passwordConfirm.editor.$el.find('input').attr('disabled',true);
					this.fields.firstName.editor.$el.find('input').attr('disabled',true);
					this.fields.lastName.editor.$el.find('input').attr('disabled',true);
					this.fields.emailAddress.editor.$el.find('input').attr('disabled',true);
					this.fields.userRoleList.editor.$el.attr('disabled',true);
				}

				if(SessionMgr.getUserProfile().get('loginId') != "admin"){
					if(this.model.get('name') != "admin"){
						if(_.contains(SessionMgr.getUserProfile().get('userRoleList'),'ROLE_SYS_ADMIN')
								|| _.contains(SessionMgr.getUserProfile().get('userRoleList'),'ROLE_KEY_ADMIN')){
							this.fields.userRoleList.editor.$el.attr('disabled',false);
						} else {
							if(!SessionMgr.isKeyAdmin()){
								this.fields.userRoleList.editor.$el.attr('disabled',true);
							}
						}
					} else {
						this.fields.userRoleList.editor.$el.attr('disabled',true);
					}
				} else {
					this.fields.userRoleList.editor.$el.attr('disabled',false);
				}
				//User does not allowed to change his role (it's own role)
				if(this.model.get('name') == SessionMgr.getUserProfile().get('loginId')){
					this.fields.userRoleList.editor.$el.attr('disabled',true);
				}
			}
		},
		renderCustomFields: function(){
			var that = this;
			that.$('[data-customfields="groupIdList"]').html(new AddGroup({
				model : that.model
			}).render().el);
			if(!that.showBasicFields) {
				that.$('[data-customfields="groupIdList"]').hide();
			}
		},
		showCustomFields : function(){
			if(!this.showBasicFields){
				this.fields.name.$el.hide();
				this.fields.firstName.$el.hide();
				this.fields.lastName.$el.hide();
				this.fields.emailAddress.$el.hide();
				this.fields.userRoleList.$el.hide();

				this.fields.name.editor.validators = this.removeElementFromArr(this.fields.name.editor.validators , 'required');
				this.fields.firstName.editor.validators = this.removeElementFromArr(this.fields.firstName.editor.validators , 'required');
				this.fields.emailAddress.editor.validators = this.removeElementFromArr(this.fields.emailAddress.editor.validators , 'required');

				this.fields.password.$el.show();
				this.fields.passwordConfirm.$el.show();
				this.$el.children('fieldset').hide()
			}
			if(	(!this.model.isNew() && (this.showBasicFields))){
				this.fields.password.$el.hide();
				this.fields.passwordConfirm.$el.hide();

				this.fields.password.editor.validators = [];
				this.fields.passwordConfirm.editor.validators = [];
				//Remove validation checks for external users
				if(this.model.get('userSource') == XAEnums.UserSource.XA_USER.value){
					this.fields.firstName.editor.validators = [];
					var labelStr = this.fields.firstName.$el.find('label').html().replace('*','');
					this.fields.firstName.$el.find('label').html(labelStr);
				}
				this.$el.children('fieldset').show()
			}
		},
		removeElementFromArr : function(arr ,elem){
			var index = $.inArray(elem,arr);
			if(index >= 0) arr.splice(index,1);
			return arr;
		},
		beforeSaveUserDetail : function(){
			if(this.model.get('userSource') != XAEnums.UserSource.XA_USER.value){
				var groupArr = this.$('[data-customfields="groupIdList"]').find('.tags').editable('getValue', true);
				if(_.isNumber(groupArr)){
					groupArr = groupArr.toString().split(',');
				}
				this.model.set('groupIdList',groupArr);
				this.model.set('status',XAEnums.ActivationStatus.ACT_STATUS_ACTIVE.value);
				this.model.unset('passwordConfirm');
			}
			if(!this.model.isNew()){
				this.model.unset('password');
			}
			//FOR USER ROLE
			if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_USER.value){
				this.model.set('userRoleList',["ROLE_USER"]);
			}else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_KEY_ADMIN.value){
				this.model.set('userRoleList',["ROLE_KEY_ADMIN"]);
            } else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value){
                this.model.set('userRoleList',["ROLE_KEY_ADMIN_AUDITOR"]);
            } else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value){
                this.model.set('userRoleList',["ROLE_ADMIN_AUDITOR"]);
            } else{
				this.model.set('userRoleList',["ROLE_SYS_ADMIN"]);
			}
			return true;
		},
		beforeSavePasswordDetail : function(){
			this.model.unset('passwordConfirm');
			//FOR USER ROLE
			if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_USER.value){
				this.model.set('userRoleList',["ROLE_USER"]);
			}else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_KEY_ADMIN.value){
				this.model.set('userRoleList',["ROLE_KEY_ADMIN"]);
            } else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value){
                this.model.set('userRoleList',["ROLE_KEY_ADMIN_AUDITOR"]);
            } else if(this.fields.userRoleList.getValue() == XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value){
                this.model.set('userRoleList',["ROLE_ADMIN_AUDITOR"]);
            } else{
				this.model.set('userRoleList',["ROLE_SYS_ADMIN"]);
			}
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		}

	});

	return UserForm;
});
