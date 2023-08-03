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

	var Backbone 	= require('backbone');
	var XAUtils 	= require('utils/XAUtils');
	var XAEnums		= require('utils/XAEnums');
	var localization	= require('utils/XALangSupport');

	var FormDataType = Backbone.Model.extend({
		type : [ 'string', 'boolean', 'int' ],
		getFormElements : function(configs, enums, attrs, form, isPolicyForm) {
			//Helpers
			
			//Get configs for perticular policy type
			var getResourceConfigs = function(configs){
				if(XAUtils.isMaskingPolicy(form.model.get('policyType'))){
					if(XAUtils.isRenderMasking(form.rangerServiceDefModel.get('dataMaskDef'))){
						var resources = form.rangerServiceDefModel.get('dataMaskDef').resources;
						if(!_.isEmpty(resources)){
							configs = form.rangerServiceDefModel.get('dataMaskDef').resources;
						}
						configs = _.map(configs, function(obj){ obj.type =  'string'; return obj; });
						return configs;
					}
				}else if(XAUtils.isRowFilterPolicy(form.model.get('policyType'))){
					if(XAUtils.isRenderRowFilter(form.rangerServiceDefModel.get('rowFilterDef'))){
						configs = form.rangerServiceDefModel.get('rowFilterDef').resources;
						configs = _.map(configs, function(obj){ obj.type =  'string'; return obj; });
						return configs;
					}
				}
				return configs;
			};
			var getValidators = function(formObj, v){
				formObj.validators = [];
				if (_.has(v, 'mandatory') && v.mandatory && v.type != 'bool') {
					formObj.validators.push('required');
					if (_.isEmpty(formObj.title)) {
						formObj.title = formObj.title + " *"
					} else {
						formObj.title = formObj.title + " *"
					}
				}
				if(_.has(v, 'validationRegEx') && !_.isEmpty(v.validationRegEx) && !v.lookupSupported){
					formObj.validators.push({'type': 'regexp', 'regexp':new RegExp(v.validationRegEx), 'message' : v.validationMessage});
				}
				return formObj;
			};
			var setDefaultValueToModel = function(form, v) {
				if(_.has(v, 'defaultValue') && !_.isEmpty(v.defaultValue) && v.type != 'bool'){
					form.model.set(v.name, v.defaultValue)
				}
				return form;
			};
			
			//Get configs for perticular policy type
			configs = getResourceConfigs(configs);
			configs = _.sortBy(configs, function(m){ return m.itemId });
			configs = _.filter(configs, function(m){
				if(m.uiHint && !_.isEmpty(m.uiHint) && XAUtils.hideIfNull(m, form)){
					return;
				}
				return m;
			})
			var samelevelFieldCreated = [];
			_.each(configs, function(v, k,config) {
				if (v != null) {
					var formObj = {}, fieldName, supportedResource = [];
					switch (v.type) {
						case 'string':
							if(!isPolicyForm) {
								formObj.type = 'Text';
								if(!_.isUndefined(v.uiHint) && !_.isEmpty(v.uiHint)){
									var UIHint = JSON.parse(v.uiHint);
									if(!_.isUndefined(UIHint.TextFieldWithIcon) && UIHint.TextFieldWithIcon){
										formObj.type = 'TextFieldWithIcon';
										formObj.errorMsg = UIHint.info;
									}
								}
								break;
							}
							if($.inArray(v.parent, samelevelFieldCreated) >= 0){
								return;
							}
							if( isPolicyForm ){
								var resourceOpts = {};
								formObj.type = 'Resource';
								formObj.editorClass = "rosource-boder"
								formObj['excludeSupport']= v.excludesSupported;
								formObj['recursiveSupport'] = v.recursiveSupported;
								formObj.name = v.name;
//								formObj.level = v.level;
								//checkParentHideShow field
								formObj.fieldAttrs = { 'data-name' : 'field-'+v.name, 'parent' : v.parent };
								formObj.fieldAttrs.fieldClass = "resorces-with-label-css"
								formObj['resourceOpts'] = {'data-placeholder': v.label };
                                                                if(!_.isUndefined(v.lookupSupported)){
                                                                        var opts = {};
									if(_.has(v, 'validationRegEx') && !_.isEmpty(v.validationRegEx)){
                                        opts['regExpValidation'] = {'type': 'regexp', 'regexp':new RegExp(v.validationRegEx), 'message' : v.validationMessage};
                                    }
                                    //To support single value input
                                    if( XAUtils.isSinglevValueInput(v) ){
                                        opts['singleValueInput'] = true;
                                    }
                                    opts['type'] = v.name;
                                    if(v.lookupSupported){
                                        if (form.serviceName) {
											opts['lookupURL'] = "service/plugins/services/lookupResource/"+form.serviceName;
										} else {
											opts['lookupURL'] = "service/plugins/services/lookupResource/"+form.rangerService.get('name');
										}
                                        resourceOpts['select2Opts'] = form.getPlugginAttr(true, opts);
                                    }else{
                                        resourceOpts['select2Opts'] = XAUtils.select2OptionForUserCreateChoice();
                                        if(!_.isUndefined(opts.singleValueInput) && opts.singleValueInput){
                                            resourceOpts['select2Opts']['maximumSelectionSize'] = 1;
                                        }
									}
									formObj['resourceOpts'] = resourceOpts; 
								}
								//same level resources check
								var optionsAttrs = [] ,parentResource;
								if(!_.isUndefined(v.level)){
									optionsAttrs = _.filter(config,function(field){ 
										if(field.level == v.level && field.parent == v.parent){
											return field;	
										}
									});
								}
								var resourceDef = _.findWhere(optionsAttrs,{'name':v.name});
								//for parent leftnode status
								if(v.parent){
									parentResource = _.findWhere(config ,{'name':v.parent});
								}
								//show only required resources in acccess policy in order to show their access types
								if(!_.isUndefined(v.parent) && !_.isEmpty(v.parent)
										&& parentResource.isValidLeaf){
                                                                        if(form.model && form.model.isNew()) {
                                                                                optionsAttrs.push({'level':v.level, name:'none',label:'none'});
                                                                        } else {
                                                                                optionsAttrs.unshift({'level':v.level, name:'none',label:'none'});
                                                                        }
								}
								if(optionsAttrs.length > 1){
									var optionsTitle = _.map(optionsAttrs,function(field){ return field.name;});
									formObj['sameLevelOpts'] = optionsTitle;
                                                                        samelevelFieldCreated.push(v.parent);
                                                                        if(!_.isUndefined(v.parent)){
                                                                                fieldName = 'sameLevel'+v.level+''+v.parent;
                                                                        }else{
                                                                                fieldName = 'sameLevel'+v.level;
                                                                        }
									formObj['title'] = '';
									formObj.fieldAttrs.fieldClass = "resorces-css";
									formObj['resourcesAtSameLevel'] = true;
									
									// formView is used to listen form events
									formObj['formView'] = form;
								}
							}
							break;
						case 'bool':
							if(!_.isUndefined(v.subType) && !_.isEmpty(v.subType)){
								formObj.type = 'Select';
								var subType = v.subType.split(':')
								formObj.options = [subType[0].substr(0, subType[0].length - 4), subType[1].substr(0, subType[1].length - 5)];
								//to set default value 
								if(form.model.isNew()){
									if(!_.isUndefined(v.defaultValue) && v.defaultValue === "false"){
										form.model.set(v.name, subType[1].substr(0, subType[1].length - 5))
									}
								}
							}else{
								formObj.type = 'Checkbox';
								formObj.options = {	y : 'Yes',n : 'No'};
							}
							break;
						case 'int':formObj.type = 'Number';break;
						case 'enum':
							var enumObj = _.find(enums, function(e) {return e && e.name == v.subType;});
							formObj.type = 'Select';
//							formObj.options = _.pluck(_.compact(enumObj.elements),'label');
							formObj.options = _.map((enumObj.elements), function(obj) {
								return { 'label' : obj.label, 'val': obj.name};
							});
							break;
						case 'path' : 
							formObj.type = 'Resource';
							formObj.editorClass = "rosource-boder-path"
							formObj['excludeSupport']= v.excludesSupported;
							formObj['recursiveSupport'] = v.recursiveSupported;
							formObj['name'] = v.name;
							formObj['editorAttrs'] = {'data-placeholder': v.label };
							formObj.fieldAttrs = { 'data-name' : 'field-'+v.name, 'parent' : v.parent };
							formObj.fieldAttrs.fieldClass = "resorces-with-label-css"
							if(!_.isUndefined(v.lookupSupported)){
								var options = {
									'containerCssClass' : v.name,
								};
							if(v.lookupSupported){
								if (form.serviceName) {
									options['lookupURL'] = "service/plugins/services/lookupResource/"+form.serviceName;
								} else {
									options['lookupURL'] = "service/plugins/services/lookupResource/"+form.rangerService.get('name');
								}
							}
								//to support regexp level validation
								if(_.has(v, 'validationRegEx') && !_.isEmpty(v.validationRegEx)){
									options['regExpValidation'] = {'type': 'regexp', 'regexp':new RegExp(v.validationRegEx), 'message' : v.validationMessage};
								}
								form.pathFieldName = v.name;
								form.pathPluginOpts = options;
								form.initilializePathPlugin = true;
							}
							formObj['initilializePathPlugin'] = true;
                                                        var optionsAttrs = [] ,parentResource;
                                                                if(!_.isUndefined(v.level)){
                                                                        optionsAttrs = _.filter(config,function(field){
                                                                                if(field.level == v.level && field.parent == v.parent){
                                                                                        return field;
                                                                                }
                                                                        });
                                                                }
                                                                var resourceDef = _.findWhere(optionsAttrs,{'name':v.name});
                                                                //for parent leftnode status
                                                                if(v.parent){
                                                                        parentResource = _.findWhere(config ,{'name':v.parent});
                                                                }
                                                                //show only required resources in acccess policy in order to show their access types
                                                                if(!_.isUndefined(v.parent) && !_.isEmpty(v.parent)
                                                                                && parentResource.isValidLeaf){
                                                                    if(form.model && form.model.isNew()) {
                                                                        optionsAttrs.push({'level':v.level, name:'none',label:'none'});
                                                                    } else {
									optionsAttrs.unshift({'level':v.level, name:'none',label:'none'});
                                                                    }
                                                                }
                                                                if(optionsAttrs.length > 1){
                                                                        var optionsTitle = _.map(optionsAttrs,function(field){ return field.name;});
                                                                        formObj['sameLevelOpts'] = optionsTitle;
                                                                        samelevelFieldCreated.push(v.parent);
                                                                        if(!_.isUndefined(v.parent)){
                                                                                fieldName = 'sameLevel'+v.level+''+v.parent;
                                                                        }else{
                                                                                fieldName = 'sameLevel'+v.level;
                                                                        }
                                                                        formObj['title'] = '';
                                                                        formObj['resourcesAtSameLevel'] = true;

                                                                        // formView is used to listen form events
                                                                        formObj['formView'] = form;
                                                                }
							break;
						case 'password':formObj.type = 'Password';break;
						default:formObj.type = 'Text';
						break;
					}
					if(_.isUndefined(formObj.title)){
						formObj.title = v.label || v.name;
					}
					formObj = getValidators(formObj, v);
					if(form.model.isNew()){
						form = setDefaultValueToModel(form, v)
					}	
					formObj['class'] = 'serviceConfig';
					if(_.isUndefined(fieldName)){
						fieldName = v.name;
					}
					attrs[fieldName] = formObj;
				}
			});
			return attrs;
		},
	});
	return FormDataType;

});
