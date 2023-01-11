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
    
	var VXAuditMap		= require('models/VXAuditMap');
	var VXPermMap		= require('models/VXPermMap');
	var VXPermMapList	= require('collections/VXPermMapList');
	var VXGroupList		= require('collections/VXGroupList');
	var VXAuditMapList	= require('collections/VXAuditMapList');
	var VXUserList		= require('collections/VXUserList');
	var PermissionList 	= require('views/policies/PermissionList');
    var vPolicyTimeList 	= require('views/policies/PolicyTimeList');
	var RangerPolicyResource		= require('models/RangerPolicyResource');
	var BackboneFormDataType	= require('models/BackboneFormDataType');
    var App             = require('App');
    var vPolicyCondition = require('views/policies/RangerPolicyConditions');

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
			_.extend(this, _.pick(options, 'rangerServiceDefModel', 'rangerService'));
    		this.setupForm();
    		Backbone.Form.prototype.initialize.call(this, options);

			this.initializeCollection();
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
		},
		/** all events binding here */
		bindEvents : function(){
			this.on('isAuditEnabled:change', function(form, fieldEditor){
    			this.evAuditChange(form, fieldEditor);
    		});
			this.on('isEnabled:change', function(form, fieldEditor){
				this.evIsEnabledChange(form, fieldEditor);
			});
			this.on('policyForm:parentChildHideShow',this.renderParentChildHideShow);
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
			var attrs = {},that = this;
                        var basicSchema = ['name','isEnabled','policyPriority','policyLabels'];
			var schemaNames = this.getPolicyBaseFieldNames();

			var formDataType = new BackboneFormDataType();
			attrs = formDataType.getFormElements(this.rangerServiceDefModel.get('resources'),this.rangerServiceDefModel.get('enums'), attrs, this, true);
                        if(this.model.schemaBase){
                                var attr1 = _.pick(_.result(this.model,'schemaBase'),basicSchema);
                                var attr2 = _.pick(_.result(this.model,'schemaBase'),schemaNames);
                                return _.extend(attr1,_.extend(attrs,attr2));
                        }
		},
		/** on render callback */
		render: function(options) {
            var that = this;
			Backbone.Form.prototype.render.call(this, options);
			//initialize path plugin for hdfs component : resourcePath
                        if(!_.isUndefined(this.initilializePathPlugin) && this.initilializePathPlugin){
				this.initializePathPlugins(this.pathPluginOpts);
			}
			if(XAUtil.isAccessPolicy(this.model.get('policyType'))){
				this.evdenyAccessChange();
			}
			if(!this.model.isNew()){
				this.setUpSwitches();
			}
			this.renderCustomFields();
			//checkParent
			this.renderParentChildHideShow();

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
			if(!this.model.isNew()){
				this.selectedResourceTypes = {};
				var resourceDefList = this.rangerServiceDefModel.get('resources');
				if(XAUtil.isMaskingPolicy(this.model.get('policyType')) && XAUtil.isRenderMasking(this.rangerServiceDefModel.get('dataMaskDef'))){
					if(!_.isEmpty(this.rangerServiceDefModel.get('dataMaskDef').resources)){
						resourceDefList = this.rangerServiceDefModel.get('dataMaskDef').resources;
					}
				}
				if(XAUtil.isRowFilterPolicy(this.model.get('policyType')) && XAUtil.isRenderRowFilter(this.rangerServiceDefModel.get('rowFilterDef'))){
					if(!_.isEmpty(this.rangerServiceDefModel.get('rowFilterDef').resources)){
						resourceDefList = this.rangerServiceDefModel.get('rowFilterDef').resources;
					}
				}
				_.each(this.model.get('resources'),function(obj,key){
					var resourceDef = _.findWhere(resourceDefList,{'name':key}),
					sameLevelResourceDef = [], parentResource ;
					sameLevelResourceDef = _.filter(resourceDefList, function(objRsc){
						if (objRsc.level === resourceDef.level && objRsc.parent === resourceDef.parent) {
							return objRsc
						}
					});
					//for parent leftnode status
                    if(resourceDef.parent){
                    	parentResource = _.findWhere(resourceDefList ,{'name':resourceDef.parent});
                    }
                    if(sameLevelResourceDef.length == 1 && !_.isUndefined(sameLevelResourceDef[0].parent)
                    		&& !_.isEmpty(sameLevelResourceDef[0].parent)
                    		&& parentResource.isValidLeaf){
//						optionsAttrs.unshift({'level':v.level, name:'none',label:'none'});
                    	sameLevelResourceDef.push({'level':sameLevelResourceDef[0].level, name:'none',label:'none'});
                    }
					if(sameLevelResourceDef.length > 1){
						obj['resourceType'] = key;
                                                if(_.isUndefined(resourceDef.parent)){
                                                        this.model.set('sameLevel'+resourceDef.level, obj);
                                                        //parentShowHide
                                                        this.selectedResourceTypes['sameLevel'+resourceDef.level] = key;
                                                }else{
                                                        this.model.set('sameLevel'+resourceDef.level+resourceDef.parent, obj);
                                                        this.selectedResourceTypes['sameLevel'+resourceDef.level+resourceDef.parent] = key;
                                                }

					}else{
						//single value support
						/*if(! XAUtil.isSinglevValueInput(resourceDef) ){
							this.model.set(resourceDef.name, obj)
						}else{
							//single value resource
							this.model.set(resourceDef.name, obj.values)
						}*/
						this.model.set(resourceDef.name, obj)
					}
                                },this);
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
                                        accessTypes: accessType,
                                        headerTitle: "",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);

                                that.$('[data-customfields="groupPermsDeny"]').html(new PermissionList({
                                        collection : that.formInputDenyList,
                                        model 	   : that.model,
                                        accessTypes: accessType,
                                        headerTitle: "Deny",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);
                                that.$('[data-customfields="groupPermsDenyExclude"]').html(new PermissionList({
                                        collection : that.formInputDenyExceptionList,
                                        model 	   : that.model,
                                        accessTypes: accessType,
                                        headerTitle: "Deny",
                                        rangerServiceDefModel : that.rangerServiceDefModel,
                                        rangerPolicyType : that.model.get('policyType')
                                }).render().el);
                        }

		},
		renderParentChildHideShow : function(onChangeOfSameLevelType, val, e) {
			var formDiv = this.$el.find('.policy-form');
			if(!this.model.isNew() && !onChangeOfSameLevelType){
				_.each(this.selectedResourceTypes, function(val, sameLevelName) {
					if(formDiv.find('.field-'+sameLevelName).length > 0){
						formDiv.find('.field-'+sameLevelName).attr('data-name','field-'+val)
					}
				});
			}
			//hide form fields if it's parent is hidden
			var resources = formDiv.find('.form-group');
			_.each(resources, function(rsrc , key ){ 
				var parent = $(rsrc).attr('parent');
				var label = $(rsrc).find('label').html();
				$(rsrc).removeClass('error');
//				remove validation and Required msg
				$(rsrc).find('.help-inline').empty();
				$(rsrc).find('.help-block').empty();
				if( !_.isUndefined(parent) && ! _.isEmpty(parent)){
	                var selector = "div[data-name='field-"+parent+"']";
	                var kclass = formDiv.find(selector).attr('class');
	                var label = $(rsrc).find('label').html();
	                if(_.isUndefined(kclass) || (kclass.indexOf("field-sameLevel") >= 0
	                                && formDiv.find(selector).find('select').val() != parent) 
	                                || formDiv.find(selector).hasClass('hideResource')){
	                	$(rsrc).addClass('hideResource');
	                	$(rsrc).removeClass('error');
//	                	reset input field to "none" if it is hide
	                	var resorceFieldName = _.pick(this.schema ,this.selectedFields[key]);
	                	if(resorceFieldName[this.selectedFields[key]].sameLevelOpts && _.contains(resorceFieldName[this.selectedFields[key]].sameLevelOpts , 'none') 
	                			&& formDiv.find(selector).find('select').val() != 'none' && onChangeOfSameLevelType){
						//change trigger and set value to selected node
							$(rsrc).find('select').val($(rsrc).find('select option:nth-child(1)').text()).trigger('change',"onChangeResources");
		                }
	                }else{
	                    if($(rsrc).find('select').val() == 'none'){
	                    		$(rsrc).find('input[data-js="resource"]').select2('disable');
	                            $(rsrc).removeClass('error');
	                            $(rsrc).find('label').html(label.split('*').join(''));
	                    }else{
	                            $(rsrc).find('input[data-js="resource"]').select2('enable');
	                            $(rsrc).find('label').html(label.slice(-1) == '*' ? label : label +"*");
	                    }
	                        $(rsrc).removeClass('hideResource');
					}
				}
			},this);
			//remove validation of fields if it's hidden
			//remove validation if fields is not empty
			_.each(this.fields, function(field, key){
				if((key.substring(0,key.length-2) === "sameLevel") && field.$el.find('[data-js="resource"]').val()!="" && field.$el.hasClass('error')){
					field.$el.removeClass('error');
					field.$el.find('.help-inline').empty();
				}
				if(field.$el.hasClass('hideResource')){
					if($.inArray('required',field.editor.validators) >= 0){
						this.defaultValidator[key] = field.editor.validators;
						field.editor.validators=[];
						var label = field.$el.find('label').html();
						field.$el.find('label').html(label.replace('*', ''));
						field.$el.removeClass('error');
						field.$el.find('.help-inline').empty();
					}
				}else{
	                if(field.$el.find('select').val() == 'none'){
	                    field.editor.validators=[];
	                    this.defaultValidator[key] = field.editor.validators;
	                    field.$el.removeClass('error');
	                    field.$el.find('.help-inline').empty();
	                }else{
	                    if(!_.isUndefined(this.defaultValidator[key])){
	                    	field.editor.validators.push('required');
		                    if($.inArray('required',field.editor.validators) >= 0){
		                        var label = field.$el.find('label').html();
		                        field.$el.find('label').html(label.slice(-1) == '*'  ? label : label +"*");
		                    }
						}
					}
				}
			}, this);
		},
		beforeSave : function(){
			var that = this, resources = {};
			//set sameLevel fieldAttr value with resource name
			_.each(this.model.attributes, function(val, key) {
				if(key.indexOf("sameLevel") >= 0 && !_.isNull(val)){
					this.model.set(val.resourceType,val);
					that.model.unset(key);
				}
			},this);
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
			_.each(resourceDef,function(obj){
				if(!_.isNull(obj)){
					var tmpObj =  that.model.get(obj.name);
					var rPolicyResource = new RangerPolicyResource();
					//single value support
//					if(! XAUtil.isSinglevValueInput(obj) ){
					if(!_.isUndefined(tmpObj) && _.isObject(tmpObj) && !_.isEmpty(tmpObj.resource)){
						rPolicyResource.set('values',_.map(tmpObj.resource, function(m) {
							if(obj.type == "path") {
								return m
							} else {
								return m.text
							}
						}));
						if(!_.isUndefined(tmpObj.isRecursive)){
							rPolicyResource.set('isRecursive', tmpObj.isRecursive)
						}
						if(!_.isUndefined(tmpObj.isExcludes)){
							rPolicyResource.set('isExcludes', tmpObj.isExcludes)
						}
						resources[obj.name] = rPolicyResource;
						that.model.unset(obj.name);
					}
//					}else{
//						//For single value resource
//						rPolicyResource.set('values',tmpObj.split(','));
//						resources[obj.name] = rPolicyResource;
//						that.model.unset(obj.name);
//					}
				}
			});
            if(this.model.has('policyLabels')){
                var policyLabel = _.isEmpty(this.model.get('policyLabels')) ? [] : this.model.get('policyLabels').split(',');
                this.model.set('policyLabels', policyLabel);
            }
            if(!_.isUndefined(App.vZone) && App.vZone.vZoneName){
                this.model.set('zoneName', App.vZone.vZoneName);
            }
			this.model.set('resources',resources);
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
		/** all post render plugin initialization */
		initializePathPlugins: function(options){
			var that= this,defaultValue = [];
                        if(!this.model.isNew() && !_.isUndefined(this.model.get('path'))){
				defaultValue = this.model.get('path').values;
			}
			var tagitOpts = {}
            if(!_.isUndefined(options.lookupURL) && options.lookupURL){
                tagitOpts["autocomplete"] = {
                    cache: false,
                    source: function( request, response ) {
                        var url = "service/plugins/services/lookupResource/"+that.rangerService.get('name');
                        var context ={
                            'userInput' : request.term,
                            'resourceName' : that.pathFieldName,
                            'resources' : {}
                        };
                        var val = that.fields[that.pathFieldName].editor.getValue('pathField');
                        context.resources[that.pathFieldName] = _.isNull(val) || _.isEmpty(val) ? [] : val.resource;
                        var p = $.ajax({
                            url : url,
                            type : "POST",
                            data : JSON.stringify(context),
                            dataType : 'json',
                            contentType: "application/json; charset=utf-8",
                        }).done(function(data){
                            if(data){
                                response(data);
                            } else {
                                response();
                            }
                        }).fail(function(responses){
                            if (responses && responses.responseJSON && responses.responseJSON.msgDesc) {
                                XAUtil.notifyError('Error', responses.responseJSON.msgDesc);
                            } else {
                                XAUtil.notifyError('Error', localization.tt('msg.resourcesLookup'));
                            }
                            response();
                        });
                        setTimeout(function(){
                            p.abort();
                            console.log('connection timeout for resource path request...!!');
                        }, 10000);
                    },
                    open : function(){
                        $(this).removeClass('working');
                    },
                    search: function() {
                     	if(!_.isUndefined(this.value) && this.value.includes('||')) {
                     		var values = this.value.trim().split('||');
                     		if (values.length > 1) {
                                for (var i = 0; i < values.length; i++) {
                                    that.fields[that.pathFieldName].editor.$el.find('[data-js="resource"]').tagit("createTag", values[i]);
                                }
                                return ''
                            } else {
                                return val
                            }
                     	}
                    }
                }
            }
            tagitOpts['beforeTagAdded'] = function(event, ui) {
                // do something special
                that.fields[that.pathFieldName].$el.removeClass('error');
                that.fields[that.pathFieldName].$el.find('.help-inline').html('');
                var tags =  [];
                if(!_.isUndefined(options.regExpValidation) && !options.regExpValidation.regexp.test(ui.tagLabel)){
                    that.fields[that.pathFieldName].$el.addClass('error');
                    that.fields[that.pathFieldName].$el.find('.help-inline').html(options.regExpValidation.message);
                    return false;
                }
            }
            tagitOpts['singleFieldDelimiter'] = '||';
            tagitOpts['singleField'] = false;
            this.fields[that.pathFieldName].editor.$el.find('input[data-js="resource"]').tagit(tagitOpts).on('change', function(e){
                //check dirty field for tagit input type : `path`
                XAUtil.checkDirtyField($(e.currentTarget).val(), defaultValue.toString(), $(e.currentTarget));
            });
		},
		getPlugginAttr :function(autocomplete, options){
			var that =this, type = options.containerCssClass, validRegExpString = true, select2Opts=[];
			if(!autocomplete)
				return{tags : true,width :'220px',multiple: true,minimumInputLength: 1, 'containerCssClass' : type};
			else {
				select2Opts = {
					containerCssClass : options.type,
					closeOnSelect : true,
					tags:true,
					multiple: true,
					width :'220px',
					tokenSeparators: [' '],
					initSelection : function (element, callback) {
						var data = [];
						//to set single select value
						if(_.isArray(JSON.parse(element.val()))) {
							$(JSON.parse(element.val())).each(function () {
								data.push({id: this, text: this});
							})
						}
						callback(data);
					},
					createSearchChoice: function(term, data) {
						term = _.escape(term);					
						if ($(data).filter(function() {
							return this.text.localeCompare(term) === 0;
						}).length === 0) {
							if(!_.isUndefined(options.regExpValidation) && !options.regExpValidation.regexp.test(term)){
									validRegExpString = false; 
							}else if($.inArray(term, this.val()) >= 0){
								return null;
							}else{
								return {
									id : "<b><i class='text-muted-select2'>Create</i></b> " + term,
									text: term,
								};
							}
						}
					},
					ajax: {
						url: options.lookupURL,
						type : 'POST',
						params : {
							timeout: 10000,
							contentType: "application/json; charset=utf-8",
						},
						cache: false,
						data: function (term, page) {
							return that.getDataParams(term, options);
						},
						results: function (data, page) { 
							var results = [];
							if(!_.isUndefined(data)){
								if(_.isArray(data) && data.length > 0){
									results = data.map(function(m, i){	return {id : m, text: m};	});
								}
								if(!_.isUndefined(data.resultSize) &&  data.resultSize != "0"){
									results = data.vXStrings.map(function(m, i){	return {id : m.value, text: m.value};	});
								}
							}
							return { 
								results : results
							};
						},
						transport: function (options) {
							$.ajax(options).fail(function(response) {
								if (response && response.responseJSON && response.responseJSON.msgDesc) {
									XAUtil.notifyError('Error', response.responseJSON.msgDesc);
								} else {
									XAUtil.notifyError('Error', localization.tt('msg.resourcesLookup'));
								}
								this.success({
									resultSize : 0
								});
							});
						}

					},	
					formatResult : function(result){
						return result.id;
					},
					formatSelection : function(result){
						return result.text;
					},
					formatNoMatches : function(term){
						if(!validRegExpString && !_.isUndefined(options.regExpValidation)){
							return options.regExpValidation.message;
						}
						return "No Matches found";
					}
				};	
				//To support single value input
				if(!_.isUndefined(options.singleValueInput) && options.singleValueInput){
					select2Opts['maximumSelectionSize'] = 1;
				}
				return select2Opts;
			}
		},
		getDataParams : function(term, options) {
			var resources = {},resourceName = options.type, dataResources = {};
			var isParent = true, name = options.type, val = null,isCurrentSameLevelField = true;
			while(isParent){
				var currentResource = _.findWhere(this.getResources(), {'name': name });
				//same level type
				if(_.isUndefined(this.fields[currentResource.name])){
                                        if(!_.isUndefined(currentResource.parent)){
                                                var sameLevelName = 'sameLevel'+currentResource.level + currentResource.parent;
                                        }else{
                                                var sameLevelName = 'sameLevel'+currentResource.level;
                                        }

					name = this.fields[sameLevelName].editor.$resourceType.val()
					val = this.fields[sameLevelName].getValue();
					if(isCurrentSameLevelField){
						resourceName = name;
					}
				}else{
					val = this.fields[name].getValue();
				}
				resources[name] = _.isNull(val) ? [] : val.resource;
				if(!_.isEmpty(currentResource.parent)){
					name = currentResource.parent;
				}else{
					isParent = false;
				}
				isCurrentSameLevelField = false;
			}
			if(resources && !_.isEmpty(resources)) {
				_.each(resources, function(val, key) {
					dataResources[key] = _.map(val, function(obj){
						return obj.text
					})
				})
			}
			var context ={
					'userInput' : term,
					'resourceName' : resourceName,
					'resources' : dataResources
				};
			return JSON.stringify(context);
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
	});
	return RangerPolicyForm;
});
