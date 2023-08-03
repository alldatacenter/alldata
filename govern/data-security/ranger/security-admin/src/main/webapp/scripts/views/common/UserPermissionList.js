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
    var App         	= require('App');
	var XAEnums			= require('utils/XAEnums');
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');
	var VXUser			= require('models/VXUser');
	
	require('bootstrap-editable');
    	
	var UserPermissionItem = Backbone.Marionette.ItemView.extend({
		_msvName : 'UserPermissionItem',
		template : require('hbs!tmpl/common/UserPermissionItem'),
		tagName : 'tr',
		templateHelpers : function(){
						
			return {
			   permissions 		: _.flatten(_.pick(XAEnums.XAPermType,  XAUtil.getPerms(this.policyType))),
			   policyKnox 		: this.policyType == XAEnums.AssetType.ASSET_KNOX.value ? true :false,
			   policyStorm 		: this.policyType == XAEnums.AssetType.ASSET_STORM.value ? true :false,
			   isModelNew		: !this.model.has('editMode'),
			   stormPerms		: this.stormPermsIds.length == 14 ? _.union(this.stormPermsIds,[-1]) : this.stormPermsIds
			};
		},
		ui : {
			selectUsers		: '[data-js="selectUsers"]',
			inputIPAddress	: '[data-js="ipAddress"]',
			tags			: '[class=tags]'
		},
		events : {
			'click [data-action="delete"]'	: 'evDelete',
			'click td'						: 'evClickTD',
			'change [data-js="selectUsers"]': 'evSelectUser',
			'change [data-js="ipAddress"]'	: 'evIPAddress'
		},

		initialize : function(options) {
			_.extend(this, _.pick(options, 'userList','policyType'));

			this.stormPermsIds = [];
			if(this.policyType == XAEnums.AssetType.ASSET_STORM.value){

				if(this.model.has('editMode') && this.model.get('editMode')){
					this.stormPermsIds = _.map(this.model.get('_vPermList'), function(p){
						if(XAEnums.XAPermType.XA_PERM_TYPE_ADMIN.value != p.permType)
							return p.permType;
					});
				}
			}
		},
 
		onRender : function() {
			var that = this;
			
			if(this.model.get('userName') != undefined){
				this.ui.selectUsers.val(this.model.get('userId').split(','));
			}
			if(!_.isUndefined(this.model.get('ipAddress'))){
				this.ui.inputIPAddress.val(this.model.get('ipAddress').toString());
			}
			
			if(this.model.has('editMode') && this.model.get('editMode')){
				_.each(this.model.get('_vPermList'), function(p){
					this.$el.find('input[data-id="' + p.permType + '"]').attr('checked', 'checked');
				},this);
			}
			if(this.policyType == XAEnums.AssetType.ASSET_STORM.value){
				this.renderStormPerms();
			}
			
			this.createSelectUserDropDown();
			this.userDropDownChange();
		},
		checkDirtyFieldForDropDown : function(e){
			var userIdList =[];
			
			if(!_.isUndefined(this.model.get('userId'))){
				userIdList = this.model.get('userId').split(',');
			}
			XAUtil.checkDirtyField(userIdList, e.val, $(e.currentTarget));
		},
		toggleAddButton : function(e){
			var temp = [];
			this.collection.each(function(m){
				if(!_.isUndefined(m.get('userId'))){
					temp.push.apply(temp, m.get('userId').split(','));
					
				}
			});
			if(!_.isUndefined(e)){
				if( !_.isUndefined(e.added) && ((temp.length + 1) == this.userList.length)) 
					$('[data-action="addUser"]').hide();
				if(!_.isUndefined(e.removed))
					$('[data-action="addUser"]').show();
			} else {
				$('[data-action="addUser"]').show();
			}
		},
		evDelete : function(){
			var that = this;
			this.collection.remove(this.model);
			this.toggleAddButton();
		},
		evClickTD : function(e){
			var $el = $(e.currentTarget),permList =[],perms =[];
			
			if($(e.toElement).is('td')){
				var $checkbox = $el.find('input');
				$checkbox.is(':checked') ? $checkbox.prop('checked',false) : $checkbox.prop('checked',true);  
			}
			var curPerm = $el.find('input').data('id');
			if(!_.isUndefined(curPerm)){
				var perms = [];
				if(this.model.has('_vPermList')){
					if(_.isArray(this.model.get('_vPermList'))){
						perms = this.model.get('_vPermList');
					} else {
						perms.push(this.model.get('_vPermList'));
					}
				}
				
				if($el.find('input[type="checkbox"]').is(':checked')){
					perms.push({permType : curPerm});
					
					if(curPerm == XAEnums.XAPermType.XA_PERM_TYPE_ADMIN.value){
						$el.parent().find('input[type="checkbox"]:not(:checked)[data-id!="'+curPerm+'"]').map(function(){
							perms.push({ permType :$(this).data('id')});
						});
						$el.parent().find('input[type="checkbox"]').prop('checked',true);
					}
				} else {
					perms = _.reject(perms,function(el) { return el.permType == curPerm; });
				}

				this.checkDirtyFieldForCheckBox(perms);
				if(!_.isEmpty(perms)){
					this.model.set('_vPermList', perms);
				} else {
					this.model.unset('_vPermList');
				}
			}
		},
		checkDirtyFieldForCheckBox : function(perms){
			var permList = [];
			if(!_.isUndefined(this.model.get('_vPermList')))
				permList = _.map(this.model.attributes._vPermList,function(obj){return obj.permType;});
			perms = _.map(perms,function(obj){return obj.permType;});
			XAUtil.checkDirtyField(permList, perms, this.$el);
		},
		createSelectUserDropDown :function(){
			var that = this;
			if(this.model.has('editMode') && !_.isEmpty(this.ui.selectUsers.val())){
				var temp = this.ui.selectUsers.val().split(",");
				_.each(temp , function(id){
					if(_.isUndefined(that.userList.get(id))){
						var user = new VXUser({id : id});
						user.fetch({async:false}).done(function(){
							that.userList.add(user);
						});
					}
				});
			}
			this.userArr = this.userList.map(function(m){
				return { id : m.id+"" , text : m.get('name')};
			});
			this.ui.selectUsers.select2({
				closeOnSelect : true,
				placeholder : 'Select User',
		//		maximumSelectionSize : 1,
				width :'220px',
				tokenSeparators: [",", " "],
				tags : this.userArr, 
				initSelection : function (element, callback) {
					var data = [];
					$(element.val().split(",")).each(function () {
						var obj = _.findWhere(that.userArr,{id:this});	
						data.push({id: this, text: obj.text});
					});
					callback(data);
				},
				createSearchChoice: function(term, data) {
				},
				ajax: { 
					url: "service/xusers/users",
					dataType: 'json',
					data: function (term, page) {
						return {name : term, isVisible : XAEnums.VisibilityStatus.STATUS_VISIBLE.value};
					},
					results: function (data, page) { 
						var results = [],selectedVals=[];
						
						selectedVals = that.getUsersSelectdValues();
						if(data.resultSize != "0"){
								results = data.vXUsers.map(function(m, i){	return {id : m.id+"", text: m.name};	});
								if(!_.isEmpty(selectedVals)){
									results = XAUtil.filterResultByIds(results, selectedVals);
								}
								return {results : results};
						}
						return {results : results};
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No user found.';
				}
				
			}).on('select2-focus', XAUtil.select2Focus);
		},
		userDropDownChange : function(){
			var that = this;
			this.ui.selectUsers.on('change',function(e){
					that.checkDirtyFieldForDropDown(e);
					that.toggleAddButton(e);
					var duplicateUsername = false;
					if(e.removed != undefined){
						var gIdArr = [],gNameArr = [];
						
						gIdArr = _.without(that.model.get('userId').split(','), e.removed.id);
						if(that.model.get('userName') != undefined){
							gNameArr = _.without(that.model.get('userName').split(','), e.removed.text);
						}
						if(!_.isEmpty(gIdArr)){
							that.model.set('userId',gIdArr.join(','));
							that.model.set('userName',gNameArr.join(','));
						} else {
							that.model.unset('userId');
							that.model.unset('userName');
						}
						return;
						
					}
					if(!_.isUndefined(e.added)){
						that.model.set('userId', e.currentTarget.value);
					}
				});
		},
		getUsersSelectdValues : function(){
			var vals = [],selectedVals = [];

			this.collection.each(function(m){
				if(!_.isUndefined(m.get('userId'))){
					vals.push.apply(vals, m.get('userId').split(','));
				}
			});
			if(!_.isEmpty(this.ui.selectUsers.select2('val'))){
				selectedVals = this.ui.selectUsers.select2('val');
			}
			vals.push.apply(vals , selectedVals);
			vals = $.unique(vals);
			return vals;
		},
		evIPAddress :function(e){
			if(!_.isEmpty($(e.currentTarget).val())){
				this.model.set('ipAddress',$(e.currentTarget).val().split(','));
			} else {
				this.model.unset('ipAddress');
			}
		},
		renderStormPerms :function(){
			var that = this;
			var permArr = _.pick(XAEnums.XAPermType,  XAUtil.getStormActions(this.policyType));
			this.stormPerms =  _.map(permArr,function(m){return {text:m.label, value:m.value};});
			this.stormPerms.push({'value' : -1, 'text' : 'Select/Deselect All'});
			
			this.ui.tags.editable({
			    placement: 'right',
//			    emptytext : 'Please select',
			    source: this.stormPerms,
			    display: function(idList,srcData) {
			    	if(_.isEmpty(idList.toString())){
			    		$(this).html('');
			    		return;
			    	}
			    	if(!_.isArray(idList))	idList = [idList];

			    	var permTypeArr = [];
		    		var valArr = _.map(idList, function(id){
		    			if(!(parseInt(id) <= 0) && (!_.isNaN(parseInt(id)))){
			    			var obj = _.findWhere(srcData,{'value' : parseInt(id)});
			    			permTypeArr.push({permType : obj.value});
							return "<span class='badge badge-dark'>" + obj.text + "</span>";
		    			}
		    		});
		    		if(that.model.has('_vPermList')){
                        var adminPerm = _.where(that.model.get('_vPermList'),{'permType': XAEnums.XAPermType.XA_PERM_TYPE_ADMIN.value });
                        permTypeArr = _.isEmpty(adminPerm) ? permTypeArr : _.union(permTypeArr,adminPerm);
                    }

		    		that.model.set('_vPermList', permTypeArr);
		    		$(this).html(valArr.join(" "));
			    },
			});
			this.$('[id^="tags-edit-"]').click(function(e) {
			    e.stopPropagation();
			    e.preventDefault();
			    that.$('#' + $(this).data('editable') ).editable('toggle');
			    that.$('input[type="checkbox"][value="-1"]').click(function(e){
					var checkboxlist =$(this).closest('.editable-checklist').find('input[type="checkbox"][value!=-1]');
					$(this).is(':checked') ? checkboxlist.prop('checked',true) : checkboxlist.prop('checked',false); 
					
				});
			});
		},
	});



	return Backbone.Marionette.CompositeView.extend({
		_msvName : 'UserPermissionList',
		template : require('hbs!tmpl/common/UserPermissionList'),
		templateHelpers :function(){

			return {
				permHeaders : XAUtil.getPermHeaders(this.policyType,false)
			};
		},
		getItemView : function(item){
			if(!item){	return;	}
			return UserPermissionItem;
		},
		itemViewContainer : ".js-formInput",
		itemViewOptions : function() {
			return {
				'collection' : this.collection,
				'userList' : this.userList,
				'policyType'	: this.policyType,
			};
		},
		events : {
			'click [data-action="addUser"]' : 'addNew'
		},
		initialize : function(options) {
			_.extend(this, _.pick(options, 'userList','policyType'));

			if(this.collection.length == 0){
				this.collection.add(new Backbone.Model());
			}
		},
		onRender : function(){
			//console.log("onRender of ArtifactFormNoteList called");
			this.toggleAddButton();
		},
		addNew : function(){
			var that =this;
			this.collection.add(new Backbone.Model());
			this.toggleAddButton();
		},
		toggleAddButton : function(){
			var userIds=[];
			this.collection.each(function(m){
				if(!_.isUndefined(m.get('userId'))){
					var temp = m.get('userId').split(',');
					userIds.push.apply(userIds,temp);
				}
			});
			if(userIds.length == this.userList.length){
				this.$('button[data-action="addUser"]').hide();
			} else {
				this.$('button[data-action="addUser"]').show();
			}
		}
	});
});
