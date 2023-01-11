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
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');
	var SessionMgr 		= require('mgrs/SessionMgr');

	var VXGroup			= require('models/VXGroup');
	var VXUser				= require('models/VXUser');
	var VXGroupList			= require('collections/VXGroupList');
	var VXUserList			= require('collections/VXUserList');
	var Vent			= require('modules/Vent');
        var VXRole			= require('models/VXRole');
        var VXRoleList		= require('collections/VXRoleList');
	
	require('bootstrap-editable');
	require('esprima');
    	
	var PermissionItem = Backbone.Marionette.ItemView.extend({
		_msvName : 'PermissionItem',
		template : require('hbs!tmpl/policies/PermissionItem'),
		tagName : 'tr',
		templateHelpers : function(){
			
			return {
				permissions 	: this.accessTypes,
				policyConditions: this.policyConditions,
				isModelNew		: !this.model.has('editMode'),
				perms			: this.permsIds.length == 14 ? _.union(this.permsIds,[-1]) : this.permsIds,
			    isMaskingPolicy : XAUtil.isMaskingPolicy(this.rangerPolicyType),
			    isAccessPolicy 	: XAUtil.isAccessPolicy(this.rangerPolicyType),
			    isRowFilterPolicy	: XAUtil.isRowFilterPolicy(this.rangerPolicyType),
			};
		},
		ui : {
			selectGroups	: '[data-js="selectGroups"]',
			selectUsers		: '[data-js="selectUsers"]',
			addPerms		: 'a[data-js="permissions"]',
			maskingType		: 'a[data-js="maskingType"]',
			rowLeveFilter	: 'a[data-js="rowLeveFilter"]',
			conditionsTags	: '[class=tags1]',
			delegatedAdmin	: 'input[data-js="delegatedAdmin"]',
			addPermissionsSpan : '.add-permissions',
			addConditionsSpan  : '.add-conditions',
			addMaskingTypeSpan : '.add-masking-type',
			addRowFilterSpan   : '.add-row-filter',
                        selectRoles : '[data-js="selectRoles"]'

		},
		events : {
			'click [data-action="delete"]'	: 'evDelete',
			'click [data-js="delegatedAdmin"]'	: 'evClickTD',
			'change [data-js="selectGroups"]': 'evSelectGroup',
			'change [data-js="selectUsers"]': 'evSelectUser',
                        'change input[class="policy-conditions"]'	: 'policyCondtionChange',
                        'change [data-js="selectRoles"]': 'evSelectRoles',
		},

		initialize : function(options) {
			_.extend(this, _.pick(options,'accessTypes','policyConditions','rangerServiceDefModel','rangerPolicyType','rangerPolicyModel','storeResourceRef'));
			this.setupPermissionsAndConditions();
			this.accessPermSetForTagMasking = false;
			Vent.on('resourceType:change', this.renderPerms, this);
		},
 
		onRender : function() {
			//To setup permissions for edit mode 
			this.setupFormForEditMode();
			//create select2 dropdown for groups and users  
                        this.createDropDown(this.ui.selectGroups, 'groups');
                        this.createDropDown(this.ui.selectUsers, 'users');
                        this.createDropDown(this.ui.selectRoles, 'roles');
			//groups or users select2 dropdown change vent 
			this.dropDownChange(this.ui.selectGroups);
			this.dropDownChange(this.ui.selectUsers);
                        this.dropDownChange(this.ui.selectRoles);
			//render permissions and policy conditions
			if(XAUtil.isTagBasedDef(this.rangerServiceDefModel)){
				this.renderPermsForTagBasedPolicies();
//				if(XAUtil.isMaskingPolicy(this.rangerPolicyType)) this.renderMaskingTypesForTagBasedPolicies();
			} else {
//				To handle scenario : Access permission doesnt changes if we change resource before adding new policy item.
				this.renderPerms(this.storeResourceRef.changeType, this.storeResourceRef.value,
						this.storeResourceRef.resourceName, this.storeResourceRef.e );
				if(XAUtil.isMaskingPolicy(this.rangerPolicyType)){
					this.renderMaskingType();
				}
			}
			this.renderPolicyCondtion();
				
			if(XAUtil.isRowFilterPolicy(this.rangerPolicyType))	this.renderRowLevelFilter();
			
		},
		setupFormForEditMode : function() {
			var permTypes = this.accessTypes;
			
			this.accessItems = _.map(permTypes, function(perm){ 
				if(!_.isUndefined(perm)) return {'type':perm.name, isAllowed : false}
			});
			if(this.model.has('editMode') && this.model.get('editMode')){
				if(!_.isUndefined(this.model.get('groupName')) && !_.isNull(this.model.get('groupName'))){
					this.ui.selectGroups.val(_.map(this.model.get('groupName'), function(name){ return _.escape(name); }));
				}
				if(!_.isUndefined(this.model.get('userName')) && !_.isNull(this.model.get('userName'))){
					this.ui.selectUsers.val(_.map(this.model.get('userName'), function(name){ return _.escape(name); }));
				}
                                if(!_.isUndefined(this.model.get('roleName')) && !_.isNull(this.model.get('roleName'))){
                                        this.ui.selectRoles.val(_.map(this.model.get('roleName'), function(name){ return _.escape(name); }));
                                }
				if(!_.isUndefined(this.model.get('conditions'))){
					_.each(this.model.get('conditions'), function(obj){
						this.$el.find('input[data-js="'+obj.type+'"]').val(obj.values.toString())
					},this);
				}
				_.each(this.model.get('accesses'), function(p){
					if(p.isAllowed){
                                                this.$el.find('input[data-name="' + p.type + '"]').prop('checked', true);
						_.each(this.accessItems,function(obj){ if(obj.type == p.type) obj.isAllowed=true;})
					}
				},this);
				
				if(!_.isUndefined(this.model.get('delegateAdmin')) && this.model.get('delegateAdmin')){
                                        this.ui.delegatedAdmin.prop('checked', true);
				}
				if(this.model.has('delegateAdmin') && !this.model.get('delegateAdmin')){
				    this.model.unset('delegateAdmin');
				}
				if(!_.isUndefined(this.model.get('rowFilterInfo')) && !_.isUndefined(this.model.get('rowFilterInfo').filterExpr)){
					this.rowFilterExprVal = this.model.get('rowFilterInfo').filterExpr
				}
			}
		},
		setupPermissionsAndConditions : function() {
			var that = this;
			this.permsIds = [], this.conditions = {};
			//Set Permissions obj
			if( this.model.has('editMode') && this.model.get('editMode')){
				_.each(this.model.get('accesses'), function(p){
					if(p.isAllowed){
						var access = _.find(that.accessTypes,function(obj){if(obj.name == p.type) return obj});
						this.permsIds.push(access.name);
					}
					
				}, this);
				//Set PolicyCondtion Obj to show in edit mode
				_.each(this.model.get('conditions'), function(p){
					this.conditions[p.type] = p.values;
				}, this);
			}
		},
		dropDownChange : function($select){
			var that = this, otherName, otherName2;
			$select.on('change',function(e){
                var name = ($(e.currentTarget).attr('data-js') == that.ui.selectGroups.attr('data-js')) ? 'group' :
                                (($(e.currentTarget).attr('data-js') == that.ui.selectUsers.attr('data-js')) ? 'user' : 'role');
				var nameList = ['user', 'group', 'role'];
				nameList.splice(nameList.indexOf(name),1);
				otherName = nameList.pop();
				otherName2 = nameList.pop();

				that.checkDirtyFieldForDropDown(e);
				
				if(_.isNull(that.model.get(otherName+'Name'))){
                    that.model.unset(otherName+'Name')
                }
                if(_.isNull(that.model.get(otherName2+'Name'))){
                    that.model.unset(otherName2+'Name')
                }
				if(e.removed != undefined){
					var gNameArr = [];
					if(that.model.get(name+'Name') != undefined)
						gNameArr = _.without(that.model.get(name+'Name'), e.removed.text);
					if(!_.isEmpty(gNameArr)){
						that.model.set(name+'Name',gNameArr);
					} else {
						that.model.unset(name+'Name');
					}
					return;
				}
				if(!_.isUndefined(e.added)){
					var nameList = _.map($(e.currentTarget).select2("data"), function(obj){return obj.text});
					that.model.set(name+'Name', nameList);
				}
			});
		},
                createDropDown :function($select, type){
			var that = this, tags = [],
                        placeholder = (type == 'users') ? 'Select Users' : ((type == 'groups') ? 'Select Groups' : 'Select Roles'),
                        searchUrl   = (type == 'users') ? "service/xusers/lookup/users" : ((type == 'groups') ? "service/xusers/lookup/groups" : "service/roles/roles");
			if(this.model.has('editMode') && !_.isEmpty($select.val())){
                                var temp = this.model.attributes[ (type == 'users') ? 'userName' : ((type == 'groups') ? 'groupName' : 'roleName')];
				_.each(temp , function(name){
					tags.push( { 'id' : _.escape( name ), 'text' : _.escape( name ) } );
				});
			}
			$select.select2({
				closeOnSelect : true,
				placeholder : placeholder,
				width :'290px',
				tokenSeparators: [",", " "],
				tags : true,
				initSelection : function (element, callback) {
					callback(tags);
				},
				ajax: {
					url: searchUrl,
					dataType: 'json',
					data: function (term, page) {
						if(type === 'roles') {
							return {
								roleNamePartial: term
							}
						} else {
							return {
								name: term,
								isVisible : XAEnums.VisibilityStatus.STATUS_VISIBLE.value,
							}
						}
					},
					results: function (data, page) {
						var results = [] , selectedVals = [];
						//Get selected values of groups/users dropdown
                                                selectedVals = that.getSelectedValues($select, type);
						if(data.totalCount != "0"){
                                                        if(type == 'users' || type == 'groups'){
								results = data.vXStrings.map(function(m){	return {id : _.escape(m.value), text: _.escape(m.value) };	});
							} else {
                                                                results = data.roles.map(function(m){	return {id : _.escape(m.name), text: _.escape(m.name) };	});
							}
							if(!_.isEmpty(selectedVals)){
								results = XAUtil.filterResultByText(results, selectedVals);
							}
							return {results : results};
						}
						return {results : results};
					},
                    transport: function (options) {
			$.ajax(options).fail(function(respones) {
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
				formatNoMatches: function(result){
                                        return (type == 'users') ? 'No user found.' : ((type == 'groups') ? 'No group found.' : 'No role found');
				}
			}).on('select2-focus', XAUtil.select2Focus);
		},
		renderPerms :function(changeType, value, resourceName, e){
	        var that = this , accessTypeByResource = this.accessTypes;
	        this.storeResourceRef.changeType = changeType;
	        this.storeResourceRef.value = value;
	        this.storeResourceRef.resourceName = resourceName;
	        this.storeResourceRef.e = e;
	        //get permissions by resource only for access policy
	        accessTypeByResource = this.getAccessPermissionForSelectedResource(changeType, value, resourceName, e);
	        //reset permissions on resource change
	        if(this.permsIds.length > 0 && !_.isUndefined(changeType) && !_.isUndefined(resourceName)){
	                this.permsIds = [];
	        }
	        this.perms =  _.map(accessTypeByResource , function(m){return {text : m.label, value : m.name};});
			if(this.perms.length > 1){
				this.perms.push({'value' : -1, 'text' : 'Select/Deselect All'});
			}
			//if policy items not present. its skip that items and move forward
			if(_.isObject(this.ui.addPerms)){
                                if(changeType){
                                    this.ui.addPerms.editable("destroy");
                                }
				//create x-editable for permissions
				this.ui.addPerms.editable({
					emptytext : 'Add Permissions',
					source: this.perms,
					value : this.permsIds,
					display: function(values,srcData) {
                        if(_.isNull(values) || _.isEmpty(values) || (_.contains(values,"-1")  &&  values.length == 1)){
                            $(this).empty();
                            that.model.unset('accesses');
                            that.ui.addPermissionsSpan.find('i').attr('class', 'fa-fw fa fa-plus');
                            that.ui.addPermissionsSpan.attr('title','add');
                            return;
						}
                        if(_.contains(values,"-1")){
                        	values = _.without(values,"-1")
						}
                        //that.checkDirtyFieldForGroup(values);
                        var permTypeArr = [];
                        var valArr = _.map(values, function(id){
                        	if(!_.isUndefined(id)){
                        		var obj = _.findWhere(that.rangerServiceDefModel.attributes.accessTypes,{'name' : id});
								permTypeArr.push({permType : obj.value});
								return "<span class='badge badge-info'>" + obj.label + "</span>";
                        	}
                        });
                        var items=[];
                        _.each(that.accessItems, function(item){
							if($.inArray( item.type, values) >= 0){
								item.isAllowed = true;
								items.push(item) ;
							}
                        },this);
                        // Save form data to model
                        that.model.set('accesses', items);
                        $(this).html(valArr.join(" "));
                        that.ui.addPermissionsSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
                        that.ui.addPermissionsSpan.attr('title','edit');
					},
                                }).on('shown', function(e, editable) {
                                    that.clickOnPermissions(that);
				});
				that.ui.addPermissionsSpan.click(function(e) {
				e.stopImmediatePropagation();
				that.$('a[data-js="permissions"]').editable('toggle');
				that.clickOnPermissions(that);
				});
			}
		},
		getAccessPermissionForSelectedResource : function(changeType, value, resourceName, e){
			var resourceDef = _.sortBy(XAUtil.policyTypeResources(this.rangerServiceDefModel , this.rangerPolicyType), function(m){ return m.itemId }),
	        policyResources = this.rangerPolicyModel.get('resources'),
	        accessTypeByResource = this.accessTypes,
	        resourceByLevelObj = _.groupBy(resourceDef, function(m){
	        	return ( _.isUndefined(m.parent) || m.parent == "" ) ? 'parent' : 'child';
	        });
	        var parentResources = resourceByLevelObj['parent'];
	        //find resource child resource edit form opens
	        if(!_.isUndefined(policyResources) && _.isUndefined(changeType)){
	        	var parentFound = false, parentName = undefined, resourceNames = _.keys(policyResources);
			// if one resource is selected that means which is parent one else find
			if(resourceNames.length == 1){
				resourceName = resourceNames[0];
			}else{
				_.each(resourceNames, function(m){
					var parentDef = _.findWhere(resourceByLevelObj['parent'], {'name': m});
					if(parentDef){
						parentFound = true;
						parentName = m;
					}
				});
				for(var i=0;i< resourceDef.length ;i++){
					if($.inArray(resourceDef[i]['name'], resourceNames) >= 0)
						if(resourceDef[i]['parent'] == parentName && resourceDef[i]['accessTypeRestrictions']){
							parentName = resourceDef[i]['name'];
							i = 0;
						}
				}
				resourceName = parentName;
			}
			var allowAccessName = _.findWhere(resourceDef, {'name': resourceName}).accessTypeRestrictions;
			accessTypeByResource = _.filter(accessTypeByResource, function(m){ return _.contains(allowAccessName , m.name)});
	        }
	        if(!resourceName){
	        	resourceName = parentResources[0].name;
	        }
	        if(resourceName == 'none'){
	        	resourceName = $(e.currentTarget).parents('div[data-name="field-none"]').attr('parent');
	        }
	        var parentResourceDef = _.findWhere(resourceByLevelObj['parent'], {'name':resourceName });
	        if(!_.isUndefined(parentResourceDef)){
			    if(_.isArray(parentResourceDef)){
				var foundParent = false, parentName = '';
				for(var i=0;i<resourceDef.length;i++){
					if(resourceDef[i].parent == "" && !foundParent){
						accessTypeByResource = _.filter(accessTypeByResource, function(m){ return $.inArray(m.name, resourceDef[i].accessTypeRestrictions)});
						foundParent = true;
						parentName = resourceDef[i]['name'];
					}
				}
			}else{
				//On Parent change
				if(parentResourceDef.isValidLeaf){
					accessTypeByResource = _.filter(accessTypeByResource, function(m){ return _.contains(parentResourceDef.accessTypeRestrictions , m.name)});
				}else{
					var childResourceDef = this.childRscDef(resourceByLevelObj['child'] , resourceName);
					accessTypeByResource = _.filter(accessTypeByResource, function(m){ return _.contains(childResourceDef.accessTypeRestrictions , m.name)});
				}
			}
	        }else{
				if(changeType == 'resourceType'){
					var def = _.findWhere(resourceDef, {'name': resourceName});
					if(def.isValidLeaf){
						accessTypeByResource = _.filter(accessTypeByResource, function(m){ return _.contains(def.accessTypeRestrictions , m.name)});
					}else{
						var childResourceDef = this.childRscDef(resourceByLevelObj['child'] , resourceName);
						accessTypeByResource = _.filter(accessTypeByResource, function(m){ return _.contains(childResourceDef.accessTypeRestrictions , m.name)});
					}
				} 
	        }
	        if(_.isEmpty(accessTypeByResource)){
			accessTypeByResource = this.accessTypes;
	        }
	        return accessTypeByResource;
		},
		//if parent isValidLeaf is false than check child isvalidLeaf
		childRscDef:function(resChild , rscName, rscDef){
			var childResourcs = _.filter(resChild, function(m){
				return m.parent == rscName 
			});
			if(!_.isEmpty(childResourcs)){
				var someVal;
				someVal = _.some(childResourcs,function(obj){
				//help of this we separate specified(selected) child resource from all childResourcs
					var $html = $('[data-name="field-'+obj.name+'"]');
					if($html.length > 0){
						rscName = obj.name;
						rscDef = obj;
						return true;
					}
				});
				if(!someVal){
					rscDef = childResourcs[0];
					rscName = childResourcs[0].name;
				}
			}
			// resource-node have isValidLeaf is true and resource have child node then render that child node permission
			if(rscDef && rscDef.isValidLeaf && !this.model.has('editMode')) {
				var hasChiled = _.filter(resChild, function(m){
					return m.parent == rscName
				});
				if(!_.isEmpty(hasChiled)) {
					rscDef = hasChiled[0];
					rscName = hasChiled[0].name;
					this.childRscDef(resChild , rscName, rscDef);
				}
			}
			return  ((rscDef.isValidLeaf) ? _.findWhere(resChild, {'name':rscName }) : this.childRscDef(resChild , rscName))
		},
		renderPermsForTagBasedPolicies :function(){
			var that = this;
			this.ui.addPerms.attr('data-type','tagchecklist')
			this.ui.addPerms.attr('title','Components Permissions')
			this.ui.delegatedAdmin.parent('td').hide();
			this.perms =  _.map(this.accessTypes,function(m){return {text:m.label, value:m.name};});
			var select2optn = { width :'600px' };
			if(XAUtil.isMaskingPolicy(this.rangerPolicyType)){
				select2optn = {width :'600px' , maximumSelectionSize : 1 };
			}
			//create x-editable for permissions
			this.ui.addPerms.editable({
			    emptytext : 'Add Permissions',
				source: this.perms,
				value : this.permsIds,
				select2option : select2optn,
				placement : 'top',
				showbuttons : 'bottom',
				display: function(values,srcData) {
					if(_.contains(values,"on"))	values = _.without(values,"on");
					if(_.isNull(values) || _.isEmpty(values)){
						$(this).empty();
						that.model.unset('accesses');
						that.ui.addPermissionsSpan.find('i').attr('class', 'fa-fw fa fa-plus');
						that.ui.addPermissionsSpan.attr('title','add');
						//disable Masking option for tag based
						if(XAUtil.isMaskingPolicy(that.rangerPolicyType)){
							that.accessPermSetForTagMasking = false;
							that.model.unset('dataMaskInfo');
							that.renderMaskingTypesForTagBasedPolicies();
							that.$el.find('input[data-id="maskTypeCustom"]').val("");
						}
						return;
					}
					//To remove selectall options
					values = _.uniq(values);
					if(values.indexOf("selectall") >= 0){
						values.splice(values.indexOf("selectall"), 1)
					}
//			    	that.checkDirtyFieldForGroup(values);
					
					var permTypeArr = [];
					var valArr = _.map(values, function(id){
						if(!_.isUndefined(id)){
							var obj = _.findWhere(srcData,{'value' : id});
							permTypeArr.push({permType : obj.value});
							return "<span class='badge badge-info'>" + id.substr(0,id.indexOf(":")).toUpperCase() + "</span>";
						}
					});
					var items=[];
					_.each(that.accessItems, function(item){ 
						if($.inArray( item.type, values) >= 0){
							item.isAllowed = true;
							items.push(item) ;
						}
					},this);
					// Save form data to model
					that.model.set('accesses', items);
					$(this).html(_.uniq(valArr).join(" "));
					that.ui.addPermissionsSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
					that.ui.addPermissionsSpan.attr('title','edit');
					
					//enabling add masking option for Tag-based
					if(XAUtil.isMaskingPolicy(that.rangerPolicyType)){
						that.accessPermSetForTagMasking = true;
						var selectedComponent = _.map(items, function(m){ return m.type.substr(0,m.type.indexOf(":")); });
						selectedComponent = _.uniq(selectedComponent);
						that.renderMaskingTypesForTagBasedPolicies(selectedComponent)
					}
				},
			}).on('hidden',function(e){
					// $(e.currentTarget).parent().find('.tag-condition-popover').remove()
					$('.popover').parent().remove()
			}).on('click', function(e) {
				e.stopPropagation();
				if($('.popover')){
					$('.tag-condition-popover').remove()
				}
//				e.preventDefault();
//				that.clickOnPermissions(that);
				 //Sticky popup
				var pop = $('.popover')
				pop.wrap('<div class="tag-fixed-popover-wrapper"></div>');
				pop.addClass('tag-fixed-popover');
				pop.find('.arrow').removeClass('arrow')
			});
			that.ui.addPermissionsSpan.click(function(e) {
				e.stopPropagation();
				if($('.popover')){
					$('.tag-condition-popover').remove()
				}
				that.$('a[data-js="permissions"]').editable('toggle');
//				that.clickOnPermissions(that);
				
				var pop = $('.popover')
				pop.wrap('<div class="tag-fixed-popover-wrapper"></div>');
				pop.addClass('tag-fixed-popover');
				pop.find('.arrow').removeClass('arrow')
			});
			
		},
		renderMaskingTypesForTagBasedPolicies :function(accessTypeSelectedComp){
			var that = this, perms = [];
			this.ui.addPerms.attr('data-type','radiolist')
			this.ui.addPerms.attr('title','Components Permissions')
//			this.ui.delegatedAdmin.parent('td').hide();
			
			var maskingTypes = this.rangerServiceDefModel.get('dataMaskDef').maskTypes;
			//get selected components masking types
			_.each(maskingTypes,function(m){
				var compName = m.name.substr(0,m.name.indexOf(":"));
				if($.inArray(compName, accessTypeSelectedComp) >= 0){
					perms.push({text:m.label, value:m.name});
				}
			}, this);
			var maskTypeVal =  [];
			if(!_.isUndefined(this.model.get('dataMaskInfo')) && !_.isUndefined(this.model.get('dataMaskInfo').dataMaskType)){
				maskTypeVal = this.model.get('dataMaskInfo').dataMaskType;
				if(!_.isUndefined(accessTypeSelectedComp) && !_.isUndefined(maskTypeVal)){
					maskTypeVal = $.inArray(maskTypeVal.substr(0,maskTypeVal.indexOf(":")), accessTypeSelectedComp) >= 0 ? maskTypeVal : [];
				}
			}
                        if(accessTypeSelectedComp || _.isEmpty(accessTypeSelectedComp)){
                            this.ui.maskingType.editable("destroy");
                        }
                        that.ui.addMaskingTypeSpan.unbind( "click" );
			this.$el.find('input[data-id="maskTypeCustom"]').unbind( "change" );
			that.ui.addMaskingTypeSpan.find('i').attr('class', 'fa-fw fa fa-plus');
			that.ui.addMaskingTypeSpan.attr('title','add');
                        this.$el.find('input[data-id="maskTypeCustom"]').css("display","none");

			//create x-editable for permissions
			this.ui.maskingType.editable({
			    emptytext : 'Add Mask Type',
				source: perms,
				value : maskTypeVal,
				placement : 'top',
				showbuttons : 'bottom',
				disabled : !this.accessPermSetForTagMasking,
				display: function(value,srcData) {
					if(_.isNull(value) || _.isUndefined(value) || _.isEmpty(value)){
						$(this).empty();
						that.ui.addPermissionsSpan.find('i').attr('class', 'fa-fw fa fa-plus');
						that.ui.addPermissionsSpan.attr('title','add');
						return;
					}
					
					var obj = _.findWhere(srcData, {'value' : value } );
					// Save form data to model
					that.model.set('dataMaskInfo', {'dataMaskType': value });
					//Custom dataMaskType
					if(value.indexOf("CUSTOM") >= 0){
						$(this).siblings('[data-id="maskTypeCustom"]').css("display","");
					}else{
						$(this).siblings('[data-id="maskTypeCustom"]').css("display","none");
						$(this).siblings('[data-id="maskTypeCustom"]').val(" ");
					}
					
					$(this).html("<span class='badge badge-info'>"+ value.substr(0,value.indexOf(":")).toUpperCase() +" : "
							+ obj.text +"</span>");
					that.ui.addMaskingTypeSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
					that.ui.addMaskingTypeSpan.attr('title','edit');
				},
			}).on('hide',function(e){
					//$(e.currentTarget).parent().find('.tag-fixed-popover-wrapper').remove()
					$('.popover').parent().remove()
                        }).on('click', function(e) {
                            e.stopPropagation();
                            e.preventDefault();
                        });
			that.ui.addMaskingTypeSpan.click(function(e) {
				e.stopPropagation();
                                 e.preventDefault();
				if(!that.accessPermSetForTagMasking){
					XAUtil.alertPopup({ msg :localization.tt('msg.pleaseSelectAccessTypeForTagMasking') });
					return;
				}
				that.$('a[data-js="maskingType"]').editable('toggle');
			});
			this.$el.find('input[data-id="maskTypeCustom"]').on('change', function(e){
				if(!_.isUndefined(that.model.get('dataMaskInfo'))){
                                        that.model.get('dataMaskInfo').valueExpr = (e.currentTarget.value);
				}
			}).trigger('change');
			if(!this.accessPermSetForTagMasking){
				that.ui.maskingType.html('Add Mask Type');
			}
		},
		clickOnPermissions : function(that) {
			var selectAll = true;
			var checklist = $('.editable-checklist').find('input[type="checkbox"]')
            _.each(checklist,function(checkbox){
                if($(checkbox).val() != -1 && !$(checkbox).is(':checked'))
                    selectAll = false;
            })
			if(selectAll){
				$('.editable-checklist').find('input[type="checkbox"][value="-1"]').prop('checked',true)
			} else {
				$('.editable-checklist').find('input[type="checkbox"][value="-1"]').prop('checked',false)
			}
			//for selectAll functionality
                        $('input[type="checkbox"][value="-1"]').click(function(e){
                var checkboxlist =$(this).closest('.editable-checklist').find('input[type="checkbox"][value!=-1]')
                $(this).is(':checked') ? checkboxlist.prop('checked',true) : checkboxlist.prop('checked',false);
            });
			$('.editable-checklist input[type="checkbox"]').click(function(e){
				if(this.value!="-1"){
					var selectAll = true;
					$('.editable-checklist input[type="checkbox"]').each(function(index,item){
						if(item.value!="-1" && !item.checked){
							selectAll = false;
						}
					});
					$('input[type="checkbox"][value="-1"]').prop('checked',selectAll);
				}
			});
		},
		renderPolicyCondtion : function() {
			var that = this;
			
			if(this.policyConditions.length > 0){
				var tmpl = _.map(this.policyConditions,function(obj){
					if(!_.isUndefined(obj.evaluatorOptions) && !_.isUndefined(obj.evaluatorOptions['ui.isMultiline']) && Boolean(obj.evaluatorOptions['ui.isMultiline'])){
						return '<div class="editable-address margin-bottom-5">\
						            <label style="display:block !important;">\
						                <span>'+obj.label+' : </span>\
						                <i title="'+localization.tt('validationMessages.jsValidationMsg')+'" class="fa-fw fa fa-info-circle" style="float: right;margin-top: 6px;"></i>\
						            </label>\
						            <textarea class="textAreaContainer" name="'+obj.name+'" placeholder="Please enter condition.."></textarea>\
						            <div class="jsValidation">\
						                <a href="javascript:;"class="jsValidationCheck btn btn-defult btn-sm" style="margin: 5px">Syntax check</a>\
						            </div>\
						       </div>'
					}
					return '<div class="editable-address margin-bottom-5"><label style="display:block !important;"><span>'+obj.label+' : </span></label><input type="text" name="'+obj.name+'" ></div>'
						
				});
				//to show only mutiline line policy codition 
				this.multiLinecond = _.filter(that.policyConditions, function(m){ return (!_.isUndefined(m.evaluatorOptions['ui.isMultiline']) && m.evaluatorOptions['ui.isMultiline']) });
				this.multiLinecond = _.isArray(this.multiLinecond) ? this.multiLinecond : [this.multiLinecond];
				//get the select input size(for bootstrap x-editable) of policy conditions
				var selectSizeList = [];
                _.each(this.policyConditions,function(policyCondition){
                	if (XAUtil.isSinglevValueInput(policyCondition)) {
                		selectSizeList.push(1);
                	} else {
                		selectSizeList.push(undefined);
                	}
                });
              //Create new bootstrap x-editable `policyConditions` dataType for policy conditions
                XAUtil.customXEditableForPolicyCond(tmpl.join(''),selectSizeList);
				//create x-editable for policy conditions
				this.$('#policyConditions').editable({
					emptytext : 'Add Conditions',
					value : this.conditions,
					display: function(value) {
						var continue_ = false, i = 0, cond = [];
						if(!value) {
							$(this).empty();
							return; 
						}
						_.each(value, function(val, name){ if(!_.isEmpty(val)) continue_ = true; });
						
						if(continue_){
							//Generate html to show on UI
							var html = _.map(value, function(val,name) {
								var label = (i%2 == 0) ? 'badge badge-dark' : 'label';
								if(_.isEmpty(val)){
									return ''; 
								}
								//Add label for policy condition
								var pcond = _.findWhere(that.multiLinecond, { 'name': name});
								if(!_.isUndefined(pcond) && !_.isUndefined(pcond['evaluatorOptions']) 
										&& ! _.isUndefined(pcond['evaluatorOptions']["ui.isMultiline"]) 
										&& ! _.isUndefined(pcond['evaluatorOptions']['engineName'])){
									cond.push({ 'type' : name, 'values' : !_.isArray(val) ? [val] : val });
									val = 	pcond['evaluatorOptions']['engineName'] + ' Condition';
								} else {
									cond.push({ 'type' : name, 'values' : !_.isArray(val) ?  val.split(',') : val });
								}
								i++;
                                return '<span class="'+label+' white-space-normal" >'+name+' : '+ _.escape(val) + '</span>';
							});
							that.model.set('conditions', cond);
							$(this).html(html);
							that.ui.addConditionsSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
							that.ui.addConditionsSpan.attr('title','edit');
						} else {
							that.model.unset('conditions');
							$(this).empty();
							that.ui.addConditionsSpan.find('i').attr('class', 'fa-fw fa fa-plus');
							that.ui.addConditionsSpan.attr('title','add');
						}
					},
					validate:function(value){
					    if(that.$el.find('.textAreaContainer').hasClass('errorClass') ||
					            that.$el.find('.jsValidation span').hasClass('validSyntax')){
					        that.$el.find('.textAreaContainer').removeClass('errorClass');
					        that.$el.find('.jsValidation').find('span').remove();
                        }
					    var error = {'flag' : false};
						_.each(value, function(val, name){
							var tmp = _.findWhere(that.multiLinecond, { 'name' : name});
							if(!_.isUndefined(tmp)){
								try {
									var t = esprima.parse(val);
								}catch(e){
									if(!error.flag){
										console.log(e.message)
										error.flag = true;
										error.message = e.message;
										error.fieldName = name;
									}
								}
							}
						})
						$('.editableform').find('.editable-error-block').remove();
						if(error.flag){
							$('.editableform').find('.editable-error-block').remove();
							$('.editableform').find('[name="'+error.fieldName+'"]').parent().append('<div class="editable-error-block help-block" style="display: none;"></div>')
							return error.message;
						}
				    },
				}).on('shown' , function(e, editable) {
				    $('.popover').addClass('tag-condition-popover');
				    $('.jsValidationCheck').on('click',function(e){
				        e.stopPropagation();
				        var $textArea = $(e.currentTarget).parent().parent();
				        if($('.editableform div.form-group').hasClass('error')){
				            $('.editableform div.form-group').removeClass('error');
				            $('.editable-error-block').remove();
				        }
				        if($textArea.find('.textAreaContainer').hasClass('errorClass') || $textArea.find('.jsValidation span').hasClass('validSyntax')){
				            $textArea.find('.textAreaContainer').removeClass('errorClass');
				            $textArea.find('.jsValidation').find('span').remove();
				        }
				        var tmp = $textArea.find('textarea').val();
                        if(!_.isUndefined(tmp) && ! _.isEmpty(tmp)){
				            var errorMessage;
                            try {
                                var t = esprima.parse(tmp);
                            }catch(e){
                                errorMessage = e.message;
                            }
                            if(!_.isUndefined(errorMessage)){
                                $textArea.find('.textAreaContainer').addClass('errorClass');
                                $textArea.find('.jsValidation').append('<span class="text-color-red">'+errorMessage+'</span>');
                            }else{
                                $textArea.find('.jsValidation').append('<span class="validSyntax">valid javascript condition</span>');
                            }
                        }
				    })
				});
				that.ui.addConditionsSpan.click(function(e) {
					e.stopPropagation();
					that.$('#policyConditions').editable('toggle');
					$('.popover').addClass('tag-condition-popover');
				});
			}else{
			    that.model.unset('conditions');
			}
		},
                getSelectedValues : function($select, type){
			var vals = [],selectedVals = [];
                        var name = (type == 'users') ? 'user' : ((type == 'groups') ? 'group' : 'role');
			if(!_.isEmpty($select.select2('data'))){
				selectedVals = _.map($select.select2('data'),function(obj){ return obj.text; });
			}
			vals.push.apply(vals , selectedVals);
			vals = $.unique(vals);
			return vals;
		},
		evDelete : function(){
			var that = this;
			this.collection.remove(this.model);
		},
		evClickTD : function(e){
			var $el = $(e.currentTarget);
			XAUtil.checkDirtyFieldForToggle($el);
			//Set Delegated Admin value 
			if($el.is(':checked')){
			    this.model.set('delegateAdmin',$el.is(':checked'));
			}else{
			    this.model.unset('delegateAdmin');
			}
			//select/deselect all functionality
			if(this.checkAll($el.find('input[type="checkbox"][value!="-1"]'))){
				$el.find('input[type="checkbox"][value="-1"]').prop('checked', true)
			} else {
			    $el.find('input[type="checkbox"][value="-1"]').prop('checked', false)
			}

		},
		checkAll : function($inputs){
			 var checkall = true;
			 $inputs.each(function(idx, input){
			    if(!checkall)   return;
			 	checkall = $(input).is(':checked') ? true : false
			 });
			 return checkall;
		},
		checkDirtyFieldForCheckBox : function(perms){
			var permList = [];
			if(!_.isUndefined(this.model.get('_vPermList')))
				permList = _.map(this.model.attributes._vPermList,function(obj){return obj.permType;});
			perms = _.map(perms,function(obj){return obj.permType;});
			XAUtil.checkDirtyField(permList, perms, this.$el);
		},
		policyCondtionChange :function(e){
			if(!_.isEmpty($(e.currentTarget).val()) && !_.isEmpty(this.policyConditions)){
				var policyCond = { 'type' : $(e.currentTarget).attr('data-js'), 'value' : $(e.currentTarget).val() } ;
				var conditions = [];
				if(this.model.has('conditions')){
					conditions = this.model.get('conditions')
				}
				conditions.push(policyCond);
				this.model.set('conditions',conditions);
			}
				
		},
		checkDirtyFieldForDropDown : function(e){
			//that.model.has('groupId')
			var groupIdList =[];
			if(!_.isUndefined(this.model.get('groupId')))
				groupIdList = this.model.get('groupId').split(',');
			XAUtil.checkDirtyField(groupIdList, e.val, $(e.currentTarget));
		},
		renderMaskingType :function(){
			var that = this, maskingTypes = [];
			this.maskTypeIds =  [];
			if(!_.isUndefined(this.model.get('dataMaskInfo')) && !_.isUndefined(this.model.get('dataMaskInfo').dataMaskType)){
				this.maskTypeIds = this.model.get('dataMaskInfo').dataMaskType
			}
			
			if(!_.isUndefined(this.rangerServiceDefModel.get('dataMaskDef')) && !_.isUndefined(this.rangerServiceDefModel.get('dataMaskDef').maskTypes)){
				maskingTypes = this.rangerServiceDefModel.get('dataMaskDef').maskTypes;
			}
			this.maskTypes =  _.map(maskingTypes, function(m){return {text:m.label, value : m.name };});
			//create x-editable for permissions
			this.ui.maskingType.editable({
			    emptytext : localization.tt('lbl.selectMaskingOption'),
				source: this.maskTypes,
				value : this.maskTypeIds,
				display: function(value,srcData) {
					if(_.isNull(value) || _.isEmpty(value)){
						$(this).empty();
						that.model.unset('dataMaskInfo');
						that.ui.addMaskingTypeSpan.find('i').attr('class', 'fa-fw fa fa-plus');
						that.ui.addMaskingTypeSpan.attr('title','add');
						return;
					}
					
					var obj = _.findWhere(srcData, {'value' : value } );
					// Save form data to model
					that.model.set('dataMaskInfo', {'dataMaskType': value });
					//Custom dataMaskType
					if(value === "CUSTOM"){
						$(this).siblings('[data-id="maskTypeCustom"]').css("display","");
					}else{
						$(this).siblings('[data-id="maskTypeCustom"]').css("display","none");
                                                $(this).siblings('[data-id="maskTypeCustom"]').val(" ")
					}
					
					$(this).html("<span class='badge badge-info'>" + obj.text + "</span>");
					that.ui.addMaskingTypeSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
					that.ui.addMaskingTypeSpan.attr('title','edit');
				},
			}).on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
			});
			that.ui.addMaskingTypeSpan.click(function(e) {
				e.stopPropagation();
				that.$('a[data-js="maskingType"]').editable('toggle');
			});
			this.$el.find('input[data-id="maskTypeCustom"]').on('change', function(e){
				if(!_.isUndefined(that.model.get('dataMaskInfo'))){
                                        that.model.get('dataMaskInfo').valueExpr = (e.currentTarget.value);
				}
			}).trigger('change');
		},
		renderRowLevelFilter :function(){
			var that = this;
			//create x-editable for permissions
			this.ui.rowLeveFilter.editable({
			    emptytext : 'Add Row Filter',
			    placeholder : 'enter expression',	
				value : this.rowFilterExprVal,
				display: function(value,srcData) {
					if(_.isNull(value) || _.isEmpty(value)){
						$(this).empty();
						that.model.unset('rowFilterInfo');
						that.ui.addRowFilterSpan.find('i').attr('class', 'fa-fw fa fa-plus');
						that.ui.addRowFilterSpan.attr('title','add');
						return;
					}	
					that.model.set('rowFilterInfo', {'filterExpr': value });
					$(this).html("<span class='badge badge-info'>" + _.escape(value) + "</span>");
					that.ui.addRowFilterSpan.find('i').attr('class', 'fa-fw fa fa-pencil');
					that.ui.addRowFilterSpan.attr('title','edit');
				},
			}).on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
			});
			that.ui.addRowFilterSpan.click(function(e) {
				e.stopPropagation();
				that.$('a[data-js="rowLeveFilter"]').editable('toggle');
			});
			
		},

	});

	return Backbone.Marionette.CompositeView.extend({
		_msvName : 'PermissionItemList',
		template : require('hbs!tmpl/policies/PermissionList'),
		templateHelpers :function(){
			return {
				permHeaders : this.getPermHeaders(),
				headerTitle : this.headerTitle
			};
		},
		getItemView : function(item){
			if(!item){
				return;
			}
			return PermissionItem;
		},
		itemViewContainer : ".js-formInput",
		itemViewOptions : function() {
			//set access type by policy type
			this.setAccessTypeByPolicyType();
			return {
				'collection' 	: this.collection,
				'accessTypes'	: this.accessTypes,
				'policyConditions' : this.rangerServiceDefModel.get('policyConditions'),
				'rangerServiceDefModel' : this.rangerServiceDefModel,
                'rangerPolicyType' : this.rangerPolicyType,
                'storeResourceRef' :  this.storeResourceRef,
                'rangerPolicyModel' : this.model,
			};
		},
		events : {
			'click [data-action="addGroup"]' : 'addNew'
		},
		initialize : function(options) {
			_.extend(this, _.pick(options, 'accessTypes','rangerServiceDefModel', 'headerTitle','rangerPolicyType'));
			if(this.collection.length == 0){
				this.collection.add(new Backbone.Model())
			}
			this.storeResourceRef = {};
		},
		onRender : function(){
			this.makePolicyItemSortable();
		},

		addNew : function(){
			var that =this;
			this.collection.add(new Backbone.Model());
		},
		getPermHeaders : function(){
			var permList = [];
			if(XAUtil.isAccessPolicy(this.rangerPolicyType) ){
				if(this.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label){
					permList.unshift(localization.tt('lbl.delegatedAdmin'));
					permList.unshift(localization.tt('lbl.permissions'));
				}else{
					permList.unshift(localization.tt('lbl.componentPermissions'));
				}
			}
			if(XAUtil.isRowFilterPolicy(this.rangerPolicyType)){
				permList.unshift(localization.tt('lbl.rowLevelFilter'));
				permList.unshift(localization.tt('lbl.accessTypes'));
			}else if(XAUtil.isMaskingPolicy(this.rangerPolicyType)){
				permList.unshift(localization.tt('lbl.selectMaskingOption'));
				permList.unshift(localization.tt('lbl.accessTypes'));
			}
			
			if(!_.isEmpty(this.rangerServiceDefModel.get('policyConditions'))){
				permList.unshift(localization.tt('h.policyCondition'));
			}
			permList.unshift(localization.tt('lbl.selectUser'));
			permList.unshift(localization.tt('lbl.selectGroup'));
                        permList.unshift(localization.tt('lbl.selectRole'));
			permList.push("");
			return permList;
		},
		setAccessTypeByPolicyType : function(){
			if(XAUtil.isMaskingPolicy(this.rangerPolicyType) && XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef'))){
				var dataMaskDef = this.rangerServiceDefModel.get('dataMaskDef');
				if(!_.isUndefined(dataMaskDef) && !_.isUndefined(dataMaskDef.accessTypes)){
					this.accessTypes =  _.map(dataMaskDef.accessTypes, function(m){return _.findWhere(this.accessTypes, {'name' : m.name });}, this);
				}
			}else if(XAUtil.isRowFilterPolicy(this.rangerPolicyType) && XAUtil.isRenderRowFilter(this.rangerServiceDefModel.get('rowFilterDef'))){
				var rowFilterDef = this.rangerServiceDefModel.get('rowFilterDef');
				if(!_.isUndefined(rowFilterDef) && !_.isUndefined(rowFilterDef.accessTypes)){
					this.accessTypes =  _.map(rowFilterDef.accessTypes, function(m){return _.findWhere(this.accessTypes, {'name' : m.name });}, this);
				}
			}
			
		},
		makePolicyItemSortable : function(){
			var that = this, draggedModel;
			this.$el.find(".js-formInput" ).sortable({
				placeholder: "ui-state-highlight",
				start : function(event, ui){
					var row = ui.item[0].rowIndex - 1;
					draggedModel = that.collection.at(row);
				},
				stop : function(event, ui){
					var row = ui.item[0].rowIndex -1;
					that.collection.remove(draggedModel, { silent : true});
					that.collection.add(draggedModel ,{ at: row, silent : true });
					that.$el.find(ui.item[0]).addClass("dirtyField");
				},
			});
		}
	});

});
