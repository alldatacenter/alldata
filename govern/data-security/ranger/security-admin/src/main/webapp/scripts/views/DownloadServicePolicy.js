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
	var DownloadservicepolicyTmpl = require('hbs!tmpl/common/downloadservicepolicy_tmpl');	 
	var RangerPolicyList	= require('collections/RangerPolicyList');
	var localization		= require('utils/XALangSupport');
	
	var DownloadServicePolicy = Backbone.Marionette.ItemView.extend({
		template : DownloadservicepolicyTmpl,
		
		initialize: function(options) {
			console.log("initialized a DownloadServicePolicy Layout");
                        var that = this, componentServices = [];
                        _.extend(this, _.pick(options, 'collection','serviceNames','serviceDefList','serviceType','services',
                                'zoneServiceDefList','zoneServices'));
                        if(!_.isEmpty(that.zoneServices) && !_.isUndefined(that.zoneServices)){
                                _.each(that.zoneServices, function(value, key){
                                        if(key === that.serviceType){
                                                componentServices = componentServices.concat(value);
                                        }
                                });
                        }else{
                                componentServices = this.services.where({'type' : this.serviceType });
                        }
			this.serviceNames = componentServices.map(function(m){ return { 'name' : m.get('name') } })
			this.bind("ok", this.okClicked);
		},
		ui:{
			'servicesName'		: '[data-id="servicesName"]',
			'componentTypeSelected'		: '[data-id="componentTypeSelected"]'
		},
		events: function() {
		},
	    
		okClicked: function (modal) {
			var that = this, el = $(modal.currentTarget),
                        urls ='/service/plugins/policies/exportJson',
            serviceName = this.ui.servicesName.val();
            if (_.isEmpty(this.ui.componentTypeSelected.val())){
            	this.$el.find('.serviceValidationFile').show();
    		}
			if(_.isEmpty(serviceName)){
        		this.$el.find('.validateFile').show();
        		if(!_.isEmpty(this.ui.componentTypeSelected.val())){
        			this.$el.find('.serviceValidationFile').hide();
        		}
				return modal.preventClose();
        	}
            var urlString = XAUtil.getBaseUrl();
			if(urlString.slice(-1) == "/") {
				urlString = urlString.slice(0,-1);
			};
                        if(App.vZone && App.vZone.vZoneName && !_.isEmpty(App.vZone.vZoneName)){
                                var exportUrl = urlString +urls+ '?serviceName='+serviceName+'&zoneName='+App.vZone.vZoneName;
                        }else{
                                var exportUrl = urlString +urls+ '?serviceName='+serviceName;
                        }
			XAUtil.blockUI();
            if (!_.isUndefined($('.latestResponse')) && $('.latestResponse').length > 0) {
                $('.latestResponse').html('<b>Last Response Time : </b>' + Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
            }
			$.ajax({
		        type: "GET",
                        url:exportUrl+'&checkPoliciesExists=true',
		        success:function(data,status,response){
		        	XAUtil.blockUI('unblock');
		        	if(response.status == 200 || response.statusText == "ok"){
                                    var downloadUrl = exportUrl+'&checkPoliciesExists=false';
				    var downloadReport = $('<a href ="'+downloadUrl+'"></a>');
				    downloadReport.appendTo('body');
				    downloadReport[0].click();
				    downloadReport.remove();
		        	}else{
		        		XAUtil.alertBoxWithTimeSet(localization.tt('msg.noPolicytoExport'))
		        	}
		        	
		        },
                        error : function(data,status,response){
				XAUtil.blockUI('unblock');
				XAUtil.defaultErrorHandler(status,data);
                        },
		    });
        },
	 	onRender: function() {
			this.serviceSelect();
		    if(_.isUndefined(this.serviceType)){
				 this.$el.find('.seviceFiled').show();
				 this.renderComponentSelect()
			}else{
				 this.$el.find('.seviceFiled').hide();
			}
		},
		renderComponentSelect: function(){
			var that = this;
                        if(!_.isEmpty(this.zoneServiceDefList) && !_.isUndefined(this.zoneServiceDefList)){
                                var options = this.zoneServiceDefList.map(function(m){ return { 'id' : m.get('name'), 'text' : m.get('name')}});
                        }else{
                                var options = this.serviceDefList.map(function(m){ return { 'id' : m.get('name'), 'text' : m.get('name')}});
                        }
			var componentTyp = options.map(function(m){return m.text})
            this.ui.componentTypeSelected.val(componentTyp);
			this.ui.componentTypeSelected.select2({
				multiple: true,
				closeOnSelect: true,
				placeholder: 'Select Component',
			    width: '700px',
			    allowClear: true,
			    data: options
			}).on('change', function(e){
				console.log(e);
				var selectedComp  = e.currentTarget.value, componentServices = [];
				_.each(selectedComp.split(","), function(type){
                                        if(!_.isEmpty(that.zoneServices) && !_.isUndefined(that.zoneServices)){
                                                _.each(that.zoneServices, function(value, key){
                                                        if(key === type){
                                                                componentServices = componentServices.concat(value);
                                                        }
                                                });
                                        }else{
                                                that.serviceNam = that.services.where({'type' : type });
                                                componentServices = componentServices.concat(that.serviceNam);
                                        }
				});
				var names = componentServices.map(function(m){ return { 'name' : m.get('name') } });
				that.serviceNames = names;
				that.collection.trigger('reset')
				that.serviceSelect(that.serviceNam)
			}).trigger('change');

		},
		serviceSelect :function(e){
			var options =this.serviceNames.map(function(m){ return { 'id' : m.name, 'text' : m.name}; });
			var serviceTyp = options.map(function(m){return m.text})
            this.ui.servicesName.val(serviceTyp);
			this.ui.servicesName.select2({
				multiple: true,
				closeOnSelect: true,
				placeholder: 'Select Service Name',
			    width: '700px',
			    allowClear: true,
			    data: options
			})
		}
		
	});
	return DownloadServicePolicy; 
});
