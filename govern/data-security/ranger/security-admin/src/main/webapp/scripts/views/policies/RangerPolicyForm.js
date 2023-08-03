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
	var XAEnums			= require('utils/XAEnums');
	var localization	= require('utils/XALangSupport');
	var XAUtil			= require('utils/XAUtils');
    
	var PermissionList 	= require('views/policies/PermissionList');
    var vPolicyTimeList 	= require('views/policies/PolicyTimeList');
	var BackboneFormDataType	= require('models/BackboneFormDataType');
    var App             = require('App');
    var vPolicyCondition = require('views/policies/RangerPolicyConditions');
	var ResourceList 	= require('views/policies/ResourceList');

	require('backbone-forms.list');
	require('backbone-forms.templates');
	require('backbone-forms');
	require('backbone-forms.XAOverrides');
	require('jquery-ui');
	require('tag-it');

	var RangerPolicyForm = Backbone.Form.extend(
	/** @lends RangerPolicyForm */
	{
		_viewName : 'RangerPolicyForm',

    	/**
		* intialize a new RangerPolicyForm Form View 
		* @constructs
		*/
		templateData : function(){
			var policyType = XAUtil.enumElementByValue(XAEnums.RangerPolicyType, this.model.get('policyType')), conditionType;
			if (XAUtil.isMaskingPolicy(policyType.value)) {
				conditionType = 'Mask';
			} else if (XAUtil.isRowFilterPolicy(policyType.value)) {
				conditionType = 'Row Filter';
			} else {
				conditionType = 'Allow';
			}
			return { 'id' : this.model.id,
					'policyType' : policyType.label,
					'conditionType' : conditionType,
					'policyTimeBtnLabel': (this.model.has('validitySchedules') && this.model.get('validitySchedules').length > 0) ? localization.tt('lbl.editValidityPeriod')
						: localization.tt('lbl.addValidityPeriod'),
					'policyConditionHideShow' : (this.rangerServiceDefModel.has('policyConditions') && !_.isEmpty(this.rangerServiceDefModel.get('policyConditions'))) ?
					true : false,
					'policyConditionIconClass': (this.model.has('conditions') && this.model.get('conditions').length > 0) ? "fa fa-pencil" : "fa fa-plus",
					'conditionsData': (this.model.has('conditions') && this.model.get('conditions').length > 0) ?
						XAUtil.getPolicyConditionDetails(this.model.get('conditions'), this.rangerServiceDefModel) : [],
				};
		},
		initialize: function(options) {
			console.log("initialized a RangerPolicyForm Form View");
			_.extend(this, _.pick(options, 'rangerServiceDefModel', 'rangerService', 'rangerServiceDefList'));
    		Backbone.Form.prototype.initialize.call(this, options);

			this.initializeCollection();
			this.setupForm();
			this.bindEvents();
			this.defaultValidator={}
		},
		initializeCollection: function(){
			if(XAUtil.isMaskingPolicy(this.model.get('policyType'))){
				this.formInputList 		= XAUtil.makeCollForGroupPermission(this.model, 'dataMaskPolicyItems');
			}else if(XAUtil.isRowFilterPolicy(this.model.get('policyType'))){
				this.formInputList 		= XAUtil.makeCollForGroupPermission(this.model, 'rowFilterPolicyItems');
			}else{
				this.formInputList 		= XAUtil.makeCollForGroupPermission(this.model, 'policyItems');
			}
			this.formInputAllowExceptionList= XAUtil.makeCollForGroupPermission(this.model, 'allowExceptions');
			this.formInputDenyList 		= XAUtil.makeCollForGroupPermission(this.model, 'denyPolicyItems');
			this.formInputDenyExceptionList = XAUtil.makeCollForGroupPermission(this.model, 'denyExceptions');
			this.formInputResourceList = new Backbone.Collection();
		},
		/** all events binding here */
		bindEvents : function(){
			this.on('isAuditEnabled:change', function(form, fieldEditor){
    			this.evAuditChange(form, fieldEditor);
    		});
			this.on('isEnabled:change', function(form, fieldEditor){
				this.evIsEnabledChange(form, fieldEditor);
			});
		},
		ui : {
			'denyConditionItems' : '[data-js="denyConditionItems"]',
			'allowExcludePerm' : '[data-js="allowExcludePerm"]',
      		'policyTimeBtn'      : '[data-js="policyTimeBtn"]',
			'policyConditions' : '[data-js="customPolicyConditions"]',
            'conditionData' : '[data-id="conditionData"]',
            'isDenyAllElse' : '[data-js="isDenyAllElse"]',
		},
		/** fields for the form
		*/
		fields: ['name', 'description', 'isEnabled', 'isAuditEnabled','recursive'],
		schema :function(){
			return this.getSchema();
		},
		getSchema : function(){
			var basicSchema = ['name','isEnabled','policyPriority','policyLabels'];
			var schemaNames = this.getPolicyBaseFieldNames();
			if(this.model.schemaBase){
				var attr1 = _.pick(_.result(this.model,'schemaBase'),basicSchema);
				var attr2 = _.pick(_.result(this.model,'schemaBase'),schemaNames);
				return _.extend(attr1,attr2);
			}
		},
		/** on render callback */
		render: function(options) {
            var that = this;
			Backbone.Form.prototype.render.call(this, options);
			if(XAUtil.isAccessPolicy(this.model.get('policyType'))){
				this.evdenyAccessChange();
			}
			if(!this.model.isNew()){
				this.setUpSwitches();
			}
			this.renderPolicyResource();
			this.renderCustomFields();

			//to show error msg on below the field(only for policy name)
            if(this.fields.isEnabled){
                    this.fields.isEnabled.$el.find('.control-label').removeClass();
            }
            if(this.fields.name){
                    this.fields.name.$el.find('.help-inline').removeClass('help-inline').addClass('help-block margin-left-5');
            }
            this.initializePlugins();
            this.setPolicyValidityTime();
            this.setPolicyCondition();
        },
            setPolicyValidityTime : function(){
              var that = this;
              this.$el.find(this.ui.policyTimeBtn).on('click', function(e){
                  var policyDirtyField = that.model.has('validitySchedules') ? new Backbone.Collection(that.model.get('validitySchedules')) : new Backbone.Collection();
                  policyDirtyField.on('change',function(){
                      that.$el.find('[data-js="policyTimeBtn"]').addClass('dirtyField');
                  });
                  var view = new vPolicyTimeList({
                      collection: policyDirtyField,
                      model : that.model
                });
                var modal = new Backbone.BootstrapModal({
                  content	: view,
                  title	: 'Policy Validity Period',
                  okText  :"Save",
                  animate : true,
                  focusOk : false,
                  escape:false,
                  // allowCancel:false,
                  modalOptions:{
                    backdrop: 'static',
                    keyboard: false
                  },
                }).open();

                modal.$el.addClass('modal-policy-time');
                $('body').addClass('hideBodyScroll')
                //To prevent modal being close by click out of modal
                modal.$el.find('.cancel, .close').on('click', function(e){
                  modal._preventClose = false;
                  $('body').removeClass('hideBodyScroll');
                  $('[data-js="policyTimeBtn"]').addClass('dirtyField');
                  $(".datetimepicker").remove();
                });
                modal.$el.find('.ok').on('click', function(e){
                    modal._preventClose = false;
                    $('body').removeClass('hideBodyScroll');
                });
                modal.on('shown', function(a){
                  this.preventClose();
                });
              });
            },

        setPolicyCondition : function(){
            var that = this;
            this.$el.find(this.ui.policyConditions).on('click', function(e){
                var view = new vPolicyCondition({
                    rangerServiceDefModel : that.rangerServiceDefModel,
                    model : that.model
                });
                var modal = new Backbone.BootstrapModal({
                  content   : view,
                    title : 'Policy Conditions',
                    okText  :"Save",
                    animate : true,
                    focusOk : false,
                    escape:false,
                    modalOptions:{
                        backdrop: 'static',
                    },
                }).open();
                modal.$el.addClass('modal-policy-conditions');
                $('body').addClass('hideBodyScroll')
                //To prevent modal being close by click out of modal
                modal.$el.find('.cancel, .close').on('click', function(e){
                  modal._preventClose = false;
                  $('body').removeClass('hideBodyScroll');
                  $('[data-js="customPolicyConditions"]').addClass('dirtyField');
                });
                modal.$el.find('.ok').on('click', function(e){
                    modal._preventClose = false;
                    $('body').removeClass('hideBodyScroll');
                    $('[data-js="customPolicyConditions"]').addClass('dirtyField');
                    var conditions = [], $data = [];
                    _.each(modal.$el.find('[data-id="inputField"],[data-id="textAreaContainer"]'), function(m, context){
                        var inputFieldName = m.name;
                        if(m.value !== " " && !_.isEmpty(m.value)){
                            conditions.push({type : inputFieldName, values : (m.value.split(',')).filter(Boolean)});
                        }
                    });
                    that.model.set('conditions',conditions);
                    if(conditions.length > 0){
                        that.$el.find('[data-id="policyCondIcon"]').removeClass('fa-fw fa fa-plus').addClass('fa-fw fa fa-pencil');
                    } else {
                        that.$el.find('[data-id="policyCondIcon"]').removeClass('fa-fw fa fa-pencil').addClass('fa-fw fa fa-plus');
                    }
                    _.each(that.model.get('conditions'), function(val){
                        console.log(that);
                        var conditionName = that.rangerServiceDefModel.get('policyConditions').find(function(m){return m.name == val.type});
                        $data.push('<tr><td width="40%">'+_.escape(conditionName.label)+'</td><td width="60%">'+(val.values).toString()+'</td></tr>')
                    });
                    if($data.length > 0){
                        that.$el.find(that.ui.conditionData).html($data);
                    }else{
                        that.$el.find(that.ui.conditionData).html('<tr><td> No Conditions </td></tr>');
                    }
                });
                modal.on('shown', function(a){
                  this.preventClose();
                });

            })
        },

		initializePlugins : function() {
			var that = this;
			this.$(".wrap-header").each(function(i, ele) {
				var wrap = $(this).next();
				// If next element is a wrap and hasn't .non-collapsible class
				if (wrap.hasClass('wrap') && ! wrap.hasClass('non-collapsible')){
					$(this).append('<a href="#" class="wrap-expand pull-right" style="display: none">show&nbsp;&nbsp;<i class="fa-fw fa fa-caret-down"></i></a>')
						   .append('<a href="#" class="wrap-collapse pull-right" >hide&nbsp;&nbsp;<i class="fa-fw fa fa-caret-up"></i></a>');
				}
			});
			// Collapse wrap
			this.$el.on("click", "a.wrap-collapse", function() {
				var self = $(this).hide(100, 'linear');
				self.parent('.wrap-header').next('.wrap').slideUp(500, function() {
					$('.wrap-expand', self.parent('.wrap-header')).show(100, 'linear');
				});
				return false;
				// Expand wrap
			}).on("click", "a.wrap-expand", function() {
				var self = $(this).hide(100, 'linear');
				self.parent('.wrap-header').next('.wrap').slideDown(500, function() {
					$('.wrap-collapse', self.parent('.wrap-header')).show(100, 'linear');
				});
				return false;
			});
			
			//Hide show wrap-header based on policyItems 
			var parentPermsObj = { groupPermsDeny : this.formInputDenyList,  };
			var childPermsObj = { groupPermsAllowExclude : this.formInputAllowExceptionList, groupPermsDenyExclude : this.formInputDenyExceptionList}
			_.each(childPermsObj, function(val, name){
				if(val.length <= 0) {
					var wrap = this.$el.find('[data-customfields="'+name+'"]').parent();
					wrap.hide();
					$('.wrap-collapse', wrap.prev('.wrap-header')).hide();
					$('.wrap-expand', wrap.prev('.wrap-header')).show();
				}
			},this)
			
			_.each(parentPermsObj, function(val, name){
				if(val.length <= 0){
					var tmp = this.$el.find('[data-customfields="'+name+'"]').next();
					var childPerm = tmp.find('[data-customfields^="groupPerms"]');
					if(childPerm.parent().css('display') == 'none'){
						var wrap = this.$el.find('[data-customfields="'+name+'"]').parent();
						wrap.hide();
						$('.wrap-collapse', wrap.prev('.wrap-header')).hide();
						$('.wrap-expand', wrap.prev('.wrap-header')).show();
					}
				}
			},this)
		},
		evAuditChange : function(form, fieldEditor){
			XAUtil.checkDirtyFieldForToggle(fieldEditor.$el);
		},
		evIsEnabledChange : function(form, fieldEditor){
			XAUtil.checkDirtyFieldForToggle(fieldEditor.$el);
		},
		evdenyAccessChange : function(){
			var that =this;
			this.$el.find(this.ui.isDenyAllElse).toggles({
			    	on : that.model.has('isDenyAllElse') ? that.model.get('isDenyAllElse') : false,
			    	text : {on : 'True', off : 'False' },
			    	width : 80,
			}).on('click', function(e){
				XAUtil.checkDirtyFieldForToggle(that.$el.find(that.ui.isDenyAllElse));
				if(that.$el.find(that.ui.isDenyAllElse).find('.toggle-slide').hasClass('active')) {
					that.$el.find(that.ui.denyConditionItems).hide();
				} else {
					that.$el.find(that.ui.denyConditionItems).show();
				}
			});

		},
		setupForm : function() {
			var that = this;
			if(!this.model.isNew()){
				var additionalResources = [this.model.get('resources')];
				additionalResources = _.union(additionalResources, this.model.get('additionalResources'));

				_.each(additionalResources, function(resources, index) {
					that.formInputResourceList.add(Object.assign({resources: resources, id : "resource"+index, policyType: that.model.get('policyType') }, resources ));
				});
			}

		},
		setUpSwitches :function(){
			var that = this;
			this.fields.isAuditEnabled.editor.setValue(this.model.get('isAuditEnabled'));
			this.fields.isEnabled.editor.setValue(this.model.get('isEnabled'));
                    if(this.model.has('policyPriority')){
                        this.fields.policyPriority.editor.setValue(this.model.get('policyPriority') == 1 ? true : false);
                    }
		},
		/** all custom field rendering */
		renderCustomFields: function(){
			var that = this,
				accessType = this.rangerServiceDefModel.get('accessTypes').filter(function(val) { return val !== null; }),
				serviceDefOptions = this.rangerServiceDefModel.get('options'),
				enableDenyAndExceptionsInPolicies = false;
			//By default hide the PolicyItems for all component except tag component
			//show all policyItems if enableDenyAndExceptionsInPolicies is set to true
			enableDenyAndExceptionsInPolicies = XAUtil.showAllPolicyItems(this.rangerServiceDefModel, this.model);
			if( !enableDenyAndExceptionsInPolicies ){
				this.$el.find(this.ui.allowExcludePerm).hide();
				this.$el.find(this.ui.denyConditionItems).remove();
			}
			if(enableDenyAndExceptionsInPolicies && this.$el.find(this.ui.isDenyAllElse).find('.toggle-slide').hasClass('active')){
				this.$el.find(this.ui.denyConditionItems).hide();
			}
			var enableDenyAndExceptions = accessType.filter(function(m){
				if(!_.contains((that.rangerServiceDefList.map(function(m){
					if(m.get('options').enableDenyAndExceptionsInPolicies == "false"){
						return m.get("name")
					}
				})).filter(Boolean), m.name.substr(0,m.name.indexOf(":")))){
					return m
				}
			})

                        that.$('[data-customfields="groupPerms"]').html(new PermissionList({
                                collection : that.formInputList,
                                model 	   : that.model,
                                accessTypes: accessType,
                                headerTitle: "",
                                rangerServiceDefModel : that.rangerServiceDefModel,
                                rangerPolicyType : that.model.get('policyType')
                        }).render().el);
						
                        if( enableDenyAndExceptionsInPolicies && !XAUtil.isMaskingPolicy(that.model.get('policyType')) ){
                                that.$('[data-customfields="groupPermsAllowExclude"]').html(new PermissionList({
                                        collection : that.formInputAllowExceptionList,
                                        model 	   : that.model,
                                        accessTypes: that.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label ? accessType : enableDenyAndExceptions,
                                        headerTitle: "",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);

                                that.$('[data-customfields="groupPermsDeny"]').html(new PermissionList({
                                        collection : that.formInputDenyList,
                                        model 	   : that.model,
                                        accessTypes: that.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label ? accessType : enableDenyAndExceptions,
                                        headerTitle: "Deny",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);
                                that.$('[data-customfields="groupPermsDenyExclude"]').html(new PermissionList({
                                        collection : that.formInputDenyExceptionList,
                                        model 	   : that.model,
                                        accessTypes: that.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label ? accessType : enableDenyAndExceptions,
                                        headerTitle: "Deny",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);
                        }

		},
		beforeSave : function(){
			var additionResources = [];
			//set sameLevel fieldAttr value with resource name
			this.formInputResourceList.each(function(model) {
				var resource = {}
				_.each(model.attributes, function(val, key) {
					var isSameLevelResource = key.indexOf("sameLevel") >= 0, isResource = !['policyType', 'resourceForm', 'none', 'resources','id'].includes(key);
					if ((isSameLevelResource || isResource) && !_.isNull(val)) {
						var resourceName = isSameLevelResource ? val.resourceType : key;
						if(resourceName && resourceName != 'none'){
							model.set(resourceName, val);
							resource[resourceName] = {
								values: val.resource && val.resource.length && _.isObject(val.resource[0]) ? _.pluck(val.resource, 'text') : val.resource
							};
							if(!_.isUndefined(val.isRecursive)){
								resource[resourceName]['isRecursive'] = val.isRecursive;
							}
							if(!_.isUndefined(val.isExcludes)){
								resource[resourceName]['isExcludes'] = val.isExcludes;
							}
						}
						if(isSameLevelResource) {
							model.unset(key);
						}
					}
				}, this);
				additionResources.push(resource);
			});
			//To set resource values
			//Check for masking policies
			var resourceDef = this.rangerServiceDefModel.get('resources');
			if(XAUtil.isMaskingPolicy(this.model.get('policyType')) && XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef'))){
				if(!_.isEmpty(this.rangerServiceDefModel.get('dataMaskDef').resources)){
					resourceDef = this.rangerServiceDefModel.get('dataMaskDef').resources;
				}else{
					resourceDef = this.rangerServiceDefModel.get('resources');
				}
			}
			if(XAUtil.isRowFilterPolicy(this.model.get('policyType')) && XAUtil.isRenderRowFilter(this.rangerServiceDefModel.get('rowFilterDef'))){
				resourceDef = this.rangerServiceDefModel.get('rowFilterDef').resources;
			}

            if(this.model.has('policyLabels')){
                var policyLabel = _.isEmpty(this.model.get('policyLabels')) ? [] : this.model.get('policyLabels').split(',');
                this.model.set('policyLabels', policyLabel);
            }
            if(!_.isUndefined(App.vZone) && App.vZone.vZoneName){
                this.model.set('zoneName', App.vZone.vZoneName);
            }
			this.model.set('resources',additionResources[0]);
			additionResources.shift();
			this.model.set('additionalResources',additionResources);
			if(this.model.get('none')) {
				this.model.unset('none');
			}
			this.model.unset('path');
			
			//Set UserGroups Permission
			var RangerPolicyItem = Backbone.Collection.extend();
			if( XAUtil.isMaskingPolicy(this.model.get('policyType')) ){
				this.model.set('dataMaskPolicyItems', this.setPermissionsToColl(this.formInputList, new RangerPolicyItem()));
			}else if( XAUtil.isRowFilterPolicy(this.model.get('policyType')) ){
				this.model.set('rowFilterPolicyItems', this.setPermissionsToColl(this.formInputList, new RangerPolicyItem()));
			}else{
	            if(this.$el.find(this.ui.isDenyAllElse).find('.toggle-slide').hasClass('active')) {
	            	this.model.set('isDenyAllElse',true);
	            } else {
	            	this.model.set('isDenyAllElse',false);
	            }
				this.model.set('policyItems', this.setPermissionsToColl(this.formInputList, new RangerPolicyItem()));
				this.model.set('allowExceptions', this.setPermissionsToColl(this.formInputAllowExceptionList, new RangerPolicyItem()));
				if(!this.model.get('isDenyAllElse')){
					this.model.set('denyPolicyItems', this.setPermissionsToColl(this.formInputDenyList, new RangerPolicyItem()));
					this.model.set('denyExceptions', this.setPermissionsToColl(this.formInputDenyExceptionList, new RangerPolicyItem()));
				}else{
					this.model.set('denyPolicyItems',[]);
					this.model.set('denyExceptions',[]);
				}
			}
			this.model.set('service',this.rangerService.get('name'));
            var policyName = this.model.get('name');
            if(this.model.has('id') && XAUtil.checkForEscapeCharacter(policyName)){
                policyName = _.unescape(policyName);
            }
            this.model.set('name', _.escape(policyName));
                        if(this.model.has('policyPriority')){
                                this.model.set('policyPriority', this.model.get('policyPriority') ? 1 : 0);
                        }

		},
		setPermissionsToColl : function(list, policyItemList) {
			list.each(function(m){
                                if(!_.isUndefined(m.get('groupName')) || !_.isUndefined(m.get("userName")) || !_.isUndefined(m.get('roleName'))){ //groupName or userName
					var RangerPolicyItem=Backbone.Model.extend()
					var policyItem = new RangerPolicyItem();
					if(!_.isUndefined(m.get('groupName')) && !_.isNull(m.get('groupName'))){
						policyItem.set("groups",m.get("groupName"));
					}
					if(!_.isUndefined(m.get('userName')) && !_.isNull(m.get('userName'))){
						policyItem.set("users",m.get("userName"));
					}
                                        if(!_.isUndefined(m.get('roleName')) && !_.isNull(m.get('roleName'))){
                                                policyItem.set("roles",m.get("roleName"));
                                        }
					if(!(_.isUndefined(m.get('conditions')) && _.isEmpty(m.get('conditions')))){
						var RangerPolicyItemConditionList = Backbone.Collection.extend();
						var rPolicyItemCondList = new RangerPolicyItemConditionList(m.get('conditions'))
						policyItem.set('conditions', rPolicyItemCondList)
					}
					if(!_.isUndefined(m.get('delegateAdmin'))){
						policyItem.set("delegateAdmin",m.get("delegateAdmin"));
					}
					if(!_.isUndefined(m.get('accesses'))){
						var RangerPolicyItemAccessList = Backbone.Collection.extend();
						var rangerPlcItemAccessList = new RangerPolicyItemAccessList(m.get('accesses'));
						policyItem.set('accesses', rangerPlcItemAccessList)
					}
					if(!_.isUndefined(m.get('dataMaskInfo'))){
						policyItem.set("dataMaskInfo",m.get("dataMaskInfo"));
					}
					if(!_.isUndefined(m.get('rowFilterInfo'))){
						policyItem.set("rowFilterInfo",m.get("rowFilterInfo"));
					}
                                        policyItemList.add(policyItem);
					
					
				}
			}, this);
			return policyItemList;
		},
		formValidation : function(coll){
                        var groupSet = false , permSet = false , groupPermSet = false , delegateAdmin = false ,
                        userSet=false, userPerm = false, userPermSet =false,breakFlag =false, condSet = false,customMaskSet = true,
                        roleSet = false, rolePermSet = false, rolePerm = false;
			console.log('validation called..');
			coll.each(function(m){
				if(_.isEmpty(m.attributes)) return;
                if(m.has('groupName') || m.has('userName') || m.has('accesses') || m.has('delegateAdmin') || m.has('roleName')){
					if(! breakFlag){
						groupSet = m.has('groupName') ? true : false;
						userSet = m.has('userName') ? true : false;
                                                roleSet = m.has('roleName') ? true : false;
                                                permSet = m.has('accesses') ? true : false;
                                                delegateAdmin = m.has('delegateAdmin') ? m.get('delegateAdmin') : false;
						if(groupSet && permSet){
							groupPermSet = true;
							userPermSet = false;
                                                        rolePermSet = false;
						}else if(userSet && permSet){
							userPermSet = true;
							groupPermSet = false;
                                                        rolePermSet = false;
                                                }else if(roleSet && permSet){
                                                        rolePermSet = true;
                                                        userPermSet = false;
                                                        groupPermSet = false;
						}else{
                            if(!((userSet || groupSet || roleSet) && delegateAdmin)){
                                    breakFlag=true;
                            }
						}
					}
				}
				if(m.has('conditions') && !_.isEmpty(m.get('conditions'))){
					condSet = m.has('conditions') ? true : false;
				}
				if(m.has('dataMaskInfo') && !_.isUndefined(m.get('dataMaskInfo').dataMaskType)){
					if( m.get('dataMaskInfo').dataMaskType.indexOf("CUSTOM") >= 0 ){
						var valueExpr = m.get('dataMaskInfo').valueExpr;
						customMaskSet = _.isUndefined(valueExpr) || _.isEmpty(valueExpr.trim()) ? false : true;
					}
				}
			});
			
			var auditStatus = this.fields.isAuditEnabled.editor.getValue();
			var obj = { groupPermSet	: groupPermSet , groupSet : groupSet,	
						userSet 		: userSet, isUsers:userPermSet,
						auditLoggin 	: auditStatus,
						condSet			: condSet,
                        customMaskSet   : customMaskSet,
                        delegateAdmin	: delegateAdmin,
                        roleSet : roleSet, rolePermSet : rolePermSet,
					};
                        if(groupSet || userSet || roleSet){
				obj['permSet'] = groupSet ? permSet : false;
				obj['userPerm'] = userSet ? permSet : false;
                                obj['rolePerm'] = roleSet ? permSet : false;
			}else{
				obj['permSet'] = permSet;
				obj['userPerm'] = userSet;
                                obj['rolePerm'] = roleSet;
			}
			return obj;
		},
		getPolicyBaseFieldNames : function(){
			var baseField = ['description','isAuditEnabled', 'isDenyAllElse'];
			if(XAUtil.isMaskingPolicy(this.model.get('policyType')) || XAUtil.isRowFilterPolicy(this.model.get('policyType'))){
				baseField = _.without(baseField, 'isDenyAllElse');
			}
			return baseField;
		},
		getResources : function(){
			if(XAUtil.isMaskingPolicy(this.model.get('policyType'))){
				if(XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef'))){
					if(!_.isEmpty(this.rangerServiceDefModel.get('dataMaskDef').resources)){
						return this.rangerServiceDefModel.get('dataMaskDef').resources;
					}
				}
			}else if(XAUtil.isRowFilterPolicy(this.model.get('policyType'))){
				if(XAUtil.isRenderRowFilter(this.rangerServiceDefModel.get('rowFilterDef'))){
					return this.rangerServiceDefModel.get('rowFilterDef').resources;
				}
			}
			return this.rangerServiceDefModel.get('resources');
		},
		renderPolicyResource : function (){
			this.policyResourceForm = new ResourceList({
				collection : this.formInputResourceList,
				rangerPolicyModel: this.model,
				rangerServiceDefModel : this.rangerServiceDefModel,
				rangerService: this.rangerService,
			}).render();
			this.$('[data-customfields="policyResources"]').html(this.policyResourceForm.el);
		},
		validatePolicyResource : function (){
			var errors = null;
			_.some(this.formInputResourceList.models, function(model) {
				model.attributes = Object.assign({}, _.pick(model.attributes, 'id','resourceForm', 'policyType'));
				errors = model.get('resourceForm').commit({validate : false});
				return ! _.isEmpty(errors);
			});
			return errors;
		}
	});
	return RangerPolicyForm;
});
