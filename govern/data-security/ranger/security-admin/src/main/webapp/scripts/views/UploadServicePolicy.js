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
	var App				= require('App');
	var XAUtil			= require('utils/XAUtils');
	var XAEnums			= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var localization	= require('utils/XALangSupport');
	var RangerServiceList 	= require('models/RangerService');
	var UploadservicepolicyTmpl = require('hbs!tmpl/common/uploadservicepolicy_tmpl');
	
	var ServiceMappingItem = Backbone.Marionette.ItemView.extend({
		_msvName : 'ServiceMappingItem',
		template : require('hbs!tmpl/common/ServiceMappingItem'),
		ui : { 
			sourceInput : 'input[data-id="source"]',
			destinationSelect : '[data-id="destination"]',
			deleteMap : 'a[data-id="delete"]',
			'overrridCheck'	: 'input[data-name="override"]:checked',
		},
		events : function(){
			var events = {};
			events['change ' + this.ui.sourceInput]	= 'onSourceChange';
			events['change ' + this.ui.destinationSelect]	= 'onDestinationSelect';
			events['click ' + this.ui.deleteMap]	= 'onDeleteMapClick';
			return events;
		},

		initialize : function(options) {
			_.extend(this, _.pick(options, 'collection','serviceNames','services','sourceData','zoneDestination','serviceType'));
			
		},
		onSourceChange : function(e){
			var sourceValue = e.currentTarget.value.trim();
			this.model.set('source', _.isEmpty(sourceValue) ? undefined : sourceValue);
		},
		onDestinationSelect : function(e) {
			this.model.set('destination', _.isEmpty(e.currentTarget.value) ? undefined : e.currentTarget.value);
			var serviceTypes = _.find( this.services.models , function(m){
				return m.get('name') == e.currentTarget.value
			});
			if(!_.isUndefined(serviceTypes)){
				this.model.set('serviceType' , serviceTypes.get('type') );
			}else{
				this.model.set('serviceType' , " " );
			}
		},
		onDeleteMapClick : function(){
			this.collection.remove(this.model)	
		},

		onRender : function() {
			var that = this;
			// source services
			this.ui.sourceInput.val(this.model.get('source'));
			var sourceOptions = _.map(_.groupBy(this.sourceData.policies , function(m){return m.service}), function(m, key){ return { 'id' : key, 'text' : key}; });
			this.ui.sourceInput.select2({
				closeOnSelect: true,
				placeholder: 'Select source name',
				width: '220px',
				allowClear: true,
				data:sourceOptions,
			});
			// destination services
			var serviceNameList = [], options;
			if(that.model && that.model.has('sourceServiceType')){
				serviceNameList = _.filter(this.serviceNames, function(m){
					return m.get('type') == that.model.get('sourceServiceType')
				})
				options = _.map(serviceNameList, function(m){ return { 'id' : m.get('name'), 'text' : m.get('name')}});
			}else{
				options = _.map(this.serviceNames, function(m){ return { 'id' : m.get('name'), 'text' : m.get('name')}});
			}
			if(_.some(options,function(m){return m.id === that.model.get('source')})){
				this.ui.destinationSelect.val(that.model.get('source'));
				this.model.set('destination', that.model.get('source'))
			}else{
				this.ui.destinationSelect.val();
			}
			this.ui.destinationSelect.select2({
				closeOnSelect: true,
				placeholder: 'Select service name',
				width: '220px',
				allowClear: true,
				data:options,
			});
		}
	});
	
	var UploadServicePolicy = Backbone.Marionette.CompositeView.extend({
		
		template : UploadservicepolicyTmpl,
		templateHelpers : function(){
			return { 'serviceType' : this.serviceType };
		},
		getItemView : function(item){
			if(!item){
				return;
			}
			return ServiceMappingItem;
		},
		itemViewContainer : ".js-serviceMappingItems",
		itemViewOptions : function() {
			return {
				'collection' 	: this.collection,
				'serviceNames' 	: this.serviceNames,
				'services'      : this.services,
				'sourceData'    : this.importFileData,
				'zoneDestination': this.ui.zoneDestination.val(),
				'serviceType'   : this.serviceType
			};
		},
		initialize: function(options) {
			this.bind("ok", this.okClicked);
			_.extend(this, _.pick(options, 'collection','serviceNames','serviceDefList','serviceType','services',
				'rangerZoneList'));
		},
		ui:{
			'importFilePolicy'  : '[data-id="uploadPolicyFile"]',
			'addServiceMaping'	: '[data-id="addServiceMaping"]',
			'fileNameClosebtn' 	: '[data-id="fileNameClosebtn"]',
			'zoneSource'        : '[data-id="zoneSource"]',
			'zoneDestination'   : '[data-id="zoneDestination"]',
			'selectFileValidationMsg' : '[data-id="selectFileValidationMsg"]',
			'selectServicesMapping': '[data-id="selectServicesMapping"]',
			'selectZoneMapping' : '[data-id="selectZoneMapping"]'
		},
		events: function() {
			var events = {};
			events['change ' + this.ui.importFilePolicy] = 'importPolicy';
			events['click ' + this.ui.addServiceMaping] = 'onAddClick';
			events['click ' + this.ui.fileNameClosebtn]	= 'fileNameClosebtn';
			return events;
		},
		okClicked: function (modal) {
			if( _.isUndefined(this.targetFileObj)){
				this.ui.selectFileValidationMsg.show();
				return modal.preventClose();
			}
			var that = this, serviceMapping = {}, fileObj = this.targetFileObj, preventModal = false , url ="", zoneMapping = {};;
			if(!_.isEmpty(this.ui.zoneDestination.val()) || !_.isEmpty(this.ui.zoneSource.val())){
				zoneMapping[this.ui.zoneSource.val()] = this.ui.zoneDestination.val();
			}
			this.collection.each(function(m){
				if( m.get('source') !== undefined && m.get('destination') == undefined 
						|| m.get('source') == undefined && m.get('destination') !== undefined ){
					that.$el.find('.serviceMapErrorMsg').show();
					that.$el.find('.serviceMapTextError').hide();
					preventModal = true;
				}
				if(!_.isUndefined(m.get('source'))){
					serviceMapping[m.get('source')] = m.get('destination');
				}
			});
			if(preventModal){
				modal.preventClose();
				return;
			}
			if(this.collection.length>1){
				that.collection.models.some(function(m){
					if (!_.isEmpty(m.attributes)) {
						if (m.has('source') && m.get('source') != '') {
							var model = that.collection.where({
								'source': m.get('source')
							});
							if (model.length > 1) {
								that.$el.find('.serviceMapTextError').show();
								that.$el.find('.serviceMapErrorMsg').hide();
								preventModal = true;
								return true;
							}
						}
					}
				})
			}
			if(preventModal){
				modal.preventClose();
				return;
			}
			this.formData = new FormData();
			this.formData.append('file', fileObj);
			//service mapping details
			if(!_.isEmpty(serviceMapping)){
				this.formData.append('servicesMapJson', new Blob([JSON.stringify(serviceMapping)],{type:'application/json'}));
			}
			//zone mapping details
			if(!_.isEmpty(zoneMapping)){
				this.formData.append('zoneMapJson', new Blob([JSON.stringify(zoneMapping)],{type:'application/json'}));
			}
			//override flag
			if(this.$el.find('input[data-name="override"]').is(':checked')){
				url = "service/plugins/policies/importPoliciesFromFile?isOverride=true";
			}else{
				url = "service/plugins/policies/importPoliciesFromFile?isOverride=false";
			}
			var compString = ''
			if(!_.isUndefined(that.serviceType)){
				compString=that.serviceType
			}else{
				var selectedZoneServices = [], selectedZone;
				if(!_.isUndefined( that.ui.zoneDestination.val()) && !_.isEmpty( that.ui.zoneDestination.val())){
                    selectedZone = _.find(that.rangerZoneList.attributes, function (m){
                        return m.name == that.ui.zoneDestination.val();
                    })
                    var zoneServiceListModel = new RangerServiceList();
                    zoneServiceListModel.clear();
                    zoneServiceListModel.fetch({
                        cache : false,
                        async : false,
                        url : "service/public/v2/api/zones/"+selectedZone.id+"/service-headers",
                    });
                    if(!_.isEmpty(zoneServiceListModel.attributes)) {
                        _.filter(zoneServiceListModel.attributes, function(obj) {
                            var zoneServiceModel = that.services.find(function(m) {
                                return m.get('name') == obj.name;
                            });
                            if (zoneServiceModel) {
                                selectedZoneServices.push(zoneServiceModel);
                            }
                        })
                    }
				}else{
					selectedZoneServices = this.serviceNames;
				}
				compString = _.map(_.groupBy(selectedZoneServices, function(m){return m.get('type')}), function(m, key){return key}).toString();
			}
			XAUtil.blockUI();
			if (!_.isUndefined($('.latestResponse')) && $('.latestResponse').length > 0) {
				$('.latestResponse').html('<b>Last Response Time : </b>' + Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
			}
			$.ajax({
				type: 'POST',
				url: url+"&serviceType="+compString,
				enctype: 'multipart/form-data',
				data: this.formData,
				cache: false,
				dataType:'Json',
				contentType: false,
				processData: false,
				success: function () {
					XAUtil.blockUI('unblock');
					var msg =  'File import successfully.' ;
					XAUtil.notifySuccess('Success', msg);
	
				},
				error : function(response,model){
					XAUtil.blockUI('unblock');
					if ( response && response.responseJSON && response.responseJSON.msgDesc){
						if(response.status == '419'){
							XAUtil.defaultErrorHandler(model,response);
						}else{
							XAUtil.notifyError('Error', response.responseJSON.msgDesc);
						}
					} else {
						XAUtil.notifyError('Error', 'File import failed.');
					}
				}
			});
		},
		onAddClick : function(){
			this.collection.add(new Backbone.Model());
		},
		onRender: function() {
			this.ui.selectFileValidationMsg.hide();
			this.ui.selectZoneMapping.hide();
			this.ui.selectServicesMapping.hide();
		},
		importPolicy : function(e){
			var that =this;
			console.log("uploading....");
			this.ui.selectFileValidationMsg.hide(); 
                        if(e.target && e.target.files.length > 0){
                                this.targetFileObj = e.target.files[0];
                        } else {
                                return
                        }
			if(!_.isUndefined(this.targetFileObj)){
                                this.$el.find('.selectFile').text(this.targetFileObj.name);
                                this.$el.find('.selectFile').append('<i></i><label class="icon fa-fw fa fa-remove fa-fw fa fa-1x fa-fw fa fa-remove-btn" data-id="fileNameClosebtn"></label>');
                                //check if file name is proper json extension or not
                                if(this.targetFileObj.type === "application/json" || (this.targetFileObj.name).match(".json$", "i")){
                                        this.selectedFileValidation(e)
                                } else {
                                        this.ui.selectFileValidationMsg.show();
                                        this.fileNameClosebtn(false);
                                        return
                                }
			} else {
                                this.$el.find('.selectFile').text("No file chosen");
			}
		},
		selectedFileValidation : function(file){
			var that = this,
			fileReader = new FileReader();
			fileReader.onload =	function(e){
				try {
					that.importFileData = JSON.parse(e.target.result);
				} catch(e) {
					// error in the above string (in this case, yes)!
					that.$el.find(that.ui.selectFileValidationMsg).html(e).show();
					return
				}
				var sourceZonePolicy = _.filter(that.importFileData.policies, function(m){
					if(m.zoneName){ return m.zoneName }
				});
				that.selectZoneMappingData(_.groupBy(sourceZonePolicy, function(m){ return m.zoneName }));
			}
			fileReader.readAsText(file.target.files[0]);
		},
		selectZoneMappingData: function(sourceZoneName){
			var that = this;
			//souece zone value
			this.ui.selectZoneMapping.show();
			this.ui.selectServicesMapping.show();
			if(sourceZoneName){
				this.ui.zoneSource.val(_.escape(_.keys(sourceZoneName)[0]));
			}else{
				this.ui.zoneSource.val('');
			}
			//Destination zone value
			this.setServiceDestination();
			//Destination service values
			if(this.serviceType && ! _.isEmpty(this.serviceType)){
				this.serviceNames = this.services.models.filter(function(m){return that.serviceType == m.get('type')});
			}else{
				this.serviceNames = this.services.models
			}
			this.setServiceSourceData();
 		},

		setServiceSourceData: function(){
			var that = this,
			serviceSources = _.groupBy(that.importFileData.policies , function(m){
				return m.service
			})
			_.map(serviceSources, function(m , key){
				var sourceServiceDef = that.serviceDefList.find(function(model){
					return model.get('id') == m[0].serviceType
				});
				if(sourceServiceDef){
					that.collection.add(new Backbone.Model({'source' : key, 'sourceServiceType' : sourceServiceDef.get('name')}));
				}else{
					that.collection.add(new Backbone.Model({'source' : key}));
				}
			})
		},
		setServiceDestination : function(){
			var that =this,
			zoneNameOption = _.map(that.rangerZoneList.attributes, function(m){
				return { 'id':m.name, 'text':m.name}
			});
			this.ui.zoneDestination.attr('disabled',false);
			this.ui.zoneDestination.select2({
				closeOnSelect: true,
				placeholder: 'Select service name',
				width: '220px',
				allowClear: true,
				data:zoneNameOption,
			}).on('change', function(e){
				that.collection.reset();
				if(e.added && !_.isEmpty(e.val)){
					var  zoneServiceList = [];
					that.ui.selectServicesMapping.show();
					that.serviceNames = that.services.models;
                    var selectedZone = _.find( that.rangerZoneList.attributes, function (m){
                        return m.name == e.val
                    })
                    var zoneServiceListModel = new RangerServiceList();
                    zoneServiceListModel.clear();
                    zoneServiceListModel.fetch({
                        cache : false,
                        async : false,
                        url : "service/public/v2/api/zones/"+selectedZone.id+"/service-headers",
                    });
                    if(!_.isEmpty(zoneServiceListModel.attributes)) {
                        _.filter(zoneServiceListModel.attributes, function(obj) {
                            var zoneServiceModel = that.serviceNames.find(function(m) {
                                return m.get('name') == obj.name;
                            });
                            if (zoneServiceModel) {
                                zoneServiceList.push(zoneServiceModel);
                            }
                        })
                    }
					that.serviceNames = zoneServiceList;
					that.setServiceSourceData();
				}else{
					if(that.serviceType && ! _.isEmpty(that.serviceType)){
						that.serviceNames = that.services.models.filter(function(m){return that.serviceType == m.get('type')});
					}else{
						that.serviceNames = that.services.models;
					}
					that.setServiceSourceData();
				}
			});
		},
                fileNameClosebtn : function(fileSelected){
                        if(fileSelected && fileSelected.hasOwnProperty('currentTarget')){
                                this.$el.find('.selectFile').text("No file chosen");
                                this.ui.selectFileValidationMsg.hide();
                        }
			this.targetFileObj = undefined;
			this.ui.importFilePolicy.val('');
			this.ui.selectServicesMapping.hide();
			this.ui.selectZoneMapping.hide();
			this.collection.reset();
			this.ui.zoneDestination.val('');
		}
		
	});
	return UploadServicePolicy; 
});
