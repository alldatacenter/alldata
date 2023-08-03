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

 /*
 *
 */
define(function(require) {
    'use strict';

	var Backbone		= require('backbone');
    var App		        = require('App');
	var XAEnums			= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');
	var VXGroup			= require('models/VXGroup');
	var VXGroupList		= require('collections/VXGroupList');
	var VXUserList		= require('collections/VXUserList');
	var VXModuleDef			= require('models/VXModuleDef');
	var VXModuleDefList		= require('collections/VXModuleDefList');
	var BackboneFormDataType	= require('models/BackboneFormDataType');
	require('bootstrap-editable');
	require('backbone-forms');
	require('backbone-forms.list');
	require('backbone-forms.templates');
	require('backbone-forms.XAOverrides');

	var ModulePermissionForm = Backbone.Form.extend({

		_viewName : 'ModulePermissionForm',
		template : require('hbs!tmpl/permissions/ModulePermissionForm_tmpl'),
		templateHelpers :function(){
		},
		templateData : function(){
                        return {
                                'id' : this.model.id,
                                'permHeaders' : this.getPermHeaders(),
                                'userList' : this.model.get('userPermList'),
                                'groupList' : this.model.get('groupPermList')
                        };
		},
		initialize : function(options) {
                        _.extend(this, _.pick(options));

			if (!this.model.isNew()){
				this.setupFieldsforEditModule();
			}
			Backbone.Form.prototype.initialize.call(this, options);
		},
		ui : {
                        selectGroups	: 'div[data-editors="selectGroups"]',
                        selectUsers		: 'div[data-editors="selectUsers"]',
                        addGroupBtn		: '[data-id="addGroupBtn"]',
                        addUserBtn		: '[data-id="addUserBtn"]',
        },
                events : function(){
                        var events = {};
                        events['click ' + this.ui.addGroupBtn ] = 'onAddGroup';
                        events['click ' + this.ui.addUserBtn ] = 'onAddUser';
                        events['click ' + '[data-js="addUser"]']  = 'onAddUserClick';
                        events['click ' + '[data-js="selectedGroupSpan"]']  = 'onRemovedGroupClick';

                        return events;
		},
		/** fields for the form
		*/
		fields: ['module', 'selectGroups','selectUsers','isAllowed'],
		schema :function(){
			return this.getSchema();
		},
		getSchema : function(){
			var that = this;
			return {
				module : {
					type		: 'Text',
					title		: localization.tt("lbl.moduleName") +' *',
					editorAttrs : {'readonly' :'readonly'},
					validation	: {'required': true},
				},
				selectGroups : {
					type : 'Select2Remote',
					editorAttrs  : {'placeholder' :'Select Group','tokenSeparators': [",", " "],multiple:true},
                                        pluginAttr: this.getPlugginAttr(true,{'lookupURL':"service/xusers/groups",'idKey':'groupId','textKey':'groupName'}),
					title : localization.tt('lbl.selectGroup')+' *'
				},
				selectUsers : {
					type : 'Select2Remote',
					editorAttrs  : {'placeholder' :'Select User','tokenSeparators': [",", " "],multiple:true},
                                        pluginAttr: this.getPlugginAttr(true,{'lookupURL':"service/xusers/users",'idKey':'userId','textKey':'userName'}),
					title : localization.tt('lbl.selectUser')+' *',
				},
				isAllowed : {
					type : 'Checkbox',
					editorAttrs  : {'checked':'checked',disabled:true},
					title : 'Is Allowed ?'
					},
			}
		},

        render: function(options) {
            var that = this;
            Backbone.Form.prototype.render.call(this, options);
            this.showGroupTag();
            this.showUserTag();
            this.$el.find('[data-js="selectedGroupList"] span i').on('click', this.removeGroup.bind(this));
            this.$el.find('[data-js="selectedUserList"] span i').on('click', this.removeUser.bind(this));
            if(this.model.get('groupPermList').length <= 0){
                this.$el.find('.emptySelectedGroups').show();
            }else{
                this.$el.find('.emptySelectedGroups').hide();
            }
            if(this.model.get('userPermList').length <= 0){
                this.$el.find('.emptySelectedUsers').show();
            }else{
                this.$el.find('.emptySelectedUsers').hide();
            }
        },

        showUserTag : function() {
            var that = this
            var userListData = this.model.get('userPermList');
            if(userListData &&!_.isEmpty(userListData)) {
                var i , j;
                for(var i=0,j=0; i<=j+200 && userListData.length > i; i++){
                    that.$el.find('.selectedUserList').append('<span class="selected-widget"><i class="icon remove fa-fw fa fa-remove" data-id="'+userListData[i].userId+'"></i>&nbsp;'+userListData[i].userName+'</span>')
                }
                that.$el.find('.selectedUserList').scroll(function(position) {
                    if (position.currentTarget.scrollHeight <= (position.currentTarget.clientHeight + position.currentTarget.scrollTop) + 10) {
                        j = i;
                        for(i; i<=j+200 && userListData.length > i; i++){
                            that.$el.find('.selectedUserList').append('<span class="selected-widget"><i class="icon remove fa-fw fa fa-remove" data-id="'+userListData[i].userId+'"></i>&nbsp;'+userListData[i].userName+'</span>')
                        }
                        that.$el.find('[data-js="selectedUserList"] span i').on('click', that.removeUser.bind(that));
                    }
                })
            }
        },
        showGroupTag : function() {
            var that = this
            var groupListData = this.model.get('groupPermList');
            if (groupListData && !_.isEmpty(groupListData)) {
                var m , n;
                for(var m=0,n=0; m<=n+200 && groupListData.length > m; m++){
                    that.$el.find('.selectedGroupList').append('<span class="selected-widget"><i class="icon remove fa-fw fa fa-remove" data-id="'+groupListData[m].groupId+'"></i>&nbsp;'+groupListData[m].groupName+'</span>')
                }
                that.$el.find('.selectedGroupList').scroll(function(position) {
                    if (position.currentTarget.scrollHeight <= (position.currentTarget.clientHeight + position.currentTarget.scrollTop) + 10) {
                        n = m;
                        for(m; m<=n+200 && groupListData.length > m; m++){
                            that.$el.find('.selectedGroupList').append('<span class="selected-widget"><i class="icon remove fa-fw fa fa-remove" data-id="'+groupListData[m].groupId+'"></i>&nbsp;'+groupListData[m].groupName+'</span>')
                        }
                        that.$el.find('[data-js="selectedGroupList"] span i').on('click', that.removeGroup.bind(that));
                    }
                })
            }
        },

		setupFieldsforEditModule : function(){
                        this.addedGroups = _.map(this.model.get('groupPermList'), function(g){ return { 'id' : g.groupId, 'text' : g.groupName} });
                        this.addedUsers = _.map(this.model.get('userPermList'), function(u){ return { 'id' : u.userId, 'text' : u.userName} });
		},
		getPermHeaders : function(){
			var permList = [];
                        permList.unshift(localization.tt('lbl.selectAndAddUser'));
                        permList.unshift(localization.tt('lbl.selectAndAddGroup'));
			return permList;
		},
		getPlugginAttr :function(autocomplete, options){
			var that = this;
			if(!autocomplete)
				return{ tags : true, width :'220px', multiple: true, minimumInputLength: 1 };
			else {
				return {
					closeOnSelect : true,
					multiple: true,
					tokenSeparators: [",", " "],
					ajax: {
						url: options.lookupURL,
						type : 'GET',
						params : {
							timeout: 3000,
							contentType: "application/json; charset=utf-8",
						},
						cache: false,
						data: function (term, page) {
							//To be checked
							return { name : term, isVisible : XAEnums.VisibilityStatus.STATUS_VISIBLE.value };
						},
						results: function (data, page) {
							var results = [], selectedVals = [];
							//Get selected values of groups/users dropdown
							selectedVals = that.getSelectedValues(options);
							if(data.resultSize != "0"){
								if(!_.isUndefined(data.vXGroups)){
                                                                    results = data.vXGroups.map(function(m, i){	return {id : m.id, text: _.escape(m.name) };	});
								} else if(!_.isUndefined(data.vXUsers)){
//								     tag base policy tab hide from KeyAdmin and KMSAuditor users
                                                                    if(that.model.get('module') === XAEnums.MenuPermissions.XA_TAG_BASED_POLICIES.label){
                                                                        _.map(data.vXUsers ,function(m, i){
                                                                            if(XAEnums.UserRoles[m.userRoleList[0]].label != 'KeyAdmin' && XAEnums.UserRoles[m.userRoleList[0]].label != 'KMSAuditor'){
                                                                                results.push({id : m.id, text: _.escape(m.name) });
                                                                            }
                                                                        });
                                                                    }else{
                                                                        results = data.vXUsers.map(function(m, i){  return {id : m.id, text: _.escape(m.name) };    });
                                                                    }
                                                                }
                                                                if(!_.isEmpty(selectedVals)){
										results = XAUtil.filterResultByText(results, selectedVals);
								}
							}
							return { results : results};
						},
						transport: function (options) {
                                                        $.ajax(options).fail(function(respones) {
								console.log("ajax failed");
                                                                XAUtil.defaultErrorHandler('error',respones);
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
					formatNoMatches : function(term){
						return options.textKey == 'groupName' ?  'No group found.' : 'No user found.'; 
					}
				};
			}
		},
		getSelectedValues : function(options){
			var vals = [],selectedVals = [];
                        var added = options.textKey == 'groupName' ? this.addedGroups : this.addedUsers;
                        if(!_.isEmpty(added)){
                                selectedVals = _.map(added, function(obj){ return obj.text; });
			}
			vals.push.apply(vals , selectedVals);
			return vals;
		},
		beforeSaveModulePermissions : function(){
                        if(!_.isEmpty(this.fields.selectGroups.editor.$el.select2('data'))
                                        || !_.isEmpty(this.fields.selectUsers.editor.$el.select2('data'))){
                                XAUtil.alertPopup({
                                        msg :localization.tt('msg.addSelectedUserGroup'),
                                });
                                return false;
			}
                        this.model.unset('selectUsers');
                        this.model.unset('selectGroups');

			return true;
		},
                onAddGroup : function(e){
                        var that = this, newPerms = [];
                        var selectedGroups = this.fields.selectGroups.editor.$el.select2('data');
                        _.each(selectedGroups, function(obj){
                                var self = that;
                                this.$el.find('[data-js="selectedGroupList"]').append('<span class="selected-widget"  ><i class="icon remove fa-fw fa fa-remove" data-js="selectedGroupIcon" data-id="'+obj.id+'"></i>&nbsp;'+obj.text+'</span>')
                                this.addedGroups.push(obj)
                                this.$el.find('[data-js="selectedGroupList"] :last').on('click',this.removeGroup.bind(this));
                                this.fields.selectGroups.editor.$el.select2('data',[]);
                                var addedGroupPerm =_.findWhere(this.model.get('groupPermList'), {'groupId': parseInt(obj.id) });
                                if(!_.isUndefined(addedGroupPerm)){
                                        addedGroupPerm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_ALLOWED.value;
                                }else{
                                        var perm = {};
                                        perm['moduleId'] = that.model.get('id');
                                        perm['groupId'] = obj['id'];
                                        perm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_ALLOWED.value;
                                        newPerms.push(perm);
                                }
                        }, this);
                        if(!_.isEmpty(newPerms)){
                                var permissions = this.model.get('groupPermList');
                                this.model.set('groupPermList', permissions.concat(newPerms));
                        }

                        this.emptyCheck();
                        if(_.isEmpty(selectedGroups))	alert(localization.tt("msg.pleaseSelectGroup"));
                        return false;

                },

                removeGroup : function(e){
                        var ele = $(e.currentTarget);
                        var id = ele.attr('data-id');
                        ele.parent().remove();
                        this.addedGroups = _.reject(this.addedGroups, function(d){ return d.id == id; });
                        var removedGroupPerm =_.findWhere(this.model.get('groupPermList'), {'groupId': parseInt(id) });
                        if(!_.isUndefined(removedGroupPerm)){
                                if(!_.has(removedGroupPerm, 'id')){
                                        this.model.set('groupPermList', _.reject(this.model.get('groupPermList'), function(perm){ return perm.groupId == removedGroupPerm.groupId }));
                                }else{
                                        removedGroupPerm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_DENIED.value;
				}
                        }
                        this.emptyCheck();
                },
                onAddUser : function(e){
                        var that = this, newPerms = [];
                        var selectedUsers = this.fields.selectUsers.editor.$el.select2('data');
                        _.each(selectedUsers, function(obj){
                                var self = that;
                                this.$el.find('[data-js="selectedUserList"]').append('<span class="selected-widget"  ><i class="icon remove fa-fw fa fa-remove" data-js="selectedUserIcon" data-id="'+obj.id+'"></i>&nbsp;'+obj.text+'</span>')
                                this.addedUsers.push(obj)
                                this.$el.find('[data-js="selectedUserList"] :last').on('click',this.removeUser.bind(this));
                                this.fields.selectUsers.editor.$el.select2('data', []);
                                var addedUserPerm =_.findWhere(this.model.get('userPermList'), {'userId': parseInt(obj.id) });
                                if(!_.isUndefined(addedUserPerm)){
                                        addedUserPerm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_ALLOWED.value;
                                }else{
                                        var perm = {};
                                        perm['moduleId'] = that.model.get('id');
                                        perm['userId'] = obj['id'];
                                        perm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_ALLOWED.value;
                                        newPerms.push(perm);
				}
                        }, this);
                        if(!_.isEmpty(newPerms)){
                                var permissions = this.model.get('userPermList');
                                this.model.set('userPermList', permissions.concat(newPerms));
                        }
                        this.emptyCheck();
                        if(_.isEmpty(selectedUsers))	alert(localization.tt("msg.pleaseSelectUser"));
                        return false;
                },
                removeUser : function(e){
                        var ele = $(e.currentTarget);
                        var id = ele.attr('data-id');
                        ele.parent().remove();
                        this.addedUsers = _.reject(this.addedUsers, function(d){ return d.id == id; });
                        var removedUserPerm =_.findWhere(this.model.get('userPermList'), {'userId': parseInt(id) });
                        if(!_.isUndefined(removedUserPerm)){
                                if(!_.has(removedUserPerm, 'id')){
                                        this.model.set('userPermList', _.reject(this.model.get('userPermList'), function(perm){ return perm.userId == removedUserPerm.userId }));
                                }else{
                                        removedUserPerm.isAllowed = XAEnums.AccessResult.ACCESS_RESULT_DENIED.value;
                                }
                        }
                        this.emptyCheck();
                },
                emptyCheck : function(type){
                        if(this.$el.find('[data-js="selectedGroupList"] span').length > 0){
                                this.$el.find('.emptySelectedGroups').hide();
                        }else{
                                this.$el.find('.emptySelectedGroups').show();
			}

                        if(this.$el.find('[data-js="selectedUserList"] span').length > 0){
                                this.$el.find('.emptySelectedUsers').hide();
                        }else{
                                this.$el.find('.emptySelectedUsers').show();
                        }
		}

	});
	
	return ModulePermissionForm;
});