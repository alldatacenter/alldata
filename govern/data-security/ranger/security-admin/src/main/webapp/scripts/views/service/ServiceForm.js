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
	var XAUtil			= require('utils/XAUtils');

	var localization	= require('utils/XALangSupport');
	var BackboneFormDataType	= require('models/BackboneFormDataType');
	var ConfigurationList		= require('views/service/ConfigurationList');
	var AuditFilterConfig		= require('views/service/AuditFilterConfig')

	require('backbone-forms');
	require('backbone-forms.list');
	require('backbone-forms.templates');
	require('backbone-forms.XAOverrides');

	var ServiceForm = Backbone.Form.extend(
	/** @lends ServiceForm */
	{
		_viewName : 'ServiceForm',

    	/**
		* intialize a new ServiceForm Form View 
		* @constructs
		*/
		templateData: function(){
			var serviceDetail="", serviceConfig="";
			_.each(this.schema, function(obj, name){
			  if(!_.isUndefined(obj['class']) && obj['class'] == 'serviceConfig'){
				  serviceConfig += name+",";
			  } else {
				  serviceDetail += name+",";
			  }
			});

			return {
				serviceDetail : serviceDetail.slice(0,-1),
				serviceConfig : serviceConfig.slice(0,-1)
			};
		},
		ui : {
			renderAuditFilter : '[data-id="renderAuditFilter"]',
		},
		events : {
			'change [data-id="renderAuditFilter"]'  : 'renderAuditFilter',
		},

		initialize: function(options) {
			console.log("initialized a ServiceForm Form View");
			_.extend(this, _.pick(options, 'rangerServiceDefModel'));
			this.extraConfigColl = new Backbone.Collection();
			this.auditFilterColl = new Backbone.Collection();
			this.setupFormForEditMode();
    		Backbone.Form.prototype.initialize.call(this, options);

			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			this.on('isEnabled:change', function(form, fieldEditor){
				this.evIsEnabledChange(form, fieldEditor);
			});
			// this.on('change [data-id="renderAuditFilter"]', function(){
			// 	this.renderAuditFilter();
			// })
		},

		/** schema for the form
		* If you see that a field should behave similarly in all the forms, its 
		* better to make the change in this.model.schema itself
		*
		* Override here ONLY if special case!!
		*/

                fields: ['name', 'displayName', 'description', 'isEnabled', 'type','configs', '_vPassword'],

		schema : function(){
			var attrs = _.pick(_.result(this.rangerServiceDefModel,'schemaBase'), this.getSerivceBaseFieldNames());
			var that = this;
			var formDataType = new BackboneFormDataType();
			return formDataType.getFormElements(this.rangerServiceDefModel.get('configs'),this.rangerServiceDefModel.get('enums'), attrs, this, false);
		},

		/** on render callback */
		render: function(options) {
			Backbone.Form.prototype.render.call(this, options);

			this.setupForm();
			this.initializePlugins();
			this.renderCustomFields();
			this.renderAuditFilterFields();
			this.renderAuditFilter();
		},
		setupFormForEditMode : function() {
			var that = this;
			if(!this.model.isNew()){
				if(this.model.get('configs')['ranger.plugin.audit.filters']) {
					var auditFilterCollValue = this.model.get('configs')['ranger.plugin.audit.filters'];
					delete this.model.get('configs')['ranger.plugin.audit.filters']
				}
				var configs = this.rangerServiceDefModel.get('configs');
				var auditFilterCollValueIndex = _.findIndex(configs,function(m){
					return m.name == 'ranger.plugin.audit.filters'
				})
				if(auditFilterCollValueIndex != -1) {
					configs.splice(auditFilterCollValueIndex, 1);
				}
				_.each(this.model.get('configs'),function(value, name){
					var configObj = _.findWhere(this.rangerServiceDefModel.get('configs'),{'name' : name });
					if(!_.isUndefined(configObj) && configObj.type == 'bool'){
						this.model.set(name, this.getStringFromBoolean(configObj, value))
					} else {
						this.model.set(name, value)
						if(_.isUndefined(configObj)){
							this.extraConfigColl.add(new Backbone.Model({'name' : name, 'value' : value}))
						}
					}
				},this);

				if(auditFilterCollValue) {
					auditFilterCollValue = JSON.parse((auditFilterCollValue).replace(/'/g, '"'));
					auditFilterCollValue.forEach(function(model) {
						that.auditFilterColl.add(new Backbone.Model(model));
					})
				}
			} else {
				var configs = this.rangerServiceDefModel.get('configs');
				var auditFilterCollValueIndex = _.findIndex(configs,function(m){
					return m.name == 'ranger.plugin.audit.filters'
				})
				if(auditFilterCollValueIndex != -1) {
					var auditFilterCollValue = configs[auditFilterCollValueIndex];
					configs.splice(auditFilterCollValueIndex, 1);
					auditFilterCollValue = JSON.parse((auditFilterCollValue.defaultValue).replace(/'/g, '"'));
					console.log(auditFilterCollValue);
					auditFilterCollValue.forEach(function(model) {
						that.auditFilterColl.add(new Backbone.Model(model));
					})
				}
			}
		},
		setupForm : function() {
			if(this.model.isNew()){
				this.fields.isEnabled.editor.setValue(XAEnums.ActiveStatus.STATUS_ENABLED.value);
			} else {
				//Set isEnabled Status
				if(XAEnums.ActiveStatus.STATUS_ENABLED.value == this.model.get('isEnabled')){
					this.fields.isEnabled.editor.setValue(XAEnums.ActiveStatus.STATUS_ENABLED.value);
				} else {
					this.fields.isEnabled.editor.setValue(XAEnums.ActiveStatus.STATUS_DISABLED.value);
				}
			}
		},
		evIsEnabledChange : function(form, fieldEditor){
			XAUtil.checkDirtyFieldForToggle(fieldEditor.$el);
		},
		/** all custom field rendering */
		renderCustomFields: function(){
			this.$('.extraServiceConfigs').html(new ConfigurationList({
				collection : this.extraConfigColl,
				model 	   : this.model,
				fieldLabel : localization.tt('lbl.addNewConfig')
			}).render().el);
		},

		/**Audit filters rendering**/
		renderAuditFilterFields: function(){
			this.$('[data-customfields="aduitFilterConfig"]').html(new AuditFilterConfig({
				collection : this.auditFilterColl,
				rangerServiceDefModel : this.rangerServiceDefModel,
				serviceName : (!_.isUndefined(this.model) && !_.isEmpty(this.model.get('name')) ? this.model.get('name') : ''),
			}).render().el);
		},

		renderAuditFilter : function(e) {
			var that = this;
			if (_.isUndefined(e)) {
				if(this.auditFilterColl.length > 0) {
					this.$el.find('input[data-id="renderAuditFilter"]').prop('checked', true);
				}
			} else {
				if ($(e.currentTarget).is(":checked")) {
					if (!_.isUndefined(this.newauditFilterColl) && this.newauditFilterColl.length > 0) {
						this.$el.find('.emptySet').remove();
						this.newauditFilterColl.forEach(function(model){
							that.auditFilterColl.add(model);
						})
					}
				} else {
					this.newauditFilterColl= this.auditFilterColl.clone();
					this.auditFilterColl.reset()
					this.renderAuditFilterFields()
				}
			}
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		formValidation : function(){
			var valid = true;
			var config = {};

			for (var i = 0; i < this.extraConfigColl.length; i++) {
				var obj = this.extraConfigColl.at(i);
				if(!_.isEmpty(obj.attributes)) {
					if (!_.isUndefined(config[obj.get('name')])) {
						XAUtil.alertPopup({
							msg : localization.tt('msg.duplicateNewConfigValidationMsg')
						});
						valid = false;
						break;
					} else {
						config[obj.get('name')] = obj.get('value');
					}
				}
			}

			return valid;
		},

		beforeSave : function(){
			var that = this;
			//Set configs for service 
			var config = {};
			if(!_.isEmpty(this.rangerServiceDefModel.get('configs'))){
				_.each(this.rangerServiceDefModel.get('configs'),function(obj){
					if(!_.isNull(obj)){
						if(obj.type == 'bool'){
							config[obj.name] = that.getBooleanForConfig(obj, that.model);
						} else {
							config[obj.name] = _.isNull(that.model.get(obj.name)) ? "" : that.model.get(obj.name).toString();
						}
						if(!_.isNull(obj.name)) {
							that.model.unset(obj.name);
						}
					}
				});
			}
			this.extraConfigColl.each(function(obj){
				if(!_.isEmpty(obj.attributes)) config[obj.get('name')] = obj.get('value');
			});
			if (this.auditFilterColl.length > 0) {
				var auditFiltter = [];
				this.auditFilterColl.each(function (e) {
					auditFiltter.push(e.attributes);
				})
				config['ranger.plugin.audit.filters'] = (JSON.stringify(auditFiltter)).replace(/"/g, "'");
			} else {
				config['ranger.plugin.audit.filters'] = "";
			}
			this.model.set('configs',config);

			//Set service type
			this.model.set('type',this.rangerServiceDefModel.get('name'))
			//Set isEnabled
			if(parseInt(this.model.get('isEnabled')) == XAEnums.ActiveStatus.STATUS_ENABLED.value){
				this.model.set('isEnabled',true);
			} else {
				this.model.set('isEnabled',false);
			}

			//Remove unwanted attributes from model
			if(!this.model.isNew()){
				_.each(this.model.attributes.configs, function(value, name){
					this.model.unset(name)
				},this);
			}
		},
		removeElementFromArr : function(arr ,elem){
			var index = $.inArray(elem,arr);
			if(index >= 0) arr.splice(index,1);
			return arr;
		},
		getBooleanForConfig : function(cofigObj, model) {
			var subType = cofigObj.subType.split(':');
			if(subType[0].indexOf(model.get(cofigObj.name)) >= 0 ){
				return true;
			} else {
				return false;
			}
		},
		getStringFromBoolean : function(configObj, value) {
			var subType = configObj.subType.split(':');
			if(subType[0].toLowerCase().indexOf(value) >= 0 ){
				return subType[0].substr(0, subType[0].length - 4);
			} else {
				return subType[1].substr(0, subType[0].length - 5);
			}
		},
		getSerivceBaseFieldNames : function(){
                         var fields = ['name', 'displayName', 'description', 'isEnabled','tagService']
			 return this.rangerServiceDefModel.get('name') == XAEnums.ServiceType.SERVICE_TAG.label ? fields.slice(0,fields.indexOf("tagService")) : fields;
		}
	});

	return ServiceForm;
});
